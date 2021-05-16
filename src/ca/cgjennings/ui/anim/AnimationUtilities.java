package ca.cgjennings.ui.anim;

import static ca.cgjennings.math.Interpolation.lerp;
import ca.cgjennings.ui.StyleUtilities;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Helper methods for working with animations.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class AnimationUtilities {

    private AnimationUtilities() {
    }

    /**
     * Given a composition, return a new composition that plays the original
     * composition in reverse.
     *
     * @param forward the forward-playing composition
     * @return a composer that wraps the original composer
     */
    public static FrameComposer reverse(final FrameComposer forward) {
        return (float position) -> {
            forward.composeFrame(1f - position);
        };
    }

    /**
     * Given a composition, return a new composition that can play the original
     * composition in a loop by playing it forwards, then backwards.
     *
     * @param forward the forward-playing composition
     * @return a composer that wraps the original composer
     */
    public static FrameComposer loop(final FrameComposer forward) {
        return (float position) -> {
            if (position < 0.5f) {
                forward.composeFrame(position * 2f);
            } else {
                forward.composeFrame(1f - (position - 0.5f) * 2f);
            }
        };
    }

    /**
     * Given a composition, return a new composition that plays a subsequence of
     * the original.
     *
     * @param original the composition to cut
     * @param start the time point to start the cut at
     * @param end the time point to end the cut at
     * @return a composer that wraps the original composer
     */
    public static FrameComposer cut(final FrameComposer original, final float start, final float end) {
        return (float position) -> {
            original.composeFrame(lerp(start, end, position));
        };
    }

    /**
     * Performs a subtle animated transition between the icon currently set on
     * target and a new icon.
     *
     * @param target the label whose icon will change
     * @param newIcon the new icon for the label
     */
    public static void animateIconTransition(final JLabel target, final Icon newIcon) {
        final Icon oldIcon = target.getIcon();
        if (!Animation.ENABLED || oldIcon == null || newIcon == null || oldIcon.equals(newIcon)) {
            target.setIcon(newIcon);
            return;
        }

        new Animation(0.2f) {
            @Override
            public void composeFrame(float position) {
                target.setIcon(createTween(target, oldIcon, newIcon, position));
            }
        }.play(target);
    }

    /**
     * Performs a subtle animated transition between the icon currently set on
     * target and a new icon.
     *
     * @param target the label whose icon will change
     * @param newIcon the new icon for the label
     */
    public static void animateIconTransition(final JButton target, final Icon newIcon) {
        final Icon oldIcon = target.getIcon();
        if (!Animation.ENABLED || oldIcon == null || newIcon == null || oldIcon.equals(newIcon)) {
            target.setIcon(newIcon);
            return;
        }

        new Animation(0.2f) {
            @Override
            public void composeFrame(float position) {
                target.setIcon(createTween(target, oldIcon, newIcon, position));
            }
        }.play(target);
    }

    private static Icon createTween(Component target, Icon oldIcon, Icon newIcon, float position) {
        if (position <= 0.05f) {
            return oldIcon;
        }
        if (position >= 0.95f) {
            return newIcon;
        }

        BufferedImage tween = new BufferedImage(
                Math.max(oldIcon.getIconWidth(), newIcon.getIconWidth()),
                Math.max(oldIcon.getIconHeight(), newIcon.getIconHeight()),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = tween.createGraphics();
        try {
            if (position < 1f) {
                oldIcon.paintIcon(target, g, 0, 0);
            }
            if (position > 0f) {
                g.setComposite(AlphaComposite.SrcOver.derive(position));
                newIcon.paintIcon(target, g, 0, 0);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(tween);
    }

    /**
     * Animates a change in the opacity of a window, if opacity changes are
     * supported and enabled.
     *
     * @param w the window to animate
     * @param startAlpha the starting alpha value for the window; if -1, the
     * window's current alpha is used
     * @param endAlpha the alpha value for the window at the end of animation
     * @param seconds the duration of the animation
     * @param disposeOnFinish if {@code true}, the window's
     * {@code dispose} method is called when the animation completes
     * @throws NullPointerException if the window {@code w} is null
     * @throws IllegalArgumentException if the alpha values are outside of the 0
     * to 1 range (except that {@code startAlpha} may be -1) or if
     * {@code seconds} is negative
     */
    public static void animateOpacityTransition(final Window w, float startAlpha, final float endAlpha, float seconds, final boolean disposeOnFinish) {
        if (w == null) {
            throw new NullPointerException("w");
        }

        if (!StyleUtilities.isOpacityChangeEnabled()) {
            if (disposeOnFinish) {
                w.dispose();
            }
            return;
        }

        final float sAlpha = startAlpha == -1f ? StyleUtilities.getWindowOpacity(w) : startAlpha;
        if (sAlpha < 0f || sAlpha > 1f) {
            throw new IllegalArgumentException("startAlpha: " + startAlpha);
        }
        if (endAlpha < 0f || endAlpha > 1f) {
            throw new IllegalArgumentException("endAlpha: " + endAlpha);
        }

        if (!Animation.ENABLED) {
            StyleUtilities.setWindowOpacity(w, endAlpha);
            if (disposeOnFinish) {
                w.dispose();
            }
        } else {
            new Animation(seconds < 0f ? 0.25f : seconds) {
                @Override
                public void composeFrame(float position) {
                    StyleUtilities.setWindowOpacity(w, lerp(position, sAlpha, endAlpha));
                    if (position == 1f && disposeOnFinish) {
                        w.dispose();
                    }
                }
            }.play(w);
        }
    }

    /**
     * Calls attention to a component by flashing its background.
     *
     * @param target the component to flash
     * @param flashColor the background colour to use when flashing; if
     * {@code null} a default colour is used
     */
    public static void attentionFlash(final JComponent target, final Color flashColor) {
        if (Animation.ENABLED) {
            final Color originalBG = target.getBackground();
            final Color flashBG = flashColor == null ? new Color(0xFFA506) : flashColor;
            final boolean wasOpaque = target.isOpaque();

            new Animation(1.5f) {
                @Override
                public void composeFrame(float position) {
                    if (position <= 0.167f || (position > 0.333f && position <= 0.5f) || (position > 0.667f && position <= 0.833f)) {
                        if (target.getBackground() != flashBG) {
                            target.setBackground(flashBG);
                            if (!wasOpaque) {
                                target.setOpaque(true);
                            }
                        }
                    } else {
                        if (target.getBackground() != originalBG) {
                            target.setBackground(originalBG);
                            if (!wasOpaque) {
                                target.setOpaque(false);
                            }
                        }
                    }
                }
            }.play();
        }
    }

//	public static FrameComposer splice( final FrameComposer... clips ) {
//		if( clips == null ) throw new NullPointerException( "clips" );
//		if( clips.length == 0 ) throw new IllegalArgumentException( "clips is empty" );
//		if( clips.length == 1 ) return clips[0];
//
//		final float[] keys = new float[ clips.length ];
//		keys[0] = 0f;
//		for( int i=1; i<clips.length; ++i ) {
//			keys[i] = 1f/clips.length * i;
//		}
//		return splice( clips, keys );
//	}
}
