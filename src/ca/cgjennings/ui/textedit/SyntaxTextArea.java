package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.platform.DesktopIntegration;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.HyperlinkEvent;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

/**
 * Syntax highlighting text area used by {@link CodeEditorBase}. Customizes
 * behaviour of the underlying implementation.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
final class SyntaxTextArea extends RSyntaxTextArea {

    /**
     * Editor that embeds this component; may be null.
     */
    private CodeEditorBase editor;

    public SyntaxTextArea() {
        this(null);
    }

    public SyntaxTextArea(CodeEditorBase editor) {
        super();
        this.editor = editor;
        String themeName = ca.cgjennings.ui.theme.ThemeInstaller.isDark() ? "dark.xml" : "light.xml";
        try (InputStream in = SyntaxTextArea.class.getResourceAsStream(themeName)) {
            Theme.load(in).apply(this);
        } catch (IOException ex) {
            StrangeEons.log.warning("unable to load editor theme, using default");
        }

        setTabSize(4);
        setTabsEmulated(true);
        setPaintTabLines(true);
        setHighlightSecondaryLanguages(true);
        setMarkOccurrences(true);
        setMarkOccurrencesDelay(250);
        setParserDelay(400);
        setFadeCurrentLineHighlight(true);
        setCodeFoldingEnabled(true);
        setCloseMarkupTags(true);
        setAntiAliasingEnabled(true);
        setFractionalFontMetricsEnabled(true);
        setHyperlinksEnabled(true);
        setLinkScanningMask(InputEvent.CTRL_DOWN_MASK);

        addHyperlinkListener(li -> {
            if (li.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL url = li.getURL();
                String proto = url.getProtocol();
                if (proto.equals("http") || proto.equals("https") || proto.equals("mailto")) {
                    try {
                        DesktopIntegration.browse(url);
                    } catch (IOException ex) {
                        // not supported or browse failed
                        StrangeEons.log.log(Level.WARNING, "failed to open " +  url, ex);
                        getToolkit().beep();
                    }
                    return;
                }
                // ... other handling
            }
        });
    }

    public void loadDefaultTheme() {
        if (defaultTheme != null) {
            defaultTheme.apply(this);
        } else {
            URL themeUrl = null;
            final ca.cgjennings.ui.theme.Theme seTheme = ca.cgjennings.ui.theme.ThemeInstaller.getInstalledTheme();
            if (seTheme != null) {
                themeUrl = seTheme.getSyntaxThemeUrl();
            }
                    
            final boolean dark = ca.cgjennings.ui.theme.ThemeInstaller.isDark();
            final URL url = SyntaxTextArea.class.getResource(dark ? "dark.xml" : "light.xml");
            loadTheme(url);
        }
    }
    private static Theme defaultTheme;

    public void loadTheme(URL themeUrl) {
        try (InputStream in = themeUrl.openStream()) {
            Theme th = Theme.load(in);
            th.apply(this);
            if (defaultTheme == null) {
                defaultTheme = th;
            }
        } catch (IOException ex) {
            StrangeEons.log.warning("unable to load editor theme " + themeUrl + ", using default");
        }
    }

    private CodeEditorBase.PopupMenuBuilder menuBuilder = null;

    public void setPopupMenuBuilder(CodeEditorBase.PopupMenuBuilder pmb) {
        menuBuilder = pmb;
    }

    public CodeEditorBase.PopupMenuBuilder getPopupMenuBuilder() {
        return menuBuilder;
    }

    @Override
    public JPopupMenu getComponentPopupMenu() {
        return createPopupMenu();
    }

    @Override
    protected JPopupMenu createPopupMenu() {
        if (editor == null) {
            return super.createPopupMenu();
        }

        JPopupMenu menu = createDefaultMenu();
        if (menuBuilder != null) {
            menu = menuBuilder.buildMenu(editor, menu);
        }
        return menu;
    }

    private JPopupMenu createDefaultMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(makeItem(Commands.CUT, this::cut));
        menu.add(makeItem(Commands.COPY, this::copy));
        menu.add(makeItem(Commands.PASTE, this::paste));
        menu.addSeparator();
        menu.add(makeItem(Commands.SELECT_ALL, this::selectAll));
        return menu;
    }

    private JMenuItem makeItem(AbstractCommand base, Runnable perform) {
        JMenuItem item = new JMenuItem();
        if (perform != null) {
            item.addActionListener(li -> perform.run());
            item.setText(base.getName());
            item.setIcon(base.getIcon());
            item.setAccelerator(base.getAccelerator());
        } else {
            item.setAction(base);
        }
        return item;
    }
}
