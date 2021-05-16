package gamedata;

import java.awt.Paint;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/**
 * Describes the various expansion symbols needed to decorate components. For
 * example, a particular game's components might make use of "Light", "Dark",
 * and "Golden" versions of an expansion's symbol, depending on the component
 * type. Templates are used by SE to help the user construct custom expansions
 * for personal use.
 *
 * <p>
 * <b>Visual Variants versus Logical Variants</b><br>
 * The template allows games to distinguish between <i>visual variants</i>
 * (different expansion symbol styles) and <i>logical variants</i>
 * (different semantic variants). For example, a game might use icons that are
 * different colours for different types of components (visual variants), and
 * draw the icon in different places depending on whether the icon is indicating
 * that the component <i>belongs</i> to an expansion or
 * <i>requires</i> that expansion (logical variants). The default expansion
 * symbol painting mechanism assumes that these two classes are identical (that
 * is, that each visual variant is also a logical variant); selecting a logical
 * variant will result in the corresponding visual variant being painted.
 *
 * <p>
 * In Strange Eons, the <b>Variant</b> menu will be populated from the logical
 * variant list, and the expansion setting on the component will be set with the
 * index of the logical variant. When creating a new expansion using
 * <b>Expansion|New</b>, the visual variant list will be used so that the user
 * can supply appropriate graphics for the different visual variants.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface ExpansionSymbolTemplate {

    /**
     * Returns the number of variant symbols needed for components of the game
     * this template describes. This is not the number of expansion symbols, but
     * the number of variant styles required for each symbol. For example, for
     * one game all of the symbols might be black. That is one symbol variant.
     * Another game might use black symbols on light cards, and white symbols on
     * dark cards. That is two symbol variants.
     *
     * @return the number of symbol variants
     */
    int getVariantCount();

    /**
     * Returns the name of the {@code n}th symbol variant; this is a simple
     * description in the UI language; e.g., "Regular", "Inversed".
     *
     * @param variant the variant number of the symbol
     * @return a description of the symbol
     */
    String getVariantName(int variant);

    /**
     * Returns an icon that can accompany the variant name to visually
     * distinguish the style of the variant. A typical icon is a disc filled
     * with a characteristic colour taken from the variant's design.
     *
     * @param variant the variant number
     * @return a simple icon that helps distinguish the variant
     */
    Icon getVariantIcon(int variant);

    /**
     * Returns the number of logical variants for games that distinguish between
     * visual variants and logical variants.
     *
     * @return the number of logical variants
     */
    int getLogicalVariantCount();

    /**
     * Returns the name of the {@code n}th logical variant for games that
     * distinguish between visual variants and logical variants.
     *
     * @param variant the logical variant number
     * @return a description of the logical variant
     */
    String getLogicalVariantName(int variant);

    /**
     * Returns an icon that can accompany the logical variant name to visually
     * distinguish the the variant.
     *
     * @param variant the logical variant number
     * @return a simple icon that helps distinguish the logical variant
     */
    Icon getLogicalVariantIcon(int variant);

    /**
     * Returns a variant of the default symbol. This is a sample image that can
     * be used if the user elects not to provide one. For example, the standard
     * expansion template uses asterisk graphics for this purpose.
     *
     * @param variant the number of the symbol
     * @return a description of the symbol
     */
    BufferedImage getDefaultSymbol(int variant);

    /**
     * Returns a suggested paint to use as a backdrop for this variant. This can
     * be used when selecting or editing a symbol with this variant. For
     * example, the end user expansion dialog (Expansion|New) will use this for
     * the drop boxes that the user drops symbol images on. If this method
     * returns {@code null}, a default paint will be used.
     *
     * @param variant the index of the variant to obtain a backdrop for
     * @return a paint to use as the backdrop, or {@code null} to use the
     * default
     */
    Paint getDesignBackdropForVariant(int variant);

    /**
     * Returns {@code true} if the components for this game will draw the
     * expansion symbols themselves instead of relying on the default mechanism.
     *
     * @return {@code true} if expansion symbols are drawn by the
     * component; {@code false} if expansion symbols are drawn by Strange
     * Eons
     */
    boolean isCustomDrawn();

    /**
     * Returns {@code true} if this template can automatically generate a
     * family of variants given an example image.
     *
     * @return {@code true} if
     * {@link #generateVariant(java.awt.image.BufferedImage, int)} is supported
     */
    boolean canGenerateVariantsAutomatically();

    /**
     * Given an example image, generates a variant automatically.
     *
     * @param baseSymbol the example symbol to base the new design on; usually
     * it has the same style as the symbol at position 0
     * @param variant the number of the symbol design to generate
     * @return the generated symbol for position {@code n}
     * @throws UnsupportedOperationException if this template cannot generate
     * symbols
     */
    BufferedImage generateVariant(BufferedImage baseSymbol, int variant);
}
