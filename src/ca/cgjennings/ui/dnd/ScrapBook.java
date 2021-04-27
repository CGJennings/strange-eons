package ca.cgjennings.ui.dnd;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import resources.ResourceKit;

/**
 * This class provides a simple interface for accessing common data types from
 * the clipboard.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ScrapBook {

    private ScrapBook() {
    }

    /**
     * Sets the clipboard text to the specified string.
     *
     * @param content the string to set as the clipboard content
     */
    public static void setText(String content) {
        setText(content, null);
    }

    /**
     * Sets the clipboard text to the specified string, using the specified
     * owner.
     *
     * @param content the string to set as the clipboard content
     * @param owner the content owner to specify
     */
    public static void setText(String content, ClipboardOwner owner) {
        check(content);
        StringSelection sel = new StringSelection(content);
        getClipboard().setContents(sel, owner);
    }

    /**
     * Returns the current text on the clipboard, or <code>null</code> if the
     * clipboard content does not include text.
     *
     * @return the current clipboard text
     */
    public static String getText() {
        return (String) get(DataFlavor.stringFlavor);
    }

    /**
     * Returns whether there is text available on the clipboard.
     *
     * @return <code>true</code> if the clipboard contains text (or can be
     * converted to text)
     */
    public static boolean isTextAvailable() {
        return available(DataFlavor.stringFlavor);
    }

    /**
     * Returns the text stored in the system selection on platforms that support
     * this feature. Returns <code>null</code> if the platform does not have a
     * system selection or it does not have any text available.
     *
     * @return the system selection text
     */
    public static String getSystemSelectionText() {
        try {
            Clipboard syssel = Toolkit.getDefaultToolkit().getSystemSelection();
            if (syssel != null) {
                return (String) get(syssel, DataFlavor.stringFlavor);
            }
        } catch (IllegalStateException ise) {
            // for some reason I have gotten an IllegalStateException here
            // on Windows, even though Windows has no system selection and
            // therefore should return null; by catching that exception here,
            // we will return null as we should
        }
        return null;
    }

    /**
     * Sets the text stored in the system selection on platforms that support
     * this feature. Has no effect if the platform does not include the concept
     * of a system selection.
     *
     * @param content the system selection text to set
     */
    public static void setSystemSelectionText(String content) {
        check(content);
        try {
            Clipboard syssel = Toolkit.getDefaultToolkit().getSystemSelection();
            if (syssel != null) {
                StringSelection sel = new StringSelection(content);
                syssel.setContents(sel, sel);
            }
        } catch (IllegalStateException ise) {
            /* see getSystemSelectionText */ }
    }

    /**
     * Sets the clipboard content to an image. Note that some platforms may not
     * support some image features. For example, Windows platforms may not
     * support image transparency (alpha channels).
     *
     * @param content the image to place on the clipboard
     */
    public static void setImage(Image content) {
        setImage(content, null);
    }

    /**
     * Sets the clipboard content to the specified image, using the specified
     * owner. Note that some platforms may not support some image features. For
     * example, Windows platforms may not support image transparency (alpha
     * channels).
     *
     * @param content the image to place on the clipboard
     * @param owner the content owner to specify
     */
    public static void setImage(Image content, ClipboardOwner owner) {
        check(content);
        ImageSelection sel = new ImageSelection(content);
        getClipboard().setContents(sel, owner == null ? sel : owner);
    }

    /**
     * Returns the image stored on the clipboard, or <code>null</code> if the
     * clipboard content does not consist of an image.
     *
     * @return the image on the clipboard, or <code>null</code>
     */
    public static BufferedImage getImage() {
        Image im = (Image) get(DataFlavor.imageFlavor);
        if (im != null) {
            return ResourceKit.prepareNewImage(im);
        }
        return null;
    }

    /**
     * Returns whether an image is available on the clipboard.
     *
     * @return <code>true</code> if an image is available
     */
    public static boolean isImageAvailable() {
        return available(DataFlavor.imageFlavor);
    }

    /**
     * Sets the clipboard content to a list of files.
     *
     * @param content an array of the files to list on the clipboard
     */
    public static void setFiles(File[] content) {
        check(content);
        FileListSelection sel = new FileListSelection(content);
        getClipboard().setContents(sel, null);
    }

    /**
     * Sets the clipboard content to a list of files.
     *
     * @param content a list of the files to place on the clipboard
     */
    public static void setFiles(List<File> content) {
        setFiles(content, null);
    }

    /**
     * Sets the clipboard content to a list of files, using the specified owner.
     *
     * @param content a list of the files to place on the clipboard
     * @param owner the content owner to specify
     */
    public static void setFiles(List<File> content, ClipboardOwner owner) {
        check(content);
        FileListSelection sel = new FileListSelection(content);
        getClipboard().setContents(sel, owner == null ? sel : owner);
    }

    /**
     * Returns the list of files on the clipboard as an array of {@link File}
     * objects, or <code>null</code>.
     *
     * @return the files on the clipboard, or <code>null</code>
     */
    public static File[] getFiles() {
        Object listObj = get(DataFlavor.javaFileListFlavor);
        if (listObj != null) {
            List fileList = (List) listObj;
            File[] files = new File[fileList.size()];
            int i = 0;
            for (Object o : fileList) {
                files[i++] = (File) o;
            }
            return files;
        }
        // OPTION: try to parse text as list of files
//		else {
//			for( DataFlavor f : getClipboard().getAvailableDataFlavors() ) {
//				if( f.isRepresentationClassReader() ) {
//					Reader r = null;
//					try {
//						r = f.getReaderForText( getClipboard().getContents( null ) );
//						List<File> files = new LinkedList<File>();
//						BufferedReader lineReader = new BufferedReader( r );
//						String line;
//						while( (line = lineReader.readLine()) != null ) {
//							// KDE may add this to end of list
//							if( line.equals( "\0" ) ) continue;
//							files.add( new File( line ) );
//						}
//						return files.toArray( new File[ files.size() ] );
//					} catch( IOException e ) {
//					} catch( UnsupportedFlavorException e ) {
//					} finally {
//						if( r != null ) try { r.close(); } catch( IOException e ) {}
//					}
//				}
//			}
//		}
        return new File[0];
    }

    /**
     * Returns whether one or more files are available on the clipboard.
     *
     * @return <code>true</code> if files are available
     */
    public static boolean isFileAvailable() {
        return available(DataFlavor.javaFileListFlavor);
    }

    private static Object get(DataFlavor flavour) {
        return get(getClipboard(), flavour);
    }

    private static Object get(Clipboard clipboard, DataFlavor flavour) {
        Transferable t = clipboard.getContents(null);
        try {
            if (t != null && t.isDataFlavorSupported(flavour)) {
                return t.getTransferData(flavour);
            }
        } catch (Exception ex) {
        }
        return null;
    }

    private static boolean available(DataFlavor flavour) {
        return getClipboard().isDataFlavorAvailable(flavour);
    }

    private static Clipboard getClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    private static void check(Object content) {
        if (content == null) {
            throw new NullPointerException("null content");
        }
    }

    /**
     * A base class for creating transferables with exactly one acceptable
     * {@link DataFlavor}.
     *
     * @param <K> the type of object represented by the selection
     */
    public static class Selection<K> implements Transferable, ClipboardOwner {

        protected DataFlavor acceptedflavor;
        protected K content;

        public Selection(K content, DataFlavor acceptedFlavor) {
            this.content = content;
            this.acceptedflavor = acceptedFlavor;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{acceptedflavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return acceptedflavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!acceptedflavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return content;
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            // does nothing
        }
    }

    /**
     * A transferable implementation for images.
     */
    public static class ImageSelection extends Selection<Image> {

        public ImageSelection(Image content) {
            super(content, DataFlavor.imageFlavor);
        }
    }

    /**
     * A transferable implementation for lists of {@link File}s.
     */
    public static class FileListSelection extends Selection<List<File>> {

        public FileListSelection(List<File> content) {
            super(content, DataFlavor.javaFileListFlavor);
        }

        public FileListSelection(File[] content) {
            this(Arrays.asList(content));
        }
    }

//	public static void main( String[] args ) {
//		System.out.println( "Text: " + getText() );
//		System.out.println( "Image: " + getImage() );
//		System.out.print( "File List: " );
//		File[] files = getFiles();
//		for( int i=0; i<files.length; ++i ) System.out.println( files[i] ); System.out.println( files.length );
//	}
}
