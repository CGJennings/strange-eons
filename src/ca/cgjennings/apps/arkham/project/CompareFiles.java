package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.algo.Diff;
import ca.cgjennings.algo.DiffListener;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import static resources.Language.string;

/**
 * Task action that displays the difference (diff) between two text files. This
 * implementation uses a simple built-in diff viewer, although the plug-in
 * authoring kit includes an example that specializes this action to use KDiff3
 * (this could be altered to use another tool if desired).
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

        final StringBuilder buff = new StringBuilder(8_192);

        ReversibleDiff diff = new ReversibleDiff(li1, li2, new DiffListener<String>() {
            @Override
            public void unchanged(Object original, Object changed, int originalIndex, String entry) {
                if (buff.length() > 0) {
                    buff.append('\n');
                }
                buff.append("  ").append(entry);
            }

            @Override
            public void inserted(Object original, Object changed, int originalIndex, String entry) {
                if (buff.length() > 0) {
                    buff.append('\n');
                }
                buff.append("+ ").append(entry);
            }

            @Override
            public void removed(Object original, Object changed, int originalIndex, String entry) {
                if (buff.length() > 0) {
                    buff.append('\n');
                }
                buff.append("- ").append(entry);
            }
        });

        diff.findChanges(li1, li2);
        String d12 = buff.toString();

        buff.delete(0, buff.length());
        diff.reverse();
        diff.findChanges(li2, li1);
        String d21 = buff.toString();

        DiffDialog d = new DiffDialog(members[0].getFile().getName(), members[1].getFile().getName(), d12, d21);
        members[0].getProject().getView().moveToLocusOfAttention(d);
        d.setVisible(true);

        return true;
    }

    private static class ReversibleDiff extends Diff<String> {

        public ReversibleDiff(String[] original, String[] changed, DiffListener<String> listener) {
            super(listener);
            // we compare the trimmed versions of both documents to
            // ignore simple changes in spacing; eq1 and eq2 hold trimmed
            // copies so we don't create a string for each comaparison
            eq1 = createComparisonArray(original);
            eq2 = createComparisonArray(changed);
        }

        @Override
        public boolean equal(String a, String b, int originalIndex, int changedIndex) {
            return eq1[originalIndex].equals(eq2[changedIndex]);
        }

        public void reverse() {
            String[] t = eq1;
            eq1 = eq2;
            eq2 = t;
        }
        private String[] eq1, eq2;

        private String[] createComparisonArray(String[] source) {
            String[] c = new String[source.length];
            for (int i = 0; i < source.length; ++i) {
                c[i] = source[i].trim().replaceAll("\\s\\s+", " ");
            }
            return c;
        }
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
        return lines.toArray(new String[0]);
    }
}
