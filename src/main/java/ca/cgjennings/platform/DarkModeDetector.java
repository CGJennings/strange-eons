package ca.cgjennings.platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * A "dark mode" detector for common platforms. Modern desktops often support
 * selecting a dark theme that inverts the typical dark-text-on-light-background
 * form. This class can detect whether such a style is active on a number of
 * common platforms. If it cannot definitively establish that dark mode is
 * active, it will report that it is <strong>not</strong> active.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.2
 */
public final class DarkModeDetector {

    /**
     * Creates a new detector instance. The value of {@link #isDetected()} is
     * not accurate until {@link #detect()} has been called.
     */
    public DarkModeDetector() {
    }

    private boolean isDark = false;

    /**
     * Detects whether dark mode is active.
     *
     * @return true if dark mode was detected, false if dark mode could not be
     * detected
     */
    public boolean detect() {
        isDark = false;
        try {
            String command;
            String pattern;
            if (PlatformSupport.PLATFORM_IS_WINDOWS) {
                command = COMMAND_WIN;
                pattern = PATTERN_WIN;
            } else if (PlatformSupport.PLATFORM_IS_MAC) {
                command = COMMAND_MAC;
                pattern = PATTERN_MAC;
            } else {
                command = COMMAND_OTHER;
                pattern = PATTERN_OTHER;
            }

            String line;
            final Process proc = Runtime.getRuntime().exec(command);
            BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            Pattern regex = Pattern.compile(pattern);

            while ((line = r.readLine()) != null) {
                if (regex.matcher(line).find()) {
                    isDark = true;
                }
            }
        } catch (IOException ex) {
            // command likely failed, assume light mode
        }
        return isDark;
    }

    /**
     * Returns the result of the most recent detection. If no detection has been
     * performed, returns false.
     *
     * @return true if dark mode was detected after the most recent call to
     * {@link #detect}.
     */
    public boolean isDetected() {
        return isDark;
    }

    private static final String COMMAND_OTHER = "gsettings get org.gnome.desktop.interface gtk-theme";
    private static final String PATTERN_OTHER = "-dark['\"]?\\s*$";

    private static final String COMMAND_WIN = "reg query \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme";
    private static final String PATTERN_WIN = "REG_DWORD\\s*0x0\\s*$";

    private static final String COMMAND_MAC = "defaults read -g AppleInterfaceStyle";
    private static final String PATTERN_MAC = "^\\s*Dark\\s*$";
}
