package ca.cgjennings.ui;

import javax.swing.Icon;

/**
 * May be implemented by classes that support a standard icon in order to have
 * that icon rendered by cell renderers.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface IconProvider {

    Icon getIcon();
}
