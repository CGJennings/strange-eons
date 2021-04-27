package ca.cgjennings.ui.anim;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.Timer;

/**
 * A simple framework for basic animation effects in a user interface. This is
 * intended for brief, noninteractive animations. Construct a new instance with
 * the desired time to complete the animation, in seconds. Override
 * {@link #composeFrame} to update the interface to a state between the initial
 * (position=0) and final (position=1) conditions. (Alternatively, use the
 * constructor that takes a {@link FrameComposer} parameter.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Animation implements FrameComposer {

    private float timeToComplete = 0.25f;
    private FrameComposer composer;

    private long startTime;
    private int maxCalls = 20;

    private boolean stopped, playing;

    /**
     * Creates a new animation that calls the built-in compose method and runs
     * for the specified time at a target frame rate of approximately 30 fps.
     *
     * @param timeToComplete the duration of the animation
     * @see #setDuration(float)
     */
    public Animation(float timeToComplete) {
        setDuration(timeToComplete);
        setMaxFrames(Math.max(2, (int) (timeToComplete * 30f + 0.5f)));
        setComposer(this);
    }

    /**
     * Creates a new animation that calls the built-in compose method and runs
     * for the specified time.
     *
     * @param timeToComplete the duration of the animation
     * @param maxFrames a hint describing the maximum number of frames to
     * display
     * @see #setDuration(float)
     * @see #setMaxFrames(int)
     */
    public Animation(float timeToComplete, int maxFrames) {
        setDuration(timeToComplete);
        setMaxFrames(maxFrames);
        setComposer(this);
    }

    /**
     * Creates a new animation that calls the built-in compose method and runs
     * for the specified time.
     *
     * @param timeToComplete the duration of the animation
     * @see #setDuration(float)
     */
    public Animation(float timeToComplete, FrameComposer composer) {
        setDuration(timeToComplete);
        setMaxFrames(Math.max(2, (int) (timeToComplete * 30f + 0.5f)));
        setComposer(composer);
    }

    /**
     * Creates a new animation that calls the specified frame composer and runs
     * for the specified time.
     *
     * @param timeToComplete the duration of the animation
     * @param maxFrames a hint describing the maximum number of frames to
     * display
     * @see #setDuration(float)
     * @see #setMaxFrames(int)
     */
    public Animation(float timeToComplete, int maxFrames, FrameComposer composer) {
        setDuration(timeToComplete);
        setMaxFrames(maxFrames);
        setComposer(composer);
    }

    /**
     * This method is called with a value between 0 (start) and 1 (end) if no
     * other composer has been set on this animation. It should set up the
     * animation state accordingly.
     *
     * @param position the animation position
     */
    @Override
    public void composeFrame(float position) {
    }

    /**
     * Starts playing the animation.
     */
    public void play() {
        if (playing) {
            stop();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (activeRunner == null) {
                        play();
                    } else {
                        EventQueue.invokeLater(this);
                    }
                }
            });
        }

        stopped = false;
        playing = true;
        if (EventQueue.isDispatchThread()) {
            activeRunner = new AnimRunner();
        } else {
            EventQueue.invokeLater(() -> {
                activeRunner = new AnimRunner();
            });
        }
    }

    private AnimRunner activeRunner;

    private class AnimRunner implements ActionListener {

        private Timer timer;
        private boolean stopped;
        private long lastTime;

        public AnimRunner() {
            if (ENABLED) {
                int rate = (int) (timeToComplete * 1000f / maxCalls + 0.5f);
                // limit frame rate to between 1..60 fps
                timer = new Timer(Math.min(1000, Math.max(16, rate)), this);
                getComposer().composeFrame(0f);
                Toolkit.getDefaultToolkit().sync();
                timer.start();
                startTime = System.nanoTime();
                lastTime = startTime;
            } else {
                getComposer().composeFrame(0f);
                getComposer().composeFrame(1f);
                playing = false;
                stopped = true;
                fireFinishAction();
            }
        }

        public void stop() {
            stopped = true;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!stopped) {
                long time = System.nanoTime();

                float position = (time - startTime) * 1e-9f / getDuration();
                if (position >= 1f) {
                    stopped = true;
                    position = 1f;
                }
                getComposer().composeFrame(position);
                Toolkit.getDefaultToolkit().sync();

                // adjust tick rate on slower machines
                int thisTime = (int) ((time - lastTime) / 1000000L);
                lastTime = time;
                if (thisTime > timer.getDelay() * 3 / 2) {
                    timer.setDelay(thisTime);
                    timer.setInitialDelay(thisTime);
                }
            }

            if (stopped) { // true if stopped or position == 1f
                timer.stop();
                playing = false;
                activeRunner = null;
                fireFinishAction();
            }
        }
    }

//	public void play( float start, float end ) {
//		if( start < 0 || start > 1f ) throw new IllegalArgumentException( "start not in 0..1: " + start );
//		if( end < 0 || end > 1f ) throw new IllegalArgumentException( "end not in 0..1: " + end );
//	}
    /**
     * Starts playing the animation after stopping any other animations started
     * with the same interrupt tag. This can be used to ensure that two
     * animations on the same object don't counteract each other. For example,
     * suppose a button darkens when the pointer moves over it and lightens when
     * the pointer moves off again. If the pointer moved quickly over and then
     * off of the button, then the darkening and lightening animations would
     * play overtop of each other. By playing these animations using the same
     * tag, such as the button instance, this effect would be prevented.
     *
     * @param interruptTag the mutual exclusion tag that will mark this
     * playthrough
     */
    public void play(final Object interruptTag) {
        if (interruptTag == null) {
            play();
            return;
        }
        if (playing) {
            stop();
            EventQueue.invokeLater(() -> {
                play(interruptTag);
            });
        }
        tagObject = interruptTag;
        final Animation excluded = tagMap.get(interruptTag);
        if (excluded != null) {
            excluded.addFinishAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    excluded.removeFinishAction(this);
                    play(interruptTag);
                }
            });
            excluded.stop();
            return;
        }
        tagMap.put(interruptTag, this);
        play();
    }
    private Object tagObject;
    private static Map<Object, Animation> tagMap = new HashMap<>();

    /**
     * This method can be called from within {@link #composeFrame} or from
     * elsewhere.
     */
    public void stop() {
        stopped = true;
        if (activeRunner != null) {
            activeRunner.stop();
        }
    }

    /**
     * Returns <code>true</code> if the animation is stopped or stopping.
     *
     * @return <code>true</code> if the animation is or will be stopped
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * Returns <code>true</code> if the animation is still playing. (This will
     * still be <code>true</code> after calling {@link #stop} until the
     * animation actually halts.)
     *
     * @return <code>true</code> if the animation is playing
     */
    public boolean isPlaying() {
        return playing;
    }

//	public static void main( String[] args ) {
//		Animation a = new Animation( 0.5f ) {
//			@Override
//			public void composeFrame( float position ) {
//				System.out.println( position );
//			}
//		};
//		a.setMaxFrames( 5 );
//		a.play();
//	}
    /**
     * Returns the ideal play time for the animation.
     *
     * @return the time for the animation to complete, in seconds
     */
    public float getDuration() {
        return timeToComplete;
    }

    /**
     * Sets the ideal play time for the animation. Actual play time will usually
     * be close to, but not exactly match, this value. Note that the animation
     * will always display positions 0 and 1; if composition is slow this may
     * take significantly longer than the play time.
     *
     * @param timeToComplete the time for the animation to complete, in seconds
     */
    public void setDuration(float timeToComplete) {
        if (timeToComplete <= 0f) {
            throw new IllegalArgumentException("timeToComplete: " + timeToComplete);
        }
        this.timeToComplete = timeToComplete;
    }

    /**
     * Returns the composer that will be used to create frames for this
     * animation.
     *
     * @return the composer
     */
    public FrameComposer getComposer() {
        return composer;
    }

    /**
     * Sets the composer that will be user to create frames for this animation.
     *
     * @param composer the animation composer to set
     */
    public void setComposer(FrameComposer composer) {
        if (composer == null) {
            throw new NullPointerException("composer");
        }
        this.composer = composer;
    }

    /**
     * Returns the limit on the number of frames that will be composed.
     *
     * @return the maxCalls
     */
    public int getMaxFrames() {
        return maxCalls;
    }

    /**
     * Sets a limit on the number of frames that will be composed.
     *
     * @param maxFrames the maximum number of frames
     */
    public void setMaxFrames(int maxFrames) {
        if (maxFrames < 2) {
            throw new IllegalArgumentException("first and last frame are always shown");
        }
        this.maxCalls = maxFrames;
    }

    /**
     * Adds an action listener that will be called when this animation finishes
     * playing or is stopped.
     *
     * @param li the listener to call
     * @throws NullPointerException if the listener is <code>null</code>
     */
    public void addFinishAction(ActionListener li) {
        if (li == null) {
            throw new NullPointerException("li");
        }
        if (listeners == null) {
            listeners = new LinkedHashSet<>();
        }
        listeners.add(li);
    }

    /**
     * Removes a previously added finish action.
     *
     * @param li the listener to remove
     */
    public void removeFinishAction(ActionListener li) {
        if (listeners == null) {
            return;
        }
        listeners.remove(li);
    }

    /**
     * Called when the animation finishes or stops to fire off action events to
     * registered listeners.
     */
    protected void fireFinishAction() {
        if (tagObject != null) {
            tagMap.remove(tagObject);
            tagObject = null;
        }
        if (listeners != null) {
            ActionEvent e = new ActionEvent(this, 0, "FINISH");
            // work on a copy of the list so that listeners can safely remove themselves
            LinkedList<ActionListener> copy = new LinkedList<>(listeners);
            for (ActionListener li : copy) {
                li.actionPerformed(e);
            }
        }
    }

    private LinkedHashSet<ActionListener> listeners;

    static final boolean ENABLED;

    static {
        boolean enable = true;
        String v = System.getProperty("ca.cgjennings.anim.enabled");
        if (v != null && (!v.isEmpty() && Character.toLowerCase(v.charAt(0)) != 't')) {
            enable = false;
        }
        ENABLED = enable;
    }
}
