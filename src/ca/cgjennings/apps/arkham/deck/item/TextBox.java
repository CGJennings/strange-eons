package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.deck.Page;
import ca.cgjennings.apps.arkham.deck.PageView;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.layout.TextStyle;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * A text box is a rectangular page item that displays formatted markup text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
public class TextBox extends AbstractRenderedItem implements SizablePageItem, EditablePageItem, ShapeStyle, OutlineStyle {

    private String text;
    private Color background;

    private Color borderColor = Color.BLACK;
    private float borderWidth;
    private DashPattern borderDash = DashPattern.SOLID;
    private int borderJoin = BasicStroke.JOIN_ROUND;
    private LineCap borderCap = LineCap.ROUND;
    private transient Shape borderShape;

    private double margin = 4;
    private double width, height;
    private boolean shrinkToFit = true;
    private boolean justifyText = false;

    private static TextBoxEditor activeRelabelDialog;

    /**
     * Creates a new, empty text box that can be placed in a deck. The initial
     * size of the box is 72 pt by 72 pt.
     */
    public TextBox() {
        this("", 72d, 72d);
    }

    /**
     * Creates a new text box with the specified text and initial size (measured
     * in points).
     */
    public TextBox(String text, double widthInPts, double heightInPts) {
        width = widthInPts;
        height = heightInPts;
        this.text = text;
        background = Color.WHITE;
    }

    @Override
    protected double getUprightWidth() {
        return width;
    }

    @Override
    protected double getUprightHeight() {
        return height;
    }

    @Override
    public Color getFillColor() {
        return background;
    }

    @Override
    public void setFillColor(Color color) {
        if (!this.background.equals(color)) {
            this.background = color;
            clearCachedImages();
            itemChanged();
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (!this.text.equals(text)) {
            this.text = text;
            clearCachedImages();
            itemChanged();
        }
    }

    @Override
    public Icon getThumbnailIcon() {
        if (sharedIcon == null) {
            sharedIcon = ResourceKit.getIcon("deck/textbox.png");
        }
        return sharedIcon;
    }
    private static transient Icon sharedIcon = null;

    @Override
    public DragHandle[] getDragHandles() {
        if (dragHandles == null) {
            dragHandles = new DragHandle[]{
                new ResizeHandle(this, ResizeHandle.CORNER_NW),
                new ResizeHandle(this, ResizeHandle.CORNER_NE),
                new ResizeHandle(this, ResizeHandle.CORNER_SW),
                new ResizeHandle(this, ResizeHandle.CORNER_SE)
            };
        }
        return dragHandles;
    }

    @Override
    public PageItem clone() {
        TextBox tb = (TextBox) super.clone();
        tb.dragHandles = null;
        return tb;
    }

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
//		final int cap = borderJoin == BasicStroke.JOIN_ROUND ? BasicStroke.CAP_ROUND : BasicStroke.CAP_SQUARE;
//		if( borderWidth > 0f) {
//			// if the text box background is transparent, we need to clip out
//			// the part of the border that overlaps the text box; otherwise
//			// we can just paint the (opaque) text box right overtop of the
//			// border slop
//			if( background.getAlpha() == 255 ) {
//				final Paint p = g.getPaint();
//				final Stroke s = g.getStroke();
//				g.setPaint(borderColor);
//
//
//				g.setStroke(new BasicStroke(borderWidth * 2, cap, borderJoin, borderWidth, borderDash.createDashArray( borderWidth*2f ), 0f ) );
//				g.draw(getRectangle());
//				g.setStroke( s );
//				g.setPaint( p );
//			} else {
//				// a cached clipped border shape; this is nulled when the
//				// object is moved or resized or the border style (other than colour)
//				// is changed
//				if( borderShape == null ) {
//					final Rectangle2D rect = getRectangle();
//					final BasicStroke s = new BasicStroke( borderWidth * 2, cap, borderJoin, borderWidth, borderDash.createDashArray( borderWidth*2f ), 0f );
//					final Area outline = new Area( s.createStrokedShape( rect ) );
//					outline.subtract( new Area( rect ) );
//					borderShape = outline;
//				}
//
//				Paint p = g.getPaint();
//				g.setPaint( borderColor );
//				g.fill( borderShape );
//				g.setPaint( p );
//			}
//		}

        // draw text box
        Rectangle2D.Double rectangle = getRectangle();
        if (text.isEmpty()) {
            Paint p = g.getPaint();
            g.setPaint(background);
            g.fill(rectangle);
            g.setPaint(p);
        } else {
            // create text layout using AbstractRenderedItem infrastructure
            super.paint(g, target, renderResolutionHint);
        }

        if (borderWidth > 0f) {
            Paint p = g.getPaint();
            Stroke s = g.getStroke();
            g.setPaint(borderColor);
            g.setStroke(RenderingAttributeFactory.createOutlineStroke(this));
            g.draw(rectangle);
            g.setStroke(s);
            g.setPaint(p);
        }
    }

    @Override
    protected BufferedImage renderImage(RenderTarget target, double resolution) {
        StrangeEons.setWaitCursor(true);
        try {
            if ((cachedRendering == null) || (cachedResolution != resolution) || (cachedQuality2 != target)) {
                int pwidth = (int) Math.round(width * (resolution / 72d));
                int pheight = (int) Math.round(height * (resolution / 72d));
                if (pwidth < 1) {
                    pwidth = 1;
                }
                if (pheight < 1) {
                    pheight = 1;
                }

                final double margin = (this.margin + borderWidth / 2f) * (resolution / 72d);

                Color bg = getFillColor();
                cachedRendering = new BufferedImage(pwidth, pheight, bg.getAlpha() == 255 ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = cachedRendering.createGraphics();
                if (bg.getAlpha() > 0) {
                    g.setPaint(bg);
                    g.fillRect(0, 0, pwidth, pheight);
                    g.setPaint(Color.BLACK);
                }
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        (target != RenderTarget.FAST_PREVIEW)
                                ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                                : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
                );
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                MarkupRenderer r = createMarkupRenderer(resolution);
                Rectangle2D.Double textRect = new Rectangle2D.Double(margin, margin, pwidth - margin * 2, pheight - margin * 2);
                if (textRect.width >= 1d && textRect.height >= 1d) {
                    r.draw(g, textRect);
                }

                g.dispose();
                cachedResolution = resolution;
                cachedQuality2 = target;
            }
        } catch (OutOfMemoryError e) {
            clearCachedImages();
            throw e;
        } finally {
            StrangeEons.setWaitCursor(false);
        }
        return cachedRendering;
    }

    @Override
    public void clearCachedImages() {
        cachedRendering = null;
        cachedResolution = -1000d;
        cachedQuality2 = null;
        super.clearCachedImages();
    }

    /**
     * @deprecated REQUIRED FOR PRE SE3.x DECKS TO LOAD; REPLACED BY
     * cachedQuality2
     */
    @Deprecated
    private int cachedQuality;

    private transient RenderTarget cachedQuality2 = null;
    private transient double cachedResolution = -1000d;
    private transient BufferedImage cachedRendering;

    protected MarkupRenderer createMarkupRenderer(double resolution) {
        MarkupRenderer r = new MarkupRenderer(resolution);
        Sheet.doStandardRendererInitialization(r);
        TextStyle style = r.getDefaultStyle();
        style.add(
                TextAttribute.FAMILY, ResourceKit.getBodyFamily(),
                TextAttribute.SIZE, 12,
                TextAttribute.FOREGROUND, Color.BLACK);
        r.setScalingLimit(0.001d);
        r.setMarkBadBox(false);
        r.setTextFitting(shrinkToFit ? MarkupRenderer.FIT_SCALE_TEXT : MarkupRenderer.FIT_NONE);
        if (justifyText) {
            r.setAlignment(MarkupRenderer.LAYOUT_JUSTIFY);
        }
        r.setMarkupText(getText());
        return r;
    }

    @Override
    public String getName() {
        return string("de-text-box-name");
    }

    @Override
    public void setSize(double width, double height) {
        if (width < 0.01) {
            width = 0.01;
        }
        if (height < 0.01) {
            height = 0.01;
        }
        if (width != getWidth() || height != getHeight()) {
            if ((getOrientation() & 1) == 0) {
                this.width = width;
                this.height = height;
            } else {
                this.width = height;
                this.height = width;
            }
            clearCachedImages();
            borderShape = null;
            itemChanged();
        }
    }

    public boolean getShrink() {
        return shrinkToFit;
    }

    public void setShrink(boolean shrink) {
        if (shrinkToFit != shrink) {
            shrinkToFit = shrink;
            clearCachedImages();
            itemChanged();
        }
    }

    @Override
    public void setX(double x) {
        super.setX(x);
        borderShape = null;
    }

    @Override
    public void setY(double y) {
        super.setY(y);
        borderShape = null;
    }

    @Override
    public Color getOutlineColor() {
        return borderColor;
    }

    @Override
    public void setOutlineColor(Color borderColor) {
        if (borderColor == null) {
            throw new NullPointerException("null borderColor");
        }
        if (!this.borderColor.equals(borderColor)) {
            this.borderColor = borderColor;
            itemChanged();
        }
    }

    @Override
    public float getOutlineWidth() {
        return borderWidth;
    }

    @Override
    public void setOutlineWidth(float borderWidth) {
        if (this.borderWidth != borderWidth) {
            this.borderWidth = borderWidth;
            borderShape = null;
            // changing the stroke width means the text margin
            // has to be updated, too
            clearCachedImages();
            itemChanged();
        }
    }

    @Override
    public DashPattern getOutlineDashPattern() {
        return borderDash;
    }

    @Override
    public void setOutlineDashPattern(DashPattern pat) {
        if (borderDash != pat) {
            borderDash = pat;
            borderShape = null;
            itemChanged();
        }
    }

    @Override
    public LineJoin getOutlineJoin() {
        return LineJoin.fromInt(borderJoin);
    }

    @Override
    public void setOutlineJoin(LineJoin join) {
        int bj = join.toInt();
        if (this.borderJoin != bj) {
            this.borderJoin = bj;
            borderShape = null;
            itemChanged();
        }
    }

    @Override
    public void setOutlineCap(LineCap cap) {
        if (borderCap != cap) {
            this.borderCap = cap;
            borderShape = null;
            itemChanged();
        }
    }

    @Override
    public LineCap getOutlineCap() {
        return borderCap;
    }

    public static void cancelActiveEditor() {
        if (activeRelabelDialog != null) {
            activeRelabelDialog.dispose();
            activeRelabelDialog = null;
        }
    }

    @Override
    public void beginEditing() {
        Page p = getPage();
        if (p == null) {
            throw new IllegalStateException("cannot edit when not attached to page");
        }
        PageView view = p.getView();
        cancelActiveEditor();
        activeRelabelDialog = new TextBoxEditor(StrangeEons.getWindow(), this);
        activeRelabelDialog.setText(getText());

        // make the dialog cover the card
        Rectangle2D.Double labelRect = getRectangle();
        Point dlgXY = view.documentToView(labelRect.x, labelRect.y);
        // must convert width, height to a point before transformation, then back to w, h
        Point dlgWH = view.documentToView(labelRect.x + labelRect.width, labelRect.y + labelRect.height);
        dlgWH.x -= dlgXY.x;
        dlgWH.y -= dlgXY.y;

        Point screenLoc = view.getLocationOnScreen();

        // set a minimum size based on the editor windows preferred size
        Dimension pref = activeRelabelDialog.getPreferredSize();
        if (dlgWH.x < pref.width) {
            dlgWH.x = pref.width;
        }
        if (dlgWH.y < pref.height) {
            dlgWH.y = pref.height;
        }

        // restrict bounds to size of the view pane
        Rectangle bounds = new Rectangle(dlgXY.x + screenLoc.x, dlgXY.y + screenLoc.y, dlgWH.x, dlgWH.y);
        final Rectangle viewBounds = new Rectangle(screenLoc, view.getSize());
        bounds = bounds.intersection(viewBounds);
        if (bounds.isEmpty()) {
            bounds.setBounds(
                    viewBounds.x + viewBounds.width / 4,
                    viewBounds.y + viewBounds.height / 4,
                    viewBounds.width / 2, viewBounds.height / 2
            );
        }
        activeRelabelDialog.setBounds(bounds);
        activeRelabelDialog.setVisible(true);
    }

    @Override
    public void customizePopupMenu(JPopupMenu menu, PageItem[] selection, boolean isSelectionFocus) {
        if (isSelectionFocus) {
            JMenuItem edit = Commands.findCommand(menu, Commands.EDIT_PAGE_ITEM);
            if (edit != null) {
                edit.setText(string("edit-text"));
            }
        }
    }

    private static final int TEXT_BOX_VERSION = 5;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(TEXT_BOX_VERSION);

        out.writeObject(text);
        out.writeObject(background);
        out.writeDouble(margin);
        out.writeDouble(width);
        out.writeDouble(height);
        out.writeObject(borderColor);
        out.writeFloat(borderWidth);
        out.writeObject(borderDash);
        out.writeInt(borderJoin);
        out.writeObject(borderCap);
        out.writeBoolean(shrinkToFit);
        out.writeBoolean(isTextJustified());
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        int version = in.readInt();

        text = (String) in.readObject();
        background = (Color) in.readObject();
        margin = in.readDouble();
        width = in.readDouble();
        height = in.readDouble();
        borderColor = (Color) in.readObject();
        if (borderColor == null) {
            borderColor = Color.BLACK;
        }
        borderWidth = in.readFloat();

        if (version >= 4) {
            borderDash = (DashPattern) in.readObject();
        } else {
            borderDash = DashPattern.SOLID;
        }

        if (version >= 2) {
            borderJoin = in.readInt();
            if (version >= 5) {
                borderCap = (LineCap) in.readObject();
            } else {
                borderCap = (borderJoin == BasicStroke.JOIN_ROUND ? LineCap.ROUND : LineCap.SQUARE);
            }
            shrinkToFit = in.readBoolean();
        } else {
            borderJoin = BasicStroke.JOIN_ROUND;
            borderCap = LineCap.ROUND;
            shrinkToFit = true;
        }

        if (version >= 3) {
            setTextJustified(in.readBoolean());
        } else {
            setTextJustified(false);
        }

        clearCachedImages();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeImpl(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readImpl(in);
    }
    private static final long serialVersionUID = -7_618_946_764_647_050_749L;

    public boolean isTextJustified() {
        return justifyText;
    }

    public void setTextJustified(boolean justifyText) {
        if (this.justifyText != justifyText) {
            this.justifyText = justifyText;
            clearCachedImages();
            itemChanged();
        }
    }
}
