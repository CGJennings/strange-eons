package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.SheetViewer;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.JUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.border.Border;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * The interface and logic for the Virtual Deck command. Instances are created
 * on demand by the associated task action.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class VirtualDeckDialog extends javax.swing.JDialog {

    private static final int PLAYERS = 9;

    /**
     * Creates new form VirtualDeckDialog
     */
    public VirtualDeckDialog(java.awt.Frame parent, List<Member> cards, CopiesList copies) {
        super(parent, false);
        for (int i = 0; i < playStacks.length; ++i) {
            playStacks[i] = new DefaultListModel<Card>();
        }
        play = playStacks[0];

        initComponents();
        createArrowIcons();

        hands = new JToggleButton[]{
            handAll, hand1, hand2, hand3, hand4, hand5, hand6, hand7, hand8
        };
        sheetPanel.add(viewer);

        deckList.setTransferHandler(dragHandler);
        playList.setTransferHandler(dragHandler);
        discardList.setTransferHandler(dragHandler);

        if (!Settings.getShared().applyWindowSettings("virtual-deck", this)) {
            pack();
            setLocationRelativeTo(parent);
        }

        for (Member m : cards) {
            final File f = m.getFile();
            if (!Deck.isDeckLayoutSupported(f)) {
                continue;
            }
            GameComponent gc = ResourceKit.getGameComponentFromFile(f, true);
            if (gc == null || !gc.isDeckLayoutSupported()) {
                continue;
            }
            int c = copies == null ? 1 : copies.getCopyCount(f);
            Card card = new Card(gc);
            deck.addElement(card);
            for (int i = 1; i < c; ++i) {
                card = card.clone();
                deck.addElement(card);
            }
            if (backIcon == null) {
                backIcon = card.createBackThumbnail();
            }
        }
        viewer.setBackground(deckPanel.getBackground());

        ownerPicker = new VirtualDeckOwnerSelector(this);
        // move 0 cards to init deck size counters
        moveCards(deck, play, 0, Collections.emptyList());
        shuffleBtnActionPerformed(null);
    }

    private final VirtualDeckOwnerSelector ownerPicker;
    private Icon backIcon;

    private DefaultListModel<Card> deck = new DefaultListModel<>();
    private DefaultListModel<Card> play;
    private DefaultListModel<Card> discard = new DefaultListModel<>();

    private JToggleButton[] hands;
    @SuppressWarnings("unchecked")
    private DefaultListModel<Card> playStacks[] = new DefaultListModel[PLAYERS];
    private int playSelections[][] = new int[PLAYERS][];
    private int playScrollPos[] = new int[PLAYERS];

    private void moveCards(DefaultListModel<Card> from, DefaultListModel<Card> to, int pos, Card[] cards) {
        moveCards(from, to, pos, Arrays.asList(cards));
    }

    private void moveCards(DefaultListModel<Card> from, DefaultListModel<Card> to, int pos, List<Card> cards) {
        if (from == to) {
            throw new IllegalArgumentException("from deck cannot be to deck");
        }

        if (!cards.isEmpty()) {
            int selHand = getPlayerNumberOfShowingHand();
            if (pos < 0 && to != null) {
                pos = to.getSize();
            }

            for (int i = 0; i < cards.size(); ++i) {
                Card c = cards.get(i);

                from.removeElement(c);

                c.faceUp = (deck != to);
                c.drawDivider = (to == play && i == cards.size() - 1);
                if (play != to) {
                    c.owner = 0;
                } else {
                    c.owner = selHand;
                }

                if (to != null) {
                    to.add(pos, c);
                }
            }

            if (to != null) {
                JList t = modelToList(to);
                ListSelectionModel m = t.getSelectionModel();
                m.clearSelection();
                int start = pos;
                int end = pos + cards.size() - 1;
                m.addSelectionInterval(start, end);
                t.ensureIndexIsVisible(end);
                t.ensureIndexIsVisible(start);
                showCard((Card) to.get(start));
            }
        }

        deckLabel.setText(string("vdeck-l-deck", deck.size()));
        playLabel.setText(string("vdeck-l-in-play", play.getSize()));
        discardLabel.setText(string("vdeck-l-discard", discard.size()));
    }

    private JList<Card> modelToList(DefaultListModel<Card> m) {
        if (m == deck) {
            return deckList;
        }
        if (m == play) {
            return playList;
        }
        if (m == discard) {
            return discardList;
        }
        throw new AssertionError("unknown model " + m);
    }

    private DefaultListModel<Card> listToModel(JList li) {
        if (li == deckList) {
            return deck;
        }
        if (li == playList) {
            return play;
        }
        if (li == discardList) {
            return discard;
        }
        throw new AssertionError("unknown list " + li);
    }

    private void showCard(Card c) {
        viewer.setSheet(c == null ? null : c.getFrontSheet());
    }

    @Override
    public void dispose() {
        if (ownerPicker != null) {
            ownerPicker.dispose();
        }
        Settings.getUser().storeWindowSettings("virtual-deck", this);
        super.dispose();
    }

    private SheetViewer viewer = new SheetViewer();

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        handGroup = new javax.swing.ButtonGroup();
        closeBtn = new javax.swing.JButton();
        jSplitPane2 = new javax.swing.JSplitPane();
        deckAndPlayOuterContainer = new javax.swing.JPanel();
        deckPanel = new javax.swing.JPanel();
        deckLabel = new javax.swing.JLabel();
        deckScroll = new javax.swing.JScrollPane();
        deckList = new javax.swing.JList<>();
        jPanel2 = new javax.swing.JPanel();
        drawLabel = new javax.swing.JLabel();
        draw4 = new javax.swing.JButton();
        draw1 = new javax.swing.JButton();
        draw2 = new javax.swing.JButton();
        draw5 = new javax.swing.JButton();
        draw6 = new javax.swing.JButton();
        draw3 = new javax.swing.JButton();
        spacerDL = new javax.swing.JLabel();
        spacerDR = new javax.swing.JLabel();
        handPanel = new javax.swing.JPanel();
        showHandPanel = new javax.swing.JPanel();
        hand5 = new javax.swing.JToggleButton();
        hand3 = new javax.swing.JToggleButton();
        hand8 = new javax.swing.JToggleButton();
        hand6 = new javax.swing.JToggleButton();
        handAll = new javax.swing.JToggleButton();
        hand4 = new javax.swing.JToggleButton();
        hand1 = new javax.swing.JToggleButton();
        hand7 = new javax.swing.JToggleButton();
        hand2 = new javax.swing.JToggleButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        handLabel = new javax.swing.JLabel();
        listContainer = new javax.swing.JPanel();
        playPanel = new javax.swing.JPanel();
        playToBottom = new javax.swing.JButton();
        playToDraw = new javax.swing.JButton();
        drawToPlay = new javax.swing.JButton();
        spacerHV = new javax.swing.JLabel();
        moveUpDownPanel = new javax.swing.JPanel();
        playToDiscard = new javax.swing.JButton();
        discardToPlay = new javax.swing.JButton();
        discardPanel = new javax.swing.JPanel();
        drawToDiscard = new javax.swing.JButton();
        discardToDraw = new javax.swing.JButton();
        discardToBottom = new javax.swing.JButton();
        spacerDV = new javax.swing.JLabel();
        playScroll = new javax.swing.JScrollPane();
        playList = new javax.swing.JList<>();
        discardScroll = new javax.swing.JScrollPane();
        discardList = new javax.swing.JList<>();
        discardLabel = new javax.swing.JLabel();
        playLabel = new javax.swing.JLabel();
        midColumnSpacer = new javax.swing.JLabel();
        sheetPanel = new javax.swing.JPanel();
        overlayPanel = new ca.cgjennings.apps.arkham.dialog.OverlayPanel();
        shuffleBtn = new javax.swing.JButton();
        resetBtn = new javax.swing.JButton();
        jTip1 = new ca.cgjennings.ui.JTip();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "pa-vdeck" )); // NOI18N

        closeBtn.setText(string( "close" )); // NOI18N
        closeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeBtnActionPerformed(evt);
            }
        });

        jSplitPane2.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        jSplitPane2.setOneTouchExpandable(true);

        deckPanel.setLayout(new java.awt.GridBagLayout());

        deckLabel.setBackground(java.awt.Color.black);
        deckLabel.setFont(deckLabel.getFont().deriveFont(deckLabel.getFont().getStyle() | java.awt.Font.BOLD));
        deckLabel.setForeground(java.awt.Color.white);
        deckLabel.setIcon(downArrow);
        deckLabel.setText(string( "vdeck-l-deck" )); // NOI18N
        deckLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
        deckLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        deckPanel.add(deckLabel, gridBagConstraints);

        deckScroll.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, java.awt.Color.gray));
        deckScroll.setPreferredSize(new java.awt.Dimension(200, 0));

        deckList.setFont(deckList.getFont().deriveFont(deckList.getFont().getSize()-1f));
        deckList.setModel( deck );
        deckList.setCellRenderer( renderer );
        deckList.setDragEnabled(true);
        deckList.setDropMode(javax.swing.DropMode.INSERT);
        deckList.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                deckListMouseDragged(evt);
            }
        });
        deckList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                deckListMouseReleased(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                deckListMousePressed(evt);
            }
        });
        deckScroll.setViewportView(deckList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        deckPanel.add(deckScroll, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        drawLabel.setFont(drawLabel.getFont().deriveFont(drawLabel.getFont().getSize()-1f));
        drawLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        drawLabel.setText(string( "vdeck-l-draw" )); // NOI18N
        drawLabel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 18, 3, 18);
        jPanel2.add(drawLabel, gridBagConstraints);

        draw4.setText("4");
        draw4.setBorder(selected);
        draw4.setBorderPainted(false);
        draw4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                draw4ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        jPanel2.add(draw4, gridBagConstraints);

        draw1.setMnemonic('d');
        draw1.setText("1");
        draw1.setBorder(selected);
        draw1.setBorderPainted(false);
        draw1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                draw1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 10, 4, 0);
        jPanel2.add(draw1, gridBagConstraints);

        draw2.setText("2");
        draw2.setBorder(selected);
        draw2.setBorderPainted(false);
        draw2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                draw2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        jPanel2.add(draw2, gridBagConstraints);

        draw5.setText("5");
        draw5.setBorder(selected);
        draw5.setBorderPainted(false);
        draw5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                draw5ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        jPanel2.add(draw5, gridBagConstraints);

        draw6.setText("6");
        draw6.setBorder(selected);
        draw6.setBorderPainted(false);
        draw6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                draw6ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 10);
        jPanel2.add(draw6, gridBagConstraints);

        draw3.setText("3");
        draw3.setBorder(selected);
        draw3.setBorderPainted(false);
        draw3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                draw3ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        jPanel2.add(draw3, gridBagConstraints);

        spacerDL.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(spacerDL, gridBagConstraints);

        spacerDR.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(spacerDR, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        deckPanel.add(jPanel2, gridBagConstraints);

        handPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        handPanel.setLayout(new java.awt.GridBagLayout());

        showHandPanel.setLayout(new java.awt.GridBagLayout());

        handGroup.add(hand5);
        hand5.setMnemonic('5');
        hand5.setText("5");
        hand5.setBorder( unselected );
        hand5.setBorderPainted(false);
        hand5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        showHandPanel.add(hand5, gridBagConstraints);

        handGroup.add(hand3);
        hand3.setMnemonic('3');
        hand3.setText("3");
        hand3.setBorder( unselected );
        hand3.setBorderPainted(false);
        hand3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        showHandPanel.add(hand3, gridBagConstraints);

        handGroup.add(hand8);
        hand8.setMnemonic('8');
        hand8.setText("8");
        hand8.setBorder( unselected );
        hand8.setBorderPainted(false);
        hand8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 10);
        showHandPanel.add(hand8, gridBagConstraints);

        handGroup.add(hand6);
        hand6.setMnemonic('6');
        hand6.setText("6");
        hand6.setBorder( unselected );
        hand6.setBorderPainted(false);
        hand6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        showHandPanel.add(hand6, gridBagConstraints);

        handGroup.add(handAll);
        handAll.setMnemonic('0');
        handAll.setSelected(true);
        handAll.setText("*");
        handAll.setBorder(selected );
        handAll.setBorderPainted(false);
        handAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 10, 4, 0);
        showHandPanel.add(handAll, gridBagConstraints);

        handGroup.add(hand4);
        hand4.setMnemonic('4');
        hand4.setText("4");
        hand4.setBorder( unselected );
        hand4.setBorderPainted(false);
        hand4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        showHandPanel.add(hand4, gridBagConstraints);

        handGroup.add(hand1);
        hand1.setMnemonic('1');
        hand1.setText("1");
        hand1.setBorder( unselected );
        hand1.setBorderPainted(false);
        hand1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        showHandPanel.add(hand1, gridBagConstraints);

        handGroup.add(hand7);
        hand7.setMnemonic('7');
        hand7.setText("7");
        hand7.setBorder( unselected );
        hand7.setBorderPainted(false);
        hand7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        showHandPanel.add(hand7, gridBagConstraints);

        handGroup.add(hand2);
        hand2.setMnemonic('2');
        hand2.setText("2");
        hand2.setBorder( unselected );
        hand2.setBorderPainted(false);
        hand2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                handActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(1, 2, 4, 0);
        showHandPanel.add(hand2, gridBagConstraints);

        jLabel1.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        showHandPanel.add(jLabel1, gridBagConstraints);

        jLabel2.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        showHandPanel.add(jLabel2, gridBagConstraints);

        handLabel.setFont(handLabel.getFont().deriveFont(handLabel.getFont().getSize()-1f));
        handLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        handLabel.setText(string( "vdeck-l-hand" )); // NOI18N
        handLabel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 18, 3, 18);
        showHandPanel.add(handLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        handPanel.add(showHandPanel, gridBagConstraints);

        playPanel.setLayout(new java.awt.GridBagLayout());

        playToBottom.setIcon( ResourceKit.getIcon( "ui/button/return-to-deck.png" ) );
        playToBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playToBottomActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 12, 4);
        playPanel.add(playToBottom, gridBagConstraints);

        playToDraw.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playToDrawActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 0, 4);
        playPanel.add(playToDraw, gridBagConstraints);

        drawToPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawToPlayActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 4, 0, 4);
        playPanel.add(drawToPlay, gridBagConstraints);

        spacerHV.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        playPanel.add(spacerHV, gridBagConstraints);

        moveUpDownPanel.setLayout(new java.awt.GridBagLayout());

        playToDiscard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playToDiscardActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 6, 4, 6);
        moveUpDownPanel.add(playToDiscard, gridBagConstraints);

        discardToPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discardToPlayActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 6);
        moveUpDownPanel.add(discardToPlay, gridBagConstraints);

        discardPanel.setLayout(new java.awt.GridBagLayout());

        drawToDiscard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawToDiscardActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 4, 0, 4);
        discardPanel.add(drawToDiscard, gridBagConstraints);

        discardToDraw.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discardToDrawActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 0, 4);
        discardPanel.add(discardToDraw, gridBagConstraints);

        discardToBottom.setIcon( ResourceKit.getIcon( "ui/button/return-to-deck.png" ) );
        discardToBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discardToBottomActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 4, 12, 4);
        discardPanel.add(discardToBottom, gridBagConstraints);

        spacerDV.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        discardPanel.add(spacerDV, gridBagConstraints);

        playScroll.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 1, 0, java.awt.Color.gray));
        playScroll.setPreferredSize(new java.awt.Dimension(200, 0));

        playList.setFont(playList.getFont().deriveFont(playList.getFont().getSize()-1f));
        playList.setModel( play );
        playList.setCellRenderer( renderer );
        playList.setDragEnabled(true);
        playList.setDropMode(javax.swing.DropMode.INSERT);
        playList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                listMousePressed(evt);
            }
        });
        playList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listValueChanged(evt);
            }
        });
        playScroll.setViewportView(playList);

        discardScroll.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, java.awt.Color.gray));
        discardScroll.setPreferredSize(new java.awt.Dimension(200, 0));

        discardList.setFont(discardList.getFont().deriveFont(discardList.getFont().getSize()-1f));
        discardList.setModel( discard );
        discardList.setCellRenderer( renderer );
        discardList.setDragEnabled(true);
        discardList.setDropMode(javax.swing.DropMode.INSERT);
        discardList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                discardListMousePressed(evt);
            }
        });
        discardList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listValueChanged(evt);
            }
        });
        discardScroll.setViewportView(discardList);

        discardLabel.setBackground(java.awt.Color.black);
        discardLabel.setFont(discardLabel.getFont().deriveFont(discardLabel.getFont().getStyle() | java.awt.Font.BOLD));
        discardLabel.setForeground(java.awt.Color.white);
        discardLabel.setIcon(downArrow);
        discardLabel.setText(string( "vdeck-l-discard" )); // NOI18N
        discardLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
        discardLabel.setOpaque(true);

        playLabel.setBackground(java.awt.Color.black);
        playLabel.setFont(playLabel.getFont().deriveFont(playLabel.getFont().getStyle() | java.awt.Font.BOLD));
        playLabel.setForeground(java.awt.Color.white);
        playLabel.setIcon(downArrow);
        playLabel.setText(string( "vdeck-l-in-play" )); // NOI18N
        playLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
        playLabel.setOpaque(true);

        javax.swing.GroupLayout listContainerLayout = new javax.swing.GroupLayout(listContainer);
        listContainer.setLayout(listContainerLayout);
        listContainerLayout.setHorizontalGroup(
            listContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(listContainerLayout.createSequentialGroup()
                .addComponent(playPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(listContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(playScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 369, Short.MAX_VALUE)
                    .addComponent(playLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 369, Short.MAX_VALUE)))
            .addGroup(listContainerLayout.createSequentialGroup()
                .addComponent(discardPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(listContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(discardScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 369, Short.MAX_VALUE)
                    .addComponent(discardLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 369, Short.MAX_VALUE)
                    .addComponent(moveUpDownPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 369, Short.MAX_VALUE)))
        );
        listContainerLayout.setVerticalGroup(
            listContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(listContainerLayout.createSequentialGroup()
                .addGroup(listContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, listContainerLayout.createSequentialGroup()
                        .addComponent(playLabel)
                        .addGap(0, 0, 0)
                        .addComponent(playScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE))
                    .addComponent(playPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE))
                .addGap(0, 0, 0)
                .addComponent(moveUpDownPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(listContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(discardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, listContainerLayout.createSequentialGroup()
                        .addComponent(discardLabel)
                        .addGap(0, 0, 0)
                        .addComponent(discardScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE))))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        handPanel.add(listContainer, gridBagConstraints);

        midColumnSpacer.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 52;
        gridBagConstraints.ipady = 8;
        handPanel.add(midColumnSpacer, gridBagConstraints);

        javax.swing.GroupLayout deckAndPlayOuterContainerLayout = new javax.swing.GroupLayout(deckAndPlayOuterContainer);
        deckAndPlayOuterContainer.setLayout(deckAndPlayOuterContainerLayout);
        deckAndPlayOuterContainerLayout.setHorizontalGroup(
            deckAndPlayOuterContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deckAndPlayOuterContainerLayout.createSequentialGroup()
                .addComponent(deckPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(handPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE))
        );
        deckAndPlayOuterContainerLayout.setVerticalGroup(
            deckAndPlayOuterContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(deckPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(handPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jSplitPane2.setLeftComponent(deckAndPlayOuterContainer);

        sheetPanel.setMinimumSize(new java.awt.Dimension(100, 2));
        sheetPanel.setPreferredSize(new java.awt.Dimension(387, 416));
        sheetPanel.setLayout(new java.awt.BorderLayout());
        jSplitPane2.setRightComponent(sheetPanel);

        ca.cgjennings.ui.ArcBorder arcBorder1 = new ca.cgjennings.ui.ArcBorder();
        arcBorder1.setThickness(1);
        overlayPanel.setBorder(arcBorder1);

        shuffleBtn.setFont(shuffleBtn.getFont().deriveFont(shuffleBtn.getFont().getSize()-1f));
        shuffleBtn.setText(string( "vdeck-b-shuffle" )); // NOI18N
        shuffleBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shuffleBtnActionPerformed(evt);
            }
        });

        resetBtn.setFont(resetBtn.getFont().deriveFont(resetBtn.getFont().getSize()-1f));
        resetBtn.setText(string( "vdeck-b-return-all" )); // NOI18N
        resetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout overlayPanelLayout = new javax.swing.GroupLayout(overlayPanel);
        overlayPanel.setLayout(overlayPanelLayout);
        overlayPanelLayout.setHorizontalGroup(
            overlayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overlayPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(shuffleBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(resetBtn)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        overlayPanelLayout.setVerticalGroup(
            overlayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, overlayPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(overlayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(shuffleBtn)
                    .addComponent(resetBtn))
                .addContainerGap())
        );

        jTip1.setTipText(string("vdeck-l-peek")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(overlayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTip1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 503, Short.MAX_VALUE)
                .addComponent(closeBtn)
                .addContainerGap())
            .addComponent(jSplitPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jSplitPane2)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(overlayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(closeBtn)
                            .addComponent(jTip1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())))
        );

        setSize(new java.awt.Dimension(916, 579));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

	private void drawToPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawToPlayActionPerformed
            moveCards(deck, play, 0, deckList.getSelectedValuesList());
	}//GEN-LAST:event_drawToPlayActionPerformed

	private void drawToDiscardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawToDiscardActionPerformed
            moveCards(deck, discard, 0, deckList.getSelectedValuesList());
	}//GEN-LAST:event_drawToDiscardActionPerformed

	private void playToDrawActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playToDrawActionPerformed
            moveCards(play, deck, 0, playList.getSelectedValuesList());
	}//GEN-LAST:event_playToDrawActionPerformed

	private void playToBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playToBottomActionPerformed
            moveCards(play, deck, -1, playList.getSelectedValuesList());
	}//GEN-LAST:event_playToBottomActionPerformed

	private void discardToDrawActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discardToDrawActionPerformed
            moveCards(discard, deck, 0, discardList.getSelectedValuesList());
	}//GEN-LAST:event_discardToDrawActionPerformed

	private void discardToBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discardToBottomActionPerformed
            moveCards(discard, deck, -1, discardList.getSelectedValuesList());
	}//GEN-LAST:event_discardToBottomActionPerformed

	private void playToDiscardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playToDiscardActionPerformed
            moveCards(play, discard, 0, playList.getSelectedValuesList());
	}//GEN-LAST:event_playToDiscardActionPerformed

	private void discardToPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discardToPlayActionPerformed
            moveCards(discard, play, 0, discardList.getSelectedValuesList());
	}//GEN-LAST:event_discardToPlayActionPerformed

	private void draw1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_draw1ActionPerformed
            if (deck.getSize() < 1) {
                getToolkit().beep();
            } else {
                moveCards(deck, play, 0, top(deck, 1));
            }
	}//GEN-LAST:event_draw1ActionPerformed

	private void draw2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_draw2ActionPerformed
            if (deck.getSize() < 2) {
                draw1ActionPerformed(evt);
            } else {
                moveCards(deck, play, 0, top(deck, 2));
            }
	}//GEN-LAST:event_draw2ActionPerformed

	private void draw3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_draw3ActionPerformed
            if (deck.getSize() < 3) {
                draw2ActionPerformed(evt);
            } else {
                moveCards(deck, play, 0, top(deck, 3));
            }
	}//GEN-LAST:event_draw3ActionPerformed

    private List<Card> top(DefaultListModel<Card> model, int count) {
        count = Math.min(count, model.getSize());
        ArrayList<Card> top = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            top.add(model.get(i));
        }
        return top;
    }

	private void shuffleBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shuffleBtnActionPerformed
            deckList.clearSelection();
            for (int i = 0; i < deck.getSize(); ++i) {
                final int j = rng.nextInt(deck.getSize());
                final Card a = deck.get(i);
                final Card b = deck.get(j);
                deck.set(i, b);
                deck.set(j, a);
            }
	}//GEN-LAST:event_shuffleBtnActionPerformed
    private Random rng = new Random();

    private void resetModelToDeck(DefaultListModel m) {
        while (m.getSize() > 0) {
            Card c = (Card) m.remove(m.getSize() - 1);
            c.owner = NOBODY;
            c.faceUp = false;
            c.drawDivider = false;
            deck.addElement(c);
        }
    }

	private void resetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetBtnActionPerformed
            resetModelToDeck(discard);
            for (int i = 0; i < playStacks.length; ++i) {
                resetModelToDeck(playStacks[i]);
            }
            shuffleBtnActionPerformed(null);
            // update deck size label
            moveCards(deck, play, 0, Collections.emptyList());
	}//GEN-LAST:event_resetBtnActionPerformed

	private void closeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeBtnActionPerformed
            dispose();
	}//GEN-LAST:event_closeBtnActionPerformed

    private boolean isPeek(MouseEvent e) {
        return e.getButton() == MouseEvent.BUTTON2 || (e.getButton() == MouseEvent.BUTTON3 && e.isShiftDown());
    }

    private static final int PEEK_BUTTON = MouseEvent.BUTTON3;
    private int peekedAtCard = -1;

	private void deckListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deckListMouseReleased
            if (isPeek(evt)) {
                peekAt(-1);
            }
	}//GEN-LAST:event_deckListMouseReleased

	private void deckListMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deckListMousePressed
            if (isPeek(evt)) {
                peekAt(getListIndex(deckList, evt));
            }
	}//GEN-LAST:event_deckListMousePressed

	private void deckListMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deckListMouseDragged
            if (peekedAtCard != -1) {
                peekAt(getListIndex(deckList, evt));
            }
	}//GEN-LAST:event_deckListMouseDragged

	private void draw4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_draw4ActionPerformed
            if (deck.getSize() < 4) {
                draw3ActionPerformed(evt);
            } else {
                moveCards(deck, play, 0, top(deck, 4));
            }
	}//GEN-LAST:event_draw4ActionPerformed

	private void draw5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_draw5ActionPerformed
            if (deck.getSize() < 5) {
                draw4ActionPerformed(evt);
            } else {
                moveCards(deck, play, 0, top(deck, 5));
            }
	}//GEN-LAST:event_draw5ActionPerformed

	private void draw6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_draw6ActionPerformed
            if (deck.getSize() < 6) {
                draw5ActionPerformed(evt);
            } else {
                moveCards(deck, play, 0, top(deck, 6));
            }
	}//GEN-LAST:event_draw6ActionPerformed

	private void handActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_handActionPerformed
            playSelections[selectedHand] = playList.getSelectedIndices();
            playScrollPos[selectedHand] = playScroll.getVerticalScrollBar().getValue();

            for (int i = 0; i < hands.length; ++i) {
                if (hands[i].isSelected()) {
                    hands[i].setBorder(selected);
                    playList.setModel(playStacks[i]);
                    play = playStacks[i];
                    selectedHand = i;
                    if (playSelections[i] != null) {
                        playList.setSelectedIndices(playSelections[i]);
                    }
                    final int fi = i;
                    EventQueue.invokeLater(() -> {
                        playScroll.getVerticalScrollBar().setValue(playScrollPos[fi]);
                    });
                } else {
                    hands[i].setBorder(unselected);
                }
            }
	}//GEN-LAST:event_handActionPerformed
    private int selectedHand = 0;

    private int getPlayerNumberOfShowingHand() {
        return selectedHand;
    }

	private void listMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listMousePressed
            if (evt == null) {
                throw new AssertionError("needs true event");
            }

            @SuppressWarnings("unchecked")
            JList<Card> sourceList = (JList<Card>) evt.getSource();

            if (evt.getButton() == MouseEvent.BUTTON3) {
                // if nothing selected, select under cursor
                int i = sourceList.locationToIndex(evt.getPoint());
                if (i < 0) {
                    return;
                }
                if (sourceList.getSelectedIndex() < 0) {
                    sourceList.setSelectedIndex(i);
                }
                Point sp = sourceList.getLocationOnScreen();
                if (sp == null) {
                    return;
                }
                Rectangle r = sourceList.getCellBounds(i, i);
                r.x += sp.x + r.getWidth() / 2;
                r.y += sp.y + r.getHeight() / 2;

                List<Card> cardList = sourceList.getSelectedValuesList();
                Card[] cards = cardList.toArray(new Card[0]);
                if (ownerPicker.setOwner(cards, r.x, r.y)) {
                    int newOwner = cards[0].owner;
                    // "Other" uses same list as "Nobody"
                    if (newOwner == OTHER) {
                        newOwner = NOBODY;
                    }
                    int showingOwner = getPlayerNumberOfShowingHand();
                    if (showingOwner != newOwner) {
                        for (int c = 0; c < cards.length; ++c) {
                            playStacks[showingOwner].removeElement(cards[c]);
                            playStacks[newOwner].add(0, cards[c]);
                        }
                    }
                }
                playList.repaint();
            } else if (evt.getButton() == MouseEvent.BUTTON1) {
                listValueChangedImpl(sourceList);
            }
	}//GEN-LAST:event_listMousePressed

    private void listValueChangedImpl(Object list) {
        JList li = (JList) list;
        showCard((Card) li.getSelectedValue());
    }

	private void listValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listValueChanged
            listValueChangedImpl(evt.getSource());
	}//GEN-LAST:event_listValueChanged

	private void discardListMousePressed( java.awt.event.MouseEvent evt ) {//GEN-FIRST:event_discardListMousePressed
            listValueChangedImpl(evt.getSource());
	}//GEN-LAST:event_discardListMousePressed

    private void peekAt(int index) {
        if (index == peekedAtCard) {
            return;
        }
        if (peekedAtCard >= 0) {
            Card c = (Card) deck.get(peekedAtCard);
            c.faceUp = false;
            redrawIndex(deckList, peekedAtCard);
            showCard(null);
        }
        if (index >= 0) {
            Card c = (Card) deck.get(index);
            c.faceUp = true;
            redrawIndex(deckList, index);
            showCard(c);
        }
        peekedAtCard = index;
    }

    private int getListIndex(JList list, MouseEvent evt) {
        Point p = evt.getPoint();
        if (p == null) {
            return -1;
        }
        return list.locationToIndex(p);
    }

    private void redrawIndex(JList list, int i) {
        Rectangle r = list.getCellBounds(i, i);
        if (r != null) {
            list.repaint(r);
        }
    }

    class Card implements IconProvider, Cloneable {

        public GameComponent gc;
        public Sheet[] sheets;
        public String name;
        public boolean faceUp;
        public int owner;
        public Icon thumb;
        public boolean drawDivider = false;

        public Card(GameComponent gc) {
            this.gc = gc;
            sheets = gc.getSheets();
            if (sheets == null) {
                sheets = gc.createDefaultSheets();
            }

            name = ResourceKit.makeStringFileSafe(gc.getFullName());
            thumb = createThumbnail(sheets[0]);
            owner = 0;
            faceUp = false;
        }

        @Override
        public Card clone() {
            try {
                return (Card) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        protected BufferedImage createImage(Sheet sh) {
            return sh.paint(RenderTarget.PREVIEW, VIEW_RES);
        }

        protected Icon createThumbnail(Sheet sh) {
            return ImageUtilities.createIconForSize(createImage(sh), THUMB_SIZE);
        }

        public Icon createBackThumbnail() {
            if (sheets.length == 1) {
                return thumb;
            } else {
                return createThumbnail(sheets[1]);
            }
        }

        public Sheet getFrontSheet() {
            return sheets[0];
        }

        private static final int THUMB_SIZE = 48;
        private static final int VIEW_RES = 200;

        @Override
        public Icon getIcon() {
            return thumb;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final int NOBODY = 0;
    private static final int OTHER = 9;

    String[] owners = new String[]{string("vdeck-b-ownerless").replace("&", ""), "1", "2", "3", "4", "5", "6", "7", "8", string("vdeck-b-other").replace("&", "")};

    private Border spacingBorder = BorderFactory.createEmptyBorder(1, 4, 1, 4);
    private Border selectionBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.ORANGE, 1),
            BorderFactory.createLineBorder(Color.YELLOW, 1)
    );
    private Border unselectedBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);

    private Border selected = BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                    selectionBorder, BorderFactory.createLoweredBevelBorder()
            ),
            spacingBorder
    );
    private Border unselected = BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                    unselectedBorder, BorderFactory.createRaisedBevelBorder()
            ),
            spacingBorder
    );

    private Icon downArrow;

    private void createArrowIcons() {
        BufferedImage bi = new BufferedImage(11, 7, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setPaint(Color.BLACK);
            g.fillRect(0, 0, 11, 7);
            g.setPaint(Color.WHITE);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.fillPolygon(new int[]{1, 5, 9}, new int[]{1, 5, 1}, 3);
        } finally {
            g.dispose();
        }
        downArrow = new ImageIcon(bi);

        JUtilities.setIconPair(playToDraw, "ui/button/left.png", "ui/button/left-hi.png", false);
        JUtilities.setIconPair(drawToPlay, "ui/button/right.png", "ui/button/right-hi.png", false);
        JUtilities.setIconPair(playToDiscard, "ui/button/down.png", "ui/button/down-hi.png", false);
        JUtilities.setIconPair(discardToPlay, "ui/button/up.png", "ui/button/up-hi.png", false);
        JUtilities.setIconPair(discardToDraw, "ui/button/left.png", "ui/button/left-hi.png", false);
        JUtilities.setIconPair(drawToDiscard, "ui/button/right.png", "ui/button/right-hi.png", false);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeBtn;
    private javax.swing.JPanel deckAndPlayOuterContainer;
    private javax.swing.JLabel deckLabel;
    private javax.swing.JList<Card> deckList;
    private javax.swing.JPanel deckPanel;
    private javax.swing.JScrollPane deckScroll;
    private javax.swing.JLabel discardLabel;
    private javax.swing.JList<Card> discardList;
    private javax.swing.JPanel discardPanel;
    private javax.swing.JScrollPane discardScroll;
    private javax.swing.JButton discardToBottom;
    private javax.swing.JButton discardToDraw;
    private javax.swing.JButton discardToPlay;
    private javax.swing.JButton draw1;
    private javax.swing.JButton draw2;
    private javax.swing.JButton draw3;
    private javax.swing.JButton draw4;
    private javax.swing.JButton draw5;
    private javax.swing.JButton draw6;
    private javax.swing.JLabel drawLabel;
    private javax.swing.JButton drawToDiscard;
    private javax.swing.JButton drawToPlay;
    private javax.swing.JToggleButton hand1;
    private javax.swing.JToggleButton hand2;
    private javax.swing.JToggleButton hand3;
    private javax.swing.JToggleButton hand4;
    private javax.swing.JToggleButton hand5;
    private javax.swing.JToggleButton hand6;
    private javax.swing.JToggleButton hand7;
    private javax.swing.JToggleButton hand8;
    private javax.swing.JToggleButton handAll;
    private javax.swing.ButtonGroup handGroup;
    private javax.swing.JLabel handLabel;
    private javax.swing.JPanel handPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSplitPane jSplitPane2;
    private ca.cgjennings.ui.JTip jTip1;
    private javax.swing.JPanel listContainer;
    private javax.swing.JLabel midColumnSpacer;
    private javax.swing.JPanel moveUpDownPanel;
    private ca.cgjennings.apps.arkham.dialog.OverlayPanel overlayPanel;
    private javax.swing.JLabel playLabel;
    private javax.swing.JList<Card> playList;
    private javax.swing.JPanel playPanel;
    private javax.swing.JScrollPane playScroll;
    private javax.swing.JButton playToBottom;
    private javax.swing.JButton playToDiscard;
    private javax.swing.JButton playToDraw;
    private javax.swing.JButton resetBtn;
    private javax.swing.JPanel sheetPanel;
    private javax.swing.JPanel showHandPanel;
    private javax.swing.JButton shuffleBtn;
    private javax.swing.JLabel spacerDL;
    private javax.swing.JLabel spacerDR;
    private javax.swing.JLabel spacerDV;
    private javax.swing.JLabel spacerHV;
    // End of variables declaration//GEN-END:variables

    private ListCellRenderer renderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Card card = (Card) value;
            if (card.faceUp) {
                setText(card.name + " (" + owners[card.owner] + ")");
                setIcon(card.thumb);
            } else {
                setText("??? (" + owners[card.owner] + ")");
                setIcon(backIcon);
            }
            return this;
        }
    };

    private TransferHandler dragHandler = new TransferHandler() {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            @SuppressWarnings("unchecked")
            JList<Card> li = (JList<Card>) c;
            return new CardTransferable(listToModel(li), li.getSelectedValuesList());
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            // cards will be deleted from source list during import
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDataFlavorSupported(cardFlavor)) {
                return false;
            }
            CardTransferData t = getData(support);
            return t.instance == VirtualDeckDialog.this;
        }

        @Override
        public boolean importData(TransferSupport support) {
            @SuppressWarnings("unchecked")
            JList<Card> target = (JList<Card>) support.getComponent();
            CardTransferData t = getData(support);
            DefaultListModel<Card> dest = listToModel(target);
            int index = ((JList.DropLocation) support.getDropLocation()).getIndex();
            if (index < 0) {
                index = dest.getSize();
            }
            moveCards(t.source, dest, index, t.cards);
            return true;
        }

        private CardTransferData getData(TransferSupport support) {
            CardTransferData t = null;
            try {
                t = (CardTransferData) support.getTransferable().getTransferData(cardFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                throw new AssertionError();
            }
            return t;
        }
    };

    private class CardTransferable implements Transferable {

        private final CardTransferData data;

        public CardTransferable(DefaultListModel<Card> source, List<Card> cards) {
            data = new CardTransferData(VirtualDeckDialog.this, source, cards);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!flavor.equals(cardFlavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{cardFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(cardFlavor);
        }
    }

    private static class CardTransferData {

        VirtualDeckDialog instance;
        DefaultListModel<Card> source;
        Card[] cards;

        public CardTransferData(VirtualDeckDialog instance, DefaultListModel<Card> source, List<Card> cards) {
            this.instance = instance;
            this.source = source;
            this.cards = cards.toArray(new Card[0]);
        }
    };

    private DataFlavor cardFlavor = new DataFlavor(CardTransferData.class, DataFlavor.javaJVMLocalObjectMimeType);
}
