package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.MarkupTarget;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.ui.textedit.InputHandler;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import ca.cgjennings.ui.textedit.Tokenizer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.KeyStroke;
import resources.AcceleratorTable;

/**
 * A helper class for creating commands that act on source files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class HSourceCommand extends DelegatedCommand {
//	private CodeEditor.CodeType type;

    private boolean comments;
    private boolean codeEditor;
    private ActionListener al;
    private int macroState;

    public HSourceCommand(String nameKey) {
        super(nameKey);
    }

    public HSourceCommand(String nameKey, String iconResource) {
        super(nameKey, iconResource);
    }

    public HSourceCommand(String nameKey, String iconResource, ActionListener editorCommand) {
        super(nameKey, iconResource);
        forAction(editorCommand);
    }

    public HSourceCommand forComments() {
        comments = true;
        return this;
    }

    public HSourceCommand forCodeEditor() {
        codeEditor = true;
        return this;
    }

    public HSourceCommand forAction(ActionListener editorCommand) {
        al = editorCommand;
        return this;
    }

    public HSourceCommand forRecording() {
        macroState = 1;
        return this;
    }

    public HSourceCommand forPlaying() {
        macroState = 2;
        return this;
    }

    public HSourceCommand key(String key) {
        KeyStroke ks = AcceleratorTable.getApplicationTable().get(key);
        if (ks != null) {
            setAccelerator(ks);
        }
        return this;
    }

    @Override
    public void performDefaultAction(ActionEvent e) {
        if (al == null) {
            throw new AssertionError("override me");
        }
        JSourceCodeEditor ed = getEditor();
        InputHandler ih = ed.getInputHandler();
        ih.executeAction(al, ed, null);
    }

    @Override
    public boolean isDefaultActionApplicable() {
        if (codeEditor) {
            return getCodeEditor() != null;
        }

        JSourceCodeEditor ed = getEditor();
        if (ed != null) {
            if (comments) {
                Tokenizer t = ed.getTokenizer();
                if (t == null) {
                    return false;
                }
                String prefix = ed.getTokenizer().getCommentPrefix();
                if (prefix == null || prefix.isEmpty()) {
                    return false;
                }
            }
            if (macroState != 0) {
                InputHandler ih = ed.getInputHandler();
                if (macroState == 1) {
                    return ih.getActionRecorder() != null;
                } else {
                    return ih.isMacroRecorded() || ih.getActionRecorder() != null;
                }
            }
            return true;
        }
        return false;
    }

    protected JSourceCodeEditor getEditor() {
        MarkupTarget mt = StrangeEons.getApplication().getMarkupTarget();
        if (mt != null && mt.getTarget() instanceof JSourceCodeEditor) {
            JSourceCodeEditor jed = (JSourceCodeEditor) mt.getTarget();
            return jed.isEditable() ? jed : null;
        }
        CodeEditor ed = getCodeEditor();
        return ed == null ? null : ed.getEditor();
    }

    protected CodeEditor getCodeEditor() {
        StrangeEonsEditor ed = StrangeEons.getActiveEditor();
        if (ed instanceof CodeEditor) {
            CodeEditor ced = (CodeEditor) ed;
            return ced.getEditor().isEditable() ? ced : null;
        }
        return null;
    }
}
