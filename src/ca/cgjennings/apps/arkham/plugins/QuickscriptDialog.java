package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.ContextBar;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.editors.AbbreviationTableManager;
import ca.cgjennings.apps.arkham.editors.CodeEditor.CodeType;
import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.textedit.EditorCommands;
import ca.cgjennings.ui.textedit.InputHandler;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import static resources.Language.string;
import resources.Settings;

/**
 * Dialog that displays the editor for {@link QuickscriptPlugin}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
@SuppressWarnings("serial")
final class QuickscriptDialog extends javax.swing.JDialog {
    /**
     * Creates a new Quickscript dialog
     */
    public QuickscriptDialog() {
        super(StrangeEons.getWindow(), false);
        initComponents();
        if (!ScriptDebugging.isInstalled()) {
            debugBtn.setEnabled(false);
            debugBtn.setVisible(false);
        }

        editor.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                isModified = true;
            }
        });

        if (!Settings.getUser().applyWindowSettings("quickscript", this)) {
            Rectangle r = getParent().getBounds();
            r.x += 32;
            r.y += 96;
            r.width = r.width / 2 < 128 ? 128 : r.width / 2 - 64;
            r.height = r.height < 128 ? 128 : r.height - 128;
            setBounds(r);
        }

        ActionListener runAction = (ActionEvent e) -> {
            runBtnActionPerformed(null);
        };

        ActionListener debugAction = (ActionEvent e) -> {
            debugBtnActionPerformed(null);
        };

        InputHandler ih = new InputHandler();
        ih.addDefaultKeyBindings();
        ih.addKeyBinding("P+R", runAction);
        ih.addKeyBinding("C+R", runAction);
        ih.addKeyBinding("A+R", runAction);
        ih.addKeyBinding("F5", runAction);

        ih.addKeyBinding("P+D", debugAction);
        ih.addKeyBinding("C+D", debugAction);
        ih.addKeyBinding("A+D", debugAction);
        ih.addKeyBinding("F3", debugAction);

        ih.addKeyBinding("S+DELETE", EditorCommands.CUT);
        ih.addKeyBinding("C+INSERT", EditorCommands.COPY);
        ih.addKeyBinding("S+INSERT", EditorCommands.PASTE);

        ih.addKeyBinding("ESCAPE", (ActionEvent e) -> {
            dispose();
        });
        editor.setInputHandler(ih);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem item = new JMenuItem(Commands.RUN_FILE.getName(), Commands.RUN_FILE.getIcon());
        item.addActionListener(runAction);
        popup.add(item);
        if (ScriptDebugging.isInstalled()) {
            item = new JMenuItem(Commands.DEBUG_FILE.getName(), Commands.DEBUG_FILE.getIcon());
            item.addActionListener(debugAction);
            popup.add(item);
        }
        popup.addSeparator();
        popup.add(Commands.CUT);
        popup.add(Commands.COPY);
        popup.add(Commands.PASTE);
        popup.addSeparator();
        popup.add(Commands.SELECT_ALL);
        editor.setComponentPopupMenu(popup);

        editor.setAbbreviationTable(AbbreviationTableManager.getTable(CodeType.JAVASCRIPT));

        clearCheck.setSelected(Settings.getUser().getYesNo(ScriptMonkey.CLEAR_CONSOLE_ON_RUN_KEY));

        String recoveredCode = getRecoveredCode();
        if (recoveredCode != null) {
            StrangeEons.log.info("recovered script");
            editor.setText(recoveredCode);
            editor.select(0, 0);
        }

        editor.addPropertyChangeListener(JSourceCodeEditor.FILE_DROP_PROPERTY, (PropertyChangeEvent evt) -> {
            List files = (List) evt.getNewValue();
            if (files.isEmpty()) {
                return;
            }
            File f = (File) files.get(0);
            if (JOptionPane.YES_OPTION
                    == JOptionPane.showConfirmDialog(StrangeEons.getWindow(), string("fd-confirm", ""), "", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
                try {
                    String code = readScriptFile(f);
                    editor.setText(code);
                    editor.select(0, 0);
                } catch (IOException e) {
                    ErrorDialog.displayError(string("app-err-open", f.getName()), e);
                }
            }
        });

        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                StrangeEons.getApplication().requestNewMarkupTarget(this);
            }
        });

        editor.putClientProperty(ContextBar.BAR_LEADING_SIDE_PROPERTY, Boolean.TRUE);
        editor.putClientProperty(ContextBar.BAR_OFFSET_PROPERTY, new Point(20, 0));

        initSyntaxChecker();
    }

    private void initSyntaxChecker() {
        editor.addHighlighter(errorHighlighter);
        editor.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                isEditing = true;
            }
        });
    }

    private void startSyntaxChecking() {
        if (syntaxCheckTimer == null) {
            syntaxCheckTimer = new Timer(500, (ActionEvent e) -> {
                checkSyntax();
            });
            syntaxCheckTimer.start();
        }
    }

    private void stopSyntaxChecking() {
        if (syntaxCheckTimer != null) {
            syntaxCheckTimer.stop();
            syntaxCheckTimer = null;
        }
    }

    private Timer syntaxCheckTimer;
    SyntaxChecker checker = new SyntaxChecker();
    SyntaxChecker.Highlighter errorHighlighter = new SyntaxChecker.Highlighter();

    private boolean isEditing;
    private boolean isModified;

    private void checkSyntax() {
        // as long as the user is typing, don't change the error state
        if (isEditing) {
            isEditing = false;
            return;
        }
        if (!isModified) {
            return;
        }
        isModified = false;

        String scriptToCheck = editor.getText();
        checker.parse(scriptToCheck);
        final SyntaxChecker.SyntaxError[] errors = checker.getErrors();
        errorHighlighter.update(editor, errors);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        editorPopup = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem runItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator sep1 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem cutItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem pasteItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator sep2 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem selectAll = new javax.swing.JMenuItem();
        runBtn = new javax.swing.JButton();
        StyleUtilities.small( runBtn );
        editor = new ca.cgjennings.ui.textedit.JSourceCodeEditor();
        clearCheck = new javax.swing.JCheckBox();
        StyleUtilities.small( clearCheck );
        debugBtn = new javax.swing.JButton();
        StyleUtilities.small( debugBtn );

        runItem.setAction( Commands.RUN_FILE );
        editorPopup.add(runItem);
        editorPopup.add(sep1);

        cutItem.setAction( Commands.CUT );
        editorPopup.add(cutItem);

        copyItem.setAction( Commands.COPY );
        editorPopup.add(copyItem);

        pasteItem.setAction( Commands.PASTE );
        editorPopup.add(pasteItem);
        editorPopup.add(sep2);

        selectAll.setAction( Commands.SELECT_ALL );
        editorPopup.add(selectAll);

        setTitle(string("qs-title")); // NOI18N

        runBtn.setMnemonic('r');
        runBtn.setText(string("qs-run")); // NOI18N
        runBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runBtnActionPerformed(evt);
            }
        });

        editor.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        editor.setComponentPopupMenu( editorPopup );
        editor.setText("println( \"Hello, Other World!\" );");
        editor.selectAll();
        editor.setTokenizer( new ca.cgjennings.ui.textedit.tokenizers.JavaScriptTokenizer() );

        clearCheck.setFont(clearCheck.getFont());
        clearCheck.setText(string("qs-clear-on-run")); // NOI18N
        clearCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearCheckActionPerformed(evt);
            }
        });

        debugBtn.setMnemonic('d');
        debugBtn.setText(string("qs-debug")); // NOI18N
        debugBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(clearCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 64, Short.MAX_VALUE)
                .addComponent(debugBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(runBtn)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {debugBtn, runBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(runBtn)
                    .addComponent(debugBtn)
                    .addComponent(clearCheck))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private static boolean weOwnRecoveryFile = false;

    private static final File RECOVERY_FILE = StrangeEons.getUserStorageFile("quickscript");

    public void run(boolean debug) {
        if (debug) {
            debugBtnActionPerformed(null);
        } else {
            runBtnActionPerformed(null);
        }
    }

    public JSourceCodeEditor getEditor() {
        return editor;
    }

    public static String readScriptFile(File f) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
            int buff = (int) f.length();
            if (buff <= 0) {
                buff = 2_048;
            }
            StringBuilder b = new StringBuilder(buff);
            String line;
            while ((line = in.readLine()) != null) {
                if (b.length() > 0) {
                    b.append('\n');
                }
                b.append(line);
            }
            return b.toString();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static String getRecoveredCode() {
        if (!RECOVERY_FILE.exists()) {
            return null;
        }
        try {
            return readScriptFile(RECOVERY_FILE);
        } catch (IOException e) {
            StrangeEons.log.log(Level.WARNING, "failed to read Quickscript recovery file", e);
        }
        return null;
    }

    /**
     * This is intended to be called as a shutdown hook to delete the
     * Quickscript automatic recovery file when the program terminates normally.
     * If we generated a recovery file this session and the program setting to
     * keep the recovery file is not set to true, then an attempt will be made
     * to delete the file.
     */
    public static void deleteRecoveredCode() {
        if (weOwnRecoveryFile && !Settings.getShared().getYesNo(KEEP_RECOVERY_FILE)) {
            RECOVERY_FILE.delete();
        }
    }

    private void writeRecoveryFile(String code) {
        // try to write code to a recovery file in case we lock up
        Writer out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(RECOVERY_FILE), "utf-8");
            out.write(code);
            // if we just wrote the very first recovery file, add an exit
            // task to delete it at shutdown
            if (!weOwnRecoveryFile) {
                weOwnRecoveryFile = true;
                StrangeEons.getApplication().addExitTask(new Runnable() {
                    @Override
                    public void run() {
                        QuickscriptDialog.deleteRecoveredCode();
                    }

                    @Override
                    public String toString() {
                        return "delete Quickscript crash recovery data";
                    }
                });
            }
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "failed to write Quickscript recovery file", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private boolean debugRun;

	private void runBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runBtnActionPerformed
            StrangeEons.setWaitCursor(true);
            getGlassPane().setVisible(true);
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                ScriptConsole console = ScriptMonkey.getSharedConsole();
                if (clearCheck.isSelected()) {
                    console.clear();
                }
                runBtn.setEnabled(false);
                String code = editor.getText();
                writeRecoveryFile(code);
                monkey = new ScriptMonkey(string("qs-title"));

                int modifiers = 0;
                if (evt != null) {
                    modifiers = evt.getModifiers();
                }

                PluginContext context = PluginContextFactory.createDummyContext(modifiers);
                monkey.bind(context);
                monkey.setBreakpoint(debugRun);
                Object result = monkey.eval(code);
                if (result != null) {
                    ScriptConsole.ConsolePrintWriter writer = console.getWriter();
                    writer.print("⤷ ");
                    writer.print(result);
                    writer.println();
                    writer.flush();
                }
            } finally {
                runBtn.setEnabled(true);
                monkey = null;
                getGlassPane().setVisible(false);
                getGlassPane().setCursor(Cursor.getDefaultCursor());
                StrangeEons.setWaitCursor(false);
                editor.requestFocusInWindow();
            }
	}//GEN-LAST:event_runBtnActionPerformed

private void clearCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearCheckActionPerformed
    Settings.getUser().set(ScriptMonkey.CLEAR_CONSOLE_ON_RUN_KEY, clearCheck.isSelected() ? "yes" : "no");
}//GEN-LAST:event_clearCheckActionPerformed

private void debugBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugBtnActionPerformed
    debugRun = true;
    try {
        runBtnActionPerformed(evt);
    } finally {
        debugRun = false;
    }
}//GEN-LAST:event_debugBtnActionPerformed

    private static final String KEEP_RECOVERY_FILE = "keep-quickscript";

    private ScriptMonkey monkey;

    public ScriptMonkey getScriptMonkey() {
        return monkey;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            startSyntaxChecking();
        } else {
            Settings.getUser().storeWindowSettings("quickscript", this);
            if (Settings.getShared().getYesNo(KEEP_RECOVERY_FILE)) {
                writeRecoveryFile(editor.getText());
            }
            stopSyntaxChecking();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox clearCheck;
    private javax.swing.JButton debugBtn;
    private ca.cgjennings.ui.textedit.JSourceCodeEditor editor;
    private javax.swing.JPopupMenu editorPopup;
    private javax.swing.JButton runBtn;
    // End of variables declaration//GEN-END:variables
}
