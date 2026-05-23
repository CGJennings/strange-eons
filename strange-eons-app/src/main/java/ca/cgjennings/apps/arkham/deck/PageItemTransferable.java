package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Transferable implementation for page items.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
final class PageItemTransferable implements Transferable {

    private PageItem[] source;

    /**
     * Creates a new transferable for the specified items. The original items
     * can be mutated without affecting the transferable.
     *
     * @param source the items to create a transferable
     * @throws NullPointerException if the source array is {@code null} or
     * contains a {@code null} element
     */
    public PageItemTransferable(PageItem[] source) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        try {
            this.source = copy(source);
        } catch (NullPointerException e) {
            throw new NullPointerException("source[i]");
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DATA_FLAVOR.equals(flavor);
    }

    @Override
    public PageItem[] getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!DATA_FLAVOR.equals(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return source;
    }

    /**
     * The data flavor used to transfer deck selections via drag-and-drop and
     * the clipboard.
     */
    public static final DataFlavor DATA_FLAVOR;

    static {
        try {
            DATA_FLAVOR = new DataFlavor(
                    DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=\"" + PageItem[].class.getName() + "\""
            );
        } catch (ClassNotFoundException e) {
            StrangeEons.log.log(Level.SEVERE, null, e);
            throw new Error();
        }
    }

    static PageItem[] copy(PageItem[] target) {
        PageItem[] copy = target.clone();
        for (int i = 0; i < copy.length; ++i) {
            copy[i] = copy[i].clone();
        }
        return copy;
    }
}
