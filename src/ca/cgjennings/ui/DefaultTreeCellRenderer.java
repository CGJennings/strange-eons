package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.UIManager;

/**
 * An alternative base class for custom tree cell renderers that renders the
 * focused cell correctly under the Nimbus look and feel.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class DefaultTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {

    public DefaultTreeCellRenderer() {
        if (nimbus) {
            fix();
        }
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (nimbus) {
            fix();
        }
        return this;
    }

    private void fix() {
        setBackgroundSelectionColor(invis);
        setBackgroundNonSelectionColor(invis);
        setBackground(null);
    }

    private final boolean nimbus = UIManager.getLookAndFeel().getName().equals("Nimbus");
    private final Color invis = new Color(0, true);
}
