/**
 * The wizard package provides the infrastructure needed to create dialogs
 * and other controls that automate a complex task by gathering information
 * from the user in a series of steps.
 * Such interface elements are commonly called "wizards", although
 * they have other names as well. (Historically, the first use of this technique
 * appears to have been on the Amiga line of personal computers, where the
 * dialogs were referred to as "genies".)
 *
 * <p>
 * Wizards use the technique of <i>progressive disclosure</i> to hide the
 * complexity of a task and break it down into small steps. Each step is
 * presented as a single page within the dialog, and the user can move between
 * pages using buttons. The user indicates that the process should be completed
 * by pressing a Finish button, at which point the automated task is carried out
 * using the settings provided by the user.
 *
 * <p>
 * A wizard constructed using this package consists of the following parts:
 * <ol>
 * <li> a model that describes the pages and pages order that comprise the steps
 * to be completed by the user
 * <li> a panel that displays the pages; this responds to messages from the
 * model which cause the displayed page to change
 * <li> a container for the panel and buttons (typically placed in a dialog box)
 * <li> a controller that links the buttons in the container to the model; this
 * allows the model to respond to button presses in the wizard, which in turn
 * causes the panel to display the updated page
 * <li> a listener (typically part of the containing dialog) that listens for
 * the user to finish the dialog; in more complex page structures it can also
 * listen for other events and modify the wizard pages dynamically in response
 * </ol>
 */
package ca.cgjennings.ui.wizard;
