package ca.cgjennings.apps.arkham.plugins.catalog;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergedCatalogTest {

    private static final URL BASE_A = url("https://a.example.com/se/");
    private static final URL BASE_B = url("https://b.example.com/se/");
    private static final UUID UUID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID UUID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UUID_3 = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void disjointUUIDs_unionInPriorityOrder() {
        Catalog a = catalog(BASE_A,
                listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha", "./alpha.seplugin"));
        Catalog b = catalog(BASE_B,
                listing(UUID_2, "2025-1-1-0-0-0-0", "Beta", "./beta.seplugin"));

        List<Listing> merged = MergedCatalog.mergeListings(Arrays.asList(a, b));

        assertEquals(2, merged.size());
        assertEquals("Alpha", merged.get(0).getName());
        assertEquals("Beta", merged.get(1).getName());
    }

    @Test
    void sameUUID_newerDateWinsRegardlessOfPriority() {
        Catalog a = catalog(BASE_A,
                listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha old", "./alpha.seplugin"));
        Catalog b = catalog(BASE_B,
                listing(UUID_1, "2025-6-1-0-0-0-0", "Alpha new", "./alpha.seplugin"));

        List<Listing> merged = MergedCatalog.mergeListings(Arrays.asList(a, b));

        assertEquals(1, merged.size());
        assertEquals("Alpha new", merged.get(0).getName());
    }

    @Test
    void sameUUID_olderInLowPriority_keepsHigherPriority() {
        Catalog a = catalog(BASE_A,
                listing(UUID_1, "2025-6-1-0-0-0-0", "Alpha new", "./alpha.seplugin"));
        Catalog b = catalog(BASE_B,
                listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha old", "./alpha.seplugin"));

        List<Listing> merged = MergedCatalog.mergeListings(Arrays.asList(a, b));

        assertEquals(1, merged.size());
        assertEquals("Alpha new", merged.get(0).getName());
    }

    @Test
    void sameUUID_dateTie_lowerIndexWins() {
        Catalog a = catalog(BASE_A,
                listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha A", "./alpha.seplugin"));
        Catalog b = catalog(BASE_B,
                listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha B", "./alpha.seplugin"));

        List<Listing> merged = MergedCatalog.mergeListings(Arrays.asList(a, b));

        assertEquals(1, merged.size());
        assertEquals("Alpha A", merged.get(0).getName());
    }

    @Test
    void unparseableID_keepsBothListings() {
        Listing identified = listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha", "./alpha.seplugin");
        Listing unidentified = listingWithRawId("not-a-catalogue-id", "Mystery", "./mystery.seplugin");
        assertNull(unidentified.getCatalogID(), "test fixture must yield null catalog ID");

        Catalog a = catalog(BASE_A, identified, unidentified);

        List<Listing> merged = MergedCatalog.mergeListings(Collections.singletonList(a));

        assertEquals(2, merged.size());
        assertTrue(merged.stream().anyMatch(li -> "Alpha".equals(li.getName())));
        assertTrue(merged.stream().anyMatch(li -> "Mystery".equals(li.getName())));
    }

    @Test
    void hiddenListingPreserved_andLandsAtEndOfFinalCatalog() {
        Listing visible = listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha", "./alpha.seplugin");
        Listing hidden = listing(UUID_2, "2025-1-1-0-0-0-0", "Hidden", "./hidden.seplugin");
        hidden.set(Listing.HIDDEN, "yes");
        Catalog source = catalog(BASE_A, visible, hidden);

        List<Listing> merged = MergedCatalog.mergeListings(Collections.singletonList(source));
        Catalog finalCatalog = new Catalog(BASE_A, merged);

        assertEquals(1, finalCatalog.size(), "hidden listing must not count toward visible size");
        assertEquals(2, finalCatalog.trueSize());
        assertEquals("Hidden", finalCatalog.get(1).getName(),
                "hidden listing should be sorted to the end by Catalog.add");
    }

    @Test
    void relativeListingURL_isAbsolutizedAgainstSourceBase() {
        Catalog a = catalog(BASE_A,
                listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha", "./alpha.seplugin"));
        Catalog b = catalog(BASE_B,
                listing(UUID_2, "2025-1-1-0-0-0-0", "Beta", "./beta.seplugin"));

        List<Listing> merged = MergedCatalog.mergeListings(Arrays.asList(a, b));

        Listing alpha = merged.stream().filter(li -> "Alpha".equals(li.getName())).findFirst().orElseThrow();
        Listing beta = merged.stream().filter(li -> "Beta".equals(li.getName())).findFirst().orElseThrow();
        assertEquals("https://a.example.com/se/alpha.seplugin", alpha.get(Listing.URL));
        assertEquals("https://b.example.com/se/beta.seplugin", beta.get(Listing.URL));
    }

    @Test
    void absoluteListingURL_isLeftAlone() {
        Listing alreadyAbsolute = listing(UUID_3, "2025-1-1-0-0-0-0", "Gamma", "https://elsewhere.example/gamma.seplugin");
        Catalog a = catalog(BASE_A, alreadyAbsolute);

        List<Listing> merged = MergedCatalog.mergeListings(Collections.singletonList(a));

        assertEquals("https://elsewhere.example/gamma.seplugin", merged.get(0).get(Listing.URL));
    }

    @Test
    void emptySourceList_returnsEmptyMerge() {
        assertTrue(MergedCatalog.mergeListings(Collections.emptyList()).isEmpty());
    }

    @Test
    void provenanceMap_capturesWinningSource() {
        Catalog a = catalog(BASE_A,
                listing(UUID_1, "2025-1-1-0-0-0-0", "Alpha old", "./alpha.seplugin"));
        Catalog b = catalog(BASE_B,
                listing(UUID_1, "2025-6-1-0-0-0-0", "Alpha new", "./alpha.seplugin"),
                listing(UUID_2, "2025-1-1-0-0-0-0", "Beta", "./beta.seplugin"));

        java.util.Map<UUID, URL> provenance = new java.util.HashMap<>();
        MergedCatalog.mergeListings(Arrays.asList(a, b), provenance);

        assertEquals(BASE_B, provenance.get(UUID_1), "newer listing came from B");
        assertEquals(BASE_B, provenance.get(UUID_2));
    }

    private static Catalog catalog(URL base, Listing... listings) {
        return new Catalog(base, Arrays.asList(listings));
    }

    private static Listing listing(UUID uuid, String date, String name, String url) {
        Properties p = new Properties();
        p.setProperty(Listing.ID, "CATALOGUEID{" + uuid + ":" + date + "}");
        p.setProperty(Listing.NAME, name);
        p.setProperty(Listing.URL, url);
        return new Listing(null, p);
    }

    private static Listing listingWithRawId(String rawId, String name, String url) {
        Properties p = new Properties();
        p.setProperty(Listing.ID, rawId);
        p.setProperty(Listing.NAME, name);
        p.setProperty(Listing.URL, url);
        return new Listing(null, p);
    }

    private static URL url(String s) {
        try {
            return new URL(s);
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }
}
