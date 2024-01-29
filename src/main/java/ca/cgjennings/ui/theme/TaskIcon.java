package ca.cgjennings.ui.theme;

import resources.ResourceKit;

/**
 * Icon that represents a task by adding the standard task background to
 * another icon.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TaskIcon extends ThemedCompoundIcon {
    private static final String TASK_ICON_BASE = "mt00";
    
    public TaskIcon(ThemedIcon image) {
        super(ResourceKit.getIcon(TASK_ICON_BASE), image);
    }
    
    public TaskIcon(String image) {
        super(TASK_ICON_BASE, image);
    }
}
