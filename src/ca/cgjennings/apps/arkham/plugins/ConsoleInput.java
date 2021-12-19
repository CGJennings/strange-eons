package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.theme.Theme;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

/**
 * Adds a simple input field to a script console to evaluate script code. Press
 * Escape to toggle closed.
 */
public final class ConsoleInput extends javax.swing.JPanel {

    /**
     * Creates a new ConsoleInput
     */
    public ConsoleInput(ScriptConsole console) {
        con = console;
        initComponents();

        final boolean dark = ThemeInstaller.isDark();
        Color bg = dark ? Color.BLACK : Color.WHITE;
        Color fg = dark ? Color.WHITE : Color.BLACK;
        setBackground(bg);
        inputField.setBackground(bg);
        inputField.setForeground(fg);
        prompt.setBackground(bg);
        prompt.setForeground(fg);

        Font conFont = UIManager.getDefaults().getFont(Theme.CONSOLE_FONT);
        inputField.setFont(conFont);
        prompt.setFont(conFont);

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (historyIndex > 0) {
                        if (historyIndex == history.size()) {
                            inputBuff = inputField.getText();
                        }
                        inputField.setText(history.get(--historyIndex));
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    final int size = history.size();
                    if (historyIndex < size) {
                        String newInput;
                        if (++historyIndex == size) {
                            newInput = inputBuff;
                        } else {
                            newInput = history.get(historyIndex);
                        }
                        inputField.setText(newInput);
                    }
                    e.consume();
                }
            }
        });
    }

    private ScriptConsole con;
    private ScriptMonkey m;
    private boolean isVisible = false;
    private boolean installed = false;
    private List<String> history = new LinkedList<>();
    private int historyIndex = 0;
    private String inputBuff = "";

    private static final String TOGGLE_NAME = "toggleInput";

    private final Action TOGGLE_ACTION = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            setConsoleInputVisible(!isVisible);
        }
    };

    private final KeyStroke TOGGLE_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    public void setConsoleInputVisible(boolean visible) {
        if (visible != isVisible) {
            isVisible = visible;
            if (visible) {
                if (!installed) {
                    installed = true;
                    if (con == null) {
                        con = ScriptMonkey.getSharedConsole();
                    }
                    con.getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW)
                            .put(TOGGLE_KEY, TOGGLE_NAME);
                    con.getRootPane().getActionMap().put(TOGGLE_NAME, TOGGLE_ACTION);
                }
                con.getBodyPanel().add(this, BorderLayout.SOUTH);
            } else {
                con.getBodyPanel().remove(this);
            }
            con.validate();
        }
    }

    public boolean isConsoleInputVisible() {
        return isVisible;
    }

    public void dispose() {
        if (installed) {
            setConsoleInputVisible(false);
            con.getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW).remove(TOGGLE_KEY);
            con.getRootPane().getActionMap().remove(TOGGLE_NAME);
            con = null;
            installed = false;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        prompt = new javax.swing.JLabel();
        inputField = new javax.swing.JTextField();

        setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.darkGray));
        setLayout(new java.awt.BorderLayout());

        prompt.setText("> ");
        prompt.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 0));
        prompt.setOpaque(true);
        add(prompt, java.awt.BorderLayout.LINE_START);

        inputField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        inputField.setOpaque(true);
        inputField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputFieldActionPerformed(evt);
            }
        });
        add(inputField, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void inputFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputFieldActionPerformed
        String code = inputField.getText().trim();
        inputField.setText(null);
        if (code.isEmpty()) {
            return;
        }

        history.add(code);
        historyIndex = history.size();
        try {
            if (m == null) {
                m = new ScriptMonkey("console");
            }
            con.getWriter().println("> " + code);
            m.bind("last", code);
            Object retval = m.eval("println('⤷ '+String(eval(last)));");
            if (retval != null) {
                con.getWriter().printObj(retval);
                con.getWriter().println();
            }
        } catch (Exception ex) {
            StrangeEons.log.log(Level.WARNING, "exception running console input", ex);
        }
    }//GEN-LAST:event_inputFieldActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField inputField;
    private javax.swing.JLabel prompt;
    // End of variables declaration//GEN-END:variables
}
