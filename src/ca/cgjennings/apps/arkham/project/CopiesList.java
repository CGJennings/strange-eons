package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.util.SortedProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * A file that contains the desired number of copies of different components in
 * a deck task. Can be queried with a file name to retrieve the number of copies
 * of that item to include.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class CopiesList {

    private SortedProperties map = new SortedProperties();
    private File source = null;

    /**
     * Creates an empty list that returns 1 copy for all cards. This list cannot
     * be stored.
     */
    public CopiesList() {
    }

    /**
     * Creates a copies list using the standard copies list file for a task
     * folder.
     *
     * @param taskFolder the folder to fetch the copies list from
     * @throws IOException if an error occurs while reading the file
     */
    public CopiesList(Task taskFolder) throws IOException {
        this(new File(taskFolder.getFile(), DeckTask.getCopiesListFileName(taskFolder)));
    }

    /**
     * Creates a copies list using the entries stored in an arbitrary file.
     *
     * @param copiesListFile the file containing the copy information
     * @throws IOException if an error occurs while reading the file
     */
    public CopiesList(File copiesListFile) throws IOException {
        source = copiesListFile;
        map = new SortedProperties();
        InputStream in = null;
        try {
            if (copiesListFile.exists()) {
                in = new FileInputStream(copiesListFile);
                map.load(in);
            }
        } catch (FileNotFoundException e) {
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Return the number of copies for a file with the same file name as the
     * specified file.
     *
     * @param file the file to determine the copy count of
     * @return the number of copies of the file to include
     */
    public int getCopyCount(File file) {
        return getCopyCount(file.getName());
    }

    /**
     * Returns the number of copies for a file with the specified name. If the
     * name ends with the suffix ".eon", and the copies list does not contain a
     * matching entry but does contain an entry for the same file name without a
     * suffix, then the ".eon" suffix will be ignored.
     *
     * @param name the file name to fetch a copy count for
     * @return the number of copies of the file to include
     */
    public int getCopyCount(String name) {
        String v = map.getProperty(name);
        if (v == null && name.endsWith(".eon")) {
            v = map.getProperty(name.substring(0, name.length() - 4));
        }
        if (v != null) {
            try {
                int copies = Integer.valueOf(v);
                if (copies < 1) {
                    return 0;
                }
                return copies;
            } catch (NumberFormatException e) {
                StrangeEons.log.log(Level.WARNING, "invalid copies entry " + name + (source == null ? "" : (" in " + source)));
            }
        }
        return 1;
    }

    /**
     * Sets the number of copies to include for a file with the same name as the
     * specified file.
     *
     * @param file the file to modify the copy count for
     * @param copies the new copy count
     * @throws NullPointerException if the file is <code>null</code>
     * @throws IllegalArgumentException if the number of copies is negative
     */
    public void setCopyCount(File file, int copies) {
        setCopyCount(file.getName(), copies);
    }

    /**
     * Sets the number of copies to include for a file with the specified name.
     *
     * @param name the file name to modify the copy count for
     * @param copies the new copy count
     * @throws NullPointerException if the name is <code>null</code>
     * @throws IllegalArgumentException if the number of copies is negative
     */
    public void setCopyCount(String name, int copies) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("missing or null name");
        }
        if (copies < 0) {
            throw new IllegalArgumentException("negative copy count: " + name);
        }

        if (name.endsWith(".eon")) {
            name = name.substring(0, name.length() - 4);
        }

        map.setProperty(name, String.valueOf(copies));
    }

    /**
     * Returns the name part of each entry explicitly included in the copies
     * list.
     *
     * @return the file names included in the copies list
     */
    public String[] getListEntries() {
        String[] keys = new String[map.size()];
        Enumeration<?> e = map.propertyNames();
        for (int i = 0; e.hasMoreElements() && i < keys.length; ++i) {
            keys[i] = (String) e.nextElement();
        }
        return keys;
    }

    /**
     * Write the copies data to the file used to construct this list.
     *
     * @throws IOException if an error occurs
     * @throws IllegalStateException if the list was not created from a file
     */
    public void store() throws IOException {
        if (source == null) {
            throw new IllegalStateException("list was not created with a source");
        }
        try (OutputStream out = new FileOutputStream(source)) {
            map.store(out, null);
        }
    }
}
