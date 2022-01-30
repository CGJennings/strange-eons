package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.AbstractToggleCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.deck.DeckEditor;
import ca.cgjennings.apps.arkham.deck.PageView;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.ui.textedit.CodeType;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.math.Interpolation;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.anim.Animation;
import ca.cgjennings.ui.anim.AnimationUtilities;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import ca.cgjennings.ui.textedit.Tokenizer;
import ca.cgjennings.ui.textedit.tokenizers.CSSTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.HTMLTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.JavaScriptTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.JavaTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.PlainTextTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.PropertyTokenizer;
import ca.cgjennings.ui.theme.Theme;
import gamedata.Game;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;

/**
 * A small floating tool bar that tracks the focused component and shows buttons
 * relevant to that context. To create new buttons that the user can add to the
 * context bar, implement the {@link Button} class (typically by subclassing
 * {@link AbstractButton} or {@link CommandButton}), and register the new button
 * with {@link #registerButton}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class ContextBar {

    private static final Color BAR_BACKGROUND;
    private static final Color BAR_FOREGROUND;
    private static final Color BUTTON_BACKGROUND;
    private static final Color BUTTON_ROLLOVER_BACKGROUND;
    private static final Color BUTTON_ROLLOVER_FOREGROUND;
    private static final Color BUTTON_ARMED_FOREGROUND;
    // for toggle buttons; generated automatically
    private static final Color BUTTON_SELECTED_BACKGROUND;

    static {
        UIDefaults uid = UIManager.getDefaults();
        BAR_BACKGROUND = uid.getColor(Theme.CONTEXT_BAR_BACKGROUND);
        BAR_FOREGROUND = uid.getColor(Theme.CONTEXT_BAR_FOREGROUND);
        BUTTON_BACKGROUND = uid.getColor(Theme.CONTEXT_BAR_BUTTON_BACKGROUND);
        BUTTON_ROLLOVER_BACKGROUND = uid.getColor(Theme.CONTEXT_BAR_BUTTON_ROLLOVER_BACKGROUND);
        BUTTON_ROLLOVER_FOREGROUND = uid.getColor(Theme.CONTEXT_BAR_BUTTON_ROLLOVER_OUTLINE_FOREGROUND);
        BUTTON_ARMED_FOREGROUND = uid.getColor(Theme.CONTEXT_BAR_BUTTON_ARMED_OUTLINE_FOREGROUND);

        final Color BASE = BAR_BACKGROUND;
        int bri = (BASE.getRed() + BASE.getGreen() + BASE.getBlue()) / 3;
        if (bri >= 32) {
            BUTTON_SELECTED_BACKGROUND = BASE.darker();
        } else {
            BUTTON_SELECTED_BACKGROUND = BASE.brighter();
        }
    }

    private static final float LOW_ALPHA = 0.25f;
    private static final float HIGH_ALPHA = 0.95f;
    private static final float ANIM_TIME = 0.33f;

    private static final Insets BUTTON_MARGIN = new java.awt.Insets(1, 4, 1, 4);
    private static final Border FIRST_BORDER = BorderFactory.createMatteBorder(1, 1, 1, 6, BAR_BACKGROUND);
    private static final Border INTERNAL_BORDER = new ButtonBorder(2, 1, 2, 1);
    private static final Border TAIL_BORDER = BorderFactory.createEmptyBorder(2, 4, 0, 0);

    private static final Icon SEPARATOR = ResourceKit.getIcon("toolbar/separator-live.png");

    @SuppressWarnings("serial")
    private static final class ButtonBorder extends MatteBorder {

        public ButtonBorder(int t, int l, int b, int r) {
            super(t, l, b, r, BAR_BACKGROUND);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            super.paintBorder(c, g, x, y, width, height);
            if (!(c instanceof javax.swing.AbstractButton)) {
                return;
            }
            javax.swing.AbstractButton btn = (javax.swing.AbstractButton) c;

            getBorderInsets(c, insets);
            if (c.getBackground() == BUTTON_ROLLOVER_BACKGROUND) {
                final int ICON_SIZE = getPreferredContextBarIconSize();
                int x1 = insets.left - 1;
                int y1 = insets.top - 1;
                int x2 = x1 + ICON_SIZE + 1;
                int y2 = y1 + ICON_SIZE + 1;

                Color outline;
                if (btn.getModel().isArmed()) {
                    outline = BUTTON_ARMED_FOREGROUND;
                } else {
                    outline = BUTTON_ROLLOVER_FOREGROUND;
                }
                g.setColor(outline);

                g.drawLine(x1 + 1, y1, x2 - 1, y1); // top
                g.drawLine(x1, y1 + 1, x1, y2 - 1); // left
                g.drawLine(x1 + 1, y2, x2 - 1, y2); // bottom
                g.drawLine(x2, y1 + 1, x2, y2 - 1); // right
            }
        }
        private Insets insets = new Insets(0, 0, 0, 0);
    }

    private static final int UPDATE_DELAY = 1_000 / 8;

    private JWindow window;
    private Button[] buttons;
    private JComponent[] peers;

    /**
     * Creates a new context bar.
     */
    public ContextBar() {
        // create the collpaser button; this is reused even if the window is regenerated
        showHideBtn = new JToggleButton();
        final Icon showIcon = ResourceKit.getIcon("toolbar/show.png");
        final Icon hideIcon = ResourceKit.getIcon("toolbar/hide.png");
        final Icon showHiIcon = ResourceKit.getIcon("toolbar/show-hi.png");
        final Icon hideHiIcon = ResourceKit.getIcon("toolbar/hide-hi.png");

        showHideBtn.setIcon(hideIcon);
        showHideBtn.setRolloverIcon(hideHiIcon);
        showHideBtn.setSelectedIcon(showIcon);
        showHideBtn.setRolloverSelectedIcon(showHiIcon);
        showHideBtn.addActionListener((ActionEvent e) -> {
            autoexpandTimer.stop();
            updateButtonStates();
        });
        showHideBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
        showHideBtn.setBorder(FIRST_BORDER);
        showHideBtn.setContentAreaFilled(false);
        showHideBtn.addMouseListener(rolloverListener);

        setButtons(fromButtonDescription(
                Settings.getShared().get(BUTTONS_SETTING, DEFAULT_BUTTON_LIST), true
        ));
    }

    /**
     * Sets the buttons included on the context bar.
     *
     * @param buttons an array of Button objects; use {@code null} to insert a
     * separator
     */
    public void setButtons(Button... buttons) {
        this.buttons = buttons.clone();
        peers = null;
        if (window != null) {
            installButtons();
            updateButtonStates();
            if (window.isVisible()) {
                repack();
            } else {
                window.pack();
            }
        }
    }

    /**
     * Returns a copy of the installed buttons.
     *
     * @return an array containing the buttons currently included on the context
     * bar
     */
    public Button[] getButtons() {
        return buttons.clone();
    }

    /**
     * Creates an array of buttons from a string description. The description
     * consist of button IDs separated by commas. A pipe character may be used
     * instead of an ID to insert a separator.
     *
     * @param buttonDesc a string in the format described above
     * @param skipInvalidButtons if {@code true}, invalid IDs will be skipped
     * @return an array of buttons; {@code null} will be used to indicate
     * separators
     * @throws IllegalArgumentException if the format was invalid and
     * {@code skipInvalidButtons} is {@code false}
     */
    public static Button[] fromButtonDescription(String buttonDesc, boolean skipInvalidButtons) {
        if (buttonDesc == null) {
            return new Button[0];
        }
        LinkedList<Button> parsed = new LinkedList<>();
        String[] tokens = buttonDesc.trim().split("\\s*,\\s*", -1);
        for (int i = 0; i < tokens.length; ++i) {
            if (tokens[i].equals("|")) {
                parsed.add(null);
            } else {
                Button b = getButton(tokens[i].trim());
                if (b == null) {
                    if (!skipInvalidButtons) {
                        throw new IllegalArgumentException("invalid format: " + buttonDesc);
                    }
                } else {
                    parsed.add(b);
                }
            }
        }
        return parsed.toArray(new Button[0]);
    }

    /**
     * Returns a string that describes the button layout defined by the
     * specified buttons.
     *
     * @param buttons the array of buttons to convert to a string description
     * @return a string of button IDs representing the current button layout
     */
    public static String toButtonDescription(Button[] buttons) {
        if (buttons == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < buttons.length; ++i) {
            if (b.length() > 0) {
                b.append(',');
            }
            if (buttons[i] == null) {
                b.append('|');
            } else {
                b.append(buttons[i].getID());
            }
        }
        return b.toString();
    }

    /**
     * Returns the context bar's current context information.
     *
     * @return the bar's context
     */
    public Context getContext() {
        if (target == null) {
            return null;
        }
        if (cachedContext == null || cachedContext.target != target) {
            cachedContext = new Context(this, target);
        }
        return cachedContext;
    }
    private Context cachedContext;

    private JComponent determineTarget() {
        JComponent newTarget = null;

        // Try to identify a target for the context bar:
        //
        // 1. First we check if the *current* markup target is null.
        // In this case, no markup field currently has focus and thus
        // we can check for special non-markup targets.
        //
        // 2. If no special target is valid, then we will look at the
        // most recently valid (i.e. non-null) markup target.
        //
        // 3. Once we have found a target, no matter the source, we perform
        // some additional validation like checking if it has a client
        // property to explicitly disable the context bar set on it.
        // 1. check for special targets
        if (StrangeEons.getApplication().getCurrentMarkupTarget() == null) {
            StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
            if (ed instanceof DeckEditor) {
                DeckEditor de = (DeckEditor) ed;
                PageView pv = de.getDeck().getActivePage().getView();
                newTarget = pv;
            }
        }

        // 2. try the last valid markup target
        if (newTarget == null) {
            final MarkupTarget mt = StrangeEons.getApplication().getMarkupTarget();
            if (mt != null) {
                newTarget = (JComponent) mt.getTarget();
            }
        }

        // 3. additional validation before accepting the target
        if (newTarget != null) {
            if (!newTarget.isShowing() || barDisabledInAncestor(newTarget)) {
                newTarget = null;
            } else {
                // trace up the target's parent tree and:
                //    if the parent is an editor, clear the target if it is not
                //    the selected editor
                Container parent = newTarget.getParent();
                while (newTarget != null && parent != null) {
                    if (parent instanceof StrangeEonsEditor) {
                        if (parent != StrangeEons.getWindow().getActiveEditor()) {
                            newTarget = null;
                        }
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        }

        return newTarget;
    }

    private static boolean barDisabledInAncestor(Component parent) {
        while (parent != null && (parent instanceof JComponent)) {
            if (Boolean.TRUE.equals(((JComponent) parent).getClientProperty(BAR_DISABLE_PROPERTY))) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void synchronizeWithTarget() {
        JComponent newtarget = determineTarget();

        // If the editor has been de/re-attached, and it has a different parent window
        // we need to recreate the toolbar so it is a child of the same window;
        // that ensures that it stays on top of the relevant window.
        // Note that install() will eventually call synchronizeWithTarget(), but
        // this time the parent window will be correct and target will be null
        // (since the bar was uninstalled), and so control will pass to the
        // final case below.
        if (newtarget != null) {
            if (JUtilities.findWindow(newtarget) != window.getParent()) {
                uninstall();
                install();
                return;
            }
        }

        // if the target control is the same as last time this was called
        // (and that control is in the same window; see above), then all we
        // need to do is make sure that it is still in the correct location
        // (in case the control has been moved/resized)
        if (newtarget == target) {
            if (target != null) {
                updateButtonStates();
                reposition();
            }
            return;
        }

        // The target control has changed since last time: the new target
        // is either null (no valid target), or another control in the same
        // parent window. Either take the window down (null) or reposition
        // for the new control.
        if (target != null) {
            target.removeKeyListener(collapseKeyListener);
            fireOnDetach();
        }
        target = newtarget;
        fireOnAttach();

        if (newtarget != null) {
            target.addKeyListener(collapseKeyListener);
            updateButtonStates();
            reposition();
            if (!window.isVisible()) {
                window.setVisible(true);
            }
        } else {
            window.dispose();
        }
    }
    private JComponent target;

    /**
     * Returns the standard, shared instance.
     *
     * @return the standard tool bar
     */
    public static ContextBar getShared() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("not in EDT");
        }
        if (shared == null) {
            shared = new ContextBar();
        }
        return shared;
    }
    private static ContextBar shared;

    /**
     * Enables or disabled the tool bar.
     *
     * @param enable if {@code true} the tool bar will be created and shown;
     * otherwise it will be hidden and associated resources will be freed
     */
    public void setEnabled(boolean enable) {
        if (enable) {
            if (!isEnabled()) {
                install();
            }
        } else {
            if (isEnabled()) {
                uninstall();
            }
        }
    }

    /**
     * Returns {@code true} if the tool bar is currently enabled. When the tool
     * bar is enabled, it will automatically pop up over the current markup
     * target.
     *
     * @return {@code true} if the tool bar is enabled
     */
    public boolean isEnabled() {
        return window != null;
    }

    /**
     * Sets whether the tool bar is collapsed so that only the expand button is
     * visible.
     *
     * @param collapsed if {@code true}, collapse the tool bar
     */
    public void setCollapsed(boolean collapsed) {
        if (collapsed != isCollapsed()) {
            showHideBtn.doClick(0);
        }
    }

    /**
     * Returns {@code true} if the tool bar is currently collapsed (only the
     * button that expands the tool bar is visible).
     *
     * @return {@code true} if the tool bar is collapsed
     */
    public boolean isCollapsed() {
        return showHideBtn.isSelected();
    }

    /**
     * Creates and installs the tool bar. Called as needed by
     * {@link #setDisplayed}.
     */
    private void install() {
        if (window != null) {
            return;
        }
        createWindow();
        installButtons();
        window.pack();
        synchronizeWithTarget();

        markupTargetListener = (PropertyChangeEvent evt) -> {
            synchronizeWithTarget();
        };
        StrangeEons.getApplication().addPropertyChangeListener(StrangeEons.MARKUP_TARGET_PROPERTY, markupTargetListener);
        markupTargetTimer = new Timer(UPDATE_DELAY, (ActionEvent e) -> {
            synchronizeWithTarget();
        });
        markupTargetTimer.start();
    }

    private PropertyChangeListener markupTargetListener;
    private Timer markupTargetTimer;

    /**
     * Disposes of the tool bar. Called as needed by {@link #setDisplayed}.
     */
    private void uninstall() {
        if (window == null) {
            return;
        }

        if (target != null) {
            target.removeKeyListener(collapseKeyListener);
            fireOnDetach();
            cachedContext = null; // to prevent a leak
        }

        if (markupTargetTimer != null) {
            markupTargetTimer.stop();
            markupTargetTimer = null;
        }

        StrangeEons.getApplication().removePropertyChangeListener(StrangeEons.MARKUP_TARGET_PROPERTY, markupTargetListener);
        markupTargetListener = null;

        window.dispose();
        window.removeMouseListener(rolloverListener);
        window.getParent().removeComponentListener(windowPositionListener);
        window.removeAll();
        if (peers != null) {
            for (int i = 0; i < peers.length; ++i) {
                peers[i].removeMouseListener(rolloverListener);
            }
        }
        firstRepack = true;
        autoexpandTimer.stop();
        peers = null;
        window = null;
        target = null;
    }

    private void createWindow() {
        // We need to figure out the correct parent window; typically the
        // app frame but it could also be a detached editor window or dialog;
        // when no target is available we default to the app frame since it
        // is known to exist by the time the context bar is first created.
        Window parent = StrangeEons.getWindow();
        JComponent currentTarget = determineTarget();
        if (currentTarget != null) {
            parent = JUtilities.findWindow(currentTarget);
            if (parent == null) {
                parent = StrangeEons.getWindow();
                StrangeEons.log.warning("null parent window for target component");
            }
        }

        window = new JWindow(parent);
        window.setFocusableWindowState(false);
        Container c = window.getContentPane();
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            jc.setBackground(BAR_BACKGROUND);
            jc.setBorder(BorderFactory.createLineBorder(BAR_FOREGROUND, 1));
        }

        c.setLayout(new BoxLayout(c, BoxLayout.LINE_AXIS));

        isLowTransparency = true;
        StyleUtilities.setWindowOpacity(window, LOW_ALPHA);
        window.getContentPane().addMouseListener(rolloverListener);
        parent.addComponentListener(windowPositionListener);
    }

    private void updateButtonStates() {
        if (window == null || buttons == null) {
            return;
        }

        boolean repack = false;
        boolean collapsed = isCollapsed();
        boolean hasHadVisibleComponentSinceLastSeparator = false;
        boolean someButtonIsClickableIfUncollapsed = false;
        Context context = getContext();
        for (int i = 0; i < buttons.length; ++i) {
            boolean visible;
            if (buttons[i] != null) {
                // regular button
                visible = context == null ? true : buttons[i].isVisibleInCurrentContext(context);
                if (visible) {
                    hasHadVisibleComponentSinceLastSeparator = true;
                }

                boolean enabled = visible && context != null && buttons[i].isEnabledInCurrentContext(context);
                if (peers[i].isEnabled() != enabled) {
                    peers[i].setEnabled(enabled);
                }
            } else {
                // separator
                visible = hasHadVisibleComponentSinceLastSeparator;
                hasHadVisibleComponentSinceLastSeparator = false;
            }

            if (visible && peers[i].isEnabled() && buttons[i] != null) {
                someButtonIsClickableIfUncollapsed = true;
            }
            if (collapsed) {
                visible = false;
            }

            if (peers[i].isVisible() != visible) {
                peers[i].setVisible(visible);
                if (i > 0 && !(peers[i - 1] instanceof ButtonPeer)) {
                    peers[i - 1].setVisible(visible);
                }
                repack = true;
            }
        }

        // find the last (rightmost) visible control and hide it if its a separator
        for (int i = buttons.length - 1; i >= 0; --i) {
            if (peers[i].isVisible()) {
                if (buttons[i] == null) {
                    peers[i].setVisible(false);
                    repack = true;
                }
                break;
            }
        }

        // if all buttons are hidden or disabled, and the bar is not disabled,
        // hide the context bar
        if (someButtonIsClickableIfUncollapsed != window.isVisible()) {
            window.setVisible(someButtonIsClickableIfUncollapsed);
        }

        // if any visibility states have changed, the toolbar needs resizing
        if (repack) {
            repack();
        }
    }

    private void repack() {
        if (firstRepack) {
            firstRepack = false;
            window.pack();
            return;
        }

        final Window windowToPack = window;
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                // window might have been uninstalled in the interim
                if (window != windowToPack) {
                    return;
                }

                final Dimension curSize = window.getSize();
                final Dimension newSize = window.getPreferredSize();
                if (!curSize.equals(newSize)) {
                    Animation anim = new Animation(ANIM_TIME / 3f) {
                        @Override
                        public void composeFrame(float position) {
                            if (window != windowToPack) {
                                stop();
                                return;
                            }
                            if (position > 0f) {
                                int width = Interpolation.lerp(position, curSize.width, newSize.width);
                                window.setSize(width, newSize.height);
                                reposition();
                            }
                        }
                    };
                    anim.play(this);
                }
            }
        });
    }
    private boolean firstRepack = true;

    private void reposition() {
        JComponent target = this.target;

        if (window == null || target == null || !window.isShowing() || !target.isShowing()) {
            return;
        }

        int x, y;

        // determine which side to place the bar on
        boolean right = true, above = true, outside = true;
        if (target.getClientProperty(BAR_INSIDE_PROPERTY) == Boolean.TRUE) {
            outside = false;
        }
        if (target.getClientProperty(BAR_BELOW_PROPERTY) == Boolean.TRUE) {
            above = false;
        }
        if (target.getClientProperty(BAR_LEADING_SIDE_PROPERTY) == Boolean.TRUE) {
            right = false;
        }
        if (!target.getComponentOrientation().isLeftToRight()) {
            right = !right;
        }
        if (window.getWidth() > target.getWidth()) {
            right = !right;
        }

        // if this control is in a scroll pane, use the scroll pane to
        // determine the window location ("target" is a local copy of the
        // member variable, so the true target component does not change)
        boolean isInScrollPane = false;
        Container parent = target.getParent();
        if (parent instanceof JViewport) {
            parent = parent.getParent();
        }
        if (parent instanceof JScrollPane) {
            target = (JScrollPane) parent;
            isInScrollPane = true;
        }

        Point sp = target.getLocationOnScreen();
        int tw = target.getWidth();
        int th = target.getHeight();
        alignmentInsets = target.getInsets(alignmentInsets);

        Object customLocator = target.getClientProperty(BAR_CUSTOM_LOCATION_PROPERTY);
        if (customLocator instanceof Locator) {
            Rectangle customRect = ((Locator) customLocator).getContextBarRectangle(this, window.getWidth(), window.getHeight(), target);
            if (customRect != null) {
                sp.x = customRect.x;
                sp.y = customRect.y;
                tw = customRect.width;
                th = customRect.height;
            }
        }

        // calculate the bar position
        if (above) {
            if (outside) {
                y = sp.y - window.getHeight();// + alignmentInsets.top;
            } else {
                y = sp.y + alignmentInsets.top;
            }
        } else {
            if (outside) {
                y = sp.y + tw;// - alignmentInsets.bottom;
            } else {
                y = sp.y + th - window.getHeight() - alignmentInsets.bottom;
            }
        }

        if (right) {
            x = sp.x + tw - window.getWidth() - alignmentInsets.right;
            if (!outside || isInScrollPane) {
                x -= getScrollBarWidth(target, true);
            }
        } else {
            x = sp.x + alignmentInsets.left;
            if (!outside || isInScrollPane) {
                x += getScrollBarWidth(target, false);
            }
        }

        Object point = target.getClientProperty(BAR_OFFSET_PROPERTY);
        if (point instanceof Point) {
            Point delta = (Point) point;
            x += delta.x;
            y += delta.y;
        }

        window.setLocation(x, y);
    }
    private Insets alignmentInsets;

    private static int getScrollBarWidth(JComponent target, boolean barOnRight) {
        int scrollBarSize = 0;
        if (target instanceof JScrollPane) {
            JScrollPane scp = (JScrollPane) target;
            JScrollBar bar = scp.getVerticalScrollBar();
            if (bar.isVisible() && barOnRight == scp.getComponentOrientation().isLeftToRight()) {
                scrollBarSize = bar.getWidth();
            }
        }
        return scrollBarSize;
    }

    /**
     * Adjusts the tool bar transparency and button rollover highlights.
     */
    private MouseAdapter rolloverListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            updateTransparency();
            if (e.getSource() instanceof javax.swing.AbstractButton) {
                activeBtn = ((javax.swing.AbstractButton) e.getSource());
                if (activeBtn.isEnabled()) {
                    activeBtn.setBackground(BUTTON_ROLLOVER_BACKGROUND);
                }
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            updateTransparency();
            if (e == null) {
                if (activeBtn != null) {
                    activeBtn.setBackground(BUTTON_BACKGROUND);
                    activeBtn = null;
                }
            } else if (e.getSource() instanceof javax.swing.AbstractButton) {
                ((javax.swing.AbstractButton) e.getSource()).setBackground(BUTTON_BACKGROUND);
            }
        }
        private javax.swing.AbstractButton activeBtn;
    };

    private ComponentListener windowPositionListener = new ComponentAdapter() {
        @Override
        public void componentMoved(ComponentEvent e) {
            synchronizeWithTarget();
        }

        @Override
        public void componentResized(ComponentEvent e) {
            synchronizeWithTarget();
        }
    };

    private KeyListener collapseKeyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if ((e.getModifiersEx() & (SHIFT_MASK | NOT_SHIFT_MASK)) == SHIFT_MASK) {
                    e.consume();
                    showHideBtn.doClick();
                }
            }
        }
    };
    private static final int SHIFT_MASK = KeyEvent.SHIFT_DOWN_MASK;
    private static final int NOT_SHIFT_MASK = KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK | KeyEvent.ALT_GRAPH_DOWN_MASK;

    private void fireOnAttach() {
        if (target == null || buttons == null) {
            return;
        }
        Context c = getContext();
        for (Button b : buttons) {
            if (b != null) {
                b.onAttach(c);
            }
        }
    }

    private void fireOnDetach() {
        if (target == null || buttons == null) {
            return;
        }
        Context c = getContext();
        for (Button b : buttons) {
            if (b != null) {
                b.onDetach(c);
            }
        }
    }

    /**
     * Called to update the tool bar's transparency.
     */
    private void updateTransparency() {
        // returns null if mouse not over window (else a Point)
        final boolean low = window.getContentPane().getMousePosition(true) == null;
        pointerIsOverBar = !low;

        if (isLowTransparency != low) {
            isLowTransparency = low;

            if (isCollapsed()) {
                if (low) {
                    autoexpandTimer.stop();
                } else {
                    autoexpandTimer.start();
                }
            }

            if (LOW_ALPHA != HIGH_ALPHA) {
                if (low) {
                    AnimationUtilities.animateOpacityTransition(window, -1f, LOW_ALPHA, ANIM_TIME, false);
                } else {
                    AnimationUtilities.animateOpacityTransition(window, -1f, HIGH_ALPHA, ANIM_TIME, false);
                }
            }
        }
    }
    private boolean isLowTransparency;

    /**
     * Returns {@code true} if the pointer is currently over the context bar.
     *
     * @return {@code true} if there is a pointer on this platform and it is
     * over the context bar
     */
    public boolean isPointerOverBar() {
        return pointerIsOverBar;
    }
    private boolean pointerIsOverBar;

    private Timer autoexpandTimer = new Timer(750, (evt) -> {
        if (window != null && isCollapsed()) {
            setCollapsed(false);
        }
    });

    {
        autoexpandTimer.setRepeats(false);
    }

    private static Icon coerceIconSize(Icon i) {
        final int ICON_SIZE = getPreferredContextBarIconSize();
        if (i != null) {
            if (i.getIconWidth() != ICON_SIZE || i.getIconHeight() != ICON_SIZE) {
                BufferedImage bi = ImageUtilities.iconToImage(i);
                bi = ImageUtilities.trim(bi);
                i = ImageUtilities.createIconForSize(bi, ICON_SIZE);
            }
        } else {
            StrangeEons.log.log(Level.WARNING, "using blank icon for null context bar button icon");
            i = new BlankIcon(ICON_SIZE, ICON_SIZE);
        }
        return i;
    }

    @SuppressWarnings("serial")
    private class ButtonPeer extends JButton {

        private Button b;

        public ButtonPeer(Button b) {
            if (b == null) {
                throw new NullPointerException("b");
            }
            this.b = b;
            if (b instanceof CommandButton) {
                CommandButton cb = (CommandButton) b;
                if (cb.command instanceof AbstractToggleCommand) {
                    cb.peer = this;
                }
            }
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(BUTTON_BACKGROUND);
            setToolTipText(b.getName());
            setIcon(coerceIconSize(b.getIcon()));
            setMargin(BUTTON_MARGIN);
            addMouseListener(rolloverListener);
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            // this sends a fake mouse exit event to clear the highlight;
            // if a dialog opens then the tool bar never gets a mouse exited event
//			rolloverListener.mouseExited( new MouseEvent( this, -1, -1L, 0, -1, -1, 0, false ) );
            super.fireActionPerformed(event);

            JComponent source = target;
            if (source != null) {
                ActionEvent ae = new ActionEvent(source, event.getID(), event.getActionCommand(), event.getModifiers());
                b.actionPerformed(event);
            }
        }
    }

    private void installButtons() {
        peers = new JComponent[buttons.length];
        window.getContentPane().removeAll();

        addShowHideButton();
        if (buttons != null) {
            for (int i = 0; i < buttons.length; ++i) {
                if (buttons[i] == null) {
                    JLabel spacer = new JLabel(SEPARATOR);
                    peers[i] = spacer;
                    window.getContentPane().add(spacer);
                } else {
                    peers[i] = new ButtonPeer(buttons[i]);
                    addPeer((ButtonPeer) peers[i], i == buttons.length - 1);
                }
            }
        }
        window.getContentPane().add(tailSpacer);
    }

    private void addShowHideButton() {
        window.getContentPane().add(showHideBtn);
    }

    private JLabel tailSpacer = new JLabel();

    {
        tailSpacer.setBorder(TAIL_BORDER);
    }

    private void addPeer(ButtonPeer b, boolean isLast) {
        b.setBorder(INTERNAL_BORDER);
        window.getContentPane().add(b);
    }

    private JToggleButton showHideBtn;

    /**
     * Adds a new kind of button that can be displayed on the tool bar. Whether
     * or not the button is actually shown will depend on whether the user adds
     * it to the tool bar.
     *
     * @param button the button template to register
     * @throws NullPointerException if the button or its ID is {@code null}
     * @throws IllegalArgumentException if the ID contains any invalid
     * characters
     */
    public static void registerButton(Button button) {
        if (button == null) {
            throw new NullPointerException("button");
        }

        String id = button.getID();
        if (id == null) {
            throw new NullPointerException("button.getID()");
        }
        if (templates.containsKey(id)) {
            throw new IllegalArgumentException("ID already registered: " + id);
        }
        for (int i = 0; i < id.length(); ++i) {
            if (!Character.isJavaIdentifierPart(id.charAt(i))) {
                throw new IllegalArgumentException("invalid ID: " + id);
            }
        }
        templates.put(id, button);
    }

    /**
     * Removes a previously registered button by its ID. Has no effect if no
     * such button is registered.
     *
     * @param id the ID of the button to remove
     */
    public static void unregisterButton(final String id) {
        final Button b = templates.get(id);
        if (b != null) {
            unregisterButton(b);
        }
    }

    /**
     * Removes a previously registered button.
     *
     * @param button the button to remove from the registry
     * @throws NullPointerException if the button or its ID is {@code null}
     * @see #registerButton
     */
    public static void unregisterButton(Button button) {
        if (button == null) {
            throw new NullPointerException("button");
        }
        if (button.getID() == null) {
            throw new NullPointerException("button.getID()");
        }

        templates.remove(button.getID());

        // if the context bar has been created, reset the button layout;
        // only do this on unregistration or the invalid button filtering
        // can lose buttons that are not registered yet
        if (shared != null) {
            ContextBar bar = getShared();
            String desc = ContextBar.toButtonDescription(bar.getButtons());
            bar.setButtons(ContextBar.fromButtonDescription(desc, true));
        }
    }

    /**
     * Returns the registered button with the given ID, or {@code null} if there
     * is no such button.
     *
     * @param id the button ID to match
     * @return the button with the requested ID, if registered
     * @throws NullPointerException if {@code id} is null
     */
    public static Button getButton(String id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        return templates.get(id);
    }

    /**
     * Returns an array of the registered buttons.
     *
     * @return the registered buttons
     */
    public static Button[] getRegisteredButtons() {
        int i = 0;
        Button[] btns = new Button[templates.size()];
        for (String key : templates.keySet()) {
            btns[i++] = templates.get(key);
        }
        return btns;
    }

    /**
     * Returns the ideal icon size for context bar icons. Icons larger or
     * smaller than this value (in either width or height) will be resized
     * automatically. This preferred icon size is fixed for any given run of the
     * application, but may change between runs. For example, the size may
     * change if a different {@link Theme} is active.
     *
     * @return the width and height of context bar icons
     */
    public static int getPreferredContextBarIconSize() {
        return 16;
    }

    private static final LinkedHashMap<String, Button> templates = new LinkedHashMap<>();

    /**
     * This class represents a buttons that can appear on the context bar.
     * Plug-ins can register new kinds of buttons, and then the user can add
     * these buttons to the context bar in the preferences dialog.
     */
    public static interface Button extends ActionListener, IconProvider {

        /**
         * Returns a unique ID for this button, such as "BOLD". Only letters,
         * digits, underscores, and currency symbols may appear in an ID.
         *
         * @return a short ID that will identify this button in the application
         * preferences
         */
        String getID();

        /**
         * Returns the button's name, or {@code null}.
         *
         * @return a localized description of the button's function
         */
        String getName();

        /**
         * Returns the button's icon.
         *
         * @return a small transparent icon used to represent the button
         */
        @Override
        Icon getIcon();

        /**
         * This method is called to notify the button when the tool bar is about
         * to be attached to a new target component (the component that it
         * floats over and that its commands normally apply to).
         *
         * @param context information about the current context
         */
        void onAttach(Context context);

        /**
         * This method is called to notify the button when the tool bar is about
         * to be detached from the current target component (the component that
         * it floats over and that its commands normally apply to).
         *
         * @param context information about the current context
         */
        void onDetach(Context context);

        /**
         * Returns {@code true} if the button should be enabled in the current
         * context.
         *
         * @param context information about the current context
         * @return {@code true} if the button should be enabled (ignored if not
         * visible)
         */
        boolean isEnabledInCurrentContext(Context context);

        /**
         * Returns {@code true} if the button should be visible in the current
         * context. A button should be invisible if it can never be enabled in
         * the current context, and disabled if it is not currently usable due
         * to a temporary restriction. For example, any text field can be copied
         * from if there is an active selection, so a copy button should always
         * be visible but should be enabled only if there is a selection. On the
         * other hand, a command that only works for script code should be
         * visible only when editing a script.
         *
         * @param context information about the current context
         * @return {@code true} if the button should be visible
         */
        boolean isVisibleInCurrentContext(Context context);
    }

    //
    //
    // PUBLIC HELPER CLASSES FOR CREATING BUTTONS
    //
    //
    /**
     * An abstract base class for implementing a context bar button. The base
     * class implements the following behaviour:
     * <ul>
     * <li> {@link #getID()}, {@link #getName()}, and {@link #getIcon()} will
     * provide an ID, name, and icon (respectively) based on the values passed
     * to the constructor
     * <li> the button is always enabled and visible
     * ({@link #isEnabledInCurrentContext} and
     * {@link #isVisibleInCurrentContext} always return {@code true})
     * </ul>
     * <p>
     * At a minimum, subclasses must implement
     * {@link #actionPerformed(java.awt.event.ActionEvent)} in order to execute
     * an action when the button is pressed.
     */
    public static abstract class AbstractButton implements Button {

        private String id;
        private String name;
        private Icon icon;

        /**
         * Creates a new button the specified id, name, and with an icon loaded
         * from the specified resource. The icon image is loaded using
         * {@link ResourceKit#getIcon(java.lang.String)}, except that if the
         * icon resource does not contain a slash, the resource will be loaded
         * from {@code resources/icons/toolbar}.
         *
         * @param id a unique button identifier
         * @param name the user-friendly name of the button
         * @param iconResource the resource to load the button's icon from
         * @throws NullPointerException if the name or icon resource is
         * {@code null}
         */
        public AbstractButton(String id, String name, String iconResource) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (iconResource == null) {
                throw new NullPointerException("iconResource");
            }
            this.id = id;
            this.name = name;

            if (iconResource.indexOf('/') < 0) {
                iconResource = "toolbar/" + iconResource;
            }
            icon = ResourceKit.getIcon(iconResource);
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        @Override
        public void onAttach(Context context) {
        }

        @Override
        public void onDetach(Context context) {
        }

        @Override
        public boolean isEnabledInCurrentContext(Context context) {
            return true;
        }

        @Override
        public boolean isVisibleInCurrentContext(Context context) {
            return true;
        }

        /**
         * Returns a string representation of the button suitable for debugging.
         * The returned string is <b>Button&lt;<i>id</i>&gt;</b>, where
         * <i>id</i>
         * is the button's ID as returned by {@link Button#getID()}.
         *
         * @return a string representation of the button
         */
        @Override
        public String toString() {
            return "Button<" + getID() + '>';
        }
    }

    /**
     * A context bar button that delegates to an {@link AbstractCommand}. The
     * button will activate the command when clicked, and be enabled or disabled
     * based on the current state of the command. The base class implementation
     * will create a button that is always visible and performs no action when
     * the context bar is attached or detached.
     */
    public static class CommandButton implements Button {

        private String id;
        private AbstractCommand command;
        private boolean autohide;

        /**
         * Creates a new {@code CommandButton} for the specified command. The
         * button's unique ID will be determined by reading the command's
         * {@link AbstractCommand#BUTTON_ID_KEY} key.
         *
         * @param command the command to generate a button for
         * @throws NullPointerException if the command is {@code null} or the
         * command does not define a button ID value
         */
        public CommandButton(AbstractCommand command) {
            this(null, command);
        }

        /**
         * Creates a new {@code CommandButton} for the specified command. The
         * command will use the specified ID.
         *
         * @param command the command to generate a button for
         * @param id the unique ID to use for the button
         * @throws NullPointerException if the command is {@code null}
         */
        public CommandButton(String id, AbstractCommand command) {
            if (command == null) {
                throw new NullPointerException("command");
            }
            if (id == null) {
                id = (String) command.getValue(AbstractCommand.BUTTON_ID_KEY);
                if (id == null) {
                    throw new IllegalArgumentException("command does not define a button ID");
                }
            }
            this.command = command;
            this.id = id;
        }

        /**
         * Returns the command that this button is a proxy for.
         *
         * @return the command used to create this button
         */
        public final AbstractCommand getCommand() {
            return command;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String getName() {
            update();
            return command.getName();
        }

        @Override
        public Icon getIcon() {
            update();
            Icon i = command.getIcon();
            if (sizedIcon != i) {
                sizedIcon = coerceIconSize(i);
                finalIcon = sizedIcon;
                if (command instanceof AbstractToggleCommand) {
                    finalIcon = new ToggleIcon((AbstractToggleCommand) command, finalIcon);
                }
            }
            return finalIcon;
        }
        private Icon sizedIcon;
        private Icon finalIcon;

        @Override
        public void onAttach(Context context) {
            if (peer != null) {
                if (pcl == null) {
                    pcl = (PropertyChangeEvent evt) -> {
                        if (evt.getPropertyName().equals(AbstractCommand.SELECTED_KEY)) {
                            peer.repaint();
                        }
                    };
                }
                command.addPropertyChangeListener(pcl);
            }
        }

        @Override
        public void onDetach(Context context) {
            if (peer != null && pcl != null) {
                command.removePropertyChangeListener(pcl);
            }
        }
        private ButtonPeer peer;
        private PropertyChangeListener pcl;

        @Override
        public boolean isEnabledInCurrentContext(Context context) {
            update();
            return command.isEnabled();
        }

        @Override
        public boolean isVisibleInCurrentContext(Context context) {
            return autohide ? isEnabledInCurrentContext(context) : true;
        }

        /**
         * Causes the default implementation of
         * {@link #isVisibleInCurrentContext} to return the same value as
         * {@link #isEnabledInCurrentContext}. (The default behaviour is to
         * simply return {@code true}.)
         *
         * @return this command button
         */
        public CommandButton hideIfDisabled() {
            autohide = true;
            return this;
        }

        /**
         * Updates the state of the command that this button is a proxy for. If
         * this is called more than once while the same event is being
         * dispatched, it will only update the actual command once (the first
         * time it is called for that event). This allows this method to be
         * called multiple times while determining the button state without a
         * significant performance penalty, even if the command's update process
         * is complex.
         */
        protected void update() {
            if (EventQueue.getCurrentEvent() != lastUpdateEvent) {
                lastUpdateEvent = EventQueue.getCurrentEvent();
                command.update();
            }
        }
        private EventObject lastUpdateEvent;

        /**
         * Activates the command associated with this button.
         *
         * @param e the action event for the button activation
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (command instanceof AbstractToggleCommand) {
                AbstractToggleCommand toggle = (AbstractToggleCommand) command;
                toggle.setSelected(!toggle.isSelected());
            }
            command.actionPerformed(e);
        }

        /**
         * Returns a string representation of the button suitable for debugging.
         * The returned string is <b>Button&lt;<i>id</i>&gt;</b>, where
         * <i>id</i>
         * is the button's ID as returned by {@link Button#getID()}.
         *
         * @return a string representation of the button
         */
        @Override
        public String toString() {
            return "Button<" + getID() + '>';
        }
    }

    private static final class ToggleIcon implements Icon {

        private final Icon i;
        private final AbstractToggleCommand c;

        public ToggleIcon(AbstractToggleCommand command, Icon toWrap) {
            c = command;
            i = toWrap;
        }

        @Override
        public void paintIcon(Component comp, Graphics g, int x, int y) {
            if (c.isSelected()) {
                g.setColor(BUTTON_SELECTED_BACKGROUND);
                g.fillRect(x, y, i.getIconWidth(), i.getIconHeight());
            }
            i.paintIcon(comp, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return i.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return i.getIconHeight();
        }
    }

    /**
     * Encapsulates information about the bar's current context. This allows
     * buttons to behave in a manner appropriate to the control that the bar is
     * attached to. Before the context bar is attached to a new control, each of
     * its buttons will be queried with this context to determine if they should
     * be hidden or disabled.
     *
     * @see Button#isEnabledInCurrentContext
     * @see Button#isVisibleInCurrentContext
     */
    public static class Context {

        private ContextBar source;
        private JComponent target;
        private boolean markup;
        private boolean isMultiline;

        private Context(ContextBar source, JComponent target) {
            if (target == null) {
                throw new NullPointerException("target");
            }
            this.source = source;
            this.target = target;
            MarkupTarget mt = StrangeEons.getApplication().getMarkupTarget();
            JComponent curTarget = mt == null ? null : (JComponent) mt.getTarget();
            markup = target == curTarget;

            // if there is a markup target (and hence a text control),
            // find out if it is a single-line type
            if (markup) {
                isMultiline = !(target instanceof JTextField);
            }
        }

        /**
         * Returns the context bar that this context information is associated
         * with.
         *
         * @return the context bar whose context is being described
         */
        public ContextBar getSource() {
            return source;
        }

        /**
         * Returns the control that the bar is attached to. In some cases, this
         * may be a child of the component that you expect. For example, the
         * context bar attaches to the editor child component of an editable
         * combo box, not the combo box itself.
         *
         * @return the control the bar is attached to
         */
        public JComponent getTarget() {
            return target;
        }

        /**
         * Returns {@code true} if the target component is also the current
         * markup target.
         *
         * @return if the bar is attached to the markup target
         */
        public boolean isMarkupTarget() {
            return markup;
        }

        /**
         * Returns {@code true} if the context bar is attached to some kind of
         * text editor, and that editor supports more than a single line of
         * text. For example, a context bar attached to a {@code JTextField}
         * would return {@code false}, while a context bar attached to a
         * {@code JTextArea} would return {@code true}.
         *
         * @return {@code true} if the context bar is attached to a multi-line
         * text editor
         */
        public boolean isMultipleLineTextEditor() {
            return isMultiline;
        }

        /**
         * If the current target is a code editor, returns the {@link CodeType}
         * of the code being edited. Otherwise, returns {@code null}. The
         * following code can be used to verify that the target is a valid
         * markup target but is not used to edit code (i.e., that it is used to
         * enter markup for a game component):
         * <pre>
         * if( context.isMarkupTarget() &amp;&amp; context.getCodeType() == null ) {
         *     // the target is (very likely) a text field for a game component
         * }
         * </pre>
         *
         * @return the type of code being edited, or {@code null}; if code is
         * being edited but the type is indeterminate, script code
         * ({@code CodeType.JAVASCRIPT}) is assumed
         */
        public CodeType getCodeType() {
            if (markup && (target instanceof JSourceCodeEditor)) {
                StrangeEonsEditor ed = getEditor();
                if (ed instanceof CodeEditor && ((CodeEditor) ed).getEditor() == target) {
                    return ((CodeEditor) ed).getCodeType();
                }

                // if this is a standalone editor component,
                // try to infer an appropriate code type
                Tokenizer tz = ((JSourceCodeEditor) target).getTokenizer();
                if (tz == null) {
                    return CodeType.PLAIN;
                }
                Class t = tz.getClass();

                if (t == JavaScriptTokenizer.class) {
                    return CodeType.JAVASCRIPT;
                }
                if (t == JavaTokenizer.class) {
                    return CodeType.JAVA;
                }
                if (t == PropertyTokenizer.class) {
                    return CodeType.SETTINGS;
                }
                if (t == HTMLTokenizer.class) {
                    return CodeType.HTML;
                }
                if (t == CSSTokenizer.class) {
                    return CodeType.CSS;
                }
                if (t == PlainTextTokenizer.class) {
                    return CodeType.PLAIN;
                }

                // give up
                StrangeEons.log.log(Level.WARNING, "don''t know CodeType to infer for tokenizer: {0}", t);
                return CodeType.PLAIN;
            }
            return null;
        }

        /**
         * If the target is within a Strange Eons editor tab, returns the
         * editor. Otherwise returns {@code null}.
         *
         * @return the editor affected by the target, or {@code null}
         */
        public StrangeEonsEditor getEditor() {
            Container p = target;
            while (p != null) {
                if (p instanceof StrangeEonsEditor) {
                    return (StrangeEonsEditor) p;
                }
                p = p.getParent();
            }
            return null;
        }

        /**
         * If the target is part of an editor for a game component, returns the
         * game component. Otherwise, returns {@code null}.
         *
         * @return the game component affected by the markup target, or
         * {@code null}
         */
        public GameComponent getGameComponent() {
            StrangeEonsEditor ed = getEditor();
            if (ed == null) {
                return null;
            }
            return ed.getGameComponent();
        }

        /**
         * If the target is part of an editor associated with a particular game,
         * returns the game. Otherwise, returns {@code null}.
         *
         * @return the game of the game component affected by the markup target,
         * or {@code null}
         */
        public Game getGame() {
            GameComponent gc = getGameComponent();
            if (gc != null) {
                final String symbol = gc.getSettings().get(Game.GAME_SETTING_KEY);
                return Game.get(symbol);
            }
            return null;
        }

        /**
         * If the target is a page in a deck, returns the target as a page view
         * instance. Otherwise, returns {@code null}.
         *
         * @return the page view represented by the target, or {@code null}
         */
        public PageView getPageView() {
            if (!markup && (target instanceof PageView)) {
                return (PageView) target;
            }
            return null;
        }

        /**
         * If the target is a page in a deck, returns the {@link Deck}.
         * Otherwise returns {@code null}.
         *
         * @return the deck represented by the target, or {@code null}
         */
        public Deck getDeck() {
            PageView v = getPageView();
            if (v != null) {
                return v.getPage().getDeck();
            }
            return null;
        }
    }

    //
    //
    // PRIVATE BUTTON CLASSES AND REGISTRATION OF BUILT-IN BUTTONS
    //
    //
    private static class MarkupCommandButton extends CommandButton {

        private boolean multiline;
        private boolean html;
        private String name;

        public MarkupCommandButton(AbstractCommand command) {
            super(command);
        }

        public MarkupCommandButton multilineOnly() {
            multiline = true;
            return this;
        }

        public MarkupCommandButton html() {
            html = true;
            return this;
        }

        public MarkupCommandButton styleName(String tag) {
            name = "<html>" + tag + getName();
            return this;
        }

        public MarkupCommandButton name(String key) {
            name = string(key);
            return this;
        }

        @Override
        public String getName() {
            return name == null ? super.getName() : name;
        }

        @Override
        public boolean isVisibleInCurrentContext(Context context) {
            if (!context.isMarkupTarget()) {
                return false;
            }

            if (context.getCodeType() != null) {
                if (html) {
                    return context.getCodeType().normalize() == CodeType.HTML;
                }
                return false;
            }

            if (multiline) {
                return context.isMultipleLineTextEditor();
            }

            return true;
        }
    }

    private static class ParagraphButton extends AbstractButton {

        private final int hsel;

        private int vsel, jsel;

        public ParagraphButton(String id, String descKey, String icon, int hsel, int vsel, int jsel) {
            super(id, descKey.replace("&", ""), icon);
            this.hsel = hsel;
            this.vsel = vsel;
            this.jsel = jsel;
        }

        @Override
        public boolean isVisibleInCurrentContext(Context context) {
            return context.isMultipleLineTextEditor() && context.getCodeType() == null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MarkupTarget mt = StrangeEons.getApplication().getMarkupTarget();
            if (mt == null) {
                return;
            }
            JComponent jc = (JComponent) mt.getTarget();
            ParagraphDialog.adjustParagraphSettings(jc, hsel, vsel, jsel);
        }
    }

    private static String string(String key) {
        return Language.string(key);
    }

    /**
     * Setting key that controls whether the default bar is enabled.
     */
    public static final String ENABLE_BAR_SETTING = "show-context-bar";
    /**
     * Setting key that lists the buttons to appear on the context bar.
     */
    public static final String BUTTONS_SETTING = "context-bar";
    /**
     * The default value used when {@link #BUTTONS_SETTING} has no value.
     */
    public static final String DEFAULT_BUTTON_LIST = "BOLD,ITALIC,UNDERLINE,|,H1,H2,|,INSERTFONT,INSERTIMAGE,|,LEFT,CENTRE,RIGHT";

    /**
     * The context bar can be prevented from appearing over a particular control
     * by setting a client property with this name to
     * {@code java.lang.Boolean.TRUE}.
     */
    public static final String BAR_DISABLE_PROPERTY = "contextbar-disable";

    /**
     * By default, the context bar appears above the control that it currently
     * affects. Setting a client property with this name to
     * {@code java.lang.Boolean.TRUE} will cause the bar to appear below the
     * control instead.
     */
    public static final String BAR_BELOW_PROPERTY = "contextbar-below";

    /**
     * By default, the context bar appears outside of the control that it
     * currently affects. Setting a client property with this name to
     * {@code java.lang.Boolean.TRUE} will cause the bar to appear inside the
     * control instead.
     */
    public static final String BAR_INSIDE_PROPERTY = "contextbar-inside";

    /**
     * By default, the context bar appears on the trailing edge of the the
     * control that it currently affects. Setting a client property with this
     * name to {@code java.lang.Boolean.TRUE} will cause the bar to appear on
     * the leading side instead.
     */
    public static final String BAR_LEADING_SIDE_PROPERTY = "contextbar-leading";

    /**
     * Adjusts the usual location of the context bar. Setting a client property
     * with this name and a {@link java.awt.Point} value on a component will
     * cause the context bar to shift the usual location of the bar by that many
     * pixels. This allows you to fine tune the location of the bar for specific
     * situations. It should only be used when necessary.
     */
    public static final String BAR_OFFSET_PROPERTY = "context-bar-offset";

    /**
     * Allows a component to have complete control over how the context bar is
     * positioned relative to that component. To define a custom location for a
     * component, set a client property on the component with this name and a
     * value that is an instance of the {@link Locator} class.
     */
    public static final String BAR_CUSTOM_LOCATION_PROPERTY = "context-bar-custom";

    /**
     * This interface is implemented by objects that customize where a context
     * bar appears over a component.
     */
    public static interface Locator {

        /**
         * Returns the rectangle that will be used to determine where the
         * context bar appears for a component. The rectangle's x and y values
         * must be in screen coordinates. The context bar will be located
         * relative to the returned rectangle as if that rectangle were the
         * bounding rectangle of the target component. For example, if the
         * {@link #BAR_BELOW_PROPERTY} is set to {@code true}, then the bar will
         * be placed below the bottom of the returned rectangle. If this method
         * returns {@code null}, then the context bar will be shown at its
         * normal location.
         *
         * @param bar the context bar that is being positioned
         * @param barWidth the width of the context bar window, in pixels
         * @param barHeight the height of the context bar window, in pixels
         * @param target the component that the context bar is attached to
         * @return the rectangle, in screen coordinates, that the bar will be
         * positioned relative to, or {@code null} to use the target component's
         * bounding rectangle
         * @see #BAR_CUSTOM_LOCATION_PROPERTY
         */
        Rectangle getContextBarRectangle(ContextBar bar, int barWidth, int barHeight, JComponent target);
    };

    private static class DeckCommandButton extends CommandButton {

        public DeckCommandButton(AbstractCommand command) {
            super(command);
        }

        @Override
        public boolean isVisibleInCurrentContext(Context context) {
            return context.getPageView() != null;
        }
    }

    static {
        registerButton(new CommandButton(Commands.CUT));
        registerButton(new CommandButton(Commands.COPY));
        registerButton(new CommandButton(Commands.PASTE));

        registerButton(new MarkupCommandButton(Commands.MARKUP_BOLD).styleName("<b>").html());
        registerButton(new MarkupCommandButton(Commands.MARKUP_ITALIC).styleName("<i>").html());
        registerButton(new MarkupCommandButton(Commands.MARKUP_UNDERLINE).name("ffd-l-underline").html());
        registerButton(new MarkupCommandButton(Commands.MARKUP_STRIKETHROUGH).name("ffd-l-strikethrough").html().multilineOnly());
        registerButton(new MarkupCommandButton(Commands.MARKUP_SUPERSCRIPT).name("ffd-l-super").html().multilineOnly());
        registerButton(new MarkupCommandButton(Commands.MARKUP_SUBSCRIPT).name("ffd-l-sub").html().multilineOnly());

        registerButton(new MarkupCommandButton(Commands.MARKUP_HEADING).html().multilineOnly());
        registerButton(new MarkupCommandButton(Commands.MARKUP_SUBHEADING).html().multilineOnly());

        registerButton(new MarkupCommandButton(Commands.MARKUP_INSERT_FONT).multilineOnly());
        registerButton(new MarkupCommandButton(Commands.MARKUP_INSERT_COLOUR).multilineOnly());
        registerButton(new MarkupCommandButton(Commands.MARKUP_INSERT_IMAGE).multilineOnly());
        registerButton(new MarkupCommandButton(Commands.MARKUP_INSERT_CHARACTERS).multilineOnly());

        registerButton(new ParagraphButton("LEFT", string("para-b-left"), "al-text-left.png", 0, -1, -1));
        registerButton(new ParagraphButton("CENTRE", string("para-b-centre"), "al-text-centre.png", 1, -1, -1));
        registerButton(new ParagraphButton("RIGHT", string("para-b-right"), "al-text-right.png", 2, -1, -1));
        registerButton(new MarkupCommandButton(Commands.MARKUP_ALIGNMENT).multilineOnly());

        registerButton(new DeckCommandButton(Commands.TO_FRONT));
        registerButton(new DeckCommandButton(Commands.TO_BACK));
        registerButton(new DeckCommandButton(Commands.GROUP));
        registerButton(new DeckCommandButton(Commands.UNGROUP));
        registerButton(new DeckCommandButton(Commands.TURN_LEFT));
        registerButton(new DeckCommandButton(Commands.TURN_RIGHT));
        registerButton(new DeckCommandButton(Commands.ALIGN_LEFT));
        registerButton(new DeckCommandButton(Commands.ALIGN_CENTER));
        registerButton(new DeckCommandButton(Commands.ALIGN_RIGHT));
        registerButton(new DeckCommandButton(Commands.ALIGN_TOP));
        registerButton(new DeckCommandButton(Commands.ALIGN_MIDDLE));
        registerButton(new DeckCommandButton(Commands.ALIGN_BOTTOM));
        registerButton(new DeckCommandButton(Commands.DISTRIBUTE_HORZ));
        registerButton(new DeckCommandButton(Commands.DISTRIBUTE_VERT));
        registerButton(new DeckCommandButton(Commands.FLIP_HORZ));
        registerButton(new DeckCommandButton(Commands.FLIP_VERT));
        registerButton(new DeckCommandButton(Commands.LOCK));
        registerButton(new DeckCommandButton(Commands.UNLOCK));

        registerButton(new DeckCommandButton(Commands.VIEW_DECK_HANDLES));
        registerButton(new DeckCommandButton(Commands.VIEW_DECK_GRID));
        registerButton(new DeckCommandButton(Commands.VIEW_DECK_MARGIN));

        registerButton(new CommandButton(Commands.RUN_FILE).hideIfDisabled());
        registerButton(new CommandButton(Commands.COMMENT_OUT).hideIfDisabled());
        registerButton(new CommandButton(Commands.REMOVE_TRAILING_SPACES).hideIfDisabled());
        registerButton(new CommandButton(Commands.SORT).hideIfDisabled());
    }
}
