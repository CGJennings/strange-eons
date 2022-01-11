package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.Collator;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.Segment;

//
// This class is based loosely on discussion and public domain code
// by Thomas Bierhance, at <http://www.orbital-computer.de/JComboBox/>.
//
/**
 * An adapter for the document in the {@code JTextComponent} of a
 * {@code JComboBox} that extends the {@code JComboBox} to automatically
 * complete the field text using entries from the combo box list.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class AutocompletionDocument implements NestingDocument {
    // the document being adapted

    private Document document;
    // the combo box to autocomplete
    private JComboBox comboBox;
    // the editor text component
    private JTextComponent editor;
    // whether autocompletion is currently enabled
    private boolean autocompletion;
    // whether autocompletion should ignore case
    private boolean ignorecase;

    /**
     * Create a new {@code AutocompletionDocument} that is wrapped around the
     * existing document within a combo box. The combo box's editor must be a
     * subclass of {@code JTextComponent}.
     * <p>
     * The new document is not installed in the combo box, so autocompletion is
     * not started automatically. To easily create and install an adapter when
     * possible, use {@link #install(JComboBox)}.
     *
     * @param comboBox
     * @throws IllegalArgumentException is the combo box's editor is not a
     * {@code JTextComponent}
     */
    public AutocompletionDocument(JComboBox comboBox) {
        Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (!(editorComponent instanceof JTextComponent)) {
            throw new IllegalArgumentException("The combo box's editor must be a subclass of JTextComponent");
        }

        this.comboBox = comboBox;
        editor = (JTextComponent) editorComponent;
        document = editor.getDocument();
        isInserting = false;
        autocompletion = true;
        ignorecase = false;

        installListeners();
    }

    private void installListeners() {
        comboBox.addActionListener((ActionEvent e) -> {
            if (!isInserting) {
                editor.selectAll();
            }
        });
    }

    /**
     * Return the original document that is being wrapped by this adapter.
     *
     * @return the wrapped document
     */
    @Override
    public Document getDocument() {
        return document;
    }

    /**
     * Return the {@code JComboBox} that this document is autocompleting.
     *
     * @return the autocompleted combo box
     */
    public JComboBox getComboBox() {
        return comboBox;
    }

    /**
     * Create and install an {@code AutocompletionDocument} on an existing
     * {@code JComboBox}, causing it to begin autocompleting the text field
     * content.
     *
     * @param comboBox the {@code JComboBox} to modify
     * @return the newly installed document if installation was successful,
     * otherwise {@code null} if it failed because the {@code JComboBox} is not
     * using a {@code JTextComponent}
     */
    public static AutocompletionDocument install(JComboBox comboBox) {
        boolean wasEditable = comboBox.isEditable();
        if (!wasEditable) {
            comboBox.setEditable(true);
        }

        AutocompletionDocument document;
        try {
            document = new AutocompletionDocument(comboBox);
            ((JTextComponent) comboBox.getEditor().getEditorComponent()).setDocument(document);
        } catch (IllegalArgumentException e) {
            // restore previous value if unable to make an autocompleter
            comboBox.setEditable(wasEditable);
            return null;
        }
        return document;
    }

    /**
     * Create and install an {@code AutocompletionDocument} on an existing
     * {@code JComboBox}, causing it to begin autocompleting the text field
     * content.
     *
     * @param comboBox the {@code JComboBox} to modify
     * @param casesensitive whether autocompletion matching should be case
     * sensitive
     * @return the newly installed document if installation was successful,
     * otherwise {@code null} if it failed because the {@code JComboBox} is not
     * using a {@code JTextComponent}
     */
    public static AutocompletionDocument install(JComboBox comboBox, boolean casesensitive) {
        AutocompletionDocument document = install(comboBox);
        document.setCaseSensitive(casesensitive);
        return document;
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
        document.remove(offs, len);
    }

    @Override
    public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
        if (!isInserting) {
            // only complete when the cursor is at the end of the line
            boolean doCompletion = getAutocompletion() && (offset == getLength());
            // if the insert is blank and the offset is 0, do not complete;
            // otherwise it becomes impossible to enter blank strings
            if (offset == 0 && str.length() == 0) {
                doCompletion = false;
            }

            // if more than 1 char inserted, do not complete:
            // this prevents completion on paste, but also allows
            // setSelectedItem to work without picking up the wrong item
            // when an exact item is set that is a prefix of a previous item
//			if( str.length() > 1 ) {
//				doCompletion = false;
//			}
            isInserting = true;
            document.insertString(offset, str, a);

            if (doCompletion) {
                String text = getText(0, getLength());
                Object match = findMatchingPrefix(text);

                if (match != null) {
                    comboBox.getModel().setSelectedItem(match);

                    // put the completed text in the editor
                    try {
                        // remove all text and insert the completed string
                        document.remove(0, getLength());
                        document.insertString(0, match.toString(), null);
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }

                    // select the completed portion
                    editor.setCaretPosition(getLength());
                    if (offset + 1 <= getLength()) {
                        editor.moveCaretPosition(offset + 1);
                    }
                }
            }

            isInserting = false;
        }
    }

    // true if currently inserting a string, so we can insert strings
    // (change the selection) without triggering another autocompletion
    private boolean isInserting = false;

    private Object findMatchingPrefix(String pattern) {
        ComboBoxModel model = comboBox.getModel();
        if (collator != null) {
            return findMatchingPrefixBinary(model, pattern);
        } else {
            return findMatchingPrefixLinear(model, pattern);
        }
    }

    private Object findMatchingPrefixLinear(ComboBoxModel model, String pattern) {
        String bestMatch = null;
        int shortest = Integer.MAX_VALUE;
        for (int i = 0, n = model.getSize(); i < n; ++i) {
            Object candidate = model.getElementAt(i);
            if (candidate != null) {
                String candidateString = candidate.toString();
                if (matches(pattern, candidateString)) {
                    if (candidateString.length() < shortest) {
                        bestMatch = candidateString;
                        shortest = bestMatch.length();
                    }
                }
            }
        }
        return bestMatch;
    }

    private boolean matches(String pattern, String candidate) {
        if (ignorecase) {
            return candidate.regionMatches(true, 0, pattern, 0, pattern.length());
        } else {
            return candidate.startsWith(pattern);
        }
    }
    private Collator collator;

    /**
     * Return the list order {@code Collator}, or {@code null} if none is set.
     *
     * @return the {@code Collator} by which elements are ordered
     */
    public Collator getCollator() {
        return collator;
    }

    /**
     * If set to a non-{@code null} object, it is assumed that list elements are
     * sorted according to {@code collator}. A faster search algorithm can be
     * employed when looking for an autocomplete match.
     *
     * @param collator the {@code Collator} by which elements are ordered
     */
    public void setCollator(Collator collator) {
        this.collator = collator;
    }

    private Object findMatchingPrefixBinary(ComboBoxModel model, String pattern) {
        Object selectedItem = model.getSelectedItem();
//	if( selectedItem != null && matches( pattern, selectedItem.toString() ) ) {
//	    return selectedItem;
//	}
        int lower = -1;
        int upper = model.getSize();
        int match;

        while (lower + 1 != upper) {
            int middle = (lower + upper) / 2;

            String item = model.getElementAt(middle).toString();

            if (matches(pattern, item)) {
                upper = middle;
            } else {
                if (collator.compare(item, pattern) < 0) {
                    lower = middle;
                } else {
                    upper = middle;
                }
            }
        }
        match = upper;
        if (match >= model.getSize() || !matches(pattern, model.getElementAt(match).toString())) {
            return null;
        }
        return model.getElementAt(match);
    }

    @Override
    public int getLength() {
        return document.getLength();
    }

    @Override
    public void addDocumentListener(DocumentListener listener) {
        document.addDocumentListener(listener);
    }

    @Override
    public void removeDocumentListener(DocumentListener listener) {
        document.removeDocumentListener(listener);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
        document.addUndoableEditListener(listener);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener listener) {
        document.removeUndoableEditListener(listener);
    }

    @Override
    public Object getProperty(Object key) {
        return document.getProperty(key);
    }

    @Override
    public void putProperty(Object key, Object value) {
        document.putProperty(key, value);
    }

    @Override
    public String getText(int offset, int length) throws BadLocationException {
        return document.getText(offset, length);
    }

    @Override
    public void getText(int offset, int length, Segment txt) throws BadLocationException {
        document.getText(offset, length, txt);
    }

    @Override
    public Position getStartPosition() {
        return document.getStartPosition();
    }

    @Override
    public Position getEndPosition() {
        return document.getEndPosition();
    }

    @Override
    public Position createPosition(int offs) throws BadLocationException {
        return document.createPosition(offs);
    }

    @Override
    public Element[] getRootElements() {
        return document.getRootElements();
    }

    @Override
    public Element getDefaultRootElement() {
        return document.getDefaultRootElement();
    }

    @Override
    public void render(Runnable r) {
        document.render(r);
    }

//	private static void createAndShowGUI() {
//		// the combo box (add/modify items if you like to)
//		final JComboBox comboBox = new JComboBox( new Object[] { "Ester", "Jordi", "Jordina", "Jordinalia", "Jorge", "Sergi", "Sergio" } );
//		AutocompletionDocument doc = install( comboBox );
////	doc.setCollator( Collator.getInstance() );
//
//		// create and show a window containing the combo box
//		final JFrame frame = new JFrame();
//		frame.setLayout( new java.awt.FlowLayout() );
//		frame.setDefaultCloseOperation( 3 );
//		frame.getContentPane().add( comboBox );
//		frame.getContentPane().add( new JButton( "Do Nothing" ) );
//		frame.pack();
//		frame.setLocationByPlatform( true );
//		frame.setVisible( true );
//	}
//
//	public static void main(String[] args) {
//
//		javax.swing.SwingUtilities.invokeLater( new Runnable() {
//
//			public void run() {
//				createAndShowGUI();
//			}
//		} );
//
//	}
    public boolean getAutocompletion() {
        return autocompletion;
    }

    public void setAutocompletion(boolean autocompletion) {
        this.autocompletion = autocompletion;
    }

    public boolean isCaseSensitive() {
        return !ignorecase;
    }

    public void setCaseSensitive(boolean casesensitive) {
        ignorecase = !casesensitive;
    }
}
