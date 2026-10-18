package ca.cgjennings.apps.arkham.sheet;

import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import resources.Language;
import resources.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SheetTest {

    @BeforeAll
    static void setGameLocale() {
        Language.setGameLocale(Locale.ENGLISH);
    }

    private static Settings settings(String... keysAndValues) {
        Properties p = new Properties();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            p.setProperty(keysAndValues[i], keysAndValues[i + 1]);
        }
        return Settings.forProperties("test", p);
    }

    @Test
    void highResolutionDefaultsToPrintResolution() {
        assertEquals(300d, Sheet.readHighResolutionTemplateResolution(settings(), "card"));
    }

    @Test
    void highResolutionReadsPpi() {
        assertEquals(600d, Sheet.readHighResolutionTemplateResolution(settings("card-hires-ppi", "600"), "card"));
    }

    @Test
    void highResolutionReadsDpiAlias() {
        assertEquals(450d, Sheet.readHighResolutionTemplateResolution(settings("card-hires-dpi", "450"), "card"));
    }

    @Test
    void highResolutionPpiTakesPrecedence() {
        assertEquals(600d, Sheet.readHighResolutionTemplateResolution(settings("card-hires-ppi", "600", "card-hires-dpi", "450"), "card"));
    }

    @Test
    void highResolutionIgnoresMalformedValue() {
        assertEquals(300d, Sheet.readHighResolutionTemplateResolution(settings("card-hires-ppi", "high"), "card"));
    }

    @Test
    void highResolutionIgnoresNonPositiveValue() {
        assertEquals(300d, Sheet.readHighResolutionTemplateResolution(settings("card-hires-ppi", "0"), "card"));
    }
}
