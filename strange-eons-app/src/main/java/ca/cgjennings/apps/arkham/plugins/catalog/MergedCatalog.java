package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import javax.swing.JProgressBar;

/**
 * A {@link Catalog} built by merging listings from several source catalogs into
 * a single unified view. When two listings share a UUID, the listing whose
 * {@link CatalogID} carries the newer date wins; if the dates are equal, the
 * listing from the lower-indexed source wins (sources earlier in the list have
 * priority).
 *
 * <p>The base URL of a merged catalog is the base URL of the highest-priority
 * source that loaded successfully. Listing URLs are rewritten to absolute form
 * against the base URL of the source they originated from, so installs
 * resolve correctly regardless of which source contributed the listing.
 */
public final class MergedCatalog extends Catalog {

    private final Map<UUID, URL> sourceByUUID;

    private MergedCatalog(URL primaryBase, Collection<Listing> listings, Map<UUID, URL> sourceByUUID) {
        super(primaryBase, listings);
        this.sourceByUUID = sourceByUUID.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(sourceByUUID));
    }

    /**
     * Loads each source URL in order and returns a single merged catalog. The
     * first source that loads successfully provides the base URL of the
     * result. Sources that fail to load are skipped and their failures logged;
     * if every source fails, the most recent failure is rethrown.
     *
     * @param sources ordered list of source URLs (index 0 = highest priority)
     * @param allowCache if {@code true}, the local cache may be used for the
     *      primary catalog URL
     * @param feedback optional progress bar to update while loading
     * @return a merged catalog containing listings from every source that
     *      loaded successfully
     * @throws IOException if every source failed to load
     */
    public static MergedCatalog merge(List<URL> sources, boolean allowCache, JProgressBar feedback) throws IOException {
        List<Catalog> loaded = new ArrayList<>(sources.size());
        IOException lastFailure = null;
        for (URL source : sources) {
            try {
                loaded.add(new Catalog(source, allowCache, feedback));
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, "failed to load catalog source: " + source, e);
                lastFailure = e;
            }
        }
        if (loaded.isEmpty()) {
            if (lastFailure != null) {
                throw lastFailure;
            }
            return new MergedCatalog(null, Collections.emptyList(), Collections.emptyMap());
        }
        Map<UUID, URL> provenance = new HashMap<>();
        List<Listing> merged = mergeListings(loaded, provenance);
        return new MergedCatalog(loaded.get(0).getBaseURL(), merged, provenance);
    }

    /**
     * Returns the base URL of the source catalog that contributed the listing
     * at the given index, or this catalog's base URL if the listing has no
     * known provenance (typically because it carries no catalog ID).
     *
     * @param n the listing index
     * @return the source base URL for the listing
     */
    public URL getSourceForListing(int n) {
        Listing li = get(n);
        CatalogID id = li.getCatalogID();
        if (id != null) {
            URL src = sourceByUUID.get(id.getUUID());
            if (src != null) {
                return src;
            }
        }
        return getBaseURL();
    }

    /**
     * Merges the listings of the given catalogs in priority order using the
     * rule: same UUID → newer date wins, ties broken by source priority
     * (earlier in the list wins). Listings without a {@link CatalogID} are
     * passed through and appended after the identified listings.
     */
    static List<Listing> mergeListings(List<Catalog> orderedCatalogs) {
        return mergeListings(orderedCatalogs, null);
    }

    static List<Listing> mergeListings(List<Catalog> orderedCatalogs, Map<UUID, URL> sourceByUUIDOut) {
        LinkedHashMap<UUID, Listing> byUUID = new LinkedHashMap<>();
        Map<UUID, CatalogID> idByUUID = new HashMap<>();
        List<Listing> unidentified = new ArrayList<>();
        for (Catalog c : orderedCatalogs) {
            URL base = c.getBaseURL();
            for (int i = 0; i < c.trueSize(); ++i) {
                Listing li = c.get(i);
                CatalogID id = li.getCatalogID();
                absolutizeURL(li, base);
                if (id == null) {
                    unidentified.add(li);
                    continue;
                }
                UUID uuid = id.getUUID();
                CatalogID existingID = idByUUID.get(uuid);
                if (existingID == null) {
                    byUUID.put(uuid, li);
                    idByUUID.put(uuid, id);
                    if (sourceByUUIDOut != null) {
                        sourceByUUIDOut.put(uuid, base);
                    }
                } else if (id.compareDates(existingID) > 0) {
                    byUUID.put(uuid, li);
                    idByUUID.put(uuid, id);
                    if (sourceByUUIDOut != null) {
                        sourceByUUIDOut.put(uuid, base);
                    }
                }
            }
        }
        List<Listing> result = new ArrayList<>(byUUID.size() + unidentified.size());
        result.addAll(byUUID.values());
        result.addAll(unidentified);
        return result;
    }

    private static void absolutizeURL(Listing li, URL base) {
        if (base == null) {
            return;
        }
        String url = li.get(Listing.URL);
        if (url == null) {
            return;
        }
        try {
            String absolute = new URL(base, url).toString();
            if (!absolute.equals(url)) {
                li.set(Listing.URL, absolute);
            }
        } catch (MalformedURLException e) {
            StrangeEons.log.log(Level.WARNING, "could not absolutize listing URL: " + url, e);
        }
    }
}
