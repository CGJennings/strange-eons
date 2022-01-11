package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.Marker;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.project.Actions;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.apps.arkham.project.ProjectView;
import ca.cgjennings.apps.arkham.project.TaskAction;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.FilteredListModel;
import ca.cgjennings.ui.FilteredListModel.ListFilter;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.JIconList;
import ca.cgjennings.ui.anim.AnimationUtilities;
import ca.cgjennings.ui.theme.ThemedIcon;
import gamedata.ClassMap;
import gamedata.Game;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import static resources.Language.string;
import resources.RawSettings;
import resources.ResourceKit;
import resources.Settings;

/**
 * The dialog that allows the user to select a new component type to create.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public final class NewEditorDialog extends javax.swing.JDialog {

    private static NewEditorDialog shared;

    /**
     * Returns the shared new editor dialog. The shared instance is used
     * whenever the <b>File|New</b> menu item is selected.
     *
     * @return the shared dialog instance
     */
    public static NewEditorDialog getSharedInstance() {
        if (shared == null) {
            throw new IllegalStateException("not created yet");
        }
        return shared;
    }

    /**
     * Creates a new component selection dialog.
     *
     * @param modal {@code true} if the dialog should be modal
     */
    public NewEditorDialog(boolean modal) {
        super(AppFrame.getApp(), modal);
        initComponents();
        categoryList.requestFocusInWindow();

        ImageIcon bannerIcon = (ImageIcon) banner.getIcon();
        bannerIcon.setImage(ResourceKit.createBleedBanner(bannerIcon.getImage()));
        getRootPane().setDefaultButton(okBtn);

        DefaultListCellRenderer renderer = new JIconList.IconRenderer();
        categoryList.setCellRenderer(renderer);

        if (shared == null) {
            shared = this;
            initComponentLists();
        } else {
            setModal(true);
            categories = shared.categories;
            componentLists = shared.componentLists;
            classMap = shared.classMap;
            defaultBanner = shared.defaultBanner;
            updateProjectStatus(StrangeEons.getWindow().getOpenProject());
            okBtn.setVisible(false);
        }

        categoryModel = new FilteredListModel<>(categories);
        categoryList.setModel(categoryModel);
        updateComponentList();

        final Settings s = Settings.getUser();
        if (!s.applyWindowSettings("neweditor", this)) {
            pack();
            setLocationRelativeTo(AppFrame.getApp());
        }

        String comp = RawSettings.getUserSetting(COMPONENT_CLASS_KEY);
        if (comp == null) {
            comp = DEFAULT_COMPONENT_CLASS;
        }
        selectClassInComponentList(comp);

        final ListFilter categoryFilter = (FilteredListModel mode, Object item) -> !hiddenCategories.contains((ComponentListItem) item);
        gameFilter.addFilterChangedListener((Object source) -> {
            Object filter = gameFilter.getFilterValue();
            // get the current selection, which we will try to recreate
            // after filtering
            List<ComponentListItem> selection = componentList.getSelectedValuesList();
            // determine which filter to use
            ListFilter catFilter = null;
            if (filter instanceof String) {
                catFilter = FilteredListModel.createStringFilter((String) filter);
            }
            if (filter instanceof Game) {
                final Game game = (Game) filter;
                catFilter = (FilteredListModel mode, Object item) -> {
                    if (item == null) {
                        return false;
                    }
                    Game target = ((ComponentListItem) item).getGame();
                    return target != null && (game.equals(target) || target.getCode().equals(Game.ALL_GAMES_CODE));
                };
            }
            // apply filter to all categories, track which categories become empty
            hiddenCategories.clear();
            for (int i = 0; i < categoryModel.getItemCount(); ++i) {
                FilteredListModel m = componentLists.get(i);
                m.setFilter(catFilter);
                if (m.getSize() == 0) {
                    hiddenCategories.add((ComponentListItem) categoryModel.getItem(i));
                }
            }
            // apply category filter and restore selection
            ComponentListItem selectedCat = categoryList.getSelectedValue();
            categoryModel.setFilter(categoryFilter);
            if (!hiddenCategories.contains(selectedCat)) {
                categoryList.setSelectedValue(selectedCat, false);
                // reselect any items not hidden by the filter
                for (Object sel : selection) {
                    FilteredListModel m = (FilteredListModel) componentList.getModel();
                    for (int i = 0; i < m.getSize(); ++i) {
                        if (sel == m.getElementAt(i)) {
                            componentList.addSelectionInterval(i, i);
                        }
                    }
                }
            }
        });
        gameFilter.setSelectedItem(s.get(FILTER_KEY, ""));
    }

    private Set<ComponentListItem> hiddenCategories = new HashSet<>();
    private List<ComponentListItem> categories;
    private FilteredListModel<ComponentListItem> categoryModel;
    private List<FilteredListModel<ComponentListItem>> componentLists;
    private Map<String, ComponentListItem> classMap;
    private Icon defaultBanner;

    private static final String FILTER_KEY = "new-component-filter";
    private static final String COMPONENT_CLASS_KEY = "new-component-class";
    private static final String DEFAULT_COMPONENT_CLASS = Marker.class.getName();

    /**
     * Searches the component lists for the specified entry and selects the
     * first instance of it, if any. Returns {@code true} if the class was found
     * (and selected). The name must be a fully qualified class or script
     * identifier, such as {@code ca.cgjennings.apps.arkham.component.Monster}.
     *
     * @param mapping the name of the class or script to select
     * @see gamedata.ClassMap.Entry#getMapping
     */
    public boolean selectClassInComponentList(String mapping) {
        if (mapping == null) {
            throw new NullPointerException("null className");
        }
        gameFilter.setSelectedItem("");
        for (int i = 0; i < componentLists.size(); ++i) {
            FilteredListModel list = componentLists.get(i);
            for (int j = 0; j < list.getItemCount(); ++j) {
                ComponentListItem item = (ComponentListItem) list.getItem(j);
                if (item.entry.getName().equals(mapping) || item.entry.getMapping().equals(mapping)) {
                    categoryList.setSelectedIndex(i);
                    componentList.setSelectedIndex(j);
                    categoryList.ensureIndexIsVisible(i);
                    componentList.ensureIndexIsVisible(j);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return the class name of the currently selected component type, or
     * {@code null} if no component is selected.
     *
     * @return the fully qualified class name of the selected component
     */
    public String getSelectedClassInComponentList() {
        Object o = componentList.getSelectedValue();
        if (o == null) {
            return null;
        }
        return ((ComponentListItem) o).entry.getMapping();
    }

    /**
     * Returns the document tab icon for a game component. Used by some scripted
     * components to determine the correct icon.
     *
     * @param gc the game component
     * @return the icon for the component
     * @deprecated No suitable replacement yet, but don't want people to start
     * using it.
     */
    @Deprecated
    public Icon getIconForComponent(GameComponent gc) {
        String targetClass = gc.getClass().getName();
        for (int i = 0; i < componentLists.size(); ++i) {
            FilteredListModel list = componentLists.get(i);
            for (int j = 0; j < list.getItemCount(); ++j) {
                ComponentListItem item = (ComponentListItem) list.getItem(j);
                if (item.entry.getMapping().equals(targetClass)) {
                    return item.getIcon();
                }
            }
        }
        if (defaultIcons == null) {
            defaultIcons = new Icon[]{
                new ThemedIcon("editors/blank-editor-icon.png", false),
                new ThemedIcon("editors/app-new-deck.png", false)
            };
        }
        if (gc instanceof Deck) {
            return defaultIcons[1];
        }
        return defaultIcons[0];
    }

    private static Icon[] defaultIcons = null;

    private void updateComponentList() {
        ComponentListItem sel = categoryList.getSelectedValue();
        if (sel == null) {
            return;
        }
        int i = ((FilteredListModel) categoryList.getModel()).getUnfilteredIndex(sel);
        if (i < 0) {
            throw new AssertionError("cat not in model");
        }

        componentList.setModel(componentLists.get(i));

        final Icon existingBanner = banner.getIcon();
        final Icon newBanner = sel.getBanner();
        if (existingBanner != newBanner) {
            if (Settings.getShared().getYesNo("advanced-ui-effects")) {
                AnimationUtilities.animateIconTransition(banner, newBanner);
            } else {
                banner.setIcon(newBanner);
            }
        }
    }

    private void initComponentLists() {
        categories = new ArrayList<>();
        defaultBanner = banner.getIcon();
        componentLists = new ArrayList<>();
        classMap = new HashMap<>();

        for (String file : gamedata.ClassMap.getClassMapFiles()) {
            parseEditorFile(file);
        }

        // delete empty categories
        List<Integer> empties = new ArrayList<>();
        for (int i = 0; i < categories.size(); ++i) {
            if (componentLists.get(i).getItemCount() == 0) {
                empties.add(i);
            }
        }
        for (int i = empties.size() - 1; i >= 0; --i) {
            int index = empties.get(i);
            categories.remove(index);
            componentLists.remove(index);
        }

        // create Everything category
        TreeSet<ComponentListItem> types = new TreeSet<>();
        for (int i = 0; i < categories.size(); ++i) {
            FilteredListModel list = componentLists.get(i);
            for (int j = 0; j < list.getItemCount(); ++j) {
                types.add((ComponentListItem) list.getItem(j));
            }
        }

        // build the "Everything" category and simultaneously log all entries
        StrangeEons.log.info("parsed all class map entries");
        categories.add(new ComponentListItem(ClassMap.ENTRY_EVERYTHING_CATEGORY));
        FilteredListModel<ComponentListItem> model = new FilteredListModel<>();
        for (ComponentListItem entry : types) {
            model.add(entry);
            StrangeEons.log.log(Level.FINE, "    {0}", entry.entry);
        }
        componentLists.add(model);
    }

    private void parseEditorFile(String resource) {
        try {
            int category = -1;
            ClassMap.Parser parser = new ClassMap.Parser(resource, false);
            ClassMap.Entry entry;
            while ((entry = parser.next()) != null) {
                if (entry.getType() == ClassMap.EntryType.CATEGORY) {
                    // find or create the category's list
                    category = -1;
                    for (int i = 0; i < categories.size(); ++i) {
                        if (categories.get(i).entry.getName().equals(entry.getName())) {
                            category = i;
                            break;
                        }
                    }
                    if (category < 0) {
                        category = categories.size();
                        categories.add(new ComponentListItem(entry));
                        componentLists.add(new FilteredListModel<>());
                    }
                } else {
                    ComponentListItem item = new ComponentListItem(entry);
                    componentLists.get(category).add(item);
                    classMap.put(entry.getKey(), item);
                }
            }
        } catch (IOException ioe) {
            ErrorDialog.displayError(string("rk-err-classmap", resource), ioe);
            return;
        }

//		String line;
//		int category = -1;
//		EscapedLineReader reader = null;
//		try {
//			URL url = ResourceKit.composeResourceURL( resource );
//			if( url == null ) {
//				ErrorDialog.displayError( string( "rk-err-classmap", resource ), null );
//				return;
//			}
//			reader = new EscapedLineReader( url.openStream() );
//			while( (line = reader.readNonemptyLine()) != null ) {
//				String[] tokens = line.split( "=" );
//				if( tokens.length < 1 ) {
//					ErrorDialog.displayError( string( "rk-err-parse-classmap", resource, line ), null );
//				}
//				tokens[0] = tokens[0].trim();
//				if( tokens.length == 1 ) {
//					ComponentListItem newCategory = new ComponentListItem( tokens[0], null );
//					category = -1;
//					for( int i=0; i < categories.size(); ++i ) {
//						if( categories.get( i ).entry.equals( newCategory.entry ) ) {
//							category = i;
//							break;
//						}
//					}
//					// category doesn't already exist: create it
//					if( category < 0 ) {
//						category = categories.size();
//						categories.add( newCategory );
//						componentLists.add( new FilteredListModel() );
//					}
//				} else {
//					if( category < 0 || tokens.length != 2 ) {
//						ErrorDialog.displayError( string( "rk-err-parse-classmap", resource, line ), null );
//					}
//					ComponentListItem item = new ComponentListItem( tokens[0], tokens[1].trim() );
//					componentLists.get( category ).add( item );
//					classMap.put( tokens[0], item );
//				}
//			}
//		} catch( IOException e ) {
//			ErrorDialog.displayError( string( "rk-err-classmap", resource ), e );
//		} finally {
//			if( reader != null ) {
//				try {
//					reader.close();
//				} catch( IOException e ) {
//				}
//			}
//		}
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        okBtn = new javax.swing.JButton();
        banner = new javax.swing.JLabel();
        componentScroll = new javax.swing.JScrollPane();
        componentList = new javax.swing.JList<>();
        catScroll = new javax.swing.JScrollPane();
        categoryList = new javax.swing.JList<>();
        javax.swing.JLabel catLabel = new javax.swing.JLabel();
        javax.swing.JLabel componentLabel = new javax.swing.JLabel();
        createInProjectBtn = new javax.swing.JButton();
        helpBtn = new ca.cgjennings.ui.JHelpButton();
        gameFilter = new ca.cgjennings.ui.JGameFilterField();
        tipLabel = new javax.swing.JLabel();
        headingUnderline = new ca.cgjennings.ui.JHeading();
        heading = new ca.cgjennings.ui.JHeading();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("app-new-dialog-title")); // NOI18N
        setName("Form"); // NOI18N

        okBtn.setText(string("app-new-create")); // NOI18N
        okBtn.setName("okBtn"); // NOI18N
        okBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handleOKAction(evt);
            }
        });

        banner.setBackground(java.awt.Color.darkGray);
        banner.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/editors/default-category.jpg"))); // NOI18N
        banner.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        banner.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, java.awt.Color.gray));
        banner.setName("banner"); // NOI18N
        banner.setOpaque(true);

        componentScroll.setName("componentScroll"); // NOI18N

        componentList.setCellRenderer( new Renderer() );
        componentList.setName("componentList"); // NOI18N
        componentList.setVisibleRowCount(12);
        componentList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                componentListMouseClicked(evt);
            }
        });
        componentList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                componentListValueChanged(evt);
            }
        });
        componentScroll.setViewportView(componentList);

        catScroll.setName("catScroll"); // NOI18N

        categoryList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        categoryList.setName("categoryList"); // NOI18N
        categoryList.setVisibleRowCount(12);
        categoryList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                categoryListValueChanged(evt);
            }
        });
        catScroll.setViewportView(categoryList);

        catLabel.setText(string("app-new-category")); // NOI18N
        catLabel.setName("catLabel"); // NOI18N

        componentLabel.setText(string("app-new-component-type")); // NOI18N
        componentLabel.setName("componentLabel"); // NOI18N

        createInProjectBtn.setText(string( "app-new-create-proj" )); // NOI18N
        createInProjectBtn.setEnabled(false);
        createInProjectBtn.setName("createInProjectBtn"); // NOI18N
        createInProjectBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createInProjectBtnActionPerformed(evt);
            }
        });

        helpBtn.setHelpPage("gc-intro");
        helpBtn.setName("helpBtn"); // NOI18N

        gameFilter.setFont(gameFilter.getFont().deriveFont(gameFilter.getFont().getSize()-1f));
        gameFilter.setName("gameFilter"); // NOI18N

        tipLabel.setFont(tipLabel.getFont().deriveFont(tipLabel.getFont().getSize()-1f));
        tipLabel.setText(string( "app-new-tip" )); // NOI18N
        tipLabel.setName("tipLabel"); // NOI18N

        headingUnderline.setName("headingUnderline"); // NOI18N

        heading.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("resources/text/interface/eons-text"); // NOI18N
        heading.setText(bundle.getString("app-new-title")); // NOI18N
        heading.setName("heading"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(banner)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(heading, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(gameFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(catScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(catLabel))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(componentLabel, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(componentScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(100, 100, 100)
                                .addComponent(okBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(createInProjectBtn))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tipLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(headingUnderline, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(heading, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gameFilter, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(2, 2, 2)
                .addComponent(headingUnderline, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(catLabel)
                    .addComponent(componentLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(catScroll)
                    .addComponent(componentScroll))
                .addGap(1, 1, 1)
                .addComponent(tipLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(createInProjectBtn)
                    .addComponent(okBtn)
                    .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addComponent(banner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void categoryListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_categoryListValueChanged
            updateComponentList();
	}//GEN-LAST:event_categoryListValueChanged

	private void componentListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_componentListMouseClicked
            if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() >= 2) {
                createSelectedComponent(!evt.isAltDown(), false);
            }
	}//GEN-LAST:event_componentListMouseClicked

    @Override
    public void dispose() {
        if (this == shared) {
            final Settings s = Settings.getUser();
            s.storeWindowSettings("neweditor", this);
            Object filter = gameFilter.getSelectedItem();
            if (filter != null) {
                s.set(FILTER_KEY, filter.toString());
            }
        }
        super.dispose();
    }

	private void handleOKAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_handleOKAction
            createSelectedComponent(
                    (evt.getModifiers() & ActionEvent.ALT_MASK) == 0,
                    (evt.getModifiers() & ActionEvent.SHIFT_MASK) != 0
            );
	}//GEN-LAST:event_handleOKAction

	private void createInProjectBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createInProjectBtnActionPerformed
            boolean success = createSelectedComponent(
                    (evt.getModifiers() & ActionEvent.ALT_MASK) == 0,
                    (evt.getModifiers() & ActionEvent.SHIFT_MASK) != 0
            );
            if (success) {
                StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
                if (ed == null) {
                    throw new AssertionError("active editor was null");
                }
                ProjectView view = AppFrame.getApp().getOpenProjectView();

                Member[] sel = view.getSelectedMembers();
                Member parent = sel.length == 0 ? view.getProject() : sel[0];
                while (parent != view.getProject() && !parent.isFolder()) {
                    parent = parent.getParent();
                }

                File f = new File(parent.getFile(), string("pa-new-comp-name") + ".eon");
                f = ProjectUtilities.getAvailableFile(f);
                ed.setFile(f);
                ed.save();
                parent.synchronize();
                Member m = view.getProject().findMember(f);
                if (m != null) {
                    view.select(m);
                    TaskAction rename = Actions.findActionByName("rename");
                    rename.perform(view.getProject(), m.getTask(), m);
                }
            }
	}//GEN-LAST:event_createInProjectBtnActionPerformed

    private void componentListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_componentListValueChanged
        boolean enable = componentList.getSelectedIndex() >= 0;
        okBtn.setEnabled(enable);
        createInProjectBtn.setEnabled(enable && projectIsOpen);
    }//GEN-LAST:event_componentListValueChanged

    public void updateProjectStatus(Project openProject) {
        projectIsOpen = openProject != null;
        componentListValueChanged(null);
    }
    private boolean projectIsOpen;

    private boolean createSelectedComponent(boolean closeWindow, boolean debug) {
        try {
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getGlassPane().setVisible(true);

            Object value = componentList.getSelectedValue();
            if (value != null) {
                ComponentListItem item = (ComponentListItem) value;

                boolean success = item.createEditor(debug) != null;

                // remember the type of the last thing we made
                if (success) {
                    RawSettings.setUserSetting(COMPONENT_CLASS_KEY, item.entry.getMapping());
                }

                if (closeWindow && success) {
                    // Workaround for 100% CPU bug
                    //
                    // If the window is only hidden, this window gets a shitstorm
                    // of IgnorePaintEvents that drives CPU usage to 100% until
                    // the window becomes visible again.
                    dispose();
                } else {
                    toFront();
                }
                return success;
            }
        } catch (Exception e) {
            ErrorDialog.displayError(string("app-new-create-err"), e);
        } finally {
            getGlassPane().setVisible(false);
            getGlassPane().setCursor(Cursor.getDefaultCursor());
        }
        return false;
    }

    /**
     * Creates an editor for a class map key; provides the effective
     * implementation for
     * {@link ca.cgjennings.apps.arkham.StrangeEons#createEditor(java.lang.String)}.
     *
     * @param key the key to create an editor for
     * @return the new editor
     * @throws IllegalArgumentException if the key is invalid
     * @throws InstantiationException if the editor cannot be created
     */
    StrangeEonsEditor createEditorFromClassMapKey(String key) throws InstantiationException {
        ComponentListItem item = classMap.get(key);
        if (item == null) {
            item = classMap.get("@" + key);
        }
        if (item == null) {
            item = classMap.get("app-new-" + key);
        }
        if (item == null) {
            item = classMap.get("@app-new-" + key);
        }
        if (item != null) {
            return item.createEditor(false);
        }
        throw new IllegalArgumentException("not a valid class map key: " + key);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel banner;
    private javax.swing.JScrollPane catScroll;
    private javax.swing.JList<ComponentListItem> categoryList;
    private javax.swing.JList<ComponentListItem> componentList;
    private javax.swing.JScrollPane componentScroll;
    private javax.swing.JButton createInProjectBtn;
    private ca.cgjennings.ui.JGameFilterField gameFilter;
    private ca.cgjennings.ui.JHeading heading;
    private ca.cgjennings.ui.JHeading headingUnderline;
    private ca.cgjennings.ui.JHelpButton helpBtn;
    private javax.swing.JButton okBtn;
    private javax.swing.JLabel tipLabel;
    // End of variables declaration//GEN-END:variables

    private final class ComponentListItem implements Comparable<ComponentListItem>, IconProvider {

        private ClassMap.Entry entry;
        private final Icon gameIcon;

        public ComponentListItem(ClassMap.Entry entry) {
            this.entry = entry;

            Game game = entry.getGame();
            if (game == null || game == Game.getAllGamesInstance()) {
                gameIcon = new BlankIcon(18);
            } else {
                gameIcon = game.getIcon();
            }
        }

        @Override
        public String toString() {
            return entry.getName();
        }

        @Override
        public Icon getIcon() {
            return entry.getIcon();
        }

        public Icon getGameIcon() {
            return gameIcon;
        }

        public Icon getBanner() {
            Icon b = entry.getBanner();
            if (b == null) {
                b = defaultBanner;
            }
            return b;
        }

        public Game getGame() {
            return entry.getGame();
        }

        public ClassMap.Entry getClassMapEntry() {
            return entry;
        }

        @Override
        public int compareTo(ComponentListItem o) {
            return entry.compareTo(o.entry);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof ComponentListItem)) {
                return false;
            }
            return entry.equals(((ComponentListItem) o).entry);
        }

        @Override
        public int hashCode() {
            return entry.hashCode();
        }

        public StrangeEonsEditor createEditor(boolean debug) {
            return entry.createEditor(debug);
        }
    }

    private static class Renderer extends JPanel implements ListCellRenderer<ComponentListItem> {

        private final DefaultListCellRenderer lhs = new DefaultListCellRenderer();
        private final DefaultListCellRenderer rhs = new DefaultListCellRenderer();
        private final Border empty = BorderFactory.createEmptyBorder();

        Renderer() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1d;
            gbc.anchor = GridBagConstraints.WEST;
            add(lhs, gbc);
            gbc.gridx = 1;
            gbc.weightx = 0d;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.ipadx = 8;
            add(rhs, gbc);
            rhs.setHorizontalAlignment(DefaultListCellRenderer.TRAILING);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ComponentListItem> list, ComponentListItem value, int index, boolean isSelected, boolean cellHasFocus) {
            lhs.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            rhs.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            lhs.setIcon(value.getIcon());
            rhs.setIcon(value.getGameIcon());
            rhs.setText(null);
            setBorder(lhs.getBorder());
            lhs.setBorder(empty);
            rhs.setBorder(empty);
            lhs.setOpaque(false);
            rhs.setOpaque(false);
            setOpaque(isSelected);
            setBackground(isSelected ? lhs.getBackground() : list.getBackground());
            return this;
        }
    };
}
