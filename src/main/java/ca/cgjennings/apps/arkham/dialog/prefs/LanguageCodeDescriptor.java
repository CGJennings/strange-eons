package ca.cgjennings.apps.arkham.dialog.prefs;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.IconProvider;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.util.Locale;
import javax.swing.Icon;
import resources.Language;

public class LanguageCodeDescriptor implements IconProvider {

    private String descriptor;
    private Icon icon;
    private Locale locale;
    private boolean disabled;
    
    public LanguageCodeDescriptor(Locale locale) {
        this.disabled = disabled;
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        if (locale.getLanguage().isEmpty()) {
            throw new IllegalArgumentException("no language: " + locale);
        }
        if (!locale.getVariant().isEmpty()) {
            locale = new Locale(locale.getLanguage(), locale.getCountry());
        }
        this.locale = locale;

        if (locale.getCountry().isEmpty()) {
            icon = Language.getIconForLanguage(locale); // disable effect for languages looks funky
        } else {
            icon = new IndentedIcon(icon(Language.getIconForCountry(locale)));
        }
        descriptor = getLocaleDescription(locale);
    }

    private Icon icon(Icon i) {
        if (disabled) {
            return ImageUtilities.createDisabledIcon(i);
        }
        return i;
    }

    public static String getLocaleDescription(Locale localeToDescribe) {
        return getLocaleDescription(localeToDescribe, null);
    }

    public static String getLocaleDescription(Locale localeToDescribe, Locale describeInLocale) {
        if (describeInLocale == null) {
            describeInLocale = Locale.getDefault();
        }
        if (localeToDescribe.getLanguage().isEmpty()) {
            throw new IllegalArgumentException("locale has no language");
        }
        if (localeToDescribe.getCountry().isEmpty()) {
            return localeToDescribe.getDisplayLanguage(describeInLocale);
        }
        ComponentOrientation co = ComponentOrientation.getOrientation(describeInLocale);
        if (co.isLeftToRight()) {
            return localeToDescribe.getDisplayLanguage(describeInLocale) + '-' + localeToDescribe.getDisplayCountry(describeInLocale);
        } else {
            return localeToDescribe.getDisplayCountry(describeInLocale) + '-' + localeToDescribe.getDisplayLanguage(describeInLocale);
        }
    }

    public String getCode() {
        return locale.toString();
    }

    public Locale getLocale() {
        return locale;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }
    
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public String toString() {
        return descriptor;
    }

    private static class IndentedIcon implements Icon {

        private final Icon icon;

        public IndentedIcon(Icon i) {
            this.icon = i;
        }

        @Override
        public int getIconHeight() {
            return icon.getIconHeight();
        }

        @Override
        public int getIconWidth() {
            return icon.getIconWidth() + INDENT;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (c == null || c.getComponentOrientation().isLeftToRight()) {
                x += INDENT;
            }
            icon.paintIcon(c, g, x, y);
        }
        private static final int INDENT = 10;
    }
}
