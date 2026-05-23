package ca.cgjennings.ui.theme;

/**
 * Theme that installs FlatLaf's Darcula look and feel — the IntelliJ
 * dark companion to {@link FlatIntelliJTheme}.
 *
 * @since 3.5
 */
public final class FlatDarculaTheme extends FlatLafTheme {

    public FlatDarculaTheme() {
        super("Darcula", "IntelliJ", "com.formdev.flatlaf.FlatDarculaLaf", true);
    }
}
