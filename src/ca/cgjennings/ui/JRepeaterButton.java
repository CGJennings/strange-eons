package ca.cgjennings.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;

/**
 * A button that generates action events as long as it is held down.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JRepeaterButton extends JButton {

    /**
     * Creates a button with no set text or icon.
     */
    public JRepeaterButton() {
        super();
        init();
    }

    /**
     * Creates a button with an icon.
     *
     * @param icon the Icon image to display on the button
     */
    public JRepeaterButton(Icon icon) {
        super(icon);
        init();
    }

    /**
     * Creates a button with text.
     *
     * @param text the text of the button
     */
    public JRepeaterButton(String text) {
        super(text);
        init();
    }

    /**
     * Creates a button where properties are taken from the {@code Action}
     * supplied.
     *
     * @param a the {@code Action} used to specify the new button
     */
    public JRepeaterButton(Action a) {
        super(a);
        init();
    }

    /**
     * Creates a button with initial text and an icon.
     *
     * @param text the text of the button
     * @param icon the Icon image to display on the button
     */
    public JRepeaterButton(String text, Icon icon) {
        super(text, icon);
        init();
    }

    /**
     * If the button is currently held down, return the number of repeats that
     * have occurred so far. Otherwise, returns 0.
     */
    public int getRepeatCount() {
        return repeats;
    }

    /**
     * Set the initial delay (in ms) between repeat events while the button is
     * pressed.
     */
    public void setRepeatDelay(int delay) {
        repeatDelay = delay;
        if (repeatTimer.isRunning()) {
            repeatTimer.setDelay(repeatDelay);
        }
    }

    /**
     * Return the initial delay (in ms) between repeat events while the button
     * is pressed.
     */
    public int getRepeatDelay() {
        return repeatDelay;
    }

    /**
     * Return the number of ms by which the delay is reduced after each repeat
     * until the minimum delay is reached.
     */
    public int getDelayDecay() {
        return delayDecay;
    }

    /**
     * Set the number of ms by which the delay is reduced after each repeat
     * until the minimum delay is reached.
     */
    public void setDelayDecay(int delayDecay) {
        this.delayDecay = delayDecay;
    }

    /**
     * Return the minimum delay, in ms, before a repeat is fired.
     */
    public int getMinimumDelay() {
        return minimumDelay;
    }

    /**
     * The delay from button press until the first repeat is fired is the
     * {@link #getRepeatDelay}. After each subsequent repeat, the delay is
     * reduced by {@link #getDelayDecay} until it reaches
     * {@link #getMinimumDelay}.
     */
    public void setMinimumDelay(int minimumDelay) {
        this.minimumDelay = minimumDelay;
    }

    /**
     * Sets whether the button fires an action event with the command string
     * {@code RELEASE_COMMAND} when the button is released. Default is
     * {@code false}.
     *
     * @param fireEvent if {@code true}, a final event will be posted when
     * the user releases the button
     */
    public void setFireEventOnRelease(boolean fireEvent) {
        fireOnRelease = fireEvent;
    }

    /**
     * Returns {@code true} if the button will fire an action command when
     * the button is released.
     *
     * @return {@code true} if the event will be fired
     */
    public boolean getFireEventOnRelease() {
        return fireOnRelease;
    }

    private void init() {
        repeatTimer = new Timer(repeatDelay, (ActionEvent e) -> {
            ++repeats;
            triggerAction(REPEAT_COMMAND);
            currentDelay -= delayDecay;
            if (currentDelay < minimumDelay) {
                currentDelay = minimumDelay;
            }
            if (repeatTimer.getDelay() != currentDelay) {
                repeatTimer.setDelay(currentDelay);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    modifiers = e.getModifiers();
                }
            }
        });

        addChangeListener((final ChangeEvent e) -> {
            // we post this to run later so that the modifiers variable has
            // a chance to update
            EventQueue.invokeLater(() -> {
                ButtonModel model1 = ((JButton) e.getSource()).getModel();
                if (model1.isPressed()) {
                    if (!repeatTimer.isRunning()) {
                        triggerAction(PRESSED_COMMAND);
                        repeats = 0;
                        currentDelay = repeatDelay;
                        repeatTimer.setDelay(repeatDelay);
                        repeatTimer.setInitialDelay(repeatDelay * 3);
                        repeatTimer.start();
                    }
                } else {
                    if (repeatTimer.isRunning()) {
                        repeatTimer.stop();
                        if (fireOnRelease) {
                            triggerAction(RELEASE_COMMAND);
                        }
                    }
                }
            });
        });
    }

    private void triggerAction(String command) {
        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command, modifiers));
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        // suppress the normal action event from the button
        if (event.getActionCommand() == null) {
            return;
        }
        super.fireActionPerformed(event);
    }

    private int modifiers;
    private int repeatDelay = 250;
    private int delayDecay = 20;
    private int minimumDelay = 50;
    private int currentDelay;
    private Timer repeatTimer;
    private int repeats;
    private boolean fireOnRelease = false;

//	public static void main( String[] args ) {
//		JFrame f = new JFrame();
//		f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//		f.setSize( 128, 64 );
//		JButton b = new JRepeaterButton( "hello" );
//		f.add( b );
//		b.addActionListener( new ActionListener() {
//			public void actionPerformed( ActionEvent e ) {
//				System.err.println( e.getModifiers() );
//			}
//		});
//		f.setVisible( true );
//	}
    /**
     * An action event with this command is fired when the button is first
     * pressed.
     */
    public static final String PRESSED_COMMAND = "initial";
    /**
     * An action event with this command is fired for each repeat as the button
     * is held down.
     */
    public static final String REPEAT_COMMAND = "repeat";
    /**
     * If the button is set to fire an event on release, an action event with
     * this command is fired when the button is released.
     */
    public static final String RELEASE_COMMAND = "released";
}
