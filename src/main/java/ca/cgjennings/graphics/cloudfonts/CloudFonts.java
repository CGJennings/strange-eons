package ca.cgjennings.graphics.cloudfonts;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

import ca.cgjennings.apps.arkham.StrangeEons;
import resources.Settings;

/**
 * Provides a default cloud font collection.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class CloudFonts {
    private CloudFonts() {
    }

    private static CloudFontCollection defaultCollection;

    public static CloudFontCollection getDefaultCollection() {
        synchronized (CloudFonts.class) {
            if (defaultCollection == null) {
                StrangeEons.log.info("creating default cloud font collection");
                defaultCollection = new DefaultCloudFontConnector().createFontCollection();
            }
            return defaultCollection;
        }
    }

    /**
     * If the user has reserved font families in the default
     * collection, this method ensures that the relevant fonts
     * are up to date and registered. This is called during
     * application startup.
     */
    public static void installReservedFamilies() {
        String list = Settings.getUser().get("cloudfont-reserved-families", "");
        String[] families = list.split("\\s+,\\s+");
        if (families.length == 0 || (families.length == 1 && families[0].isBlank())) return;

        StrangeEons.log.log(Level.INFO, "installing reserved cloud font families");
        synchronized (userReservedFonts) {            
            for (String family : families) {
                family = family.trim();
                if(!family.isEmpty()) {
                    // the user means for it to be reserved even if it temporarily fails to load
                    userReservedFonts.add(family);
                    try {
                        CloudFontFamily cff = getDefaultCollection().getFamily(family);
                        if (cff != null) {
                            cff.register();
                        } else {
                            StrangeEons.log.log(Level.WARNING, "reserved cloud font family not found in collection: {0}", family);
                        }
                    } catch (IOException ex) {
                        StrangeEons.log.log(Level.WARNING, "reserved cloud font family failed to load: " + family, ex);
                    }
                }
            }
        }
    }

    /**
     * Returns true if the user has reserved the specified font family
     * @param familyName the font family to check
     * @return true if the user has reserved the font family, either
     * in this session or in a previous session
     */
    public static boolean isReservedFamily(CloudFontFamily family) {
        synchronized (userReservedFonts) {
            if (family.getCollection() != getDefaultCollection()) {
                return false;
            }            
            return userReservedFonts.contains(family.getName());
        }
    }

    /**
     * Adds a font family to the user's reserved list. Reserving
     * a font family causes that family to be loaded and registered
     * during application startup so that it is always available for the
     * user to use by family name. The font will also be updated
     * automatically as updates are published (when the local
     * cache is updated by the cloud font connector). Note that
     * only families from the default collection can be reserved.
     * 
     * @param family the font family to reserve
     * @throws NullPointerException if family is null
     * @throws IllegalArgumentException if family is not from the default collection
     */
    public static void addReservedFamily(CloudFontFamily family) {
        family = Objects.requireNonNull(family, "family");
        if (family.getCollection() != getDefaultCollection()) {
            throw new IllegalArgumentException("family is not from the default collection");
        }

        final String familyName = family.getName();
        synchronized (userReservedFonts) {
            if (userReservedFonts.add(familyName)) {
                updateReservedFamilyList();
                try {
                    CloudFontFamily cff = getDefaultCollection().getFamily(familyName);
                    if (cff != null) {
                        cff.register();
                    } else {
                        StrangeEons.log.log(Level.WARNING, "family not found in collection: {0}", familyName);
                    }
                } catch (IOException ex) {
                    StrangeEons.log.log(Level.WARNING, "failed to load and register cloud font family: " + familyName, ex);
                }
            };
        }
    }

    /**
     * Removes a font family from the user's reserved list.
     * This method has no effect if the family is not reserved.
     * 
     * @param family the font family to remove
     * @see #addReservedFamily
     */
    public static void removeReservedFamily(CloudFontFamily family) {
        family = Objects.requireNonNull(family, "family");
        if (family.getCollection() != getDefaultCollection()) {
            return;
        }

        final String familyName = family.getName();
        synchronized (userReservedFonts) {
            if (userReservedFonts.remove(familyName)) {
                updateReservedFamilyList();
            }
        }
    }

    private static void updateReservedFamilyList() {
        Settings.getUser().set("cloudfont-reserved-families", String.join(",", userReservedFonts));
    }

    private static final Set<String> userReservedFonts = new HashSet<>();
}
