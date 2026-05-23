package gamedata;

import java.awt.Color;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import resources.Language;
import resources.ResourceKit;

/**
 * Default implementation of {@link ExpansionSymbolTemplate} used by games that
 * do not define their own.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
class DefaultExpansionSymbolTemplate extends AbstractExpansionSymbolTemplate {

    private int variants;

    public DefaultExpansionSymbolTemplate() {
        this(2);
    }

    public DefaultExpansionSymbolTemplate(int variantCount) {
        variants = variantCount;
    }

    @Override
    public int getVariantCount() {
        return variants;
    }

    @Override
    public String getVariantName(int variant) {
        switch (variant) {
            case 0:
                return Language.string("expsym-desc-0");
            case 1:
                return Language.string("expsym-desc-1");
            case 2:
                return Language.string("expsym-desc-2");
        }
        throw new IndexOutOfBoundsException("invalid symbol: " + variant);
    }

    @Override
    public BufferedImage getDefaultSymbol(int variant) {
        if (defaultSymbols == null) {
            defaultSymbols = new BufferedImage[3];
            BufferedImage baseSymbol = ResourceKit.getImage("expansiontokens/XX.png");
            for (int i = 0; i < defaultSymbols.length; ++i) {
                defaultSymbols[i] = generateVariant(baseSymbol, i);
            }
        }
        if (variant >= 0 && variant < defaultSymbols.length) {
            return defaultSymbols[variant];
        }
        throw new IndexOutOfBoundsException("invalid symbol: " + variant);
    }
    private BufferedImage[] defaultSymbols;

    @Override
    public Paint getDesignBackdropForVariant(int variant) {
        if (variant == 0) {
            return new Color(0xeeeeee);
        } else if (variant == 1) {
            return new Color(0x111111);
        }
        return null;
    }

    @Override
    public boolean canGenerateVariantsAutomatically() {
        return true;
    }
};
