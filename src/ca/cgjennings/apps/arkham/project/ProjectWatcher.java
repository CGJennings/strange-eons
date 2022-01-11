package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.Timer;

/**
 * Watches for file changes within a {@link Project}. The root {@link Project}
 * instance for a project maintains an instance of this class and makes it
 * accessible to other project members via a
 * {@linkplain Project#getWatcher() package private method}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
final class ProjectWatcher {

    private final Project project;
    private WatchService watcher;
    private final Map<WatchKey, Member> keyMap;
    private volatile Thread eventThread;
    private Timer queueTrigger;
    private final Set<PendingUpdate> updateQueue = Collections.synchronizedSet(new LinkedHashSet<>());
    private PendingUpdate lastAdded; // for coalescing

    /**
     * Creates a watcher for the specified project. The project creates this
     * itself before adding child members. Folder child members will register
     * themselves with the watcher as they are created.
     *
     * @param p the project to watch
     */
    public ProjectWatcher(Project p) {
        // if for some reason we fail to create the watcher, this will be null;
        // in this case the methods all still work but they are no-ops
        project = p;
        keyMap = Collections.synchronizedMap(new HashMap<>(64));
        try {
            watcher = FileSystems.getDefault().newWatchService();
            eventThread = new Thread("Project watcher") {
                @Override
                public void run() {
                    processEvents();
                }
            };
            eventThread.setDaemon(true);
            queueTrigger = new Timer(250, (ActionEvent e) -> {
                processPendingQueueEvents();
            });
            queueTrigger.setRepeats(false);

            // only once eveything has been successfully set up do we
            // start the thread and register the caller as the first
            // member to monitor
            eventThread.start();
            register(p);
        } catch (IOException e) {
            watcher = null;
            StrangeEons.log.log(Level.SEVERE, "failed to create service", e);
        }
    }

    /**
     * Register a new member with the watcher. The member must represent some
     * kind of folder.
     *
     * @param m the member to register
     */
    public void register(Member m) {
        if (!m.isFolder()) {
            throw new IllegalArgumentException("not a folder");
        }
        if (watcher == null) {
            return;
        }

        Path path = m.getFile().toPath();
        try {
            WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            keyMap.put(key, m);
            StrangeEons.log.log(Level.FINE, "watching {0}", m);
        } catch (IOException e) {
            StrangeEons.log.log(Level.WARNING, "failed to register " + m.getName(), e);
        }
    }

    /**
     * Unregisters a member with the watcher. If the watcher is monitoring the
     * folder represented by the member, it will stop monitoring it.
     *
     * @param m the member to unregister
     */
    public void unregister(Member m) {
        if (!m.isFolder()) {
            throw new IllegalArgumentException("not a folder");
        }
        if (watcher == null) {
            return;
        }

        synchronized (keyMap) {
            for (Entry<WatchKey, Member> entry : keyMap.entrySet()) {
                if (entry.getValue().equals(m)) {
                    WatchKey wk = entry.getKey();
                    wk.cancel();
                    wk.pollEvents();
                }
            }
        }
    }

    /**
     * Closes the watcher, ending the watch process.
     */
    public void close() {
        if (watcher == null) {
            return;
        }
        queueTrigger.stop();
        Thread t = eventThread;
        eventThread = null;
        t.interrupt();
        try {
            t.join();
        } catch (InterruptedException e) {
            // it is generally the case that the watcher will be completely
            // shutdown when the method returns, but we don't guarantee it
        }
        watcher = null;
    }

    /**
     * Processes events from the file watcher, bundling them up into a queue of
     * {@code PendingUpdate}s which will be acted on in the EDT after a brief
     * delay.
     */
    private void processEvents() {
        StrangeEons.log.info("started watching project " + project.getName());
        for (;;) {
            try {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException ie) {
                    watcher.close();
                    break;
                } catch (NullPointerException npe) {
                    // very rarely, we might interrupt this thread, join it,
                    // and then get interrupted before the thread dies, in
                    // which case this will eventually end up here
                    return;
                }

                // handle all queued events in turn
                for (WatchEvent<?> event : key.pollEvents()) {
                    final Kind<?> anyKind = event.kind();
                    if (anyKind == OVERFLOW) {
                        StrangeEons.log.info("queue overflowed: project may be out of synch");
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    final Kind<Path> kind = (Kind<Path>) anyKind;
                    if (kind == ENTRY_CREATE || kind == ENTRY_DELETE) {
                        Member m = keyMap.get(key);
                        enqueue(kind, m);
                        updateQueue.add(new PendingUpdate(kind, m));
                    } else if (kind == ENTRY_MODIFY) {
                        // determine the exact member that was modified
                        Member m = keyMap.get(key);
                        m = m.findChild(((Path) event.context()).toFile());
                        if (m != null) {
                            enqueue(kind, m);
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    keyMap.remove(key);
                }

                // signal the watcher to update the project after a delay
                queueTrigger.restart();
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, "exception during watch event", e);
            }
        }
        StrangeEons.log.log(Level.INFO, "stopped watching project {0}", project.getName());
    }

    /**
     * Adds an entry to the update queue. This method is a bit smarter than
     * adding an entry explicitly since it can coalesce entries. This reduces
     * the number of calls to {@code Member.synchronizeImpl}, which is fairly
     * expensive.
     *
     * @param kind the kind of event to add
     * @param m the member that the event applies to
     */
    private void enqueue(Kind<Path> kind, Member m) {
        synchronized (updateQueue) {
            if (updateQueue.isEmpty()) {
                lastAdded = null;
            }
            if (lastAdded != null && lastAdded.member == m) {
                if (kind == ENTRY_MODIFY) {
                    // coalesce ENTRY_MODIFY events
                    if (lastAdded.kind == kind) {
                        StrangeEons.log.info("coalesced ENTRY_MODIFY update");
                        return;
                    }
                } else {
                    StrangeEons.log.info("coalesced ENTRY_* update");
                    // coalesce all other events as equivalent, but promote
                    // the type to ENTRY_CREATE if different
                    if (lastAdded.kind != kind) {
                        lastAdded.kind = ENTRY_CREATE;
                    }
                    return;
                }
            }
            // can't coalesce, add member
            lastAdded = new PendingUpdate(kind, m);
            updateQueue.add(lastAdded);
        }
    }

    /**
     * This method is called by members when they are
     * {@link Member#synchronize() synchronize()}d. Calling the method is meant
     * to be a hint that a batch of updates has been completed and that now is a
     * good time to update the project tree. It will cause any pending updates
     * to be processed in the near future.
     *
     * @param folder the folder being synchronized
     */
    public void synchronize(final Member folder) {
        if (watcher == null) {
            return;
        }
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> {
                synchronize(folder);
            });
            return;
        }
        synchronized (updateQueue) {
            enqueue(null, folder);
            processPendingQueueEvents();
        }
    }

    /**
     * Explicitly adds a member to the queue of pending watch updates. This can
     * be used when you want to explicitly synch several folders in a batch.
     * Enqueue all of the folders except the last using this method, then
     * {@link #synchronize(ca.cgjennings.apps.arkham.project.Member)} the final
     * folder.
     *
     * @param folder the folder to add to the update queue
     */
    public void synchronizeLater(final Member folder) {
        if (watcher == null) {
            return;
        }
        enqueue(null, folder);
    }

    /**
     * Handles all currently queued events by updating the project structure.
     * After this method returns, the queue will be empty (although it may
     * immediately and asynchronously start filling up again).
     *
     * <p>
     * <b>This method may only be called from the EDT.</b>
     */
    private void processPendingQueueEvents() {
        /////////////////////////////////////////////
        // THIS METHOD IS ONLY CALLED FROM THE EDT //
        /////////////////////////////////////////////
        synchronized (updateQueue) {
            for (PendingUpdate pu : updateQueue) {
                StrangeEons.log.log(Level.INFO, "project watcher update: {0}", pu);
                if (pu.kind == ENTRY_MODIFY) {
                    // TODO: update project view properties if currently showing this file
                } else {
                    pu.member.synchronizeImpl();
                }
            }
            updateQueue.clear();
        }
    }

    /**
     * A small structure that captures the state of a pending update so it can
     * be placed in the update queue.
     */
    private static class PendingUpdate {

        public Kind<Path> kind;
        public Member member;

        public PendingUpdate(Kind<Path> kind, Member member) {
            this.kind = kind;
            this.member = member;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 17 * hash + Objects.hashCode(this.kind);
            hash = 17 * hash + Objects.hashCode(this.member);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            // we don't check null or class since this class is for internal use
            /*
			if( obj == null ) {
				return false;
			}
			if( getClass() != obj.getClass() ) {
				return false;
			}
             */
            final PendingUpdate other = (PendingUpdate) obj;
            if (kind != other.kind) {
                return false;
            }
            return member.equals(other.member);
        }

        @Override
        public String toString() {
            if (kind == null) {
                return "EXPLICIT_SYNCH " + member.getName();
            }
            return kind.name() + ' ' + member.getName();
        }
    }
}
