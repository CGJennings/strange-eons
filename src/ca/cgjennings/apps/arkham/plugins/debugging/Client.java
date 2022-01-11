package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import static ca.cgjennings.apps.arkham.plugins.debugging.Command.escapeHTML;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.AbstractPixelwiseFilter;
import ca.cgjennings.graphics.filters.CompoundPixelwiseFilter;
import ca.cgjennings.graphics.filters.GreyscaleFilter;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.DefaultTreeCellRenderer;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.IconBorder;
import ca.cgjennings.ui.TreeLabelExposer;
import ca.cgjennings.ui.table.JHeadlessTable;
import ca.cgjennings.ui.table.MultilineTableCellRenderer;
import ca.cgjennings.ui.textedit.HTMLStyler;
import ca.cgjennings.ui.textedit.Token;
import ca.cgjennings.ui.textedit.TokenType;
import ca.cgjennings.ui.textedit.tokenizers.JavaScriptTokenizer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.FocusManager;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.Painter;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import resources.Settings;

/**
 * The script debugger client application. This client-side application allows
 * the user to control the debugging process by connecting to a Strange Eons
 * script debug server.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public final class Client extends javax.swing.JFrame {

    /**
     * Creates a new debugger client window.
     */
    private Client() {
        try {
            readSettings();
        } catch (IOException e) {
            System.err.println("Warning: failed to read debugger client settings");
            e.printStackTrace();
        }

        debugIcon = new ImageIcon(image("icons/application/db64.png"));

        initComponents();
        statusPanel.add(status, BorderLayout.EAST);
        new TreeLabelExposer(scopeTree);

        initFrame();
        initTimer();
        updateToolState();

        hostField.setText(host);
        portField.setValue(port);
    }

    /**
     * Sends {@code command} to the debug server with the given arguments,
     * returning the response as an array of lines.
     *
     * @param command the command to perform
     * @param args the arguments (may be {@code null} if no arguments)
     * @return the result of the command as an array of lines, or {@code null}
     * if there was an error
     * @throws NullPointerException if {@code name} is null
     * @throws IllegalArgumentException if the number of arguments is incorrect
     */
    private String[] perform(Command command, String... args) {
        waitCursor(true);
        try {
            return performQuietly(command, args);
        } finally {
            waitCursor(false);
        }
    }

    /**
     * Evaluates an expression in the requested stack frame and returns the
     * result as a string. Expressions can only be evaluated when a script is
     * interrupted.
     *
     * @param expression the expression to evaluate
     * @param stackFrame the stack frame to evaluate (0 for the top frame, -1 to
     * use the default frame)
     * @return the value of the expression as a string, or {@code null}
     */
    private String eval(String expression, int stackFrame) {
        threadAssert();
        stackFrame = stackIndex(stackFrame);
        String result = null;
        if (isInterrupted()) {
            final String[] response = perform(Command.EVAL, String.valueOf(stackFrame), expression);
            result = mergeResult(response);
            if (result == null) {
                checkConnection();
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if a script is currently interrupted.
     *
     * @return {@code true} if connected to a server and the client has detected
     * that the server has reached a breakpoint
     */
    private boolean isInterrupted() {
        threadAssert();
        return connected && interrupted;
    }

    /**
     * Returns {@code true} if the client is connected to a debug server.
     *
     * @return if the server was available the last time the client checked
     */
    private boolean isConnected() {
        return connected;
    }

    /**
     * Actively checks whether the connection is still valid. Typically, this is
     * called after {@link #perform} returns {@code null} to check whether a
     * connection is still valid. If the connection appears to be broken, this
     * method will set the connection flag to {@code false} (see
     * {@link #isConnected()}), then start a thread that will attempt to
     * reconnect automatically.
     */
    private void checkConnection() {
        threadAssert();

        final long START_TIME = System.currentTimeMillis();
        if (connected) {
            if (performQuietly(Command.PROBE) != null) {
                updateToolState();
                showPanel("main");
                return;
            }
        }

        connected = false;
        updateToolState();

        if (autoConnect) {
            showPanel("connecting");
            Thread connectThread = new Thread(() -> {
                // already tried once
                final int MAX_ATTEMPTS = CONNECT_RETRY_LIMIT - 1;
                for (int retry = 0; retry < MAX_ATTEMPTS && (System.currentTimeMillis() - START_TIME) < 10_000; ++retry) {
                    System.err.println("Connection failed; retry #" + (retry + 1));
                    if (perform(Command.PROBE) != null) {
                        EventQueue.invokeLater(() -> {
                            updateToolState();
                            synchAll();
                            // synchAll will switch to main panel when done
                        });
                        reconnectTimer.stop();
                        connected = true;
                        return;
                    }
                }
                connected = false;
                errorLabel.setVisible(true);
                errorLabel2.setVisible(true);
                showPanel("connect");
            }, "Server Connection Thread");
            connectThread.setDaemon(true);
            connectThread.start();
        }
    }
    // The number of times to try connecting to the server
    // before assuming that it is offline.
    private static final int CONNECT_RETRY_LIMIT = 5;

    /**
     * Called by the public API to check that the EDT thread is used.
     */
    private void threadAssert() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("the public API must be called from the event dispatch thread; see java.awt.EventQueue.invokeLater");
        }
    }

    /**
     * Sets the content of the file display to {@code sourceText} and scrolls to
     * the indicated line.
     *
     * @param sourceText the source code of the file to display (may be
     * {@code null})
     * @param line the number of the line to highlight (counting from 0)
     */
    private void showFile(String sourceText, int line) {
        if (sourceText == null) {
            source.setSource(null);
            return;
        }
        source.setSource(sourceText);
        if (line < 0) {
            line = 0;
        }
        if (line >= source.lines.length) {
            line = source.lines.length - 1;
        }

        // scroll line to center of scroll panel
        Rectangle rect = sourceTable.getCellRect(line, 0, true);
        if (rect == null) {
            return;
        }
        rect.x = sourceTable.getVisibleRect().x;
        rect.width = 1;
        rect.y -= sourceScroll.getHeight() / 2;
        if (rect.y < 0) {
            rect.y = 0;
        }
        rect.height = sourceScroll.getHeight();
        sourceTable.scrollRectToVisible(rect);
    }

    /**
     * Updates the status label with a standard status message.
     */
    private void updateStatus() {
        boolean hold = false;
        String text;
        if (connected) {
            if (interrupted) {
                wasInterruptedSinceLastConnection = true;
                text = string("msg-interrupted", stackModel.get(0), interruptingException == null ? interruptedThread : interruptingException);
            } else {
                if (wasInterruptedSinceLastConnection) {
                    text = string("msg-running");
                } else {
                    text = string("msg-connected");
                }
            }
        } else {
            hold = true;
            wasInterruptedSinceLastConnection = false;
            text = string("msg-not-connected");
        }
        status.setHold(hold);
        status.setText(text);
    }
    private boolean wasInterruptedSinceLastConnection;
    private HeartbeatLabel status = new HeartbeatLabel(string("msg-init"));

    private void initFrame() {
        new IconBorder(new ImageIcon(image("icons/ui/find.png"))).install(filterField);

        portField.getEditor().setOpaque(false);

        final String[] files = {"application/db16.png", "application/db32.png", "application/db64.png", "application/db256.png"};
        LinkedList<BufferedImage> icons = new LinkedList<>();
        for (String f : files) {
            icons.add(image("icons/" + f));
        }
        setIconImages(icons);

        sourceTable.getColumnModel().getColumn(0).setCellRenderer(sourceRenderer);
        sourceTable.getColumnModel().getColumn(1).setCellRenderer(sourceRenderer);
        ((DefaultTreeCellRenderer) scopeRenderer).setFont(scopeObjField.getFont());

        watchModel.init();

        updateInfoTableList(true);

        filterField.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                EventQueue.invokeLater(Client.this::applyInfoTableFilter);
            }
        });

        fixButtonLook();

        pack();
        if (!settings.applyWindowSettings("client", Client.this)) {
            setSize(800, 600);
            setLocationByPlatform(true);
        }

        EventQueue.invokeLater(() -> {
            int h = getHeight() - toolPanel.getPreferredSize().height; //sourceSplit.getHeight();
            int w = getWidth(); //sourceSplit.getWidth();
            sourceSplit.setDividerLocation(h / 2);
            scriptSplit.setDividerLocation(w / 4);
            watchSplit.setDividerLocation(w / 4 + w * 3 / 4 / 2);
            scopeTreeSplit.setDividerLocation(h / 4);
            stackSplit.setDividerLocation(w / 4);
        });

        installAccelerator("help", commandHelp);
        installAccelerator("raw", commandRaw);
        installAccelerator("stack-down", commandStackDown);
        installAccelerator("stack-up", commandStackUp);

        installToolTip("reconnect", backBtn);

        installToolTip("debug-view", debugBtn);
        installToolTip("table-view", infoBtn);

        installToolTip("pause", breakBtn);
        installToolTip("walk", walkBtn);
        installToolTip("run", runBtn);

        installToolTip("step-over", stepOverBtn);
        installToolTip("step-in", stepIntoBtn);
        installToolTip("step-out", stepOutBtn);

        installToolTip("break-func", breakEnterBtn);
        installToolTip("break-return", breakExitBtn);
        installToolTip("break-ex", breakThrowBtn);
        installToolTip("break-debug", breakDebuggerBtn);

        installToolTip("on-top", onTopBtn);
        installToolTip("force-exit", stopBtn);

        if (settings.getBoolean("on-top")) {
            onTopBtn.setSelected(true);
        }
    }

    private Action commandHelp = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            helpButton.openHelpPage();
        }
    };

    private Action commandRaw = new AbstractAction() {
        private RawCommandDialog d;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (d == null) {
                d = new RawCommandDialog(Client.this);
            }
            d.setVisible(true);
        }
    };

    private Action commandStackDown = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            stackList.setSelectedIndex(stackIndex(stackIndex(-1) + 1));
        }
    };

    private Action commandStackUp = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            stackList.setSelectedIndex(stackIndex(stackIndex(-1) - 1));
        }
    };

    /**
     * Set up the timer that calls {@link #heartbeat()} at regular intervals.
     */
    private void initTimer() {
        final Timer t = new Timer(HEARTBEAT_PERIOD, (ActionEvent e) -> {
            heartbeat();
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (!t.isRunning()) {
                    t.start();
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                t.stop();
            }
        });
    }

    private void installToolTip(String key, AbstractButton target) {
        final String name = string(key + "-name");
        final String desc = string(key + "-desc");
        final String keyDesc = string(key + "-keydesc");

        if (desc.isEmpty() && keyDesc.isEmpty()) {
            target.setToolTipText(name);
        } else {
            target.setToolTipText(
                    "<html><table border=0 width=200><tr><td><b>"
                    + name + "</b><td align=right>&nbsp;&nbsp;" + keyDesc + "</td><tr>"
                    + (!desc.isEmpty() ? "<td colspan=2><font size=-2>" + desc + "</td>" : "")
                    + "</table>"
            );
        }

        installAccelerator(key, target);
    }

    private void installAccelerator(String key, final Object target) {
        final String stroke = string(key + "-keystroke");
        if (stroke.isEmpty()) {
            return;
        }

        final KeyStroke k = PlatformSupport.getKeyStroke(stroke);
        if (k != null) {
            JComponent rootPane = getRootPane();

            Action a;

            if (target instanceof Action) {
                a = (Action) target;
            } else {
                a = new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ((AbstractButton) target).doClick();
                    }
                };
            }
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(k, key);
            rootPane.getActionMap().put(key, a);
        } else {
            System.err.println("DEBUG CLIENT: Warning: invalid key stroke property: " + key);
        }
    }

    private AbstractPixelwiseFilter disableFilter = new CompoundPixelwiseFilter(
            /*new BrightnessContrastFilter( -0.3, -0.2 ),*/new GreyscaleFilter()
    );

    private void fixButtonLook() {
        UIDefaults fixes = new UIDefaults();
        fixes.put("Button[Disabled].backgroundPainter", NO_OP_PAINTER);
        fixes.put("ToggleButton[Disabled].backgroundPainter", NO_OP_PAINTER);
        fixes.put("ToggleButton[Disabled+Selected].backgroundPainter", NO_OP_PAINTER);
        for (int i = 0; i < toolPanel.getComponentCount(); ++i) {
            if (!(toolPanel.getComponent(i) instanceof AbstractButton)) {
                continue;
            }
            AbstractButton b = (AbstractButton) toolPanel.getComponent(i);
            BufferedImage bi = ImageUtilities.iconToImage(b.getIcon());
            bi = disableFilter.filter(bi, null);
            b.setDisabledIcon(new ImageIcon(bi));
            b.putClientProperty("Nimbus.Overrides.InheritDefaults", Boolean.TRUE);
            b.putClientProperty("Nimbus.Overrides", fixes);
            b.updateUI();
        }
    }
    private final Painter NO_OP_PAINTER = (Graphics2D g, Object object, int width1, int height1) -> {
    };
    private final Border linkBorder = new MatteBorder(0, 2, 2, 2, Color.LIGHT_GRAY) {
        private final MatteBorder disabled = new MatteBorder(0, 2, 2, 2, new Color(72, 72, 72));

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (!c.isEnabled()) {
                disabled.paintBorder(c, g, x, y, width, height);
            } else {
                super.paintBorder(c, g, x, y, width, height);
            }
        }
    };

    private static BufferedImage image(String res) {
        URL url = Client.class.getResource("/resources/" + res);
        if (url != null) {
            try {
                return ImageIO.read(url);
            } catch (IOException e) {
            }
        }
        return null;
    }

    private static Icon icon(String res, int size) {
        BufferedImage img = image("icons/" + res);
        if (img == null) {
            return null;
        }
        if (size < 0) {
            size = img.getWidth();
        }
        return ImageUtilities.createIconForSize(img, size);
    }

    private static Icon icon(String res) {
        return icon(res, 14);
    }
    private final Icon debugIcon;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        watchPopup = new javax.swing.JPopupMenu();
        watchCopyItem = new javax.swing.JMenuItem();
        sourcePopup = new javax.swing.JLabel();
        toolPanel = new javax.swing.JPanel();
        backBtn = new javax.swing.JButton();
        runBtn = new javax.swing.JButton();
        breakBtn = new javax.swing.JButton();
        stopBtn = new javax.swing.JButton();
        stepIntoBtn = new javax.swing.JButton();
        stepOutBtn = new javax.swing.JButton();
        stepOverBtn = new javax.swing.JButton();
        onTopBtn = new javax.swing.JToggleButton();
        walkBtn = new javax.swing.JButton();
        breakEnterBtn = new javax.swing.JToggleButton();
        breakExitBtn = new javax.swing.JToggleButton();
        breakThrowBtn = new javax.swing.JToggleButton();
        infoBtn = new javax.swing.JToggleButton();
        breakDebuggerBtn = new javax.swing.JToggleButton();
        debugBtn = new javax.swing.JToggleButton();
        linkLabel = new javax.swing.JLabel();
        cardRoot = new javax.swing.JPanel();
        connectingPanel = new javax.swing.JPanel();
        javax.swing.JLabel connectLabel = new javax.swing.JLabel();
        javax.swing.JProgressBar connectBar = new javax.swing.JProgressBar();
        connectPanel = new javax.swing.JPanel();
        javax.swing.JLabel configLabel = new javax.swing.JLabel();
        javax.swing.JLabel portLabel = new javax.swing.JLabel();
        portField = new javax.swing.JSpinner();
        scanBtn = new javax.swing.JButton();
        connectBtn = new javax.swing.JButton();
        errorLabel = new javax.swing.JLabel();
        leftFill = new javax.swing.JLabel();
        rightFill = new javax.swing.JLabel();
        javax.swing.JLabel hostLabel = new javax.swing.JLabel();
        hostField = new javax.swing.JTextField();
        errorLabel2 = new javax.swing.JLabel();
        mainPanel = new javax.swing.JPanel();
        sourceSplit =  new ThinSplitPane() ;
        scriptSplit =  new ThinSplitPane() ;
        javax.swing.JPanel scriptPanel = new javax.swing.JPanel();
        javax.swing.JScrollPane scriptScroll = new javax.swing.JScrollPane();
        scriptList = new javax.swing.JList();
        scriptTitle = new javax.swing.JLabel();
        javax.swing.JPanel sourcePanel = new javax.swing.JPanel();
        sourceScroll = new javax.swing.JScrollPane();
        sourceTable =  new JHeadlessTable() ;
        sourceTitlePanel = new javax.swing.JPanel();
        sourceTitle = new javax.swing.JLabel();
        removeAllBreaksBtn = new ca.cgjennings.ui.JLinkLabel();
        watchSplit =  new ThinSplitPane() ;
        stackSplit =  new ThinSplitPane() ;
        javax.swing.JPanel stackPanel = new javax.swing.JPanel();
        javax.swing.JScrollPane stackScroll = new javax.swing.JScrollPane();
        stackList = new javax.swing.JList();
        stackTitle = new javax.swing.JLabel();
        javax.swing.JPanel watchPanel = new javax.swing.JPanel();
        watchTitle = new javax.swing.JLabel();
        watchScroll = new javax.swing.JScrollPane();
        watchTable = new javax.swing.JTable();
        javax.swing.JPanel scopePanel = new javax.swing.JPanel();
        scopeTitle = new javax.swing.JLabel();
        scopeTreeSplit =  new ThinSplitPane() ;
        scopeTreeScroll = new javax.swing.JScrollPane();
        scopeTree = new javax.swing.JTree();
        javax.swing.JPanel scopeValuePanel = new javax.swing.JPanel();
        scopeOutField = new javax.swing.JScrollPane();
        scopeObjField = new javax.swing.JTextArea();
        typePanel = new javax.swing.JPanel();
        javax.swing.JLabel typeHeadLabel = new javax.swing.JLabel();
        typeLabel = new javax.swing.JLabel();
        propHeadLabel = new javax.swing.JLabel();
        propertyField = new javax.swing.JTextField();
        infoPanel = new javax.swing.JPanel();
        infoTitle = new javax.swing.JLabel();
        infoTablePanel = new javax.swing.JPanel();
        infoControlPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        infoSelectCombo = new javax.swing.JComboBox();
        refreshBtn = new javax.swing.JButton();
        filterField = new ca.cgjennings.ui.JLabelledField();
        clearCaches = new javax.swing.JButton();
        infoScroll = new javax.swing.JScrollPane();
        infoTable = new javax.swing.JTable();
        statusPanel = new javax.swing.JPanel();
        helpButton = new ca.cgjennings.ui.JHelpButton();

        watchCopyItem.setText("Copy");
        watchCopyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                watchCopyItemActionPerformed(evt);
            }
        });
        watchPopup.add(watchCopyItem);

        sourcePopup.setBackground(new java.awt.Color(181, 205, 255));
        sourcePopup.setFont(sourcePopup.getFont().deriveFont(sourcePopup.getFont().getSize()-1f));
        sourcePopup.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 102, 255)), javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        sourcePopup.setOpaque(true);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(string( "app-title" )); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        toolPanel.setBackground(java.awt.Color.darkGray);
        toolPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.orange), javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.lightGray)));

        backBtn.setBackground(java.awt.Color.black);
        backBtn.setForeground(java.awt.Color.lightGray);
        backBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/ui/back-hi.png"))); // NOI18N
        backBtn.setBorderPainted(false);
        backBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        backBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backBtnActionPerformed(evt);
            }
        });

        runBtn.setBackground(java.awt.Color.black);
        runBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/continue.png"))); // NOI18N
        runBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        runBtn.setBorderPainted(false);
        runBtn.setFocusable(false);
        runBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        runBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runBtnActionPerformed(evt);
            }
        });

        breakBtn.setBackground(java.awt.Color.black);
        breakBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/pause.png"))); // NOI18N
        breakBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        breakBtn.setBorderPainted(false);
        breakBtn.setFocusable(false);
        breakBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        breakBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                breakBtnMousePressed(evt);
            }
        });
        breakBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                breakBtnActionPerformed(evt);
            }
        });

        stopBtn.setBackground(java.awt.Color.black);
        stopBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/terminate.png"))); // NOI18N
        stopBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        stopBtn.setBorderPainted(false);
        stopBtn.setFocusable(false);
        stopBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        stopBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopBtnActionPerformed(evt);
            }
        });

        stepIntoBtn.setBackground(java.awt.Color.black);
        stepIntoBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/stepin.png"))); // NOI18N
        stepIntoBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        stepIntoBtn.setBorderPainted(false);
        stepIntoBtn.setFocusable(false);
        stepIntoBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        stepIntoBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stepIntoBtnActionPerformed(evt);
            }
        });

        stepOutBtn.setBackground(java.awt.Color.black);
        stepOutBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/stepout.png"))); // NOI18N
        stepOutBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        stepOutBtn.setBorderPainted(false);
        stepOutBtn.setFocusable(false);
        stepOutBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        stepOutBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stepOutBtnActionPerformed(evt);
            }
        });

        stepOverBtn.setBackground(java.awt.Color.black);
        stepOverBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/stepover.png"))); // NOI18N
        stepOverBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        stepOverBtn.setBorderPainted(false);
        stepOverBtn.setFocusable(false);
        stepOverBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        stepOverBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stepOverBtnActionPerformed(evt);
            }
        });

        onTopBtn.setBackground(java.awt.Color.black);
        onTopBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/ui/keep-on-top.png"))); // NOI18N
        onTopBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        onTopBtn.setBorderPainted(false);
        onTopBtn.setFocusable(false);
        onTopBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        onTopBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onTopBtnActionPerformed(evt);
            }
        });

        walkBtn.setBackground(java.awt.Color.black);
        walkBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/walk.png"))); // NOI18N
        walkBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        walkBtn.setBorderPainted(false);
        walkBtn.setFocusable(false);
        walkBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        walkBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                walkBtnActionPerformed(evt);
            }
        });

        breakEnterBtn.setBackground(java.awt.Color.black);
        breakEnterBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/break-on-enter.png"))); // NOI18N
        breakEnterBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        breakEnterBtn.setBorderPainted(false);
        breakEnterBtn.setFocusable(false);
        breakEnterBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        breakEnterBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                breakEnterBtnActionPerformed(evt);
            }
        });

        breakExitBtn.setBackground(java.awt.Color.black);
        breakExitBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/break-on-exit.png"))); // NOI18N
        breakExitBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        breakExitBtn.setBorderPainted(false);
        breakExitBtn.setFocusable(false);
        breakExitBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        breakExitBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                breakExitBtnActionPerformed(evt);
            }
        });

        breakThrowBtn.setBackground(java.awt.Color.black);
        breakThrowBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/break-on-throw.png"))); // NOI18N
        breakThrowBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        breakThrowBtn.setBorderPainted(false);
        breakThrowBtn.setFocusable(false);
        breakThrowBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        breakThrowBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                breakThrowBtnActionPerformed(evt);
            }
        });

        infoBtn.setBackground(java.awt.Color.black);
        infoBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/ui/table.png"))); // NOI18N
        infoBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        infoBtn.setBorderPainted(false);
        infoBtn.setFocusable(false);
        infoBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        infoBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoBtnActionPerformed(evt);
            }
        });

        breakDebuggerBtn.setBackground(java.awt.Color.black);
        breakDebuggerBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/debugger/break-on-debugger.png"))); // NOI18N
        breakDebuggerBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        breakDebuggerBtn.setBorderPainted(false);
        breakDebuggerBtn.setFocusable(false);
        breakDebuggerBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        breakDebuggerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                breakDebuggerBtnActionPerformed(evt);
            }
        });

        debugBtn.setBackground(java.awt.Color.black);
        debugBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/ui/bug-report.png"))); // NOI18N
        debugBtn.setSelected(true);
        debugBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        debugBtn.setBorderPainted(false);
        debugBtn.setFocusable(false);
        debugBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        debugBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugBtnActionPerformed(evt);
            }
        });

        linkLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(0, 16, 0, 16),  linkBorder ));

        javax.swing.GroupLayout toolPanelLayout = new javax.swing.GroupLayout(toolPanel);
        toolPanel.setLayout(toolPanelLayout);
        toolPanelLayout.setHorizontalGroup(
            toolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(backBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(toolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(linkLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, toolPanelLayout.createSequentialGroup()
                        .addComponent(debugBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(infoBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(32, 32, 32)
                .addComponent(breakBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(walkBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(runBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(stepOverBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(stepIntoBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(stepOutBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(breakEnterBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(breakExitBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(breakThrowBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(breakDebuggerBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(stopBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(onTopBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        toolPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {backBtn, breakBtn, breakDebuggerBtn, breakEnterBtn, breakExitBtn, breakThrowBtn, debugBtn, infoBtn, onTopBtn, runBtn, stepIntoBtn, stepOutBtn, stepOverBtn, stopBtn, walkBtn});

        toolPanelLayout.setVerticalGroup(
            toolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(toolPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(toolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(debugBtn)
                    .addComponent(infoBtn)
                    .addComponent(breakBtn)
                    .addComponent(walkBtn)
                    .addComponent(runBtn)
                    .addComponent(stepOverBtn)
                    .addComponent(stepIntoBtn)
                    .addComponent(stepOutBtn)
                    .addComponent(breakEnterBtn)
                    .addComponent(breakExitBtn)
                    .addComponent(breakThrowBtn)
                    .addComponent(breakDebuggerBtn)
                    .addComponent(onTopBtn)
                    .addComponent(stopBtn)
                    .addComponent(backBtn))
                .addGap(1, 1, 1)
                .addComponent(linkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        toolPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {backBtn, breakBtn, breakDebuggerBtn, breakEnterBtn, breakExitBtn, breakThrowBtn, debugBtn, infoBtn, onTopBtn, runBtn, stepIntoBtn, stepOutBtn, stepOverBtn, stopBtn, walkBtn});

        getContentPane().add(toolPanel, java.awt.BorderLayout.NORTH);

        cardRoot.setLayout(new java.awt.CardLayout());

        connectingPanel.setBackground(java.awt.Color.white);
        connectingPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(0, 0, 0)));
        connectingPanel.setLayout(new java.awt.GridBagLayout());

        connectLabel.setFont(connectLabel.getFont().deriveFont(connectLabel.getFont().getStyle() | java.awt.Font.BOLD, connectLabel.getFont().getSize()+2));
        connectLabel.setIcon( debugIcon );
        connectLabel.setText(string( "msg-connecting" )); // NOI18N
        connectLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        connectLabel.setIconTextGap(16);
        connectLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        connectingPanel.add(connectLabel, gridBagConstraints);

        connectBar.setValue(100);
        connectBar.setIndeterminate(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        connectingPanel.add(connectBar, gridBagConstraints);

        cardRoot.add(connectingPanel, "connecting");

        connectPanel.setBackground(java.awt.Color.white);
        connectPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(0, 0, 0)));
        connectPanel.setLayout(new java.awt.GridBagLayout());

        configLabel.setFont(configLabel.getFont().deriveFont(configLabel.getFont().getStyle() | java.awt.Font.BOLD, configLabel.getFont().getSize()+2));
        configLabel.setIcon( debugIcon );
        configLabel.setText(string( "connect-title" )); // NOI18N
        configLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        configLabel.setIconTextGap(8);
        configLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 24, 4);
        connectPanel.add(configLabel, gridBagConstraints);

        portLabel.setDisplayedMnemonic('p');
        portLabel.setLabelFor(portField);
        portLabel.setText(string( "connect-port" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        connectPanel.add(portLabel, gridBagConstraints);

        portField.setModel(new javax.swing.SpinnerNumberModel(8888, 80, 65535, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 4, 4);
        connectPanel.add(portField, gridBagConstraints);

        scanBtn.setText(string("connect-scan")); // NOI18N
        scanBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scanBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        connectPanel.add(scanBtn, gridBagConstraints);

        connectBtn.setMnemonic('c');
        connectBtn.setText(string( "connect-btn" )); // NOI18N
        connectBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        connectPanel.add(connectBtn, gridBagConstraints);

        errorLabel.setFont(errorLabel.getFont().deriveFont(errorLabel.getFont().getSize()-1f));
        errorLabel.setForeground(java.awt.Color.red);
        errorLabel.setText(string( "connect-error" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 0, 4);
        connectPanel.add(errorLabel, gridBagConstraints);

        leftFill.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        connectPanel.add(leftFill, gridBagConstraints);

        rightFill.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        connectPanel.add(rightFill, gridBagConstraints);

        hostLabel.setDisplayedMnemonic('h');
        hostLabel.setLabelFor(hostField);
        hostLabel.setText(string( "connect-host" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        connectPanel.add(hostLabel, gridBagConstraints);

        hostField.setColumns(20);
        hostField.setText("localhost");
        hostField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 4, 4);
        connectPanel.add(hostField, gridBagConstraints);

        errorLabel2.setFont(errorLabel2.getFont().deriveFont(errorLabel2.getFont().getSize()-1f));
        errorLabel2.setForeground(java.awt.Color.red);
        errorLabel2.setText(string( "connect-error2" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        connectPanel.add(errorLabel2, gridBagConstraints);

        cardRoot.add(connectPanel, "connect");

        mainPanel.setLayout(new java.awt.BorderLayout());

        sourceSplit.setBorder(null);
        sourceSplit.setDividerLocation(300);
        sourceSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        sourceSplit.setResizeWeight(0.5);

        scriptSplit.setDividerLocation(200);

        scriptPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        scriptPanel.setLayout(new java.awt.BorderLayout());

        scriptScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scriptScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scriptList.setFont(scriptList.getFont().deriveFont(scriptList.getFont().getSize()-1f));
        scriptList.setModel( fileModel );
        scriptList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        scriptList.setCellRenderer( fileListRenderer );
        scriptList.setVisibleRowCount(12);
        scriptList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                scriptListValueChanged(evt);
            }
        });
        scriptScroll.setViewportView(scriptList);

        scriptPanel.add(scriptScroll, java.awt.BorderLayout.CENTER);

        scriptTitle.setBackground(java.awt.Color.black);
        scriptTitle.setFont(scriptTitle.getFont().deriveFont(scriptTitle.getFont().getStyle() | java.awt.Font.BOLD));
        scriptTitle.setForeground(java.awt.Color.white);
        scriptTitle.setIcon( icon("project/folder.png") );
        scriptTitle.setText(string( "pane-script-list" )); // NOI18N
        scriptTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 2, 4));
        scriptTitle.setOpaque(true);
        scriptPanel.add(scriptTitle, java.awt.BorderLayout.PAGE_START);

        scriptSplit.setLeftComponent(scriptPanel);

        sourcePanel.setMinimumSize(new java.awt.Dimension(0, 0));
        sourcePanel.setLayout(new java.awt.BorderLayout());

        sourceScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        sourceTable.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        sourceTable.setModel( source );
        sourceTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        sourceTable.setFillsViewportHeight(true);
        sourceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                sourceTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sourceTableMouseExited(evt);
            }
        });
        sourceTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                sourceTableMouseMoved(evt);
            }
        });
        sourceScroll.setViewportView(sourceTable);

        sourcePanel.add(sourceScroll, java.awt.BorderLayout.CENTER);

        sourceTitlePanel.setLayout(new java.awt.BorderLayout());

        sourceTitle.setBackground(java.awt.Color.black);
        sourceTitle.setFont(sourceTitle.getFont().deriveFont(sourceTitle.getFont().getStyle() | java.awt.Font.BOLD));
        sourceTitle.setForeground(java.awt.Color.white);
        sourceTitle.setIcon( icon("project/script.png") );
        sourceTitle.setText(string( "pane-source-code" )); // NOI18N
        sourceTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
        sourceTitle.setOpaque(true);
        sourceTitlePanel.add(sourceTitle, java.awt.BorderLayout.CENTER);

        removeAllBreaksBtn.setBackground(java.awt.Color.black);
        removeAllBreaksBtn.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 1, new java.awt.Color(99, 99, 99)), javax.swing.BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        removeAllBreaksBtn.setForeground(new java.awt.Color(57, 120, 171));
        removeAllBreaksBtn.setText("Remove All Breakpoints\n");
        removeAllBreaksBtn.setFont(removeAllBreaksBtn.getFont().deriveFont(removeAllBreaksBtn.getFont().getStyle() | java.awt.Font.BOLD, removeAllBreaksBtn.getFont().getSize()-1));
        removeAllBreaksBtn.setOpaque(true);
        removeAllBreaksBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                removeAllBreaksBtnMousePressed(evt);
            }
        });
        sourceTitlePanel.add(removeAllBreaksBtn, java.awt.BorderLayout.EAST);

        sourcePanel.add(sourceTitlePanel, java.awt.BorderLayout.NORTH);

        scriptSplit.setRightComponent(sourcePanel);

        sourceSplit.setLeftComponent(scriptSplit);

        watchSplit.setDividerLocation(450);
        watchSplit.setResizeWeight(0.5);

        stackSplit.setDividerLocation(200);

        stackPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        stackPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        stackPanel.setLayout(new java.awt.BorderLayout());

        stackScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        stackScroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        stackList.setFont(stackList.getFont().deriveFont(stackList.getFont().getSize()-1f));
        stackList.setModel( stackModel );
        stackList.setVisibleRowCount(12);
        stackList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                stackListMouseClicked(evt);
            }
        });
        stackList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                stackValueChanged(evt);
            }
        });
        stackScroll.setViewportView(stackList);

        stackPanel.add(stackScroll, java.awt.BorderLayout.CENTER);

        stackTitle.setBackground(java.awt.Color.black);
        stackTitle.setFont(stackTitle.getFont().deriveFont(stackTitle.getFont().getStyle() | java.awt.Font.BOLD));
        stackTitle.setForeground(java.awt.Color.white);
        stackTitle.setIcon( icon("project/copies.png") );
        stackTitle.setText(string( "pane-call-stack" )); // NOI18N
        stackTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
        stackTitle.setOpaque(true);
        stackPanel.add(stackTitle, java.awt.BorderLayout.PAGE_START);

        stackSplit.setLeftComponent(stackPanel);

        watchPanel.setBackground(java.awt.Color.white);
        watchPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        watchPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        watchPanel.setLayout(new java.awt.BorderLayout());

        watchTitle.setBackground(java.awt.Color.black);
        watchTitle.setFont(watchTitle.getFont().deriveFont(watchTitle.getFont().getStyle() | java.awt.Font.BOLD));
        watchTitle.setForeground(java.awt.Color.white);
        watchTitle.setIcon( icon("project/table.png") );
        watchTitle.setText(string( "pane-watches" )); // NOI18N
        watchTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
        watchTitle.setOpaque(true);
        watchPanel.add(watchTitle, java.awt.BorderLayout.PAGE_START);

        watchScroll.setBackground(java.awt.Color.white);
        watchScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        watchTable.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        watchTable.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        watchTable.setModel( watchModel );
        watchTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        watchTable.setComponentPopupMenu(watchPopup);
        watchTable.setDragEnabled(true);
        watchTable.setDropMode(javax.swing.DropMode.ON);
        watchScroll.setViewportView(watchTable);

        watchPanel.add(watchScroll, java.awt.BorderLayout.CENTER);

        stackSplit.setRightComponent(watchPanel);

        watchSplit.setLeftComponent(stackSplit);

        scopePanel.setMinimumSize(new java.awt.Dimension(0, 0));
        scopePanel.setLayout(new java.awt.BorderLayout());

        scopeTitle.setBackground(java.awt.Color.black);
        scopeTitle.setFont(scopeTitle.getFont().deriveFont(scopeTitle.getFont().getStyle() | java.awt.Font.BOLD));
        scopeTitle.setForeground(java.awt.Color.white);
        scopeTitle.setIcon( icon("toolbar/al-left.png") );
        scopeTitle.setText(string( "pane-scope-tree" )); // NOI18N
        scopeTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
        scopeTitle.setOpaque(true);
        scopePanel.add(scopeTitle, java.awt.BorderLayout.PAGE_START);

        scopeTreeSplit.setDividerLocation(100);
        scopeTreeSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        scopeTreeSplit.setResizeWeight(0.5);

        scopeTreeScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scopeTreeScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        ToolTipManager.sharedInstance().registerComponent( scopeTree );
        scopeTree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
        scopeTree.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scopeTree.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("<root>");
        scopeTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        scopeTree.setCellRenderer( scopeRenderer );
        scopeTree.setEnabled(false);
        scopeTree.setRootVisible(false);
        scopeTree.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {
            }
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {
                scopeTreeTreeWillExpand(evt);
            }
        });
        scopeTree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            public void treeCollapsed(javax.swing.event.TreeExpansionEvent evt) {
                scopeTreeTreeCollapsed(evt);
            }
            public void treeExpanded(javax.swing.event.TreeExpansionEvent evt) {
                scopeTreeTreeExpanded(evt);
            }
        });
        scopeTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                scopeTreeValueChanged(evt);
            }
        });
        scopeTreeScroll.setViewportView(scopeTree);

        scopeTreeSplit.setTopComponent(scopeTreeScroll);

        scopeValuePanel.setLayout(new java.awt.BorderLayout());

        scopeOutField.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scopeOutField.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scopeObjField.setColumns(20);
        scopeObjField.setEditable(false);
        scopeObjField.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        scopeObjField.setTabSize(4);
        scopeOutField.setViewportView(scopeObjField);

        scopeValuePanel.add(scopeOutField, java.awt.BorderLayout.CENTER);

        typePanel.setBackground(java.awt.Color.white);
        typePanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        typeHeadLabel.setFont(typeHeadLabel.getFont().deriveFont(typeHeadLabel.getFont().getStyle() | java.awt.Font.BOLD, typeHeadLabel.getFont().getSize()-1));
        typeHeadLabel.setText(string( "pane-scope-prop-type" )); // NOI18N

        typeLabel.setFont(typeLabel.getFont().deriveFont(typeLabel.getFont().getSize()-1f));
        typeLabel.setText(" ");

        propHeadLabel.setFont(propHeadLabel.getFont().deriveFont(propHeadLabel.getFont().getStyle() | java.awt.Font.BOLD, propHeadLabel.getFont().getSize()-1));
        propHeadLabel.setText(string( "pane-scope-prop-name" )); // NOI18N

        propertyField.setFont(propertyField.getFont().deriveFont(propertyField.getFont().getSize()-1f));
        propertyField.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        propertyField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                propertyFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout typePanelLayout = new javax.swing.GroupLayout(typePanel);
        typePanel.setLayout(typePanelLayout);
        typePanelLayout.setHorizontalGroup(
            typePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(typePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(typePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(propHeadLabel)
                    .addComponent(typeHeadLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(typePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(typeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE)
                    .addComponent(propertyField, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE))
                .addContainerGap())
        );
        typePanelLayout.setVerticalGroup(
            typePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(typePanelLayout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addGroup(typePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(propHeadLabel)
                    .addComponent(propertyField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(1, 1, 1)
                .addGroup(typePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeHeadLabel)
                    .addComponent(typeLabel))
                .addContainerGap())
        );

        scopeValuePanel.add(typePanel, java.awt.BorderLayout.NORTH);

        scopeTreeSplit.setBottomComponent(scopeValuePanel);

        scopePanel.add(scopeTreeSplit, java.awt.BorderLayout.CENTER);

        watchSplit.setRightComponent(scopePanel);

        sourceSplit.setRightComponent(watchSplit);

        mainPanel.add(sourceSplit, java.awt.BorderLayout.CENTER);

        cardRoot.add(mainPanel, "main");

        infoPanel.setLayout(new java.awt.BorderLayout());

        infoTitle.setBackground(java.awt.Color.black);
        infoTitle.setFont(infoTitle.getFont().deriveFont(infoTitle.getFont().getStyle() | java.awt.Font.BOLD));
        infoTitle.setForeground(java.awt.Color.white);
        infoTitle.setIcon( icon("toolbar/al-left.png") );
        infoTitle.setText(string( "tab-title" )); // NOI18N
        infoTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
        infoTitle.setOpaque(true);
        infoPanel.add(infoTitle, java.awt.BorderLayout.PAGE_START);

        infoTablePanel.setLayout(new java.awt.BorderLayout());

        infoControlPanel.setBackground(java.awt.Color.white);
        infoControlPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.lightGray));

        jLabel3.setDisplayedMnemonic('t');
        jLabel3.setLabelFor(infoSelectCombo);
        jLabel3.setText(string( "tab-table" )); // NOI18N

        infoSelectCombo.setMaximumRowCount(20);
        infoSelectCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoSelectComboActionPerformed(evt);
            }
        });

        refreshBtn.setText(string( "tab-refresh" )); // NOI18N
        refreshBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoSelectComboActionPerformed(evt);
            }
        });

        filterField.setColumns(30);
        filterField.setLabel(string( "tab-filter" )); // NOI18N

        clearCaches.setFont(clearCaches.getFont().deriveFont(clearCaches.getFont().getSize()-1f));
        clearCaches.setText(string("tab-clear-caches")); // NOI18N
        clearCaches.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearCachesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout infoControlPanelLayout = new javax.swing.GroupLayout(infoControlPanel);
        infoControlPanel.setLayout(infoControlPanelLayout);
        infoControlPanelLayout.setHorizontalGroup(
            infoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(infoSelectCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(refreshBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 317, Short.MAX_VALUE)
                .addComponent(clearCaches)
                .addGap(18, 18, 18)
                .addComponent(filterField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        infoControlPanelLayout.setVerticalGroup(
            infoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoControlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(infoControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel3)
                    .addComponent(infoSelectCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(refreshBtn)
                    .addComponent(filterField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearCaches))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        infoTablePanel.add(infoControlPanel, java.awt.BorderLayout.PAGE_START);

        infoScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        infoTable.setAutoCreateRowSorter(true);
        infoTable.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        infoTable.setFont(infoTable.getFont().deriveFont(infoTable.getFont().getSize()-1f));
        infoTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        infoTable.setFillsViewportHeight(true);
        infoScroll.setViewportView(infoTable);

        infoTablePanel.add(infoScroll, java.awt.BorderLayout.CENTER);

        infoPanel.add(infoTablePanel, java.awt.BorderLayout.CENTER);

        cardRoot.add(infoPanel, "info");

        getContentPane().add(cardRoot, java.awt.BorderLayout.CENTER);

        statusPanel.setBackground(java.awt.Color.darkGray);
        statusPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(0, 0, 0)), javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        statusPanel.setLayout(new java.awt.BorderLayout());

        helpButton.setHelpPage("dm-debugger");
        statusPanel.add(helpButton, java.awt.BorderLayout.WEST);

        getContentPane().add(statusPanel, java.awt.BorderLayout.PAGE_END);
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && !connected) {
            showPanel("connecting");
            EventQueue.invokeLater(this::checkConnection);
        }
    }

    private void updateToolState() {
        if (walkBtn.getModel().isArmed() || walkBtn.getModel().isPressed()) {
            isWalking = false;
        }

        boolean en = connected;
        for (int i = 0; i < toolPanel.getComponentCount(); ++i) {
            Component c = toolPanel.getComponent(i);
            if (c == onTopBtn) {
                continue;
            }
            c.setEnabled(en);
        }

        if (!en) {
            return;
        }

        breakBtn.setEnabled(!interrupted);
        walkBtn.setEnabled(interrupted);
        runBtn.setEnabled(interrupted);

        stepOverBtn.setEnabled(interrupted);
        stepIntoBtn.setEnabled(interrupted);
        stepOutBtn.setEnabled(interrupted);

        scopeTree.setEnabled(interrupted);
    }

    /**
     * Ensures that the entries in {@code model} match the list of strings. If
     * possible, the selection is maintained. Returns {@code true} if the model
     * had to be changed.
     *
     * @param values the list of strings that the model must match
     * @param model a model to synchronize with the list
     * @return {@code true} if the model was updated
     */
    private boolean updateModel(Object[] values, JList list, DefaultListModel model) {
        boolean update = false;
        if (model.size() != values.length) {
            update = true;
        } else {
            for (int i = 0; i < values.length; ++i) {
                if (!values[i].equals(model.get(i))) {
                    update = true;
                    break;
                }
            }
        }
        if (!update) {
            return false;
        }

        Object sel = list.getSelectedValue();
        int selIndex = -1;
        model.removeAllElements();
        for (int i = 0; i < values.length; ++i) {
            model.addElement(values[i]);
            if (values[i].equals(sel)) {
                selIndex = i;
            }
        }
        list.setSelectedIndex(selIndex);
        return true;
    }

    private void synchAll() {
        waitCursor(true);
        try {
            synchFiles();
            synchSource();
            synchStack();
            synchWatches();
            synchScopeTree();

            String[] states = performQuietly(Command.BREAKSTATUS);
            if (states != null) {
                final JToggleButton[] breakOns = {breakEnterBtn, breakExitBtn, breakThrowBtn, breakDebuggerBtn};
                for (int i = 0; i < breakOns.length; ++i) {
                    breakOns[i].setSelected(states[i] != null && states[i].equals("1"));
                }
            }
            showPanel("main");
        } finally {
            waitCursor(false);
        }
    }

    private void synchFiles() {
        String[] files = perform(Command.FILELIST);
        if (files == null) {
            return;
        }

        Arrays.sort(files);
        updateModel(files, scriptList, fileModel);
    }
    private DefaultListModel fileModel = new DefaultListModel();

    private void synchSource() {
        source.synch();
    }

    private void synchStack() {
        String[] stack = perform(Command.CALLSTACK);
        if (stack == null) {
            return;
        }

        StackFrame[] frames = new StackFrame[stack.length / 2];
        for (int i = 0, j = 0; i < stack.length; i += 2) {
            frames[j++] = new StackFrame(stack[i], Integer.parseInt(stack[i + 1]));
        }

        updateModel(frames, stackList, stackModel);
    }
    private DefaultListModel stackModel = new DefaultListModel();

    private void synchWatches() {
        watchModel.updateValues(-1);
    }

    private Timer reconnectTimer = new Timer(7_500, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            FocusManager fm = FocusManager.getCurrentManager();
            Component c = fm.getFocusOwner();
            if (c == hostField || c == portField || c == connectBtn) {
                return;
            }
            ((Timer) e.getSource()).stop();
            connectBtnActionPerformed(e);
        }
    });

    private void showPanel(String name) {
        if (name.equals("main") && infoBtn.isSelected()) {
            name = "info";
            updateInfoTableList(false);

        }
        if (EventQueue.isDispatchThread()) {
            ((CardLayout) cardRoot.getLayout()).show(cardRoot, name);
            panel = name;

            if (name.equals("connect") && autoConnect) {
                reconnectTimer.start();
            }
        } else {
            final String fName = name;
            EventQueue.invokeLater(() -> {
                showPanel(fName);
            });
        }
    }

    private String getPanel() {
        threadAssert();
        return panel;
    }

    private String panel = "connecting";

	private void connectBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectBtnActionPerformed
            host = hostField.getText().trim();
            port = ((Number) portField.getValue()).intValue();
            if (host.isEmpty()) {
                host = "localhost";
            }
            connected = false;
            autoConnect = true;
            checkConnection();
	}//GEN-LAST:event_connectBtnActionPerformed

    private void reachedBreakpoint() {
        synchStack();
        synchWatches();
        synchScopeTree();

        if (stackModel.size() > 0) {
            StackFrame top = (StackFrame) stackModel.get(0);
            showFile(top.file, top.line - 1);
        }

        if (isWalking) {
            // causes too much UI activity at high HEARTBEAT_PERIOD
            //setInterrupted( false );
            performQuietly(Command.STEPINTO);
        }
    }

    private String interruptedThread;
    private String interruptingException;

    private void heartbeat() {
        if (connected) {
            if (maybeReachedBreakpoint) {
                String[] reply = perform(Command.INTERRUPTED);
                if (reply == null) {
                    checkConnection();
                    return;
                }
                if (reply[0].equals("1")) {
                    interruptedThread = Command.unescapeProtocolText(reply[1]);
                    interruptingException = reply.length == 2 || reply[2].isEmpty() ? null : Command.unescapeProtocolText(reply[2]);
                    setInterrupted(true);
                    reachedBreakpoint();
                } else {
                    setInterrupted(false);
                }
                maybeReachedBreakpoint = false;
                checkConnection();
            } else {
                // this will probe the server and coincidentally check if a
                // breakpoint has been reached
                checkConnection();

                // check for new files from time to time
                if (heartbeatSyncCounter++ >= 15 * (1_000 / HEARTBEAT_PERIOD)) {
                    heartbeatSyncCounter = 0;
                    synchFiles();
                }

                // double-check if the script is interrupted from time-to-time
                if (!maybeReachedBreakpoint && !interrupted && (heartbeatSyncCounter & 15) == 15) {
                    String[] reply = performQuietly(Command.INTERRUPTED);
                    if (reply != null && reply[0].equals("1")) {
                        maybeReachedBreakpoint = true;
                    }
                }
            }
        }
        updateStatus();
    }

    private String host = InetAddress.getLoopbackAddress().getHostName();
    private int port = 8888;

    // Counts heartbeats; when this reaches a predefined limit,
    // some periodic synchronization will be performed.
    private int heartbeatSyncCounter = 0;
    // The rate at which the client will check for breakpoint hits
    private static final int HEARTBEAT_PERIOD = 100;
    private boolean isWalking = false;
    private volatile boolean maybeReachedBreakpoint = true;
    private volatile boolean interrupted = false;
    private volatile boolean connected = false;
    private volatile boolean autoConnect = true;
    // This is a cookie that is compared with the value returned from the
    // PROBE command. When the cookie does not match, it indicates that a
    // new breakpoint has been triggered. send() automatically checks if
    // the command it is sending is a PROBE, and if so it will compare the
    // cookies and set the maybeReachedInterrupt flag if they don't match.
    private volatile int lastFrameUpdate = -1;

    private void setInterrupted(boolean i) {
        if (i != interrupted) {
            updateToolState();
            interrupted = i;

            // just got interrupted, try to steal focus
            // (on Windows, taskbar icon will blink)
            if (i) {
                if (!isVisible()) {
                    setVisible(true);
                }
                if ((getExtendedState() & ICONIFIED) != 0) {
                    setExtendedState(getExtendedState() & ~ICONIFIED);
                }
                requestFocusInWindow();
                if (!isActive()) {
                    toFront();
                }
            } // otherwise, repaint the source table to change the green top
            // of stack outline and clear the stack frame
            else {
                sourceTable.repaint();
                ((DefaultListModel) stackList.getModel()).clear();
            }
        }
    }

	private void runBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runBtnActionPerformed
            checkConnection();
            isWalking = false;
            setInterrupted(false);
            perform(Command.CONTINUE);
	}//GEN-LAST:event_runBtnActionPerformed

	private void breakBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_breakBtnActionPerformed
            isWalking = false;
            if (perform(Command.BREAK) == null) {
                checkConnection();
            }
	}//GEN-LAST:event_breakBtnActionPerformed

	private void stopBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopBtnActionPerformed
            if (JOptionPane.showConfirmDialog(this, new String[]{
                "<html><b>" + string("force-exit-verify-1"),
                string("force-exit-verify-2")
            },
                    string("force-exit-name"),
                    JOptionPane.YES_NO_OPTION,
                    0, debugIcon) != JOptionPane.YES_OPTION) {
                return;
            }

            waitCursor(true);
            try {
                perform(Command.STOP);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                connected = false;
                updateToolState();
                showPanel("connect");
            } finally {
                waitCursor(false);
            }
	}//GEN-LAST:event_stopBtnActionPerformed

	private void stepIntoBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepIntoBtnActionPerformed
            checkConnection();
            if (!connected) {
                return;
            }
            setInterrupted(false);
            isWalking = false;
            perform(Command.STEPINTO);
	}//GEN-LAST:event_stepIntoBtnActionPerformed

	private void stepOutBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepOutBtnActionPerformed
            checkConnection();
            if (!connected) {
                return;
            }
            setInterrupted(false);
            isWalking = false;
            perform(Command.STEPOUT);
	}//GEN-LAST:event_stepOutBtnActionPerformed

	private void stepOverBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepOverBtnActionPerformed
            checkConnection();
            if (!connected) {
                return;
            }
            setInterrupted(false);
            isWalking = false;
            perform(Command.STEPOVER);
	}//GEN-LAST:event_stepOverBtnActionPerformed

	private void scriptListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_scriptListValueChanged
            String url = (String) scriptList.getSelectedValue();
            showFile(url, -1);
	}//GEN-LAST:event_scriptListValueChanged

	private void sourceTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sourceTableMouseMoved
            Point p = evt.getPoint();
            int col = sourceTable.columnAtPoint(p);
            Cursor c = Cursor.getDefaultCursor();
            if (col == 0) {
                int row = sourceTable.rowAtPoint(p);
                if (row >= 0 && source.lines[row].breakable) {
                    c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                }
            } else {
                SourceModel model = (SourceModel) sourceTable.getModel();
                String id = model.identifierUnderPoint(sourceTable, p);

                if (id != null) {
                    if (!id.equals(popupExpression)) {
                        // hide current pop-up
                        sourceTableMouseExited(null);
                        sourcePopup.setText(createSourcePopupText(id));
                        sourcePopup.setSize(sourcePopup.getPreferredSize());
                        Point sp = evt.getLocationOnScreen();
                        popup = PopupFactory.getSharedInstance().getPopup(sourceTable, sourcePopup, sp.x + 24, sp.y + 16);
                        popup.show();
                    }
                } else {
                    sourceTableMouseExited(null);
                }
                popupExpression = id;
            }
            sourceTable.setCursor(c);
	}//GEN-LAST:event_sourceTableMouseMoved

    private String createSourcePopupText(String id) {
        StringBuilder b = new StringBuilder("<html><b>")
                .append(id).append("</b>:");

        if (interrupted) {
            String[] result = performQuietly(Command.EVAL, String.valueOf(stackIndex(-1)), "''+" + id + ';');
            if (result == null) {
                b.append("<font color='red'>&nbsp;&nbsp;<tt>").append(escapeHTML(string("watch-cant-eval")));
            } else if (result.length == 1 && result[0].startsWith("ReferenceError: ")) {
                b.append("<font color='gray'>&nbsp;&nbsp;<tt>").append(escapeHTML(string("watch-no-scope")));
            } else if (result.length == 0 || (result.length == 1 && result[0].isEmpty())) {
                b.append("<font color='gray'>&nbsp;&nbsp;<tt>").append(escapeHTML(string("watch-empty")));
            } else if (result.length == 1 && (id.length() + result[0].length() <= 40)) {
                b.append("&nbsp;&nbsp;<tt>").append(escapeHTML(result[0]));
            } else {
                b.append("<pre>");
                result = wrapPopupExpression(result);
                for (int i = 0; i < result.length; ++i) {
                    if (i > 0) {
                        b.append('\n');
                    }
                    b.append(escapeHTML(result[i]));
                    if (i == MAX_POPUP_EXPRESSION_LINES - 1 && result.length > MAX_POPUP_EXPRESSION_LINES) {
                        b.append("\n...");
                        break;
                    }
                }
            }
        } else {
            b.append("<font color='gray'>&nbsp;&nbsp;<tt>").append(escapeHTML(string("watch-not-interrupted")));
        }
        return b.toString();
    }

    private static String[] wrapPopupExpression(String[] lines) {
        int longLines = 0;
        for (int i = 0; i < lines.length; ++i) {
            if (lines[i].length() > MAX_POPUP_EXPRESSION_COLS) {
                longLines++;
            }
        }

        if (longLines == 0) {
            return lines;
        }

        ArrayList<String> wrapped = new ArrayList<>(MAX_POPUP_EXPRESSION_LINES + 1);
        for (int i = 0; i <= MAX_POPUP_EXPRESSION_LINES && i < lines.length && wrapped.size() <= MAX_POPUP_EXPRESSION_LINES; ++i) {
            String li = lines[i];
            while (li.length() >= MAX_POPUP_EXPRESSION_COLS) {
                wrapped.add(li.substring(0, MAX_POPUP_EXPRESSION_COLS));
                li = li.substring(MAX_POPUP_EXPRESSION_COLS);
            }
            wrapped.add(li);
        }

        return wrapped.toArray(new String[0]);
    }

    private static final int MAX_POPUP_EXPRESSION_LINES = 20;
    private static final int MAX_POPUP_EXPRESSION_COLS = 80;

	private void sourceTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sourceTableMouseClicked
            if (evt.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            Point p = evt.getPoint();
            int col = sourceTable.columnAtPoint(p);
            if (col == 0) {
                int row = sourceTable.rowAtPoint(p);
                if (row >= 0 && source.lines[row].breakable) {
                    String[] reply = perform(Command.TOGGLEBREAK, source.file, String.valueOf(row));
                    if (reply == null) {
                        checkConnection();
                    } else {
                        synchSource();
                    }
                }
            }
	}//GEN-LAST:event_sourceTableMouseClicked

	private void stackValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_stackValueChanged
            synchWatches();
            synchScopeTree();
            if (stackModel.isEmpty()) {
                return;
            }
            StackFrame frame = (StackFrame) stackList.getSelectedValue();
            if (frame == null) {
                frame = (StackFrame) stackModel.get(0);
            }
            showFile(frame.file, frame.line);
	}//GEN-LAST:event_stackValueChanged

	private void removeAllBreaksBtnMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_removeAllBreaksBtnMousePressed
            String file = source.file;
            if (perform(Command.CLEARBREAKPOINTS, file) != null) {
                synchSource();
            } else {
                checkConnection();
            }
}//GEN-LAST:event_removeAllBreaksBtnMousePressed

	private void onTopBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onTopBtnActionPerformed
            setAlwaysOnTop(onTopBtn.isSelected());
	}//GEN-LAST:event_onTopBtnActionPerformed

	private void walkBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_walkBtnActionPerformed
            if (!isWalking) {
                setInterrupted(false);
                perform(Command.BREAK);
                maybeReachedBreakpoint = true;
            }
            isWalking = !isWalking;
	}//GEN-LAST:event_walkBtnActionPerformed

	private void infoBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoBtnActionPerformed
            debugBtn.setSelected(!infoBtn.isSelected());

            showPanel("main");
            checkConnection();
	}//GEN-LAST:event_infoBtnActionPerformed

	private void breakEnterBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_breakEnterBtnActionPerformed
            checkConnection();
            if (!connected) {
                return;
            }
            isWalking = false;
            perform(Command.BREAKONENTER, breakEnterBtn.isSelected() ? "1" : "0");
	}//GEN-LAST:event_breakEnterBtnActionPerformed

	private void breakExitBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_breakExitBtnActionPerformed
            checkConnection();
            if (!connected) {
                return;
            }
            isWalking = false;
            perform(Command.BREAKONEXIT, breakExitBtn.isSelected() ? "1" : "0");
	}//GEN-LAST:event_breakExitBtnActionPerformed

	private void breakThrowBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_breakThrowBtnActionPerformed
            checkConnection();
            if (!connected) {
                return;
            }
            isWalking = false;
            perform(Command.BREAKONTHROW, breakThrowBtn.isSelected() ? "1" : "0");
	}//GEN-LAST:event_breakThrowBtnActionPerformed

	private void breakBtnMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_breakBtnMousePressed
            if (evt.getButton() == MouseEvent.BUTTON1) {
                // when update rate is very fast, it becomes difficult to hit this button
                // because it may be en/disabled rapidly and this interrupts the press
                isWalking = false;
            }
	}//GEN-LAST:event_breakBtnMousePressed

	private void infoSelectComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoSelectComboActionPerformed
            if (infoSelectCombo.getSelectedItem() == null) {
                return;
            }
            String name = infoSelectCombo.getSelectedItem().toString();

            if (isVisible()) {
                String[] data = perform(Command.INFOTABLE, name);
                if (data == null) {
                    checkConnection();
                    return;
                }
                InfoTable table = new InfoTable(data);
                table.install(infoTable);
                infoTable.setRowSorter(new TableRowSorter<>(infoTable.getModel()));
                applyInfoTableFilter();
            }
	}//GEN-LAST:event_infoSelectComboActionPerformed

    @SuppressWarnings("unchecked")
    private void applyInfoTableFilter() {
        RowFilter f = null;
        String criteria = filterField.getText().trim();
        if (!criteria.isEmpty()) {
            f = RowFilter.regexFilter("(?i)(?u)" + Pattern.quote(criteria));
        }
        ((TableRowSorter<TableModel>) infoTable.getRowSorter()).setRowFilter(f);
    }

	private void breakDebuggerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_breakDebuggerBtnActionPerformed
            checkConnection();
            if (!connected) {
                return;
            }
            isWalking = false;
            perform(Command.BREAKONDEBUGGER, breakExitBtn.isSelected() ? "1" : "0");
	}//GEN-LAST:event_breakDebuggerBtnActionPerformed

	private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
            if (connected && interrupted) {
                Object[] message = {
                    "<html><b>" + string("app-verify-exit"),
                    string("app-verify-exit2"),
                    string("app-verify-exit3")
                };
//			Object[] options = {
//
//			};
                int v = JOptionPane.showOptionDialog(this, message, getTitle(),
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                        null, null, JOptionPane.NO_OPTION
                );
                if (v != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            //		if( connected ) {
//			setVisible( false );
//			return;
//		}
//		if( performQuietly( Command.PROBE ) != null ) {
//			System.exit( 0 );
//		}
            settings.set("on-top", onTopBtn.isSelected() ? "yes" : "no");
            settings.storeWindowSettings("client", this);
            watchModel.updateSettings();

            try {
                writeSettings();
            } catch (IOException e) {
                System.err.println("DEBUG CLIENT: failed to store user settings");
            }
            System.exit(0);
	}//GEN-LAST:event_formWindowClosing

	private void watchCopyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_watchCopyItemActionPerformed
            Clipboard clip = getToolkit().getSystemClipboard();
            watchTable.getTransferHandler().exportToClipboard(watchTable, clip, TransferHandler.COPY);
	}//GEN-LAST:event_watchCopyItemActionPerformed

	private void stackListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_stackListMouseClicked
            // simulate changing selection so selecting same spot twice works
            if (evt.getButton() == MouseEvent.BUTTON1) {
                stackValueChanged(null);
            }
	}//GEN-LAST:event_stackListMouseClicked

	private void scopeTreeTreeCollapsed(javax.swing.event.TreeExpansionEvent evt) {//GEN-FIRST:event_scopeTreeTreeCollapsed
            // note that path has closed, therefore no need to reopen when updating tree
            String expr = treePathToExpression(evt.getPath());
            openScopes.remove(expr);

            // also collapse kids?
            String child = expr + '\0';
            LinkedList<String> kids = new LinkedList<>();
            for (String s : openScopes) {
                if (s.startsWith(child)) {
                    kids.add(s);
                }
            }
            for (String s : kids) {
                openScopes.remove(s);
            }
	}//GEN-LAST:event_scopeTreeTreeCollapsed

	private void scopeTreeTreeExpanded(javax.swing.event.TreeExpansionEvent evt) {//GEN-FIRST:event_scopeTreeTreeExpanded
            String expr = treePathToExpression(evt.getPath());
            openScopes.add(expr);
	}//GEN-LAST:event_scopeTreeTreeExpanded

	private void scopeTreeTreeWillExpand(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {//GEN-FIRST:event_scopeTreeTreeWillExpand
            TreePath path = evt.getPath();

            // if we have already expanded this node, we know the children already: do nothing
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getChildCount() != 1 || ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject() != DUMMY_NODE_OBJECT) {
                return;
            }

            String expr = treePathToExpression(path);
            int scope = stackList.getSelectedIndex();
            if (scope < 0) {
                scope = 0;
            }
            String[] kids = perform(Command.SCOPE, String.valueOf(scope), expr);
            if (kids != null) {
                node.removeAllChildren();
                java.util.Arrays.sort(kids);
                for (int i = 0; i < kids.length; ++i) {
                    int split = kids[i].lastIndexOf('\0');
                    String name = kids[i].substring(0, split);
                    boolean hasKids = kids[i].charAt(split + 1) != '0';

                    split = kids[i].indexOf(':', split + 2);

                    String type = "";
                    if (split >= 0) {
                        type = kids[i].substring(split + 1);
                    }

                    ScopeDesc desc = new ScopeDesc(name, type, kids.length > 0);
                    DefaultMutableTreeNode child = new DefaultMutableTreeNode(desc);
                    node.add(child);
                    // if object has at least one property, add a dummy node
                    // so this node can be expanded
                    if (hasKids) {
                        child.add(new DefaultMutableTreeNode(DUMMY_NODE_OBJECT));
                    }
                }
            }
	}//GEN-LAST:event_scopeTreeTreeWillExpand

	private void scopeTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_scopeTreeValueChanged
            TreePath path = scopeTree.getSelectionPath();
            if (path == null) {
                scopeObjField.setText("");
                typeLabel.setText("");
                propertyField.setText("");
                return;
            }
            int frame = stackList.getSelectedIndex();
            if (frame < 0) {
                frame = 0;
            }

            String expr = treePathToExpression(path);
            String[] value = perform(Command.SCOPEEVAL, String.valueOf(frame), expr);
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < value.length; ++i) {
                if (i > 0) {
                    b.append('\n');
                }
                b.append(value[i]);
            }
            scopeObjField.setText(b.toString());
            scopeObjField.select(0, 0);

            typeLabel.setText(((ScopeDesc) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject()).type);

            StringBuilder p = new StringBuilder(40);
            if (path.getPathCount() >= 2) {
                if (((DefaultMutableTreeNode) path.getPathComponent(1)).getUserObject().equals(SCOPE_THIS)) {
                    p.append("this");
                }
                for (int i = 2; i < path.getPathCount(); ++i) {
                    if (p.length() > 0) {
                        p.append('.');
                    }
                    p.append(((DefaultMutableTreeNode) path.getPathComponent(i)).getUserObject());
                }
            }
            propertyField.setText(p.toString());
	}//GEN-LAST:event_scopeTreeValueChanged

    /**
     * Handling pressing the debug view button. Swap views.
     */
	private void debugBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugBtnActionPerformed
            infoBtn.setSelected(!debugBtn.isSelected());

            showPanel("main");
            checkConnection();
	}//GEN-LAST:event_debugBtnActionPerformed

	private void sourceTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sourceTableMouseExited

            /////////////////////////////////////////////////////////////////////
            // NOTE: this method is called in place of a hidePopup() method;   //
            //       if other functionality is added you MUST separate current //
            //       calls to sourceTableMouseExited( null ) to a hidePopup()  //
            /////////////////////////////////////////////////////////////////////
            if (popup != null) {
                popup.hide();
                popup = null;
            }
	}//GEN-LAST:event_sourceTableMouseExited

	private void propertyFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_propertyFieldActionPerformed
            String expr = propertyField.getText().trim();
            if (expr.isEmpty()) {
                scopeTree.clearSelection();
                return;
            }
            String[] tokens = expr.split("\\.");
            StringBuilder scope = new StringBuilder();
            for (int i = 0; i < tokens.length; ++i) {
                if (i == 0) {
                    if (tokens[0].equals("this")) {
                        scope.append("<this>");
                        continue;
                    } else {
                        scope.append("<scope>\0");
                    }
                } else {
                    scope.append('\0');
                }
                scope.append(tokens[i]);
            }
            TreePath path = expressionToTreePath(scope.toString(), true);
            if (path != null) {
                scopeTree.setSelectionPath(path);
                scopeTree.scrollPathToVisible(path);
            } else {
                UIManager.getLookAndFeel().provideErrorFeedback(scopeTree);
            }
	}//GEN-LAST:event_propertyFieldActionPerformed

	private void clearCachesActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_clearCachesActionPerformed
            perform(Command.CACHEMETRICS, "-2");
            Object sel = infoSelectCombo.getSelectedItem();
            if (sel != null && sel.toString().equals("Cache Metrics")) {
                infoSelectComboActionPerformed(null);
            } else {
                infoSelectCombo.setSelectedItem("Cache Metrics");
            }
	}//GEN-LAST:event_clearCachesActionPerformed

    private void scanBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scanBtnActionPerformed
        DiscoveryDialog dd = new DiscoveryDialog(this);
        dd.setVisible(true);
        DiscoveryService.ServerInfo info = dd.getSelectedServer();
        if (info != null) {
            hostField.setText(info.address.getHostName());
            portField.setValue(info.port);
            if (connectBtn.isEnabled()) {
                connectBtn.doClick(0);
            }
        }
    }//GEN-LAST:event_scanBtnActionPerformed

    private void backBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backBtnActionPerformed
        connected = false;
        autoConnect = false;
        updateToolState();
        errorLabel.setVisible(false);
        errorLabel2.setVisible(false);
        showPanel("connect");
    }//GEN-LAST:event_backBtnActionPerformed

    private Popup popup;
    private String popupExpression;

    private String mergeResult(String[] result) {
        if (result == null) {
            return null;
        }
        StringBuilder b = new StringBuilder(result.length * 20);
        for (int i = 0; i < result.length; ++i) {
            b.append(result[i]);
        }
        return b.toString();
    }

    /**
     * Returns the actual stack frame index for a stack frame argument. If
     * {@code index} is less than 0, it will return the selected stack frame.
     * The result is clamped the the number of frames.
     *
     * @param stackFrame the index to correct
     * @return the actual index for this index parameter
     */
    private int stackIndex(int stackFrame) {
        if (stackFrame < 0) {
            stackFrame = stackList.getSelectedIndex();
        }
        if (stackFrame >= stackList.getModel().getSize()) {
            stackFrame = stackList.getModel().getSize() - 1;
        }
        if (stackFrame < 0) {
            stackFrame = 0;
        }
        return stackFrame;
    }

    /**
     * Performs the command without displaying a wait cursor.
     *
     * @param command the command to perform
     * @param args the arguments (may be {@code null} if no arguments)
     * @return the result of the command as an array of lines, or {@code null}
     * if there was an error
     * @throws NullPointerException if {@code name} is null
     * @throws IllegalArgumentException if the number of arguments is incorrect
     */
    private String[] performQuietly(Command command, String... args) {
        final int argc = args == null ? 0 : args.length;
        if (argc != command.getArgCount()) {
            throw new IllegalArgumentException(
                    "expected " + command.getArgCount() + " for " + command + ", but got " + argc
            );
        }
        return send(command.name(), false, args);
    }

    /**
     * Sends a command to the server and returns the reply. This is the internal
     * implementation used by {@link #perform} and {@link #performQuietly}. It
     * is not restricted to commands enumerated by {@code Command}.
     *
     * @param name the raw string that names the command
     * @param raw if {@code true}, the server reply is sent back verbatim (only
     * used for debugging)
     * @param args the arguments (may be {@code null} if no arguments)
     * @return the result of the command as an array of lines, or {@code null}
     * if there was an error
     * @throws NullPointerException if {@code name} is null
     */
    String[] send(String name, boolean raw, String... args) {
        Socket s = null;
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            s = new Socket(host, port);
            s.setSoTimeout(soTimeout);
            s.setPerformancePreferences(2, 1, 0);
            s.setTcpNoDelay(true);
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), TextEncoding.DEBUGGER_CS)));
            in = new BufferedReader(new InputStreamReader(s.getInputStream(), TextEncoding.DEBUGGER_CS));
            out.println("SEDP3");
            out.println(name);
            if (args != null) {
                for (int i = 0; i < args.length; ++i) {
                    out.println(args[i]);
                }
            }
            out.flush();

            LinkedList<String> results = new LinkedList<>();
            String line;
            while ((line = in.readLine()) != null) {
                results.add(line);
            }

            // if raw mode return immediately with the unprocessed results
            if (raw) {
                return results.toArray(new String[0]);
            }

            if (results.isEmpty() || !results.get(0).equals("SEDP3 OK")) {
                System.err.println("Strange Eons Debugger Client: Protocol Exception");
                for (String v : results) {
                    System.err.println(v);
                }
                return null;
            }
            results.remove(0);
            String[] lines = results.toArray(new String[0]);

            // if we got results from a PROBE command, check the cookie
            if (lines.length == 1 && name.equals(Command.PROBE.name())) {
                int cookie = Integer.parseInt(lines[0]);
                if (lastFrameUpdate != cookie) {
                    maybeReachedBreakpoint = true;
                    lastFrameUpdate = cookie;
                }
            }

            return lines;
        } catch (SocketTimeoutException e) {
            fail(e);
            if (soTimeout < 32000) {
                soTimeout *= 2;
            }
            return null;
        } catch (IOException e) {
            fail(e);
            return null;
        } finally {
            close(out);
            close(in);
            try {
                if (s != null) {
                    s.close();
                }
            } catch (IOException e) {
                fail(e);
            }
        }
    }

    private int soTimeout = 500;

    private void close(Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            fail(e);
        }
    }

    private void fail(IOException e) {
        System.err.println("DEBUG CLIENT: Connect exception");
        e.printStackTrace();
    }

    /**
     * Starts the debugger client application. Note that this should be started
     * as a separate process from the main application so that the debugger and
     * the application have separate event dispatch threads.
     *
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        ClientArguments arguments = new ClientArguments();
        arguments.parse(null, args);
        if (arguments.search) {
            searchForClients(arguments.host);
            System.exit(0);
        }

        java.awt.EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            } catch (Exception e) {
            }
            Client client = new Client();
            if (arguments.host != null) {
                client.host = arguments.host.trim();
            }
            if (arguments.port >= 1 && arguments.port <= 65535) {
                client.port = arguments.port;
            }
            client.setVisible(true);
        });
    }

    private static void searchForClients(String host) {
        try {
            DiscoveryService ds = host == null ? new DiscoveryService() : new DiscoveryService(InetAddress.getByName(host));
            ds.setDiscoveryConsumer(info -> {
                // since multi-threaded, entire description must use just one println
                System.out.println(
                        "Host:         " + info.address.getHostName() + ", port " + info.port + '\n'
                        + "ID:           " + info.pid + '.' + info.hash + '\n'
                        + "Version:      build " + info.buildNumber + " (" + info.version + ")\n"
                        + "Test bundles: " + info.testBundle + "\n"
                );
            });
            ds.search();
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(20);
        }
    }

    /**
     * Show or hide a wait cursor.
     *
     * @param wait if {@code true}, show a wait cursor
     */
    private void waitCursor(boolean wait) {
        if (!EventQueue.isDispatchThread()) {
            return;
        }
        final Component glass = getGlassPane();
        if (wait) {
            if (waitDepth++ == 0) {
                glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                getGlassPane().setVisible(true);
            }
        } else {
            if (--waitDepth == 0) {
                glass.setCursor(Cursor.getDefaultCursor());
                glass.setVisible(false);
            }
            if (waitDepth < 0) {
                System.err.println("Assertion: waitDepth < 0");
                waitDepth = 0;
            }
        }
    }
    private int waitDepth = 0;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backBtn;
    private javax.swing.JButton breakBtn;
    private javax.swing.JToggleButton breakDebuggerBtn;
    private javax.swing.JToggleButton breakEnterBtn;
    private javax.swing.JToggleButton breakExitBtn;
    private javax.swing.JToggleButton breakThrowBtn;
    private javax.swing.JPanel cardRoot;
    private javax.swing.JButton clearCaches;
    private javax.swing.JButton connectBtn;
    private javax.swing.JPanel connectPanel;
    private javax.swing.JPanel connectingPanel;
    private javax.swing.JToggleButton debugBtn;
    private javax.swing.JLabel errorLabel;
    private javax.swing.JLabel errorLabel2;
    private ca.cgjennings.ui.JLabelledField filterField;
    private ca.cgjennings.ui.JHelpButton helpButton;
    private javax.swing.JTextField hostField;
    private javax.swing.JToggleButton infoBtn;
    private javax.swing.JPanel infoControlPanel;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JScrollPane infoScroll;
    private javax.swing.JComboBox infoSelectCombo;
    private javax.swing.JTable infoTable;
    private javax.swing.JPanel infoTablePanel;
    private javax.swing.JLabel infoTitle;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel leftFill;
    private javax.swing.JLabel linkLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JToggleButton onTopBtn;
    private javax.swing.JSpinner portField;
    private javax.swing.JLabel propHeadLabel;
    private javax.swing.JTextField propertyField;
    private javax.swing.JButton refreshBtn;
    private ca.cgjennings.ui.JLinkLabel removeAllBreaksBtn;
    private javax.swing.JLabel rightFill;
    private javax.swing.JButton runBtn;
    private javax.swing.JButton scanBtn;
    private javax.swing.JTextArea scopeObjField;
    private javax.swing.JScrollPane scopeOutField;
    private javax.swing.JLabel scopeTitle;
    private javax.swing.JTree scopeTree;
    private javax.swing.JScrollPane scopeTreeScroll;
    private javax.swing.JSplitPane scopeTreeSplit;
    private javax.swing.JList scriptList;
    private javax.swing.JSplitPane scriptSplit;
    private javax.swing.JLabel scriptTitle;
    private javax.swing.JLabel sourcePopup;
    private javax.swing.JScrollPane sourceScroll;
    private javax.swing.JSplitPane sourceSplit;
    private javax.swing.JTable sourceTable;
    private javax.swing.JLabel sourceTitle;
    private javax.swing.JPanel sourceTitlePanel;
    private javax.swing.JList stackList;
    private javax.swing.JSplitPane stackSplit;
    private javax.swing.JLabel stackTitle;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JButton stepIntoBtn;
    private javax.swing.JButton stepOutBtn;
    private javax.swing.JButton stepOverBtn;
    private javax.swing.JButton stopBtn;
    private javax.swing.JPanel toolPanel;
    private javax.swing.JLabel typeLabel;
    private javax.swing.JPanel typePanel;
    private javax.swing.JButton walkBtn;
    private javax.swing.JMenuItem watchCopyItem;
    private javax.swing.JPopupMenu watchPopup;
    private javax.swing.JScrollPane watchScroll;
    private javax.swing.JSplitPane watchSplit;
    private javax.swing.JTable watchTable;
    private javax.swing.JLabel watchTitle;
    // End of variables declaration//GEN-END:variables

    /**
     * Renders entries in the list of source files.
     */
    private static final DefaultListCellRenderer fileListRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String text = value.toString();
            if (text.startsWith("jar:")) {
                int excl = text.indexOf('!');
                text = text.substring(excl + 1);
            }
            super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            if (getPreferredSize().width > list.getWidth()) {
                setToolTipText(value.toString());
            } else {
                setToolTipText(null);
            }
            return this;
        }
    };
    private static final LineInfo[] EMPTY_TABLE = new LineInfo[0];
    private SourceModel source = new SourceModel();

    /**
     * Table model for source code files.
     */
    private class SourceModel extends AbstractTableModel {

        private String file;
        private LineInfo[] lines = EMPTY_TABLE;
        private final JavaScriptTokenizer tokenizer = new JavaScriptTokenizer();
        private final HTMLStyler codeStyler = new HTMLStyler(tokenizer);

        public void setSource(String url) {
            waitCursor(true);
            try {
                lines = EMPTY_TABLE;
                file = url;
                synch();
                fireTableDataChanged();
                sourceTable.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            } finally {
                waitCursor(false);
            }
        }

        public void synch() {
            if (file == null) {
                return;
            }

            checkConnection();
            String[] text = perform(Command.SOURCE, file);
            String[] bps = perform(Command.BREAKPOINTS, file);

            if (text == null || bps == null) {
                return;
            }

            boolean lineUpdate = false;
            if (lines.length != text.length) {
                lineUpdate = true;
            } else {
                for (int i = 0; i < text.length; ++i) {
                    if (!text[i].equals(lines[i].text)) {
                        lineUpdate = true;
                        break;
                    }
                }
            }

            if (lineUpdate) {
                codeStyler.reset();
                lines = new LineInfo[text.length];
                for (int i = 0; i < text.length; ++i) {
                    lines[i] = new LineInfo(
                            i + 1, false, false, text[i],
                            "<html><pre>" + codeStyler.styleLine(text[i], i));
                }

                TableColumn col = sourceTable.getColumnModel().getColumn(0);
                String maxLine = String.valueOf(lines.length).replaceAll(".", "0");
                FontMetrics fm = sourceTable.getFontMetrics(sourceTable.getFont());
                int width = Math.max(fm.stringWidth(maxLine), fm.stringWidth("0000")) + 8;
                col.setWidth(width);
                col.setPreferredWidth(width);
                col.setMinWidth(width);
                col.setMaxWidth(width);
                col.setResizable(false);
            }

            boolean breakUpdate = lineUpdate;
            int j = 0;
            int nextLine = -1;
            for (int i = 0; i < lines.length; ++i) {
                if (j >= bps.length) {
                    breakUpdate |= lines[i].setBreak(false, false);
                } else {
                    if (nextLine < 0) {
                        nextLine = Integer.parseInt(bps[j]);
                        j += 2;
                    }
                    if (i == nextLine) {
                        if (j == bps.length) {
                            breakUpdate |= lines[i].setBreak(true, false);
                        } else {
                            breakUpdate |= lines[i].setBreak(true, bps[j - 1].equals("X"));
                        }
                        nextLine = -1;
                    } else {
                        breakUpdate |= lines[i].setBreak(false, false);
                    }
                }
            }
            if (breakUpdate) {
                sourceTable.repaint();
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return lines.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return lines[rowIndex];
        }

        /**
         * For the given table and point, try to find an identifier to look up.
         * If found, try to determine a value. If a value can be determined,
         * return it as a string. Otherwise return {@code null}.
         *
         * @param t the table (normally sourceTable)
         * @param p the point in the table
         * @return a valid JS identifier or dot-expression, or {@code null}
         */
        public String identifierUnderPoint(JTable t, Point p) {
            final int row = t.rowAtPoint(p);
            if (row < 0) {
                return null;
            }
            int col = t.columnAtPoint(p);
            if (col != 1) {
                return null; // the source col, not the line num col
            }
            LineInfo lineInfo = lines[row];
            final String sourceLine = lineInfo.text;
            TableCellRenderer r = t.getCellRenderer(row, col);

            // copy the style of the renderer to a text field, then use the
            // text field to figure out what character the cursor is over
            JLabel c = (JLabel) r.getTableCellRendererComponent(t, lineInfo, false, false, row, col);
            idLookupField.setBorder(c.getBorder());
            idLookupField.setFont(c.getFont());
            idLookupField.getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);
            idLookupField.setText(sourceLine); // use the unformatted version (without html tags)
            idLookupField.setSize(Integer.MAX_VALUE, idLookupField.getPreferredSize().height);
            idLookupField.select(0, 0);

            // move the point from table space to text field space
            p.x -= t.getCellRect(row, col, false).x;
            p.y = idLookupField.getHeight() / 2;

            // find the index of the character under the mouse
            col = idLookupField.viewToModel(p);
            if (col >= sourceLine.length()) {
                col = sourceLine.length() - 1;
            }
            if (col < 0) {
                return null;
            }

            // find the identifier that our column is in
            String expression = null;
            boolean inID = false;
            int start = 0;
            for (int i = 0; i <= sourceLine.length(); ++i) {
                // add a sentinel to the end of the line so we always end the current ID, if any
                char ch = i == sourceLine.length() ? '\0' : sourceLine.charAt(i);

                if (inID) {
                    // valid ID part?
                    // Note: if a '.', we allow it only before the column under the cursor;
                    // that way you can point at the last word of "id.property" to look
                    // up "id.property", or the first word to look up just "id"
                    if (!(Character.isJavaIdentifierPart(ch) || (ch == '.' && i < col))) {
                        // we found an ID, is the point under the cursor within the ID?
                        // if so, capture the expression and stop looping
                        if (col < i) {
                            expression = sourceLine.substring(start, i);
                            break;
                        }
                        inID = false;
                    }

                } else {
                    // valid ID start?
                    if (Character.isJavaIdentifierStart(ch) || ch == '@' || ch == '#') {
                        // if we are after the point under the cursor, then the cursor is not
                        // over a valid ID so we can stop looping
                        if (col < i) {
                            break;
                        }
                        start = i;
                        inID = true;
                    }
                }
            }

            // check if our identifer is actually a keyword, comment or other non-ID
            if (expression != null) {
                if (row >= tokenizer.getLineCount()) {
                    AssertionError e = new AssertionError("invalid state: tokenizer does not represent displayed document");
                    e.printStackTrace();
                    return null;
                }
                Token prevToken = null;
                Token token = tokenizer.tokenize(sourceLine, row);
                int offset = 0;

                // NOTE: we use start instead of col because sometimes the tokenizer
                // mistokenizes a segment after a dot, e.g., in Object.get it thinks
                // get is a keyword
                while (offset <= start && token != null) {
                    offset += token.length();
                    prevToken = token;
                    token = token.next();
                }
                token = prevToken;
                // find this ID in list of acceptable IDs
                boolean found = false;
                if (token != null) {
                    for (int i = 0; i < acceptedIDs.length; ++i) {
                        if (acceptedIDs[i] == token.type()) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    expression = null;
                }
            }

            return expression;
        }
        private final JTextField idLookupField = new JTextField();
        private final TokenType[] acceptedIDs = new TokenType[]{
            TokenType.PLAIN, TokenType.KEYWORD2, TokenType.LITERAL_SPECIAL_1, TokenType.LITERAL_SPECIAL_2
        };
    };

    /**
     * Syntax highlighting source table renderer.
     */
    private DefaultTableCellRenderer sourceRenderer = new DefaultTableCellRenderer() {

        private final Border lineBorder = BorderFactory.createEmptyBorder(0, 4, 0, 1);
        private final Border numberBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(0, 1, 0, 4));
        private final Border lastLineBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY),
                lineBorder);
        private final Border lastNumberBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY),
                numberBorder);
        private final Color selectionBkg = new Color(0xB9_DBFD);
        private final Color numberBkg = new Color(0xf2_f2f2);
        private final Color breakBkg = new Color(0xFD_B9D4);
        private final Color stackTopBkg = new Color(0xD4_FDB9);
        private final Color stackTopUninterruptedBkg = new Color(0xB9_D4FD);
        private Font linkFont;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            LineInfo info = (LineInfo) value;
            setFont(table.getFont());
            final boolean lastLine = row == table.getRowCount() - 1;
            if (column == 0) {
                setBorder(lastLine ? lastNumberBorder : numberBorder);
                if (info.breakable) {
                    if (linkFont == null) {
                        linkFont = getFont().deriveFont(
                                Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
                    }
                    setFont(linkFont);
                    setForeground(Color.BLUE);
                    setBackground(info.breakpoint ? breakBkg : numberBkg);
                } else {
                    setForeground(Color.BLACK);
                    setBackground(numberBkg);
                }
                setHorizontalAlignment(RIGHT);
                setText(String.valueOf(info.number));
            } else {
                setBorder(lastLine ? lastLineBorder : lineBorder);
                setBackground(isSelected ? selectionBkg : Color.WHITE);

                final int sourceLine = row + 1;
                for (int i = stackModel.getSize() - 1; i >= 0; --i) {
                    StackFrame frame = (StackFrame) stackModel.get(i);
                    if (frame.line == sourceLine && frame.file.equals(source.file)) {
                        if (i == 0) {
                            setBackground(interrupted ? stackTopBkg : stackTopUninterruptedBkg);
                        } else {
                            setBackground(stackTopUninterruptedBkg);
                        }
                    }
                }

                setForeground(Color.BLACK);
                setHorizontalAlignment(LEFT);
                setText(info.formatted);
            }
            return this;
        }
    };

    /**
     * Represents a line in the current source file.
     */
    private static class LineInfo {

        public Integer number;
        public boolean breakable;
        public boolean breakpoint;
        public String text;
        public String formatted;

        public LineInfo(Integer number, boolean breakable, boolean breakpoint, String text, String formatted) {
            this.number = number;
            this.breakable = breakable;
            this.breakpoint = breakpoint;
            this.text = text;
            this.formatted = formatted;
        }

        /**
         * Updates whether this line info represents the line as being a valid
         * breakpoint and having a break set, and returns {@code true} if this
         * changes the internal state of this object. This does not effect
         * whether the line has a breakpoint on the server.
         *
         * @param breakable if {@code true} represents that the line should be
         * breakable
         * @param breakpoint if {@code true} represents that the line has a
         * breakpoint set
         * @return {@code true} if the internal representation of the line
         * changes as a result
         */
        public boolean setBreak(boolean breakable, boolean breakpoint) {
            if (this.breakable != breakable || this.breakpoint != breakpoint) {
                this.breakable = breakable;
                this.breakpoint = breakpoint;
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return formatted;
        }
    }

    /**
     * Represents a call stack frame in the interrupted script execution.
     */
    private static class StackFrame {

        private String file;
        private int line;

        public StackFrame(String file, int line) {
            this.file = file;
            this.line = line;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StackFrame other = (StackFrame) obj;
            if ((this.file == null) ? (other.file != null) : !this.file.equals(other.file)) {
                return false;
            }
            return this.line == other.line;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 67 * hash + (this.file != null ? this.file.hashCode() : 0);
            hash = 67 * hash + this.line;
            return hash;
        }

        @Override
        public String toString() {
            return file + " : " + line;
        }
    }
    private WatchModel watchModel = new WatchModel();

    /**
     * Model for the watch expression table.
     */
    private class WatchModel extends AbstractTableModel {

        private final ArrayList<String> expr = new ArrayList<>();
        private ArrayList<String> eval = new ArrayList<>();

        @Override
        public String getColumnName(int column) {
            return column == 0 ? string("watch-expr-col") : string("watch-val-col");
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return expr.size() + 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == expr.size()) {
                return "";
            } else {
                return columnIndex == 0 ? expr.get(rowIndex) : eval.get(rowIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        public void addWatch(String watchExpr) {
            setValueAt(watchExpr, expr.size(), 0);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String val = aValue.toString().trim();
            if (rowIndex == expr.size()) {
                if (!val.isEmpty()) {
                    expr.add(val);
                    eval.add("undefined");
                    fireTableRowsInserted(rowIndex, rowIndex);
                    watchTable.setRowHeight(rowIndex, watchTable.getRowHeight());
                    watchTable.clearSelection();
                    watchTable.addRowSelectionInterval(rowIndex + 1, rowIndex + 1);
                    updateValue(rowIndex, -1);
                }
            } else {
                if (!val.isEmpty()) {
                    expr.set(rowIndex, val);
                    eval.set(rowIndex, "undefined");
                    updateValue(rowIndex, -1);
                } else {
                    delete(rowIndex);
                }
            }
        }

        public void delete(int row) {
            if (row == eval.size()) {
                return;
            }
            expr.remove(row);
            eval.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public void updateValues(int frame) {
            frame = stackIndex(frame);
            int num = expr.size();
            for (int i = 0; i < num; ++i) {
                updateValue(i, frame);
            }
        }

        private final String RUNNING_EVALUATION = string("watch-not-interrupted");
        private final String[] DUMMY_EVALUATION = {string("watch-cant-eval")};

        public void updateValue(int row, int frame) {
            String evaluation;
            if (!interrupted) {
                evaluation = RUNNING_EVALUATION;
                watchTable.setRowHeight(row, watchTable.getRowHeight());
            } else {
                frame = stackIndex(frame);

                String[] reply = performQuietly(Command.EVAL, String.valueOf(frame), expr.get(row));
                if (reply == null) {
                    reply = DUMMY_EVALUATION;
                }

                if (reply.length < 2) {
                    evaluation = reply.length == 0 ? "" : reply[0];
                    watchTable.setRowHeight(row, watchTable.getRowHeight());
                } else {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < reply.length; ++i) {
                        if (i > 0) {
                            b.append('\n');
                        }
                        b.append(reply[i]);
                    }
                    int lineHeight = watchTable.getFontMetrics(watchTable.getFont()).getHeight();
                    int height = lineHeight * reply.length + (watchTable.getRowHeight() - lineHeight);
                    watchTable.setRowHeight(row, height);
                    evaluation = b.toString();
                }
            }

            eval.set(row, evaluation.trim());
            fireTableCellUpdated(row, 1);
        }

        private void init() {
            Action deleteRows = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (watchTable.isEditing()) {
                        return;
                    }
                    int[] rows = watchTable.getSelectedRows();
                    if (rows.length == 0) {
                        return;
                    }

                    int postDeleteSelection = rows[rows.length - 1] + 1 - rows.length;
                    Arrays.sort(rows);
                    for (int i = rows.length - 1; i >= 0; --i) {
                        if (rows[i] == expr.size()) {
                            continue;
                        }
                        watchModel.delete(rows[i]);
                    }
                    postDeleteSelection = Math.max(0, Math.min(postDeleteSelection, watchModel.getRowCount() - 1));
                    watchTable.setRowSelectionInterval(postDeleteSelection, postDeleteSelection);
                }
            };
            InputMap imap = watchTable.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap amap = watchTable.getActionMap();
            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "DELETE");
            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.META_DOWN_MASK), "DELETE");
            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_DOWN_MASK), "DELETE");
            amap.put("DELETE", deleteRows);

            //watchTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
            final DefaultCellEditor editor = ((DefaultCellEditor) watchTable.getDefaultEditor(String.class));
            editor.setClickCountToStart(1);
            editor.getComponent().setFont(watchTable.getFont());

            watchTable.addMouseMotionListener(new MouseMotionAdapter() {

                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    int col = watchTable.columnAtPoint(p);
                    Cursor c = Cursor.getDefaultCursor();
                    if (col == 0) {
                        c = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
                    }
                    watchTable.setCursor(c);
                }
            });
            JTableHeader thead = watchTable.getTableHeader();
            thead.setFont(thead.getFont().deriveFont(thead.getFont().getSize2D() - 2f));
//			watchRenderer.setVerticalTextPosition( JLabel.TOP );
//			watchRenderer.setVerticalAlignment( JLabel.TOP );
            thead.setReorderingAllowed(false);
            watchTable.setDefaultRenderer(Object.class, watchRenderer);
            updateMinColWidth(watchTable);

            for (int i = 0; settings.get("watch-" + i) != null; ++i) {
                watchModel.addWatch(settings.get("watch-" + i));
            }
        }

        private void updateSettings() {
            int i = 0;
            for (; i < expr.size(); ++i) {
                settings.set("watch-" + i, expr.get(i));
            }
            for (; settings.get("watch-" + i) != null; ++i) {
                settings.reset("watch-" + i);
            }
        }
    }

    /**
     * Watch expression table cell renderer.
     */
    private TableCellRenderer watchRenderer = new MultilineTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            boolean enterWatchHint = false;
            String s;
            if (column == 0) {
                s = value.toString();
                if (s.isEmpty() && row == table.getRowCount() - 1) {
                    enterWatchHint = true;
                    s = string("watch-hint");
                }
            } else if (row < table.getRowCount() - 1) {
                s = watchModel.eval.get(row);
            } else {
                s = "";
            }
            super.getTableCellRendererComponent(table, s, isSelected, hasFocus, row, column);

            if ((column == 1 && !interrupted) || enterWatchHint) {
                setForeground(Color.GRAY);
            }

            //setToolTipText( s );
            return this;
        }
    };

    /**
     * Sets all columns in a table to min width of 1.
     */
    private static void updateMinColWidth(JTable table) {
        for (int i = 0; i < table.getColumnCount() - 1; ++i) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setMinWidth(1);
        }
    }

    /**
     * The shared scope description representing the current scope.
     */
    private final ScopeDesc SCOPE_SCOPE = new ScopeDesc("<scope>", null, true);

    /**
     * The shared scope description representing the this object in the current
     * scope.
     */
    private final ScopeDesc SCOPE_THIS = new ScopeDesc("<this>", null, true);

    /**
     * A temporary object set as the child of an unexpanded tree node when its
     * true children have not yet been loaded from the server.
     */
    private static final String DUMMY_NODE_OBJECT = "...";

    private void synchScopeTree() {
        String sel = treePathToExpression(scopeTree.getSelectionPath());

        // currently rebuilds the tree from scratch
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
        final DefaultMutableTreeNode scope = new DefaultMutableTreeNode(SCOPE_SCOPE);
        final DefaultMutableTreeNode thiz = new DefaultMutableTreeNode(SCOPE_THIS);
        root.add(scope);
        root.add(thiz);
        scope.add(new DefaultMutableTreeNode(DUMMY_NODE_OBJECT));
        thiz.add(new DefaultMutableTreeNode(DUMMY_NODE_OBJECT));

        DefaultTreeModel scopeModel = getScopeModel();
        scopeModel.setRoot(root);
        scopeModel.reload();
        for (final String k : openScopes) {
            expressionToTreePath(k, true);
        }

        // update root icons
        String[] result = performQuietly(Command.SCOPE, String.valueOf(stackIndex(-1)), "<root>");
        if (result != null && result.length == 2) {
            ScopeDesc node = SCOPE_SCOPE;
            for (int i = 0; i < 2; ++i) {
                int typeStart = result[i].indexOf(':') + 1;
                if (typeStart > 0) {
                    node.updateType(result[i].substring(typeStart));
                }
                node = SCOPE_THIS;
            }
        }

        scopeTree.setSelectionPath(expressionToTreePath(sel, false));
    }

    /**
     * Convert a sequence of expression names separated by null characters into
     * a tree path in the scope tree. If {@code openPath} is {@code true}, the
     * scope nodes will be opened as the path is decoded. This is necessary when
     * the tree is being updated after reaching a new breakpoint since the tree
     * model might be new and thus not have the necessary children installed.
     *
     * @param expr the expression to expand
     * @param openPath if {@code true} expands the tree as it goes
     * @return the path used to access this expression in the scope tree, or
     * {@code null}
     */
    private TreePath expressionToTreePath(String expr, boolean openPath) {
        if (expr == null) {
            return null;
        }
        String[] els = expr.split("\0");
        Object[] nodes = new Object[els.length + 1]; // +1 for hidden tree root node
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) getScopeModel().getRoot();
        nodes[0] = parent;

        for (int n = 0; n < els.length; ++n) {
            int kids = parent.getChildCount();
            int i;
            for (i = 0; i < kids; ++i) {
                DefaultMutableTreeNode kid = (DefaultMutableTreeNode) parent.getChildAt(i);
                ScopeDesc desc = (ScopeDesc) kid.getUserObject();
                if (els[n].equals(desc.name)) {
                    nodes[n + 1] = kid;
                    if (openPath) {
                        TreePath path = new TreePath(Arrays.copyOf(nodes, n + 2));
                        scopeTree.expandPath(path);
                    }
                    parent = kid;
                    break;
                }
            }
            // expression is not in the tree model
            if (i == kids) {
                return null;
            }
        }
        return new TreePath(nodes);
    }

    private String treeNodeToExpression(DefaultMutableTreeNode node) {
        if (node == null) {
            return null;
        }
        Object[] path = node.getUserObjectPath();
        if (path.length < 2) {
            return null;
        }
        StringBuilder b = new StringBuilder(path[1].toString());
        for (int i = 2; i < path.length; ++i) {
            b.append('\0').append(path[i].toString());
        }
        return b.toString();
    }

    private String treePathToExpression(TreePath path) {
        if (path == null) {
            return null;
        }
        return treeNodeToExpression((DefaultMutableTreeNode) path.getLastPathComponent());
    }

    private DefaultTreeModel getScopeModel() {
        return (DefaultTreeModel) scopeTree.getModel();
    }

    private HashSet<String> openScopes = new HashSet<>();
    private TreeCellRenderer scopeRenderer = new DefaultTreeCellRenderer() {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            value = ((DefaultMutableTreeNode) value).getUserObject();
            if (value instanceof ScopeDesc) {
                final ScopeDesc sd = (ScopeDesc) value;
                setIcon(sd.icon);
                setToolTipText(sd.toolTip);
            }
            return this;
        }

        @Override
        public Icon getClosedIcon() {
            return null;
        }

        @Override
        public Icon getLeafIcon() {
            return null;
        }

        @Override
        public Icon getOpenIcon() {
            return null;
        }
    };

    /**
     * The user object that represents a node in the scope tree.
     */
    private static class ScopeDesc {

        private Icon icon;
        private String name;
        private String type;
        private String toolTip;
        private final boolean hasKids;

        public ScopeDesc(String name, String typeDesc, boolean hasKids) {
            this.name = name;
            this.hasKids = true;
            updateType(typeDesc);
        }

        public void updateType(String typeDesc) {
            type = typeDesc == null ? "" : typeDesc;
            if (type.isEmpty() || type.equalsIgnoreCase("null") || type.equalsIgnoreCase("undefined")) {
                icon = ICON_NULL;
            } else {
                if (!hasKids) {
                    icon = ICON_LEAF_PROPERTY;
                } else {
                    if (type.equalsIgnoreCase("Function")) {
                        icon = ICON_FUNCTION;
                    } else if (type.equalsIgnoreCase("global")) {
                        icon = ICON_GLOBAL;
                    } else if (type.equalsIgnoreCase("Call")) {
                        icon = ICON_CALL;
                    } else if (type.equalsIgnoreCase("JavaPackage")) {
                        icon = ICON_PACKAGE;
                    } else if (type.startsWith("JavaClass (")) {
                        icon = ICON_CLASS;
                    } else {
                        icon = ICON_OBJECT;
                    }
                }
            }
            toolTip = type.isEmpty() ? null : type;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Icon ICON_OBJECT = icon("ui/dev/circle.png", -1);
    private static final Icon ICON_FUNCTION = icon("ui/dev/ui/dev/triangle.png", -1);
    private static final Icon ICON_LEAF_PROPERTY = icon("ui/dev/ui/dev/diamond.png", -1);
    private static final Icon ICON_GLOBAL = icon("ui/dev/ui/dev/globe.png", -1);
    private static final Icon ICON_CALL = icon("ui/dev/ui/dev/cross.png", -1);
    private static final Icon ICON_NULL = icon("ui/dev/ui/dev/square.png", -1);
    private static final Icon ICON_PACKAGE = icon("ui/dev/ui/dev/package.png", -1);
    private static final Icon ICON_CLASS = icon("ui/dev/ui/dev/class.png", -1);

    /**
     * A 3-pixel solid dark gray split pane.
     */
    private static class ThinSplitPane extends JSplitPane {

        public ThinSplitPane() {
            setUI(new BasicSplitPaneUI());
            setBackground(Color.DARK_GRAY);
            setDividerSize(3);
            setOneTouchExpandable(false);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder());
            setContinuousLayout(true);
        }

        /**
         * Creates a link between two split panes that synchronizes their
         * divider sizes so that when one is resized the other is also.
         *
         * @param otherSplitPane the split pane to synchronize with
         */
        public void synchronizeDividerLocation(final JSplitPane otherSplitPane) {
            PropertyChangeListener li = (PropertyChangeEvent e) -> {
                int pos = (Integer) e.getNewValue();
                if (e.getSource() == otherSplitPane) {
                    if (getDividerLocation() != pos) {
                        setDividerLocation(pos);
                        // check if it was really set
                        if (getDividerLocation() != pos) {
                            otherSplitPane.setDividerLocation(getDividerLocation());
                        }
                    }
                } else {
                    if (otherSplitPane.getDividerLocation() != pos) {
                        otherSplitPane.setDividerLocation(pos);
                        // check if it was really set
                        if (otherSplitPane.getDividerLocation() != pos) {
                            setDividerLocation(otherSplitPane.getDividerLocation());
                        }
                    }
                }
            };
            addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, li);
            otherSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, li);
        }
    }

    /**
     * Reloads the list of available tables from the server, in case a new table
     * generator has been registered. By default, this is done only if
     * {@code INFO_TABLE_LIST_UPDATE_DELAY} ms have elapsed since the last
     * update.
     *
     * @param force if {@code true}, forces the list to be updated no matter how
     * recently this was last called
     */
    private void updateInfoTableList(boolean force) {
        if (force) {
            timeSinceLastInfoTableUpdate = System.currentTimeMillis();
        } else {
            final long time = System.currentTimeMillis();
            final long elapsed = time - timeSinceLastInfoTableUpdate;
            if (elapsed < INFO_TABLE_LIST_UPDATE_DELAY) {
                return;
            }
            timeSinceLastInfoTableUpdate = time;
        }

        String[] names = perform(Command.INFOTABLELIST);
        if (names == null) {
            checkConnection();
            return;
        }
        String sel = (String) infoSelectCombo.getSelectedItem();
        infoSelectCombo.setModel(new DefaultComboBoxModel(names));
        // restore old selection
        if (sel != null) {
            for (int i = 0; i < names.length; ++i) {
                if (names[i].equals(sel)) {
                    infoSelectCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    private long timeSinceLastInfoTableUpdate = 0L;
    private static final long INFO_TABLE_LIST_UPDATE_DELAY = 1_000 * 60 * 3;

    private void writeSettings() throws IOException {
        try (FileOutputStream out = new FileOutputStream(settingFile)) {
            props.store(out, "Debugger Client Settings");
        }
    }

    private void readSettings() throws IOException {
        try (FileInputStream in = new FileInputStream(settingFile)) {
            props.load(in);
        }
    }
    private final Properties props = new Properties();
    private Settings settings = Settings.forProperties(null, props);
    private static final File settingFile = StrangeEons.getUserStorageFile("debugger-client");

    private static String string(String key) {
        try {
            return ResourceBundle.getBundle("resources/text/interface/debugger").getString(key);
        } catch (MissingResourceException mre) {
            return "MISSING: " + key;
        }
    }

    private static String string(String key, Object... args) {
        return String.format(string(key), args);
    }
}
