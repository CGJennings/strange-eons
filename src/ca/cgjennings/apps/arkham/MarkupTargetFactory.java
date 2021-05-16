package ca.cgjennings.apps.arkham;

import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import java.awt.Container;
import java.awt.Font;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JSpinner;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Creates {@link MarkupTarget} instances for valid components.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class MarkupTargetFactory {

    private MarkupTargetFactory() {
    }

    /**
     * Returns {@code true} if a markup target can be created for an
     * object. To be valid, the potential target must be non-{@code null},
     * and must be a supported type of interface component. If
     * {@code strict} is {@code true}, then the potential target must
     * also be showing, enabled, and editable. Currently supported component
     * types include text fields (any subclass of {@code JTextComponent})
     * and code editor controls. Other types might be supported in future
     * versions.
     *
     * <p>
     * Text fields are normally only accepted as valid targets when they are
     * descendants of the active game component editor. This eliminates a lot of
     * false positives, such as the path field in file dialogs. The default
     * decision as to whether a component is accepted can be overridden by
     * setting the client property
     * {@code MarkupTarget.FORCE_MARKUP_TARGET_PROPERTY} to either
     * {@code Boolean.TRUE} (accept if possible) or
     * {@code Boolean.FALSE} (always reject). To be accepted, the component
     * must still be of a supported type and (if {@code strict}) be
     * showing, enabled, and editable.
     *
     * @param potentialTarget the potential markup target to check
     * @param strict if {@code true}, the target must be showing, enabled,
     * and editable
     * @return {@code true} if the component can be a markup target
     */
    public static boolean isValidTarget(final Object potentialTarget, final boolean strict) {
        if (potentialTarget instanceof JComponent) {
            final JComponent jc = (JComponent) potentialTarget;
            if (strict && !jc.isEnabled()) {
                return false;
            }
            final Object explicit = jc.getClientProperty(MarkupTarget.FORCE_MARKUP_TARGET_PROPERTY);

            // always accept a code editor (if editable and showing) unless explicitly told not to
            if (jc instanceof JSourceCodeEditor) {
                if (strict && !(((JSourceCodeEditor) jc).isEditable() && jc.isShowing())) {
                    return false;
                }
                return !Boolean.FALSE.equals(explicit);
            }

            // the other controls we know how to deal with are text fields
            if (potentialTarget instanceof JTextComponent) {
                final JTextComponent jt = (JTextComponent) potentialTarget;

                if (!jt.isEditable() || !jt.isShowing()) {
                    return false;
                }

                // If the client property is forcing a decision, make it now. After
                // this, we can simply return the default decision to use when no
                // property is set. (Note that null is not an instanceof anything.)
                if (explicit instanceof Boolean) {
                    return ((Boolean) explicit);
                }

                // This is a hack that excludes the field for entering line numbers
                // in JCodeEditors. Since they come from another library, we
                // have to check for them another way than looking for the explicit
                // property.
                if ("JCodeEditorLineNumber".equals(jt.getName())) {
                    return false;
                }

                // By default we don't allow the field if it is not in the current
                // editor. This eliminates a lot of common cases like text fields
                // and dialogs. We also check that the field is not an editor for
                // a JSpinner control, which has a preset list of values
                // (usually numbers).
                StrangeEonsAppWindow win = StrangeEons.getWindow();
                if (win == null) {
                    return false; // shouldn't happen, but just in case
                }
                JInternalFrame editor = (JInternalFrame) win.getActiveEditor();
                if (editor != null && jt.isShowing()) {
                    Container parent = jt;
                    while (parent != null) {
                        if (parent == editor) {
                            return true;
                        }
                        if (parent instanceof JSpinner) {
                            return false;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns a markup target instance for a component if it is valid;
     * otherwise, returns {@code null}.
     *
     * @param potentialTarget the potential target to create a markup target for
     * @param strict if {@code true}, the component must be showing,
     * enabled, and editable
     * @return a markup target compatible with the provided object, or
     * {@code null}
     */
    public static MarkupTarget createMarkupTarget(Object potentialTarget, boolean strict) {
        JUtilities.threadAssert(); // ensure synched to EDT

        if (potentialTarget == cachedComponent) {
            if (isValidTarget(potentialTarget, strict)) {
                return cachedTarget;
            } else {
                return null;
            }
        }

        if (isValidTarget(potentialTarget, strict)) {
            MarkupTarget t;
            if (potentialTarget instanceof JSourceCodeEditor) {
                t = createCodeEditorInstance((JSourceCodeEditor) potentialTarget);
            } else {
                t = createTextComponentInstance((JTextComponent) potentialTarget);
            }
            cachedComponent = potentialTarget;
            cachedTarget = t;
            return t;
        }

        return null;
    }

    private static Object cachedComponent;
    private static MarkupTarget cachedTarget;

    private static MarkupTarget createTextComponentInstance(final JTextComponent c) {
        return new AbstractMarkupTarget() {
            @Override
            public Object getTarget() {
                return c;
            }

            @Override
            public String getText() {
                return c.getText();
            }

            @Override
            public int length() {
                return c.getDocument().getLength();
            }

            @Override
            public String getSelectedText() {
                String s = c.getSelectedText();
                return s == null ? "" : s;
            }

            @Override
            protected String getTextImpl(int start, int length) {
                try {
                    return c.getText(start, length);
                } catch (BadLocationException ex) {
                    throw new AssertionError();
                }
            }

            @Override
            public int getSelectionEnd() {
                return c.getSelectionEnd();
            }

            @Override
            public int getSelectionStart() {
                return c.getSelectionStart();
            }

            @Override
            public int setSelectedText(String text) {
                String t = MarkupTargetFactory.escapeUndisplayableChars(c.getFont(), text, true);
                c.replaceSelection(t);
                return t.length();
            }

            @Override
            public void setSelectionEnd(int end) {
                c.setSelectionEnd(clamp(end));
            }

            @Override
            public void setSelectionStart(int start) {
                c.setSelectionStart(clamp(start));
            }

            @Override
            public void select(int start, int end) {
                c.setCaretPosition(clamp(start));
                c.moveCaretPosition(clamp(end));
            }

            @Override
            public void selectNone() {
                int p = c.getCaretPosition();
                c.select(p, p);
            }

            @Override
            public void copy() {
                c.copy();
            }

            @Override
            public void cut() {
                c.cut();
            }

            @Override
            public void paste() {
                c.paste();
            }
        };
    }

    private static MarkupTarget createCodeEditorInstance(final JSourceCodeEditor c) {
        return new AbstractMarkupTarget() {
            @Override
            public Object getTarget() {
                return c;
            }

            @Override
            protected String getTextImpl(int start, int length) {
                String s = c.getText(start, length);
                return s == null ? "" : s;
            }

            @Override
            public String getSelectedText() {
                return c.getSelectedText();
            }

            @Override
            public int getSelectionEnd() {
                return c.getSelectionEnd();
            }

            @Override
            public int getSelectionStart() {
                return c.getSelectionStart();
            }

            @Override
            public String getText() {
                return c.getText();
            }

            @Override
            public int length() {
                return c.getDocumentLength();
            }

            @Override
            public int setSelectedText(String text) {
                String t = MarkupTargetFactory.escapeUndisplayableChars(c.getFont(), text, false);
                c.setSelectedText(t);
                return t.length();
            }

            @Override
            public void setSelectionEnd(int end) {
                c.setSelectionEnd(clamp(end));
            }

            @Override
            public void setSelectionStart(int start) {
                c.setSelectionStart(clamp(start));
            }

            @Override
            public void select(int start, int end) {
                c.select(clamp(start), clamp(end));
            }

            @Override
            public void selectNone() {
                c.selectNone();
            }

            @Override
            public void copy() {
                c.copy();
            }

            @Override
            public void cut() {
                c.cut();
            }

            @Override
            public void paste() {
                c.paste();
            }
        };
    }

    private static abstract class AbstractMarkupTarget implements MarkupTarget {

        @Override
        public int selectionLength() {
            return Math.abs(getSelectionEnd() - getSelectionStart());
        }

        @Override
        public String getText(int start, int length) {
            if (start < 0) {
                throw new IllegalArgumentException("start < 0");
            }
            if (length < 0) {
                throw new IllegalArgumentException("length < 0");
            }
            if (start + length > length()) {
                length = length() - start;
            }
            return getTextImpl(start, length);
        }

        protected abstract String getTextImpl(int start, int length);

        protected int clamp(int offset) {
            if (offset < 0) {
                offset = 0;
            } else {
                final int len = length();
                if (offset > len) {
                    offset = len;
                }
            }
            return offset;
        }

        @Override
        public void selectAll() {
            select(0, length());
        }

        @Override
        public void tagSelectedText(String prefix, String suffix, boolean caseSensitive) {
            int start = getSelectionStart();
            int end = getSelectionEnd();
            boolean swappedStartAndEnd = false;
            if (start > end) {
                int t = start;
                start = end;
                end = t;
                swappedStartAndEnd = true;
            }

            String text = getSelectedText();
            String ncPrefix = prefix, ncSuffix = suffix, ncText = text;
            if (!caseSensitive) {
                ncPrefix = prefix.toLowerCase(Locale.ENGLISH);
                ncSuffix = suffix.toLowerCase(Locale.ENGLISH);
                ncText = text.toLowerCase(Locale.ENGLISH);
            }

            // check if the selection is surrounded by the prefix and/or suffix,
            // and extend the selection if so
            boolean mutatedSelection = false;
            if (!ncText.startsWith(ncPrefix)) {
                // back off one letter at a time to see if we can form the prefix
                // this gives the user some leeway for making sloppy selections
                for (int i = 1; i <= prefix.length(); ++i) {
                    int s = start - i;
                    if (s < 0) {
                        break;
                    }
                    String f = getText(s, prefix.length());
                    if ((caseSensitive && f.equals(prefix)) || f.equalsIgnoreCase(prefix)) {
                        start = s;
                        mutatedSelection = true;
                        break;
                    }
                }
            }

            if (!ncText.endsWith(ncSuffix)) {
                // again, try extending one letter at a time
                // to see if we can form the suffix
                for (int i = suffix.length() - 1; i >= 0; --i) {
                    int s = end - i;
                    if (s < 0) {
                        continue;
                    }
                    String f = getText(s, suffix.length());
                    if ((caseSensitive && f.equals(suffix)) || f.equalsIgnoreCase(suffix)) {
                        end = s + suffix.length();
                        mutatedSelection = true;
                        break;
                    }
                }
            }

            if (mutatedSelection) {
                select(start, end);
                text = getSelectedText();
                ncText = caseSensitive ? text : text.toLowerCase(Locale.ENGLISH);
            }

            // Decide what the new text will be:
            // if the text is surrounded by the prefix and suffix, remove them,
            // otherwise add them.
            String replacement;
            if (ncText.startsWith(ncPrefix) && ncText.endsWith(ncSuffix)) {
                replacement = text.substring(prefix.length(), text.length() - suffix.length());
                prefix = suffix = "";
            } else {
                replacement = prefix + text + suffix;
            }

            setSelectedText(replacement);

            // restore the selection
            start += prefix.length();
            end = start + replacement.length() - prefix.length() - suffix.length();
            if (swappedStartAndEnd) {
                select(end, start);
            } else {
                select(start, end);
            }
        }

        @Override
        public void requestFocus() {
            ((JComponent) getTarget()).requestFocus();
        }
    }

    private static String escapeUndisplayableChars(Font textFont, String insert, boolean tagStyleEscape) {
        StringBuilder b = new StringBuilder(insert.length() + 16);
        for (int i = 0; i < insert.length(); ++i) {
            char c = insert.charAt(i);
            if (textFont.canDisplay(c)) {
                b.append(c);
            } else {
                if (tagStyleEscape) {
                    b.append(String.format("<u+%04x>", (int) c));
                } else {
                    b.append(String.format("\\u%04x", (int) c));
                }
            }
        }
        return b.length() == insert.length() ? insert : b.toString();
    }

    /**
     * Explicitly allows or disallows a component to be a markup target. An
     * explicitly allowed component must still be of an appropriate kind and
     * must be showing, enabled, and editable if checked with strict mode. The
     * primary effect of explicitly allowing a component to be a markup target
     * is to allow text fields that are not descendants of the active editor to
     * be targeted, such as fields in a dialog box.
     * <p>
     * This is a convenience method that sets the
     * {@code FORCE_MARKUP_TARGET_PROPERTY} client property on the target
     * component.
     *
     * @param target the target to modify
     * @param allow whether targeting should be explicitly enabled or explicitly
     * disabled
     * @throws NullPointerException if the target is {@code null}
     */
    public static void enableTargeting(JComponent target, boolean allow) {
        if (target == null) {
            throw new NullPointerException("target");
        }
        if (target instanceof JComboBox) {
            target = (JComponent) ((JComboBox) target).getEditor().getEditorComponent();
        }
        target.putClientProperty(MarkupTarget.FORCE_MARKUP_TARGET_PROPERTY, allow);
    }
}
