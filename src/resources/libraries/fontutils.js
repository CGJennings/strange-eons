/*
 fontutils.js - version 8
 Support for finding and registering fonts.
 */
const FontUtils = {
    findMatchingFamily(families, defaultFamily) {
        if (defaultFamily === undefined) {
            defaultFamily = ResourceKit.getBodyFamily();
        }
        return ResourceKit.findAvailableFontFamily(families, defaultFamily);
    },
    availableFontFamilies() {
        var families = Array.from(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames(Language.getInterfaceLocale())
                );
        return families;
    },
    registerFontFamily(key) {
        return useLibrary.__$.getSettingProvider().registerFontFamily(key);
    },
    registerFontFamilyFromResources() {
        if (arguments.length === 0) {
            throw new Error("at least one resource file name required");
        }
        var b = "";
        for (var i = 0; i < arguments.length; ++i) {
            if (i > 0)
                b += ",";
            b += arguments[i];
        }
        return ResourceKit.registerFontFamily(b.toString())[0].family;
    }
};