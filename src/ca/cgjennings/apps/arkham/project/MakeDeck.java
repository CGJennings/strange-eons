package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.Deck;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Task action that automatically lays out a deck of cards.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @see DeckPacker
 * @since 2.1
 */
public class MakeDeck extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-makedeck");
    }

    @Override
    public String getDescription() {
        return string("pa-makedeck-tt");
    }

    @Override
    public boolean perform(final Project project, final Task task, final Member member) {
        final MakeDeckDialog d = new MakeDeckDialog(StrangeEons.getWindow(), true);
        project.getView().moveToLocusOfAttention(d);
        final DeckPacker packer = d.showDialog();
        if (packer == null) {
            return false;
        }

        new BusyDialog(StrangeEons.getWindow(), string("pa-makedeck-busy"), () -> {
            try {
                CopiesList copies;
                try {
                    copies = new CopiesList(task);
                } catch (IOException e) {
                    copies = new CopiesList();
                    StrangeEons.log.log(Level.WARNING, "unable to read copies list, using card count of 1 for all files", e);
                }
                File deckFile = new File(task.getFile(), d.getSelectedFileName());
                List<Member> list = ProjectUtilities.listMatchingMembers(task, true, "eon");
                for (Member m : list) {
                    if (m == null) {
                        NullPointerException npe = new NullPointerException("Warning: null member");
                        npe.fillInStackTrace();
                        npe.printStackTrace();
                        continue;
                    }
                    if (deckFile.equals(m.getFile())) {
                        continue;
                    }
                    BusyDialog.getCurrentDialog().setStatusText(string("pa-makedeck-busy-read", m.getFile().getName()));
                    packer.add(m.getFile(), copies.getCopyCount(m.getFile()));
                }

                Deck deck = packer.createLayout();
                try {
                    if (!packer.isCancelled()) {
                        deck.setSaveFileHint(deckFile);
                        ResourceKit.writeGameComponentToFile(deckFile, deck);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (CancellationException cancel) {
                // user cancelled the operation
            }
        }, (ActionEvent e) -> {
            packer.cancel();
        });
        task.synchronize();
        return true;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        if (member != null || task == null) {
            return false;
        }
        String type = task.getSettings().get(Task.KEY_TYPE);
        if (NewTaskType.DECK_TYPE.equals(type)) {
            return true;
        }
        return NewTaskType.FACTORY_TYPE.equals(type);
    }

}
