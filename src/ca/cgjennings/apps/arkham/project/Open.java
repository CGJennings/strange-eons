package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.AbstractStrangeEonsEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.dialog.DictionaryEditor;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.TextIndexViewer;
import ca.cgjennings.apps.arkham.editors.CardLayoutEditor;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.ui.textedit.CodeType;
import ca.cgjennings.platform.DesktopIntegration;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Level;
import javax.swing.JDialog;
import static resources.Language.string;
import resources.Settings;

/**
 * A task action that opens project files. How a file is handled when it is
 * opened depends on the user's settings, which may specify rules for various
 * file extensions. The default rule has Strange Eons first check if it knows
 * how to open the file (either via a registered {@link InternalOpener} or else
 * via a built-in default file handling mechanism). If it does not know how to
 * deal with the file itself, and if the platform supports this capability, the
 * Strange Eons will ask the operating system to open the file if it knows how.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Open extends TaskAction {

    public Open() {
    }

    @Override
    public String getLabel() {
        return string("open");
    }

    protected int cascade;

    @Override
    public boolean performOnSelection(Member[] members) {
        cascade = 0;
        return super.performOnSelection(members);
    }

    @Override
    @SuppressWarnings("fallthrough")
    public boolean perform(Project project, Task task, Member member) {
        if (member == null) {
            throw new AssertionError();
        }

        File f = member.getFile();

        try {
            switch (getOpenRule(member.getExtension())) {
                case INTERNAL_EDIT:
                    if (tryInternalOpen(project, member, f)) {
                        return true;
                    }
                // fallthrough
                case DESKTOP_EDIT:
                    return tryDesktopEdit(f);
                case INTERNAL_OPEN:
                    if (tryInternalOpen(project, member, f)) {
                        return true;
                    }
                // fallthrough
                case DESKTOP_OPEN:
                    return tryDesktopOpen(f);
                case DESKTOP_PRINT:
                    return tryDesktopPrint(f);
                case CUSTOM_COMMAND:
                    return runCommand(f, getRuleCommand(member.getExtension()));
            }
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
        }
        return false;
    }

    public boolean tryDesktopOpen(File f) {
        if (DesktopIntegration.OPEN_SUPPORTED) {
            try {
                DesktopIntegration.open(f);
                return true;
            } catch (Exception e) {
                StrangeEons.log.log(Level.WARNING, "exception while opening " + f, e);
            }
        }
        return false;
    }

    public boolean tryDesktopEdit(File f) {
        if (DesktopIntegration.EDIT_SUPPORTED) {
            try {
                DesktopIntegration.edit(f);
                return true;
            } catch (Exception e) {
                StrangeEons.log.log(Level.WARNING, "exception while opening " + f + " for editing", e);
            }
        }
        return false;
    }

    public boolean tryDesktopPrint(File f) {
        if (DesktopIntegration.PRINT_SUPPORTED) {
            try {
                DesktopIntegration.print(f);
                return true;
            } catch (Exception e) {
                StrangeEons.log.log(Level.WARNING, "exception while opening " + f + " for printing", e);
            }
        }
        return false;
    }

    public boolean tryInternalOpen(Project project, Member member, File f) throws IOException {
        StrangeEonsEditor[] editors = StrangeEons.getWindow().getEditorsShowingFile(f);
        if (editors.length > 0) {
            editors[0].select();
            return true;
        }

        if (member != null && ProjectUtilities.matchExtension(member, internalTypes)) {
            StrangeEons.getWindow().openFile(f);
            return true;
        }

        if (customOpeners != null) {
            for (InternalOpener io : customOpeners) {
                if (io.appliesTo(f)) {
                    try {
                        io.open(f);
                    } catch (Exception e) {
                        ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
                    }
                    return true;
                }
            }
        }

        if (member != null && f.getName().equals("eons-plugin")) {
            RootEditor.showRootEditor(member, cascade++);
            return true;
        }

        if (DeckTask.isCopiesList(member)) {
            CodeEditor ed = new CodeEditor(f, ProjectUtilities.ENC_SETTINGS, CodeType.SETTINGS);
            ed.setFileDropListener(new DeckTask.CopiesFileDropListener(ed));
            StrangeEons.getWindow().addEditor(ed);
            return true;
        }

        // default handling for built-in files
        final String ext = member != null ? member.getExtension() : ProjectUtilities.getFileExtension(f);
        switch (ext) {
            case "cardlayout":
                StrangeEons.getWindow().addEditor(new CardLayoutEditor(f));
                return true;
            case "idx":
                JDialog d = new TextIndexViewer(StrangeEons.getWindow(), f);
                if (project != null) {
                    project.getView().moveToLocusOfAttention(d);
                }
                d.setVisible(true);
                return true;

            case "cpl":
            case "3tree":
                            try {
                DictionaryEditor ed = new DictionaryEditor(StrangeEons.getWindow(), f);
                if (project != null) {
                    project.getView().moveToLocusOfAttention(ed);
                }
                ed.setVisible(true);
            } catch (IOException e) {
                ErrorDialog.displayError(string(""), e);
            }
            return true;
        }

        CodeEditor ed = null;
        // check extension against known code types
        if (ed == null) {
            for (CodeType type : CodeType.values()) {
                if (!type.getExtension().equals(ext)) {
                    continue;
                }
                ed = new CodeEditor(f, type);
            }
        }
        // check some fallback types
        if (ed == null) {
            if (MetadataSource.TextMetadata.isDocType(member)) {
                ed = new CodeEditor(f, CodeType.PLAIN);
            }
        }
        if (ed != null) {
            StrangeEons.getWindow().addEditor(ed);
            return true;
        }

        return false;
    }

    /**
     * Runs a command as if for the {@link OpenRule#CUSTOM_COMMAND} rule. See
     * {@link #setRuleCommand(java.lang.String, java.lang.String)} for details.
     *
     * @param f the file that the command applies to (used to complete
     * <tt>%f</tt> variables).
     * @param commandString the command string to use
     * @return {@code true} if the command is successfully started
     * @throws IOException if an exception occurs while executing the command
     */
    public static boolean runCommand(File f, String commandString) throws IOException {
        String[] tokens = splitCommand(commandString);
        String path = f.getAbsolutePath();
        for (int i = 0; i < tokens.length; ++i) {
            tokens[i] = tokens[i].replace("%%", "%\0").replace("%f", path).replace("%\0", "%");
        }
        Runtime.getRuntime().exec(tokens, null, f.getParentFile());
        return true;
    }

    /**
     * Splits a command string into tokens by splitting on spaces unless those
     * spaces are enclosed in a quote sequence. See
     * {@link #setRuleCommand(java.lang.String, java.lang.String)} for details.
     *
     * @param commandString
     * @return an array of command line tokens
     */
    public static String[] splitCommand(String commandString) {
        LinkedList<String> tokens = new LinkedList<>();
        StringBuilder token = new StringBuilder();
        int state = 0;
        for (int i = 0; i < commandString.length(); ++i) {
            char c = commandString.charAt(i);
            switch (state) {
                case 0:
                    if (Character.isWhitespace(c)) {
                        tokens.add(token.toString());
                        token.delete(0, token.length());
                    } else if (c == '\"') {
                        state = 1;
                    } else {
                        token.append(c);
                    }
                    break;
                case 1:
                    if (c == '\"') {
                        state = 0;
                        // clever trick: inside a quote, "" will produce a
                        // quote in the token without (effectively) leaving quote mode
                        // the first quote will leave, the second brings us back, the
                        // check below inserts a quote in the token
                        if (i < commandString.length() - 1 && commandString.charAt(i + 1) == '\"') {
                            token.append('\"');
                        }
                    } else {
                        token.append(c);
                    }
                    break;
                default:
                    throw new AssertionError("unknown state: " + state);
            }
        }
        // add last token, if any
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens.toArray(String[]::new);
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member != null && !member.isFolder();
    }

    private static final String[] internalTypes = new String[]{
        "eon", "seplugin", "seext", "setheme", "selibrary"
    };

    /**
     * Returns an array of the extensions for which an {@link OpenRule} has been
     * set.
     *
     * @return an array of file name extensions for which an explicit rule
     * exists
     */
    public static String[] getRuleExtensions() {
        HashSet<String> extensions = new HashSet<>();
        for (String key : Settings.getUser().getKeySet()) {
            if (key.equals("open-rule")) {
                extensions.add("");
            } else if (key.startsWith("open-rule-")) {
                extensions.add(key.substring("open-rule-".length()));
            }
        }
        String[] exts = extensions.toArray(String[]::new);
        java.util.Arrays.sort(exts);
        return exts;
    }

    /**
     * Sets the rule used to open files with the given extension. The rule will
     * be applied to all files with the given extension unless a
     * {@link SpecializedAction} overrides the standard open action. (Note that
     * specializing the open action is not recommended. Instead, if you wish to
     * add custom code to handle opening certain files, you should register an
     * {@link InternalOpener} with the standard open action.)
     *
     * @param extension the file extension to set the rule for
     * @param rule the rule to set
     */
    public static void setOpenRule(String extension, OpenRule rule) {
        Settings.getUser().set(openRuleKey(extension), rule.name());
        if (rule != OpenRule.CUSTOM_COMMAND) {
            Settings.getUser().reset("exec-" + openRuleKey(extension));
        }
    }

    /**
     * Returns the {@link OpenRule} that controls handling for files with the
     * given file name extension. If no rule is explicitly set for an extension,
     * the default rule is {@link OpenRule#INTERNAL_OPEN}.
     *
     * @param extension the file extension to return a rule for
     * @return the rule used when opening files with the given extension
     */
    public static OpenRule getOpenRule(String extension) {
        OpenRule rule = OpenRule.INTERNAL_OPEN;
        String r = Settings.getShared().get(openRuleKey(extension));
        if (r != null) {
            r = r.trim();
            if (!r.isEmpty()) {
                try {
                    rule = OpenRule.valueOf(r);
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return rule;
    }

    /**
     * Returns the rule that controls handling files with the given file name
     * extension to its default state.
     *
     * @param extension the extension to revert to its default rule
     */
    public static void deleteOpenRule(String extension) {
        Settings.getUser().reset(openRuleKey(extension));
        Settings.getUser().reset("exec-" + openRuleKey(extension));
    }

    /**
     * Sets the command to execute when opening files with the given file name
     * extension when they use the open rule {@link OpenRule#CUSTOM_COMMAND}.
     * <p>
     * The escape sequence
     * <tt>%f</tt> will be replaced with the complete path to the file. (The
     * sequence <tt>%%</tt> may be used to insert a plain percent sign.) Command
     * line tokens that contain spaces must be enclosed in double quotes; to
     * include a double quote within such a sequence it must be escaped as
     * <tt>\&quot;</tt>.
     *
     * @param extension the file name extension to set the command string for
     * @param commandString the command string to use when opening such files
     */
    public static void setRuleCommand(String extension, String commandString) {
        Settings.getUser().set("exec-" + openRuleKey(extension), commandString);
    }

    /**
     * Returns the command that is executed when opening files with the given
     * file name extension if it uses the open rule
     * {@link OpenRule#CUSTOM_COMMAND}.
     *
     * @param extension the file name extension to get the command string for
     * @return the command string used to open matching files
     */
    public static String getRuleCommand(String extension) {
        return Settings.getUser().get("exec-" + openRuleKey(extension));
    }

    /**
     * Rules that describe how a file should be opened by the open action. The
     * rules for various file types are set by file name extension.
     */
    public static enum OpenRule {
        /**
         * Attempt to open the file using the application set to "edit" files of
         * this type on the user's desktop.
         */
        DESKTOP_EDIT,
        /**
         * Attempt to open the file using the application set to "open" files of
         * this type on the user's desktop.
         */
        DESKTOP_OPEN,
        /**
         * Attempt to open the file using the application set to "print" files
         * of this type on the user's desktop.
         */
        DESKTOP_PRINT,
        /**
         * If a registered {@link InternalOpener} accepts this file, then the
         * first such opener is used to open the file. If no opener is
         * registered but Strange Eons has a built-in method for dealing with
         * the file, then the built-in method is used. Otherwise, the
         * {@link #DESKTOP_EDIT} rule is used.
         */
        INTERNAL_EDIT,
        /**
         * If a registered {@link InternalOpener} accepts this file, then the
         * first such opener is used to open the file. If no opener is
         * registered but Strange Eons has a built-in method for dealing with
         * the file, then the built-in method is used. Otherwise, the
         * {@link #DESKTOP_OPEN} rule is used.
         */
        INTERNAL_OPEN,
        /**
         * Uses a custom command line to handle the file. The command to execute
         * is set with {@link #setRuleCommand}.
         */
        CUSTOM_COMMAND
    }

    private static String openRuleKey(String extension) {
        return "open-rule" + (extension.isEmpty() ? "" : ("-" + extension.toLowerCase(Locale.CANADA)));
    }

    /**
     * If you wish to add special handling for opening a file type, you may
     * register an {@code InternalOpener} for it. The opener will be used to
     * open files that it applies to if the open rule for the file is either
     * {@link OpenRule#INTERNAL_OPEN} or {@link OpenRule#INTERNAL_EDIT}.
     */
    public interface InternalOpener {

        /**
         * Returns {@code true} if this opener should be used to open the
         * specified file.
         *
         * @param f the file to check
         * @return {@code true} if the opener can open the file, {@code false}
         * otherwise
         */
        public boolean appliesTo(File f);

        /**
         * Opens the specified file, typically by creating a new editor and
         * adding it to the main application window.
         *
         * @param f the file to create an editor for
         * @throws Exception if an exception occurs while creating the editor
         * @see AbstractStrangeEonsEditor
         */
        public void open(File f) throws Exception;
    }

    /**
     * Registers a new internal opener for handling a custom file type.
     *
     * @param opener the opener being registered
     * @see #unregisterOpener
     * @see InternalOpener
     */
    public static void registerOpener(InternalOpener opener) {
        if (opener == null) {
            throw new NullPointerException("opener");
        }
        if (customOpeners == null) {
            customOpeners = new LinkedHashSet<>();
        }
        customOpeners.add(opener);
    }

    /**
     * Unregisters a previously registered internal opener for handling a custom
     * file type. Has no effect if the opener is {@code null} or is not
     * registered.
     *
     * @param opener the opener being unregistered
     * @see #registerOpener
     * @see InternalOpener
     */
    public static void unregisterOpener(InternalOpener opener) {
        if (opener == null) {
            throw new NullPointerException("opener");
        }
        if (customOpeners == null) {
            return;
        }
        customOpeners.remove(opener);
    }

    private static LinkedHashSet<InternalOpener> customOpeners;
}
