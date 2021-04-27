package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.anim.Animation;
import ca.cgjennings.ui.anim.AnimationUtilities;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.RootPaneContainer;
import javax.swing.Timer;
import javax.swing.UIManager;
import se.datadosen.component.RiverLayout;

/**
 * Dialog used to display messages posted to a queue.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class MessageDialog extends javax.swing.JDialog {

    public MessageDialog(Frame owner, Messenger.Message message) {
        super(owner);
        init(message);
    }

    public MessageDialog(Dialog owner, Messenger.Message message) {
        super(owner);
        init(message);
    }

    public MessageDialog(Window owner, Messenger.Message message) {
        super(owner);
        init(message);
    }

    private void init(Messenger.Message m) {
        initComponents();
        Color bg = UIManager.getColor(Theme.MESSAGE_BACKGROUND);
        borderContainer.setBackground(bg);
        messagePanel.setBackground(bg);

        final boolean modeless = !m.isDialogStyle();

        if (modeless) {
            setFocusableWindowState(false);
            getRootPane().putClientProperty("Window.shadow", Boolean.FALSE);
        }

        if (m.icon == null) {
            iconLabel.setVisible(false);
        } else {
            iconLabel.setIcon(m.icon);
        }

        int y = 0;
        messagePanel.setLayout(new RiverLayout(0, 2));
        if (m.message != null) {
            for (String s : m.message) {
                if (s == null) {
                    s = " ";
                }
                messagePanel.add(new JLabel(s), messagePanel.getComponentCount() > 0 ? "br" : "");
            }
        }
        if (m.inlineComponents != null) {
            for (JComponent c : m.inlineComponents) {
                if (c == null) {
                    continue;
                }
                messagePanel.add(c, messagePanel.getComponentCount() > 0 ? "br" : "");
            }
        }
        pack();
        updateWindowLocation();

        // N.B. Need to track this so it can be removed on dispose(), to avoid a leak
        Container parent = getParent();
        parentMovementListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateWindowLocation();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                updateWindowLocation();
            }
        };
        parent.addComponentListener(parentMovementListener);

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                hasHadFocus = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (hasHadFocus && !modeless) {
                    setVisible(false);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!disposing) {
                    if (fadeOutTimer != null) {
                        fadeOutTimer.stop();
                        fadeOutTimer = null;
                    } else {
                        new Animation(0.000001f).play(MessageDialog.this);
                        EventQueue.invokeLater(() -> {
                            StyleUtilities.setWindowOpacity(MessageDialog.this, OPACITY);
                        });
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!disposing && modeless) {
                    setVisible(false);
                }
            }
        });

        addToQueue(this);
    }

    private ComponentAdapter parentMovementListener;

    private static final int MAX_LEN = 75;

    private void updateWindowLocation() {
        Container parent = getParent();
        Point screenLoc = parent.getLocationOnScreen();
        int x = (parent.getWidth() - getWidth()) / 2;
        int y = 0;
        if (parent instanceof RootPaneContainer) {
            y += ((RootPaneContainer) parent).getContentPane().getY();
        }
        setLocation(x + screenLoc.x, y + screenLoc.y + parent.getInsets().top);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        borderContainer = new javax.swing.JPanel();
        iconLabel = new javax.swing.JLabel();
        messagePanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setUndecorated(true);

        borderContainer.setBackground( UIManager.getColor( Theme.MESSAGE_BACKGROUND ) );
        borderContainer.setBorder( UIManager.getBorder( Theme.MESSAGE_BORDER_INFORMATION ) );
        borderContainer.setLayout(new java.awt.BorderLayout());

        iconLabel.setBackground(java.awt.Color.white);
        iconLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        iconLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        iconLabel.setOpaque(true);
        borderContainer.add(iconLabel, java.awt.BorderLayout.LINE_START);

        messagePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        messagePanel.setLayout(null);
        borderContainer.add(messagePanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(borderContainer, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void setVisible(boolean b) {
        if (b) {
            if (disposing || isVisible()) {
                return;
            }
            StyleUtilities.setWindowOpacity(this, 1f / 255f);
            final int TARGET_HEIGHT = getHeight();
            setSize(getWidth(), 1);
            final Animation FADE_IN = new Animation(0.2f) {
                @Override
                public void composeFrame(float position) {
                    StyleUtilities.setWindowOpacity(MessageDialog.this, position * OPACITY);
                    setSize(getWidth(), Math.max(1, (int) (position * TARGET_HEIGHT + 0.5f)));
                }
            };
            FADE_IN.play(this);
            final Animation FADE_OUT = new Animation(10f) {
                @Override
                public void composeFrame(float position) {
                    StyleUtilities.setWindowOpacity(MessageDialog.this, OPACITY - (position * OPACITY));
                    if (position == 1f) {
                        dispose();
                        setVisible(false);
                    }
                }
            };
            fadeOutTimer = new Timer(4000, (ActionEvent e) -> {
                if (!disposing) {
                    fadeOutTimer = null;
                    FADE_OUT.play();
                }
            });
            fadeOutTimer.setRepeats(false);
            fadeOutTimer.start();
            super.setVisible(true);
        } else {
            if (fadeOutTimer != null) {
                fadeOutTimer.stop();
                fadeOutTimer = null;
            }
            if (disposing) {
                super.setVisible(false);
            } else {
                disposing = true;
                float opacity = StyleUtilities.getWindowOpacity(this);
                AnimationUtilities.animateOpacityTransition(this, opacity, 0f, (opacity / OPACITY) * 0.2f, true);
            }
        }
    }
    private boolean disposing;
    private Timer fadeOutTimer;
    private boolean hasHadFocus;
    private static final float OPACITY = 0.9f;

    @Override
    public void dispose() {
//		if( !disposing ) {
        removeFromQueue(this);
        getParent().removeComponentListener(parentMovementListener);
        disposing = true;
//		}
        super.dispose();
    }

    private static void addToQueue(MessageDialog w) {
        Container p = w.getParent();
        LinkedList<MessageDialog> queue = queueMap.get(p);
        if (queue == null) {
            queue = new LinkedList<>();
            queueMap.put(p, queue);
        }
        queue.add(w);
        if (queue.size() == 1 && Messenger.isQueueProcessingEnabled()) {
            w.setVisible(true);
        }
    }

    private static void removeFromQueue(MessageDialog w) {
        Container p = w.getParent();
        LinkedList<MessageDialog> queue = queueMap.get(p);
        if (queue != null && queue.get(0) == w) {
            queue.removeFirst();
            if (queue.isEmpty()) {
                queueMap.remove(p);
            } else if (Messenger.isQueueProcessingEnabled()) {
                queue.get(0).setVisible(true);
            }
        } else {
            StrangeEons.log.severe("no queue for parent window");
        }
    }

    static void restartQueues() {
        for (LinkedList<MessageDialog> queue : queueMap.values()) {
            if (queue != null && !queue.isEmpty()) {
                MessageDialog dlg = queue.get(0);
                if (!dlg.disposing && !dlg.isVisible()) {
                    dlg.setVisible(true);
                }
            }
        }
    }

    private static HashMap<Container, LinkedList<MessageDialog>> queueMap = new HashMap<>();

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel borderContainer;
    private javax.swing.JLabel iconLabel;
    private javax.swing.JPanel messagePanel;
    // End of variables declaration//GEN-END:variables
}
