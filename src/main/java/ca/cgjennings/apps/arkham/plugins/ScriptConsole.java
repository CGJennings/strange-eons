package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.ToolWindow;
import ca.cgjennings.apps.arkham.TrackedWindow;
import ca.cgjennings.ui.theme.Palette;
import ca.cgjennings.ui.theme.Theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.Painter;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A window that provides a console that can be written to. The console supports
 * both an output stream and an error stream, each of which is displayed in its
 * own style in the window. Plain text output sent to the console will also be
 * sent to the
 * {@linkplain System#out standard output}/{@linkplain System#err standard error}
 * streams. In addition to supporting plain text, is possible to write null {@linkplain ca.cgjennings.apps.arkham.plugins.ScriptConsole.ConsolePrintWriter#insertImage(java.awt.Image) images},
 * {@linkplain ca.cgjennings.apps.arkham.plugins.ScriptConsole.ConsolePrintWriter#insertComponent(java.awt.Component) user interface components},
 * and blocks of simple
 * {@linkplain ca.cgjennings.apps.arkham.plugins.ScriptConsole.ConsolePrintWriter#insertHTML(java.lang.String) HTML code}
 * to the console.
 *
 * <p>
 * The console recognizes text patterns characteristic of the stack trace
 * elements printed when an exception is thrown, and is capable of opening the
 * associated source files.
 *
 * <p>
 * <b>Note:</b> Instead of creating a new console window, use the {@linkplain ScriptMonkey#getSharedConsole() shared console
 * created by the scripting system}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class ScriptConsole extends ToolWindow implements TrackedWindow {
    private static enum StreamType {
        OUT, INFO, WARN, ERROR;

        public boolean isError() {
            return this.ordinal() >= WARN.ordinal();
        }
    }

    private ConsolePrintWriter out, info, warn, error;
    private ConsoleWriter outcon, infocon, warncon, errcon;
    private OutputStream outstream, infostream, warnstream, errstream;
    private Color outColor, errorColor, warnColor, infoColor, backgroundColor;
    private Painter<JComponent> bgpainter;
    
    private ConsoleInput conInput;

    private static Color color(String key, Color def) {
        Color c = UIManager.getDefaults().getColor(key);
        if (c == null) {
            c = UIManager.getLookAndFeelDefaults().getColor(key);
            if (c == null) {
                c = def;
            }
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private void initStyles() {
        backgroundColor = color(Theme.CONSOLE_BACKROUND,
                Palette.get.background.opaque.fill
        );
        outColor = color(Theme.CONSOLE_OUTPUT,
                Palette.get.contrasting(backgroundColor.getRGB()).opaque.text
        );
        errorColor = color(
                Theme.CONSOLE_ERROR,
                Palette.get.contrasting(backgroundColor.getRGB()).opaque.red
        );
        warnColor = color(
                Theme.CONSOLE_WARNING,
                Palette.get.contrasting(backgroundColor.getRGB()).opaque.yellow
        );
        infoColor = color(
                Theme.CONSOLE_INFO,
                Palette.get.contrasting(backgroundColor.getRGB()).opaque.blue
        );
        console.setSelectionColor(color(
                Theme.CONSOLE_SELECTION_BACKGROUND,
                Palette.get.harmonizing(backgroundColor.getRGB()).opaque.yellow
        ));
        console.setSelectedTextColor(color(Theme.CONSOLE_SELECTION_FOREGROUND,
                Palette.get.contrasting(backgroundColor.getRGB()).opaque.text
        ));
        
        bgpainter = (Painter<JComponent>) UIManager.getDefaults().get(Theme.CONSOLE_BACKGROUND_PAINTER);
        
        Font font = UIManager.getDefaults().getFont(Theme.CONSOLE_FONT);
        if (font == null) {
            font = ResourceKit.getEditorFont();
        }        
        console.setFont(font);
        outcon.createStyles();
        infocon.createStyles();
        warncon.createStyles();
        errcon.createStyles();
    }

    /**
     * Creates new output console window.
     */
    public ScriptConsole(java.awt.Frame parent) {
        super(parent, false);
        initComponents();
        setBody(scrollPane);

        outcon = new ConsoleWriter(StreamType.OUT);
        infocon = new ConsoleWriter(StreamType.INFO);
        warncon = new ConsoleWriter(StreamType.WARN);
        errcon = new ConsoleWriter(StreamType.ERROR);
        out = new ConsolePrintWriter(outcon);
        info = new ConsolePrintWriter(infocon);
        warn = new ConsolePrintWriter(warncon);
        error = new ConsolePrintWriter(errcon);

        if (StrangeEons.getWindow() != null) {
            StrangeEons.getWindow().startTracking(this);
        }

        if (!Settings.getUser().applyWindowSettings("console", this)) {
            GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            Rectangle r = dev.getDefaultConfiguration().getBounds();
            setBounds(r.x + r.width - getWidth() - 32, 32, getWidth(), getHeight());
        }

        console.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                    ConsoleErrorLocation el = getErrorAtPoint(e.getPoint());
                    if (el != null) {
                        el.open();
                    }
                }
            }
        });
        console.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                ConsoleErrorLocation el = getErrorAtPoint(e.getPoint());
                if (el != null) {
                    final Cursor c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                    if (!console.getCursor().equals(c)) {
                        console.setCursor(c);
                    }
                } else {
                    final Cursor c = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
                    if (!console.getCursor().equals(c)) {
                        console.setCursor(c);
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                final Cursor c = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
                if (!console.getCursor().equals(c)) {
                    console.setCursor(c);
                }
            }
        });
        
        conInput = new ConsoleInput(this);
        conInput.setConsoleInputVisible(true);
    }
    private Timer flushTimer = new Timer(100, (ActionEvent e) -> {
        __flushAllPendingFragments();
    });

    ConsoleInput getConsoleInput() {
        return conInput;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            if (!flushTimer.isRunning()) {
                if (backgroundColor == null) {
                    initStyles();
                }
                __flushAllPendingFragments();
                flushTimer.start();
            }
        } else {
            if (this == ScriptMonkey.getSharedConsole()) {
                Settings.getUser().storeWindowSettings("console", ScriptConsole.this);
            }
            flushTimer.stop();
        }
        super.setVisible(visible);
    }

    /**
     * Returns an output stream that can be used to write to the stdout stream
     * of the console. No translation is performed on the bytes written to this
     * stream.
     *
     * @return the console's output stream
     */
    public synchronized OutputStream getOutputStream() {
        if (outstream == null) {
            outstream = new ConsoleOutputStream(outcon);
        }
        return outstream;
    }

    /**
     * Returns an output stream that can be used to write to the stdout
     * "information" stream of the console.
     *
     * @return the console's output stream
     */
    public synchronized OutputStream getInfoStream() {
        if (infostream == null) {
            infostream = new ConsoleOutputStream(infocon);
        }
        return infostream;
    }

    /**
     * Returns an output stream that can be used to write to the stderr
     * "warning" stream of the console.
     *
     * @return the console's output stream
     */    
    public synchronized OutputStream getWarningStream() {
        if (warnstream == null) {
            warnstream = new ConsoleOutputStream(warncon);
        }
        return warnstream;
    }

    /**
     * Returns an output stream that can be used to write to the stderr stream
     * of the console. No translation is performed on the bytes written to this
     * stream.
     *
     * @return the console's error stream
     */
    public synchronized OutputStream getErrorStream() {
        if (errstream == null) {
            errstream = new ConsoleOutputStream(errcon);
        }
        return errstream;
    }

    /**
     * Returns a {@code PrintWriter} that can be used to write to the stdout
     * stream of the console.
     *
     * @return a print writer for {@link #getOutputStream()}
     */
    public ConsolePrintWriter getWriter() {
        return out;
    }

    /**
     * Returns a {@code PrintWriter} that can be used to write to the stdout
     * "information" stream of the console.
     *
     * @return a print writer for {@link #getInfoStream()}
     */
    public ConsolePrintWriter getInfoWriter() {
        return info;
    }

    /**
     * Returns a {@code PrintWriter} that can be used to write to the stderr
     * "warning" stream of the console.
     *
     * @return a print writer for {@link #getWarningStream()}
     */
    public ConsolePrintWriter getWarningWriter() {
        return warn;
    }

    /**
     * Returns a {@code PrintWriter} that can be used to write to the stderr
     * stream of the console.
     *
     * @return a print writer for {@link #getErrorStream()}
     */
    public ConsolePrintWriter getErrorWriter() {
        return error;
    }

    /**
     * Flush all pending console output.
     */
    public void flush() {
        isQueued = false;
        if (EventQueue.isDispatchThread()) {
            __flushAllPendingFragments();
        }
    }
    private volatile boolean isQueued = false;

    /**
     * Request that writes to the stdout stream of this console be queued up
     * until a subsequent call to {@link #flush()}. It is not always possible to
     * obey this request.
     */
    public void queue() {
        isQueued = true;
    }

    public void clear() {
        console.setText("");
    }

    /**
     * Returns the current console text.
     *
     * @return the text of the console
     */
    public Object getHistoryText() {
        return console.getText();
    }

    /** Moves the scrollbar to the first line. */
    public void scrollToTop() {
        EventQueue.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(0);
        });
    }

    /** Moves the scrollbar to the last line. */
    public void scrollToBottom() {
        EventQueue.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPopupMenu consolePopup = new javax.swing.JPopupMenu();
        errorSeparator = new javax.swing.JPopupMenu.Separator();
        clearItem = new javax.swing.JMenuItem();
        clipboardSeparator = new javax.swing.JSeparator();
        copyItem = new javax.swing.JMenuItem();
        selectAllItem = new javax.swing.JMenuItem();
        logSeparator = new javax.swing.JPopupMenu.Separator();
        logItem = new javax.swing.JMenuItem();
        scrollPane = new javax.swing.JScrollPane();
        console = new TextPane();

        consolePopup.add(errorSeparator);

        clearItem.setText(string("plug-console-clear")); // NOI18N
        clearItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearItemActionPerformed(evt);
            }
        });
        consolePopup.add(clearItem);
        consolePopup.add(clipboardSeparator);

        copyItem.setText(string("copy")); // NOI18N
        copyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyItemActionPerformed(evt);
            }
        });
        consolePopup.add(copyItem);

        selectAllItem.setText(string("select-all")); // NOI18N
        selectAllItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllItemActionPerformed(evt);
            }
        });
        consolePopup.add(selectAllItem);
        consolePopup.add(logSeparator);

        logItem.setText(string("plug-console-log")); // NOI18N
        logItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logItemActionPerformed(evt);
            }
        });
        consolePopup.add(logItem);

        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scrollPane.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        console.setEditable(false);
        console.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        console.setDragEnabled(true);
        console.setOpaque(false);
        console.setPreferredSize(new java.awt.Dimension(600, 300));
        scrollPane.setViewportView(console);

        setTitle(string("plug-console-title")); // NOI18N
        setName("Script Console"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        setSize(new java.awt.Dimension(600, 300));
    }// </editor-fold>//GEN-END:initComponents

	private void clearItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearItemActionPerformed
            clear();
	}//GEN-LAST:event_clearItemActionPerformed

private void copyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyItemActionPerformed
    console.copy();
}//GEN-LAST:event_copyItemActionPerformed

private void selectAllItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllItemActionPerformed
    console.selectAll();
}//GEN-LAST:event_selectAllItemActionPerformed

private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    setVisible(false);
}//GEN-LAST:event_formWindowClosing

    private void logItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logItemActionPerformed
        for(var entry : StrangeEons.getLogEntries()) {
            if (entry.level == Level.SEVERE) {
                error.print(entry.message);
            } else if (entry.level == Level.WARNING) {
                warn.print(entry.message);
            } else {
                info.print(entry.message);
            }
        }
    }//GEN-LAST:event_logItemActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem clearItem;
    private javax.swing.JSeparator clipboardSeparator;
    private javax.swing.JTextPane console;
    private javax.swing.JMenuItem copyItem;
    private javax.swing.JPopupMenu.Separator errorSeparator;
    private javax.swing.JMenuItem logItem;
    private javax.swing.JPopupMenu.Separator logSeparator;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JMenuItem selectAllItem;
    // End of variables declaration//GEN-END:variables

    @Override
    public Icon getIcon() {
        return ResourceKit.getIcon("application/script-output.png");
    }

    public class ConsolePrintWriter extends PrintWriter {
        private final StreamType stream;

        private ConsolePrintWriter(ConsoleWriter out) {
            super(out, true);
            stream = out.stream;
        }

        public void insertImage(Color c) {
            final int SIZE = console.getFont().getSize() + 2;
            BufferedImage im = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = im.createGraphics();
            try {
                if (c.getAlpha() < 255) {
                    g.setPaint(new ca.cgjennings.graphics.paints.CheckeredPaint());
                    g.fillRect(0, 0, SIZE, SIZE);
                }
                g.setPaint(c);
                g.fillRect(0, 0, SIZE, SIZE);
            } finally {
                g.dispose();
            }
            insertImage(im);
            print(" #" + resources.Settings.Colour.from(c).toString() + ' ');
        }
        
        public void insertImage(Image img) {
            insertImage(new ImageIcon(img));
        }

        public void insertImage(Icon img) {
            pending.add(new StreamFragment(stream, img));
        }

        public void insertComponent(Component c) {
            pending.add(new StreamFragment(stream, c));
        }

        public void insertHTML(String html) {
            html = "<html>" + html;
            JLabel text = new JLabel(html);
            text.setFont(console.getFont());
            text.setForeground(outColor);
            insertComponent(text);
        }

        // all printing should get routed through one of these write methods
        @Override
        public void write(final String s, final int off, final int len) {
            if (EventQueue.isDispatchThread()) {
                super.write(s, off, len);
            } else {
                EventQueue.invokeLater(() -> {
                    ConsolePrintWriter.super.write(s, off, len);
                });
            }
        }

        @Override
        public void write(final int c) {
            if (EventQueue.isDispatchThread()) {
                super.write(c);
            } else {
                EventQueue.invokeLater(() -> {
                    ConsolePrintWriter.super.write(c);
                });
            }
        }

        @Override
        public void write(final char buf[], final int off, final int len) {
            if (EventQueue.isDispatchThread()) {
                super.write(buf, off, len);
            } else {
                EventQueue.invokeLater(() -> {
                    ConsolePrintWriter.super.write(buf, off, len);
                });
            }
        }

        @Override
        public void print(Object obj) {
            if (obj == null) {
                super.print("null");
            } else if (obj.getClass().isArray()) {
                super.print('[');
                final int len = Array.getLength(obj);
                for (int i = 0; i < len; ++i) {
                    if (i > 0) {
                        super.print(", ");
                    }
                    print(Array.get(obj, i));
                }
                super.print(']');
            } else if (obj instanceof Collection) {
                super.print('[');
                int count = 0;
                for (Object item : (Collection<?>) obj) {
                    if (count++ > 0) {
                        super.print(", ");
                    }
                    print(item);
                }
                super.print(']');
            } else if (obj instanceof Number) {
                synchronized (formatter) {
                    super.print(formatter.format(obj));
                }
            } else if (obj instanceof Scriptable) {
                if (Context.getCurrentContext() != null) {
                    super.print(ScriptableObject.callMethod((Scriptable) obj, "toString", NO_ARG_ARRAY));
                } else {
                    ContextFactory.getGlobal().enterContext();
                    try {
                        super.print(ScriptableObject.callMethod((Scriptable) obj, "toString", NO_ARG_ARRAY));
                    } finally {
                        Context.exit();
                    }
                }
            } else {
                super.print(obj);
            }
        }
        private final Object[] NO_ARG_ARRAY = new Object[0];
        private final NumberFormat formatter;

        {
            formatter = NumberFormat.getNumberInstance();
            formatter.setGroupingUsed(false);
            formatter.setMinimumFractionDigits(0);
            formatter.setMaximumFractionDigits(16);
            formatter.setRoundingMode(RoundingMode.HALF_EVEN);
        }

        // a helper method to make it easier to specify the print(Object) signature from scripts
        public final void printObj(Object obj) {
            print(obj);
        }

        @Override
        public void flush() {
            ScriptConsole.this.flush();
        }

        public void queue() {
            ScriptConsole.this.queue();
        }
    }

    // by the time a write gets here, we should already be in EDT
    private class ConsoleWriter extends Writer {

        private PrintStream companion;
        private MutableAttributeSet attr;
        private StreamType stream;

        public ConsoleWriter(StreamType stream) {
            super(ScriptConsole.this);
            this.stream = stream;
            companion = stream.isError() ? System.err : System.out;
        }

        public void createStyles() {
            Font f = console.getFont();
            attr = new SimpleAttributeSet(console.getInputAttributes());
            Color textColor;
            switch(stream) {
                case OUT:
                    textColor = outColor == null ? Palette.get.foreground.opaque.text : outColor;
                    break;
                case INFO:
                    textColor = infoColor == null ? Palette.get.foreground.opaque.blue : infoColor;
                    break;
                case WARN:
                    textColor = warnColor == null ? Palette.get.foreground.opaque.yellow : warnColor;
                    break;
                case ERROR:
                    textColor = errorColor == null ? Palette.get.foreground.opaque.red : errorColor;
                    break;
                default:
                    throw new AssertionError();
            }

            if (stream.isError()) {
                StyleConstants.setForeground(attr, textColor);
                StyleConstants.setBold(attr, true);
            } else {
                StyleConstants.setForeground(attr, textColor);
                StyleConstants.setBold(attr, f.isBold());
            }
        }

        @Override
        public void write(final char[] cbuff, final int off, final int len) throws IOException {
            pending.add(new StreamFragment(stream, cbuff, off, len));
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException {
            pending.add(new StreamFragment(stream, str, off, len));
        }

        @Override
        public void write(final int c) throws IOException {
            pending.add(new StreamFragment(stream, (char) c));
        }

        @Override
        public void flush() {
            ScriptConsole.this.flush();
        }

        @Override
        public void close() throws IOException {
        }
    }

    private class TextPane extends JTextPane {

        public TextPane() {
            setUI(new BasicTextPaneUI() {
                @Override
                protected void paintBackground(Graphics g1) {
                }
            });
            setSelectionColor(outColor);
            setSelectedTextColor(backgroundColor);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showPopup(e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showPopup(e);
                    }
                }
            });
        }

        @Override
        public void paintComponent(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            Rectangle r = scrollPane.getViewport().getViewRect();
            AffineTransform at = g.getTransform();
            g.translate(r.x, r.y);
            g.setClip(0, 0, r.width, r.height);

            if (bgpainter == null) {
                Paint p = g.getPaint();
                g.setPaint(backgroundColor);
                g.fillRect(0, 0, r.width, r.height);
                g.setPaint(p);
            } else {
                bgpainter.paint(g, this, r.width, r.height);
            }

            g.setTransform(at);
            Map<?,?> rh = (Map<?,?>) getToolkit().getDesktopProperty("awt.font.desktophints");
            g.addRenderingHints(rh);
            super.paintComponent(g);
        }

        @Override
        public void repaint(int x, int y, int width, int height) {
            super.repaint(0, 0, getWidth(), getHeight());
        }

        private void showPopup(MouseEvent e) {
            final JPopupMenu menu = new JPopupMenu();
            final ConsoleErrorLocation el = getErrorAtPoint(e.getPoint());
            if (el != null) {
                String label;
                if (el.getLineNumber() < 1) {
                    label = string("plug-console-go-to-error-no-line-number", el.getShortIdentifier());
                } else {
                    label = string("plug-console-go-to-error", el.getShortIdentifier(), el.getLineNumber());
                }
                JMenuItem errorItem = new JMenuItem(label);
                errorItem.addActionListener((ActionEvent e1) -> {
                    el.open();
                });
                menu.add(errorItem);
                menu.add(errorSeparator);
            }
            menu.add(clearItem);
            menu.add(clipboardSeparator);
            menu.add(copyItem);
            menu.add(selectAllItem);
            menu.add(logSeparator);
            menu.add(logItem);
            menu.show(this, e.getX(), e.getY());
        }
    }

    private final class ConsoleOutputStream extends OutputStream {

        private final ConsoleWriter w;
        private final StreamType stream;

        public ConsoleOutputStream(ConsoleWriter w) {
            this.w = w;
            this.stream = w.stream;
        }

        @Override
        public void write(int b) throws IOException {
            w.write(b);
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void flush() throws IOException {
            ScriptConsole.this.flush();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            pending.add(new StreamFragment(stream, b, off, len));
        }
    }

    private static enum FragType {
        BYTE, CHAR, BYTEBUFF, CHARBUFF, STRBUFF, ICON, COMPONENT
    }

    private static final class StreamFragment {

        private StreamType stream;
        private FragType type;
        private int b;
        private Object buff;
        private int off, len;

        public StreamFragment(StreamType stream, char ch) {
            this.stream = stream;
            type = FragType.CHAR;
            this.b = ch;
        }

        public StreamFragment(StreamType stream, byte[] bbuff, int off, int len) {
            this.stream = stream;
            type = FragType.BYTEBUFF;
            this.buff = bbuff.clone();
            this.off = off;
            this.len = len;
        }

        public StreamFragment(StreamType stream, char[] cbuff, int off, int len) {
            this.stream = stream;
            type = FragType.CHARBUFF;
            this.buff = cbuff.clone();
            this.off = off;
            this.len = len;
        }

        public StreamFragment(StreamType stream, String str, int off, int len) {
            this.stream = stream;
            type = FragType.STRBUFF;
            this.buff = str;
            this.off = off;
            this.len = len;
        }

        public StreamFragment(StreamType stream, Icon icon) {
            this.stream = stream;
            this.type = FragType.ICON;
            this.buff = icon;
        }

        public StreamFragment(StreamType stream, Component comp) {
            this.stream = stream;
            this.type = FragType.COMPONENT;
            this.buff = comp;
        }
    }

    private void __flushAllPendingFragments() {
        if (isQueued) {
            return;
        }

        if (!EventQueue.isDispatchThread()) {
            throw new AssertionError("not in EDT");
        }

        // prevents repeated forced repaints if nothing to flush:
        // avoids flashing of inserted components
        if (pending.isEmpty()) {
            return;
        }

        // ensure that style info has been loaded
        if (backgroundColor == null) {
            initStyles();
        }

        StreamFragment f;
        while ((f = pending.poll()) != null) {
            switch (f.type) {
                case BYTE:
                case CHAR:
                    __insert(f.stream, String.valueOf((char) f.b));
                    break;
                case BYTEBUFF:
                    __insert(f.stream, new String((byte[]) f.buff, f.off, f.len));
                    break;
                case CHARBUFF:
                    __insert(f.stream, new String((char[]) f.buff, f.off, f.len));
                    break;
                case STRBUFF:
                    String s = (String) f.buff;
                    if (f.off > 0 || f.len < s.length()) {
                        s = s.substring(f.off, f.off + f.len);
                    }
                    __insert(f.stream, s);
                    break;
                case COMPONENT:
                    int endOfDoc = console.getDocument().getLength();
                    console.select(endOfDoc, endOfDoc);
                    console.insertComponent((Component) f.buff);
                    __postinsert();
                    break;
                case ICON:
                    endOfDoc = console.getDocument().getLength();
                    console.select(endOfDoc, endOfDoc);
                    console.insertIcon((Icon) f.buff);
                    __postinsert();
                    break;
                default:
                    throw new AssertionError("unknown fragment type: " + f.type);
            }
        }

        if (System.nanoTime() - lastFlushRepaintTime > MIN_FLUSH_REPAINT_PERIOD) {
            __postinsert();
            scrollPane.paintImmediately(0, 0, scrollPane.getWidth(), scrollPane.getHeight());
            lastFlushRepaintTime = System.nanoTime();
        }
    }

    private void __postinsert() {
        int endOfDoc = console.getDocument().getLength();
        if (!isVisible() && getOwner() != null && getOwner().isVisible()) {
            setVisible(true);
            getRootPane().paintImmediately(0, 0, getWidth(), getHeight());
        }

        try {
            @SuppressWarnings("deprecation")
            Rectangle r = console.modelToView(endOfDoc);
            if (r != null) {
                Rectangle vis = console.getVisibleRect();
                r.y -= vis.height;
                r.height = vis.height;
                console.scrollRectToVisible(r);
            }
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.SEVERE, null, ble);
        }

    }

    private void __insert(StreamType stream, String text) {
        if (text.length() == 0) {
            return;
        
        }
        AttributeSet attr;
        switch(stream) {
            case OUT:
                attr = outcon.attr;
                break;
            case INFO:
                attr = infocon.attr;
                break;
            case WARN:
                attr = warncon.attr;
                break;
            case ERROR:
                attr = errcon.attr;
                break;
            default:
                throw new AssertionError();
        }
        try {
            StyledDocument sd = console.getStyledDocument();
            sd.insertString(sd.getLength(), text, attr);
        } catch (BadLocationException e) {
        }
        PrintStream companion = stream.isError() ? errcon.companion : outcon.companion;
        if (companion != null) {
            companion.print(text);
        }

        EventQueue.invokeLater(this::__postinsert);
    }
    private long lastFlushRepaintTime = System.nanoTime();
    private static final long MIN_FLUSH_REPAINT_PERIOD = 30_000_000;
    private ConcurrentLinkedQueue<StreamFragment> pending = new ConcurrentLinkedQueue<>();

    /**
     * Returns a description of the error at the offset into the console text
     * under the specified point in the console window. Returns {@code null} if
     * the line at that point does not represent a valid stack trace entry.
     *
     * @param p the point over the script console
     * @return a description of the stack trace element at that line, or
     * {@code null}
     * @see #getErrorAtOffset(int)
     *
     * @deprecated
     */
    @Deprecated
    public ConsoleErrorLocation getErrorAtPoint(Point p) {
        int pos = console.viewToModel(p);
        return pos < 0 ? null : getErrorAtOffset(pos);
    }
    
    /**
     * Returns a description of the error at the offset into the console text
     * under the specified point in the console window. Returns
     * {@code null} if the line at that point does not represent a valid
     * stack trace entry.
     *
     * @param p the point over the script console
     * @return a description of the stack trace element at that line, or
     * {@code null}
     * @see #getErrorAtOffset(int)
     */
    public ConsoleErrorLocation getErrorAtPoint(Point2D p) {
        int pos = console.viewToModel2D(p);
        return pos < 0 ? null : getErrorAtOffset(pos);
    }    

    /**
     * Returns a description of the error at the line at offset {@code pos} in
     * the script console's text, or {@code null} if that line is not a stack
     * trace line. The script console does not remember which lines are produced
     * from a script stack trace. Rather, it attempts to parse lines on demand
     * to extract the needed information. Therefore it can be fooled by lines
     * that closely mimic the format of script stack traces.
     * <p>
     * If the line at the requested position cannot be parsed as a script stack
     * trace, then the immediately following line will also be tried. This is a
     * convenience in case the requested line is the line of the error message
     * rather than a trace element.
     *
     * @param pos the offset into the console text
     * @return a description of the stack trace element at that line, or
     * {@code null}
     */
    public ConsoleErrorLocation getErrorAtOffset(int pos) {
        // find paragraph element for pos
        StyledDocument document = console.getStyledDocument();
        Element para = document.getParagraphElement(pos);
        ConsoleErrorLocation line = parseStackElementLine(document, para);
        return line;
    }

    private ConsoleErrorLocation parseStackElementLine(StyledDocument document, Element para) {
        int start = para.getStartOffset();
        int end = para.getEndOffset();

        String text;
        try {
            text = document.getText(start, end - start);
        } catch (BadLocationException e) {
            StrangeEons.log.log(Level.SEVERE, null, e);
            return null;
        }

        start = text.indexOf("\tat ");
        if (start < 0) {
            return null;
        }
        // clip off "\tat " prefix
        text = text.substring(start + 4);
        ConsoleErrorLocation cel = new ConsoleErrorLocation(text);
        // see the description of the above package private constructor
        return cel.getIdentifier() == null ? null : cel;
    }
}
