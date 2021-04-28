package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.*;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.commands.DelegatedCommand;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.item.AbstractFlippableItem;
import ca.cgjennings.apps.arkham.deck.item.CardFace;
import ca.cgjennings.apps.arkham.deck.item.DependentPageItem;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.io.FileChangeListener;
import ca.cgjennings.io.FileChangeMonitor;
import ca.cgjennings.io.FileChangeMonitor.ChangeType;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.spelling.ui.JSpellingTextArea;
import ca.cgjennings.spelling.ui.JSpellingTextField;
import ca.cgjennings.ui.JCloseableTabbedPane;
import ca.cgjennings.ui.JLabelledField;
import ca.cgjennings.ui.JReorderableTabbedPane;
import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.TabbedPaneReorderListener;
import ca.cgjennings.ui.dnd.AbstractDragAndDropHandler;
import ca.cgjennings.ui.dnd.DragManager;
import ca.cgjennings.ui.dnd.DragToken;
import ca.cgjennings.ui.dnd.FileDrop;
import gamedata.Game;
import gamedata.TileSet;
import gamedata.TileSet.Entry;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.print.PrintException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * Editor for decks of cards (collections of game components laid out on pages)
 * and custom expansion boards.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 1.62
 */
@SuppressWarnings("serial")
public class DeckEditor extends AbstractGameComponentEditor<Deck> implements Printable, TabbedPaneReorderListener, FileChangeListener {

    private void tc() {
        if (!EventQueue.isDispatchThread()) {
            Throwable t = new Throwable("*** NOT IN EDT ***");
            t.fillInStackTrace();
            t.printStackTrace(System.out);
        }
    }

    private Deck deck;

    /**
     * Number of special item tabs not used to display tiles,
     * i.e., tools and faces at positions 0 and 1.
     */
    private static final int RESERVED_TABS = 2;

    private PageItemList[] tileSetLists;

    /**
     * Creates a new deck editor with a new, empty {@link Deck} attached.
     */
    public DeckEditor() {
        this(new Deck());
    }

    /**
     * Creates a new deck editor for the specified {@link Deck}.
     *
     * @param d the deck to be edited
     */
    public DeckEditor(Deck d) {
        boolean wasSaved = !d.hasUnsavedChanges();
        initComponents();
        ((JLabelledField) findField).setLabel(string("de-l-search"));
        pageTab.addTab("", ResourceKit.getIcon("ui/deck/add-page.png"), new JPanel(), string("de-l-add-page"));
        optionLabel.setText(" ");
        localizeForPlatform();

        // Workaround:
        // Default preview splitter gets sized slightly too small
        Container content = getRootPane().getContentPane();
        for (int i = 0; i < content.getComponentCount(); ++i) {
            if ("previewSplitPane".equals(content.getComponent(i).getName())) {
                JSplitPane psp = (JSplitPane) content.getComponent(i);
                psp.setDividerLocation(psp.getDividerLocation() + 24);
            }
        }

        new FileDrop(this, lhsPanelSplitter, this::addFilesToCardList);

        pageTab.addTabbedPaneReorderListener(this);
        ((JCloseableTabbedPane) pageTab).setAutocloseEnabled(false);
        ((JCloseableTabbedPane) pageTab).addTabClosingListener((JCloseableTabbedPane source, int tab, boolean isDirty) -> {
            pageTab.setSelectedIndex(tab);
            removeCurrentPage();
        });

        DefaultComboBoxModel paperList = new DefaultComboBoxModel();
        paperSizeCombo.setModel(paperList);

        tileSetLists = new PageItemList[] {
            (PageItemList) toolsList, (PageItemList) facesList,
            (PageItemList) tilesList, (PageItemList) boardBitsList,
            (PageItemList) decorationsList, (PageItemList) otherList
        };
        facesListValueChanged(null); // disable the remove faces btn

        getRootPane().putClientProperty(Commands.HELP_CONTEXT_PROPERTY, "deck-intro");
        cropPanel.putClientProperty(Commands.HELP_CONTEXT_PROPERTY, "deck-pubmarks");

        setGameComponent(d);

        // workaround for longest game name being cut off
        Dimension pref = gameCombo.getPreferredSize();
        pref.width += 16;
        gameCombo.setPreferredSize(pref);

        reloadPaperCombo(true);

        Page page;
        if (deck.getPageCount() == 0) {
            page = addPage();
        } else {
            page = deck.getPage(0);
        }
        deck.setActivePage(page);

        populateFieldsFromComponent();

        JComponent[] nonMarkupFields = new JComponent[]{
            findField, cropWeightField, cropPrintWeightField,
            cropDistField, cropLengthField, nameField,
            commentField
        };
        for (JComponent nmf : nonMarkupFields) {
            if (nmf instanceof JSpinner) {
                nmf = ((JSpinner.DefaultEditor) ((JSpinner) nmf).getEditor()).getTextField();
            }
            MarkupTargetFactory.enableTargeting(nmf, false);
        }

        createTimer(150);

        // if the object palette did not exist before, this will create it
        // and connect it to our deck
        EventQueue.invokeLater(() -> {
            PropertyPalette.getShared();
            reloadToolsList();
            reloadTileSets();
        });

        if (wasSaved) {
            deck.markSaved();
        }
    }

    /**
     * Return the deck instance the editor controls for non-interactive editing.
     * 
     * @return the edited deck
     */
    public Deck getDeck() {
        return deck;
    }

    /**
     * Returns the active deck page, or <code>null</code>.
     *
     * @return the currently edited page
     */
    public Page getActivePage() {
        if (deck == null) {
            return null;
        }
        return deck.getActivePage();
    }

    /**
     * Returns the page view of the currently edited page, or <code>null</code>.
     *
     * @return the view component of the active page
     */
    public PageView getActivePageView() {
        Page p = getActivePage();
        return p != null ? p.getView() : null;
    }

    /**
     * Reloads the available paper types. The types are filtered to match the
     * game setting of the deck.
     *
     * @param selectDefaultPaperTypeInsteadOfRestoringSelection
     */
    private void reloadPaperCombo(boolean selectDefaultPaperTypeInsteadOfRestoringSelection) {
        PaperProperties sel = (PaperProperties) paperSizeCombo.getSelectedItem();

        Set<PaperProperties> papers = deck.getPaperSizes();

        if (selectDefaultPaperTypeInsteadOfRestoringSelection) {
            if (deck == null || deck.getPaperProperties() == null) {
                sel = PaperSets.getDefaultPaper(papers);
            } else {
                sel = PaperSets.findBestPaper(deck.getPaperProperties(), papers);
            }
        } else {
            sel = PaperSets.findBestPaper(sel, papers);
        }
        paperSizeCombo.setModel(PaperSets.setToComboBoxModel(papers));
        paperSizeCombo.setSelectedItem(sel);
    }

    /**
     * Reloads the tools list from the {@link Tools} registry.
     */
    private void reloadToolsList() {
        tc();
        Object sel = toolsList.getSelectedValue();
        DefaultListModel m = new DefaultListModel();
        for (PageItem proto : Tools.getRegisteredTools()) {
            m.addElement(proto);
        }
        toolsList.setModel(m);
        if (sel != null) {
            toolsList.setSelectedValue(sel, true);
        }
    }

    /**
     * Reloads the tile sets, filtering out any tiles that belong to other
     * games. Much of the work is deferred to another thread since drawing
     * thumbnails of all of the tiles can be time consuming.
     */
    private void reloadTileSets() {
        tc();
        // Verify that any previous thread is done ///////////
        Thread running = tileSetReloader;
        if (running != null && running.isAlive()) {
            running.interrupt();
            try {
                StrangeEons.log.log(Level.INFO, "Interrupting loader thread {0}", running);
                running.join(500);
            } catch (InterruptedException e) {}
            EventQueue.invokeLater(this::reloadTileSets);
            return;
        }
        //////////////////////////////////////////////////////

        // clear existing lists
        StrangeEons.log.info("Started reloading tile sets");
        final long startTime = System.nanoTime();

        final String[] tabNames = new String[]{
            "de-tab-tiles", "de-tab-decorations", "de-tab-board-pieces", "de-tab-misc"
        };
        final JScrollPane[] scrolls = new JScrollPane[]{
            tilesScroll, decorationsScroll, boardBitsScroll, otherScroll
        };
        final JList[] lists = new JList[]{
            tilesList, decorationsList, boardBitsList, otherList
        };
        final DefaultListModel[] models = new DefaultListModel[lists.length];
        final Object[] selections = new Object[lists.length];
        final LinkedList[] toLoad = new LinkedList[lists.length];
        for (int i = 0; i < selections.length; ++i) {
            selections[i] = lists[i].getSelectedValue();
            models[i] = new DefaultListModel();
            lists[i].setModel(models[i]);
            toLoad[i] = new LinkedList<>();
        }

        // find out what tiles we will be using and hide the tabs for any
        // empty lists
        String game = getDeck().getSettings().get(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE);
        for (Entry en : TileSet.getTileSetEntries()) {
            String forGame = en.getGameCode();
            if (forGame.equals(game) || forGame.equals(Game.ALL_GAMES_CODE)) {
                toLoad[en.getCategory().ordinal() - RESERVED_TABS].add(en);
            }
        }
        while (cardTileTab.getTabCount() > RESERVED_TABS) {
            cardTileTab.remove(RESERVED_TABS);
        }
        for (int i = 0; i < scrolls.length; ++i) {
            if (toLoad[i].size() > 0) {
                cardTileTab.addTab(string(tabNames[i]), scrolls[i]);
            }
        }

        // repopulate the tabs from another thread
        tileSetReloader = new Thread("Tile set reloader thread") {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < scrolls.length; ++i) {
                        while (toLoad[i].size() > 0) {
                            Entry en = (Entry) toLoad[i].removeFirst();
                            final int set = i;
                            final PageItem pi = en.getPrototypeItem();
                            StrangeEons.log.fine("Loading tile " + pi.getName());
                            try {
                                pi.getThumbnailIcon();
                                EventQueue.invokeLater(() -> {
                                    models[set].addElement(pi);
                                    if (selections[set] == pi) {
                                        lists[set].setSelectedValue(pi, true);
                                    }
                                });
                            } catch(Throwable t) {
                                StrangeEons.log.log(Level.SEVERE, "Exception while loading " + pi.getName(), t);
                                return;
                            }
                            // check if EDT asked us to stop; finally clause will set thread to null
                            if (Thread.interrupted()) {
                                StrangeEons.log.info("Tile set reload was interrupted");
                                return;
                            }
                        }
                    }
                    StrangeEons.log.log(Level.INFO, "Finished reloading tile sets in {0} s", (System.nanoTime() - startTime) / 1000000000d);
                } finally {
                    tileSetReloader = null;
                }
            }
        };
        tileSetReloader.start();
    }
    private volatile Thread tileSetReloader;

    /**
     * Creates an instance of the tile with the specified name.
     *
     * @param name the name of the tile to create (the first line of the tile's
     * entry in its tile set file)
     * @return a new instance of a tile with the specified name
     * @throws IllegalArgumentException if no tile with the requested name
     * exists
     */
    public PageItem createStandardTile(String name) {
        tc();
        if (name == null) {
            throw new NullPointerException("name");
        }

        Set<Entry> entries = TileSet.getTileSetEntries();

        // match against the original (unlocalized) tile name
        for (Entry entry : entries) {
            if (entry.getTileName().equals(name)) {
                return entry.getPrototypeItem().clone();
            }
        }

        // not found, try matching against localized names
        for (Entry entry : entries) {
            if (entry.getName().equals(name)) {
                return entry.getPrototypeItem().clone();
            }
        }

        throw new IllegalArgumentException("invalid tile name: " + name);
    }

    private void initAccelerators() {
        InputMap imap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap amap = getRootPane().getActionMap();

        registerKeyboardAction((ActionEvent e) -> {
            addPage();
        }, "", PlatformSupport.getKeyStroke("menu T"), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

//		addAccelerator( imap, amap, PlatformSupport.getKeyStroke( "menu T" ), "NEW TAB", new AbstractAction() {
//			@Override
//            public void actionPerformed( ActionEvent e ) {
//
//            }
//        } );
        addAccelerator(imap, amap, PlatformSupport.getKeyStroke("menu X"), "CUT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deck.cut();
            }
        });
        addAccelerator(imap, amap, PlatformSupport.getKeyStroke("menu C"), "COPY", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deck.copy();
            }
        });
        addAccelerator(imap, amap, PlatformSupport.getKeyStroke("menu V"), "PASTE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deck.paste();
            }
        });
        AbstractAction zoomOut = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deck.getActivePage().getView().adjustZoomBySteps(-1);
            }
        };
        addAccelerator(imap, amap, PlatformSupport.getKeyStroke("menu -"), "ZOOMOUT", zoomOut);
        addAccelerator(imap, amap, PlatformSupport.getKeyStroke("menu shift -"), "ZOOMOUT", zoomOut);

        AbstractAction zoomIn = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deck.getActivePage().getView().adjustZoomBySteps(+1);
            }
        };
        addAccelerator(imap, amap, PlatformSupport.getKeyStroke("menu +"), "ZOOMIN", zoomIn);
        addAccelerator(imap, amap, PlatformSupport.getKeyStroke("menu shift +"), "ZOOMIN", zoomIn);
    }

    private void addAccelerator(InputMap imap, ActionMap amap, KeyStroke key, String command, Action a) {
        imap.put(key, command);
        amap.put(command, a);
    }

//    private void installStandardTools() {
//        TextBox card = new TextBox();
//        card.setSize( 144d, 72d );
//        card.setText( string( "de-text-box-content" ) );
//        Line line = new Line();
//		Curve curve = new Curve();
//        CustomTile tile = new CustomTile( "", 75d );
//		TuckBox box = new TuckBox();
//
//        //for( int i = 0; i < tileSetLists.length; ++i ) {
//            DefaultListModel model = (DefaultListModel) tileSetLists[ SET_TOOLS ].getModel();
//            model.addElement( card );
//            model.addElement( tile );
//            model.addElement( line );
//			model.addElement( curve );
//			model.addElement( box );
//          //  ((PageItemRenderer) tileSetLists[i].getCellRenderer()).setReservedEntries( model.getSize() );
//        //}
//    }
    @Override
    public void tabbedPanesReordered(JReorderableTabbedPane source, int oldindex, int newindex) {
        deck.reorderPage(oldindex, newindex);
        updatePageTitles(0);
    }

    /**
     * Selects the paper closest to the specified dimensions of those currently
     * included in the deck's paper list. This method is provided as a
     * convenience for scripts that are setting up expansion boards. (Note that
     * if the paper is for a specific game, you may need to set that game's code
     * in the deck's private settings and reload the paper list before the
     * desired size will be available.)
     *
     * @param pageWidth the width of the desired paper
     * @param pageHeight the height of the desired paper
     */
    public void selectBestPaper(double pageWidth, double pageHeight) {
        int match = -1;
        double minError = Double.MAX_VALUE;
        ComboBoxModel m = paperSizeCombo.getModel();
        for (int i = 0; i < m.getSize(); ++i) {
            PaperProperties pp = (PaperProperties) m.getElementAt(i);
            double dx = pageWidth - pp.getPageWidth();
            double dy = pageHeight - pp.getPageHeight();
            double errorSq = (dx * dx) + (dy * dy);
            if (errorSq < minError) {
                match = i;
                minError = errorSq;
            }
        }
        if (match >= 0) {
            paperSizeCombo.setSelectedIndex(match);
        }
    }

//	public static void createPaperList( DefaultComboBoxModel list ) {
//		createPaperList( list, true );
//	}
//
//    public static void createPaperList( DefaultComboBoxModel list, boolean includePseudoSizes ) {
//		for( PaperProperties p : PaperProperties.getStandardPapers() ) {
//			list.addElement( p );
//		}
//
//		int count = PaperProperties.getCustomPaperCount();
//		for( int i=0; i<count; ++i ) {
//			if( !includePseudoSizes && Settings.getShared().getBoolean( "paper-is-pseudo-" + (i+1) ) ) {
//				continue;
//			}
//			PaperProperties[] pp = PaperProperties.getCustomPaper( i );
//			list.addElement( pp[0] );
//			list.addElement( pp[1] );
//		}
//    }
//
//    /** Find the paper option that best matches the default paper size of the default printer. */
//    public static int findBestDefaultPaper( DefaultComboBoxModel list ) {
//        PrinterJob job = PrinterJob.getPrinterJob();
//        PageFormat pf = job.defaultPage();
//        pf.setOrientation( PageFormat.LANDSCAPE );
//        return findBestPaper( pf, list );
//    }
//
//	/**
//	 * Selects the paper size that most closely matches the requested dimensions.
//	 * @param width the width to match, in points
//	 * @param height the height to match, in points
//	 */
//    public void selectBestPaper( double width, double height ) {
//        int i = findBestPaper( width, height, (DefaultComboBoxModel) paperSizeCombo.getModel() );
//        paperSizeCombo.setSelectedIndex( i );
//    }
//
//    /** Find the paper option that best matches a give page format. */
//    public static int findBestPaper( PageFormat pf, DefaultComboBoxModel list ) {
//        return findBestPaper( pf.getWidth(), pf.getHeight(), list );
//    }
//
//    public static int findBestPaper( double width, double height, DefaultComboBoxModel list ) {
//        int bestMatch = 0;
//        double minError = Double.MAX_VALUE;
//        for( int i = 0; i < list.getSize(); ++i ) {
//            PaperProperties paper = (PaperProperties) list.getElementAt( i );
//            double dx = width - paper.getPageWidth();
//            double dy = height - paper.getPageHeight();
//            double error = Math.sqrt( (dx * dx) + (dy * dy) );
//            if( error < minError ) {
//                minError = error;
//                bestMatch = i;
//            }
//        }
//        return bestMatch;
//    }
//
//	public static int findPaperInList( DefaultComboBoxModel model, PaperProperties paper ) {
//		for( int i=0; i<model.getSize(); ++i ) {
//			if( paper.equals( (PaperProperties) model.getElementAt( i ) ) ) {
//				return i;
//			}
//		}
//		return -1;
//	}
//
//    static void selectPaperInList( JComboBox combo, DefaultComboBoxModel model, PaperProperties paper ) {
//        boolean found = false;
//        for( int j = model.getSize(); j >= 0; --j ) {
//            if( paper.equals( (PaperProperties) model.getElementAt( j ) ) ) {
//                combo.setSelectedIndex( j );
//                found = true;
//                break;
//            }
//        }
//        if( !found ) {
//            model.addElement( paper );
//            combo.setSelectedIndex( model.getSize()-1 );
//        }
//    }
    /**
     * Create a new, blank page and add it to the end of the deck.
     */
    public Page addPage() {
        tc();
        final Page p = deck.addNewPage();
        final PageView v = new PageView();
        p.setView(v);
        addPageViewTab(v);
        pageTab.setSelectedComponent(v);
        return p;
    }

    /**
     * Create a new tab containing a view for a page.
     */
    private void addPageViewTab(PageView view) {
        dragManager.addDropTarget(view);
        view.setOptionLabel(optionLabel);
        pageTab.insertTab(string("de-l-tab-label", pageTab.getTabCount()), null, view, null, pageTab.getTabCount() - 1);
    }

    public void addCards(Object[] cards) {
        tc();
        PageView v = (PageView) pageTab.getSelectedComponent();

        Page p = v.getPage();
        for (int i = 0; i < cards.length; ++i) {
            p.addCardFromTemplate((PageItem) cards[i]);
        }
    }

    public void addCards(List<PageItem> cards) {
        tc();
        final PageView v = (PageView) pageTab.getSelectedComponent();
        final Page p = v.getPage();
        for(PageItem pi : cards) {
            p.addCardFromTemplate(pi);
        }
    }

    @Override
    public void populateFieldsFromComponent() {
        try {
            isPopulating = true;

            Game game = Game.get(deck.getSettings().get(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE));
            if (game == null) {
                game = Game.getAllGamesInstance();
            }
            gameCombo.setSelectedItem(game);

            // ensure that the deck's paper size is the selected one
            DefaultComboBoxModel m = (DefaultComboBoxModel) paperSizeCombo.getModel();
            Set<PaperProperties> set = PaperSets.modelToSet(m);
            PaperProperties sel = PaperSets.findBestPaper(deck.getPaperProperties(), set);
            paperSizeCombo.setSelectedItem(sel);

            nameField.setText(deck.getName());
            commentField.setText(deck.getComment());

            showCropCheck.setSelected(deck.getPublishersMarksEnabled());
            cropWeightField.setValue(deck.getPublishersMarkWidth());
            cropDistField.setValue(deck.getPublishersMarkDistance());
            cropLengthField.setValue(deck.getPublishersMarkLength());
            cropPrintWeightField.setValue(deck.getPublishersMarkPrintWidth());
            deck.updateCropMarkManagers();

            fakeBleedCheck.setSelected(deck.isAutoBleedMarginEnabled());

            super.populateFieldsFromComponent();
        } finally {
            isPopulating = false;
        }
    }
    private boolean isPopulating = false;

    @Override
    protected void populateComponentFromDelayedFields() {
        commentFieldsFocusLost(null);
    }

    @Override
    public void redrawPreview() {
        tc();
        for (int i = 0; i < deck.getPageCount(); ++i) {
            PageView v = deck.getPage(i).getView();
            if (v != null) {
                v.forceRerender();
            }
        }
    }

    @Override
    protected void clearImpl() {
        int pages = deck.getPageCount();

        // check if any page has content
        if (hasUnsavedChanges()) {
            for (int i = 0; i < pages; ++i) {
                if (getDeck().getPage(i).getCardCount() == 0) {
                    continue;
                }
                if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(
                        this, string("de-verify-clear"), "",
                        JOptionPane.YES_NO_OPTION
                )) {
                    return;
                }
                break;
            }
        }

        // remove all pages
        for (int i = 0; i < pages; ++i) {
            pageTab.setSelectedIndex(0);
            removeCurrentPage(false);
        }

        // TODO: should clear game, comments, etc.
    }

    /**
     * Creates an image containing the content of the specified deck page as if
     * it was printed at the specified resolution. The <code>pageBuffer</code>
     * parameter can be <code>null</code>, in which case a suitable image will
     * be created and returned. If you are creating images of multiple pages,
     * you can pass <code>null</code> for the first call and then re-use the
     * returned image for subsequent calls.
     *
     * @param pageBuffer an image to draw the page content on; may be
     * <code>null</code>
     * @param pageIndex the page to draw
     * @param ppi the resolution to draw the page at
     * @return the image that was drawn; the same value as
     * <code>pageBuffer</code> if that parameter was non-<code>null</code>,
     * otherwise a new, suitable image
     * @throws IllegalArgumentException if the page index is invalid or the
     * resolution is not positive
     * @throws OutOfMemoryError if there is insufficient memory to allocate the
     * page image and/or render high resolution content to it
     */
    BufferedImage createPageImage(BufferedImage pageBuffer, int pageIndex, double ppi) {
        if (pageIndex < 0 || pageIndex >= deck.getPageCount()) {
            throw new IllegalArgumentException("pageIndex: " + pageIndex);
        }
        if (ppi < 1d) {
            throw new IllegalArgumentException("ppi: " + ppi);
        }

        PaperProperties pp = deck.getPaperProperties();
        PageFormat pf = pp.createCompatiblePageFormat(false);
        if (pageBuffer == null) {
            pageBuffer = deck.getPaperProperties().createCompatibleImage(ppi);
        }

        Graphics2D g = null;
        try {
            double scale = ppi / 72;
            int pwidth = (int) ((pageBuffer.getWidth() + 1) * scale);
            int pheight = (int) ((pageBuffer.getWidth() + 1) * scale);
            g = pageBuffer.createGraphics();
            g.scale(scale, scale);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, pwidth, pheight);
            print(g, pf, pageIndex, ppi);
        } finally {
            if (g != null) {
                g.dispose();
            }
        }

        return pageBuffer;
    }

    /**
     * Implements EXPORT for decks. Since decks do not have sheets, this exports
     * an image of each deck page.
     */
    @Override
    protected void exportImpl() {
        ImageExporter export = ImageExporter.getSharedInstance();
        boolean accepted;
        try {
            accepted = export.beginExport(deck.getName(), deck.getComment(), 150d, true, false, false, false);
        } catch (IOException e) {
            ErrorDialog.displayError(string("rk-err-export"), e);
            return;
        }

        if (accepted) {
            new BusyDialog(StrangeEons.getWindow(), string("busy-exporting"), () -> {
                ImageExporter export1 = ImageExporter.getSharedInstance();
                double dpi = export1.getTargetResolution();
                try {
                    BufferedImage page = null;
                    for (int i = 0; i < deck.getPageCount(); ++i) {
                        page = createPageImage(page, i, dpi);
                        export1.exportImage(String.valueOf(i + 1), page);
                    }
                }catch (OutOfMemoryError oom) {
                    ErrorDialog.outOfMemory();
                }catch (IOException e) {
                    ErrorDialog.displayError(string("rk-err-export"), e);
                }catch (Exception e) {
                    StrangeEons.log.log(Level.SEVERE, null, e);
                } finally {
                    try {
                        export1.endExport();
                    }catch (IOException e) {
                        ErrorDialog.displayError(string("rk-err-export"), e);
                    }
                }
            });
        }
    }

    @Override
    public boolean canPerformCommand(AbstractCommand command) {
        if (command == Commands.SPIN_OFF) {
            return false;
        }
        if (command == Commands.FIND) {
            return true;
        }
        if (command == Commands.SELECT_ALL) {
            return true;
        }
        return super.canPerformCommand(command);
    }

    @Override
    public boolean isCommandApplicable(AbstractCommand command) {
        if (command == Commands.SPIN_OFF) {
            return false;
        }
        if (command == Commands.FIND) {
            return true;
        }
        if (command == Commands.SELECT_ALL || command == Commands.CUT || command == Commands.COPY || command == Commands.PASTE) {
            boolean ok = !((DelegatedCommand) command).isDefaultActionApplicable();
            if ((command == Commands.CUT || command == Commands.COPY) && getDeck().getSelectionSize() == 0) {
                ok = false;
            }
            if (command == Commands.PASTE && Deck.isDeckClipboardEmpty()) {
                ok = false;
            }
            if (command == Commands.SELECT_ALL && getDeck().getActivePage().getCardCount() == 0) {
                ok = false;
            }
            return ok;
        }
        if(command == Commands.VIEW_INK_SAVER) {
            return true;
        }
        return canPerformCommand(command);
    }

    @Override
    public void performCommand(AbstractCommand command) {
        if (command == Commands.FIND) {
            if (findField.hasFocus()) {
                findFieldActionPerformed(null);
            } else {
                findField.requestFocusInWindow();
            }
        } else if (command == Commands.SELECT_ALL) {
            getDeck().selectAll(getDeck().getActivePage());
        } else if (command == Commands.CUT) {
            getDeck().cut();
        } else if (command == Commands.COPY) {
            getDeck().copy();
        } else if (command == Commands.PASTE) {
            getDeck().paste();
        } else if(command == Commands.VIEW_INK_SAVER) {
            boolean enable = Commands.VIEW_INK_SAVER.isSelected();
            for(int p=0, len=deck.getPageCount(); p<len; ++p) {
                Page page = deck.getPage(p);
                boolean needsRepaint = false;
                for(int i=0, ilen=page.getCardCount(); i<ilen; ++i) {
                    PageItem pi = page.getCard(i);
                    if(pi instanceof CardFace) {
                        CardFace card = (CardFace) pi;
                        card.getSheet().setPrototypeRenderingModeEnabled(enable);
                        card.refresh();
                        needsRepaint = true;
                    }
                }
                if(needsRepaint) {
                    getActivePageView().repaint();
                }
            }
        } else {
            super.performCommand(command);
        }
    }

    @Override
    public void replaceEditedComponent(Deck newCharacter) {
        tc();
        setGameComponent(newCharacter);

        // we need to either stop the timer or do this before running the
        // busy dialog as it will allow timer events to hit this editor,
        // which will cause it to overwrite the name and comment fields
        stopTimedUpdates();
        populateFieldsFromComponent();

        for (int i = 0; i < pageTab.getTabCount() - 1; ++i) {
            pageTab.remove(i);
        }
        LinkedList<File> fileList = new LinkedList<>();
        for (int i = 0; i < deck.getPageCount(); ++i) {
            PageView v = new PageView();
            Page p = deck.getPage(i);
            p.setView(v);
            addPageViewTab(v);
            PageItem[] cards = p.getCards();
            for (int j = 0; j < cards.length; ++j) {
                if (cards[j] instanceof CardFace) {
                    CardFace f = (CardFace) cards[j];
                    fileList.add(new File(f.getPath()));
                }
            }
        }
        pageTab.setSelectedIndex(0);
        updatePageTitles(0);

        final File[] thumbCards = fileList.toArray(new File[fileList.size()]);
//        if( thumbCards.length < 0 ) {
//            // there are multiple cards; tell the user this may take a while
//            new BusyDialog( StrangeEons.getWindow(), string( "busy-thumbnails" ), new Runnable() {
//				@Override
//                public void run() {
//                    addFilesToCardList( thumbCards );
//                }
//            } );
//        } else {
        addFilesToCardList(thumbCards);
//        }

        requestFocusInView(deck.getActivePage().getView());
        populateFieldsFromComponent();
        resumeTimedUpdates();
    }

    @Override
    public void save() {
        deck.setSaveFileHint(getFile());
        try {
            super.save();
        } finally {
            deck.setSaveFileHint(null);
        }
    }

    @Override
    public void handleOpenRequest(Deck newCharacter, File path) {
        tc();
        // tracking the saved flag lets us mark it as unsaved if the deck
        // had some of the card paths updated while opening the deck
        boolean unsaved = deck.hasUnsavedChanges();

        replaceEditedComponent(newCharacter);

        setFile(path);

        if (!unsaved) {
            deck.markSaved();
        }

    }

    @Override
    protected String getDefaultFileName() {
        return ResourceKit.makeStringFileSafe(
                deck.getName() + " " + String.valueOf(++newDeckFileNameIndex) + ".eon");
    }
    private static int newDeckFileNameIndex = 0;

    /**
     * Displays a dialog that allows the user to print the deck. If the user
     * elects to proceed with the print, {@link #printImpl} will be called to
     * perform the actual printing.
     *
     * <p>
     * The displayed dialog also allows the user to modify page splitting
     * options, which are used to tile large deck pages onto a smaller physical
     * page size.
     */
    @Override
    public void print() {
        print(false);
    }

    /**
     * Prints the content of this deck editor. If a direct print is specified,
     * then then no further deck-related options are offered first. Only the
     * platform-specific print dialog, if any, is displayed before printing
     * begins.
     *
     * @param direct if <code>true</code>, no deck-specific print options are
     * shown
     */
    public void print(boolean direct) {
        if (!isCommandApplicable(Commands.PRINT)) {
            return;
        }

        if (!direct) {
            PrintSetupDialog psd = new PrintSetupDialog(this);
            // if this returns false, the user either cancelled the print
            // or it was handled by the dialog (e.g., the print to PDF option was used).
            if (!psd.showDialog()) {
                return;
            }
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(getTitle());

        StrangeEons.setWaitCursor(true);
        try {
            printImpl(job);
        } catch (PrinterAbortException e) {
        } catch (Exception e) {
            ErrorDialog.displayError(string("ae-err-print"), e);
        } finally {
            StrangeEons.setWaitCursor(false);
        }
    }

    @Override
    protected void printImpl(PrinterJob printJob) throws PrintException, PrinterException {
        final String printJobName = getFile() == null ? deck.getName() : (deck.getName() + " (" + getFile().getName() + ')');

        job = printJob;
        job.setJobName(printJobName);
        Book book = new Book();
        lastPageIndex = -1; // for tracking when resources can be freed

        // handle page splitting setup:
        //   if no splitting print directly to the virtual page spec
        //   if splitting create a virtual printable that delegates to this
        if (!deck.isPaperSplitting() || deck.getPrinterPaperProperties() == null) {
            final int pageCount = deck.getPageCount();
            book.append(
                    new MonitoredPrintable(this, pageCount),
                    deck.getPaperProperties().createCompatiblePageFormat(false),
                    pageCount
            );
            paperSplitter = null;
        } else {
            PaperProperties physical = deck.getPrinterPaperProperties();
            paperSplitter = new PaperSplitter(deck.getPaperProperties(), physical);
            paperSplitter.setPrintableFrameWidth(deck.getSplitBorder());
            paperSplitter.setPrintableFrameColor(deck.getSplitBorderColor());
            final int pageCount = paperSplitter.getTotalPhysicalPagesRequired(deck.getPageCount());
            book.append(
                    new MonitoredPrintable(paperSplitter.createPrintable(this), pageCount),
                    physical.createCompatiblePageFormat(false),
                    pageCount
            );
        }

        job.setPageable(book);
        if (ResourceKit.showPrintDialog(job)) {
            new BusyDialog(StrangeEons.getWindow(), string("busy-printing"), () -> {
                try {
                    job.print();
                } catch (PrinterAbortException e) {
                } catch (Exception e) {
                    ErrorDialog.displayError(string("ae-err-print"), e);
                } catch (OutOfMemoryError e) {
                    ErrorDialog.outOfMemory();
                } finally {
                    job = null;
                }
            }, (ActionEvent e) -> {
                job.cancel();
            });
        }
        job = null;
        paperSplitter = null;
    }
    private PrinterJob job;
    private PaperSplitter paperSplitter;

    @Override
    public int print(Graphics g1, PageFormat pf, int pageIndex) {
        return print(g1, pf, pageIndex, -1d);
    }

    /**
     * The implementation of print(). If <code>printResolution</code> is set to
     * a positive value, cards are prepared at that resolution. (This is set to
     * an explicit value when exporting.) Otherwise, if it is less than 0, the
     * card resolution is inferred from printer job settings.
     *
     * @param g1 graphics instance for printing
     * @param pf page format to conform to
     * @param pageIndex page index
     * @param printResolution print resolution, or -1 to determine automatically
     * @return <code>PAGE_EXISTS</code> or <code>NO_SUCH_PAGE</code>
     */
    private int print(Graphics g1, PageFormat pf, int pageIndex, double printResolution) {
        final Settings s = Settings.getUser();
        final RenderTarget renderTarget = printResolution > 0 ? RenderTarget.EXPORT : RenderTarget.PRINT;

        if (pageIndex >= deck.getPageCount()) {
            return Printable.NO_SUCH_PAGE;
        }

        int physicalPage = paperSplitter == null ? pageIndex : paperSplitter.getPhysicalPageBeingPrinted();
        StrangeEons.log.log(Level.INFO, "printing deck page {0} (physical page {1})", new Object[]{pageIndex, physicalPage});

        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // if no resolution specified, determine it from the target device's transform
        double dpi = printResolution;
        if (dpi < 0) {
            // determine the effective printing resolution of the target
            double[] matrix = new double[6];
            g.getTransform().getMatrix(matrix);

            double xDpi = 72d * (matrix[1] < 0d ? -matrix[1] : matrix[1]);
            double yDpi = 72d * (matrix[2] < 0d ? -matrix[2] : matrix[2]);
            double devDpi = Math.round(Math.min(xDpi, yDpi));

            dpi = Math.min(s.getDouble("target-print-dpi-max"),
                    Math.max(s.getDouble("target-print-dpi-min"), devDpi));
            StrangeEons.log.log(Level.INFO, "selected print resolution of {0} dpi", (int) (dpi + 0.5d));
        }

//		if( s.getYesNo( "print-scaling-enabled" ) ) {
//			g.scale( s.getDouble( "print-x-scale" ), s.getDouble( "print-y-scale" ) );
//		}
        // get the graphics clipping rectangle; we'll use this to determine if
        // we should clip a page item
        Rectangle2D clipRect = new Rectangle2D.Double(0, 0, pf.getWidth(), pf.getHeight());

        Page page = deck.getPage(pageIndex);
        CropMarkManager cropMarks = new CropMarkManager();
        cropMarks.setEnabled(deck.getPublishersMarksEnabled());
        cropMarks.setMarkWidth(deck.getPublishersMarkPrintWidth());
        cropMarks.setMarkSize(deck.getPublishersMarkDistance(), deck.getPublishersMarkLength());
        cropMarks.update(page);
        cropMarks.paint(g);

        BusyDialog dialog = BusyDialog.getCurrentDialog();
        PageItem[] cards = page.getCards();

        if (pageIndex != lastPageIndex) {
            lastPageIndex = pageIndex;
            lastPass = 0;
            if (pageIndex > 0) {
                freePrintResourcesForPage(pageIndex - 1, physicalPage);
            }
        } else {
            ++lastPass;
        }

        // Cards
        if (job == null || !job.isCancelled()) {
            for (int i = 0; i < cards.length; ++i) {
                PageItem c = cards[i];

                // cull and do not render clipped cards
                Rectangle2D.Double r = c.getRectangle();
                if (clipRect.intersects(r)) {
                    if (dialog != null) {
                        dialog.setStatusText(string("de-print-status", physicalPage + 1, lastPass + 1));
                    }

                    int attemptsLeft = 2;
                    while (attemptsLeft > 0) {
                        try {
                            c.prepareToPaint(renderTarget, dpi);
                            break;
                        } catch (OutOfMemoryError e) {
                            if (--attemptsLeft == 0) {
                                throw e;
                            }
                            StrangeEons.log.info("out of memory, freeing cached hi-res components");
                            freePrintResourcesForPage(pageIndex, physicalPage);
                        }
                    }
                    c.paint(g, renderTarget, dpi);

                    if (job != null && job.isCancelled()) {
                        break;
                    }
                } else {
                    StrangeEons.log.log(Level.FINEST, "object clipped: {0}", c.getName());
                }
            }
        }

        return Printable.PAGE_EXISTS;
    }
    private int lastPageIndex;
    private int lastPass;

    private void freePrintResourcesForPage(int pageIndex, int displayedPage) {
        BusyDialog dialog = BusyDialog.getCurrentDialog();
        PageItem[] cards = deck.getPage(pageIndex).getCards();

        if (dialog != null) {
            dialog.setStatusText(string("de-print-res", displayedPage + 1));
        }

        for (PageItem c : cards) {
            c.prepareToPaint(RenderTarget.PREVIEW, 150d);
        }
    }

    private TransferHandler listTransferHandler = new TransferHandler() {
        @Override
        public boolean importData(TransferSupport support) {
            return false;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        cardListMenu = new javax.swing.JPopupMenu();
        sortNameItem = new javax.swing.JMenuItem();
        sortFileNameItem = new javax.swing.JMenuItem();
        sortAreaItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        addCardsItem = new javax.swing.JMenuItem();
        removeFacesItem = new javax.swing.JMenuItem();
        lhsPanel = new javax.swing.JPanel();
        lhsPanelSplitter = new javax.swing.JSplitPane();
        mainControlTab = new javax.swing.JTabbedPane();
        deckPanel = new javax.swing.JPanel();
        nameField = new JSpellingTextField();
        javax.swing.JLabel nameLabel = new javax.swing.JLabel();
        javax.swing.JLabel gameLabel = new javax.swing.JLabel();
        gameCombo = new ca.cgjennings.ui.JGameCombo();
        ca.cgjennings.ui.JTip gameTip = new ca.cgjennings.ui.JTip();
        javax.swing.JLabel paperSizeLabel = new javax.swing.JLabel();
        paperSizeCombo = new javax.swing.JComboBox();
        customPaperBtn = new javax.swing.JButton();
        StyleUtilities.small(customPaperBtn);
        cropPanel = new javax.swing.JPanel();
        showCropCheck = new javax.swing.JCheckBox();
        cropMeasurePanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        cropWeightField = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        cropDistField = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        cropLengthField = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        cropPrintWeightField = new javax.swing.JSpinner();
        jLabel10 = new javax.swing.JLabel();
        fakeBleedCheck = new javax.swing.JCheckBox();
        commentPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        commentField = new JSpellingTextArea();
        componentPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        addCardsBtn = new javax.swing.JButton();
        remCardsBtn = new javax.swing.JButton();
        cardTileTab = new javax.swing.JTabbedPane();
        toolsScroll = new javax.swing.JScrollPane();
        toolsList = new PageItemList();
        facesScroll = new javax.swing.JScrollPane();
        facesList = new PageItemList();
        tilesScroll = new javax.swing.JScrollPane();
        tilesList = new PageItemList();
        decorationsScroll = new javax.swing.JScrollPane();
        decorationsList = new PageItemList();
        boardBitsScroll = new javax.swing.JScrollPane();
        boardBitsList = new PageItemList();
        otherScroll = new javax.swing.JScrollPane();
        otherList = new PageItemList();
        findPanel = new javax.swing.JPanel();
        javax.swing.JLabel findLabel = new javax.swing.JLabel();
        StyleUtilities.small( findLabel );
        findField =  new JLabelledField() ;
        ((JLabelledField) findField).setTextForeground( new Color(0,0,4) );
        rhsPanel = new javax.swing.JPanel();
        pageTab =  new DeckPageTabbedPane();
        optionLabel = new javax.swing.JLabel();

        sortNameItem.setMnemonic('n');
        sortNameItem.setText(string("de-sort-name")); // NOI18N
        sortNameItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortNameItemActionPerformed(evt);
            }
        });
        cardListMenu.add(sortNameItem);

        sortFileNameItem.setMnemonic('f');
        sortFileNameItem.setText(string("de-sort-file")); // NOI18N
        sortFileNameItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortFileNameItemActionPerformed(evt);
            }
        });
        cardListMenu.add(sortFileNameItem);

        sortAreaItem.setMnemonic('s');
        sortAreaItem.setText(string("de-sort-area")); // NOI18N
        sortAreaItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortAreaItemActionPerformed(evt);
            }
        });
        cardListMenu.add(sortAreaItem);
        cardListMenu.add(jSeparator1);

        addCardsItem.setMnemonic('a');
        addCardsItem.setText(string("de-b-add-cards")); // NOI18N
        addCardsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCardsBtnActionPerformed(evt);
            }
        });
        cardListMenu.add(addCardsItem);

        removeFacesItem.setMnemonic('r');
        removeFacesItem.setText(string("de-b-remove-cards")); // NOI18N
        removeFacesItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remCardsBtnActionPerformed(evt);
            }
        });
        cardListMenu.add(removeFacesItem);

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle(string("de-l-title")); // NOI18N

        lhsPanel.setLayout(new java.awt.BorderLayout());

        lhsPanelSplitter.setDividerSize(8);
        lhsPanelSplitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        lhsPanelSplitter.setOneTouchExpandable(true);

        nameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commentFieldsFocusLost(evt);
            }
        });

        nameLabel.setLabelFor(nameField);
        nameLabel.setText(string("de-l-props")); // NOI18N

        gameLabel.setLabelFor(gameCombo);
        gameLabel.setText(string("de-l-game")); // NOI18N

        gameCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gameComboActionPerformed(evt);
            }
        });

        gameTip.setTipText(string("de-tip-game")); // NOI18N

        paperSizeLabel.setLabelFor(paperSizeCombo);
        paperSizeLabel.setText(string("de-l-paper-size")); // NOI18N

        paperSizeCombo.setMaximumRowCount(12);
        paperSizeCombo.setRenderer( PaperSets.createListCellRenderer() );
        paperSizeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paperSizeComboActionPerformed(evt);
            }
        });

        customPaperBtn.setText(string("de-b-cust-paper")); // NOI18N
        customPaperBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customPaperBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout deckPanelLayout = new javax.swing.GroupLayout(deckPanel);
        deckPanel.setLayout(deckPanelLayout);
        deckPanelLayout.setHorizontalGroup(
            deckPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deckPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(deckPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(deckPanelLayout.createSequentialGroup()
                        .addGroup(deckPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameLabel)
                            .addComponent(gameLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(deckPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(deckPanelLayout.createSequentialGroup()
                                .addComponent(gameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(gameTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(nameField)))
                    .addComponent(paperSizeLabel)
                    .addGroup(deckPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(deckPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(paperSizeCombo, javax.swing.GroupLayout.Alignment.TRAILING, 0, 284, Short.MAX_VALUE)
                            .addComponent(customPaperBtn))))
                .addContainerGap())
        );
        deckPanelLayout.setVerticalGroup(
            deckPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deckPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(deckPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameLabel)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(deckPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(gameLabel)
                    .addComponent(gameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gameTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(paperSizeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(paperSizeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customPaperBtn)
                .addContainerGap(36, Short.MAX_VALUE))
        );

        mainControlTab.addTab(string("de-l-deck"), deckPanel); // NOI18N

        showCropCheck.setText(string( "de-l-show-crop" )); // NOI18N
        showCropCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showCropCheckActionPerformed(evt);
            }
        });

        jLabel5.setText(string( "de-l-crop-weight" )); // NOI18N

        cropWeightField.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.1f), Float.valueOf(5.0f), Float.valueOf(0.25f)));
        cropWeightField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cropFieldStateChanged(evt);
            }
        });

        jLabel6.setText(string( "de-l-crop-dist" )); // NOI18N

        cropDistField.setModel(new javax.swing.SpinnerNumberModel(9.0d, 0.0d, 72.0d, 1.0d));
        cropDistField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cropFieldStateChanged(evt);
            }
        });

        jLabel7.setText(string( "de-l-crop-length" )); // NOI18N

        cropLengthField.setModel(new javax.swing.SpinnerNumberModel(14.0d, 1.0d, 72.0d, 1.0d));
        cropLengthField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cropFieldStateChanged(evt);
            }
        });

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getSize()-1f));
        jLabel8.setText(string( "de-l-crop-points" )); // NOI18N

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getSize()-1f));
        jLabel9.setText(string( "de-l-crop-screen" )); // NOI18N

        cropPrintWeightField.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.1f), Float.valueOf(5.0f), Float.valueOf(0.25f)));
        cropPrintWeightField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cropPrintWeightFieldcropFieldStateChanged(evt);
            }
        });

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getSize()-1f));
        jLabel10.setText(string( "de-l-crop-print" )); // NOI18N

        javax.swing.GroupLayout cropMeasurePanelLayout = new javax.swing.GroupLayout(cropMeasurePanel);
        cropMeasurePanel.setLayout(cropMeasurePanelLayout);
        cropMeasurePanelLayout.setHorizontalGroup(
            cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cropMeasurePanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(cropMeasurePanelLayout.createSequentialGroup()
                        .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cropLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(cropMeasurePanelLayout.createSequentialGroup()
                                .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cropDistField)
                                    .addComponent(cropWeightField, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel9))
                                .addGap(18, 18, 18)
                                .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10)
                                    .addComponent(cropPrintWeightField, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addComponent(jLabel8))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        cropMeasurePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cropDistField, cropLengthField, cropWeightField});

        cropMeasurePanelLayout.setVerticalGroup(
            cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cropMeasurePanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addGap(1, 1, 1)
                .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(cropWeightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cropPrintWeightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cropDistField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(cropMeasurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cropLengthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fakeBleedCheck.setText(string("de-l-fake-bleed")); // NOI18N
        fakeBleedCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fakeBleedCheckActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout cropPanelLayout = new javax.swing.GroupLayout(cropPanel);
        cropPanel.setLayout(cropPanelLayout);
        cropPanelLayout.setHorizontalGroup(
            cropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cropPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(cropPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(cropMeasurePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(showCropCheck)
                    .addComponent(fakeBleedCheck))
                .addContainerGap(35, Short.MAX_VALUE))
        );
        cropPanelLayout.setVerticalGroup(
            cropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cropPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(showCropCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cropMeasurePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fakeBleedCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        mainControlTab.addTab(string( "de-l-pub-marks" ), cropPanel); // NOI18N

        commentField.setColumns(20);
        commentField.setLineWrap(true);
        commentField.setTabSize(4);
        commentField.setWrapStyleWord(true);
        commentField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commentFieldsFocusLost(evt);
            }
        });
        jScrollPane3.setViewportView(commentField);

        javax.swing.GroupLayout commentPanelLayout = new javax.swing.GroupLayout(commentPanel);
        commentPanel.setLayout(commentPanelLayout);
        commentPanelLayout.setHorizontalGroup(
            commentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, commentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE)
                .addContainerGap())
        );
        commentPanelLayout.setVerticalGroup(
            commentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, commentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
                .addContainerGap())
        );

        mainControlTab.addTab(string("ae-tab-comments"), commentPanel); // NOI18N

        lhsPanelSplitter.setLeftComponent(mainControlTab);

        componentPanel.setMinimumSize(new java.awt.Dimension(1, 0));

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD, jLabel4.getFont().getSize()+2));
        jLabel4.setText(string( "de-l-card-sheets" )); // NOI18N
        jLabel4.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        addCardsBtn.setText(string("de-b-add-cards")); // NOI18N
        addCardsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCardsBtnActionPerformed(evt);
            }
        });

        remCardsBtn.setText(string("de-b-remove-cards")); // NOI18N
        remCardsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remCardsBtnActionPerformed(evt);
            }
        });

        cardTileTab.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        cardTileTab.setFont(cardTileTab.getFont().deriveFont(cardTileTab.getFont().getSize()-1f));
        cardTileTab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cardTileTabStateChanged(evt);
            }
        });

        toolsScroll.setBorder(null);

        toolsList.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        toolsList.setFont(toolsList.getFont().deriveFont(toolsList.getFont().getSize()+3f));
        toolsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        toolsList.setComponentPopupMenu(cardListMenu);
        toolsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tilesListMouseClicked(evt);
            }
        });
        toolsScroll.setViewportView(toolsList);

        cardTileTab.addTab(string( "de-tab-tools" ), toolsScroll); // NOI18N

        facesScroll.setBorder(null);

        facesList.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        facesList.setFont(facesList.getFont().deriveFont(facesList.getFont().getSize()+3f));
        facesList.setComponentPopupMenu(cardListMenu);
        facesList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tilesListMouseClicked(evt);
            }
        });
        facesList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                facesListValueChanged(evt);
            }
        });
        facesScroll.setViewportView(facesList);

        cardTileTab.addTab(string("de-tab-cards"), facesScroll); // NOI18N

        tilesScroll.setBorder(null);

        tilesList.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tilesList.setFont(tilesList.getFont().deriveFont(tilesList.getFont().getSize()+3f));
        tilesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tilesList.setComponentPopupMenu(cardListMenu);
        tilesList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tilesListMouseClicked(evt);
            }
        });
        tilesScroll.setViewportView(tilesList);

        cardTileTab.addTab(string("de-tab-tiles"), tilesScroll); // NOI18N

        decorationsScroll.setBorder(null);

        decorationsList.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        decorationsList.setFont(decorationsList.getFont().deriveFont(decorationsList.getFont().getSize()+3f));
        decorationsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        decorationsList.setComponentPopupMenu(cardListMenu);
        decorationsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tilesListMouseClicked(evt);
            }
        });
        decorationsScroll.setViewportView(decorationsList);

        cardTileTab.addTab(string("de-tab-decorations"), decorationsScroll); // NOI18N

        boardBitsScroll.setBorder(null);

        boardBitsList.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        boardBitsList.setFont(boardBitsList.getFont().deriveFont(boardBitsList.getFont().getSize()+3f));
        boardBitsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        boardBitsList.setComponentPopupMenu(cardListMenu);
        boardBitsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tilesListMouseClicked(evt);
            }
        });
        boardBitsScroll.setViewportView(boardBitsList);

        cardTileTab.addTab(string("de-tab-board-pieces"), boardBitsScroll); // NOI18N

        otherScroll.setBorder(null);

        otherList.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        otherList.setFont(otherList.getFont().deriveFont(otherList.getFont().getSize()+3f));
        otherList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        otherList.setComponentPopupMenu(cardListMenu);
        otherList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tilesListMouseClicked(evt);
            }
        });
        otherScroll.setViewportView(otherList);

        cardTileTab.addTab(string("de-tab-misc"), otherScroll); // NOI18N

        findPanel.setBackground(java.awt.Color.white);
        findPanel.setLayout(new java.awt.GridBagLayout());

        findLabel.setBackground(java.awt.Color.white);
        findLabel.setDisplayedMnemonic('f');
        findLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        findLabel.setIcon( ResourceKit.getIcon( "ui/find-sm.png" ) );
        findLabel.setLabelFor(findField);
        findLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        findLabel.setMinimumSize(new java.awt.Dimension(11, 10));
        findLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        findPanel.add(findLabel, gridBagConstraints);

        findField.setBackground( Color.WHITE );
        findField.setFont(findField.getFont().deriveFont(findField.getFont().getSize()-1f));
        findField.setBorder(null);
        findField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findFieldActionPerformed(evt);
            }
        });
        findField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                findFieldKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        findPanel.add(findField, gridBagConstraints);

        javax.swing.GroupLayout componentPanelLayout = new javax.swing.GroupLayout(componentPanel);
        componentPanel.setLayout(componentPanelLayout);
        componentPanelLayout.setHorizontalGroup(
            componentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(componentPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(addCardsBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(remCardsBtn)
                .addContainerGap(37, Short.MAX_VALUE))
            .addComponent(cardTileTab, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
            .addComponent(findPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
            .addGroup(componentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 307, Short.MAX_VALUE)
                .addContainerGap())
        );

        componentPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addCardsBtn, remCardsBtn});

        componentPanelLayout.setVerticalGroup(
            componentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(componentPanelLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(componentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addCardsBtn)
                    .addComponent(remCardsBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cardTileTab, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(findPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        lhsPanelSplitter.setRightComponent(componentPanel);

        lhsPanel.add(lhsPanelSplitter, java.awt.BorderLayout.CENTER);

        pageTab.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        pageTab.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        pageTab.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pageTabMouseClicked(evt);
            }
        });
        pageTab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                pageTabStateChanged(evt);
            }
        });

        optionLabel.setFont(optionLabel.getFont().deriveFont(optionLabel.getFont().getSize()-2f));
        optionLabel.setText("help");

        javax.swing.GroupLayout rhsPanelLayout = new javax.swing.GroupLayout(rhsPanel);
        rhsPanel.setLayout(rhsPanelLayout);
        rhsPanelLayout.setHorizontalGroup(
            rhsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rhsPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(optionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(pageTab, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        rhsPanelLayout.setVerticalGroup(
            rhsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, rhsPanelLayout.createSequentialGroup()
                .addComponent(pageTab, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(optionLabel)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lhsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rhsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lhsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(rhsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void pageTabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_pageTabStateChanged
        // when we add the new page button a state changed event will be fired,
        // but at this time deck will be null and we'll ignore the event
        if (deck == null) {
            return;
        }

        if (pageTab.getSelectedComponent() == null || pageTab.getTabCount() == 1) {
            return;
        }

        if (pageTab.getSelectedIndex() == pageTab.getTabCount() - 1) {
            pageTab.setSelectedIndex(pageTab.getTabCount() - 2);
            return;
        }

        final PageView view = (PageView) pageTab.getSelectedComponent();
        deck.setActivePage(view.getPage());
        requestFocusInView(view);
    }//GEN-LAST:event_pageTabStateChanged

    private void requestFocusInView(final PageView view) {
        if (view == null) {
            return;
        }
        view.requestFocusInWindow();
        SwingUtilities.invokeLater(view::requestFocusInWindow);
    }

    private void facesListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_facesListValueChanged
        remCardsBtn.setEnabled(facesList.getSelectedIndices().length > 0);
    }//GEN-LAST:event_facesListValueChanged

    private void refocusOnPage() {
        pageTab.getSelectedComponent().requestFocusInWindow();
    }

    private void paperSizeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paperSizeComboActionPerformed
        if (paperSizeCombo.getSelectedIndex() >= 0) {
            deck.setPaperProperties((PaperProperties) paperSizeCombo.getSelectedItem());
        }
    }//GEN-LAST:event_paperSizeComboActionPerformed

    public void setPaperProperties(PaperProperties p) {
        paperSizeCombo.setSelectedItem(p);
        paperSizeComboActionPerformed(null);
    }

    public void removeCurrentPage() {
        removeCurrentPage(true);
    }

    private void removeCurrentPage(boolean verifyWithUser) {
        int t = pageTab.getSelectedIndex();
        if (t == pageTab.getTabCount() - 1) {
            StrangeEons.log.log(Level.SEVERE, "should not be able to remove current page on Add Page tab");
            return;
        }

        if (verifyWithUser && getDeck().getPage(t).getCardCount() > 0) {
            if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(
                    getParent(), string("de-verify-delete",
                            pageTab.getTitleAt(t)), "",
                    JOptionPane.YES_NO_OPTION
            )) {
                return;
            }
        }

        dragManager.removeDropTarget(getDeck().getPage(t).getView());

        if (pageTab.getTabCount() == 2) {
            addPage();
        }
        pageTab.remove(t);
        deck.removePage(t);
        updatePageTitles(t);
    }

    private void updatePageTitles(int startIndex) {
        for (int i = startIndex; i < pageTab.getTabCount() - 1; ++i) {
            String title = deck.getPage(i).getTitle();
            if (title == null) {
                title = String.format(string("de-l-tab-label"), i + 1);
            }
            pageTab.setTitleAt(i, title);
        }
    }

    public void setCurrentPage(int i) {
        pageTab.setSelectedIndex(i);
    }

    private void remCardsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remCardsBtnActionPerformed
        int[] sel = facesList.getSelectedIndices();
        for (int i = sel.length - 1; i >= 0; --i) {
            ((DefaultListModel) facesList.getModel()).remove(sel[i]);
        }
    }//GEN-LAST:event_remCardsBtnActionPerformed

    private void addCardsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addCardsBtnActionPerformed
//        cardTileTab.setSelectedIndex( 0 );
        addFilesToCardList(ResourceKit.showMultiOpenDialog(this));
    }//GEN-LAST:event_addCardsBtnActionPerformed

    private void pageTabMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pageTabMouseClicked
        if (evt.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        int tab = pageTab.indexAtLocation(evt.getX(), evt.getY());
        int clicks = evt.getClickCount();

        if (clicks == 2 && tab < 0) {
            addPage();
        } else if (tab == pageTab.getTabCount() - 1) {
            addPage();
        }
    }//GEN-LAST:event_pageTabMouseClicked

    private void sortCardList(PageItemList list, Comparator<PageItem> sortComparator) {
        final DefaultListModel<PageItem> model = list.getModel();

        // create an array of editable items, removing them from the model
        final PageItem[] items = new PageItem[model.getSize()];
        for (int i = 0; i < items.length; ++i) {
            items[i] = model.get(0);
            model.remove(0);
        }

        // use item name as secondary sort key
        Arrays.sort(items, NAME_COMPARATOR);
        if (sortComparator != NAME_COMPARATOR) {
            Arrays.sort(items, sortComparator);
        }

        // add the items back in sorted order
        for (int i = 0; i < items.length; ++i) {
            model.addElement(items[i]);
        }
    }

    private static final Comparator<PageItem> NAME_COMPARATOR = new Comparator<PageItem>() {
        private Collator collator = Language.getInterface().getCollator();

        @Override
        public int compare(PageItem o1, PageItem o2) {
            return collator.compare(o1.getName(), o2.getName());
        }
    };

    private static final Comparator<PageItem> AREA_COMPARATOR = (PageItem o1, PageItem o2) -> {
        double a1 = o1.getWidth() * o1.getHeight();
        double a2 = o2.getWidth() * o2.getHeight();
        return a1 < a2 ? -1 : (a1 > a2 ? 1 : 0);
    };

    private static final Comparator<PageItem> FILE_NAME_COMPARATOR = new Comparator<PageItem>() {
        private Collator collator = Language.getInterface().getCollator();

        @Override
        public int compare(PageItem o1, PageItem o2) {
            String n1 = o1.getName();
            String n2 = o2.getName();
            if (o1 instanceof DependentPageItem) {
                n1 = ((DependentPageItem) o1).getPath();
            }
            if (o2 instanceof DependentPageItem) {
                n2 = ((DependentPageItem) o2).getPath();
            }
            return collator.compare(n1, n2);
        }
    };

	private void customPaperBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customPaperBtnActionPerformed
            DefaultComboBoxModel model = (DefaultComboBoxModel) paperSizeCombo.getModel();
            PaperProperties pp = (PaperProperties) paperSizeCombo.getSelectedItem();
            CustomPaperDialog cpd = new CustomPaperDialog(customPaperBtn, pp, false);

            PaperProperties newPaper = cpd.showDialog();
            if (newPaper == null) {
                newPaper = pp;
            }
            deck.setPaperProperties(newPaper);
            reloadPaperCombo(true); // will update list and pick the one we just set
	}//GEN-LAST:event_customPaperBtnActionPerformed

private void cardTileTabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cardTileTabStateChanged
    tc();
    int sel = cardTileTab.getSelectedIndex();
    if (sel == 1) {
        facesListValueChanged(null); // update remove faces btn based on selection
    } else {
        remCardsBtn.setEnabled(false);
    }
}//GEN-LAST:event_cardTileTabStateChanged

private void findFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findFieldActionPerformed
    // the basic procedure is to start searching with the item after the
    // currently selected item, moving down each list and then to the next
    // list to the right; when the last list has been searched, continue
    // from the first list --- keep searching until we find a match
    // or every item has been considered once
    int currentList = cardTileTab.getSelectedIndex();
    String criteria = findField.getText().trim().toLowerCase();

    // gray out non-matching entries
    for (int i = 0; i < tileSetLists.length; ++i) {
        tileSetLists[i].getPageItemRenderer().setSearchTerm(criteria);
    }

    PageItemList list = tileSetLists[currentList];
    int selected = list.getSelectedIndex();
    DefaultListModel<PageItem> model = list.getModel();

    // search the current list (tab) from the selected index to the end
    if (searchListIndices(criteria, list, model, selected + 1, model.size())) {
        return;
    }

    // search each list after the current list, one at a time. when the last
    // list is reached, wrap around to the first list and keep searching up
    // until we get to the list before the current list
    for (int i = (currentList + 1) % tileSetLists.length;
            i != currentList;
            i = (i + 1) % tileSetLists.length) {
        PageItemList nextList = tileSetLists[i];
        if (!nextList.isVisible()) {
            continue;
        }

        DefaultListModel<PageItem> nextModel = nextList.getModel();
        if (searchListIndices(criteria, nextList, nextModel, 0, nextModel.size())) {
            cardTileTab.setSelectedIndex(i);
            return;
        }
    }

    // if the current list has a selected item, then we haven't searched from
    // the start of the list to the selected item (inclusive); do that now
    if (selected > 0) {
        if (searchListIndices(criteria, list, model, 0, selected)) {
            return;
        }
    }

    getToolkit().beep();
}//GEN-LAST:event_findFieldActionPerformed

private void commentFieldsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commentFieldsFocusLost
    if (deck == null) {
        return;
    }
    deck.setName(nameField.getText());
    deck.setComment(commentField.getText());
}//GEN-LAST:event_commentFieldsFocusLost

private void tilesListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tilesListMouseClicked
    if (evt.getClickCount() >= 2) {
        if (evt.getSource() instanceof PageItemList) {
            addCards(((PageItemList) evt.getSource()).getSelectedValuesList());
        }
    }
}//GEN-LAST:event_tilesListMouseClicked

private void sortNameItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortNameItemActionPerformed
    sortCardList(tileSetLists[cardTileTab.getSelectedIndex()], NAME_COMPARATOR);
}//GEN-LAST:event_sortNameItemActionPerformed

private void sortAreaItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortAreaItemActionPerformed
    sortCardList(tileSetLists[cardTileTab.getSelectedIndex()], AREA_COMPARATOR);
}//GEN-LAST:event_sortAreaItemActionPerformed

private void sortFileNameItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortFileNameItemActionPerformed
    sortCardList(tileSetLists[cardTileTab.getSelectedIndex()], FILE_NAME_COMPARATOR);
}//GEN-LAST:event_sortFileNameItemActionPerformed

private void findFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_findFieldKeyTyped
    EventQueue.invokeLater(() -> {
        // gray out non-matching entries
        String criteria = findField.getText().trim().toLowerCase();
        for (int i = 0; i < tileSetLists.length; ++i) {
            tileSetLists[i].getPageItemRenderer().setSearchTerm(criteria);
        }
        cardTileTab.getSelectedComponent().repaint();
    });
}//GEN-LAST:event_findFieldKeyTyped

private void cropFieldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cropFieldStateChanged
    if (deck == null || isPopulating) {
        return;
    }
    deck.setPublishersMarkWidth(((Number) cropWeightField.getValue()).floatValue());
    deck.setPublishersMarkDistance(((Number) cropDistField.getValue()).doubleValue());
    deck.setPublishersMarkLength(((Number) cropLengthField.getValue()).doubleValue());
}//GEN-LAST:event_cropFieldStateChanged

private void showCropCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showCropCheckActionPerformed
    if (deck == null || isPopulating) {
        return;
    }
    deck.setPublishersMarksEnabled(showCropCheck.isSelected());
}//GEN-LAST:event_showCropCheckActionPerformed

private void cropPrintWeightFieldcropFieldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cropPrintWeightFieldcropFieldStateChanged
    if (deck == null || isPopulating) {
        return;
    }
    deck.setPublishersMarkPrintWidth(((Number) cropWeightField.getValue()).floatValue());
}//GEN-LAST:event_cropPrintWeightFieldcropFieldStateChanged

    private void gameComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gameComboActionPerformed
        Game sel = gameCombo.getSelectedItem();
        if (sel != null && getDeck() != null && !isPopulating) {
            StrangeEons.setWaitCursor(true);
            try {
                getDeck().getSettings().set(Game.GAME_SETTING_KEY, sel.getCode());
            } finally {
                StrangeEons.setWaitCursor(false);
            }
        }
    }//GEN-LAST:event_gameComboActionPerformed

    private void fakeBleedCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fakeBleedCheckActionPerformed
        deck.setAutoBleedMarginEnabled(fakeBleedCheck.isSelected());
        PageView v = deck.getActivePage().getView();
        if (v != null) {
            v.repaint();
        }
    }//GEN-LAST:event_fakeBleedCheckActionPerformed

    private boolean searchListIndices(String criteria, JList list, DefaultListModel model, int start, int end) {
        for (int i = start; i < end; ++i) {
            if (((PageItem) model.get(i)).getName().toLowerCase().contains(criteria)) {
                list.setSelectedIndex(i);
                list.scrollRectToVisible(list.getCellBounds(i, i));
                return true;
            }
        }
        return false;
    }

    public void addFilesToCardList(File[] files) {
        tc();
        if (files == null) {
            return;
        }

        BusyDialog busy = BusyDialog.getCurrentDialog();
        if (busy != null) {
            busy.setProgressMaximum(files.length);
        }

        DefaultListModel model = (DefaultListModel) facesList.getModel();

        int monitorPeriod = 0;
        Settings rk = Settings.getShared();
        if (rk.get("file-monitoring-period") != null) {
            monitorPeriod = rk.getInt("file-monitoring-period");
        }

        for (int i = 0; i < files.length; ++i) {
            if (busy != null) {
                busy.setProgressCurrent(i);
            }

            File f = files[i];

            if (!Deck.isDeckLayoutSupported(f)) {
                ErrorDialog.displayError(string("de-err-add-nonsheet"), null);
                continue;
            }

            GameComponent g = ResourceKit.getGameComponentFromFile(f);

            if (g != null) {
                String name = g.getFullName();
                Sheet[] sheets = g.createDefaultSheets();

                if (sheets == null) {
                    ErrorDialog.displayError(string("de-err-add-nonsheet"), null);
                    continue;
                }

                String path = f.getAbsolutePath();
                for (int j = 0; j < sheets.length; ++j) {
                    if (!isSheetInList(model, path, j)) {
                        PageItem card = new CardFace(g, path, j);
                        model.addElement(card);
                        card.getThumbnailIcon();
                    }
                }

                if (monitorPeriod > 0) {
                    FileChangeMonitor.getSharedInstance().addFileChangeListener(this, f);
                }
                cardTileTab.setSelectedIndex(1);
            }
        }
    }

    private boolean isSheetInList(DefaultListModel model, String path, int index) {
        for (int i = 0; i < model.getSize(); ++i) {
            PageItem c = (PageItem) model.getElementAt(i);
            if (c instanceof DependentPageItem) {
                DependentPageItem dpi = (DependentPageItem) c;
                if (dpi.getPath().equals(path)) {
                    if (c instanceof CardFace) {
                        CardFace face = (CardFace) c;
                        if (face.getSheetIndex() == index) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void fileChanged(final File f, ChangeType type) {
        tc();
        if (type != ChangeType.DELETED) {
            final FileChangeListener listener = this;
            SwingUtilities.invokeLater(() -> {
                StrangeEons.setWaitCursor(true);
                GameComponent gc1 = ResourceKit.getGameComponentFromFile(f, false);
                // TODO: update of custom tiles
                if (gc1 != null && gc1.isDeckLayoutSupported()) {
                    gc1.createDefaultSheets();
                    int updates = 0, result;
                    // check card list
                    for (int i = 0; i < facesList.getModel().getSize(); ++i) {
                        PageItem c = facesList.getModel().getElementAt(i);
                        if ((result = possiblyRefreshCard(f, c, gc1)) == REFRESH_INCOMPATIBLE) {
                            // delete this entry and decrement counter so next card not missed
                            ((DefaultListModel) facesList.getModel()).remove(i--);
                        }
                        updates += result;
                    }
                    // check cards on pages
                    for (int p = 0; p < deck.getPageCount(); ++p) {
                        Page page = deck.getPage(p);
                        for (int c = 0; c < page.getCardCount(); ++c) {
                            if ((result = possiblyRefreshCard(f, page.getCard(c), gc1)) == REFRESH_INCOMPATIBLE) {
                                // delete this entry and decrement counter so next card not missed
                                page.removeCard(page.getCard(c--));
                            }
                            updates += result;
                        }
                    }
                    // if we found nothing to update, all instances must have
                    // been deleted from the deck; stop listening for changes
                    if (updates == 0) {
                        FileChangeMonitor.getSharedInstance().removeFileChangeListener(listener, f);
                    }
                }
                StrangeEons.setWaitCursor(false);
            });
        }
    }

    /**
     * Update a changed card if it matches the path of <code>f</code>. Helper
     * method for {@link #fileChanged}.
     */
    private int possiblyRefreshCard(File f, PageItem c, GameComponent component) {
        tc();

        if (c instanceof DependentPageItem) {

            DependentPageItem dpi = (DependentPageItem) c;
            if (f.equals(new File(dpi.getPath()))) {
                if (dpi instanceof CardFace) {
                    if (((CardFace) dpi).refresh(component)) {
                        return REFRESH_CHANGED;
                    } else {
                        return REFRESH_INCOMPATIBLE;
                    }
                } else {
                    if (dpi.refresh()) {
                        return REFRESH_CHANGED;
                    } else {
                        return REFRESH_INCOMPATIBLE;
                    }
                }
            }
            return REFRESH_NOT_CHANGED;
        }
        return REFRESH_NOT_CHANGED;
    }
    private static int REFRESH_NOT_CHANGED = 0;
    private static int REFRESH_CHANGED = 1;
    private static int REFRESH_INCOMPATIBLE = 2;

    @Override
    public void dispose() {
        super.dispose();
        FileChangeMonitor.getSharedInstance().removeFileChangeListener(this);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addCardsBtn;
    private javax.swing.JMenuItem addCardsItem;
    private javax.swing.JList<PageItem> boardBitsList;
    private javax.swing.JScrollPane boardBitsScroll;
    private javax.swing.JPopupMenu cardListMenu;
    private javax.swing.JTabbedPane cardTileTab;
    private javax.swing.JTextArea commentField;
    private javax.swing.JPanel commentPanel;
    private javax.swing.JPanel componentPanel;
    private javax.swing.JSpinner cropDistField;
    private javax.swing.JSpinner cropLengthField;
    private javax.swing.JPanel cropMeasurePanel;
    private javax.swing.JPanel cropPanel;
    private javax.swing.JSpinner cropPrintWeightField;
    private javax.swing.JSpinner cropWeightField;
    private javax.swing.JButton customPaperBtn;
    private javax.swing.JPanel deckPanel;
    private javax.swing.JList<PageItem> decorationsList;
    private javax.swing.JScrollPane decorationsScroll;
    private javax.swing.JList<PageItem> facesList;
    private javax.swing.JScrollPane facesScroll;
    private javax.swing.JCheckBox fakeBleedCheck;
    private javax.swing.JTextField findField;
    private javax.swing.JPanel findPanel;
    private ca.cgjennings.ui.JGameCombo gameCombo;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel lhsPanel;
    private javax.swing.JSplitPane lhsPanelSplitter;
    private javax.swing.JTabbedPane mainControlTab;
    private javax.swing.JTextField nameField;
    private javax.swing.JLabel optionLabel;
    private javax.swing.JList<PageItem> otherList;
    private javax.swing.JScrollPane otherScroll;
    private ca.cgjennings.ui.JReorderableTabbedPane pageTab;
    private javax.swing.JComboBox paperSizeCombo;
    private javax.swing.JButton remCardsBtn;
    private javax.swing.JMenuItem removeFacesItem;
    private javax.swing.JPanel rhsPanel;
    private javax.swing.JCheckBox showCropCheck;
    private javax.swing.JMenuItem sortAreaItem;
    private javax.swing.JMenuItem sortFileNameItem;
    private javax.swing.JMenuItem sortNameItem;
    private javax.swing.JList<PageItem> tilesList;
    private javax.swing.JScrollPane tilesScroll;
    private javax.swing.JList<PageItem> toolsList;
    private javax.swing.JScrollPane toolsScroll;
    // End of variables declaration//GEN-END:variables

    private PropertyChangeListener settingListener = (PropertyChangeEvent evt) -> {
        if (evt.getPropertyName().equals(Game.GAME_SETTING_KEY)) {
            reloadPaperCombo(false);
            reloadTileSets();
        }
    };

    @Override
    public void setGameComponent(Deck component) {
        if (deck != null) {
            deck.getSettings().removePropertyChangeListener(settingListener);
        }
        super.setGameComponent(component);
        if (component == null) {
            return;
        }

        deck = component;
        deck.getSettings().addPropertyChangeListener(settingListener);
    }

    public int getSelectedPageIndex() {
        return pageTab.getSelectedIndex();
    }

    private class DeckPageTabbedPane extends JCloseableTabbedPane {

        public DeckPageTabbedPane() {
            super();
            menu = new JPopupMenu();
            menu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    int tab = getSelectedIndex();
                    boolean enable = tab >= 0 && tab < getTabCount() - 1;
                    for (int i = 0; i < menu.getComponentCount(); ++i) {
                        menu.getComponent(i).setEnabled(enable);
                    }
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
            JMenuItem item = new JMenuItem(string("remove"));
            item.addActionListener((ActionEvent e) -> {
                int tab = getSelectedIndex();
                if (tab >= 0) {
                    DeckEditor.this.removeCurrentPage();
                }
            });
            menu.add(item);
            setComponentPopupMenu(menu);
        }
        private JPopupMenu menu;

        @Override
        protected boolean isTabClosable(String title, Icon icon, Component component, String tip, int index) {
            return !title.isEmpty();
        }

        @Override
        protected boolean isTabReorderable(int index) {
            return index < getTabCount() - 1;
        }

        @Override
        protected boolean canReorderTo(int oldIndex, int newIndex) {
            return newIndex < getTabCount() - 1;
        }

        @Override
        protected JWindow createDragImage(int dragIndex) {
            JWindow w = super.createDragImage(dragIndex);
            if (w != null) {
                StyleUtilities.setWindowOpacity(w, 0.8f);
            }
            return w;
        }

        @Override
        public JPopupMenu getComponentPopupMenu() {

            return super.getComponentPopupMenu();
        }

        @Override
        public void editTitle(final int i) {
            if (i >= getTabCount() - 1) {
                getToolkit().beep();
                return;
            }

            if (i < 0) {
                return;
            }

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new InlinePageNameEditor(getTitleAt(i), i);
                }
            });
        }
    }

    private static final Border INLINE_EDITOR_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, 3),
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK, 1),
                    BorderFactory.createLineBorder(Color.WHITE, 1)
            )
    );

    private class InlinePageNameEditor extends JTextField {

        public InlinePageNameEditor(String text, int tab) {
            super(text);
            setBorder(INLINE_EDITOR_BORDER);
            MarkupTargetFactory.enableTargeting(this, false);
            oldText = text;
            i = tab;
            old = pageTab.getTabComponentAt(tab);
            setMinimumSize(old.getPreferredSize());
            setPreferredSize(old.getPreferredSize());
            setFont(pageTab.getFont());

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        cancel = true;
                        done();
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        done();
                    }
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    done();
                    if (!cancel) {
                        String title = getText().trim();
                        if (title.length() == 0 || oldText.equals(title)) {
                            title = null;
                        }
                        getDeck().getPage(i).setTitle(title);
                        updatePageTitles(i);
                    }
                }
            });
            selectAll();
            pageTab.setTabComponentAt(i, this);
            requestFocusInWindow();
        }

        protected void done() {
            if (old != null) {
                if (oldText.equals(title)) {
                    // prevent clicking from converting null (dynamic Page N) to static Page N
                    cancel = true;
                }
                pageTab.setTabComponentAt(i, old);
                old.validate();
            }
            old = null;
        }
        private boolean cancel = false;
        private Component old;
        private int i;
        private String oldText;
    }

    private final AbstractDragAndDropHandler<PageItem> dragAndDropHandler = new AbstractDragAndDropHandler<PageItem>() {
        private DragManager<PageItem> manager;
        DragToken<PageItem> token;
        private KeyEventDispatcher ked = new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    int code = e.getExtendedKeyCode();
                    if (code == KeyEvent.VK_ESCAPE) {
                        manager.cancelDrag();
                        return true;
                    }
                }
                KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
                for (Field f : Commands.class.getFields()) {
                    if (Modifier.isStatic(f.getModifiers()) && AbstractCommand.class.isAssignableFrom(f.getType())) {
                        try {
                            AbstractCommand c = (AbstractCommand) f.get(null);
                            KeyStroke target = (KeyStroke) c.getValue(PageView.PAGE_VIEW_ACTION_KEY);
                            if (token != null && target != null && target.equals(ks)) {
                                viewCommandPressed(c);
                            }
                        } catch (Exception ex) {
                            StrangeEons.log.log(Level.SEVERE, null, ex);
                        }
                    }
                }
                return true;
            }

            private void viewCommandPressed(AbstractCommand c) {
                PageItem pi = token.getObject();

                boolean repaint = false;
                if (pi instanceof AbstractFlippableItem) {
                    AbstractFlippableItem fi = (AbstractFlippableItem) pi;
                    if (c == Commands.FLIP_HORZ) {
                        fi.flip();
                        repaint = true;
                    } else if (c == Commands.FLIP_VERT) {
                        fi.turnLeft();
                        fi.turnLeft();
                        fi.flip();
                        repaint = true;
                    } else if (c == Commands.TURN_LEFT) {
                        fi.turnLeft();
                        repaint = true;
                    } else if (c == Commands.TURN_RIGHT) {
                        fi.turnRight();
                        repaint = true;
                    }
                }
                if (repaint) {
                    PageView v = getActivePageView();
                    if (v != null) {
                        v.repaint();
                    }
                }
            }
        };

        @Override
        public DragToken<PageItem> createDragToken(DragManager<PageItem> manager, JComponent dragSource, Point dragPoint) {
            JList list = (JList) dragSource;
            PageItem sel = (PageItem) list.getSelectedValue();
            DragToken<PageItem> token = null;
            if (sel != null) {
                ListSelectionModel lsm = list.getSelectionModel();
                if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {
                    list.setEnabled(false);
                    sel = sel.clone();
                    if (sel instanceof CardFace) {
                        ((CardFace) sel).setAutoBleedMarginEnabled(deck.isAutoBleedMarginEnabled());
                    }
                    token = new DragToken<>(sel, ImageUtilities.iconToImage(sel.getThumbnailIcon()), 0, 0);
                }
            }
            if (token != null) {
                this.manager = manager;
                this.token = token;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ked);
            }
            return token;
        }

        @Override
        public void dragMove(DragManager<PageItem> manager, JComponent dragSource, DragToken<PageItem> token, JComponent dropTarget, Point location) {
            PageItem pi = token.getObject();
            PageView v = (PageView) dropTarget;
            v.setDropItem(pi);
        }

        @Override
        public void dragFinished(DragManager<PageItem> manager, JComponent dragSource, DragToken<PageItem> token, JComponent dropTarget) {
            // dropTarget will be null if drag cancelled, but we need to ensure
            // that the drop item is always reset
            PageView target = getActivePageView();
            if (target != null) {
                target.setDropItem(null);
            }
            dragSource.setEnabled(true);
            dragManager.setTokenVisible(true);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(ked);
            manager = null;
            token = null;
        }

        @Override
        public void dragEnter(DragManager<PageItem> manager, JComponent dragSource, DragToken<PageItem> token, JComponent dropTarget, Point location) {
            dragManager.setTokenVisible(false);
        }

        @Override
        public void dragExit(DragManager<PageItem> manager, JComponent dragSource, DragToken<PageItem> token, JComponent dropTarget) {
            dragManager.setTokenVisible(true);
            PageView v = (PageView) dropTarget;
            v.setDropItem(null);
        }

        @Override
        public boolean handleDrop(DragManager<PageItem> manager, JComponent dragSource, DragToken<PageItem> token, JComponent dropTarget, Point location) {
            PageView v = (PageView) dropTarget;
            v.drop(location);
            return true;
        }
    };
    private DragManager<PageItem> dragManager = new DragManager<>(dragAndDropHandler);

    private class PageItemList extends JList<PageItem> {
        public PageItemList() {
            super(new DefaultListModel<>());
            setCellRenderer(new PageItemRenderer());
            dragManager.addDragSource(this);
        }

        @Override
        public DefaultListModel<PageItem> getModel() {
            return (DefaultListModel<PageItem>) super.getModel();
        }

        public PageItemRenderer getPageItemRenderer() {
            // safely returns correct type after constructor completes
            return (PageItemRenderer) getCellRenderer();
        }

        @Override
        public String getToolTipText( MouseEvent event ) {
            int i = locationToIndex( event.getPoint() );
            if( i >= 0 ) {
                PageItem c = getModel().getElementAt(i);
                if( c instanceof DependentPageItem )
                return ((DependentPageItem) c).getPath();
            }
            return super.getToolTipText(event);
        }
    }
}
