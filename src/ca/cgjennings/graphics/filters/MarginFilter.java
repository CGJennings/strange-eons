package ca.cgjennings.graphics.filters;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

// TODO: provide a proper implementation
/**
 * This filter adds or removes space around the outside of an image. The pixel
 * values of added space depend on the {@link EdgeHandling} mode.
 *
 * <p>
 * <b>In-place filtering:</b> This class <b>does not</b> support in-place
 * filtering (the source and destination images must be different).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class MarginFilter extends AbstractImageFilter {

    private int top, left, bottom, right;
    private EdgeHandling edgeMode = EdgeHandling.ZERO;

    public MarginFilter() {
    }

    public MarginFilter(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public void setMargin(Insets i) {
        setTopMargin(i.top);
        setLeftMargin(i.left);
        setBottomMargin(i.bottom);
        setRightMargin(i.right);
    }

    public Insets getMargin() {
        return new Insets(top, left, bottom, right);
    }

    public int getBottomMargin() {
        return bottom;
    }

    public void setBottomMargin(int bottom) {
        this.bottom = bottom;
    }

    public int getLeftMargin() {
        return left;
    }

    public void setLeftMargin(int left) {
        this.left = left;
    }

    public int getRightMargin() {
        return right;
    }

    public void setRightMargin(int right) {
        this.right = right;
    }

    public int getTopMargin() {
        return top;
    }

    public void setTopMargin(int top) {
        this.top = top;
    }

    /**
     * Sets the edge handling mode to one of REPEAT, WRAP, or ZERO. The edge
     * handling mode is used to create stand-in pixel values for pixels at the
     * edge of the image, where part of the kernel would lie outside of the
     * image.
     *
     * @param edgeMode the edge handling mode
     * @throws NullPointerException if the edge handling mode is
     * <code>null</code>
     */
    public void setEdgeHandling(EdgeHandling edgeMode) {
        if (edgeMode == null) {
            throw new NullPointerException("edgeMode");
        }
        if (this.edgeMode != edgeMode) {
            this.edgeMode = edgeMode;
        }
    }

    /**
     * Returns the current edge handling mode.
     *
     * @return the edge handling mode
     */
    public EdgeHandling getEdgeHandling() {
        return edgeMode;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if (top == 0 && bottom == 0 && left == 0 && right == 0) {
            if (dest == null || dest == src) {
                return src;
            } else {
                return new CloneFilter().filter(src, dest);
            }
        }

        final int sW = src.getWidth();
        final int sH = src.getHeight();
        final int dW = sW + left + right;
        final int dH = sH + top + bottom;

        if (dW <= 0 || dH <= 0) {
            throw new IllegalArgumentException("source image too small for the given negative margins");
        }

        if (dest == null) {
            ColorModel cm = src.getColorModel();
            dest = new BufferedImage(cm, cm.createCompatibleWritableRaster(dW, dH), cm.isAlphaPremultiplied(), null);
        } else {
            if (dest.getWidth() != dW || dest.getHeight() != dH) {
                throw new IllegalArgumentException("destination image size does not match margin");
            }
            new ClearFilter().filter(dest, dest);
        }

        Graphics2D g = dest.createGraphics();
        try {
            switch (getEdgeHandling()) {
                case ZERO:
                    g.drawImage(src, top, left, null);
                    break;
                case REPEAT:
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    if (left > 0) {
                        g.drawImage(src, 0, top, left, top + sH, 0, 0, 1, sH, null);
                    }
                    if (right > 0) {
                        g.drawImage(src, left + sW, top, dW, top + sH, sW - 1, 0, sW, sH, null);
                    }
                    if (top > 0) {
                        g.drawImage(src, left, 0, left + sW, top, 0, 0, sW, 1, null);
                        if (left > 0) {
                            g.drawImage(src, 0, 0, left, top, 0, 0, 1, 1, null);
                        }
                        if (right > 0) {
                            g.drawImage(src, left + sW, 0, dW, top, sW - 1, 0, sW, 1, null);
                        }
                    }
                    if (bottom > 0) {
                        g.drawImage(src, left, top + sW, left + sW, dH, 0, sH - 1, sW, sH, null);
                        if (left > 0) {
                            g.drawImage(src, 0, top + sW, left, dH, 0, sH - 1, 1, sH, null);
                        }
                        if (right > 0) {
                            g.drawImage(src, left + sW, top + sW, dW, dH, sW - 1, sH - 1, sW, sH, null);
                        }
                    }
                    g.drawImage(src, top, left, null);
                    break;
                case WRAP:
                    int x1 = top,
                     y = left;
                    while (x1 > 0) {
                        x1 -= sW;
                    }
                    while (y > 0) {
                        y -= sH;
                    }
                    for (; y < dH; y += sH) {
                        for (int x = x1; x < dW; x += sW) {
                            g.drawImage(src, x, y, null);
                        }
                    }
                    break;
            }
        } finally {
            g.dispose();
        }

        return dest;
    }

//	public static void main( String[] args ) {
//		BufferedImage src = new BufferedImage( 3, 3, BufferedImage.TYPE_INT_RGB );
//		int[] p = new int[] {
//		 0xff0000, 0x00ffff, 0x0000ff,
//		 0xffff00, 0xffffff, 0xff00ff,
//		 0x7700ff, 0x00ff77, 0xff7700,
//		};
//		AbstractImageFilter.setARGB( src, p );
//
////		MarginFilter mf = new MarginFilter( 1, 1, 1, 1 );
//		MarginFilter mf = new MarginFilter( 3, 3, 3, 3 );
//		mf.setEdgeHandling( EdgeHandling.EXTEND );
//		final BufferedImage bi = mf.filter( src, null );
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				int w = bi.getWidth()*16;
//				int h = bi.getHeight()*16;
//				BufferedImage zoom = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
//				Graphics2D g  = zoom.createGraphics();
//				try {
//					g.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
//					g.drawImage( bi, 0, 0, w, h, null);
//				} finally {
//					g.dispose();
//				}
//				ImageViewer iv = new ImageViewer( null, zoom, true );
//				iv.setVisible( true );
//			}
//		});
//
//	}
}
