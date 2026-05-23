package ca.cgjennings.ui;

import java.util.EventListener;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface TabClosingListener extends EventListener {

    public void tabClosing(JCloseableTabbedPane source, int tab, boolean isDirty);
}
