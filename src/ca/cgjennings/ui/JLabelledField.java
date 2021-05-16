package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.MarkupTargetFactory;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextField;
import javax.swing.text.Document;

/**
 * A quick and dirty labelled text field that shows a message until it gains
 * focus for the first time. Note that getText() will return the label text if
 * it has not gained focus, and text colour changes may not be honoured.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JLabelledField extends JTextField {

    private boolean showingLabel = false;
    private Color trueFG, labelFG, defLabelFG;
    private String label = "";

    public JLabelledField() {
        super();
        init();
    }

    public JLabelledField(Document doc, String text, int columns) {
        super(doc, text, columns);
        init();
    }

    public JLabelledField(String text, int columns) {
        super(text, columns);
        init();
    }

    public JLabelledField(int columns) {
        super(columns);
        init();
    }

    public JLabelledField(String text) {
        super(text);
        init();
    }

    private void init() {
        trueFG = super.getForeground();

        labelFG = getDisabledTextColor();
        labelFG = new Color(
                (trueFG.getRed() + labelFG.getRed() * 2) / 3,
                (trueFG.getGreen() + labelFG.getGreen() * 2) / 3,
                (trueFG.getBlue() + labelFG.getBlue() * 2) / 3
        );
        defLabelFG = labelFG;

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (showingLabel) {
                    showingLabel = false;
                    JLabelledField.super.setText("");
                    setForeground(trueFG);
                }
                selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    showingLabel = true;
                    JLabelledField.super.setText(label);
                    select(0, 0);
                    setForeground(labelFG);
                }
            }
        });

        setForeground(labelFG);
        showingLabel = true;

        MarkupTargetFactory.enableTargeting(this, false);
    }

    /**
     * {@inheritDoc} If the label is currently showing, then the text of the
     * field is the empty string.
     *
     * @return the text
     */
    @Override
    public String getText() {
        if (showingLabel) {
            return "";
        } else {
            return super.getText();
        }
    }

    @Override
    public void setText(String t) {
        if (showingLabel) {
            showingLabel = false;
            setForeground(trueFG);
        }
        super.setText(t);
    }

    /**
     * Returns the current label text.
     *
     * @return the text to display when the label is showing
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the text to use when displaying the field label.
     *
     * @param label the text to display when the label is showing
     */
    public void setLabel(String label) {
        this.label = label;
        if (showingLabel) {
            super.setText(label);
            select(0, 0);
        }
    }

    /**
     * Returns {@code true} if the label is currently being displayed. The
     * label is displayed when the text field contains the empty string and the
     * field does not have focus.
     *
     * @return {@code true} if the label text is showing
     */
    public boolean isLabelShowing() {
        return !getText().equals(label) && showingLabel;
    }

    /**
     * Sets the foreground colour used for editing text.
     *
     * @param fg
     */
    public void setTextForeground(Color fg) {
        trueFG = fg;
        if (!showingLabel) {
            setForeground(fg);
        }
    }

    /**
     * Returns the foreground colour used for editing text.
     */
    public Color getTextForeground() {
        return trueFG;
    }

    /**
     * Returns the colour used to draw the label text.
     *
     * @return the label foreground colour
     */
    public Color getLabelForeground() {
        return labelFG;
    }

    /**
     * Sets the foreground colour used when drawing the label.
     *
     * @param labelFG the new label foreground colour
     */
    public void setLabelForeground(Color labelFG) {
        if (labelFG == null) {
            labelFG = defLabelFG;
        }

        this.labelFG = labelFG;
        if (showingLabel) {
            setForeground(labelFG);
        }
    }
}
