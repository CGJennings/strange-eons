package ca.cgjennings.apps.arkham;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JPanel;

/**
 * A dummy panel that maintains a link between the {@link TEditorTabPane} and a
 * {@link TAttachedEditor}. The tab in the tab strip has an instance of this
 * class set as the component for the tab that represents a document. Using
 * {@link #getLinkedEditor}, event listeners can forward events for the tab to
 * the linked editor.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class TEditorTabLink extends JPanel {

    private TAttachedEditor frame;

    public TEditorTabLink(TAttachedEditor linkedFrame) {
        setBackground(Color.BLACK);
        setOpaque(true);

        frame = linkedFrame;
        Dimension zero = new Dimension(1, 1);
        setPreferredSize(zero);
        setMinimumSize(zero);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        setSize(zero);
    }

    public TAttachedEditor getLinkedEditor() {
        return frame;
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, 1);
    }

}
