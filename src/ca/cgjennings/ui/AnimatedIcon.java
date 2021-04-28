package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * An icon that animates through a series of images at a fixed rate. This has a
 * similar effect to setting a GIF image on an icon, without the drawbacks of
 * the GIF image format.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class AnimatedIcon implements Icon {

    private final static int DEFAULT_DELAY = 63;
    private final static int DEFAULT_CYCLES = -1;

    private JComponent component;
    private Image[] frames;
    private int cycles;

    //  Track the X, Y location of the Icon within its parent JComponent so we
    //  can request a repaint of only the Icon and not the entire JComponent
    private int iconX;
    private int iconY;
    private int iconWidth;
    private int iconHeight;
    private int currentFrame;
    private int cyclesCompleted;
    private boolean animationFinished = false;
    private Timer timer;

    /**
     * Create an AnimatedIcon using the default frame rate (approximately 16
     * fps).
     *
     * @param component the component the icon will be painted on
     * @param frames the animation frames
     */
    public AnimatedIcon(JComponent component, Image... frames) {
        this(component, DEFAULT_DELAY, frames);
    }

    /**
     * Create an AnimatedIcon that will repeat endlessly.
     *
     * @param component the component the icon will be painted on
     * @param delay the delay between painting each icon, in ms
     * @param frames	the Icons to be painted as part of the animation
     */
    public AnimatedIcon(JComponent component, int delay, Image... frames) {
        this(component, delay, DEFAULT_CYCLES, frames);
    }

    /**
     * Create an AnimatedIcon.
     *
     * @param component the component the icon will be painted on
     * @param delay the delay between painting each icon, in ms
     * @param cycles the number of times to repeat the animation sequence, -1
     * for no limit
     * @param frames	the Icons to be painted as part of the animation
     */
    public AnimatedIcon(JComponent component, int delay, int cycles, Image... frames) {
        if (component == null) {
            throw new NullPointerException("component");
        }
        if (delay < 0) {
            throw new IllegalArgumentException("negative delay");
        }
        if (cycles < -1) {
            throw new IllegalArgumentException("cycles: " + cycles);
        }
        if (frames == null) {
            throw new NullPointerException("frames");
        }
        if (frames.length == 0) {
            throw new IllegalArgumentException("frames.length == 0");
        }

        this.component = component;
        setCycles(cycles);

        int w = frames[0].getWidth(null);
        int h = frames[0].getHeight(null);
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] == null) {
                throw new NullPointerException("icons[" + i + "]");
            }
            if (frames[i].getWidth(null) != w || frames[i].getHeight(null) != h) {
                throw new IllegalArgumentException("frames sizes differ");
            }
        }
        this.frames = frames.clone();
        iconWidth = w;
        iconHeight = h;

        timer = new Timer(delay, (ActionEvent e) -> {
            int frame = currentFrame + 1;
            if (frame == AnimatedIcon.this.frames.length) {
                frame = 0;
                if (++cyclesCompleted == AnimatedIcon.this.cycles) {
                    animationFinished = true;
                }
            }
            
            setCurrentFrame(frame);
            
            if (animationFinished) {
                timer.stop();
            }
        });
    }

    /**
     * Gets the currently displayed frame.
     *
     * @return the index of the displayed image
     */
    public int getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Set the currently displayed frame.
     *
     * @param frame the index of the image to be displayed
     */
    public void setCurrentFrame(int frame) {
        currentFrame = frame;
        component.repaint(iconX, iconY, iconWidth, iconHeight);
    }

    /**
     * Get the cycles to complete before animation stops.
     *
     * @return the number of cycles
     */
    public int getCycles() {
        return cycles;
    }

    /**
     * Specify the number of times to repeat each animation sequence, or cycle.
     *
     * @param cycles the number of cycles to complete before the animation
     * stops, or -1 for continuous
     */
    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    /**
     * Gets the delay between frames, in ms.
     *
     * @return the delay between successive frames
     */
    public int getDelay() {
        return timer.getDelay();
    }

    /**
     * Sets the delay between frames, in ms. To set the frame rate in frames per
     * second, use <code>setDelay( 1000/frameRate )</code>.
     *
     * @param delay the delay between successive frames
     */
    public void setDelay(int delay) {
        timer.setDelay(delay);
    }

    /**
     * Returns the frame with the specified index.
     *
     * @param index the index of the frame to be returned
     * @return the frame at the specified index
     * @exception IndexOutOfBoundsException if the index is out of range
     */
    public Image getFrame(int index) {
        try {
            return frames[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
    }

    /**
     * Returns the number of frames in the animation.
     *
     * @return the total number of frames
     */
    public int getFrameCount() {
        return frames.length;
    }

    /**
     * Pause the animation.
     */
    public void pause() {
        timer.stop();
    }

    /**
     * Continue the animation from where the animation was paused, or restart it
     * if it has completed.
     */
    public void play() {
        if (!timer.isRunning()) {
            if (animationFinished) {
                setCurrentFrame(0);
                animationFinished = false;
                cyclesCompleted = 0;
                timer.start();
            } else {
                timer.restart();
            }
        }
    }

    /**
     * Stop the animation.
     */
    public void stop() {
        if (!animationFinished) {
            timer.stop();
            setCurrentFrame(0);
            animationFinished = true;
        }
    }

    /**
     * Gets the width of this icon.
     *
     * @return the width of the icon in pixels.
     */
    @Override
    public int getIconWidth() {
        return iconWidth;
    }

    /**
     * Gets the height of this icon.
     *
     * @return the height of the icon in pixels.
     */
    @Override
    public int getIconHeight() {
        return iconHeight;
    }

    /**
     * Paint the current frame
     *
     * @param c The component on which the icon is painted
     * @param g the graphics context
     * @param x the X coordinate of the icon's top-left corner
     * @param y the Y coordinate of the icon's top-left corner
     */
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (c == component) {
            iconX = x;
            iconY = y;
        }
        g.drawImage(frames[currentFrame], x, y, null);

        if ((!animationFinished) && (!timer.isRunning())) {
            play();
        }
    }
}
