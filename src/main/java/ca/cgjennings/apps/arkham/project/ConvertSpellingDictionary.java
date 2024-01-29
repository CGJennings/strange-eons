package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.spelling.dict.Tools;
import ca.cgjennings.spelling.dict.WordList;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import static resources.Language.string;

/**
 * A task action for converting between spelling dictionary formats (plain text,
 * common prefix length compression, and ternary tree).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ConvertSpellingDictionary extends TaskActionTree {

    public ConvertSpellingDictionary() {
        add(new Converter(string("pa-cd-list"), "txt"));
        add(new Converter(string("prj-prop-dict-cpl"), "cpl"));
        add(new Converter(string("prj-prop-dict-tst"), "3tree"));
    }

    @Override
    public String getLabel() {
        return string("pa-ci-name");
    }

    public static class Converter extends TaskAction {

        private final String desc;
        private String ext;

        public Converter(String description, String extension) {
            desc = description;
            ext = extension;
        }

        @Override
        public boolean appliesTo(Project project, Task task, Member member) {
            // there is a real member that is not a folder and has one of
            // the dictionary format extensions other than this one
            return member != null && (!member.hasChildren())
                    && ProjectUtilities.matchExtension(member,
                            MetadataSource.DictionaryMetadata.DICTIONARY_EXTENSIONS
                    )
                    && !ProjectUtilities.matchExtension(member, ext);
        }

        @Override
        public String getLabel() {
            return desc;
        }

        @Override
        public String getActionName() {
            return "ConvertSpellingDictionary." + ext;
        }

        @Override
        public boolean perform(Project project, Task task, Member member) {
            try {
                WordList wl = Tools.read(member.getFile());

                // currently only TSTs may have freq data, so any conversion
                // will lose this data if present
                if (wl.hasFrequencyRanks()) {
                    final String[] options = new String[]{
                        string("pa-l-freq-warn"), string("cancel")
                    };
                    int okOpt = 0;
                    if (PlatformSupport.PLATFORM_IS_MAC) {
                        okOpt = 1;
                        options[1] = options[0];
                        options[0] = string("cancel");
                    }
                    int opt = JOptionPane.showOptionDialog(StrangeEons.getWindow(),
                            string("pa-l-freq-warn"),
                            string("pa-ci-ok"),
                            0, JOptionPane.WARNING_MESSAGE, null, options, okOpt
                    );
                    if (opt != okOpt) {
                        return true;
                    }
                }

                File f = ProjectUtilities.changeExtension(member.getFile(), ext);
                f = ProjectUtilities.getAvailableFile(f);

                Tools.write(f, Tools.listToSet(wl));

                member.getParent().synchronize();
            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-convert"), e);
            }
            return true;
        }
    }
}
