package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.ColourDialog;
import ca.cgjennings.apps.arkham.ContextBar;
import ca.cgjennings.apps.arkham.MarkupTarget;
import ca.cgjennings.apps.arkham.NewEditorDialog;
import ca.cgjennings.apps.arkham.ParagraphDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import static ca.cgjennings.apps.arkham.commands.HDeckCommand.getDeckEditor;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.deck.DeckEditor;
import ca.cgjennings.apps.arkham.deck.Page;
import ca.cgjennings.apps.arkham.deck.ViewOptions;
import ca.cgjennings.apps.arkham.deck.item.EditablePageItem;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.deck.item.StyleCapture;
import ca.cgjennings.apps.arkham.deck.item.StyleEditor;
import ca.cgjennings.apps.arkham.dialog.EndUserExpansion;
import ca.cgjennings.apps.arkham.dialog.ExpansionSelectionDialog;
import ca.cgjennings.apps.arkham.dialog.InsertCharsDialog;
import ca.cgjennings.apps.arkham.dialog.InsertImageDialog;
import ca.cgjennings.apps.arkham.dialog.PluginManager;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogDialog;
import ca.cgjennings.apps.arkham.plugins.catalog.ConfigureUpdatesDialog;
import ca.cgjennings.apps.arkham.project.ProjectView;
import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.ui.JHelpButton;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.textedit.EditorCommands;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import ca.cgjennings.ui.textedit.Tokenizer;
import java.awt.Color;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import resources.ResourceKit;
import resources.Settings;

/**
 * Standard command actions supported by Strange Eons.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Commands {

    private Commands() {
    }

    ///////////////////
    // FILE COMMANDS //////////////////////////////////////////////////////////
    ///////////////////
    /**
     * Displays a dialog that allows the user to create a new game component.
     */
    public static final AbstractCommand NEW_GAME_COMPONENT = new AbstractCommand("app-new", "toolbar/new.png") {
        @Override
        public void actionPerformed(ActionEvent e) {
            NewEditorDialog ned = NewEditorDialog.getSharedInstance();
            ned.setVisible(true);
            ned.toFront();
        }
    };
    /**
     * Displays a dialog that allows the user to create a new project.
     */
    public static final AbstractCommand NEW_PROJECT = new HProxyCommand("app-new-project");
    /**
     * Displays a dialog that allows the user to open game components.
     */
    public static final AbstractCommand OPEN = new HProxyCommand("app-open", "toolbar/open.png");
    /**
     * Displays a dialog that allows the user to open a project.
     */
    public static final AbstractCommand OPEN_PROJECT = new AbstractCommand("app-open-project") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StrangeEonsAppWindow w = StrangeEons.getWindow();
            w.setWaitCursor();
            try {
                File f = ResourceKit.showProjectFolderDialog(w);
                if (f == null) {
                    return;
                }
                w.setOpenProject(f);
            } finally {
                w.setDefaultCursor();
            }
        }
    };
    /**
     * Closes the active editor. If the editor has unsaved changes, the user
     * will be given the option to save the file or cancel the command.
     */
    public static final AbstractCommand CLOSE = new AbstractCommand("app-close") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StrangeEonsEditor ed = StrangeEons.getActiveEditor();
            if (ed != null) {
                ed.close();
            }
        }

        @Override
        public void update() {
            setEnabled(StrangeEons.getActiveEditor() != null);
        }
    };
    /**
     * Closes the open project, if any.
     */
    public static final AbstractCommand CLOSE_PROJECT = new AbstractCommand("app-close-project") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StrangeEonsAppWindow w = StrangeEons.getWindow();
            w.closeProject();
        }
    };
    /**
     * Saves the current document in editors that support this command.
     */
    public static final DelegatedCommand SAVE = new DelegatedCommand("app-save", "toolbar/save.png");
    /**
     * Saves the current document under a new name in editors that support this
     * command.
     */
    public static final DelegatedCommand SAVE_AS = new DelegatedCommand("app-saveas");
    /**
     * Saves every open editor that supports the {@link #SAVE} command and has
     * unsaved changes.
     */
    public static final AbstractCommand SAVE_ALL = new AbstractCommand("app-saveall") {
        @Override
        public void update() {
            boolean enable = false;
            for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditors()) {
                // if the command is applicable, it should have unsaved changes, but
                // we are playing it safe here
                if (ed.isCommandApplicable(Commands.SAVE) && ed.hasUnsavedChanges()) {
                    enable = true;
                    break;
                }
            }
            setEnabled(enable);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditors()) {
                if ((!ed.isCommandApplicable(Commands.SAVE))) {
                    continue;
                }
                if (!ed.hasUnsavedChanges()) {
                    continue;
                }
                ed.save();
            }
        }
    };
    /**
     * Exports content from editors that support this command.
     */
    public static final DelegatedCommand EXPORT = new DelegatedCommand("app-export");
    /**
     * Prints content from editors that support this command.
     */
    public static final DelegatedCommand PRINT = new DelegatedCommand("app-print", "toolbar/print.png");
    /**
     * Exits the application after giving the user a chance to save open files
     * with unsaved changes.
     */
    public static final AbstractCommand EXIT = new AbstractCommand("app-exit") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StrangeEons.getWindow().exitApplication(false);
        }
    };

    ///////////////////
    // EDIT COMMANDS //////////////////////////////////////////////////////////
    ///////////////////
    /**
     * Adds a clone of the current document to the application.
     */
    public static final DelegatedCommand SPIN_OFF = new DelegatedCommand("app-clone");
    /**
     * Clears all content from editors that support this command.
     */
    public static final DelegatedCommand CLEAR = new DelegatedCommand("app-clear");
    /**
     * A {@link DelegatedCommand} for the clipboard cut operation. The default
     * action will attempt to perform a cut in the active component, or your
     * editor can handle the command itself.
     */
    public static final DelegatedCommand CUT = new HClipboardCommand("app-cut", "toolbar/cut.png");
    /**
     * A {@link DelegatedCommand} for the clipboard copy operation. The default
     * action will attempt to perform a copy in the active component, or your
     * editor can handle the command itself.
     */
    public static final DelegatedCommand COPY = new HClipboardCommand("app-copy", "toolbar/copy.png");
    /**
     * A {@link DelegatedCommand} for the clipboard paste operation. The default
     * action will attempt to perform a paste in the active component, or your
     * editor can handle the command itself.
     */
    public static final DelegatedCommand PASTE = new HClipboardCommand("app-paste", "toolbar/paste.png");
    /**
     * A {@link DelegatedCommand} for selecting all content in a component that
     * supports selection. The default action will attempt to perform a select
     * all in the active component, (such as a text field or a list that
     * supports multiple selection) or your editor can handle the command
     * itself.
     */
    public static final DelegatedCommand SELECT_ALL = new HClipboardCommand("app-select-all");
    /**
     * Activates a search tool in an editor that supports this command.
     */
    public static final DelegatedCommand FIND = new DelegatedCommand("app-find");
    /**
     * Activates the find in project field, making it visible if necessary.
     */
    public static final AbstractCommand FIND_IN_PROJECT = new AbstractCommand("app-find-project") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ProjectView v = StrangeEons.getWindow().getOpenProjectView();
            if (v == null) {
                return;
            }
            v.setFindInProjectsVisible(true);
        }

        @Override
        public void update() {
            setEnabled(StrangeEons.getOpenProject() != null);
        }
    };
    /**
     * Shows the <b>Preferences</b> dialog using the default location and
     * category.
     *
     * @see StrangeEonsAppWindow#showPreferencesDialog
     */
    public static final AbstractCommand PREFERENCES = new AbstractCommand("app-settings") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StrangeEons.getWindow().showPreferencesDialog(null, null);
        }
    };

    ///////////////////
    // VIEW COMMANDS //////////////////////////////////////////////////////////
    ///////////////////

    /**
     * Enables and disables Ink Saver mode.
     */
    public static final AbstractToggleCommand VIEW_INK_SAVER = new AbstractToggleCommand("app-ink-saver", "ui/view/ink-saver.png") {
        {
            setSelected(Settings.getShared().getYesNo("render-as-prototype"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            final boolean enable = isSelected();
            StrangeEonsEditor[] ed = StrangeEons.getWindow().getEditors();
            for(int i=0; i<ed.length; ++i) {
                if(ed[i].isCommandApplicable(this)) {
                    try {
                        ed[i].performCommand(this);
                    } catch(Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
            Settings.getUser().set("render-as-prototype", enable ? "yes" : "no");
        }
    };


    /**
     * Toggles visibility of the context bar.
     */
    public static final AbstractToggleCommand VIEW_CONTEXT_BAR = new AbstractToggleCommand("app-context-bar", "ui/view/contextbar.png") {
        {
            setSelected(Settings.getShared().getYesNo("show-context-bar"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final boolean enable = isSelected();
            ContextBar.getShared().setEnabled(enable);
            Settings.getUser().set("show-context-bar", enable ? "yes" : "no");
        }
    };
    /**
     * Toggles visibility of drag handles in decks.
     */
    public static final AbstractToggleCommand VIEW_DECK_HANDLES = new HViewToggleCommand("app-deck-handles", "toolbar/view-handles.png", ViewOptions.class, "DragHandlePainted");
    /**
     * Toggles visibility of snapping grid in decks.
     */
    public static final AbstractToggleCommand VIEW_DECK_GRID = new HViewToggleCommand("app-deck-grid", "toolbar/view-grid.png", ViewOptions.class, "GridPainted");
    /**
     * Toggles visibility of page margins in decks.
     */
    public static final AbstractToggleCommand VIEW_DECK_MARGIN = new HViewToggleCommand("app-deck-margin", "toolbar/view-margin.png", ViewOptions.class, "MarginPainted");
    /**
     * Toggles visibility of the source code navigator.
     */
    public static final AbstractToggleCommand VIEW_SOURCE_NAVIGATOR = new AbstractToggleCommand("app-source-nav") {
        {
            setSelected(CodeEditor.isNavigatorVisible());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CodeEditor.setNavigatorVisible(isSelected());
        }
    };
    /**
     * Toggles visibility of region boxes on game component previews, which help
     * when debugging component layouts.
     */
    public static final AbstractToggleCommand VIEW_REGION_BOXES = new AbstractToggleCommand("app-show-regions") {
        {
            final boolean debug = Settings.getShared().getYesNo(KEY_SHOW_DEBUG_BOXES);
            MarkupRenderer.DEBUG = debug;
            setSelected(debug);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final boolean debug = isSelected();
            MarkupRenderer.DEBUG = debug;
            Settings.getUser().set(KEY_SHOW_DEBUG_BOXES, debug ? "yes" : "no");
            StrangeEons.getWindow().redrawPreviews();
        }
        private static final String KEY_SHOW_DEBUG_BOXES = "show-debug-boxes";
    };

    /**
     * Creates a new expansion symbol.
     */
    public static final DelegatedCommand EXPANSION_NEW = new DelegatedCommand("app-create-exp") {
        @Override
        public boolean isDefaultActionApplicable() {
            return true;
        }

        @Override
        public void performDefaultAction(ActionEvent e) {
            EndUserExpansion d = new EndUserExpansion(StrangeEons.getWindow());
            Object src = e.getSource();
            d.setLocationRelativeTo(src instanceof Component ? (Component) src : StrangeEons.getWindow());
            d.setVisible(true);
        }
    };
    /**
     * Copies the expansion symbol on the current component.
     */
    public static final DelegatedCommand EXPANSION_COPY = new HExpClipCommand(true);
    /**
     * Pastes the last-copies expansion symbol onto the current component.
     */
    public static final DelegatedCommand EXPANSION_PASTE = new HExpClipCommand(false);
    /**
     * Opens an {@link ExpansionSelectionDialog} for the active game component
     * editor.
     */
    public static final DelegatedCommand EXPANSION_CHOOSE = new DelegatedCommand("app-exp-choose") {
        @Override
        public boolean isDefaultActionApplicable() {
            return StrangeEons.getActiveGameComponent() != null;
        }

        @Override
        public void performDefaultAction(ActionEvent e) {
            StrangeEonsEditor ed = StrangeEons.getActiveEditor();
            if (ed instanceof AbstractGameComponentEditor) {
                ExpansionSelectionDialog d = new ExpansionSelectionDialog((AbstractGameComponentEditor) ed);
                d.setVisible(true);
            }
        }
    };

    /**
     * Opens a dialog that allows the user to modify the paragraph attributes of
     * a block of markup text.
     */
    public static final DelegatedCommand MARKUP_ALIGNMENT = new HMarkupCommand("app-paragraph", "toolbar/paragraph.png") {
        @Override
        protected void insertMarkup(ActionEvent e, MarkupTarget mt) {
            ParagraphDialog d = new ParagraphDialog(StrangeEons.getWindow());
            setDialogLocation(d, e, mt);
            d.showDialog((JComponent) mt.getTarget());
        }
    };
    /**
     * Opens a dialog that allows the user to modify the paragraph attributes of
     * a block of markup text.
     */
    public static final DelegatedCommand MARKUP_INSERT_COLOUR = new HMarkupCommand("app-insert-colour", "toolbar/colour.png") {
        @Override
        protected void insertMarkup(ActionEvent e, MarkupTarget mt) {
            ColourDialog d = new ColourDialog(StrangeEons.getWindow(), true);
            setDialogLocation(d, e, mt);
            d.setSelectedColor(lastInsertedColor);
            d.setVisible(true);
            Color c = d.getSelectedColor();

            if (c != null) {
                lastInsertedColor = c;
                String hex;
                if (c.getAlpha() < 255) {
                    hex = String.format("%08x", c.getRGB());
                } else {
                    hex = String.format("%06x", c.getRGB() & 0x00ff_ffff);
                }
                String colour = (Locale.getDefault().getCountry().equals(Locale.US)) ? "color" : "colour";
                mt.tagSelectedText("<" + colour + " #" + hex + ">", "</" + colour + ">", false);
            }
        }
        private Color lastInsertedColor = Color.BLACK;
    };
    /**
     * Opens a dialog that allows the user to modify the font attributes of a
     * block of markup text.
     */
    public static final DelegatedCommand MARKUP_INSERT_FONT = new HMarkupCommand("app-insert-font", "toolbar/font.png") {
        @Override
        protected void insertMarkup(ActionEvent e, MarkupTarget mt) {
            FontFormatDialog d = new FontFormatDialog(StrangeEons.getWindow(), true);
            setDialogLocation(d, e, mt);
            String[] markup = d.showDialog();
            if (markup != null) {
                mt.tagSelectedText(markup[0], markup[1], false);
            }
        }
    };
    /**
     * Inserts or removes bold tags around the selected text.
     */
    public static final DelegatedCommand MARKUP_BOLD = new HMarkupCommand("app-bold", "toolbar/bold.png", "<b>", "</b>");
    /**
     * Inserts or removes italic tags around the selected text.
     */
    public static final DelegatedCommand MARKUP_ITALIC = new HMarkupCommand("app-italic", "toolbar/italic.png", "<i>", "</i>");
    /**
     * Inserts or removes underline tags around the selected text.
     */
    public static final DelegatedCommand MARKUP_UNDERLINE = new HMarkupCommand("app-underline", "toolbar/underline.png", "<u>", "</u>");
    /**
     * Inserts or removes strikethrough tags around the selected text.
     */
    public static final DelegatedCommand MARKUP_STRIKETHROUGH = new HMarkupCommand("app-strikethrough", "toolbar/strikethrough.png", "<del>", "</del>");
    /**
     * Inserts or removes superscript tags around the selected text.
     */
    public static final DelegatedCommand MARKUP_SUPERSCRIPT = new HMarkupCommand("app-superscript", "toolbar/superscript.png", "<sup>", "</sup>");
    /**
     * Inserts or removes subscript tags around the selected text.
     */
    public static final DelegatedCommand MARKUP_SUBSCRIPT = new HMarkupCommand("app-subscript", "toolbar/subscript.png", "<sub>", "</sub>");
    /**
     * Inserts or removes heading tags around the selected text.
     */
    public static final DelegatedCommand MARKUP_HEADING = new HMarkupCommand("app-h1", "toolbar/h1.png", "<h1>", "</h1>");
    /**
     * Inserts or removes subheading tags around the selected text.
     */
    public static final DelegatedCommand MARKUP_SUBHEADING = new HMarkupCommand("app-h2", "toolbar/h2.png", "<h2>", "</h2>");
    /**
     * Opens a dialog that allows the user to select an image, a tag for which
     * will be inserted into the markup target.
     */
    public static final DelegatedCommand MARKUP_INSERT_IMAGE = new HMarkupCommand("app-insert-image", "toolbar/picture.png") {
        @Override
        protected void insertMarkup(ActionEvent e, MarkupTarget mt) {
            if (insertImageDlg == null) {
                insertImageDlg = new InsertImageDialog(StrangeEons.getWindow(), true);
            }
            setDialogLocation(insertImageDlg, e, mt);
            insertImageDlg.setVisible(true);
            String markup = insertImageDlg.getMarkupString();
            if (markup != null) {
                mt.setSelectedText(markup);
            }
        }
        private InsertImageDialog insertImageDlg;
    };
    /**
     * Opens a dialog that allows the user to select an image, a tag for which
     * will be inserted into the markup target.
     */
    public static final DelegatedCommand MARKUP_INSERT_CHARACTERS = new HMarkupCommand("app-insert-chars", "toolbar/symbol.png") {
        @Override
        protected void insertMarkup(ActionEvent e, MarkupTarget mt) {
            InsertCharsDialog d = new InsertCharsDialog(StrangeEons.getWindow(), true);
            setDialogLocation(d, e, mt);
            d.setVisible(true);

            String text = d.getSelectedText();
            if (text != null) {
                if (!(mt.getTarget() instanceof JSourceCodeEditor) && (text.indexOf('>') >= 0 || text.indexOf('<') >= 0)) {
                    StringBuilder b = new StringBuilder(text.length() + 16);
                    for (int i = 0; i < text.length(); ++i) {
                        char ch = text.charAt(i);
                        if (ch == '<') {
                            b.append("<lt>");
                        } else if (ch == '>') {
                            b.append("<gt>");
                        } else {
                            b.append(ch);
                        }
                    }
                    text = b.toString();
                }

                mt.setSelectedText(text);
            }
        }
    };
    /**
     * Displays the markup abbreviation table editor.
     */
    public static final AbstractCommand MARKUP_ABBREVIATIONS = new HAbbrevTable(true);

    /**
     * Moves the selected objects in front of other objects in a deck.
     */
    public static final DelegatedCommand TO_FRONT = new HDeckCommand("app-to-front", "toolbar/move-front.png", 1) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            ed.getDeck().moveSelectionToFront();
        }
    }.key('F');
    /**
     * Moves the selected objects behind other objects in a deck.
     */
    public static final DelegatedCommand TO_BACK = new HDeckCommand("app-to-back", "toolbar/move-back.png", 1) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            ed.getDeck().moveSelectionToBack();
        }
    }.key('B');
    /**
     * Groups together the selected objects.
     */
    public static final DelegatedCommand GROUP = new HDeckCommand("app-group", "toolbar/group.png", 2) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            ed.getDeck().groupSelection();
        }
    }.key('G');
    /**
     * Breaks up the selected group into separate objects.
     */
    public static final DelegatedCommand UNGROUP = new HDeckCommand("app-ungroup", "toolbar/ungroup.png", 2) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            ed.getDeck().ungroupSelection();
        }

        @Override
        public boolean isDefaultActionApplicable() {
            if (super.isDefaultActionApplicable()) {
                for (PageItem pi : getDeckEditor().getDeck().getSelection()) {
                    if (pi.getGroup() != null) {
                        return true;
                    }
                }
            }
            return false;
        }
    }.key('U');
    /**
     * Aligns the selected objects to the left edge of the most recently
     * selected object.
     */
    public static final DelegatedCommand ALIGN_LEFT = new HDeckCommand("app-align-left", "toolbar/al-left.png", 2) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            PageItem[] sel = ed.getDeck().getSelection();
            double x = sel[sel.length - 1].getX();
            for (int i = 0; i < sel.length - 1; ++i) {
                sel[i].setX(x);
            }
            ed.getActivePageView().repaint();
        }
    };
    /**
     * Aligns the selected objects to the horizontal centre of the most recently
     * selected object.
     */
    public static final DelegatedCommand ALIGN_CENTER = new HDeckCommand("app-align-centerh", "toolbar/al-center.png", 2) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            PageItem[] sel = ed.getDeck().getSelection();
            double x = sel[sel.length - 1].getX() + sel[sel.length - 1].getWidth() / 2d;
            for (int i = 0; i < sel.length - 1; ++i) {
                sel[i].setX(x - sel[i].getWidth() / 2d);
            }
            ed.getActivePageView().repaint();
        }
    };
    /**
     * Aligns the selected objects to the right edge of the most recently
     * selected object.
     */
    public static final DelegatedCommand ALIGN_RIGHT = new HDeckCommand("app-align-right", "toolbar/al-right.png", 2) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            PageItem[] sel = ed.getDeck().getSelection();
            double x = sel[sel.length - 1].getX() + sel[sel.length - 1].getWidth();
            for (int i = 0; i < sel.length - 1; ++i) {
                sel[i].setX(x - sel[i].getWidth());
            }
            ed.getActivePageView().repaint();
        }
    };
    /**
     * Aligns the selected objects to the top edge of the most recently selected
     * object.
     */
    public static final DelegatedCommand ALIGN_TOP = new HDeckCommand("app-align-top", "toolbar/al-top.png", 2) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            PageItem[] sel = ed.getDeck().getSelection();
            double y = sel[sel.length - 1].getY();
            for (int i = 0; i < sel.length - 1; ++i) {
                sel[i].setY(y);
            }
            ed.getActivePageView().repaint();
        }
    };
    /**
     * Aligns the selected objects to the vertical middle of the most recently
     * selected object.
     */
    public static final DelegatedCommand ALIGN_MIDDLE = new HDeckCommand("app-align-centerv", "toolbar/al-middle.png", 2) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            PageItem[] sel = ed.getDeck().getSelection();
            double y = sel[sel.length - 1].getY() + sel[sel.length - 1].getHeight() / 2d;
            for (int i = 0; i < sel.length - 1; ++i) {
                sel[i].setY(y - sel[i].getHeight() / 2d);
            }
            ed.getActivePageView().repaint();
        }
    };
    /**
     * Aligns the selected objects to the bottom edge of the most recently
     * selected object.
     */
    public static final DelegatedCommand ALIGN_BOTTOM = new HDeckCommand("app-align-bottom", "toolbar/al-bottom.png", 2) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            PageItem[] sel = ed.getDeck().getSelection();
            double y = sel[sel.length - 1].getY() + sel[sel.length - 1].getHeight();
            for (int i = 0; i < sel.length - 1; ++i) {
                sel[i].setY(y - sel[i].getHeight());
            }
            ed.getActivePageView().repaint();
        }
    };
    /**
     * Distributes the selected objects evenly across the horizontal axis.
     */
    public static final DelegatedCommand DISTRIBUTE_HORZ = new HDeckCommand("app-dist-horz", "toolbar/dist-horz.png", 3) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            PageItem[] sel = ed.getDeck().getSelection();
            if (sel.length < 3) {
                return;
            }

            Arrays.sort(sel, (PageItem o1, PageItem o2) -> {
                double cmp = o1.getX() - o2.getX();
                return (cmp == 0d) ? (0) : (cmp < 0d ? -1 : 1);
            });

            final int last = sel.length - 1;
            double span = (sel[last].getX() + sel[last].getWidth()) - sel[0].getX();
            double space = span;
            for (int i = 0; i <= last; ++i) {
                space -= sel[i].getWidth();
            }
            space /= last;
            double x = sel[0].getX();
            for (int i = 0; i <= last; ++i) {
                sel[i].setX(x);
                x += space + sel[i].getWidth();
            }
            ed.getActivePageView().repaint();
        }
    };
    /**
     * Distributes the selected objects evenly across the vertical axis.
     */
    public static final DelegatedCommand DISTRIBUTE_VERT = new HDeckCommand("app-dist-vert", "toolbar/dist-vert.png", 3) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            PageItem[] sel = ed.getDeck().getSelection();
            if (sel.length < 3) {
                return;
            }

            Arrays.sort(sel, (PageItem o1, PageItem o2) -> {
                double cmp = o1.getY() - o2.getY();
                return (cmp == 0d) ? (0) : (cmp < 0d ? -1 : 1);
            });

            final int last = sel.length - 1;
            double span = (sel[last].getY() + sel[last].getHeight()) - sel[0].getY();
            double space = span;
            for (int i = 0; i <= last; ++i) {
                space -= sel[i].getHeight();
            }
            space /= last;
            double y = sel[0].getY();
            for (int i = 0; i <= last; ++i) {
                sel[i].setY(y);
                y += space + sel[i].getHeight();
            }
            ed.getActivePageView().repaint();
        }
    };
    /**
     * Turns the deck selection left.
     */
    public static final DelegatedCommand TURN_LEFT = new HDeckCommand("app-turn-left", "toolbar/rotate-left.png", 1) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            ed.getDeck().turnSelectionLeft();
        }
    }.key('L');
    /**
     * Turns the deck selection right.
     */
    public static final DelegatedCommand TURN_RIGHT = new HDeckCommand("app-turn-right", "toolbar/rotate-right.png", 1) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            ed.getDeck().turnSelectionRight();
        }
    }.key('R');
    /**
     * Turns the deck selection 180 degrees.
     */
    public static final DelegatedCommand TURN_180 = new HDeckCommand("app-turn-over", 1) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            Deck d = ed.getDeck();
            d.beginCompoundEdit();
            try {
                d.turnSelectionLeft();
                d.turnSelectionLeft();
            } finally {
                d.endCompoundEdit();
            }
        }
    };
    /**
     * Mirrors the deck selection horizontally.
     */
    public static final DelegatedCommand FLIP_HORZ = new HDeckCommand("app-mirror", "toolbar/mirror.png", 1) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            ed.getDeck().flipSelection();
        }
    }.key('M');
    /**
     * Mirrors the deck selection vertically.
     */
    public static final DelegatedCommand FLIP_VERT = new HDeckCommand("app-mirror-v", "toolbar/mirror-vert.png", 1) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            DeckEditor ed = getDeckEditor();
            if (ed == null) {
                return;
            }
            Deck d = ed.getDeck();
            d.beginCompoundEdit();
            try {
                d.turnSelectionLeft();
                d.turnSelectionLeft();
                d.flipSelection();
            } finally {
                d.endCompoundEdit();
            }
        }
    }.key('V');
    /**
     * Centers all content on a deck page.
     */
    public static final DelegatedCommand CENTER_CONTENT = new HDeckCommand("app-center-content", 0) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            if (!isDefaultActionApplicable()) {
                return;
            }
            DeckEditor ed = getDeckEditor();
            Page pg = ed.getDeck().getActivePage();
            if (pg != null) {
                pg.centerContent();
            }
        }
    };

    /**
     * Locks the selected deck items.
     */
    public static final DelegatedCommand LOCK = new HDeckCommand("app-lock", "toolbar/lock.png", 1) {
        @Override
        public boolean isDefaultActionApplicable() {
            if (super.isDefaultActionApplicable()) {
                PageItem[] sel = getSelection();
                for (PageItem i : getSelection()) {
                    if (!i.isSelectionLocked()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void performDefaultAction(ActionEvent e) {
            if (isDefaultActionApplicable()) {
                PageItem[] sel = getSelection();
                for (PageItem i : getSelection()) {
                    i.setSelectionLocked(true);
                }
            }
        }
    }.key('C');

    /**
     * Locks the selected deck items.
     */
    public static final DelegatedCommand UNLOCK = new HDeckCommand("app-unlock", "toolbar/unlock.png", 1) {
        @Override
        public boolean isDefaultActionApplicable() {
            if (super.isDefaultActionApplicable()) {
                PageItem[] sel = getSelection();
                for (PageItem i : getSelection()) {
                    if (i.isSelectionLocked()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void performDefaultAction(ActionEvent e) {
            if (isDefaultActionApplicable()) {
                PageItem[] sel = getSelection();
                for (PageItem i : getSelection()) {
                    i.setSelectionLocked(false);
                }
            }
        }
    }.key('O');

    /**
     * Unlocks all locked deck items.
     */
    public static final DelegatedCommand UNLOCK_ALL = new HDeckCommand("app-unlockall", 0) {
        @Override
        public boolean isDefaultActionApplicable() {
            if (super.isDefaultActionApplicable()) {
                PageItem[] sel = getSelection();
                for (PageItem i : getSelection()) {
                    if (i.isSelectionLocked()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void performDefaultAction(ActionEvent e) {
            if (isDefaultActionApplicable()) {
                PageItem[] sel = getSelection();
                for (PageItem i : getSelection()) {
                    i.setSelectionLocked(false);
                }
            }
        }
    };

    /**
     * Edits the selected an item in a deck; if the selection has more than one
     * object in it, only the last-selected item is considered.
     */
    public static final DelegatedCommand EDIT_PAGE_ITEM = new HDeckCommand("app-edit-item", 1) {
        @Override
        public boolean isDefaultActionApplicable() {
            if (super.isDefaultActionApplicable()) {
                return getTarget() != null;
            }
            return false;
        }

        @Override
        public void performDefaultAction(ActionEvent e) {
            if (getDeckEditor() == null) {
                return;
            }
            EditablePageItem target = getTarget();
            if (target != null) {
                target.beginEditing();
            }
        }

        private EditablePageItem getTarget() {
            PageItem[] items = getSelection();
            if (items.length < 1) {
                return null;
            }
            PageItem target = items[items.length - 1];
            return target instanceof EditablePageItem ? (EditablePageItem) target : null;
        }
    }.key("ENTER");
    /**
     * Opens the style editor for the selected page items in a deck.
     */
    public static final DelegatedCommand EDIT_STYLE = new HDeckCommand("app-style", 1) {
        @Override
        public boolean isDefaultActionApplicable() {
            if (super.isDefaultActionApplicable()) {
                return StyleEditor.selectionHasStyledItems(getSelection());
            }
            return false;
        }

        @Override
        public void performDefaultAction(ActionEvent e) {
            if (!isDefaultActionApplicable()) {
                return;
            }
            DeckEditor deckEd = getDeckEditor();
            PageItem[] sel = deckEd.getDeck().getSelection();
            StyleEditor ed = new StyleEditor(StrangeEons.getWindow(), true);
            ed.initializeForSelection(sel);
            PointerInfo pi = MouseInfo.getPointerInfo();
            if (pi != null) {
                Point mp = pi.getLocation();
                mp.x -= ed.getWidth() / 2;
                mp.y -= ed.getHeight() / 2;
                ed.setLocation(mp);
            } else {
                ed.setLocationRelativeTo(deckEd);
            }
            JUtilities.snapToDesktop(ed);
            ed.setVisible(true);
        }
    }.key("SLASH");
    /**
     * Copy the style information for the current selection into the global
     * style clipboard.
     */
    public static final DelegatedCommand COPY_STYLE = new HDeckCommand("app-copy-style", 1) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            if (!isDefaultActionApplicable()) {
                return;
            }
            new StyleCapture(getSelection()).copy();
        }

        @Override
        public boolean isDefaultActionApplicable() {
            return Commands.EDIT_STYLE.isDefaultActionApplicable();
        }
    };
    /**
     * Apply the current global style clipboard style settings to the deck
     * selection.
     */
    public static final DelegatedCommand PASTE_STYLE = new HDeckCommand("app-paste-style", 1) {
        @Override
        public boolean isDefaultActionApplicable() {
            return StyleCapture.canPaste() && super.isDefaultActionApplicable();
        }

        @Override
        public void performDefaultAction(ActionEvent e) {
            if (!isDefaultActionApplicable()) {
                return;
            }
            StyleCapture.paste((Object[]) getSelection());
        }
    };
    /**
     * Inverts the current selection.
     */
    public static final DelegatedCommand SELECT_INVERSE = new HDeckCommand("app-inv-selection", 0) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            if (!isDefaultActionApplicable()) {
                return;
            }
            getDeckEditor().getDeck().invertSelection();
        }
    }.key('I');
    /**
     * Restores the previous selection in a deck.
     */
    public static final DelegatedCommand SELECT_RESTORE = new HDeckCommand("app-reselect", 0) {
        @Override
        public void performDefaultAction(ActionEvent e) {
            if (!isDefaultActionApplicable()) {
                return;
            }
            Deck deck = getDeckEditor().getDeck();
            deck.selectNumberedGroup(Deck.RESELECT_GROUP);
        }

        @Override
        public boolean isDefaultActionApplicable() {
            return super.isDefaultActionApplicable()
                    && getDeckEditor().getDeck().getSelectionGroupSize(Deck.RESELECT_GROUP) > 0;
        }
    }.key("shift R");

    /**
     * Runs the script in the active code editor.
     */
    public static final DelegatedCommand RUN_FILE = new HRunScriptCommand(false);
    /**
     * Debugs the script in the active code editor.
     */
    public static final DelegatedCommand DEBUG_FILE = new HRunScriptCommand(true);
    /**
     * Tests the bundle associated with the open editor file (or project view
     * selection).
     */
    public static final AbstractCommand TEST_BUNDLE = new HBundleCommand("app-test", "testbundle");
    /**
     * Makes the bundle associated with the open editor file (or project view
     * selection).
     */
    public static final AbstractCommand MAKE_BUNDLE = new HBundleCommand("app-make", "makebundle");
    /**
     * Moves the selected lines up in a code editor.
     */
    public static final DelegatedCommand MOVE_LINES_UP = new HSourceCommand("app-shift-up").forAction(EditorCommands.MOVE_SELECTION_UP).key("move-selection-up");
    /**
     * Moves the selected lines down in a code editor.
     */
    public static final DelegatedCommand MOVE_LINES_DOWN = new HSourceCommand("app-shift-down").forAction(EditorCommands.MOVE_SELECTION_DOWN).key("move-selection-down");
    /**
     * Comments out the selected lines in a code editor.
     */
    public static final DelegatedCommand COMMENT_OUT = new HSourceCommand("app-comment", "toolbar/comment-out.png").forComments().forAction(EditorCommands.COMMENT_SELECTION).key("comment-selection");
    /**
     * Uncomments the selected lines in a code editor, reversing a previously
     * performed {@link #COMMENT_OUT} command.
     */
    public static final DelegatedCommand UNCOMMENT = new HSourceCommand("app-uncomment", "toolbar/uncomment.png").forComments().forAction(EditorCommands.UNCOMMENT_SELECTION).key("uncomment-selection");

    /**
     * Removes trailing spaces from the ends of source lines.
     */
    public static final DelegatedCommand REMOVE_TRAILING_SPACES = new HSourceCommand("app-trim-right", "toolbar/trim-right.png") {
        @Override
        public void performDefaultAction(ActionEvent e) {
            final JSourceCodeEditor ed = getEditor();
            if (ed == null) {
                return;
            }
            try {
                ed.getDocument().beginCompoundEdit();
                int caretPos = ed.getCaretPosition();
                int caretLine = ed.getLineOfOffset(caretPos);
                int caretOffset = caretPos - ed.getLineStartOffset(caretLine);
                int topLine = ed.getFirstDisplayedLine();

                if (caretPos == ed.getMarkPosition()) {
                    ed.selectAll();
                }
                String[] lines = EditorCommands.getSelectedLineText(ed, true);
                for (int i = 0; i < lines.length; ++i) {
                    String line = lines[i];
                    int len = line.length();
                    while ((len > 0) && Character.isWhitespace(line.charAt(len - 1))) {
                        len--;
                    }
                    lines[i] = line.substring(0, len);
                }
                EditorCommands.setSelectedLineText(ed, lines, lines.length);

                // replace caret as close as possible to previous position
                caretOffset = Math.min(caretOffset, ed.getLineLength(caretLine));
                ed.setCaretPosition(ed.getLineStartOffset(caretLine) + caretOffset);
                ed.setFirstDisplayedLine(topLine);
            } finally {
                ed.getDocument().endCompoundEdit();
            }
        }
    };
    /**
     * Sorts selected lines in a code editor.
     */
    public static final DelegatedCommand SORT = new HSourceCommand("app-sort", "toolbar/sort.png") {
        @Override
        public void performDefaultAction(ActionEvent e) {
            CodeEditor ed = getCodeEditor();
            if (ed != null) {
                ed.sortSelectedLines();
            }
        }
    }.forCodeEditor();

    /**
     * Plays the macro that was most recently recorded in a code editor.
     */
    public static final DelegatedCommand PLAY_MACRO = new HSourceCommand("app-play-macro", "toolbar/play-macro.png").forPlaying().forAction(EditorCommands.PLAY_LAST_MACRO).key("play-last-macro");
    /**
     * Starts recording a macro in the code editor.
     */
    public static final DelegatedCommand START_RECORDING_MACRO = new HSourceCommand("app-record-macro").forAction(EditorCommands.BEGIN_MACRO).key("begin-macro");
    /**
     * Stops recording a macro in the code editor.
     */
    public static final DelegatedCommand STOP_RECORDING_MACRO = new HSourceCommand("app-stop-macro").forRecording().forAction(EditorCommands.END_MACRO).key("end-macro");
    /**
     * Opens the code completion popup in the current code editor, if available.
     */
    public static final DelegatedCommand COMPLETE_CODE = new HSourceCommand("app-code-complete", null, EditorCommands.COMPLETE_CODE) {
        @Override
        public boolean isDefaultActionApplicable() {
            if (super.isDefaultActionApplicable()) {
                JSourceCodeEditor ed = getEditor();
                Tokenizer t = ed.getTokenizer();
                if (t != null && t.getCodeCompleter() != null) {
                    return true;
                }
            }
            return false;
        }
    }.key("complete-code");
    /**
     * Displays the source code abbreviation table editor.
     */
    public static final AbstractCommand CODE_ABBREVIATIONS = new HAbbrevTable(false);

    //////////////////////
    // Toolbox Commands /////////////////////////////////////////////////////
    //////////////////////
    /**
     * Opens a dialog box that allows the user to configure update
     * notifications.
     */
    public static final AbstractCommand CONFIGURE_UPDATES = new AbstractCommand("app-plugin-updates") {
        @Override
        public void actionPerformed(ActionEvent e) {
            new ConfigureUpdatesDialog().setVisible(true);
        }
    };

    /**
     * Opens the plug-in catalog.
     */
    public static final AbstractCommand PLUGIN_CATALOG = new AbstractCommand("app-catalog") {
        @Override
        public void actionPerformed(ActionEvent e) {
            CatalogDialog d = new CatalogDialog(StrangeEons.getWindow());
            d.setVisible(true);
        }
    };

    /**
     * Opens the plug-in manager.
     */
    public static final AbstractCommand PLUGIN_MANAGER = new AbstractCommand("app-toolbox-manage") {
        @Override
        public void actionPerformed(ActionEvent e) {
            PluginManager d = new PluginManager();
            d.setVisible(true);
        }
    };

    ///////////////////
    // Help Commands ////////////////////////////////////////////////////////
    ///////////////////
    /**
     * Command that displays context-sensitive help. The command searches the
     * component tree of the control with focus. It looks for either a component
     * of type {@link JHelpButton JHelpButton} or else a component with the
     * client property {@link #HELP_CONTEXT_PROPERTY} set. If either of these is
     * found, the help page that they describe is displayed. If no suitable page
     * is found, then the User Manual will be displayed instead.
     *
     * <p>
     * The action command of the action event used to fire the command can be
     * the name of a help page to open, either the title of a Wiki article or a
     * URL. In this case, that page is opened directly instead of searching the
     * component tree.
     */
    public static final DelegatedCommand HELP = new HHelpCommand();

    /**
     * A client property that names a Web page that shows help for that part of
     * the interface. The property value should be a <code>String</code>. The
     * value can either be the name of a page in the Wiki or else the URL of a
     * Web page.
     *
     * @see #HELP
     */
    public static final String HELP_CONTEXT_PROPERTY = "se-help-context";

    /**
     * Opens the User Manual for browsing.
     */
    public static final AbstractCommand HELP_USER_MANUAL = new HDocPageCommand("app-user-manual", "um-index");

    /**
     * Opens the Developer Manual for browsing.
     */
    public static final AbstractCommand HELP_DEV_MANUAL = new HDocPageCommand("app-dev-manual", "dm-index");

    /**
     * Opens the Translation Manual for browsing.
     */
    public static final AbstractCommand HELP_TRANSLATOR_MANUAL = new HDocPageCommand("app-translator-manual", "tm-index");

    /**
     * Opens the JS API documentation for browsing.
     */
    public static final AbstractCommand HELP_DEV_JS_API = new HDocPageCommand("app-dev-jsapi", "https://cgjennings.github.io/se3docs/assets/jsdoc/");

    /**
     * Opens the Java API documentation for browsing.
     */
    public static final AbstractCommand HELP_DEV_JAVA_API = new HDocPageCommand("app-dev-javaapi", "https://cgjennings.github.io/se3docs/assets/javadoc/");

    /**
     * Files a bug report with no specific message or exception information.
     *
     * @see StrangeEons#fileBugReport
     */
    public static final AbstractCommand FILE_BUG_REPORT = new HFileBugReport();

    /**
     * Displays the About dialog.
     *
     * @see StrangeEonsAppWindow#showAboutDialog()
     */
    public static final AbstractCommand ABOUT = new AbstractCommand("app-about", "application/16.png") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StrangeEons.getWindow().showAboutDialog();
        }
    };

    /**
     * Returns a command handler that is currently able to handle this command
     * by following a default search strategy. The exact strategy used to find a
     * relevant {@link Commandable} may change in future versions, but it is
     * guaranteed that:
     * <ul>
     * <li> any registered handlers will be consulted first (in order of
     * registration)
     * <li> the active editor (if any) will be consulted after any registered
     * handlers
     * <li> the main application window will be consulted last
     * </ul>
     * The first consulted command handler for which the command is currently
     * applicable will be returned, or if no handler is found, <code>null</code>
     * is returned instead.
     *
     * <p>
     * Note that if you wish to create a command that is delegated to a
     * {@link Commandable} that can be discovered through this method, it is
     * sufficient to subclass {@link DelegatedCommand}.
     *
     * @param command the command to find a handler for; typically this is an
     * instance of {@link DelegatedCommand}
     * @return a command handler that can currently handle the command, or
     * <code>null</code> if none can be found
     * @throws NullPointerException if the command is <code>null</code>
     */
    public static Commandable findCommandable(AbstractCommand command) {
        if (command == null) {
            throw new NullPointerException("command");
        }

        // check commandables
        synchronized (Commands.class) {
            if (registry != null) {
                for (Commandable c : registry) {
                    if (c.canPerformCommand(command) && c.isCommandApplicable(command)) {
                        return c;
                    }
                }
            }
        }

        // check editor, if any
        StrangeEonsAppWindow win = StrangeEons.getWindow();
        StrangeEonsEditor ed = win.getActiveEditor();
        if (ed != null && ed.isCommandApplicable(command)) {
            return ed;
        }

        // check app window
        if (win.isCommandApplicable(command)) {
            return win;
        }
        return null;
    }

    /**
     * Registers a new command handler to be consulted when trying to execute a
     * {@link DelegatedCommand}.
     *
     * @param commandable the command handler to consult
     * @throws NullPointerException if the commandable is <code>null</code>
     * @see #findCommandable
     * @see #unregisterCommandable
     */
    public synchronized static void registerCommandable(Commandable commandable) {
        if (commandable == null) {
            throw new NullPointerException("commandable");
        }
        if (registry == null) {
            registry = new LinkedHashSet<>();
        }
        registry.add(commandable);
    }

    /**
     * Unregisters a previously registered command handler.
     *
     * @param commandable the commandable to unregister
     * @see #registerCommandable
     */
    public synchronized static void unregisterCommandable(Commandable commandable) {
        if (registry != null) {
            registry.remove(commandable);
        }
    }

    private static LinkedHashSet<Commandable> registry;

    /**
     * Returns the item in a popup menu that will execute the specified command,
     * or <code>null</code> if the menu does not contain such an item.
     *
     * @param menu the menu to search
     * @param command the command to search for
     * @return the item within the menu tree that contains the command
     */
    public static JMenuItem findCommand(JPopupMenu menu, AbstractCommand command) {
        int count = menu.getComponentCount();
        for (int i = 0; i < count; ++i) {
            Component c = menu.getComponent(i);
            if (c instanceof JMenu) {
                JMenuItem match = findCommand((JMenu) c, command);
                if (match != null) {
                    return match;
                }
            } else if (c instanceof JMenuItem) {
                JMenuItem mi = (JMenuItem) c;
                if (mi.getAction() == command) {
                    return mi;
                }
            }
        }
        return null;
    }

    /**
     * Returns the item in a menu that will execute the specified command, or
     * <code>null</code> if the menu does not contain such an item.
     *
     * @param menu the menu to search
     * @param command the command to search for
     * @return the item within the menu tree that contains the command
     */
    public static JMenuItem findCommand(JMenu menu, AbstractCommand command) {
        int count = menu.getMenuComponentCount();
        for (int i = 0; i < count; ++i) {
            Component c = menu.getMenuComponent(i);
            if (c instanceof JMenu) {
                JMenuItem match = findCommand((JMenu) c, command);
                if (match != null) {
                    return match;
                }
            } else if (c instanceof JMenuItem) {
                JMenuItem mi = (JMenuItem) c;
                if (mi.getAction() == command) {
                    return mi;
                }
            }
        }
        return null;
    }

    /**
     * Returns the item in a menu bar that will execute the specified command,
     * or <code>null</code> if the menu does not contain such an item.
     *
     * @param menu the menu bar whose menus should be searched, or
     * <code>null</code> to use the main application window
     * @param command the command to search for
     * @return the item within the menu tree that contains the command
     */
    public static JMenuItem findCommand(JMenuBar menu, AbstractCommand command) {
        if (menu == null) {
            final StrangeEonsAppWindow app = StrangeEons.getWindow();
            if (app == null) {
                StrangeEons.log.warning("app window does not exist");
                return null;
            }
            menu = app.getJMenuBar();
        }

        int count = menu.getMenuCount();
        for (int i = 0; i < count; ++i) {
            JMenuItem match = findCommand(menu.getMenu(i), command);
            if (match != null) {
                return match;
            }
        }
        return null;
    }
}
