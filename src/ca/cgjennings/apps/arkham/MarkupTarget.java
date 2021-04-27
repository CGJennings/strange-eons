package ca.cgjennings.apps.arkham;

/**
 * A markup target is an adapter that allows modification of the text in user
 * interface components that can contain markup text for a game component. The
 * markup target presents a common interface for modifying the text in the
 * target component so that the underlying component type is transparent to the
 * user. Typical markup targets are text fields, text areas, and code editors.
 * <p>
 * Although you can create a markup target for any valid component directly, it
 * is more common to use the application markup target. For example, the
 * commands in the <b>Markup</b> menu use the application markup target as the
 * target of their commands.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface MarkupTarget {

    /**
     * If set in a component's client properties, forces whether a component is
     * or is not considered to be a markup target. Set to
     * <code>Boolean.TRUE</code> or <code>Boolean.FALSE</code>. Otherwise,
     * markup target candidates are evaluated using heuristics. To be valid, the
     * component must still be an acceptable type of control, and (in strict
     * validation mode) showing, enabled, and editable.
     *
     * @see MarkupTargetFactory#isValidTarget(java.lang.Object, boolean)
     */
    public static final String FORCE_MARKUP_TARGET_PROPERTY = "SE-markup-target";

    /**
     * Returns the current text of the markup target.
     *
     * @return the entire editable contents of the target
     */
    String getText();

    /**
     * Returns the length of the current text of the markup target.
     *
     * @return the length of the entire editable contents of the target
     */
    int length();

    /**
     * Returns the selected text of the markup target. If there is no selection,
     * returns the empty string.
     *
     * @return the current selection
     */
    String getSelectedText();

    /**
     * Returns the length of the selected text of the markup target. If there is
     * no selection, returns 0.
     *
     * @return the length of the current selection
     */
    int selectionLength();

    /**
     * Returns an arbitrary substring of the text of the markup target. The
     * returned text will be empty if the start position is past the end of the
     * document or the length is 0. If there are fewer characters after the
     * start offset than the requested length, then the rest of the document is
     * returned.
     *
     * @param start the start index of the substring
     * @param length the number of characters to include
     * @return the text of the markup target from <code>start</code> to
     * <code>end-1</code>, inclusive
     * @throws IllegalArgumentException if start or length is negative
     */
    String getText(int start, int length);

    /**
     * Changes the selection in the markup target. Invalid selections will be
     * clamped to the valid range of the document. Note that <code>start</code>
     * does not need to be less than <code>end</code>; the cursor will be
     * located at the <code>end</code> offset.
     *
     * @param start the start offset of the new selection
     * @param end the end offset of the new selection
     */
    void select(int start, int end);

    /**
     * Selects all text in the document.
     */
    void selectAll();

    /**
     * Clears the selection without changing the caret position.
     */
    void selectNone();

    /**
     * Returns the start offset of the selection; if there is no selection, this
     * will be the same as {@link #getSelectionEnd()}.
     *
     * @return the selection start offset
     */
    int getSelectionStart();

    /**
     * Sets the start of the selection. Invalid values will be clamped to the
     * bounds of the document.
     *
     * @param start the new selection start
     * @see #getSelectionStart()
     * @see #select(int, int)
     */
    void setSelectionStart(int start);

    /**
     * Returns the end offset of the selection. The end offset is the offset of
     * the cursor position within the document text.
     *
     * @return the selection end offset
     */
    int getSelectionEnd();

    /**
     * Sets the end of the selection. Invalid values will be clamped to the
     * bounds of the document.
     *
     * @param end the new selection end
     * @see #getSelectionEnd()
     * @see #select(int, int)
     */
    void setSelectionEnd(int end);

    /**
     * Inserts text at the cursor position. If there is an active selection, it
     * will be replaced. The inserted text may be modified before insertion, so
     * the actual number of characters inserted may not be the same as
     * <code>text.length()</code>.
     *
     * @param text the text to insert
     * @return the number of characters inserted
     * @throws NullPointerException if text is <code>null</code>
     */
    int setSelectedText(String text);

    /**
     * Tags the selected text with a prefix and suffix. If the selected text is
     * already surrounded by the prefix and suffix, then the prefix and suffix
     * will be removed. This is a simple way to add or remove modal markup tags
     * around the selection. For example, to bold the selected text (or unbold
     * if it is surround by a bold tag pair):<br>
     * <code>tagSelection( "&lt;b&gt;", "&lt;/b&gt;" )</code>
     *
     * @param prefix the prefix to insert (or remove) at the start of the
     * selection
     * @param suffix the suffix to insert (or remove) at the end of the
     * selection
     * @param caseSensitive if <code>true</code>, the prefix and suffix are
     * case-sensitive
     */
    void tagSelectedText(String prefix, String suffix, boolean caseSensitive);

    /**
     * Cuts the current selection to the clipboard.
     */
    void cut();

    /**
     * Copies the current selection to the clipboard.
     */
    void copy();

    /**
     * Replaces the current selection with the clipboard contents.
     */
    void paste();

    /**
     * Returns the actual target wrapped by this <code>MarkupTarget</code>.
     *
     * @return the underlying object that is manipulated by this markup target
     * instance
     */
    Object getTarget();

    /**
     * If the target is a component, then it will request input focus.
     */
    void requestFocus();

//	/**
//	 * Add a new <code>MarkupTargetListener</code> to this editor.
//	 *
//	 * @param mtl the listener to add
//	 * @since 2.00.9
//	 */
//	@Override
//	public void addMarkupTargetListener( MarkupTargetListener mtl ) {
//		listenerList.add( MarkupTargetListener.class, mtl );
//	}
//
//	/**
//	 * Remove a <code>MarkupTargetListener</code> from this editor.
//	 *
//	 * @param mtl the listener to remove
//	 * @since 2.00.9
//	 */
//	@Override
//	public void removeMarkupTargetListener( MarkupTargetListener mtl ) {
//		listenerList.remove( MarkupTargetListener.class, mtl );
//	}
//
//	public boolean canInsertMarkup() {
//		if( markupTarget == null ) return false;
//
//
//
//		
//
//		Object forceProperty = markupTarget.getClientProperty( FORCE_MARKUP_TARGET_PROPERTY );
//		if( forceProperty == Boolean.FALSE ) return false;
//		if( forceProperty == Boolean.TRUE ) return true;
//
//		// always allow code editors, even if in other windows, unless explicitly
//		// disabled by property---useful for plug-ins, IDE stuff
//		if( markupTarget instanceof JCodeEditor && markupTarget.isShowing() ) {
//			return ((JCodeEditor) markupTarget).isEditable();
//		}
//
//		if( markupTarget instanceof JTextComponent ) {
//			if( !((JTextComponent) markupTarget).isEditable() ) return false;
//		}
//
//		// HACK: check for the field that allows entering line numbers in JCodeEditors
//		if( "JCodeEditorLineNumber".equals( markupTarget.getName() ) ) return false;
//
//		// don't allow if it is not in the current editor so we eliminate a lot
//		// of misc text fields in dialogs and whatnot
//		JInternalFrame editor = (JInternalFrame) getActiveEditor();
//		if( editor != null && MarkupTargetHelper.isValidTarget( markupTarget ) && markupTarget.isShowing() ) {
//			Container parent = markupTarget;
//			while( parent != null && parent != this ) {
//				if( parent == editor ) {
//					return true;
//				}
//				if( parent instanceof JSpinner ) return false;
//				parent = parent.getParent();
//			}
//		}
//
//		return false;
//	}
//
//	@Override
//	public JComponent getMarkupTarget() {
//		return canInsertMarkup() ? markupTarget : null;
//	}
//
//	@Override
//	public void insertMarkup( String insert ) {
//		if( canInsertMarkup() ) {
//			MarkupTargetHelper.insertMarkup( markupTarget, insert );
//		}
//	}
//
//	@Override
//	public void insertMarkupTags( String prefix, String suffix ) {
//		if( canInsertMarkup() ) {
//			MarkupTargetHelper.insertMarkupTags( markupTarget, prefix, suffix );
//		}
//	}
//
//	public String getMarkupTargetSelection() {
//		return MarkupTargetHelper.getMarkupTargetSelection();
//	}
//	
}
