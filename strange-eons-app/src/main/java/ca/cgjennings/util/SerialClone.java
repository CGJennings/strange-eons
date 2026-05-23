package ca.cgjennings.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import ca.cgjennings.io.SEObjectInputStream;
import ca.cgjennings.io.SEObjectOutputStream;

/**
 * Clones an object by round-tripping through serialization.
 */
public final class SerialClone {
    private SerialClone() {
    }

    /** Clones an object by round-tripping through serialization. */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T clone(T obj) {
        ByteArrayOutputStream buff = new ByteArrayOutputStream(100 * 1024);
        try {
            try (ObjectOutputStream out = new SEObjectOutputStream(buff)) {
                out.writeObject(obj);
            }
            ObjectInputStream in = new SEObjectInputStream(
                    new ByteArrayInputStream(buff.toByteArray())
            );            
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
