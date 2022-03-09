package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.HSBPanel;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.Tintable;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.ui.textedit.CodeEditorBase;
import ca.cgjennings.ui.textedit.CodeType;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import resources.Language;

/**
 * Helper methods and classes used to support implementation of the UI script
 * libraries (<a href='scriptdoc:uicontrols'>uicontrols</a>,
 * <a href='scriptdoc:uilayout'>uilayout</a>, and
 * <a href='scriptdoc:uibindings'>uibindings</a>).
 *
 * This class is public only to allow access from script code; there is no need
 * for regular code to refer to this class.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public final class UILibraryHelper {

    private UILibraryHelper() {
    }

    /**
     * Support class used to implement the binding of {@link HSBPanel}s to tint
     * settings in the <a href='scriptdoc:uibindings'>uibindings</a>
     * library.
     */
    public static class TintableBinding implements Tintable {

        public float h, s, b;
        private final Runnable callAfterWrite;

        public TintableBinding(Runnable callAfterWrite) {
            this.callAfterWrite = callAfterWrite;
        }

        @Override
        public float[] getTint() {
            return new float[]{h, s, b};
        }

        @Override
        public void setTint(float hueShift, float saturation, float brightness) {
            h = hueShift;
            s = saturation;
            b = brightness;
            callAfterWrite.run();
        }
    }

    /**
     * Support method used to help implement container layout for the
     * <a href='scriptdoc:uilayout#StackcontrolAbstractContainer'>Stack</a>
     * layout container.
     *
     * @param content the objects to stack
     * @return a panel containing the layout
     */
    public static JPanel createStack(Object[] content) {
        if (sharedUI == null) {
            sharedUI = new OptionPaneUI();
            sharedUI.installUI(new JOptionPane(""));
        }
        JPanel body = new JPanel(new GridBagLayout());
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = cons.gridy = 0;
        cons.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        cons.gridheight = 1;
        cons.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        cons.insets = new java.awt.Insets(0, 0, 3, 0);

        sharedUI.createMessageArea(body, cons, content);
        return body;
    }

    public static JOptionPane createPane(Object[] content, Icon icon) {
        JOptionPane pane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon);
        pane.setUI(new OptionPaneUI());
        return pane;
    }

    private static OptionPaneUI sharedUI;

    private static class OptionPaneUI extends BasicOptionPaneUI {

        public void createMessageArea(JPanel body, GridBagConstraints cons, Object[] content) {
            addMessageComponents(body, cons, content, 60, true);
        }

        @Override
        protected int getMaxCharactersPerLineCount() {
            return 65;
        }

        @Override
        protected void addMessageComponents(Container container, GridBagConstraints cons, Object msg, int maxll, boolean internallyCreated) {
            if (msg == null) {
                return;
            }
            if (msg instanceof CodeArea) {
                cons.fill = GridBagConstraints.BOTH;
                cons.weightx = cons.weighty = 1d;
                container.add((Component) msg, cons);
                cons.weightx = cons.weighty = 0;
                cons.fill = GridBagConstraints.NONE;
                cons.gridy++;
                if (!internallyCreated) {
                    hasCustomComponents = true;
                }
            } else if (msg instanceof JPanel && ((JPanel) msg).getLayout() instanceof FlowLayout) {
                // this prevents Row objects from having vertical fill
                cons.fill = GridBagConstraints.HORIZONTAL;
                cons.weightx = 1d;
                container.add((Component) msg, cons);
                cons.weightx = 0;
                cons.fill = GridBagConstraints.NONE;
                cons.gridy++;
                if (!internallyCreated) {
                    hasCustomComponents = true;
                }
            } else {
                super.addMessageComponents(container, cons, msg, maxll, internallyCreated);
            }
        }
    }

    /**
     * Creates a new control that can be used to edit script content. Used by
     * the <a href='scriptdoc:uicontrols'>uicontrols</a> library.
     *
     * @return a new script editing component
     */
    public static JComponent createCodeArea() {
        return new CodeArea();
    }

    @SuppressWarnings("serial")
    public static class CodeArea extends CodeEditorBase {

        CodeArea() {
            setCodeType(CodeType.JAVASCRIPT);
            setContentFeedbackVisible(false);

            final ActionListener runAction = (ev) -> {
                if (isExecutable()) {
                    execute();
                }
            };

            addKeyBinding("A+R", runAction);
            addKeyBinding("F5", runAction);

            setPopupMenuBuilder(new PopupMenuBuilder() {
                @Override
                public JPopupMenu buildMenu(CodeEditorBase editor, JPopupMenu menu) {
                    if (isExecutable()) {
                        JMenuItem item = new JMenuItem(Commands.RUN_FILE.getName(), Commands.RUN_FILE.getIcon());
                        item.addActionListener(runAction);
                        menu.insert(item, 0);
                        menu.insert(new JSeparator(), 1);
                    }
                    return menu;
                }
            });
        }

        private boolean executable = true;

        /**
         * Returns whether the editor will support running its contents when the
         * code type is {@code JAVASCRIPT}.
         *
         * @return if true, users can run code directly from the control; always
         * returns false if code type is not {@code JAVASCRIPT}
         * @see #setCodeType(ca.cgjennings.ui.textedit.CodeType)
         * @see #execute
         */
        public boolean isExecutable() {
            return executable && getCodeType() == CodeType.JAVASCRIPT;
        }

        /**
         * Sets whether the editor will support running its contents when the
         * code type is {@code JAVASCRIPT}.
         *
         * @return if true, users can run code directly from the control
         */
        public void setExecutable(boolean executable) {
            this.executable = executable;
        }

        /**
         * Runs the code in the editor. Does nothing if the control is
         * {@linkplain #isExecutable() not executable}.
         *
         * @return the result of evaluating the code
         * @throws IllegalState
         */
        public Object execute() {
            if (!isExecutable()) {
                String why;
                if (getCodeType() != CodeType.JAVASCRIPT) {
                    why = "code type is " + getCodeType();
                } else {
                    why = "executable property is false";
                }
                throw new IllegalStateException(why);
            }
            return execute(getText());
        }

        /**
         * Runs arbitrary script code.
         *
         * @param code the code to run
         * @return returns the result of evaluating the code
         */
        public static Object execute(String code) {
            Object result = null;
            StrangeEons.setWaitCursor(true);
            try {
                ScriptMonkey monkey = new ScriptMonkey(Language.string("qs-title"));
                PluginContext context = PluginContextFactory.createDummyContext();
                monkey.bind(context);
                result = monkey.eval(code);
            } finally {
                StrangeEons.setWaitCursor(false);
            }
            return result;
        }
    }

    /**
     * A button group that supports including the group in a
     * <a href='scriptdoc:uibindings'>Bindings</a> script object.
     */
    @SuppressWarnings("serial")
    public static class BindableGroup extends ButtonGroup {

        private String[] values;

        /**
         * Creates a new button group that supports binding to setting values.
         *
         * @param buttons the buttons to include in the group
         * @param values the setting value to bind to each group member
         */
        public BindableGroup(AbstractButton[] buttons, String[] values) {
            if (buttons == null) {
                throw new NullPointerException("buttons");
            }
            if (buttons.length == 0) {
                throw new IllegalArgumentException("no buttons were specified");
            }
            for (int i = 0; i < buttons.length; ++i) {
                if (buttons[i] == null) {
                    throw new NullPointerException("buttons[" + i + ']');
                }
            }
            if (values == null) {
                throw new NullPointerException("values");
            }
            for (int i = 0; i < values.length; ++i) {
                if (values[i] == null) {
                    throw new NullPointerException("values[" + i + ']');
                }
            }
            if (buttons.length != values.length) {
                throw new IllegalArgumentException("number of values does not match number of buttons");
            }

            ActionListener clickListener = (ActionEvent e) -> {
                fireActionPerformed(toSetting());
            };

            this.values = values;
            for (AbstractButton b : buttons) {
                super.add(b);
                b.addActionListener(clickListener);
            }
        }

        @Override
        public void add(AbstractButton b) {
            throw new UnsupportedOperationException("button set cannot be changed after construction");
        }

        @Override
        public void remove(AbstractButton b) {
            throw new UnsupportedOperationException("button set cannot be changed after construction");
        }

        /**
         * Selects the button in the group that is mapped to the specified
         * setting value.
         *
         * @param v the value of the button to select
         * @throws IllegalArgumentException if the value does not map to any
         * button in the group
         */
        public void fromSetting(String v) {
            for (int i = 0; i < values.length; ++i) {
                if (values[i].equals(v)) {
                    buttons.get(i).setSelected(true);
                    return;
                }
            }
            throw new IllegalArgumentException("unknown setting value: " + v);
        }

        /**
         * Returns the setting value mapped to by the selected button.
         *
         * @return the value for the selected button
         * @throws IllegalStateException if no button in the group is selected
         */
        public String toSetting() {
            for (int i = 0; i < values.length; ++i) {
                if (buttons.get(i).isSelected()) {
                    return values[i];
                }
            }
            throw new IllegalStateException("no button in group is selected");
        }

        private final EventListenerList listenerList = new EventListenerList();

        /**
         * Adds a listener for changes to the selected button. This is only
         * fired if the user selects a button, not if the button is selected by
         * calling {@code setSelected(true)}. To simulate the user clicking a
         * button, call its {@code doClick()} method. The action event that is
         * generated will have this as its source and the command string will be
         * equal to the setting value of the selected button.
         *
         * @param l the listener to add
         */
        public void addActionListener(ActionListener l) {
            listenerList.add(ActionListener.class, l);
        }

        /**
         * Removes a registered action listener.
         *
         * @param l the listener to remove
         */
        public void removeActionListener(ActionListener l) {
            listenerList.remove(ActionListener.class, l);
        }

        /**
         * Fires an action event to all listeners that indicates that the button
         * with the given value was selected.
         *
         * @param value the setting value of the selected button
         */
        protected void fireActionPerformed(String value) {
            Object[] listeners = listenerList.getListenerList();
            if (listeners.length == 0) {
                return;
            }

            ActionEvent evt = new ActionEvent(this, 0, value);

            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == ActionListener.class) {
                    ((ActionListener) listeners[i + 1]).actionPerformed(evt);
                }
            }
        }
    }
}
