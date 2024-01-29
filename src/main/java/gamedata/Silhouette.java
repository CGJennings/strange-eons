package gamedata;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.AbstractPixelwiseFilter;
import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.ui.IconProvider;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import javax.swing.Icon;
import resources.Language;
import resources.ResourceKit;

/**
 * A stencil shape that can be used to "cut out" a marker or token. Silhouette
 * shapes are defined using a greyscale image; white areas will be cut off while
 * black areas will be included as part of the marker's shape. Shades of grey
 * are correspondingly transparent and can be used to create soft edges via
 * antialiasing.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Silhouette implements IconProvider {

    private String key;
    private String portrait;
    private Icon icon;
    private String stencilRes;
    private BufferedImage stencil;
    private double bleedMargin;

    private static final HashMap<String, Silhouette> sils = new HashMap<>(64);
    private static final int ICON_SIZE = 18;

    /**
     * Creates a new silhouette with unique key {@code key}, that will obtain a
     * stencil image from {@code stencilResource} and use
     * {@code portraitResource} as its default portrait.
     *
     * @param key the unique key
     * @param stencilResource resource file of the stencil image
     * @param portraitResource resource file of the portrait image; may be
     * {@code null}
     * @param bleedMargin the bleed margin surrounding the silhouette, in
     * points, or 0
     */
    public Silhouette(String key, String stencilResource, String portraitResource, double bleedMargin) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (stencilResource == null) {
            throw new NullPointerException("stencilResource");
        }

        this.key = key;
        this.portrait = portraitResource;
        this.stencilRes = stencilResource;
        this.bleedMargin = bleedMargin;
    }

    /**
     * This constructor is used when a saved file contains a stencil that is not
     * installed in this copy of Strange Eons.
     *
     * @param key
     * @param stencilResource
     * @param stencil
     */
    public Silhouette(String key, String stencilResource, BufferedImage stencil) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (stencilResource == null) {
            throw new NullPointerException("stencilResource");
        }
        if (stencil == null) {
            throw new IllegalArgumentException("stencil");
        }

        this.key = key;
        this.stencilRes = stencilResource;
        this.stencil = stencil;
        icon = ImageUtilities.createIconForSize(stencil, ICON_SIZE);
    }

    private synchronized void lazyInit() {
        if (stencil != null) {
            return;
        }

        stencil = createStencil(ResourceKit.getImage(stencilRes));

        float scale = ImageUtilities.idealCoveringScaleForImage(ICON_SIZE, ICON_SIZE, stencil.getWidth(), stencil.getHeight());
        int maxDimen = Math.min(150, Math.max(stencil.getWidth(), stencil.getHeight()));
        float factor = Math.max(0.667f, maxDimen / 150f);

        BufferedImage scaled = ImageUtilities.resample(stencil, scale * factor);
        icon = ImageUtilities.createIconForSize(scaled, ICON_SIZE);
    }

    private static BufferedImage createStencil(BufferedImage src) {
        BufferedImage stencil = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        return stencilFilter.filter(src, stencil);
    }

    /**
     * Returns this silhouette's unique key.
     *
     * @return the silhouette key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns a localized description of this silhouette.
     *
     * @return the silhouette's description
     */
    public String getLabel() {
        if (key.startsWith("@")) {
            if (decodedKey == null) {
                decodedKey = key.substring(1).replace('_', '-');
            }
            return Language.string(decodedKey);
        } else {
            return key;
        }
    }
    private String decodedKey;

    /**
     * Returns a small icon that can be used to represent this silhouette in an
     * interface.
     *
     * @return an icon of the silhouette shape
     */
    @Override
    public Icon getIcon() {
        lazyInit();
        return icon;
    }

    /**
     * Returns the stencil image for this silhouette. The stencil image's alpha
     * channel defines the shape of markers created with this silhouette.
     *
     * @return the stencil image for this silhouette
     */
    public BufferedImage getStencil() {
        lazyInit();
        return stencil;
    }

    /**
     * Returns the image resource name for this silhouette's default portrait
     * resource. This is guaranteed not to be {@code null}.
     *
     * @return the silhouette's default portrait
     */
    public String getDefaultPortrait() {
        if (portrait == null) {
            return DEFAULT_PORTRAIT;
        } else {
            return portrait;
        }
    }

    /**
     * Returns this silhouette's bleed margin, in points, or 0 if the silhouette
     * does not include a bleed margin.
     *
     * @return the bleed margin surrounding the silhouette, in points, or 0
     */
    public double getBleedMargin() {
        return bleedMargin;
    }

    /**
     * The portrait resource that will be returned as the default portrait by
     * all silhouettes who do not specify one.
     */
    public static final String DEFAULT_PORTRAIT = "portraits/marker-portrait.jp2";

    /**
     * Returns a string representation of the silhouette. This is a cover for
     * {@link #getLabel()}.
     *
     * @return the silhouette's description
     */
    @Override
    public String toString() {
        return getLabel();
    }

    private static AbstractPixelwiseFilter stencilFilter = new AbstractPixelwiseFilter() {
        @Override
        public void filterPixels(int[] argb, int start, int end) {
            for (int p = start; p < end; ++p) {
                argb[p] = /* 0xffffff | */ (0xff - (argb[p] & 0xff) << 24);
            }
        }
    };

    /**
     * Returns {@code true} if this silhouette and {@code obj} have the same
     * key.
     *
     * @param obj the object to test
     * @return {@code true} if {@code obj} is a silhouette with the same key
     */
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Silhouette && key.equals(((Silhouette) obj).key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    /**
     * Returns the Silhouette with the specified key, or {@code null} if there
     * is none.
     *
     * @param key the key to search for
     * @return the silhouette with the given key, or {@code null}
     */
    public static Silhouette get(String key) {
        return sils.get(key);
    }

    /**
     * Returns an array of all of the currently registered silhouettes.
     *
     * @return a possibly empty array of all silhouettes
     */
    public static Silhouette[] getSilhouettes() {
        Silhouette[] registered;
        synchronized (sils) {
            Collection<Silhouette> s = sils.values();
            registered = s.toArray(Silhouette[]::new);
            if (sorter == null) {
                sorter = (Silhouette lhs, Silhouette rhs) -> Language.getInterface().getCollator().compare(lhs.getLabel(), rhs.getLabel());
            }
        }
        Arrays.sort(registered, sorter);
        return registered;
    }
    private static Comparator<Silhouette> sorter;

    /**
     * Adds silhouettes by parsing a silhouette definition file stored in
     * resources.
     *
     * @param silhouetteResource the file to parse
     * @throws NullPointerException if the resource file is {@code null}
     * @throws IOException if an error occurs while reading the resource file
     * @throws ResourceParserException if an error occurs while parsing the file
     */
    public static void add(String silhouetteResource) throws IOException {
        Silhouette s = null;
        EscapedLineReader r = null;
        try {
            try {
                r = new EscapedLineReader(ResourceKit.composeResourceURL(silhouetteResource));
            } catch (NullPointerException n) {
                throw new FileNotFoundException(silhouetteResource);
            }

            String[] p;
            while ((p = r.readProperty()) != null) {
                String key = p[0];
                String value = p[1], stencil, portrait = null;
                double bleed = 0d;
                if (value.isEmpty()) {
                    throw new ResourceParserException(silhouetteResource, "missing value for " + key, r);
                }

                String[] tokens = value.trim().split("\\s*\\|\\s*");

                stencil = tokens[0];
                if (tokens.length > 1) {
                    portrait = tokens[1];
                }
                if (tokens.length > 2) {
                    try {
                        bleed = Double.parseDouble(tokens[2]);
                    } catch (NumberFormatException e) {
                        throw new ResourceParserException(silhouetteResource, "invalid bleed margin: " + value, r);
                    }
                }

                int div = stencil.indexOf('|');
                if (div >= 0) {
                    portrait = stencil.substring(div + 1).trim();
                    stencil = stencil.substring(0, div).trim();
                    if (stencil.isEmpty()) {
                        throw new ResourceParserException(silhouetteResource, "missing stencil for " + key, r);
                    }
                    if (portrait.isEmpty()) {
                        portrait = null;
                    }
                } else {
                    stencil = stencil.trim();
                }
                s = new Silhouette(key, stencil, portrait, bleed);
                registerImpl(s);
            }
        } finally {
            if (r != null) {
                r.close();
            }
        }
        if (s != null) {
            Listeners.fireRegistrationEvent(s);
        }
    }

    /**
     * Registers a new silhouette.
     *
     * @param s the new silhouette to register
     * @throws NullPointerException if {@code s} is {@code null}
     */
    public synchronized static void register(Silhouette s) {
        registerImpl(s);
        Listeners.fireRegistrationEvent(s);
    }

    private synchronized static void registerImpl(Silhouette s) {
        if (s == null) {
            throw new NullPointerException("silhouette");
        }
        synchronized (sils) {
            if (sils.containsKey(s.getKey())) {
                throw new IllegalArgumentException("a silhouette with this key has already been registered");
            }
            sils.put(s.getKey(), s);
        }
    }

    /**
     * Unregisters a silhouette.
     *
     * @param s the silhouette to unregister
     */
    public static void unregister(Silhouette s) {
        synchronized (sils) {
            if (sils.containsKey(s.getKey())) {
                sils.remove(s.getKey());
                Listeners.fireUnregistrationEvent(s);
            }
        }
    }

    /**
     * Listeners that are informed of registration changes.
     */
    public static final RegistrationEventSource<Expansion> Listeners = new RegistrationEventSource<>();

}
