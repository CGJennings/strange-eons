package ca.cgjennings.graphics.shapes;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.Project;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import resources.CoreComponents;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * A vector image whose content is loaded from an SVG source file. Most SVG 1.1
 * files should be compatible; scripting and animation are not supported since
 * the {@link VectorImage} class represents a static image rather than a
 * dynamic, interactive object.
 *
 * <p>
 * This class requires that the
 * {@link CoreComponents#SVG_IMAGE_SUPPORT SVG_IMAGE_SUPPORT} core component be
 * installed, as that plug-in contains support code necessary to render the SVG
 * content. If that library is not installed, but the {@code SVGVectorImage}
 * constructor is called from the event dispatch thread (EDT), then the user
 * will be given the opportunity to immediately download and install the missing
 * component. If the user chooses not to, or if the constructor is called from a
 * thread other than the EDT, then a
 * {@link resources.CoreComponents.MissingCoreComponentException MissingCoreComponentException}
 * exception will be thrown from the constructor.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class SVGVectorImage extends AbstractVectorImage {

    private VectorFileBridge bridge;
    private static Method bridgeMethod;

    private synchronized static Method getBridgeMethod() {
        if (bridgeMethod == null) {
            try {
                Class<?> bridgeClass = Class.forName("ca.cgjennings.graphics.shapes.BatikSVGBridge");
                bridgeMethod = bridgeClass.getMethod("bridge", URL.class);
            } catch (ClassNotFoundException ex) {
                // expected when SVG core not installed
            } catch (NoSuchMethodException nsm) {
                StrangeEons.log.log(Level.SEVERE, null, nsm);
            }
        }
        return bridgeMethod;
    }

    /**
     * Creates a vector image from SVG content stored in a file.
     *
     * @param file the file to load content from
     * @throws IOException if an I/O error occurs while loading the content
     * @throws IllegalArgumentException if the vector content is invalid
     * @throws resources.CoreComponents.MissingCoreComponentException if the SVG
     * support library is not installed and the user chooses not to install it
     */
    public SVGVectorImage(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        init(file.toURI().toURL());
    }

    /**
     * Creates a vector image from SVG content stored at a URL.
     *
     * @param location the URL to load content from
     * @throws IOException if an I/O error occurs while loading the content
     * @throws IllegalArgumentException if the vector content is invalid
     * @throws resources.CoreComponents.MissingCoreComponentException if the SVG
     * support library is not installed and the user chooses not to install it
     */
    public SVGVectorImage(URL location) throws IOException {
        if (location == null) {
            throw new NullPointerException("location");
        }

        // Batik does not accept res: and project: URLs, so we need to translate
        // these into equivalent URLs that Batik knows how to deal with before
        // passing them on.
        final String protocol = location.getProtocol();
        if ("res".equals(protocol)) {
            URL original = location;
            location = ResourceKit.composeResourceURL(location.toString());
            if (location == null) {
                throw new FileNotFoundException(original.toString());
            }
        } else if ("project".equals(protocol)) {
            URL original = location;
            location = null;
            Project op = StrangeEons.getOpenProject();
            if (op != null) {
                Member m = op.findMember(original);
                if (m != null) {
                    try {
                        location = m.getFile().toURI().toURL();
                    } catch (MalformedURLException mue) {
                        // shouldn't happen, but will throw FNF below
                    }
                }
            }
            if (location == null) {
                throw new FileNotFoundException(original.toString());
            }
        }

        init(location);
    }

    /**
     * Initializes the vector image by creating a {@link VectorFileBridge} for
     * the content at the URL. (The bridge provides the minimum interface
     * necessary to render the underlying vector content.) This requires a
     * separate plug-in; if the plug-in is not available and the call is being
     * made from the EDT, an offer is made to download the plug-in. If the call
     * is made from outside of the EDT and the library is not available, a
     * {@link CoreComponents.MissingCoreComponentException} exception is thrown.
     *
     * @param source the source URL; if necessary, this must already be
     * transformed into a form acceptable to the underlying SVG library
     * @throws IOException if an I/O error occurs while loading the content
     * @throws resources.CoreComponents.MissingCoreComponentException if the SVG
     * support library is not installed and the user chooses not to install it
     */
    private void init(URL source) throws IOException {
        Method svgBridge = getBridgeMethod();
        if (svgBridge == null) {
            try {
                if (EventQueue.isDispatchThread()) {
                    CoreComponents.SVG_IMAGE_SUPPORT.validate();
                    svgBridge = getBridgeMethod();
                }
                if (svgBridge == null) {
                    throw new CoreComponents.MissingCoreComponentException(string("core-info"));
                }
            } catch (CoreComponents.MissingCoreComponentException mcc) {
                // Can be thrown if not in EDT or user refuses to validate;
                // in either case we just want a chance to log more info
                StrangeEons.log.warning("SVG core component not installed");
                throw mcc;
            }
        }
        try {
            try {
                bridge = (VectorFileBridge) svgBridge.invoke(null, source);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                StrangeEons.log.log(Level.SEVERE, null, ex);
                throw ex;
            } catch (InvocationTargetException ex) {
                if (ex.getCause() != null) {
                    throw ex.getCause();
                } else {
                    throw ex;
                }
            }

            Rectangle2D bounds = bridge.bounds();
            tx = -bounds.getX();
            ty = -bounds.getY();
            iw = bounds.getWidth();
            ih = bounds.getHeight();
        } catch (IOException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new IOException("Invalid SVG content", t);
        }
    }

    @Override
    protected void render(Graphics2D g) {
        try {
            if (bridge != null) {
                bridge.render(null, g);
            }
        } catch (Throwable t) {
            StrangeEons.log.log(Level.WARNING, "uncaught vector rendering exception", t);
        }
    }

    @Deprecated
    @Override
    protected void finalize() throws Throwable {
        try {
            if (bridge != null) {
                bridge.dispose();
                bridge = null;
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Returns {@code true} if the SVG core library is installed. Note that it
     * is not required to call this before attempting to create an SVG image. If
     * possible, the SVG core will be loaded on demand (see the class
     * description for details).
     *
     * @return {@code true} if SVG image file support is available
     */
    public static boolean isSupported() {
        return getBridgeMethod() != null;
    }

    /**
     * ***************************************************************************
     ** What follows is an implementation for the SVG Salamander library should
     * ** * one wish to switch. It is much smaller (~10%) than the Batik
     * libraries, ** * but it does not support as much of the SVG spec, is less
     * likely to be ** * kept up to date, and did not render content as well in
     * my testing. **
     * ***************************************************************************
     */

    /*
	private void init( URL source ) throws IOException {
		SVGUniverse u = new SVGUniverse();
		URI uri = u.loadSVG( source );
		svg = u.getDiagram( uri );
		try {
			Rectangle2D bounds = svg.getRoot().getShape().getBounds2D();//BoundingBox();
			tx = -bounds.getX();
			ty = -bounds.getY();
			iw = bounds.getWidth();
			ih = bounds.getHeight();
		} catch( Exception e ) {
			throw new IllegalArgumentException( "invalid vector data", e );
		}
	}

	@Override
	protected void render( Graphics2D g ) {
		try {
			svg.render( g );
		} catch( SVGException e ) {
			StrangeEons.log.log( Level.WARNING, "uncaught vector rendering exception", e );
		}
	}
     */
    /**
     * ***************************************************************************
     ** End of SVG Salamander implementation. **
     * ***************************************************************************
     */
}
