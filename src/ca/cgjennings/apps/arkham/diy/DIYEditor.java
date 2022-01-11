package ca.cgjennings.apps.arkham.diy;

import ca.cgjennings.apps.arkham.*;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.spelling.ui.JSpellingTextArea;
import java.awt.Graphics2D;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import static resources.Language.string;

/**
 * The game component editor used to edit scriptable {@link DIY} components.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
@SuppressWarnings("serial")
public class DIYEditor extends AbstractGameComponentEditor<DIY> {

    public DIYEditor(DIY diy) {
        initComponents();
        localizeForPlatform();

        replaceEditedComponent(diy);

        initializeSheetViewers(frontBackPane);
        populateFieldsFromComponent();
        getGameComponent().markSaved();
        createTimer();
    }

    public DIYEditor() {
        this(DIY.createTestInstance(new Handler() {

            @Override
            public void create(DIY diy) {
                writer.println("Dummy DIY: create");
            }

            @Override
            public void createInterface(DIY diy, DIYEditor editor) {
                writer.println("Dummy DIY: createInterface");
            }

            @Override
            public void createFrontPainter(DIY diy, DIYSheet sheet) {
                writer.println("Dummy DIY: createFrontPainter");
            }

            @Override
            public void createBackPainter(DIY diy, DIYSheet sheet) {
                writer.println("Dummy DIY: createBackPainter");
            }

            @Override
            public void paintFront(Graphics2D g, DIY diy, DIYSheet sheet) {
                writer.println("Dummy DIY: paintFront");
            }

            @Override
            public void paintBack(Graphics2D g, DIY diy, DIYSheet sheet) {
                writer.println("Dummy DIY: paintBack");
            }

            @Override
            public void onRead(DIY diy, ObjectInputStream objectInputStream) {
                writer.println("Dummy DIY: onRead");
            }

            @Override
            public void onWrite(DIY diy, ObjectOutputStream objectOutputStream) {
                writer.println("Dummy DIY: onWrite");
            }

            @Override
            public void onClear(DIY diy) {
                writer.println("Dummy DIY: clearAll");
            }

            @Override
            public Portrait getPortrait(int index) {
                writer.println("Dummy DIY: getPortrait " + index);
                return null;
            }

            @Override
            public int getPortraitCount() {
                writer.println("Dummy DIY: getPortraitCount");
                return 0;
            }

            private PrintWriter writer = ScriptMonkey.getSharedConsole().getErrorWriter();

        }, null));
    }

    private boolean testMode;

    /**
     * Sets whether test mode is enabled. When enabled, the editor will never
     * indicate that it has unsaved changes and neither the
     * {@link Commands#SAVE SAVE} nor {@link Commands#SAVE_AS SAVE_AS} commands
     * will be considered applicable. The {@code diy} scripting library's
     * {@code testDIYScript()} function will call this method to enable test
     * mode on the editor it creates.
     *
     * @param enable if {@code true}, test mode will be enabled
     * @see #isTestModeEnabled()
     * @see #hasUnsavedChanges()
     * @see #isCommandApplicable
     */
    public void setTestModeEnabled(boolean enable) {
        testMode = enable;
        if (testMode) {
            setUnsavedChanges(false);
        }
    }

    /**
     * Returns {@code true} if test mode is enabled.
     *
     * @return {@code true} if the editor's test mode is enabled
     */
    public boolean isTestModeEnabled() {
        return testMode;
    }

    @Override
    protected void populateComponentFromDelayedFields() {
        DIY diy = getGameComponent();

        if (diy.getNameField() != null) {
            diy.setName(diy.getNameField().getText());
        }

        diy.setComment(commentField.getText());
    }

    @Override
    public void populateFieldsFromComponent() {
        DIY diy = getGameComponent();

        if (diy.getNameField() != null) {
            diy.getNameField().setText(diy.getName());
        }
        commentField.setText(diy.getComment());
        commentField.select(0, 0);

        if (diy.getPortraitKey() != null) {
            portraitAdjPanel.updatePanel();
        }

        super.populateFieldsFromComponent();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        controlTabPane = new javax.swing.JTabbedPane();
        imagePanel = new javax.swing.JPanel();
        portraitAdjPanel = new ca.cgjennings.apps.arkham.PortraitPanel();
        markerAdjPanel = new ca.cgjennings.apps.arkham.PortraitPanel();
        commentPanel = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        commentField = new JSpellingTextArea();
        frontBackPane = new javax.swing.JTabbedPane();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setName("Form"); // NOI18N
        getContentPane().setLayout(new java.awt.GridBagLayout());

        controlTabPane.setName("controlTabPane"); // NOI18N

        imagePanel.setName("imagePanel"); // NOI18N

        portraitAdjPanel.setName("portraitAdjPanel"); // NOI18N

        markerAdjPanel.setName("markerAdjPanel"); // NOI18N
        markerAdjPanel.setPanelTitle(string("ae-panel-portrait-marker")); // NOI18N

        javax.swing.GroupLayout imagePanelLayout = new javax.swing.GroupLayout(imagePanel);
        imagePanel.setLayout(imagePanelLayout);
        imagePanelLayout.setHorizontalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(portraitAdjPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(markerAdjPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        imagePanelLayout.setVerticalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(portraitAdjPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(markerAdjPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        controlTabPane.addTab(string("ae-tab-portrait"), imagePanel); // NOI18N

        commentPanel.setName("commentPanel"); // NOI18N

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder(string("ae-panel-drc"))); // NOI18N
        jPanel12.setName("jPanel12"); // NOI18N

        jLabel19.setText(string("ae-l-drc")); // NOI18N
        jLabel19.setName("jLabel19"); // NOI18N

        jScrollPane7.setName("jScrollPane7"); // NOI18N

        commentField.setColumns(20);
        commentField.setLineWrap(true);
        commentField.setRows(5);
        commentField.setTabSize(4);
        commentField.setWrapStyleWord(true);
        commentField.setDragEnabled(true);
        commentField.setName("commentField"); // NOI18N
        jScrollPane7.setViewportView(commentField);

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel12Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE))
                    .addComponent(jLabel19, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout commentPanelLayout = new javax.swing.GroupLayout(commentPanel);
        commentPanel.setLayout(commentPanelLayout);
        commentPanelLayout.setHorizontalGroup(
            commentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        commentPanelLayout.setVerticalGroup(
            commentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commentPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel12.getAccessibleContext().setAccessibleName(string("ae-panel-drc")); // NOI18N

        controlTabPane.addTab(string("ae-tab-comments"), commentPanel); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(controlTabPane, gridBagConstraints);

        frontBackPane.setMinimumSize(new java.awt.Dimension(1, 1));
        frontBackPane.setName("frontBackPane"); // NOI18N
        frontBackPane.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(frontBackPane, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-827)/2, (screenSize.height-483)/2, 827, 483);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Removes all editor tabs that were added using script code via the
     * {@code uilayout} library's {@code AbstractContainer.addToEditor} method.
     */
    public void removeCustomTabs() {
        for (int i = controlTabPane.getTabCount() - 1; i >= 0; --i) {
            JComponent c = (JComponent) controlTabPane.getComponentAt(i);
            if (c.getClientProperty("se-custom-tab") != null) {
                controlTabPane.remove(i);
            }
        }
    }

    @Override
    public void replaceEditedComponent(DIY diy) {
        removeAllStrangeEonsListeners(); // ???
        removeCustomTabs();

        String eventScript = diy.getSettings().get(ScriptMonkey.ON_INSTALL_EVENT_KEY);
        if (eventScript != null) {
            ScriptMonkey monkey = new ScriptMonkey(ScriptMonkey.ON_INSTALL_EVENT_KEY);
            monkey.bind(PluginContextFactory.createDummyContext());
            monkey.bind(ScriptMonkey.VAR_EDITOR, this);
            monkey.bind(ScriptMonkey.VAR_COMPONENT, diy);
            monkey.eval(eventScript);
            monkey.call("onInstall", this, diy);
        }

        // set the component and create new sheets
        setGameComponent(diy);

        if (diy != null) {
            diy.createInterface(diy, this);

            int portraitTabIndex = -1;
            for (int i = 0; i < controlTabPane.getTabCount(); ++i) {
                if (controlTabPane.getComponentAt(i) == imagePanel) {
                    portraitTabIndex = i;
                    break;
                }
            }

            if (diy.getPortraitKey() != null) {
                portraitAdjPanel.setPortrait(diy.getPortrait(0));
                if (portraitTabIndex < 0) {
                    controlTabPane.add(imagePanel, controlTabPane.getTabCount() - 1);
                }
                if (diy.getFaceStyle() == DIY.FaceStyle.CARD_AND_MARKER) {
                    markerAdjPanel.setParentPanel(portraitAdjPanel);
                    markerAdjPanel.setVisible(true);
                    markerAdjPanel.setPortrait(diy.getPortrait(1));
                } else {
                    markerAdjPanel.setPortrait(null);
                    markerAdjPanel.setVisible(false);
                }
            } else {
                portraitAdjPanel.setPortrait(null);
                if (portraitTabIndex >= 0) {
                    controlTabPane.remove(portraitTabIndex);
                }
                markerAdjPanel.setPortrait(null);
                markerAdjPanel.setVisible(false);
            }

            // Finally, update the editor controls to match the new character.
            populateFieldsFromComponent();

            // ensure the first tab is selected
            controlTabPane.setSelectedIndex(0);
        }

        fixSheetViewers();
    }

    private void fixSheetViewers() {
        // TODO: what if # sheets doesn't match
        for (int i = 0; i < frontBackPane.getTabCount(); ++i) {
            SheetViewer viewer = (SheetViewer) frontBackPane.getComponentAt(i);
            viewer.setSheet(sheets[i]);
        }
    }

    /**
     * Returns the portrait panel for the component's built-in portrait. If
     * there is no built-in portrait, returns {@code null}.
     *
     * @return the portrait panel for the built-in portrait, or {@code null}
     */
    public PortraitPanel getPortraitPanel() {
        DIY diy = getGameComponent();

        PortraitPanel panel = null;
        if (diy != null && diy.getPortraitKey() != null) {
            panel = portraitAdjPanel;
        }
        return panel;
    }

    /**
     * Returns the portrait panel for the component's built-in marker portrait.
     * If there is no built-in marker portrait, returns {@code null}.
     *
     * @return the portrait panel for the built-in marker, or {@code null}
     */
    public PortraitPanel getMarkerPanel() {
        DIY diy = getGameComponent();

        PortraitPanel panel = null;
        if (diy != null && diy.getPortraitKey() != null && diy.getFaceStyle() == DIY.FaceStyle.CARD_AND_MARKER) {
            panel = markerAdjPanel;
        }
        return panel;
    }

    /**
     * Returns the tabbed pane that component editing panels are placed in.
     *
     * @return the tabbed pane that is the ancestor of the editing controls
     */
    public JTabbedPane getTabbedPane() {
        return controlTabPane;
    }

    /**
     * Returns the sheet viewer that is displaying the face with the specified
     * index.
     *
     * @param index the index of the face to obtain the view for
     * @return the requested sheet viewer
     * @throws IndexOutOfBoundsException if the sheet index is less than 0 or
     * greater than or equal to the number of faces
     */
    public SheetViewer getSheetViewer(int index) {
        if (index < 0 || index >= viewers.length) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        return viewers[index];
    }

    @Override
    public boolean hasUnsavedChanges() {
        if (isTestModeEnabled()) {
            return false;
        }
        return super.hasUnsavedChanges();
    }

    @Override
    public boolean isCommandApplicable(AbstractCommand command) {
        boolean cpc = super.isCommandApplicable(command);
        if (cpc && isTestModeEnabled()) {
            // since no unsaved changes, regular SAVE already disabled
            if (command == Commands.SAVE_AS) {
                cpc = false;
            }
        }
        return cpc;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea commentField;
    private javax.swing.JPanel commentPanel;
    private javax.swing.JTabbedPane controlTabPane;
    private javax.swing.JTabbedPane frontBackPane;
    private javax.swing.JPanel imagePanel;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JScrollPane jScrollPane7;
    private ca.cgjennings.apps.arkham.PortraitPanel markerAdjPanel;
    private ca.cgjennings.apps.arkham.PortraitPanel portraitAdjPanel;
    // End of variables declaration//GEN-END:variables
}
