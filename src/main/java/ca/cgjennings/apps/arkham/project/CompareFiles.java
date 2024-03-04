package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.algo.Diff;
import ca.cgjennings.algo.DiffListener;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import static resources.Language.string;

/**
 * Task action that displays the difference (diff) between two text files. This
 * implementation uses a simple built-in diff viewer.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class CompareFiles extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-diff");
    }

    @Override
    public String getDescription() {
        return string("pa-diff-tt");
    }

    @Override
    public boolean performOnSelection(Member[] members) {
        if (!appliesToSelection(members)) {
            return false;
        }
        
        String[] li1 = fetch(members[0]);
        if (li1 == null) {
            return false;
        }
        String[] li2 = fetch(members[1]);
        if (li2 == null) {
            return false;
        }
        
        final Collection<DiffLine> diff = new LinkedList<>();
        new Diff<String>(new DiffListener<String>() {
            @Override
            public void unchanged(String[] original, String[] changed, int originalIndex, String element) {
                diff.add(new DiffLine(0, element));
            }

            @Override
            public void inserted(String[] original, String[] changed, int originalIndex, String insertedelement) {
                diff.add(new DiffLine(1, insertedelement));
            }

            @Override
            public void removed(String[] original, String[] changed, int originalIndex, String removedelement) {
                diff.add(new DiffLine(-1, removedelement));
            }
        }).findChanges(li1, li2);
        
        DiffDialog d = new DiffDialog(members[0].getFile().getName(), members[1].getFile().getName(), diff);
        members[0].getProject().getView().moveToLocusOfAttention(d);
        d.setVisible(true);

        return true;
    }
    
    class DiffLine {
        public DiffLine(int state, String text) {
            this.state = state;
            this.text = text;
        }

        /** Deleted if &lt; 0, unchanged if 0, inserted if &gt; 0. */
        public final int state;
        /** Line text. */
        public final String text;
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        throw new UnsupportedOperationException("must be performed on two files via perform( Member[] )");
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return members.length == 2
                && members[0].getMetadataSource().getDefaultCharset(members[0]) != null
                && members[1].getMetadataSource().getDefaultCharset(members[1]) != null;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return false;
    }

    private String[] fetch(Member m) {
        File f = m.getFile();
        FileInputStream in = null;
        BufferedReader r = null;
        LinkedList<String> lines = new LinkedList<>();
        try {
            in = new FileInputStream(f);
            r = new BufferedReader(new InputStreamReader(in, m.getMetadataSource().getDefaultCharset(m)));
            String line;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
            return null;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e2) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e2) {
                }
            }
        }
        return lines.toArray(String[]::new);
    }
}
