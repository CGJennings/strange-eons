package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.StrangeEons;
import gamedata.Game;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import resources.ResourceKit;
import resources.Settings;

/**
 * A dropdown list that allows lists of options to be filtered by search term or
 * by game.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JGameFilterField extends JComboBox<Game> {

    public JGameFilterField() {
        init();
    }

    public JGameFilterField(Game[] items) {
        super(items);
        init();
    }

    public JGameFilterField(ComboBoxModel<Game> aModel) {
        super(aModel);
        init();
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        Border b;
        if (icon == null) {
            b = intBorder;
        } else {
            final Insets iconInsets = new Insets(0, icon.getIconWidth() + 12, 0, 0);
            b = new AbstractBorder() {
                @Override
                public Insets getBorderInsets(Component c) {
                    return iconInsets;
                }

                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    final Icon icon = JGameFilterField.this.icon;
                    int pos = (height - icon.getIconHeight()) / 2;
                    icon.paintIcon(c, g, x + 6, pos + y);
                }
            };
        }
        ((JComponent) getEditor().getEditorComponent()).setBorder(b);
    }

    public Icon getIcon() {
        return icon;
    }

    private void init() {
        try {
            setEditable(true);
            intBorder = ((JComponent) getEditor().getEditorComponent()).getBorder();
            setIcon(ResourceKit.getIcon("ui/find-sm.png"));
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Game) {
                        setIcon(((Game) value).getIcon());
                    } else {
                        setIcon(null);
                    }
                    return this;
                }
            });
            setModel(new DefaultComboBoxModel<>(Game.getGames(false)));
            setSelectedItem(Settings.getUser().get(KEY, ""));

            // add listeners after default value set so that events are not fired during init
            try {
                ((JTextComponent) getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentEventAdapter() {
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        fireFilterChangedEvent();
                    }
                });
            } catch (Exception e) {
                // in some L&Fs, might not be a JTextComponent
                // the action listener will still catch these
            }
            addActionListener((ActionEvent e) -> {
                fireFilterChangedEvent();
            });
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "failed to populate filter", e);
            setModel(new DefaultComboBoxModel<>());
        }
    }
    private Icon icon;
    private Border intBorder;

    protected void fireFilterChangedEvent() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == FilteredListModel.FilterChangeListener.class) {
                ((FilteredListModel.FilterChangeListener) listeners[i + 1]).filterChanged(this);
            }
        }
    }

    public void addFilterChangedListener(FilteredListModel.FilterChangeListener l) {
        listenerList.add(FilteredListModel.FilterChangeListener.class, l);
    }

    public void removeFilterChangedListener(FilteredListModel.FilterChangeListener l) {
        listenerList.remove(FilteredListModel.FilterChangeListener.class, l);
    }

    /**
     * Returns the current filter value. This is either a {@link Game}, or a
     * filtering {@code String}, or {@code null} if the filter value is empty.
     *
     * @return a filter value based on the current selection
     */
    public Object getFilterValue() {
        Object o;
        try {
            o = ((JTextComponent) getEditor().getEditorComponent()).getText();
        } catch (Exception e) {
            o = getSelectedItem();
        }
        if (o == null) {
            return null;
        }
        String v = o.toString().trim();
        if (v.isEmpty()) {
            return null;
        }
        DefaultComboBoxModel<Game> m = (DefaultComboBoxModel<Game>) getModel();
        for (int i = 0; i < m.getSize(); ++i) {
            String t = ((Game) m.getElementAt(i)).getUIName().trim();
            if (t.equalsIgnoreCase(v)) {
                return m.getElementAt(i);
            }
        }
        return v;
    }

    private static final String KEY = "component-filter";
}
