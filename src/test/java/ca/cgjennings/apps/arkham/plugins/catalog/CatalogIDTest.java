package ca.cgjennings.apps.arkham.plugins.catalog;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogIDTest {

    private static final UUID UUID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void monthTokenIsOneIndexed() {
        CatalogID id = new CatalogID(UUID_1, Instant.parse("2026-05-25T12:34:56.789Z"));

        assertEquals("2026-5-25-12-34-56-789", id.toDateString());
    }

    @Test
    void januaryIsOne() {
        assertEquals("2026-1-1-0-0-0-000",
                new CatalogID(UUID_1, Instant.parse("2026-01-01T00:00:00.000Z")).toDateString());
    }

    @Test
    void millisecondsAreZeroPadded() {
        assertEquals("2026-5-25-0-0-0-050",
                new CatalogID(UUID_1, Instant.parse("2026-05-25T00:00:00.050Z")).toDateString());
    }

    @Test
    void parserAcceptsLegacyUnpaddedMilliseconds() {
        String legacy = "CATALOGUEID{" + UUID_1 + ":2026-5-25-0-0-0-50}";

        CatalogID parsed = CatalogID.extractCatalogID(legacy);

        assertNotNull(parsed);
        assertEquals(50, parsed.getDate().get(Calendar.MILLISECOND));
        assertEquals(Calendar.MAY, parsed.getDate().get(Calendar.MONTH));
    }

    @Test
    void parserAcceptsLegacyZeroMilliseconds() {
        String legacy = "CATALOGUEID{" + UUID_1 + ":2026-1-1-0-0-0-0}";

        CatalogID parsed = CatalogID.extractCatalogID(legacy);

        assertNotNull(parsed);
        assertEquals(0, parsed.getDate().get(Calendar.MILLISECOND));
        assertEquals(Calendar.JANUARY, parsed.getDate().get(Calendar.MONTH));
    }

    @Test
    void decemberIsTwelve() {
        assertEquals("2026-12-31-23-59-59-999",
                new CatalogID(UUID_1, Instant.parse("2026-12-31T23:59:59.999Z")).toDateString());
    }

    @Test
    void roundTripPreservesDate() {
        CatalogID original = new CatalogID();
        String s = original.toString();

        CatalogID parsed = CatalogID.extractCatalogID(s);

        assertNotNull(parsed);
        assertTrue(original.sameUUID(parsed));
        assertEquals(0, original.compareDates(parsed));
        assertEquals(s, parsed.toString());
    }

    @Test
    void parseRoundsTripAcrossEveryMonth() {
        for (int m = Calendar.JANUARY; m <= Calendar.DECEMBER; m++) {
            Instant when = LocalDateTime.of(2026, m + 1, 15, 0, 0, 0).toInstant(ZoneOffset.UTC);

            CatalogID id = new CatalogID(UUID_1, when);
            CatalogID parsed = CatalogID.extractCatalogID(id.toString());

            assertNotNull(parsed, "month " + m);
            assertEquals(m, parsed.getDate().get(Calendar.MONTH), "month " + m);
        }
    }
}
