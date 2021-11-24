package ca.cgjennings.apps.arkham.diy;

import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.sheet.MarkerStyle;
import ca.cgjennings.apps.arkham.sheet.PrintDimensions;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import resources.ResourceKit;
import resources.Settings;

/**
 * The {@link Sheet} implementation used by DIY components.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class DIYSheet extends Sheet<DIY> {

    private final DIY diy;
    private final int index;
    private final int diyFlags;

    /**
     * Creates a new sheet for a DIY component. A sheet is not normally created
     * directly, but is instead created by calling
     * {@link DIY#createDefaultSheets()}. Unlike most sheets, a
     * {@code DIYSheet} does not contain the sheet painting code itself,
     * but instead defers painting to the component it was created for.
     *
     * @param diy the component for which this sheet is being created
     * @param templateKey a template key name used to determine the basic
     * properties of the sheet
     * @param index the index of the sheet (0 for front, 1 for back, and so on)
     * @see DIY#paintFront
     * @see DIY#paintBack
     */
    @SuppressWarnings("deprecation")
    public DIYSheet(DIY diy, String templateKey, int index) {
        super(diy, templateKey);
        this.diy = diy;
        this.index = index;
        diyFlags = diy.getFlags();
        setCornerRadius(diy.getCornerRadius());
    }

    /**
     * Returns the index of this sheet. This can be used to differentiate which
     * face to drawn when the component has more than two faces. Even-numbered
     * sheets are drawn by calling {@link DIY#paintFront}, while odd-numbered
     * sheets are drawn by calling {@link DIY#paintBack}.
     *
     * @return the index of the face, counting from 0
     */
    public int getSheetIndex() {
        return index;
    }

    @Override
    protected void paintSheet(RenderTarget target) {
        Graphics2D g = null;
        try {
            g = createGraphics();

            if ((diyFlags & DIY.OPT_NO_QUALITY_INIT) == 0) {
                applyContextHints(g);
            }

            hiResModeCache = -1;
            activeRenderTarget = target;

            if ((index & 1) == 0) {
                diy.paintFront(g, diy, this);
            } else {
                diy.paintBack(g, diy, this);
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * Returns the currently requested target when called while the sheet is
     * being drawn. When called at any other time, returns the target of the
     * most recent render request.
     *
     * @return the current render target
     */
    public RenderTarget getRenderTarget() {
        return activeRenderTarget;
    }
    private RenderTarget activeRenderTarget;

    /**
     * This method can be called from a painting function to set rendering hints
     * on a graphics context. (These are normally set automatically on the
     * graphics context passed to the painter.)
     *
     * @param g the graphics context to modify
     */
    @Override
    public void applyContextHints(Graphics2D g) {
        super.applyContextHints(g);
    }

    /**
     * Returns {@code true} if the current or most recent rendering was
     * being done in high resolution mode. To return {@code true}, one of
     * the following statements must hold:
     * <ol>
     * <li> the high resolution substitution mode is set to {@code FORCE},
     * or
     * <li> the high resolution substitution mode is set to {@code ENABLE},
     * the render target is either {@code PRINT} or {@code EXPORT},
     * and the requested resolution is greater than the resolution of the
     * template image.
     * </ol>
     *
     * <p>
     * When this method returns {@code true}, painting methods should use
     * the highest resolution source images available.
     *
     * @return {@code true} if in "high resolution" mode
     * @since 2.1a11
     */
    public boolean isHighResolutionRendering() {
        if (hiResModeCache < 0) {
            DIY.HighResolutionMode mode = diy.getHighResolutionSubstitutionMode();
            if (mode == DIY.HighResolutionMode.DISABLE) {
                hiResModeCache = 0;
            } else if (mode == DIY.HighResolutionMode.FORCE) {
                hiResModeCache = 1;
            } else {
                if ((activeRenderTarget == RenderTarget.EXPORT || activeRenderTarget == RenderTarget.PRINT) && getScalingFactor() > 1d) {
                    hiResModeCache = 1;
                } else {
                    hiResModeCache = 0;
                }
            }
        }
        return hiResModeCache == 1;
    }
    private int hiResModeCache = -1;

    /**
     * Paints an image at its normal size at the specified location. This method
     * will perform automatic high resolution image substitution if there is a
     * key with same name as {@code imageKey} but with
     * {@code "-hires"} appended.
     *
     * @param g the graphics context to use for painting
     * @param imageKey the settings key of the image
     * @param x the horizontal offset from the left side of the template image
     * @param y the vertical offset from the top edge of the template image
     */
    public void paintImage(Graphics2D g, String imageKey, int x, int y) {
        Settings s = diy.getSettings();
        BufferedImage image = ResourceKit.getImage(s.get(imageKey));
        if (isHighResolutionRendering()) {
            String value = s.get(imageKey + "-hires");
            if (value != null) {
                BufferedImage hires = ResourceKit.getImage(value);
                g.drawImage(hires, x, y, image.getWidth(), image.getHeight(), null);
                return;
            }
        }
        g.drawImage(image, x, y, null);
    }

    /**
     * Paints an image at a location and size that are taken from a region
     * setting. This method will perform automatic high resolution image
     * substitution if there is a key with same name as {@code imageKey}
     * but with {@code "-hires"} appended.
     *
     * @param g the graphics context to use for painting
     * @param imageKey the settings key of the image
     * @param regionKey the settings key of the region where the image should be
     * drawn, without the <tt>"-region"</tt> suffix
     */
    public void paintImage(Graphics2D g, String imageKey, String regionKey) {
        Rectangle r = diy.getSettings().getRegion(regionKey);
        paintImage(g, imageKey, r.x, r.y, r.width, r.height);
    }

    /**
     * Paints an image at a location and size that are taken from a region
     * setting. The image resource is determined from the value of
     * <tt>sharedKey</tt>, while the region is obtained by concatenating
     * <tt>"-region"</tt> to the shared key name. This method will perform
     * automatic high resolution image substitution if there is a key with same
     * name as {@code sharedKey} but with {@code "-hires"} appended.
     *
     * @param g the graphics context to use for painting
     * @param sharedKey the settings key of the image, and base name of the
     * region key
     */
    public void paintImage(Graphics2D g, String sharedKey) {
        paintImage(g, sharedKey, sharedKey);
    }

    /**
     * Paints an image at the specified location and size. This method will
     * perform automatic high resolution image substitution if there is a key
     * with same name as {@code imageKey} but with {@code "-hires"}
     * appended.
     *
     * @param g the graphics context to use for painting
     * @param imageKey the settings key of the image
     * @param x the horizontal offset from the left side of the template image
     * @param y the vertical offset from the top edge of the template image
     * @param width the width of the image
     * @param height the height of the image
     */
    public void paintImage(Graphics2D g, String imageKey, int x, int y, int width, int height) {
        Settings s = diy.getSettings();
        BufferedImage image = ResourceKit.getImage(s.get(imageKey));
        if (isHighResolutionRendering()) {
            String value = s.get(imageKey + "-hires");
            if (value != null) {
                BufferedImage hires = ResourceKit.getImage(value);
                g.drawImage(hires, x, y, width, height, null);
                return;
            }
        }
        g.drawImage(image, x, y, width, height, null);
    }

    /**
     * Paints a numbered image at a location and size that are taken from a
     * region setting. This is similar to
     * {@linkplain #paintImage(java.awt.Graphics2D, java.lang.String) painting an image with a shared key},
     * but the image to paint is determined using
     * {@link Settings#getNumberedImageResource(java.lang.String, int)}. The
     * image resource is determined from the value of
     * <tt>sharedKey</tt> and <tt>number</tt>, while the region is obtained by
     * concatenating <tt>"-region"</tt> to the shared key name. This method will
     * perform automatic high resolution image substitution if there is a key
     * with same name as {@code sharedKey} but with {@code "-hires"}
     * appended.
     *
     * @param g the graphics context to use for painting
     * @param sharedKey the settings key of the image, and base name of the
     * region key
     * @param number the number of the image to load
     */
    public void paintNumberedImage(Graphics2D g, String sharedKey, int number) {
        Settings s = diy.getSettings();
        BufferedImage bi = null;
        if (isHighResolutionRendering()) {
            String hiresKey = sharedKey + "-hires";
            if (s.get(hiresKey) != null) {
                bi = s.getNumberedImageResource(hiresKey, number);
            }
        }
        if (bi == null) {
            bi = s.getNumberedImageResource(sharedKey, number);
        }
        paintImage(g, bi, sharedKey);
    }

    /**
     * Paints an image at the specified location and size.
     *
     * @param g the graphics context to use for painting
     * @param image the image to draw
     * @param regionKey the settings key of the region where the image should be
     * drawn, without the <tt>"-region"</tt> suffix
     */
    public void paintImage(Graphics2D g, BufferedImage image, String regionKey) {
        paintImage(g, image, diy.getSettings().getRegion(regionKey));
    }

    /**
     * Paints an image at the specified location and size.
     *
     * @param g the graphics context to use for painting
     * @param image the image to draw
     * @param region the region in which to draw the image
     */
    public void paintImage(Graphics2D g, BufferedImage image, Rectangle region) {
        g.drawImage(image, region.x, region.y, region.width, region.height, null);
    }

    /**
     * Paints the card template image that was set when the card was created.
     * This method can perform automatic high resolution image substitution if
     * there is a key with same name as the template key but with
     * {@code "-hires"} appended.
     *
     * @param g the graphics context to use for painting
     */
    public void paintTemplateImage(Graphics2D g) {
        BufferedImage template = getTemplateImage();
        if (isHighResolutionRendering()) {
            String value = diy.getSettings().get(getTemplateKey() + "-hires");
            if (value != null) {
                BufferedImage hires = ResourceKit.getImage(value);
                g.drawImage(hires, 0, 0, template.getWidth(), template.getHeight(), null);
                return;
            }
        }
        g.drawImage(template, 0, 0, null);
    }

    public void paintPortrait(Graphics2D g) {
        String key = diy.getPortraitKey();
        if (key == null) {
            if (warnPortrait) {
                return;
            }
            ScriptMonkey.getSharedConsole().getErrorWriter().println(
                    "DIY component called paintPortrait, but no portrait key was set: " + diy
            );
            warnPortrait = true;
            return;
        }

        Rectangle portraitRect = diy.getSettings().getRegion(key + "-portrait-clip");

        final boolean obeyClip = diy.getPortraitClipping();
        Shape oldClip = null;
        if (obeyClip) {
            oldClip = g.getClip();
            g.clip(portraitRect);
        }

        if (!isTransparent() && diy.isPortraitBackgroundFilled()) {
            BufferedImage template = getTemplateImage();
            Paint p = g.getPaint();
            g.setPaint(Color.WHITE);
            g.fillRect(0, 0, template.getWidth(), template.getHeight());
            g.setPaint(p);
        }

        Portrait p = diy.getPortrait(0);
        BufferedImage portrait = p.getImage();

        double scale = p.getScale();
        double panX = p.getPanX();
        double panY = p.getPanY();

        double scaledWidth = portrait.getWidth() * scale;
        double scaledHeight = portrait.getHeight() * scale;

        double centerX = scaledWidth / 2d;
        double centerY = scaledHeight / 2d;
        double regionX = portraitRect.getX() + portraitRect.getWidth() / 2d;
        double regionY = portraitRect.getY() + portraitRect.getHeight() / 2d;

        g.drawImage(portrait,
                (int) (regionX - centerX + panX /*+ 0.5d*/),
                (int) (regionY - centerY + panY /*+ 0.5d*/),
                (int) (scaledWidth + 0.5d),
                (int) (scaledHeight + 0.5d),
                null
        );

        if (obeyClip) {
            g.setClip(oldClip);
        }
    }

    /**
     * Paints the portrait within the marker clip region for components with the
     * {@link DIY.FaceStyle#CARD_AND_MARKER} face style.
     *
     * @param g the graphics context to paint the marker portrait into
     * @throws IllegalStateException if the component is not a
     * {@code CARD_AND_MARKER} type
     */
    public void paintMarkerPortrait(Graphics2D g) {
        if (diy.getFaceStyle() != DIY.FaceStyle.CARD_AND_MARKER) {
            throw new IllegalStateException("component has no marker");
        }

        String key = diy.getPortraitKey();
        if (key == null) {
            if (warnPortrait) {
                return;
            }
            ScriptMonkey.getSharedConsole().getErrorWriter().println(
                    "DIY component called paintMarkerPortrait, but no portrait key was set: " + diy
            );
            warnPortrait = true;
            return;
        }

        Rectangle portraitRect = diy.getSettings().getRegion(key + "-marker-clip");

        final boolean obeyClip = diy.getMarkerClipping();
        Shape oldClip = null;
        if (obeyClip) {
            oldClip = g.getClip();
            g.setClip(portraitRect);
        }

        if (!isTransparent() && diy.isMarkerBackgroundFilled()) {
            BufferedImage template = getTemplateImage();
            Paint p = g.getPaint();
            g.setPaint(Color.WHITE);
            g.fillRect(0, 0, template.getWidth(), template.getHeight());
            g.setPaint(p);
        }

        Portrait p = diy.getPortrait(1);
        BufferedImage portrait = p.getImage();

        double scale = p.getScale();
        double panX = p.getPanX();
        double panY = p.getPanY();

        double scaledWidth = portrait.getWidth() * scale;
        double scaledHeight = portrait.getHeight() * scale;

        double centerX = scaledWidth / 2d;
        double centerY = scaledHeight / 2d;
        double regionX = portraitRect.getX() + portraitRect.getWidth() / 2d;
        double regionY = portraitRect.getY() + portraitRect.getHeight() / 2d;

        g.drawImage(portrait,
                (int) (regionX - centerX + panX /*+ 0.5d*/),
                (int) (regionY - centerY + panY /*+ 0.5d*/),
                (int) (scaledWidth + 0.5d),
                (int) (scaledHeight + 0.5d),
                null
        );

        if (obeyClip) {
            g.setClip(oldClip);
        }
    }

    @Override
    public boolean isTransparent() {
        return diy.getTransparentFaces();
    }

    @Override
    public boolean isVariableSize() {
        return diy.getVariableSizedFaces();
    }

    @Override
    public DeckSnappingHint getDeckSnappingHint() {
        return diy.getDeckSnappingHint();
    }

    @Override
    public double getBleedMargin() {
        return diy.getBleedMargin();
    }

    @Override
    public double[] getFoldMarks() {
        double[] marks = diy.getCustomFoldMarksInternal(index);
        if (marks == null) {
            return null;
        }
        return marks.clone();
    }

    @Override
    public boolean hasFoldMarks() {
        return diy.getCustomFoldMarksInternal(index) != null;
    }

    @Override
    public PrintDimensions getPrintDimensions() {
        // use base class for simple case
        if (!isVariableSize()) {
            return super.getPrintDimensions();
        }

        // could be anything... we'll have to render the image and then measure
        BufferedImage bi = paint(RenderTarget.PRINT, getTemplateResolution());
        double ppi = getPaintingResolution();
        return new PrintDimensions(bi, ppi);
    }

    @Override
    public MarkerStyle getMarkerStyle() {
        if (index == 2 && diy.getFaceStyle() == DIY.FaceStyle.CARD_AND_MARKER) {
            return diy.getMarkerStyle();
        }
        return null;
    }

    // flags to track warning messages that should be printed no more than once
    private boolean warnPortrait;

    @Override
    public String toString() {
        return "DIYSheet{script=" + diy.getHandlerScript() + ", templateKey=" + getTemplateKey() + ", index=" + index + '}';
    }
}
