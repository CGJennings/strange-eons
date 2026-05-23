package ca.cgjennings.ui.theme;

/**
 * Theme that installs FlatLaf's IntelliJ light look and feel.
 *
 * @since 3.5
 */
public final class FlatIntelliJTheme extends FlatLafTheme {

    public FlatIntelliJTheme() {
        super("IntelliJ", "IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf", false);
    }
}
