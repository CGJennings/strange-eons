package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.MarkupTarget;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.ui.textedit.CodeEditorBase;
import ca.cgjennings.ui.textedit.InputHandler;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
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

    private boolean comments;
    private boolean forEditorTab;
    private String forAction;

    public HSourceCommand(String nameKey) {
        super(nameKey);
    }

    public HSourceCommand(String nameKey, String iconResource) {
        super(nameKey, iconResource);
    }

    public HSourceCommand(String nameKey, String iconResource, String forAction) {
        super(nameKey, iconResource);
        this.forAction = forAction;
    }

    public HSourceCommand forComments() {
        comments = true;
        return this;
    }

    public HSourceCommand forCodeEditor() {
        forEditorTab = true;
        return this;
    }

    public HSourceCommand forAction(String forAction) {
        this.forAction = forAction;
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
        if (forAction == null) {
            throw new AssertionError("override me");
        }
        CodeEditorBase ed = getEditor();
        ed.performAction(forAction);
    }

    @Override
    public boolean isDefaultActionApplicable() {
        CodeEditorBase ed;
        if (forEditorTab) {
            CodeEditor tab = getCodeEditor();
            if (tab == null) {
                return false;
            }
            ed = tab.getEditor();
        } else {
            ed = getEditor();
        }
        if (ed == null) {
            return false;
        }
        
        if (forAction != null) {
            return ed.canPerformAction(forAction);
        }
        
        return true;
    }

    protected CodeEditorBase getEditor() {
        MarkupTarget mt = StrangeEons.getApplication().getMarkupTarget();
        if (mt != null && mt.getTarget() instanceof CodeEditorBase) {
            CodeEditorBase jed = (CodeEditorBase) mt.getTarget();
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
