package ca.cgjennings.ui.textedit;

/*
 * This is an adaptation of the {@code RSyntaxTextArea} error strip to customize
 * how the markers are painted. The adapted code is used under a BSD license, as
 * described in the about dialog template.
 * 
 * https://github.com/bobbylight/RSyntaxTextArea/blob/master/RSyntaxTextArea/src/main/java/org/fife/ui/rsyntaxtextarea/ErrorStrip.java
 */

import ca.cgjennings.ui.theme.Palette;
import java.awt.Color;
import java.awt.Graphics2D;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;

/**
 * Displays markers for points of interest in a code editor.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
final class ErrorStrip extends ErrorStripClone {
	/**
	 * Constructor.
	 *
	 * @param textArea The text area we are examining.
	 */
	public ErrorStrip(RSyntaxTextArea textArea) {
        super(textArea);
        setCaretMarkerColor(Palette.get.foreground.translucent.text);
        setCaretMarkerOnTop(true);
	}

    @Override
    protected void paintCaretMarker(Graphics2D g, int width, int height) {
        final int y0 = height / 2 + (height & 1);
        g.drawLine(0, y0, width, y0);
    }

    @Override
    protected void paintParserNoticeMarker(Graphics2D g, ParserNotice notice, int width, int height) {
        Color c = notice.getColor();
        if (c == null) {
            c = Palette.get.background.opaque.grey;
        }
        g.setColor(c);
        g.fillRect(0, 0, width, height);
    }
}