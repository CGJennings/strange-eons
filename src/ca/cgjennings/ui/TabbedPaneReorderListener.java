/*
 * TabbedPaneReorderListener.java
 *
 * Created on April 19, 2007, 10:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package ca.cgjennings.ui;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface TabbedPaneReorderListener {

    public abstract void tabbedPanesReordered(JReorderableTabbedPane source, int oldindex, int newindex);
}
