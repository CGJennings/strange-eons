/*
 * Backwards compatibility for old scripts.
 * This script is run automatically after loading
 * the common library when compatibilty mode is enabled.
 */

useLibrary("imageutils");
useLibrary("fontutils");

// common.js
var uselibrary = useLibrary;
var usesettings = useSettings;
var subclass = (constructor, superConstructor) => constructor.subclass( superConstructor );
var error = s => Error.error(s);
error.handleUncaught = ex => Error.handleUncaught(ex);
error.missingLibrary = function( shortName, fileName ) {
	useLibrary.__threadassert();
	alert( string( "scriptlib-missing-lib-alert", shortName, fileName ), true );
	Error.error( string( "scriptlib-missing-lib", fileName ) );
};
var hide = () => {Console.visible = false;};


// extension.js
var offerToHideArkhamHorror = () => {};
var GameData = {
    registerGame(code, gameName, iconImage) {
        if (iconImage != null && !(iconImage instanceof java.awt.BufferedImage)) {
            iconImage = ImageUtils.get(iconImage.toString());
        }
        gamedata.Game.register(code, gameName, gameName, iconImage, null);
    },
    
    registerExpansion(code, name, iconImage, cardImage, invCardImage) {
        const BI = java.awt.image.BufferedImage;
        if (iconImage == null) {
            iconImage = 'icons/un-expansion-icon.png';
        }
        if (!(iconImage instanceof BI)) {
            iconImage = ImageUtils.get(iconImage.toString());
        }
        if (cardImage == null) {
            cardImage = 'expansiontokens/XX.png';
        }
        if (!(cardImage instanceof BI)) {
            cardImage = ImageUtils.get(cardImage.toString());
        }
        if (invCardImage == null) {
            invCardImage = ca.cgjennings.graphics.ImageUtilities.invert(
                    ca.cgjennings.graphics.ImageUtilities.desaturate(cardImage)
            );
        }
        if (!(invCardImage instanceof BI)) {
            invCardImage = ImageUtils.get(invCardImage.toString());
        }
        const game = gamedata.Game.allGamesInstance;
        gamedata.Expansion.register(game, code, name, name, iconImage, [cardImage, invCardImage]);
    },

    setGameForExpansion(exp, game) {
        gamedata.Expansion.setGameForExpansion(exp, game);
    },

    parseEditors(resource) {
        ClassMap.add(resource);
    },

    parseSettings(resource) {
        Settings.shared.addSettingsFrom(resource);
    }
};


// fontutils.js
var availableFontFamilies = () => FontUtils.availableFontFamilies();
var registerFontFamily = key => FontUtils.registerFontFamily(key);
var findMatchingFamily = (families, defaultFamily) => FontUtils.findMatchingFamily(families, defaultFamily);
var registerFontFamilyFromResources = function() {
    return FontUtils.registerFontFamilyFromResources.apply(this, arguments);
}


// imageutils.js
var Image = {
    STITCH_HORIZONTAL: 1,
    STITCH_VERTICAL: 2,
    HORIZONTAL: 1,
    VERTICAL: 2,
    create(w, h, t) {
        return ImageUtils.create(w, h, t);
    },
    createForResolution(d, w, h, t) {
        return ImageUtils.createForResolution(d, w, h, t);
    },
    fetchImageResource(u, c) {
        return ImageUtils.get(u, c);
    },
    fetchIconResource(u) {
        return ImageUtils.getIcon(u);
    },
    createIcon(i, s) {
        return ImageUtils.createIcon(i, s);
    },
    stitch(i1, i2, e) {
        return ImageUtils.stitch(i1, i2, e);
    },
    resize(i, w, h, f) {
        return ImageUtils.resize(i, w, h, f);
    },
    fit(i, w, h, f) {
        return ImageUtils.fit(i, w, h, f);
    },
    crop(i, x, y, w, h) {
        return ImageUtils.crop(i, x, y, w, h);
    },
    tint(i, h, s, b) {
        return ImageUtils.tint(i, h, s, b);
    },
    mirror(i, h, v) {
        return ImageUtils.mirror(i, h, v);
    },
    invert(i) {
        return ImageUtils.invert(i);
    },
    desaturate(i) {
        return ImageUtils.desaturate(i);
    },
    read(f) {
        return ImageUtils.read(f);
    },
    write(i, f, r, q, p, d) {
        return ImageUtils.write(i, f, r, q, p, d);
    },
    save(i, f) {
        return ImageUtils.save(i, f);
    },
    view(i, t, m) {
        return ImageUtils.view(i, t, m);
    }
};