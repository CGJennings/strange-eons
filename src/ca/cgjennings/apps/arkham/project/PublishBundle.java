package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog;
import ca.cgjennings.apps.arkham.plugins.catalog.Listing;
import ca.cgjennings.apps.arkham.plugins.catalog.MD5Checksum;
import ca.cgjennings.apps.arkham.plugins.catalog.PluginBundlePublisher;
import static ca.cgjennings.apps.arkham.plugins.catalog.PluginBundlePublisher.*;
import ca.cgjennings.apps.arkham.plugins.catalog.PluginBundlePublisher.CompressionMethod;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import static resources.Language.string;

/**
 * Task action that prepares a bundle for publication in a catalogue by creating
 * an equivalent <i>published bundle</i>. Published bundles are a highly
 * compressed network transport format used by the catalogue system to reduce
 * network use when installing plug-ins from a catalogue server.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @see PluginBundlePublisher
 * @see Catalog
 */
public class PublishBundle extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-pub-bundle");
    }

    @Override
    public String getDescription() {
        return string("pa-pub-bundle-tt");
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member != null && ProjectUtilities.matchExtension(member, ProjectUtilities.BUNDLE_EXTENSIONS);
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        if (member == null) {
            return false;
        }

        final File f = member.getFile();

        BusyDialog d = new BusyDialog(StrangeEons.getWindow(), string("pa-pub-bundle-busy"), () -> {
            StrangeEonsAppWindow af = StrangeEons.getWindow();
            try {
                af.setWaitCursor();
                Listing listing = publish(f, null);
                PrintWriter con = ScriptMonkey.getSharedConsole().getWriter();
                con.println(listing.toString());
            } finally {
                af.setDefaultCursor();
            }
        });

        member.getParent().synchronize();

        return true;
    }

    /**
     * Converts a bundle to published format and returns a {@link Listing}
     * containing any catalog information specified by the bundle's root file.
     *
     * @param srcBundle the plug-in bundle to publish
     * @param method the compression method, expressed as an integer
     * @return a listing containing catalog information from the root file
     * @deprecated Takes an integer compression method for backwards
     * compatibility. Use
     * {@link #publish(java.io.File, ca.cgjennings.apps.arkham.plugins.catalog.PluginBundlePublisher.CompressionMethod)}
     * instead.
     */
    @Deprecated
    public static Listing publish(File srcBundle, int method) {
        CompressionMethod cm;
        switch (method) {
            case 0:
                cm = CompressionMethod.BZIP2;
                break;
            case 1:
                cm = CompressionMethod.GZIP;
                break;
            case 2:
                cm = CompressionMethod.LZMA;
                break;
            default:
                cm = null;
        }
        return publish(srcBundle, cm);
    }

    /**
     * Converts a bundle to published format and returns a {@link Listing}
     * containing any catalog information specified by the bundle's root file.
     * If publication fails, an error is displayed to the user and the method
     * returns {@code null}. The caller can specify a compression method to
     * use for the bundle; if this is {@code null}, then both BZIP2 and
     * LZMA compression will be tested and the one which yields the smallest
     * published bundle size will be used.
     *
     * @param srcBundle the plug-in bundle to publish
     * @param method the compression method to use
     * @return catalog listing information contained in the plug-in root
     */
    public static Listing publish(File srcBundle, CompressionMethod method) {
        BusyDialog.maximumProgress(12);

        File unwrapFile = new File(srcBundle + ".unwraptmp");
        File verifyFile = new File(srcBundle + ".verifytmp");

        CompressionMethod method2;
        String ext, ext2;
        File outFile, outFile2;
        if (method == null) {
            method = CompressionMethod.BZIP2;
            method2 = CompressionMethod.LZMA;
            ext = method.getPublishedBundleExtension();
            ext2 = method2.getPublishedBundleExtension();
            outFile = new File(srcBundle + ext);
            outFile2 = new File(srcBundle + ext2);
        } else {
            method2 = null;
            ext = method.getPublishedBundleExtension();
            ext2 = null;
            outFile = new File(srcBundle + ext);
            outFile2 = null;
        }

        // create a catalog listing that combines info extracted from the
        // bundle and generated by the publication process (e.g., MD5)
        File srcFile = srcBundle;
        Listing listing;
        long downloadSize;
        long installSize;
        try {
            PluginBundle pb = new PluginBundle(srcBundle);

            // unwrap a wrapped bundle to plain format
            if (pb.getFormat() != PluginBundle.FORMAT_PLAIN) {
                BusyDialog.statusText(string("pa-pub-bundle-s0")); //unwrapping
                pb.copy(unwrapFile);
                pb = new PluginBundle(unwrapFile);
                srcFile = unwrapFile;
            }

            // create a listing, copying relevant properties from the root file
            listing = new Listing(srcBundle);

            // enusure bundle itself is uncompressed, then
            // compute an integrity hash for the uncompressed bundle
            BusyDialog.statusText(string("pa-pub-bundle-s2"));
            BusyDialog.currentProgress(4);
            srcFile = pb.createUncompressedArchive();
            installSize = srcFile.length();
            MD5Checksum md5Verify = MD5Checksum.forFile(srcFile);

            // compres the bundle
            BusyDialog.currentProgress(5);
            BusyDialog.statusText(string("pa-pub-bundle-s3"));
            compress(srcFile, outFile, method);
            downloadSize = outFile.length();
            // if we are detecting the best compression, try
            // the second method, figure out which is best and delete the other
            if (method2 != null) {
                compress(srcFile, outFile2, method2);
                long downloadSize2 = outFile2.length();
                if (downloadSize2 <= downloadSize) { // use <= since LZMA faster than BZIP2
                    outFile.delete();
                    method = method2;
                    outFile = outFile2;
                    ext = ext2;
                    downloadSize = downloadSize2;
                } else {
                    outFile2.delete();
                }
                // after this point, the "2" variables (method2, etc.) are no longer
                // needed (and may not be valid); the normal variable name holds
                // the winner
            }

            // compute an integrity hash for the download; this is used for the
            // catalog, while md5verify is used to verify the publishing process below
            MD5Checksum md5ForDownload = MD5Checksum.forFile(outFile);

            // now verify the published bundle
            BusyDialog.currentProgress(8);
            BusyDialog.statusText(string("pa-pub-bundle-s4")); // verify
            decompress(outFile, verifyFile, method);
            BusyDialog.currentProgress(11);
            MD5Checksum md5Verify2 = MD5Checksum.forFile(verifyFile);
            BusyDialog.currentProgress(12);

            if (!md5Verify.matches(md5Verify2.getChecksumString())) {
                throw new IOException("the bundle failed the verification test");
            }

            if (downloadSize != 0L) {
                listing.set(Listing.SIZE, String.valueOf(downloadSize));
            }
            if (installSize != 0L) {
                listing.set(Listing.INSTALL_SIZE, String.valueOf(installSize));
            }
            listing.setChecksum(md5ForDownload);

            // add bundle extension to URL
            listing.set(Listing.URL, listing.get(Listing.URL) + ext);

            // if the compression method requires a certain build number,
            // update the listing's minimum build if necessary
            int minVer = method.getMinimumBuildNumber();
            if (minVer > 0) {
                int listedVersion = 0;
                String v = listing.get(Listing.MINIMUM_VERSION);
                if (v != null) {
                    try {
                        listedVersion = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        // listedVersion = 0;
                    }
                }
                if (listedVersion < minVer) {
                    listing.set(Listing.MINIMUM_VERSION, String.valueOf(minVer));
                }
            }

        } catch (IOException e) {
            outFile.delete();
            ErrorDialog.displayError(string("prj-err-convert", srcBundle.getName()), e);
            return null;
        } finally {
            unwrapFile.delete();
            verifyFile.delete();
        }

        return listing;
    }

    private static volatile boolean openOperationCancelled;

    static {
        Open.registerOpener(new Open.InternalOpener() {
            @Override
            public boolean appliesTo(File f) {
                return ProjectUtilities.matchExtension(f, PUBLISHED_EXTS);
            }

            @Override
            public void open(final File f) throws Exception {
                // remove the .pbz extension
                final File dest = ProjectUtilities.getAvailableFile(
                        ProjectUtilities.changeExtension(f, null)
                );
                openOperationCancelled = false;
                new BusyDialog(string("cat-unpack"), () -> {
                    BusyDialog.maximumProgress(1000);
                    try {
                        PluginBundlePublisher.publishedBundleToPluginBundle(f, dest);
                    }catch (IOException e) {
                        StrangeEons.log.log(Level.SEVERE, "uncaught unpack exception", e);
                        ErrorDialog.displayError(string("prj-err-convert", f.getName()), e);
                    }
                }, (ActionEvent e) -> {
                    openOperationCancelled = true;
                });

                if (openOperationCancelled) {
                    dest.delete();
                } else {
                    Project p = StrangeEons.getWindow().getOpenProject();
                    if (p != null) {
                        Member m = p.findMember(dest);
                        if (m != null && p.getView() != null) {
                            p.getView().select(m);
                        }
                    }
                }
            }
        });
    }
    private static final String[] PUBLISHED_EXTS = new String[]{
        "pgz", "pbz", "plzm"
    };
}
