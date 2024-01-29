package ca.cgjennings.ui.textedit;

import ca.cgjennings.ui.IconProvider;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Objects;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * A default renderer for code completions.
 */
public class CompletionRenderer extends DefaultListCellRenderer {
    private DefaultListCellRenderer rhsRenderer = new DefaultListCellRenderer();
    private Font font;
    
    public CompletionRenderer(CodeEditorBase editor) {
        setHorizontalAlignment(JLabel.LEADING);
        setVerticalAlignment(JLabel.CENTER);
        rhsRenderer.setHorizontalAlignment(JLabel.TRAILING);
        rhsRenderer.setVerticalAlignment(JLabel.CENTER);
        font = editor.getTextArea().getFont();
        setFont(font);
        rhsRenderer.setFont(font.deriveFont(Font.ITALIC, font.getSize2D() - 2f));
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setVerticalAlignment(JLabel.CENTER);
        rhsRenderer.setVerticalAlignment(JLabel.CENTER);
        setFont(font);
        if (value instanceof IconProvider) {
            setIcon(((IconProvider) value).getIcon());
        } else {
            setIcon(null);
        }
        if (value instanceof SecondaryTextProvider) {
            setText(Objects.toString(((SecondaryTextProvider) value).getText()));
            rhsRenderer.setText(Objects.toString(((SecondaryTextProvider) value).getSecondaryText()));
        } else {
            setText(Objects.toString(value));
            rhsRenderer.setText(null);
        }
        rhsRenderer.setOpaque(false);
        return this;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!rhsRenderer.getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g;
            Composite oldComp = g2.getComposite();
            g2.setComposite(AlphaComposite.SrcOver.derive(0.8f));
            rhsRenderer.setSize(getSize());
            rhsRenderer.paint(g);
            g2.setComposite(oldComp);
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        Dimension d2 = rhsRenderer.getPreferredSize();
        d.width += d2.width + 16;
        return d;
    }
}
