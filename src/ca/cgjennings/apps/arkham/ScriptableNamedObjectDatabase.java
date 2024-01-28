package ca.cgjennings.apps.arkham;

import java.util.HashMap;
import org.mozilla.javascript.Scriptable;

/**
 * Represents a database of named objects, basically a thread-safe mapping from
 * strings (names) to objects. This class provides both a Java-friendly and a
 * JavaScript-friendly interface to the objects in its database. To manipulate
 * named objects from Java, use {@link #getObject},
 * {@link #putObject}, and {@link #removeObject}. (The rest of the methods
 * implement scripting support.)
 * <p>
 * To manipulate named objects from JavaScript, they can be accessed with array
 * notation or as property names:<br>
 * <pre>
 * StrangeEons.namedObject['myObj'] = function() { return 42; };
 * StrangeEons.namedObject['myObj']();
 * </pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
final class ScriptableNamedObjectDatabase implements Scriptable, NamedObjectDatabase {

    private final HashMap<String, Object> db = new HashMap<>();

    /**
     * Creates a new, empty named object database.
     */
    public ScriptableNamedObjectDatabase() {
    }

    /**
     * Returns the object associated with the requested name, or {@code null} if
     * there is no object associated with the name.
     *
     * @param name the object name to return
     * @return the value of the object in the database, or {@code null}
     * @throws NullPointerException if name is {@code null}
     */
    @Override
    public Object getObject(String name) {
        if (name == null) {
            throw new NullPointerException("object name");
        }
        synchronized (db) {
            return db.get(name);
        }
    }

    /**
     * Associates an object with the specified name. If an object with this name
     * already exists, it must first be explicitly removed. (This prevents
     * accidental name conflicts from going undiagnosed.)
     *
     * @param name the name of the object to return
     * @param object the object to associate with the name in the database
     * @throws NullPointerException if the name or object is {@code null}
     * @throws IllegalArgumentException if the name is already associated with
     * an object
     */
    @Override
    public void putObject(String name, Object object) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (object == null) {
            throw new NullPointerException("object");
        }
        synchronized (db) {
            if (db.containsKey(name)) {
                StrangeEons.log.warning("named object already defined: " + name);
            }
            db.put(name, object);
        }
    }

    /**
     * Removes the object with the requested name, if any.
     *
     * @param name the name of the object to remove
     * @throws NullPointerException if name is {@code null}
     */
    @Override
    public void removeObject(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        synchronized (db) {
            db.remove(name);
        }
    }

    /**
     * Returns the JS class name for the object.
     *
     * @return "ScriptableNamedObjectDatabase"
     */
    @Override
    public String getClassName() {
        return "NamedObjectDatabase";
    }

    /**
     * Deletes a named JS property: forwards to {@link #removeObject}
     *
     * @param name the property to delete
     */
    @Override
    public void delete(String name) {
        removeObject(name);
    }

    /**
     * Deletes an indexed JS property: forwards to {@link #removeObject}
     *
     * @param index the property to delete
     */
    @Override
    public void delete(int index) {
        delete(String.valueOf(index));
    }

    /**
     * Returns the named JS property: forwards to {@link #getObject}.
     *
     * @param name the property to look up
     * @param start not used
     * @return the value of the property
     */
    @Override
    public Object get(String name, Scriptable start) {
        return getObject(name);
    }

    /**
     * Returns the indexed property: forwards to {@link #getObject}.
     *
     * @param index the property to look up
     * @param start not used
     * @return the value of the property
     */
    @Override
    public Object get(int index, Scriptable start) {
        return get(String.valueOf(index), start);
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return "[object NamedObjectDatabase]";
    }

    @Override
    public Object[] getIds() {
        synchronized (db) {
            return db.keySet().toArray();
        }
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        putObject(name, value);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        putObject(String.valueOf(index), value);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        synchronized (db) {
            return db.containsKey(name);
        }
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return has(String.valueOf(index), start);
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        Scriptable proto = instance.getPrototype();
        while (proto != null) {
            if (proto.equals(this)) {
                return true;
            }
            proto = proto.getPrototype();
        }

        return false;
    }

    @Override
    public Scriptable getParentScope() {
        return parent;
    }

    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    @Override
    public void setParentScope(Scriptable parent) {
        this.parent = parent;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    private Scriptable prototype, parent;
}
