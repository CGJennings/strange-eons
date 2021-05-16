package ca.cgjennings.apps.arkham.component;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.io.SEObjectInputStream;
import ca.cgjennings.io.SEObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;

/**
 * Provides access to the game component metadata stored in modern game
 * component files. Depending on the version of Strange Eons that wrote the
 * file, different levels of metadata will be available. You can determine which
 * level of metadata is supported by calling {@link #getMetadataVersion()}. The
 * following table lists the metadata available for each possible version
 * (higher version numbers include all of the data available from lower version
 * numbers):
 * <dl>
 * <dt>Version -1
 * <dd>No metadata available.
 * <dt>Version 1
 * <dd>Class name, component name, deck layout support.
 * <dt>Version 2
 * <dd>Build number, DIY script resource.
 * </dl>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.00
 */
public class ComponentMetadata {

    private final String[] data;
    private static final int VERSION = 0;
    private static final int CLASS = 1;
    private static final int NAME = 2;
    private static final int NO_DECK_LAYOUT = 3;

    private static final int BUILD = 4;
    private static final int DIY_SCRIPT = 5;

    private static final int MAX_ENTRIES = 6;
    private static final int CURRENT_VERSION = 2;

    /**
     * Creates a new {@code ComponentMetadata} instance that reads metadata
     * from the specified file, if available. If there is an error while reading
     * the metadata, the resulting object will be identical to that of a file
     * that does not contain metadata.
     *
     * @param source the file to component file to read metadata from
     */
    public ComponentMetadata(File source) {
        data = readRawMetaData(source);
    }

    /**
     * Returns the version of the metadata available from the file. Older
     * components that have no metadata will return -1. If the actual version of
     * the metadata in the file is greater than the version understood by this
     * class, the version number is capped at the version understood by this
     * class.
     *
     * @return the version of the metadata available through this class, or -1
     * if no metadata is available
     */
    public int getMetadataVersion() {
        if (data != null) {
            try {
                int ver = Integer.parseInt(data[VERSION]);
                if (ver > CURRENT_VERSION) {
                    ver = CURRENT_VERSION;
                }
                return ver;
            } catch (NumberFormatException e) {
            }
        }
        return -1;
    }

    /**
     * Returns the build number of the version of Strange Eons that this file
     * was written from, or -1 if there is no metadata or the metadata version
     * is less than 2.
     *
     * @return the build number, or -1 if this is unavailable
     */
    public int getBuildNumber() {
        if (data != null && data[BUILD] != null) {
            try {
                return Integer.parseInt(data[BUILD]);
            } catch (NumberFormatException e) {
            }
        }
        return -1;
    }

    /**
     * Returns the {@code Class} instance that the game component is an
     * instance of. Returns {@code null} if the component has no metadata.
     *
     * @return the class representing the component
     * @throws ClassNotFoundException if the class cannot currently be loaded;
     * for example, if it is part of a plug-in that is not currently installed
     */
    @SuppressWarnings("unchecked")
    public Class<? extends GameComponent> getComponentClass() throws ClassNotFoundException {
        if (data == null) {
            return null;
        }
        return (Class<? extends GameComponent>) Class.forName(data[CLASS]);
    }

    /**
     * Returns the name of the class that the game component is an instance of.
     * Returns {@code null} if the component has no metadata.
     *
     * @return the class name, if available
     */
    public String getComponentClassName() {
        return data == null ? null : data[CLASS];
    }

    /**
     * Returns the name of the component, as would be returned by calling
     * {@link GameComponent#getFullName()} on the actual component.
     *
     * @return the component's name, or {@code null} if not available
     */
    public String getName() {
        return data == null ? null : data[NAME];
    }

    /**
     * Returns {@code true} if the component has metadata and can be
     * included in a deck. Most components can be included in a deck, but some
     * can't, including case books and other decks.
     *
     * @return {@code true} if it is known for certain that the component
     * can be placed in a deck
     */
    public boolean isDeckLayoutSupported() {
        return data != null && data[NO_DECK_LAYOUT] == null;
    }

    /**
     * Returns the resource identifier of the script file used to create the
     * component, if it is a DIY component and uses a script. Returns
     * {@code null} if the metadata version is less than 2 or the component
     * is not a script-based DIY component.
     *
     * @return the name of the DIY script, or {@code null}
     */
    public String getDIYScriptResource() {
        return data == null ? null : data[DIY_SCRIPT];
    }

    /**
     * Writes metadata for a component at the head of a component stream.
     *
     * @param out the stream to write to
     * @param gc the component to produce metadata for
     * @throws IOException if an error occurs while writing to the stream
     */
    public static void writeMetadataToStream(SEObjectOutputStream out, GameComponent gc) throws IOException {
        out.writeObject("METADATA");
        // V1
        out.writeObject(String.valueOf(CURRENT_VERSION));
        out.writeObject(gc.getClass().getName());
        out.writeObject(gc.getFullName());
        out.writeObject(gc.isDeckLayoutSupported() ? null : "NDL");

        // V2
        out.writeObject(StrangeEons.getBuildNumber());
        if (gc instanceof DIY) {
            out.writeObject(((DIY) gc).getHandlerScript());
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Given a version string read from the head of some metadata, return the
     * total number of entries to expect, including the version entry. In the
     * event of an error, returns -1.
     *
     * @param versionString the extracted version number string
     * @return the number of entries to read for that version, or -1
     */
    private static int getNumEntries(String versionString) {
        int v = -1;
        try {
            v = Integer.parseInt(versionString);
        } catch (NumberFormatException e) {
            return -1;
        }
        // funky
        if (v < 1) {
            StrangeEons.log.log(Level.SEVERE, "invalid metadata version ", v);
            return -1;
        }

        switch (v) {
            case 1:
                return 4;

            // treat anything higher than current version as current version
            default:
                return 6;
        }
    }

    /**
     * Reads the raw metadata header and returns it as an array of strings.
     * Returns {@code null} if no metadata is available or there is an
     * error.
     *
     * @param source the file to read
     * @return an array of metadata strings, or {@code null}
     */
    private static String[] readRawMetaData(File source) {
        // check for metadata byte signature
        boolean foundSignature = false;
        FileInputStream sigin = null;
        try {
            final int SIGBUFF = 64;
            sigin = new FileInputStream(source);
            byte[] sigbytes = new byte[SIGBUFF];
            for (int i = 0; i < SIGBUFF; ++i) {
                int b = sigin.read();
                if (b < 0) {
                    break;
                }
                sigbytes[i] = (byte) b;
            }
            final int WINMAX = SIGBUFF - METADATA_SIGNATURE.length;
            for (int w = 0; w < WINMAX; ++w) {
                int s = 0;
                for (; s < METADATA_SIGNATURE.length && sigbytes[w + s] == METADATA_SIGNATURE[s]; ++s);
                if (s >= METADATA_SIGNATURE.length) {
                    foundSignature = true;
                    break;
                }
            }
        } catch (IOException e) {
        } finally {
            if (sigin != null) {
                try {
                    sigin.close();
                } catch (IOException e) {
                }
            }
        }
        if (!foundSignature) {
            return null;
        }

        // signature found; read in data
        ObjectInputStream in = null;
        try {
            in = new SEObjectInputStream(new FileInputStream(source));
            Object o = in.readObject();
            if ((o instanceof String) && ((String) o).equals("METADATA")) {
                String versionString = (String) in.readObject();
                int entries = getNumEntries(versionString);
                if (entries < 0) {
                    return null;
                }
                String[] metadata = new String[MAX_ENTRIES];
                metadata[0] = versionString;
                for (int i = 1; i < entries && i < MAX_ENTRIES; ++i) {
                    o = in.readObject();
                    // check for Integer due to a bug introduced when switching
                    // from microversion string to build number
                    if (o instanceof Integer) {
                        o = o.toString();
                    }
                    metadata[i] = (String) o;
                }
                return metadata;
            }
        } catch (Exception e) {
            StrangeEons.log.log(Level.SEVERE, "unexpected error reading metadata entry", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    private static final byte[] METADATA_SIGNATURE = {
        0x4D, 0x45, 0x54, 0x41, 0x44, 0x41, 0x54, 0x41
    };
}
