package ca.cgjennings.apps.arkham.deck.item;

import gamedata.TileSet;
import java.awt.image.BufferedImage;

/**
 * Interface implemented by scripted tiles in tile sets.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see TileSet
 */
public interface TileProvider {

    /**
     * Creates an new image and paints the current tile content on it.
     *
     * @param tile the tile being painted
     * @return an image of the tile content; it must have the same resolution as
     * that specified by the tile's tile set entry
     */
    BufferedImage createTileImage(Tile tile);
}
