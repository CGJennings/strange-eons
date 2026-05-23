package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.BusyDialog;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

/**
 * A wrapper for {@link Printable}s that updates the currently displayed
 * {@link BusyDialog}'s progress bar to reflect the currently printing page
 * index.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class MonitoredPrintable implements Printable {

    private final Printable monitoredPrintable;
    private final int totalPageCount;
    private int maxPage = -1;

    /**
     * Creates a new monitored printable that delegates printing to the
     * specified Printable.
     *
     * @param monitoredPrintable the printable that will be called to print page
     * content
     * @param totalPageCount the number of pages that will be printed
     */
    public MonitoredPrintable(Printable monitoredPrintable, int totalPageCount) {
        this.monitoredPrintable = monitoredPrintable;
        this.totalPageCount = totalPageCount;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (maxPage == -1) {
            BusyDialog.maximumProgress(totalPageCount);
        }
        int exists = monitoredPrintable.print(graphics, pageFormat, pageIndex);
        if (exists == Printable.PAGE_EXISTS) {
            maxPage = Math.max(maxPage, pageIndex);
            BusyDialog.currentProgress(maxPage + 1);
        }
        return exists;
    }
}
