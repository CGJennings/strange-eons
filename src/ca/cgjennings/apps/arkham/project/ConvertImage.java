package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import static resources.Language.string;
import resources.Settings;

/**
 * A task action tree that can convert and/or recompress a source image to one
 * of the standard Strange Eons image formats for plug-ins (png, jpg, jp2).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ConvertImage extends TaskActionTree {

    public ConvertImage() {
        add(new Converter("PNG", "png"));
        add(new Converter("JPEG", "jpg"));
        add(new Converter("JPEG2000", "jp2", "j2k"));
    }

    @Override
    public String getLabel() {
        return string("pa-ci-name");
    }

    @Override
    public String getDescription() {
        return string("pa-ci-tt");
    }

    /**
     * Setting key that stores that default value for the option to replace the
     * original file.
     */
    public static String KEY_REPLACE_ORIGINAL = "replace-converted-image";

    private static final String[] INPUT_FORMATS = ImageIO.getReaderFileSuffixes();

    static class Converter extends TaskAction {

        protected String ext;
        protected String label;
        protected String desc;

        public Converter(String formatDesc, String formatExtension) {
            this(formatDesc, formatExtension, null);
        }

        public Converter(String formatDesc, String formatExtension, String keyfrag) {
            if (formatExtension == null) {
                throw new NullPointerException();
            }
            ext = formatExtension;
            label = formatDesc;

            if (keyfrag == null) {
                keyfrag = ext;
            }
            desc = string("exf-" + keyfrag);
            int colon = desc.lastIndexOf(':');
            if (colon >= 0) {
                desc = desc.substring(0, colon).trim();
            }
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getActionName() {
            return "convertimage-" + ext;
        }

        @Override
        public String getDescription() {
            return desc;
        }

        @Override
        public boolean perform(Project project, Task task, Member member) {
            if (member != null) {
                return performOnSelection(new Member[]{member});
            }
            return false;
        }

        @Override
        public boolean appliesTo(Project project, Task task, Member member) {
            return member != null && ProjectUtilities.matchExtension(member, INPUT_FORMATS);
        }

        static void convert(File source, String sourceFormat, Object dest, ImageWriter writer, ImageWriteParam outparam) throws IOException {
            ImageOutputStream out = null;
            ImageReader reader = null;
            ImageInputStream in = null;

            try {
                Iterator<ImageReader> rit = ImageIO.getImageReadersBySuffix(sourceFormat);
                if (!rit.hasNext()) {
                    throw new IOException("Unable to read format: " + sourceFormat);
                }
                reader = rit.next();

                in = ImageIO.createImageInputStream(source);
                reader.setInput(in);
                ImageReadParam inparam = reader.getDefaultReadParam();
                IIOImage iio = reader.readAll(0, inparam);

                ImageTypeSpecifier its = ImageTypeSpecifier.createFromRenderedImage(iio.getRenderedImage());
                IIOMetadata imMetadata = writer.getDefaultImageMetadata(its, outparam);
                iio.setMetadata(imMetadata);

                out = ImageIO.createImageOutputStream(dest);
                writer.setOutput(out);
                writer.write(null, iio, outparam);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.INFO, null, e);
                    }
                }
                if (reader != null) {
                    try {
                        reader.dispose();
                    } catch (Exception e) {
                        StrangeEons.log.log(Level.INFO, null, e);
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.INFO, null, e);
                    }
                }
            }
        }

        private void write(Member source, File dest, ImageWriter writer, ImageWriteParam outparam) throws IOException {
            long start = System.currentTimeMillis();
            convert(source.getFile(), source.getExtension(), dest, writer, outparam);
            if (StrangeEons.log.isLoggable(Level.FINE)) {
                StrangeEons.log.fine(String.format("converted image to %s in %,d ms", dest.getName(), System.currentTimeMillis() - start));
            }
        }

        @Override
        public boolean performOnSelection(Member[] members) {
            if (members.length == 0) {
                return true;
            }

            Project proj = members[0].getProject();
            ImageWriter writer = null;
            boolean allOK = true;

            try {
                Iterator<ImageWriter> wit = ImageIO.getImageWritersBySuffix(ext);
                if (!wit.hasNext()) {
                    throw new IOException("Unable to write format: " + ext);
                }
                writer = wit.next();

                // allow user to choose compression settings
                ImageWriteParam outparam = writer.getDefaultWriteParam();
                ConvertImageDialog d = new ConvertImageDialog(StrangeEons.getWindow(), ext);
                boolean canReplace = members.length == 1 && ext.equalsIgnoreCase(members[0].getExtension());
                d.setReplaceable(canReplace);
                d.setPreviewOptions(members.length == 1, members[0].getExtension(), members[0].getFile(), writer);
                d.setImageWriteParam(outparam);
                d.getControlPanel().loadDefaults(ext);
                d.pack();
                proj.getView().moveToLocusOfAttention(d);
                if (!d.showDialog()) {
                    return false;
                }
                d.getControlPanel().saveDefaults(ext);

                for (int i = 0; i < members.length; ++i) {
                    if (appliesTo(proj, null, members[i])) {
                        File dest = null;
                        try {
                            boolean copyBackOverOriginal = canReplace && Settings.getUser().getYesNo(KEY_REPLACE_ORIGINAL);
                            dest = members[i].getFile().getParentFile();
                            dest = new File(dest, members[i].toString() + "." + ext);
                            dest = ProjectUtilities.getAvailableFile(dest);
                            write(members[i], dest, writer, outparam);
                            if (copyBackOverOriginal) {
                                // fixes corrupt JP2 bug when replacing original
                                // if the replace original option is active, double
                                // check that the file is legal before we overwrite
                                // the original
                                BufferedImage temp = ImageIO.read(dest);
                                if (temp != null) {
                                    ProjectUtilities.copyFile(dest, members[i].getFile());
                                    dest.delete();
                                } else {
                                    throw new IOException("the image that was written is corrupt");
                                }
                            }
                        } catch (Exception e) {
                            dest.delete();
                            ErrorDialog.displayError(string("prj-err-write", dest.getName()), e);
                            allOK = false;
                        }
                    }
                }

                if (members.length == 1) {
                    members[0].getParent().synchronize();
                } else {
                    proj.synchronizeAll();
                }

            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-create-new"), e);
                allOK = false;
            } finally {
                if (writer != null) {
                    writer.dispose();
                }
            }
            return allOK;
        }
    }
}
