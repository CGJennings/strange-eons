package resources;

import ca.cgjennings.algo.SplitJoin;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.ComponentMetadata;
import ca.cgjennings.apps.arkham.component.FileRecoveryException;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.conversion.ConversionSession;
import ca.cgjennings.apps.arkham.component.conversion.UpgradeConversionTrigger;
import ca.cgjennings.apps.arkham.deck.DeckDeserializationSupport;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import static ca.cgjennings.apps.arkham.dialog.ErrorDialog.displayError;
import ca.cgjennings.apps.arkham.dialog.InsertCharsDialog;
import ca.cgjennings.apps.arkham.dialog.Messenger;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog.VersioningState;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectFolderDialog;
import ca.cgjennings.apps.arkham.project.Task;
import ca.cgjennings.apps.arkham.project.TaskGroup;
import ca.cgjennings.graphics.FilteredMultiResolutionImage;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.MultiResolutionImageResource;
import ca.cgjennings.graphics.shapes.AbstractVectorImage;
import ca.cgjennings.graphics.shapes.SVGVectorImage;
import ca.cgjennings.graphics.shapes.VectorIcon;
import ca.cgjennings.graphics.shapes.VectorImage;
import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.io.InvalidFileFormatException;
import ca.cgjennings.io.SEObjectInputStream;
import ca.cgjennings.io.SEObjectOutputStream;
import ca.cgjennings.layout.TextStyle;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.AnimatedIcon;
import ca.cgjennings.ui.FileNameExtensionFilter;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.theme.Palette;
import ca.cgjennings.ui.theme.TaskIcon;
import ca.cgjennings.ui.theme.Theme;
import ca.cgjennings.ui.theme.ThemeInstaller;
import ca.cgjennings.ui.theme.ThemedGlyphIcon;
import ca.cgjennings.ui.theme.ThemedIcon;
import ca.cgjennings.ui.theme.ThemedImageIcon;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.StyleContext;
import static resources.Language.string;

/**
 * The resource kit manages a virtual resource folder that combines resources
 * from many sources (main application, installed plug-in bundles, project
 * folder, etc.) into a single virtual file system. Resources in this file
 * system are identified using a relative URL-like syntax with three parts (two
 * of which are optional):
 *
 * <p>
 * <tt>[/] [folder/]* file.ext</tt>
 * <ol>
 * <li>The first part is the least used and most technical:<br>
 * If the first character is a slash (/), then the identifier is absolute;
 * otherwise the rest of the name is relative to {@code /resources}. (Resources
 * are normally stored in the {@code resources} folder of the file system;
 * however, sometimes it is useful to access a resource that is in the classpath
 * of the application or a plug-in bundle. This can be done using an absolute
 * identifier.)
 * <li>The second part consists of zero or more folder names separated by
 * slashes.
 * <li>The third part, the only part which must be present, is the file
 * identifier. Note that a path that ends in a folder name instead of a file
 * name is not considered valid. The result of using such an identifier is
 * undefined.
 * </ol>
 *
 * In order to locate the actual resource file for a resource name, call
 * {@link #composeResourceURL} with the identifier. This will return a URL that
 * can be used to access the resource, or {@code null} if the resource cannot be
 * found.
 *
 * <p>
 * Alternatively, you can create a URL object for a resource using the special
 * protocol {@code res:}, for example:<br>
 * {@code URL u = new URL( "res://icons/application/app.png" );}
 *
 * <p>
 * Note that the URLs produced with these two different methods are <b>not</b>
 * the same. A {@code res:} URL is a URL for the virtual file system, while
 * {@code composeResourceURL} returns a URL for the actual location of the file.
 * In fact, when you try to access a {@code res:} URL's data using a method like
 * {@code openStream}, the protocol handler will itself call
 * {@code composeResourceURL} to find the file in question.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 1.0
 */
public class ResourceKit {

    private static final String KEY_EXPERIMENTAL_FEATURES = "enable-experimental-features";
    private static final String KEY_USE_ALL_PRINT_SERVICES = "print-enable-all-print-services";

    /**
     * Returns a string that describes the specific build of this release of the
     * application. This method is here only for backwards compatibility with
     * old plug-ins.
     *
     * @deprecated The complex microversion string has been replaced by a simple
     * build number that is easier to compare when enabling version-dependent
     * features. To obtain the build number, call
     * {@link StrangeEons#getBuildNumber}.
     *
     * @return the microversion string for this build (the build number as a
     * {@code String})
     */
    @Deprecated
    public static String getMicroversionString() {
        return String.valueOf(StrangeEons.getBuildNumber());
    }

    /**
     * This class cannot be instantiated.
     */
    private ResourceKit() {
    }
    
    /**
     * Returns an estimate of the desktop scaling factor of the primary display.
     * @return the scaling factor, or 1 if no scaling appears to be applied
     */
    public static double estimateDesktopScalingFactor() {
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        return dpi > 96 ? (double) dpi / 96d : 1d;
    }

    /**
     * Returns the URL of a resource from a path and file relative to a
     * conceptual resources folder. The conceptual resources folder typically
     * represents many different virtual folders, including the resources folder
     * of the main application and that of any installed plug-in bundles. If the
     * requested file cannot be found, {@code null} will be returned.
     *
     * <p>
     * The contents of the virtual resources folder are determined by searching
     * for the requested file in the following locations (in order until a file
     * is found):
     * <ol>
     * <li>If the resource path is actually an absolute URL then it is converted
     * to a URL object and returned, except that if it is a {@code res://} URL
     * the procedure continues as if the {@code res://} prefix was not present.
     * Absolute URLs are indicated by the presence of a colon (:) in the
     * identifier.
     * <li>The contents of the user resource folder, if any.
     * <li>The {@code resources} folders of the main application and all
     * installed plug-in bundles, in the order of installation.
     * <li>The {@code resources} folder (if any) of all task folders in the open
     * project. Note that this URL is typically only valid while the project is
     * open.
     * </ol>
     *
     * <p>
     * <b>Folders:</b> Resource identifiers can include relative paths, but the
     * final part of the identifier must name a file, not a directory. The
     * result of attempting to compose a resource URL that points to a
     * directory, or of attempting to use such a URL should one be returned, if
     * undefined.
     *
     * <p>
     * <b>Case Sensitivity:</b> Because the virtual file system actually reads
     * its files from a mixture of sources, file names may or may not be case
     * sensitive depending on where the actual file is located. Therefore, you
     * should always assume that resource identifiers are case sensitive.
     *
     * @param resource a file path relative to the virtual resources folder
     * @return a URL that can be used to access the named resource, or
     * {@code null}
     */
    public static URL composeResourceURL(String resource) {

        // 0. Normalize res:// URLs
        resource = normalizeResourceIdentifier(resource);

        // 1. Check for absolute/res:// URLs
        if (resource.indexOf(':') >= 0) {
            // NOTE: this CANNOT be a res:// URL because it has been normalized
            // (This condition must hold, as otherwise when the res:// protocol
            // handler calls this method to decode a res:// URL, it would enter an
            // infinite loop.)
            try {
                // most likely an explicit project URL; try to connect so that
                // if the file does not exist we can return null
                URL u = new URL(resource);
                try {
                    u.openConnection().connect();
                } catch (IOException e) {
                    return null;
                }
                return u;
            } catch (MalformedURLException e) {
                StrangeEons.log.log(Level.WARNING, "could not create absolute URL from \"{0}\"", resource);
                return null;
            }
        }

        // 2. Check if the user is using a custom resource folder that contains
        //    the requested file.
        if (userResourceFolder != null) {
            String relativeFile = (resource.startsWith("/") ? (".." + resource) : resource)
                    .replace('/', File.separatorChar);
            File f = new File(userResourceFolder, relativeFile);
            if (f.exists()) {
                try {
                    return f.toURI().toURL();
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        }

        // 3. Check if the resource exists on the class path, either the
        //    main application or an installed plug-in.
        URL url = ResourceKit.class.getResource(resource);
        if (url != null) {
            return url;
        }

        // 4. If there is an open project, search it for task folders that
        //    contain a "resources" folder and try to locate the resource
        //    relative to those folders.
        StrangeEonsAppWindow af = StrangeEons.getWindow();
        if (af == null || af.getOpenProject() == null) {
            return null;
        }
        Project proj = af.getOpenProject();
        if (proj == null) {
            return null;
        }

        List<TaskGroup> stack = new LinkedList<>();
        stack.add(proj);
        while (!stack.isEmpty()) {
            TaskGroup tg = stack.remove(0);
            if (tg.hasChildren()) {
                int kids = tg.getChildCount();
                for (int k = 0; k < kids; ++k) {
                    Member m = tg.getChildAt(k);
                    if (m instanceof Task) {
                        Member resFolder = m.findChild("resources");
                        if (resFolder != null) {
                            String resourceRelative = resource.startsWith("/") ? (".." + resource) : resource;
                            File f = new File(resFolder.getFile(), resourceRelative);
                            if (f.exists()) {
                                // create a project: url from the file---we could just
                                // return the file, but the image cache takes project:
                                // URLs as a hint not to cache the file
                                return proj.findMember(f).getURL();
                            }
                        }
                    }
                    if (m instanceof TaskGroup) {
                        stack.add((TaskGroup) m);
                    }
                }
            }
        }

        // 5. Resource not found
        return null;
    }

    private static final File userResourceFolder;

    static {
        // app is null when loading ResKit from, e.g., a GUI builder
        StrangeEons app = StrangeEons.getApplication();
        userResourceFolder = app == null ? null : app.getCommandLineArguments().resfolder;
    }
    
    /**
     * Returns a resource path for a resource that is relative
     * to the specified class.
     * 
     * @param base the class that the resource is stored relative to
     * @param relativePathToResource the path to the resource relative to the class
     * @return the resource path of the resource (whether or not it exists)
     */
    public static String getIdentifier(Class<?> base, String relativePathToResource) {
        String path = "/" + base.getPackageName().replace('.', '/') + '/' + relativePathToResource;
        return normalizeResourceIdentifier(path);
    }

    /**
     * Returns a normalized version of the identifier. Because
     * {@link #composeResourceURL} accepts both virtual resource paths and
     * regular URLs, a given resource file can be accessed by multiple
     * identifiers. This method reduces some (though not all) redundant names to
     * their plain virtual resource path equivalent, if one exists. The intended
     * purpose is to improve the efficiency of caching mechanisms or other cases
     * where it could be useful to quickly detect that two different identifier
     * strings actually refer to the same resource.
     * <i>Most plug-in developers will never have a reason to call this
     * method.</i>
     *
     * <p>
     * The precise effect of this method may change from version to version, but
     * the following three conditions are guaranteed:
     * <ol>
     * <li>If the identifier is {@code null}, it will be returned unchanged.
     * <li>If the identifier is a virtual resource path rather than an absolute
     * URL, it will be returned unchanged and the method will return very
     * quickly.
     * <li>If the identifier is an absolute URL identifier, then it <i>may</i>
     * be changed to a virtual resource path that points to the same file.
     * <li>Any two identifiers that (a) are changed by this method and (b) refer
     * to the same file will be changed to the same string.
     * </ol>
     *
     * @param resource an identifier to normalize
     * @return an equivalent (possibly identical) identifier
     */
    public static String normalizeResourceIdentifier(String resource) {
        if (resource == null) {
            return resource;
        }
        int len = resource.length();
        if (len < 4) {
            return resource;
        }
        if (resource.charAt(0) != 'r' || resource.charAt(1) != 'e' || resource.charAt(2) != 's' || resource.charAt(3) != ':') {
            return resource;
        }

        // res URLs are allowed any number of slashes at start, though either 2
        // (relative) or 3 (absolute) are normal. We need to count how many there
        // are, and then keep 1 of them if the count is odd (so result is still absolute)
        int beginIndex = 4; //N.B. 4 is even, therefore parity of beginIndex will equal parity of # of slashes
        while (beginIndex < len && resource.charAt(beginIndex) == '/') {
            ++beginIndex;
        }
        beginIndex -= beginIndex & 1;

        return resource.substring(beginIndex);
    }

    /**
     * Returns {@code true} if a resource file is not expected to change during
     * the lifetime of the application. This can be used as a hint to caching
     * mechanisms that the resource is safe to cache. For example, resources
     * that come from an installed plug-in bundle can be cached because the
     * bundle contents cannot change while the application is still running,
     * while resources that come from a project must be handled more carefully
     * because the user might modify them at any time.
     *
     * @param composedResourceURL the URL of the resource in question
     * @return {@code true} if the resource cannot change while the application
     * is running
     * @since 3.0
     */
    public static boolean isResourceStatic(URL composedResourceURL) {
        if (composedResourceURL == null) {
            return false;
        }

        String p = composedResourceURL.getProtocol();

        // If this is a res:// URL, then we can't tell yet: we need to
        // resolve it into a non-res:// URL to determine the real source.
        if (p.equals("res")) {
            composedResourceURL = composeResourceURL(composedResourceURL.toExternalForm());
            // If null, then the real source was not found:
            // return false in case the resource exists later
            if (composedResourceURL == null) {
                return false;
            }
            p = composedResourceURL.getProtocol();
        }

        // We never want to cache project URLs, because we expect the user
        // to change them. We always want to cache jar URLs, because they
        // from the main app or a linked plug-in bundle, so they can't change.
        // Other URLs, like file or http are less obvious. By default we
        // also cache these.
        if (cacheAggressively) {
            // cache everything except project URLs
            return !p.equals("project");
        } else {
            // only cache jar URLs
            return p.equals("jar");
        }
    }
    private static boolean cacheAggressively = true;

    /**
     * Returns an input stream that can be used to read from the specified
     * resource.
     *
     * @param resource the relative location of a resource file
     * @return an input stream that reads bytes from the resource
     * @throws FileNotFoundException if the resource does not exist
     * @throws IOException if an I/O error occurs while opening the stream
     * @throws NullPointerException if the specified resource is {@code null}
     */
    public static InputStream getInputStream(String resource) throws IOException {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        URL url = composeResourceURL(resource);
        if (url == null) {
            throw new FileNotFoundException(resource);
        }
        return url.openStream();
    }

    /**
     * Returns an icon created from an image resource. By default, this method
     * looks for the icon in the application's "icons" resource folder. Plug-ins
     * that wish to load an icon from a different folder can override this by
     * using a full "res://" resource descriptor or by starting the resource
     * with a '/' and providing the full path to the resource. For example to
     * load the icon from the resource "foo/icon.png", you could use either
     * "res://foo/icon.png" or "/resources/foo/icon.png".
     * <p>
     * The returned icon is a {@link ThemedImageIcon} instance. This means that
     * the currently installed {@link Theme} will be given an opportunity to
     * modify the resource location, modify the returned image before it is
     * converted into an icon, or both.
     *
     * @param iconResource the resource identifier for the icon
     * @return an icon consisting of the requested image or a theme-dependent
     * substitute
     * @throws NullPointerException if {@code iconResource} is {@code null}
     * @see Theme#applyThemeToImage
     */
    public static ThemedIcon getIcon(String iconResource) {
        ThemedIcon icon;
        
        // see if this resource has been mapped to a new location or type
        iconResource = getIconMapping(iconResource);
        
        // see if this resource ends in a query parameter and if so extract it
        String query = null;
        {
            int question = iconResource.lastIndexOf('?');
            if (question >= 0) {
                query = iconResource.substring(question + 1);
                iconResource = iconResource.substring(0, question);
            }
        }
        
        // check for a glyph-based icon descriptor
        if (iconResource.startsWith(ThemedGlyphIcon.GLYPH_RESOURCE_PREFIX)) {
            icon = new ThemedGlyphIcon(iconResource);
        } else {
            // relative to icons directory by default
            if (iconResource.charAt(0) != '/' && iconResource.indexOf(':') < 0) {
                if (!iconResource.startsWith("icons/")) {
                   iconResource = "icons/" + iconResource;
                }
            }
            icon = new ThemedImageIcon(iconResource);
        }
        return applyIconQuery(icon, query);
    }
    
    private static ThemedIcon applyIconQuery(ThemedIcon icon, String query) {
        if (query != null) {
            switch (query) {
                case "task":
                    icon = new TaskIcon(icon);
                    break;
                default:
                    StrangeEons.log.warning("unknown icon query ?" + query);
            }
        }
        return icon;
    }

    private static String getIconMapping(String iconResource) {
        String normalized = iconResource;
        // toolbar/h1.png -> toolbar/h1
        if (normalized.endsWith(".png") || normalized.endsWith(".jpg")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        // icons/toolbar/h1 -> toolbar/h1
        if (normalized.startsWith("icons/")) {
            normalized = normalized.substring(6);
        } else if (normalized.startsWith("res://icons/")) {
            normalized = normalized.substring(12);
        } else if (normalized.startsWith("/resources/icons/")) {
            normalized = normalized.substring(17);
        }
        String mapping = iconMap.get(normalized);
        if (mapping == null && !normalized.startsWith("/")) {
            int justTheFileName = normalized.lastIndexOf('/');
            if (justTheFileName >= 0) {
                mapping = iconMap.get(normalized.substring(justTheFileName + 1));
            }
        }
        return mapping == null ? iconResource : mapping;
    }

    private static Map<String, String> iconMap;

    static {
        iconMap = new HashMap<>();
        try (InputStream in = ResourceKit.class.getResourceAsStream("icons/map.properties")) {
            EscapedLineReader elr = new EscapedLineReader(in);
            String[] pair;
            while ((pair = elr.readProperty()) != null) {
                iconMap.put(pair[0], pair[1]);
            }
        } catch (IOException | RuntimeException ioe) {
            StrangeEons.log.log(Level.SEVERE, "unable to load icon map", ioe);
        }
    }

    /**
     * Returns an image after allowing the installed theme an opportunity to
     * modify it to match the interface theme. This should not be used
     * on images that will be drawn on game components!
     *
     * @param resource the image resource to theme
     * @return the themed version of the image
     */
    public static BufferedImage getThemedImage(String resource) {
        BufferedImage bi = getImage(resource);
        Theme t = ThemeInstaller.getInstalledTheme();
        if (t != null) {
            bi = t.applyThemeToImage(bi);
        }
        return bi;
    }

    /**
     * Given the path to a resource file containing an image, returns the image.
     * If it exists and the file is valid, the returned image will use either
     * the <tt>INT_RGB</tt> or <tt>INT_ARGB</tt> formats. (Or, if the setting
     * <tt>premultiply-image-alpha</tt> is {@code true}, then a premultiplied
     * equivalent.)
     *
     * <p>
     * The resource file must either be an image encoded in one of the supported
     * file types (PNG, JPEG, or JPEG2000) or else a script file with the
     * extension
     * <tt>.js</tt> that contains a resource creation script. (See
     * {@link ScriptMonkey#runResourceCreationScript}.)
     *
     * <p>
     * If the file does not exist or is not a valid image file (or resource
     * script), then a special "missing image" image is returned to indicate
     * this visually to the user.
     *
     * <p>
     * Note that images may be cached, so if the resource is modified then the
     * returned result may not reflect these changes.
     *
     * @param resource the relative file path of the image resource
     * @return the image stored at the named resource, or a substitute missing
     * image symbol
     * @throws NullPointerException if the resource identifier is {@code null}
     * @throws IllegalArgumentException if the resource identifier is empty
     * @see #isResourceStatic
     */
    public static BufferedImage getImage(final String resource) {
        StrangeEons.setWaitCursor(true);
        BufferedImage result = null;
        try {
            synchronized (imageCache) {
                result = fetchImageUnsafe(resource, true, true);
            }
            if (result == null) {
                result = getMissingImage();
                ErrorDialog.displayErrorOnce(resource, string("rk-err-image-resource", resource), null);
            }
        } catch (IOException e) {
            ErrorDialog.displayErrorOnce(resource, string("rk-err-image-resource", resource), e);
        } finally {
            StrangeEons.setWaitCursor(false);
        }
        return result;
    }

    /**
     * This method returns an image as if by {@link #getImage}, but without
     * obvious feedback for the user. The wait cursor is not set while loading
     * the image, and an error dialog is not displayed if the attempt to load
     * the image fails.
     *
     * @param resource the image resource path
     * @return an image for the resource path
     * @throws NullPointerException if the resource identifier is {@code null}
     * @throws IllegalArgumentException if the resource identifier is empty
     */
    public static BufferedImage getImageQuietly(String resource) {
        BufferedImage bi = null;
        synchronized (imageCache) {
            try {
                bi = fetchImageUnsafe(resource, true, true);
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, "unable to read image " + resource, e);
            }
        }
        if (bi == null) {
            bi = getMissingImage();
            
            Throwable ex = null;
            if (StrangeEons.getReleaseType() == StrangeEons.ReleaseType.DEVELOPMENT) {
                ex = new FileNotFoundException(resource).fillInStackTrace();
            }

            StrangeEons.log.log(Level.WARNING, "image resource not found: " + resource, ex);
        }
        return bi;
    }

    /**
     * Returns an array of image resources for the provided names. Each name
     * will be looked up and the matching image found as if by calling
     * {@link #getImageQuietly(java.lang.String)} for each name in turn, and
     * placing the result at the matching index in the output array. The benefit
     * of getting images with this method is that they can be loaded
     * concurrently if the system has multiple processors (CPUs or cores).
     *
     * <p>
     * The supplied output array may be {@code null}, in which case a new array
     * will be created. If a non-{@code null} output array is given, it must be
     * at least as long as the number of names provided.
     *
     * @param inputResources image resources to load
     * @param outputImages an array to be filled in with the resulting images
     * @return the output array that holds the images
     * @throws NullPointerException if any resource identifier is {@code null}
     * @throws IllegalArgumentException if any resource identifier is empty
     */
    public static BufferedImage[] getImagesQuietly(String[] inputResources, BufferedImage[] outputImages) {
        if (inputResources == null) {
            throw new NullPointerException("names");
        }
        if (outputImages == null) {
            outputImages = new BufferedImage[inputResources.length];
        }
        if (inputResources.length > outputImages.length) {
            throw new IllegalArgumentException("outputImages.length must be at least " + inputResources.length);
        }

        synchronized (imageCache) {
            if (Runtime.getRuntime().availableProcessors() == 1) {
                for (int i = 0; i < inputResources.length; ++i) {
                    outputImages[i] = getImageQuietly(inputResources[i]);
                }
                return outputImages;
            }

            // check if everything is cached already before starting threads
            boolean allCached = true;
            for (int i = 0; i < inputResources.length; ++i) {
                if (inputResources[i] == null) {
                    throw new NullPointerException("inputNames[" + i + "]");
                }
                if (inputResources[i].isEmpty()) {
                    throw new IllegalArgumentException("inputNames[" + i + "]");
                }
                outputImages[i] = fetchCachedImageUnsafe(inputResources[i]);
                if (outputImages[i] == null) {
                    allCached = false;
                }
            }
            if (allCached) {
                return outputImages;
            }

            boolean[] cache = new boolean[inputResources.length];
            ParallelImageLoader[] tasks = new ParallelImageLoader[inputResources.length];
            for (int i = 0; i < inputResources.length; ++i) {
                tasks[i] = new ParallelImageLoader(i, inputResources, outputImages, cache);
            }
            try {
                SplitJoin.getInstance().run(tasks);
                for (int i = 0; i < inputResources.length; ++i) {
                    if (outputImages[i] == null) {
                        outputImages[i] = getMissingImage();
                    }
                    if (cache[i]) {
                        imageCache.put(inputResources[i], new SoftReference<>(outputImages[i]));
                    }
                }
            } catch (ExecutionException e) {
                StrangeEons.log.log(Level.WARNING, "parallel loader threw uncaught exception", e.getCause());
            }
        }
        return outputImages;
    }

    private static final class ParallelImageLoader implements Runnable {

        private final int i;
        private final String[] in;
        private final BufferedImage[] out;
        private final boolean[] cache;

        public ParallelImageLoader(int i, String[] in, BufferedImage[] out, boolean[] cache) {
            this.i = i;
            this.in = in;
            this.out = out;
            this.cache = cache;
        }

        @Override
        public void run() {
            BufferedImage bi = null;
            try {
                if (out[i] != null) {
                    return;
                }
                bi = fetchImageUnsafe(in[i], false, false);
                if (in[i].endsWith(".js")) {
                    cache[i] = true;
                } else {
                    URL u = composeResourceURL(in[i]);
                    if (u != null) {
                        cache[i] = isResourceStatic(u);
                    }
                }
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, "unable to read image " + in[i], e);
            }
            out[i] = bi;
        }
    }

    /**
     * Returns an image resource, from the image cache if possible. This method
     * does not lock the image cache, so it must already be locked by a
     * higher-level method.
     *
     * @param name the image resource to load
     * @param checkCache if {@code true}, check in the cache first
     * @param allowCaching if {@code true}, then the result can be cached
     * @return the image, or if the image can't be loaded, the missing image (or
     * {@code null})
     * @throws IOException if an I/O error occurs while reading the image
     */
    private static BufferedImage fetchImageUnsafe(String name, boolean checkCache, boolean allowCaching) throws IOException {
        name = normalizeResourceIdentifier(name); // improves cache performance

        BufferedImage bi;
        if (checkCache) {
            bi = fetchCachedImageUnsafe(name);
            if (bi != null) {
                return bi;
            }
        } else {
            bi = null;
        }

        // not cached; load or create
        boolean cacheResult;
        if (name.endsWith(".js")) {
            bi = (BufferedImage) ScriptMonkey.runResourceCreationScript(name);
            cacheResult = allowCaching;
        } else {
            URL url = composeResourceURL(name);

            // EVIL HACK
            // the resource doesn't exist; if it is a true res:// location
            // and the file extension is not .jp2, try loading with a .jp2
            // as the image may have been converted (e.g., older deck files
            // use .png monster movement arrows)
            if (url == null) {
                if (!name.endsWith(".jp2") && name.indexOf(':') < 0) {
                    String altName;
                    int dot = name.lastIndexOf('.');
                    if (dot >= 0) {
                        altName = name.substring(0, dot) + ".jp2";
                    } else {
                        altName = name + ".jp2";
                    }
                    bi = fetchImageUnsafe(altName, checkCache, allowCaching);
                    if (bi != null) {
                        StrangeEons.log.log(Level.INFO, "replaced missing resource with {0}", altName);
                    }
                }
                return bi;
            }

            bi = ImageIO.read(url);
            cacheResult = allowCaching && isResourceStatic(url);
        }

        if (bi != null) {
            bi = prepareNewImage(bi);
            if (cacheResult) {
                imageCache.put(name, new SoftReference<>(bi));
            }
        }

        return bi;
    }

    /**
     * Returns an image if it is in the image cache; otherwise returns
     * {@code null}. This method does not lock the image cache, so it must
     * already be locked by a higher-level method.
     */
    private static BufferedImage fetchCachedImageUnsafe(String name) {
        BufferedImage image = null;
        Reference<BufferedImage> cachedReference = imageCache.get(name);
        if (cachedReference != null) {
            image = cachedReference.get();
        }
        return image;
    }

    /**
     * Returns the stand-in image that is used when an image cannot be loaded.
     *
     * @return an image that provides a visual cue that the intended image is
     * not available
     */
    public synchronized static BufferedImage getMissingImage() {
        if (missingImage == null) {
            missingImage = getMissingVectorImage().createRasterImage(64, 64, true);
        }
        return missingImage;
    }
    private static BufferedImage missingImage;

    /**
     * Prepares a newly obtained image for use by ensuring that it is in the
     * image format required by the application. For example, when an image is
     * read from a file it may be returned as whatever type of image was the
     * most convenient for the file format. This method will check the format of
     * an image, convert it to the optimal format if necessary, and then return
     * it. If no conversion is required, the original image is returned. Note
     * that images that you obtain via the resource kit or other parts of the
     * Strange Eons API will already be in the correct format.
     *
     * @param image the image to prepare
     * @return an image in the correct format; either the original or a
     * converted one, or {@code null} if {@code image} was {@code null}
     */
    public static BufferedImage prepareNewImage(Image image) {
        if (image == null) {
            return null;
        }

        BufferedImage bi;
        if (image instanceof BufferedImage) {
            bi = (BufferedImage) image;
        } else {
            bi = ImageUtilities.imageToBufferedImage(image);
        }

        if (PREMULTIPLY && bi.getTransparency() != Transparency.OPAQUE) {
            bi = ImageUtilities.ensurePremultipliedFormat(bi);
        } else {
            bi = ImageUtilities.ensureIntRGBFormat(bi);
        }
        return bi;
    }
    private static final boolean PREMULTIPLY = Settings.getShared().getYesNo("premultiply-image-alpha");

    private static final Map<String, SoftReference<BufferedImage>> imageCache = Collections.synchronizedMap(
            new HashMap<>(299)
    );

    /**
     * Given the path to a resource file containing a vector image, returns the
     * image. The resource file must be in the SVG format. Note that there are
     * some limitations on the supported SVG features: SVG files with scripts
     * are not supported at all, and filter effects are not completely
     * supported.
     *
     * <p>
     * If the specified file does not exist or is not valid, a special "missing
     * image" image is returned.
     *
     * @param resource the vector image resource to load
     * @return a vector image created from the content of the resource, or the
     * "missing image" vector image
     * @since 3.0
     */
    public static VectorImage getVectorImage(String resource) {
        return vectorCache.get(resource);
    }

    private static final AbstractResourceCache<String, VectorImage> vectorCache = new AbstractResourceCache<String, VectorImage>(VectorImage.class, "Vector image cache") {
        @Override
        protected String canonicalizeIdentifier(String identifier) {
            return normalizeResourceIdentifier(identifier);
        }

        @Override
        protected boolean allowCaching(String canonicalIdentifier, VectorImage loadedResource) {
            return isResourceStatic(composeResourceURL(canonicalIdentifier));
        }

        @Override
        protected VectorImage loadResource(String canonicalIdentifier) {
            URL url = composeResourceURL(canonicalIdentifier);

            // Batik can't handle project URLs as a URI
            if (url.getProtocol().equals("project")) {
                URL newURL = null;
                Project p = StrangeEons.getOpenProject();
                if (p != null) {
                    Member m = p.findMember(url);
                    if (m != null) {
                        try {
                            newURL = m.getFile().toURI().toURL();
                        } catch (MalformedURLException ex) {
                        }
                    }
                }
                if (newURL == null) {
                    return ResourceKit.getMissingVectorImage();
                }
                url = newURL;
            }

            VectorImage vi = null;
            if (url != null) {
                try {
                    vi = new SVGVectorImage(url);
                } catch (Exception e) {
                    StrangeEons.log.log(Level.WARNING, "error loading vector image: " + canonicalIdentifier, e);
                }
            }
            if (vi == null) {
                vi = getMissingVectorImage();
            }
            return vi;
        }
    };

    /**
     * Returns the vector image used as a stand-in when a requested resource
     * cannot be loaded.
     *
     * @return the "missing image" vector image
     */
    public static synchronized VectorImage getMissingVectorImage() {
        if (missingVector == null) {
            missingVector = new AbstractVectorImage() {
                {
                    tx = ty = 1;
                    iw = ih = 17;
                }

                @Override
                protected void render(Graphics2D g) {
                    Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Stroke os = g.getStroke();
                    Paint op = g.getPaint();
                    BasicStroke s = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
                    g.setStroke(s);
                    g.setPaint(Palette.get.pastel.opaque.pink);
                    g.fillRect(0, 0, 15, 15);
                    g.setPaint(Color.BLACK);
                    g.drawRect(0, 0, 15, 15);
                    g.setPaint(Palette.get.dark.opaque.red);
                    g.drawLine(3, 3, 12, 12);
                    g.drawLine(3, 12, 12, 3);
                    g.setPaint(op);
                    g.setStroke(os);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
                }
            };
        }
        return missingVector;
    }

    private static VectorImage missingVector;

    /**
     * Returns an icon that displays a vector image at the specified width and
     * height. The icon size can be changed later using methods in the returned
     * {@link VectorIcon}.
     *
     * @param resource the image resource to create an icon of
     * @param iconWidth the initial width of the icon, in pixels
     * @param iconHeight the initial height of the icon, in pixels
     * @return an new icon that paints the specified image
     * @see #getVectorImage
     */
    public static VectorIcon getVectorIcon(String resource, int iconWidth, int iconHeight) {
        VectorImage vi = getVectorImage(resource);
        return new VectorIcon(vi, iconWidth, iconHeight, true);
    }

    ////////////////////////
    //                    //
    // FONT METHODS       ////////////////////////////////////////////////////
    //                    //
    ////////////////////////
    /**
     * Load core fonts: only call if the core typefaces bundle is installed.
     * Normally called from {@link #getBodyFamily}.
     */
    private synchronized static void initCoreFonts() {
        FontRegistrationResult[] results = null;
        try {
            RawSettings.loadGlobalSettings("fonts/core/core-font-table.settings");

            // check if the font doesn't support enough glyphs for the
            // selected game language
            boolean useSubst = false;
            String subst = Settings.getUser().get("use-generic-serif-body-for-languages");
            if (subst != null) {
                String lang = Language.getGameLocale().getLanguage();
                for (String s : subst.split("\\s*,\\s*")) {
                    if (lang.equals(s)) {
                        useSubst = true;
                        break;
                    }
                }
            }
            if (useSubst) {
                coreBodyFamily = Font.SERIF;
                coreBodyFont = new Font(Font.SERIF, Font.PLAIN, 10);
                return;
            }

            String value = RawSettings.getSetting("core-fonts-body");
            results = registerFontFamily(value);
        } catch (Exception e) {
            StrangeEons.log.log(Level.SEVERE, "failed to load core body fonts", e);
        }

        if (results != null && results.length > 0) {
            coreBodyFamily = results[0].getFamily();
            coreBodyFont = results[0].getFont();
        }
        // else getBodyFamily() will init fallback family and font

        // the fallback font is huge and takes several seconds to load
        // so we'll do it in another thread
        new Thread() {
            @Override
            public void run() {
                try {
                    Font lrglyphs = getFont(RawSettings.getSetting("core-fonts-lr"), 24f);
                    InsertCharsDialog.addFallbackFont(lrglyphs);
                } catch (Throwable t) {
                    StrangeEons.log.log(Level.SEVERE, null, t);
                }
            }
        }.start();
    }

    /**
     * Returns a {@code Font} instance for the <i>standard body font</i>: this
     * is the default Times-like serif font that is the default font for markup
     * on game components. It is normally accessed by its family name via
     * {@link #getBodyFamily()} rather than by {@code Font}. Please see the
     * documentation for that method for further details.
     *
     * @return an instance of the standard body font
     * @see #getBodyFamily()
     */
    public synchronized static Font getBodyFont() {
        getBodyFamily();
        return coreBodyFont;
    }

    /**
     * Returns the family name of the <i>standard body font</i>: this is the
     * default Times-like serif font that is the default font for markup on game
     * components. If the core typefaces library is installed, then this will
     * return the core body font from that library. Otherwise, it will return a
     * serif font that depends on the platform and available system fonts. (It
     * is strongly recommended that the core typefaces library be installed so
     * that game components will appear consistent across platforms.)
     *
     * @return the family name of the standard body font
     */
    public synchronized static String getBodyFamily() {
        if (coreBodyFamily != null) {
            return coreBodyFamily;
        }

        if (CoreComponents.CORE_TYPEFACES.getInstallationState() != VersioningState.NOT_INSTALLED) {
            initCoreFonts();
        } else {
            Catalog.addPostInstallationHook(new Runnable() {
                @Override
                public void run() {
                    if (CoreComponents.CORE_TYPEFACES.getInstallationState() != VersioningState.NOT_INSTALLED) {
                        Catalog.removePostInstallationHook(this);
                        initCoreFonts();
                    }
                }

                @Override
                public String toString() {
                    return "core font loader";
                }
            });
        }
        if (coreBodyFamily == null) {
            coreBodyFamily = findAvailableFontFamily("Linux Libertine", Font.SERIF);
            coreBodyFont = new Font(coreBodyFamily, Font.PLAIN, 10);
        }
        return coreBodyFamily;
    }

    private static Font coreBodyFont;
    private static String coreBodyFamily;

    /**
     * Returns the default font for editing markup in a text field.
     *
     * @return the font to use for text editing
     */
    public synchronized static Font getEditorFont() {
        if (editorFont == null) {
            Font baseFont;
            String family = RawSettings.getSetting("edit-font-family");
            family = family == null ? "" : family.trim();
            if (family.isEmpty() || family.equalsIgnoreCase("default")) {
                if (PlatformSupport.PLATFORM_IS_MAC) {
                    baseFont = locateAvailableFont("Menlo", "Monaco", "Consolas", Font.MONOSPACED);
                } else {
                    baseFont = locateAvailableFont("Consolas", Font.MONOSPACED);
                }
            } else {
                baseFont = locateAvailableFont(family, Font.MONOSPACED);
            }
            Settings rk = Settings.getShared();
            editorFont = baseFont.deriveFont(
                    (rk.getYesNo("edit-font-bold") ? Font.BOLD : 0)
                    | (rk.getYesNo("edit-font-italic") ? Font.ITALIC : 0),
                    rk.getPointSize("edit-font", 12f));
        }
        return editorFont;
    }
    private static Font editorFont;

    private static Font locateAvailableFont(String... families) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        if (families == null | families.length == 0) {
            throw new IllegalArgumentException("missing families");
        }
        Font font = null;
        for (int i = 0; i < families.length; ++i) {
            font = sc.getFont(families[i], Font.PLAIN, 13);
            if (families[i].equals(font.getFamily())) {
                break;
            }
        }
        if (font == null) {
            font = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        }
        return font;
    }

    /**
     * Returns a font that is optimized for displaying characters at very small
     * size. This is useful for dynamically creating small icons or other UI
     * elements. The returned font is only guaranteed to support the basic ASCII
     * character set, and might not include distinct lower case letters.
     *
     * @return a font optimized for small size
     */
    public static synchronized Font getTinyFont() {
        if (localeIconFont == null) {
            InputStream in = null;
            try {
                in = ResourceKit.composeResourceURL("fonts/tiny/slkscr.ttf").openStream();
                localeIconFont = Font.createFont(Font.TRUETYPE_FONT, in);
                localeIconFont = localeIconFont.deriveFont(8f);
            } catch (Exception e) {
                localeIconFont = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
                StrangeEons.log.warning("unable to load standard tiny font; using substitute");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return localeIconFont;
    }
    private static Font localeIconFont;

    /**
     * Creates a single font from a file in the application resources. The font
     * is not registered, meaning that it is not visible to the rest of the
     * application if looked up by its family name.
     *
     * @param resource the font resource file
     * @return an instance of the font at the requested point size
     * @throws NullPointerException if the resource is {@code null}
     * @throws InvalidFileFormatException if the resource is not a supported
     * font file format or the font is corrupt
     * @throws FileNotFoundException if the resource file does not exist
     * @throws IOException if an I/O error occurs while creating the font
     * @see #registerFont(java.lang.String)
     * @see #registerFontFamily(java.lang.String)
     */
    public static Font getFont(String resource, float pointSize) throws IOException {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        resource = normalizeResourceIdentifier(resource);

        synchronized (fontCache) {
            Font f = fontCache.get(resource);
            if (f != null) {
                if (f.getSize2D() == pointSize) {
                    return f;
                }
                return f.deriveFont(pointSize);
            }

            URL url = composeResourceURL(resource);
            if (url == null) {
                throw new FileNotFoundException(resource);
            }

            int deducedType = Font.TYPE1_FONT;
            int len = resource.length();
            if (len >= 4) {
                String ext = resource.substring(len - 4, len);
                if (ext.equalsIgnoreCase(".ttf") || ext.equalsIgnoreCase(".otf")) {
                    deducedType = Font.TRUETYPE_FONT;
                }
            }

            InputStream in = null;
            try {
                URLConnection c = url.openConnection();
                in = c.getInputStream();
                f = Font.createFont(deducedType, in);
                if (c.getContentLengthLong() != -1L) {
                    cachedFontsSize += c.getContentLengthLong();
                }

            } catch (FontFormatException e) {
                throw new InvalidFileFormatException("bad font file format", e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }

            f = f.deriveFont(pointSize);
            fontCache.put(resource, f);

            StrangeEons.log.log(Level.INFO, "loaded font {0} ({1}, {2} glyphs)", new Object[]{resource, f.getFontName(), f.getNumGlyphs()});

            return f;
        }
    }

    /**
     * Attempts to register a single font. The font is first loaded from the
     * resource file as if by using {@link #getFont}. If this is successful,
     * then an attempt is made to register the font. A registered font can be
     * created by its family name just as if it were installed in the user's
     * operating system. For example, it can be used in markup boxes with the
     * {@code &lt;family&gt;} tag or the font family text attribute. (If
     * registration fails, it is usually because a font with the same
     * name&mdash;possibly the same font&mdash;is installed on the user's
     * system.)
     *
     * @param resourceFile the font resource file to register
     * @return an object that provides access to the font, its family name, and
     * the result of registration
     * @throws NullPointerException if the resource is {@code null}
     * @throws InvalidFileFormatException if the resource is not a supported
     * font file format or the font is corrupt
     * @throws FileNotFoundException if the resource file does not exist
     * @throws IOException if an I/O error occurs while creating the font
     * @see #getFont
     * @see #registerFontFamily
     */
    public static FontRegistrationResult registerFont(String resourceFile) throws IOException {
        if (resourceFile == null) {
            throw new NullPointerException("resourceName");
        }

        synchronized (fontRegistry) {
            FontRegistrationResult result = fontRegistry.get(resourceFile);
            if (result != null) {
                return result;
            }

            Font unregisteredFont = getFont(resourceFile, 10f);
            boolean registered = GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(unregisteredFont);
            if (registered) {
                // See also FontRegistrationResult
//				registeredFamilies.add( unregisteredFont.getFamily() );
                registeredFamilies.add(unregisteredFont.getFontName());
            }
            result = new FontRegistrationResult(unregisteredFont, registered);
            fontRegistry.put(resourceFile, result);
            if (registered) {
                StrangeEons.log.log(Level.INFO, "registered font: {0} ({1})", new Object[]{resourceFile, unregisteredFont.getFontName()});
            } else {
                StrangeEons.log.log(Level.WARNING, "font registration failed: {0} ({1})", new Object[]{resourceFile, unregisteredFont.getFontName()});
            }
            return result;
        }
    }

    private static final HashMap<String, Font> fontCache = new HashMap<>();
    private static long cachedFontsSize;

    private static final HashMap<String, FontRegistrationResult> fontRegistry = new HashMap<>();
    private static final HashSet<String> registeredFamilies = new HashSet<>();

    /**
     * Attempts to register a group of font resources as a single family. (For
     * example, the regular, bold, italic, and bold italic versions of the
     * font.) The method takes a string containing a comma-separated list of
     * resource files; each resource file is one of the fonts to register. The
     * first entry in the list should be the regular variant of the file; the
     * order of the remaining resources does not matter. Each resource in the
     * list will be loaded and registered as if by
     * {@link #registerFont(java.lang.String)}, and an array of registration
     * results is returned. Typically, the caller will want to get the name of
     * the font to use for {@link TextStyle}s by calling
     * {@code ResourceKit.registerFontList( families )[0].getFamily()}.
     *
     * <p>
     * After the first entry in the list, subsequent entries will be assumed to
     * be located in the same resource folder as the first entry if they do not
     * include a '/' character. For example, the following list would load a
     * family of two fonts located in the <tt>foo/fonts</tt>
     * resource folder: <tt>foot/fonts/bar_regular.ttf, bar_italic.ttf</tt>.
     *
     * @param commaSeparatedList the list of resource files to register
     * @return an array of results, one for each registered resource in the same
     * order as listed by the caller
     * @throws NullPointerException if the list is {@code null}
     * @throws InvalidFileFormatException if any resource is not a supported
     * font file format or the font is corrupt
     * @throws FileNotFoundException if any resource file does not exist
     * @throws IOException if an I/O error occurs while creating a font
     * @see #registerFont
     */
    public static FontRegistrationResult[] registerFontFamily(String commaSeparatedList) throws IOException {
        if (commaSeparatedList == null) {
            throw new NullPointerException("commaSeparatedList");
        }
        String[] resources = commaSeparatedList.trim().split("\\s*,\\s*");

        FontRegistrationResult[] results = new FontRegistrationResult[resources.length];
        if (resources.length == 0) {
            StrangeEons.log.warning("font resource list is empty");
            return results;
        }

        // complete path of entries after the first
        int pathPos = resources[0].lastIndexOf('/');
        if (pathPos >= 0) {
            String path = resources[0].substring(0, pathPos + 1);
            for (int i = 1; i < resources.length; ++i) {
                if (resources[i].indexOf('/') < 0) {
                    resources[i] = path + resources[i];
                }
            }
        }

        // register each resource and check for OS X bug
        for (int i = 0; i < resources.length; ++i) {
            results[i] = registerFont(resources[i]);
        }
        return results;
    }

    /**
     * Returns {@code true} if one or more fonts with the specified family name
     * have been registered through {@link #registerFont}.
     *
     * @param familyName the family name of the font to check
     * @return {@code true} if at least one font with that family name has been
     * registered successfully
     * @throws NullPointerException if the family name is {@code null}
     */
    public static boolean isFamilyRegistered(String familyName) {
        if (familyName == null) {
            throw new NullPointerException("familyName");
        }
        return registeredFamilies.contains(familyName);
    }

    /**
     * Returns a font instance that is equivalent to {@code font} but which will
     * default to using kerning and ligatures (if available for the font and if
     * supported by the particular rendering method). Note that this method is
     * not needed for markup text drawn on components, as the markup system
     * already provides its own support for kerning and ligatures, and it looks
     * up fonts by family name rather than {@code Font} instance.
     *
     * @param font the font to modify
     * @return a font instance that is equivalent to {@code font} but will use
     * kerning and ligatures when possible
     * @throws NullPointerException if the font is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Font enableKerningAndLigatures(Font font) {
        if (font == null) {
            throw new NullPointerException("font");
        }
        Map attrMap = font.getAttributes();
        if (attrMap.get(TextAttribute.KERNING) == TextAttribute.KERNING_ON
                && attrMap.get(TextAttribute.LIGATURES) == TextAttribute.LIGATURES_ON) {
            return font;
        }

        attrMap = new HashMap();
        attrMap.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        attrMap.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        return font.deriveFont(attrMap);
    }

    ////////////////////////
    //                    //
    // CACHE METHODS       ////////////////////////////////////////////////////
    //                    //
    ////////////////////////
    /**
     * Registers a cache's metrics instance to make it available for lookup.
     *
     * @param cm the metrics instance to register
     * @throws NullPointerException if the metrics instance is {@code null}
     */
    public static void registerCacheMetrics(CacheMetrics cm) {
        if (cm == null) {
            throw new NullPointerException("cm");
        }
        synchronized (metricRegistry) {
            metricRegistry.add(cm);
        }
    }

    /**
     * Unregisters a cache's metrics instance. Most caches exist for the
     * lifetime of the application and are never unregistered.
     *
     * @param cm the metrics instance to unregister
     */
    public static void unregisterCacheMetrics(CacheMetrics cm) {
        synchronized (metricRegistry) {
            metricRegistry.remove(cm);
        }
    }

    /**
     * Returns an array of the currently registered cache metrics instances,
     * sorted by name (that is, their {@code toString()} value).
     *
     * @return an array of registered cache metrics instances
     */
    public static CacheMetrics[] getRegisteredCacheMetrics() {
        CacheMetrics[] cm;
        synchronized (metricRegistry) {
            cm = metricRegistry.toArray(CacheMetrics[]::new);
        }
        final Collator coll = Language.getInterface().getCollator();
        Arrays.sort(cm, (CacheMetrics o1, CacheMetrics o2) -> coll.compare(o1.toString(), o2.toString()));
        return cm;
    }

    private static final HashSet<CacheMetrics> metricRegistry;

    static {
        metricRegistry = new HashSet<>();
        synchronized (metricRegistry) {
            registerCacheMetrics(new CacheMetrics() {
                @Override
                public int getItemCount() {
                    int count = 0;
                    synchronized (imageCache) {
                        Set<String> keys = imageCache.keySet();
                        for (String key : keys) {
                            if (imageCache.get(key).get() != null) {
                                ++count;
                            }
                        }
                    }
                    return count;
                }

                @Override
                public long getByteSize() {
                    long bytes = 0;
                    synchronized (imageCache) {
                        Set<String> keys = imageCache.keySet();
                        for (String key : keys) {
                            BufferedImage bi = imageCache.get(key).get();
                            if (bi != null) {
                                bytes += bi.getWidth() * bi.getHeight() * 4; // 1 int per pixel
                            }
                        }
                    }
                    return bytes;
                }

                @Override
                public void clear() {
                    synchronized (imageCache) {
                        imageCache.clear();
                    }
                }

                @Override
                public Class getContentType() {
                    return BufferedImage.class;
                }

                @Override
                public boolean isClearSupported() {
                    return true;
                }

                @Override
                public String toString() {
                    return "Image cache";
                }

                @Override
                public String status() {
                    return String.format(
                            "%,d images (%,d KiB)", getItemCount(), (getByteSize() + 512L) / 1024L
                    );
                }
            });

            registerCacheMetrics(new CacheMetrics() {
                @Override
                public int getItemCount() {
                    synchronized (fontCache) {
                        return fontCache.size();
                    }
                }

                @Override
                public long getByteSize() {
                    synchronized (fontCache) {
                        return cachedFontsSize;
                    }
                }

                @Override
                public void clear() {
                    // must be done in this order to match the order used
                    // when registering fonts
                    synchronized (fontRegistry) {
                        synchronized (fontCache) {
                            fontCache.clear();
                            cachedFontsSize = 0L;
                            fontRegistry.clear();
                        }
                    }
                }

                @Override
                public Class getContentType() {
                    return Font.class;
                }

                @Override
                public boolean isClearSupported() {
                    return false;
                }

                @Override
                public String toString() {
                    return "Font cache";
                }

                @Override
                public String status() {
                    return String.format(
                            "%,d fonts (%,d KiB)", getItemCount(), (getByteSize() + 512L) / 1024L
                    );
                }
            });

            registerCacheMetrics(vectorCache.createCacheMetrics(true));
        }
    }

    //
    // Error Handling
    //
    public static void missingKeyError(String key) {
        IllegalArgumentException e = new IllegalArgumentException(key);
        StrangeEons.log.log(Level.WARNING, string("rk-err-missing-key", key));
        displayError(string("rk-err-missing-key", key), null);
    }

    private static JFileChooser openFileChooser;
    private static JFileChooser saveFileChooser;
    private static JFileChooser exportFileChooser;
    private static JFileChooser imageFileChooser;
    private static JFileChooser scriptFileChooser;
    private static JFileChooser projectFileChooser;
    private static JFileChooser folderFileChooser;

    private static FileFilter fileFilter = new FileNameExtensionFilter(string("rk-filter-eon"), "eon");
    private static FileFilter exportFileFilter = new FileNameExtensionFilter(string("rk-filter-zip"), "zip");
    private static FileFilter bitmapImageFileFilter = new FileNameExtensionFilter(string("rk-filter-image"), "jp2", "png", "jpg", "jpeg", "gif", "bmp");
    private static FileFilter bitmapVectorFileFilter = new FileNameExtensionFilter(string("rk-filter-image"), "jp2", "png", "jpg", "jpeg", "svg", "svgz", "gif", "bmp");
    private static FileFilter scriptFileFilter = new FileNameExtensionFilter(string("rk-filter-script"), "js");
    private static FileFilter pluginFileFilter = new FileNameExtensionFilter(string("rk-filter-plugin"), "seplugin", "seext");
    private static final FileFilter projectFileFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                try {
                    if (Project.isProjectFolder(f)) {
                        return true;
                    }
                } catch (IOException e) {
                    return false;
                }
                File test = f;
                while (test != null) {
                    if (Task.isTaskFolder(f)) {
                        return false;
                    }
                    test = test.getParentFile();
                }
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return string("rk-filter-project");
        }
    };

    /**
     * This method is called during startup to allow the resource kit to set up
     * its shared file dialogs. This method is public only to cross a package
     * boundary. There is no need for user code to call this method.
     */
    public static void initializeFileDialogs() {
        JUtilities.threadAssert();
        if (openFileChooser != null) {
            return;
        }

        openFileChooser = createFileChooser("default-open-folder");
        new ca.cgjennings.ui.fcpreview.GameComponentPreviewer(openFileChooser);
        saveFileChooser = createFileChooser("default-save-folder");
        exportFileChooser = createFileChooser("default-export-folder");
        imageFileChooser = createFileChooser("default-image-folder");
        new ca.cgjennings.ui.fcpreview.ImagePreviewer(imageFileChooser);
        scriptFileChooser = createFileChooser("default-script-folder");
        projectFileChooser = createFileChooser("default-project-folder");
        projectFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        projectFileChooser.setAcceptAllFileFilterUsed(false);
        projectFileChooser.setFileFilter(projectFileFilter);
        folderFileChooser = createFileChooser("default-folder-folder");
        folderFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderFileChooser.setAcceptAllFileFilterUsed(true);
    }

    /**
     * Displays a file dialog for opening a single game component.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showOpenDialog(Component parent) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        String verb = string("rk-dialog-open");
        openFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        openFileChooser.setDialogTitle(verb);
        openFileChooser.setFileFilter(fileFilter);
        openFileChooser.setMultiSelectionEnabled(false);
        openFileChooser.setApproveButtonText(verb);

        if (openFileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            saveDefaultFolder(openFileChooser);
            return openFileChooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Displays a file dialog for opening multiple game components.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @return the selected files (empty if no file was selected)
     */
    public static File[] showMultiOpenDialog(Component parent) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        String verb = string("rk-dialog-open");
        openFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        openFileChooser.setDialogTitle(verb);
        openFileChooser.setFileFilter(fileFilter);
        openFileChooser.setApproveButtonText(verb);
        openFileChooser.setMultiSelectionEnabled(true);

        if (openFileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            saveDefaultFolder(openFileChooser);
            return openFileChooser.getSelectedFiles();
        }

        return null;
    }

    /**
     * Displays a file dialog for saving a single game component. If the
     * selected file exists, the user must confirm whether they wish to replace
     * the existing file.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @param baseName an optional suggested file name
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showSaveDialog(Component parent, String baseName) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        String verb = string("rk-dialog-save");
        saveFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        saveFileChooser.setDialogTitle(verb);
        saveFileChooser.setFileFilter(fileFilter);
        saveFileChooser.setSelectedFile(new File(baseName));
        saveFileChooser.setApproveButtonText(verb);

        File selectedFile;
        boolean askAgain;
        do {
            askAgain = false;
            if (saveFileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            selectedFile = saveFileChooser.getSelectedFile();
            if (selectedFile.exists()) {
                int confirm = confirmFileReplace(selectedFile);
                if (confirm == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
                if (confirm == JOptionPane.NO_OPTION) {
                    askAgain = true;
                }
            }
        } while (askAgain);

        saveDefaultFolder(saveFileChooser);
        return selectedFile;
    }

    /**
     * Displays a file dialog for opening a single image file.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @return the selected file, or {@code null} if no file was selected
     * @since 1.5
     */
    public static File showImageFileDialog(Component parent) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        imageFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        imageFileChooser.setFileFilter(SVGVectorImage.isSupported() ? bitmapVectorFileFilter : bitmapImageFileFilter);
        imageFileChooser.setDialogTitle(string("rk-dialog-portrait"));
        imageFileChooser.setApproveButtonText(string("rk-dialog-open"));

        if (imageFileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            saveDefaultFolder(imageFileChooser);
            return imageFileChooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Displays a file dialog for opening a single image file that will accept
     * only bitmap image formats (such as JPEG) and not vector image formats
     * (such as SVG).
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @return the selected file, or {@code null} if no file was selected
     * @since 3.0
     */
    public static File showBitmapImageFileDialog(Component parent) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        imageFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        imageFileChooser.setFileFilter(bitmapImageFileFilter);
        imageFileChooser.setDialogTitle(string("rk-dialog-portrait"));
        imageFileChooser.setApproveButtonText(string("rk-dialog-open"));

        if (imageFileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            saveDefaultFolder(imageFileChooser);
            return imageFileChooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Displays a file dialog for exporting content to a ZIP archive. If the
     * selected file exists, the user must confirm whether they wish to replace
     * the existing file.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @param baseName an optional suggested file name
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showZipFileDialog(Component parent, String baseName) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        exportFileChooser.setDialogTitle(string("rk-dialog-zip"));
        exportFileChooser.setSelectedFile(new File(makeStringFileSafe(baseName.replace(" ", "") + ".zip")));
        exportFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        exportFileChooser.setFileFilter(exportFileFilter);
        exportFileChooser.setApproveButtonText(string("rk-dialog-export"));

        File selectedFile;
        boolean askAgain;
        do {
            askAgain = false;
            if (exportFileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            selectedFile = exportFileChooser.getSelectedFile();
            if (selectedFile.exists()) {
                int confirm = confirmFileReplace(selectedFile);
                if (confirm == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
                if (confirm == JOptionPane.NO_OPTION) {
                    askAgain = true;
                }
            }
        } while (askAgain);

        saveDefaultFolder(exportFileChooser);
        return selectedFile;
    }

    /**
     * Displays a file dialog for saving a file. If the selected file exists,
     * the user must confirm whether they wish to replace the existing file.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @param base the base location or file
     * @param fileTypeDescription a description of the file type
     * @param fileNameExtensions typical file extensions for this file type
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showGenericSaveDialog(Component parent, File base, String fileTypeDescription, String... fileNameExtensions) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        if (fileNameExtensions == null) {
            throw new NullPointerException("fileExtensions");
        }
        if (fileNameExtensions.length == 0) {
            throw new IllegalArgumentException("fileExtensions is empty");
        }

        String verb = string("rk-dialog-save");
        saveFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        saveFileChooser.setDialogTitle(verb);
        saveFileChooser.setSelectedFile(base);
        saveFileChooser.setFileFilter(new FileNameExtensionFilter(fileTypeDescription, fileNameExtensions));
        saveFileChooser.setApproveButtonText(verb);

        File selectedFile;
        boolean askAgain;
        do {
            askAgain = false;
            if (saveFileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            selectedFile = saveFileChooser.getSelectedFile();
            if (selectedFile.exists() && !selectedFile.equals(base)) {
                int confirm = confirmFileReplace(selectedFile);
                if (confirm == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
                if (confirm == JOptionPane.NO_OPTION) {
                    askAgain = true;
                }
            }
        } while (askAgain);

        saveDefaultFolder(saveFileChooser);
        return selectedFile;
    }

    /**
     * Displays a file dialog for opening a file.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @param base the base location or file
     * @param fileType a description of the file type
     * @param fileExtensions typical file extensions for this file type
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showGenericOpenDialog(Component parent, File base, String fileType, String... fileExtensions) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        String verb = string("rk-dialog-open");
        saveFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        saveFileChooser.setDialogTitle(verb);
        saveFileChooser.setSelectedFile(base);
        saveFileChooser.setFileFilter(new FileNameExtensionFilter(fileType, fileExtensions));
        saveFileChooser.setApproveButtonText(verb);

        if (saveFileChooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        return saveFileChooser.getSelectedFile();
    }

    /**
     * Displays a file dialog for exporting a file. If the selected file exists,
     * the user must confirm whether they wish to replace the existing file.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @param basename the base location or file
     * @param fileType a description of the file type
     * @param fileExtension typical file extension for this file type
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showGenericExportFileDialog(Component parent, String basename, String fileType, String fileExtension) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        exportFileChooser.setDialogTitle(string("rk-dialog-export"));
        exportFileChooser.setSelectedFile(new File(makeStringFileSafe(basename.replace(" ", "") + "." + fileExtension)));
        exportFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        exportFileChooser.setFileFilter(new FileNameExtensionFilter(fileType, fileExtension));
        exportFileChooser.setApproveButtonText(string("rk-dialog-export"));

        File selectedFile;
        boolean askAgain;
        do {
            askAgain = false;
            if (exportFileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            selectedFile = exportFileChooser.getSelectedFile();
            if (selectedFile.exists()) {
                int confirm = confirmFileReplace(selectedFile);
                if (confirm == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
                if (confirm == JOptionPane.NO_OPTION) {
                    askAgain = true;
                }
            }
        } while (askAgain);

        saveDefaultFolder(exportFileChooser);
        return selectedFile;
    }

    /**
     * Displays a file dialog for opening a script file.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showScriptFileDialog(Component parent) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        scriptFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        scriptFileChooser.setFileFilter(scriptFileFilter);
        scriptFileChooser.setDialogTitle(string("rk-dialog-script"));
        scriptFileChooser.setApproveButtonText(string("rk-dialog-open"));

        if (scriptFileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            saveDefaultFolder(scriptFileChooser);
            return scriptFileChooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Displays a file dialog for opening a plug-in bundle file.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showPluginFileDialog(Component parent) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }
        scriptFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        scriptFileChooser.setFileFilter(pluginFileFilter);
        scriptFileChooser.setDialogTitle(string("rk-dialog-plugin"));
        scriptFileChooser.setApproveButtonText(string("rk-dialog-open"));

        if (scriptFileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            saveDefaultFolder(scriptFileChooser);
            return scriptFileChooser.getSelectedFile();
        }
        return null;
    }

    /**
     * Displays a file dialog for opening a project folder.
     *
     * @param parent a parent component for the dialog; may be {@code null} in
     * which case the main application window is used
     * @return the selected file, or {@code null} if no file was selected
     */
    public static File showProjectFolderDialog(Component parent) {
        initializeFileDialogs();
        if (parent == null) {
            parent = StrangeEons.getWindow();
        }

        // use PFD if enabled
        if (ProjectFolderDialog.isFolderDialogEnabled()) {
            final String KEY = getFolderKey(projectFileChooser);
            ProjectFolderDialog d = new ProjectFolderDialog(StrangeEons.getWindow(), ProjectFolderDialog.Mode.SELECT_PROJECT);

            if (RawSettings.getUserSetting(KEY) != null) {
                File folder = new File(RawSettings.getUserSetting(KEY));
                while (folder != null && !folder.isDirectory()) {
                    folder = folder.getParentFile();
                }
                d.setSelectedFolder(folder);
            } else {
                d.setSelectedFolder(null);
            }

            File folder = d.showDialog();

            if (folder != null) {
                RawSettings.setUserSetting(KEY, folder.getAbsolutePath());
                RawSettings.writeUserSettings();
            }
            return folder;
        }

        // otherwise use default dialog
        projectFileChooser.setDialogTitle(string("prj-dc-open"));
        if (projectFileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File f = projectFileChooser.getSelectedFile();
            try {
                if (!Project.isProjectFolder(f)) {
                    return null;
                }
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, null, e);
                return null;
            }
            projectFileChooser.setSelectedFile(f.getParentFile());
            saveDefaultFolder(projectFileChooser);
            projectFileChooser.ensureFileIsVisible(f);
            return f;
        }
        return null;
    }

    public static File showFolderDialog(Component parent, String title, String okText) {
        folderFileChooser.setDialogTitle(title);
        folderFileChooser.setApproveButtonText(okText);
        if (folderFileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File f = folderFileChooser.getSelectedFile();
            saveDefaultFolder(folderFileChooser);
            return f;
        }
        return null;
    }

    private static int confirmFileReplace(File f) {
        return JOptionPane.showConfirmDialog(StrangeEons.getWindow(),
                string("fd-confirm", f.getName()), "", JOptionPane.YES_NO_CANCEL_OPTION);
    }

    /**
     * Shows a print dialog for a print job. This method may display either the
     * native dialog (that allows selection from native print services) or a
     * custom dialog (that allows selection from non-native print services)
     * depending on application preference settings.
     *
     * @param job the printer job to be used for printing
     * @return {@code true} if the user confirms that printing should proceed
     */
    public static boolean showPrintDialog(PrinterJob job) {
        return job.printDialog(createPrintRequestAttributeSet(job));
    }

    /**
     * Creates a new, mutable print request attribute set based on the
     * application preference settings.
     *
     * @param job the printer job to create an attribute set for; may be
     * {@code null}
     * @return a print attribute set that reflects user preferences
     * @since 3.0
     */
    public static PrintRequestAttributeSet createPrintRequestAttributeSet(PrinterJob job) {
        HashPrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
        Settings s = Settings.getShared();

        if (!s.getBoolean("use-default-print-owner")) {
            try {
                final Attribute o = getDialogOwnerAttribute();
                if (o != null) {
                    attr.add(o);
                }
            } catch (Throwable t) {
                // probably a non-Sun JDK
            }
        }

        if (job != null) {
            attr.add(new javax.print.attribute.standard.JobName(job.getJobName(), null));
        }

        if (s.getBoolean(KEY_USE_ALL_PRINT_SERVICES)) {
            attr.add(javax.print.attribute.standard.DialogTypeSelection.COMMON);
        } else {
            attr.add(javax.print.attribute.standard.DialogTypeSelection.NATIVE);
        }

        return attr;
    }

    private static Attribute getDialogOwnerAttribute() {
        // try to find the dialog owner attribute, which is Sun API in Java 7
        if (!DLG_OWNER_ATTRIBUTE_TRIED) {
            DLG_OWNER_ATTRIBUTE_TRIED = true;
            try {
                DLG_OWNER_ATTRIBUTE = (Attribute) Class.forName("sun.print.DialogOwner")
                        .getConstructor(Frame.class).newInstance(StrangeEons.getWindow());
            } catch (Throwable t) {
                // Sun API not found, leave as null and we'll ignore
            }
        }
        return DLG_OWNER_ATTRIBUTE;
    }
    private static boolean DLG_OWNER_ATTRIBUTE_TRIED;
    private static Attribute DLG_OWNER_ATTRIBUTE;

    /**
     * Given one of the resource kit file choosers, return its settings key.
     *
     * @param d a file chooser known to {@code ResourceKit}
     * @return that chooser's settings key
     */
    private static String getFolderKey(JFileChooser d) {
        String key = null;
        if (d == openFileChooser) {
            key = "default-open-folder";
        } else if (d == saveFileChooser) {
            key = "default-save-folder";
        } else if (d == exportFileChooser) {
            key = "default-export-folder";
        } else if (d == imageFileChooser) {
            key = "default-image-folder";
        } else if (d == scriptFileChooser) {
            key = "default-script-folder";
        } else if (d == projectFileChooser) {
            key = "default-project-folder";
        } else if (d == folderFileChooser) {
            key = "default-folder-folder";
        } else {
            throw new AssertionError("Unknown dialog for getting folder key: " + d);
        }
        return key;
    }

    /**
     * Stores the current location of the file chooser to that choosers settings
     * key.
     *
     * @param d the chooser to save the folder for
     */
    private static void saveDefaultFolder(JFileChooser d) {
        File dir = d.getCurrentDirectory();
        if (d.isDirectorySelectionEnabled()) {
            File sel = d.getSelectedFile();
            if (sel.isDirectory()) {
                dir = sel;
            }
        }
        RawSettings.setUserSetting(getFolderKey(d), dir.getAbsolutePath());
        RawSettings.writeUserSettings();
    }

    /**
     * Creates a new file chooser using a default folder from the specified key.
     *
     * @param key folder key
     * @return a file chooser initialized for the folder key
     */
    private static JFileChooser createFileChooser(String key) {
        String f = RawSettings.getUserSetting(key);
        // create folder with saved folder, or default folder if there is no setting
        JFileChooser fc = null;
        try {
            StrangeEons.log.log(Level.INFO, "creating file chooser {0} (path: \"{1}\")", new Object[]{key, f});
            fc = new JFileChooser(f);
        } catch (RuntimeException e) {
            // sometimes first attempt to create chooser fails with NPE,
            // but subsequent attempt may succeed [Java Bug ID: 4711700];
            //
            // sometimes a runtime exception is thrown with message
            // "Could not get shell folder ID list" [Java Bug ID: 6544857]
            //
            // sometimes an ArrayIndexOutOfBounds exception is thrown under
            // Windows Vista [Java Bug ID: 6449933]
            //
            // (All of these are RuntimeException subclasses.)
            //
            // to work around, we first try creating the chooser again
            // (sometimes this works); if that fails we try creating the
            // chooser in the Metal L&F from now on
            StrangeEons.log.warning("initial instantiation of file chooser failed: will try workarounds");
            final int MAX_ATTEMPTS = 5;
            for (int i = 0; (i < MAX_ATTEMPTS) && (fc == null); ++i) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                }
                try {
                    fc = new JFileChooser(f);
                } catch (RuntimeException e2) {
                }
            }

            if (fc == null) {
                // We will try creating the chooser in the Metal look and feel to
                // keep it from doing the Windows-specific things that might be
                // causing the problem.
                // Nimbus appears to cache/ignore settings after init, so the following
                // is insufficient:
                //    UIManager.getDefaults().put("FileChooserUI", "javax.swing.plaf.metal.MetalFileChooserUI");
                // Instead, we set and then restore the L&F around the chooser creation
                LookAndFeel laf = UIManager.getLookAndFeel();
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                } catch (Exception lfe) {
                }

                try {
                    fc = new JFileChooser(f);
                    try {
                        UIManager.setLookAndFeel(laf);
                    } catch (Exception lfe) {
                    }
                } catch (RuntimeException e3) {
                    StrangeEons.log.severe("exhausted JFileChooser workarounds; giving up");
                    throw e3;
                }
                StrangeEons.log.warning("file chooser UI degraded to Metal L&F due to JFileChooser bugs");
            }
        }

        final int CHOOSER_SIZE = 780;
        fc.setSize(CHOOSER_SIZE, CHOOSER_SIZE * 3 / 4);

        return fc;
    }

    /**
     * Gets the default folder from settings and applies it to the chooser. The
     * setting key is selected based on the chooser passed in.
     *
     * @param d chooser to set the directory on
     */
    private static void applyDefaultFolder(JFileChooser d) {
        d.setCurrentDirectory(null);

        String f = RawSettings.getUserSetting(getFolderKey(d));
        if (f != null) {
            try {
                d.setCurrentDirectory(new File(f));
            } catch (Throwable t) {
                displayError("Unable to set default folder directory.", t);
            }
        }
    }

    /**
     * Strips illegal characters out of a potential file name. If no characters
     * are stripped, {@code name} is returned unchanged.
     *
     * @param string the potential file name
     * @return a copy of name with any illegal characters stripped out
     */
    public static String makeStringFileSafe(String string) {
        string = AbstractGameComponent.filterComponentText(string);
        StringBuilder b = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if ("?[]/\\=+<>:;\",*|^~".indexOf(c) < 0) {
                b.append(c);
            }
        }
        // return the original string if it didn't change
        if (b.length() == string.length()) {
            return string;
        }
        return b.toString();
    }

    /**
     * Makes a string safe for inclusion as literal HTML by escaping ampersands,
     * less than symbols, and greater than symbols with their HTML entity
     * expressions. If the string contains none of these characters, the
     * original string is returned (including if it is {@code null}).
     *
     * @param string the string to make safe for HTML display (may be
     * {@code null})
     * @return an HTML-safe version with escaped characters where needed, or the
     * original string
     */
    public static String makeStringHTMLSafe(String string) {
        if (string == null) {
            return null;
        }
        int i;
        final int len = string.length();
        for (i = 0; i < len; ++i) {
            final char c = string.charAt(i);
            if (c == '&' || c == '<' || c == '>') {
                break;
            }
        }
        // no escaping required, return original
        if (i == len) {
            return string;
        }
        // OK from 0 to i-1, then escape as we go
        final StringBuilder b = new StringBuilder(string.length() * 4 / 3)
                .append(string, 0, i);
        for (; i < len; ++i) {
            final char c = string.charAt(i);
            if (c == '&') {
                b.append("&amp;");
            } else if (c == '<') {
                b.append("&lt;");
            } else if (c == '>') {
                b.append("&gt;");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Returns the first font family that is installed from a comma-separated
     * list of font families.
     *
     * @param familyList a comma-separated list of family names
     * @param defaultFamily the value to return if none of the listed families
     * is available
     * @return the first available font family, or {@code defaultFamily}
     */
    public static String findAvailableFontFamily(String familyList, String defaultFamily) {
        if (familyList == null) {
            return defaultFamily;
        }

        String[] families = familyList.trim().split("\\s*,\\s*");
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.getDefault());

        for (int i = 0; i < families.length; ++i) {
            String family = families[i];
            for (int j = 0; j < installed.length; ++j) {
                if (family.equalsIgnoreCase(installed[j])) {
                    return installed[j];
                }
            }
        }
        return defaultFamily;
    }

    /**
     * Reads a {@link GameComponent} from a file. If an error occurs while
     * reading or creating the component, an error message will be displayed to
     * the user as if the component was opened using the <b>File|Open</b>
     * command, and {@code null} will be returned.
     *
     * @param file the file to load the component from
     * @return the game component stored in the file, or {@code null}
     * @see #getGameComponentFromFile(java.io.File, boolean)
     */
    public static GameComponent getGameComponentFromFile(File file) {
        return getGameComponentFromFile(file, true);
    }

    /**
     * Reads a {@link GameComponent} from a file. If an error occurs while
     * reading or creating the component, and {@code reportError} is
     * {@code true}, then an error message will be displayed to the user as if
     * the component was opened using the <b>File|Open</b>
     * command. In any case, if the component cannot be created then
     * {@code null} will be returned.
     *
     * @param file the file to load the component from
     * @param reportError if {@code false}, errors will be logged but not
     * displayed
     * @return the game component stored in the file, or {@code null}
     * @see #getGameComponentFromFile(java.io.File, boolean)
     */
    public static GameComponent getGameComponentFromFile(File file, boolean reportError) {
        if (file == null) {
            throw new NullPointerException("file");
        }

        // in case the component turns out to be a card deck, this will set
        // things up so that we can look for the cards that make up the deck
        // more intelligently.
        File oldDefaultFolderStack = DeckDeserializationSupport.getShared().getDefaultFallbackFolder();
        DeckDeserializationSupport.getShared().setDefaultFallbackFolder(file.getParentFile());
        try {
            return getGameComponentFromStream(new FileInputStream(file), file.getName(), reportError);
        } catch (IOException e) {
            if (reportError) {
                displayError(string("app-err-open", file.getName()), e);
            } else {
                StrangeEons.log.log(Level.WARNING, "exception while reading " + file, e);
            }
            return null;
        } finally {
            DeckDeserializationSupport.getShared().setDefaultFallbackFolder(oldDefaultFolderStack);
        }
    }

    /**
     * Reads a {@link GameComponent} from an input stream. If an error occurs
     * while reading or creating the component, and {@code reportError} is
     * {@code true}, then an error message will be displayed to the user as if
     * the component was opened using the <b>File|Open</b>
     * command. In any case, if the component cannot be created then
     * {@code null} will be returned.
     *
     * @param in the input stream to read from
     * @param sourceDescription a description of the source of the input stream
     * (such as a file or URL); this is displayed as part of an error message,
     * if any
     * @param reportError if {@code true}, any errors will be reported to the
     * user with suitable error messages; otherwise, errors will simply be
     * logged
     * @return a game component deserialized from the input stream, or
     * {@code null}
     * @see SEObjectInputStream
     */
    @SuppressWarnings("deprecation")
    public static GameComponent getGameComponentFromStream(InputStream in, String sourceDescription, boolean reportError) {
        GameComponent gc = null;
        SEObjectInputStream oi = null;

        try {
            oi = new SEObjectInputStream(in);
            Object o = oi.readObject();
            while (!(o instanceof GameComponent)) {
                o = oi.readObject();
            }
            gc = (GameComponent) o;
            // convert the component if required
            UpgradeConversionTrigger trigger = gc.createUpgradeConversionTrigger();
            if (trigger != null) {
                gc = ConversionSession.convertGameComponent(trigger, gc, reportError);
            }
            // verify that required cores are installed
            if (gc != null) {
                gc.coreCheck();
            }
        } catch (FileRecoveryException recover) {
            File recoverFile = recover.getTempFile();
            gc = getGameComponentFromFile(recoverFile, false);
            recoverFile.delete();
            String message = gc != null ? recover.getSuccessMessage() : recover.getFailureMessage();
            if (message != null) {
                Messenger.displayWarningMessage(null, message);
            }
        } catch (StreamCorruptedException e) {
            StrangeEons.log.log(Level.SEVERE, string("app-err-badmagic", sourceDescription), e);
            if (reportError) {
                displayError(string("app-err-badmagic", sourceDescription), null);
            }
            return null;
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, string("app-err-open", sourceDescription), e);
            if (reportError) {
                displayError(string("app-err-open", sourceDescription), e);
            }
            return null;
        } finally {
            if (oi != null) {
                try {
                    oi.close();
                } catch (IOException e) {
                    StrangeEons.log.log(Level.WARNING, null, e);
                }
            }
        }
        return gc;
    }

    /**
     * Writes a {@link GameComponent} to a file.
     *
     * @param f the file to write to
     * @param gc the game component to serialize to the file
     * @throws IOException if an I/O error occurs while writing the file
     * @see #getGameComponentFromFile
     */
    public static void writeGameComponentToFile(File f, GameComponent gc) throws IOException {
        try (SEObjectOutputStream oo = new SEObjectOutputStream(new FileOutputStream(f))) {
            ComponentMetadata.writeMetadataToStream(oo, gc);
            oo.writeObject(gc);
        }
    }

    /**
     * Creates a custom cursor using an image resource, which may be a
     * multiresolution image resource.
     * 
     * @param cursorImage the base image resource to use for the cursor
     * @param hotspot the location of the hotspot on the base image
     * @param cursorName the cursor name, for accessibility
     * @param fallback the cursor to return if your cursor cannot be supported
     * @return the custom cursor, or the fallback
     */
    public static Cursor createCustomCursor(String cursorImage, Point hotspot, String cursorName, Cursor fallback) {
        try {
            MultiResolutionImageResource mri = new MultiResolutionImageResource(cursorImage);
            Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(1, 1);
            if (d.width > 0 && d.height > 0) {
                BufferedImage base = mri.getBaseImage();
                BufferedImage scaled = mri.getResolutionVariant(d.width, d.height);
                if (scaled.getWidth() != d.width || scaled.getHeight() != d.height) {
                    scaled = ImageUtilities.resample(scaled, d.width, d.height);
                }

                double hx = (double) hotspot.x * (double) scaled.getWidth() / (double) base.getWidth();
                double hy = (double) hotspot.y * (double) scaled.getHeight() / (double) base.getHeight();
                Cursor custom = Toolkit.getDefaultToolkit().createCustomCursor(
                        scaled,
                        new Point((int) (hx+0.5d), (int) (hy+0.5d)),
                        cursorName
                );
                return custom;
            }
        } catch (HeadlessException hex) {
            // use fallback
        }
        return fallback;
    }

    @Deprecated
    public static BufferedImage createBleedBanner(Image source) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        BufferedImage grad = getImage("icons/banner/gradient.png");
        BufferedImage dest = new BufferedImage(source.getWidth(null), source.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dest.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
            g.setComposite(AlphaComposite.DstOut);
            g.drawImage(grad, 0, dest.getHeight() - grad.getHeight(), dest.getWidth(), grad.getHeight(), null);
        } finally {
            g.dispose();
        }
        return dest;
    }

    /**
     * Returns a bleed banner icon. This is an icon based on a variation of
     * a standard image, {@code icons/banner/banner.jpg}, with a specific
     * alpha gradient effect applied to the bottom edge. (An example use
     * is the new game component dialog.)
     *
     * @param source the source banner image; if does not specify a directory,
     * the default is {@code icons/banner}.
     * @return an image with a transparent lower edge
     * @throws NullPointerException if the source image is {@code null}
     */
    public static ThemedIcon createBleedBanner(String resource) {
        if (resource.indexOf('/') < 0) {
            resource = "icons/banner/" + resource;
        }
        return bannerCache.get(resource);
    }
    private static ThemedIcon createBleedBannerImpl(String resource) {
        MultiResolutionImageResource mim = new MultiResolutionImageResource(resource);
        BufferedImage bleedGradient = getImage("icons/banner/gradient.png");
        FilteredMultiResolutionImage filtered = new FilteredMultiResolutionImage(mim) {
            @Override
            public Image applyFilter(Image source) {
                BufferedImage im = new BufferedImage(source.getWidth(null), source.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = im.createGraphics();
                try {
                    g.drawImage(source, 0, 0, null);

                    g.setComposite(AlphaComposite.DstOut);
                    if (im.getWidth() == BANNER_WIDTH && im.getHeight() == BANNER_HEIGHT) {
                        g.drawImage(bleedGradient, 0, BANNER_HEIGHT - bleedGradient.getHeight(), null);
                    } else {
                        g.scale((double) im.getWidth() / (double) BANNER_WIDTH, (double) im.getHeight() / (double) BANNER_HEIGHT);
                        g.drawImage(bleedGradient, 0, BANNER_HEIGHT - bleedGradient.getHeight(), BANNER_WIDTH, bleedGradient.getHeight(), null);
                    }
                } finally {
                    g.dispose();
                }
                return im;
            }
        };
        return new ThemedImageIcon(filtered, BANNER_WIDTH, BANNER_HEIGHT);
    }
    private static final int BANNER_WIDTH = 117, BANNER_HEIGHT = 362;
    
    private static final AbstractResourceCache<String,ThemedIcon> bannerCache = new AbstractResourceCache<>(ThemedIcon.class, "Banners") {
        @Override
        protected ThemedIcon loadResource(String canonicalIdentifier) {
            return createBleedBannerImpl(canonicalIdentifier);
        }
        @Override
        protected long estimateResourceMemoryUse(ThemedIcon resource) {
            double scale = estimateDesktopScalingFactor();
            if (scale > 1d) {
                // total memory required is typically 1 base image + 1 scaled image
                double base = (BANNER_WIDTH * BANNER_HEIGHT) * 4d;
                base += base * scale * scale;
                return (long) base;
            } else {
                return (long) (BANNER_WIDTH * BANNER_HEIGHT * 4);
            }
        }
    };
    

    /**
     * Creates a new wait icon (an animated icon that indicates a lengthy
     * delay). These icons are tied to a specific component, so a new one must
     * be created whenever one is required.
     *
     * @param comp the component that the icon will be set on
     * @return an animated wait icon for the component
     */
    public static AnimatedIcon createWaitIcon(JComponent comp) {
        if (waitFrames == null) {
            waitFrames = new BufferedImage[8];
            int[] coords = new int[]{
                15, 4, 23, 7, 26, 15, 23, 23, 15, 27, 7, 23, 4, 15, 7, 7
            };
            Color interior = new Color(0x69a9c9);
            Ellipse2D.Float shape = new Ellipse2D.Float();
            for (int frame = 0; frame < 8; ++frame) {
                int pos = frame;
                BufferedImage bi = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bi.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(interior);
                    for (int ball = 0; ball < 8; ++ball) {
                        float delta = (pos + 1f) / 2f;
                        shape.setFrame(
                                coords[ball * 2] - delta, coords[ball * 2 + 1] - delta, pos + 1f, pos + 1f
                        );
                        g.fill(shape);
                        if (++pos == 8) {
                            pos = 0;
                        }
                    }
                } finally {
                    g.dispose();
                }
                waitFrames[7 - frame] = bi;
            }
        }
        return new AnimatedIcon(comp, waitFrames);
    }
    private static Image[] waitFrames;

    /**
     * Returned by {@link #registerFont} to describe the result of registration.
     */
    public static final class FontRegistrationResult {

        private Font f;
        private boolean ok;

        private FontRegistrationResult(Font font, boolean registered) {
            if (font == null) {
                throw new NullPointerException();
            }
            f = font;
            ok = registered;
        }

        /**
         * Returns a {@code Font} instance for the font that was created from
         * the resource. The font will have a size of 1 point; a different size
         * can be created by calling the font's {@code deriveFont} method or, if
         * registration was successful, by creating a new {@code Font} with this
         * font's family name.
         *
         * @return a font created from the stream
         */
        public Font getFont() {
            return f;
        }

        /**
         * Returns the name of the font. The font name can be used to create an
         * instance of the font using
         * {@link Font#Font(java.lang.String, int, int)}.
         *
         * @return the font's name
         */
        public String getFamily() {
            return f.getFamily(); // f.getFontName();
        }

        /**
         * Returns {@code true} if the font was registered. Note that if the
         * font was not registered, it is usually because there is already a
         * font with the same name installed on the user's system. Therefore,
         * you will probably get <i>some version</i> of the font by using the
         * font's family name. (If there is a problem loading the font, this
         * will result in an exception when registration is attempted.)
         *
         * @return {@code true} if the font was registered; {@code false}
         * otherwise
         */
        public boolean isRegistrationSuccessful() {
            return ok;
        }

        @Override
        public String toString() {
            return "FontRegistrationResult{ font=" + f.getFontName() + ", registered=" + ok + " }";
        }
    }
}
