package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

/**
 * Syntax highlighting text area used by {@link CodeEditorBase}.
 * Customizes behaviour of the underlying implementation.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
final class SyntaxTextArea extends RSyntaxTextArea {
        
    private CodeEditorBase editor;

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
