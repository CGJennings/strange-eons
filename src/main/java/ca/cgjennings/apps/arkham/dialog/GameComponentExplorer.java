package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.component.PortraitProvider;
import ca.cgjennings.ui.EditorPane;
import ca.cgjennings.ui.JUtilities;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.WeakHashMap;
import java.util.logging.Level;
import javax.swing.event.HyperlinkEvent;
import static resources.Language.string;
import resources.Settings;

/**
 * A dialog that explores the state of a game component using a browsing-style
 * interface. This was a plug-in under 2.x, but it is now simply a built-in
 * development aid.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class GameComponentExplorer extends javax.swing.JDialog {

    /**
     * Creates new form GameComponentExplorer
     */
    public GameComponentExplorer(Window parent, Object obj) {
        super(parent == null ? StrangeEons.getWindow() : parent, ModalityType.MODELESS);
        eons = StrangeEons.getWindow();
        JUtilities.makeUtilityWindow(this);
        initComponents();

        textPane.addHyperlinkListener((HyperlinkEvent evt) -> {
            if (evt.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                return;
            }
            onHyperlinkActivation(evt.getDescription());
        });

        if (obj instanceof AbstractGameComponentEditor) {
            obj = GameObjectImpl.create((AbstractGameComponentEditor) obj);
        }
        textPane.setText(createDescription(obj));
        textPane.select(0, 0);
        setSize(640, 400);
        setLocationRelativeTo(parent);
    }

    private void onHyperlinkActivation(String description) {
        try {
            Integer id = Integer.valueOf(description);
            Object gc = linkMap.get(id);
            if (gc != null) {
                if (gc instanceof BufferedImage) {
                    ImageViewer iv = new ImageViewer(this, (BufferedImage) gc, false);
                    iv.setLocationByPlatform(true);
                    iv.setVisible(true);
                } else {
                    GameComponentExplorer ex = new GameComponentExplorer(this, gc);
                }
            }
        } catch (NumberFormatException e) {
            StrangeEons.log.log(Level.SEVERE, "error decoding link", e);
        }
    }

    private static int linkID = 0;
    private static WeakHashMap<Integer, Object> linkMap;
    private StrangeEonsAppWindow eons;

    private static String createDescription(Object o) {
        StringBuilder b = new StringBuilder();
        b.append("<html><body style='font-family:Monospace;'>");
        if (o != null) {
            b.append("<table width='100%' border='0'>");
            if (o instanceof GameObject) {
                GameObject gc = (GameObject) o;
                appendConstants(b, gc);
                appendMethods(b, gc);
                appendState(b, gc);
                if (gc.unwrap() instanceof PortraitProvider) {
                    appendPortraits(b, (PortraitProvider) gc.unwrap());
                }
            } else if (o instanceof Settings) {
                appendSettings(b, (Settings) o);
            }
            b.append("</table>");
        } else {
            b.append(string("cv-na"));
        }
        b.append("</body></html>");
        return b.toString();
    }

    private static void addSection(StringBuilder b, String title) {
        b.append("<tr><td colspan='2'>&nbsp;<br><b><u>").append(title)
                .append("</u></b></td>").append("</tr>");
    }

    private static void addLine(StringBuilder b, String left, String right) {
        b.append("<tr><td style='color: blue;' valign='top'>")
                .append(escape(left)).append("</td><td valign='top'>")
                .append(escape(right)).append("</tr>");
    }

    private static String escape(String s) {
        if (s == null) {
            s = "<null>";
        }
        if (s.startsWith("<a href")) {
            return s;
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                .replace("  ", "&nbsp;&nbsp;");
    }

    private static void appendSettings(StringBuilder b, Settings s) {
        addSection(b, string("cv-settings"));
        String[] keys = s.getKeySet().toArray(new String[s.size()]);
        Arrays.sort(keys);
        for (String key : keys) {
            addLine(b, key, s.get(key));
        }
    }

    private static void appendConstants(StringBuilder b, GameObject gc) {
        String[] names = gc.getConstantNames();
        if (names.length > 0) {
            addSection(b, string("cv-constants"));
            b.append("<tr><td colspan=2>");
            for (int i = 0; i < names.length; ++i) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append("<span style='color: blue'>")
                        .append(names[i])
                        .append("</span> = ")
                        .append(cleanValue(gc.getConstant(names[i])));
            }
            b.append("</td></tr>");
        }
    }

    private static void appendMethods(StringBuilder b, GameObject gc) {
        addSection(b, string("cv-methods"));
        for (String method : gc.getMethodNames()) {
            StringBuilder signature = new StringBuilder();
            signature.append(cleanTypeName(gc.getActualReturnType(method))).append(" ");
            signature.append(method).append('(');
            Class[] types = gc.getActualArgumentTypes(method);
            for (int i = 0; i < types.length; ++i) {
                if (i > 0) {
                    signature.append(", ");
                }
                signature.append(cleanTypeName(types[i]));
            }
            signature.append(")");
            addLine(b, method, signature.toString());
        }
    }

    private static String cleanTypeName(Class type) {
        String name = type.getCanonicalName();
        if (name.startsWith("java.lang.")) {
            name = name.substring("java.lang.".length());
        } else if (name.equals("ca.cgjennings.apps.arkham.plugins.GameComponent")) {
            name = "GameComponent";
        } else if (name.startsWith("ca.cgjennings.apps.arkham.")) {
            name = name.substring("ca.cgjennings.apps.".length());
        }
        return name;
    }

    private static void appendPortraits(StringBuilder b, PortraitProvider pp) {
        addSection(b, string("cv-portraits"));
        int count = pp.getPortraitCount();
        for (int i = 0; i < count; ++i) {
            addLine(b, "Portrait", "" + i);
            Portrait p = pp.getPortrait(i);
            EnumSet<Portrait.Feature> features = p.getFeatures();
            addLine(b, "Features", cleanValue(features));
            if (features.contains(Portrait.Feature.SOURCE)) {
                addLine(b, "Source", p.getSource());
            }
            addLine(b, "Image", cleanValue(p.getImage()));
            if (features.contains(Portrait.Feature.SCALE)) {
                addLine(b, "Scale", cleanValue(p.getScale()));
            }
            if (features.contains(Portrait.Feature.PAN)) {
                addLine(b, "Pan X", cleanValue(p.getPanX()));
                addLine(b, "Pan Y", cleanValue(p.getPanY()));
            }
            if (features.contains(Portrait.Feature.ROTATE)) {
                addLine(b, "Rotation", cleanValue(p.getRotation()));
            }
        }
    }

    private static void appendState(StringBuilder b, GameObject gc) {
        addSection(b, string("cv-state"));
        for (String method : gc.getMethodNames()) {
            if ((method.startsWith("get") || method.startsWith("is")) && gc.getArgumentTypes(method).length == 0) {
                String name = method.substring(method.startsWith("get") ? "get".length() : "is".length());
                String value;
                try {
                    value = cleanValue(gc.call(method));
                } catch (InvocationTargetException ite) {
                    value = "(threw " + ite.getCause() + ")";
                }
                addLine(b, name, value);
            }
        }
    }

    private static String cleanValue(Object value) {
        if (value == null) {
            return "<null>";
        }
        if (value.getClass().isArray()) {
            StringBuilder b = new StringBuilder().append('{');
            for (int i = 0; i < Array.getLength(value); ++i) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(cleanValue(Array.get(value, i)));
            }
            b.append('}');
            return b.toString();
        } else if (value instanceof GameObjectCollection) {
            GameObjectCollection gcc = (GameObjectCollection) value;
            StringBuilder b = new StringBuilder();

            for (int i = 0; i < gcc.length(); ++i) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(cleanValue(gcc.get(i)));
                b.append(' ').append(i);
            }
            return b.toString();
        } else if (value instanceof Settings) {
            Settings s = (Settings) value;
            if (s.size() == 0) {
                return string("cv-settings-link-empty");
            } else {
                return string(
                        "cv-settings-link", createLink(s), s.size()
                );
            }
        } else if (value instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) value;
            return String.format(
                    "<a href='%d'>BufferedImage (%,d \u00d7 %,d)</a>",
                    createLink(bi), bi.getWidth(), bi.getHeight()
            );
        } else if (value instanceof GameObject) {
            String link = String.format("<a href='%d'>" + unwrap(value).getClass().getCanonicalName() + "</a>", createLink(value));
            if (unwrap(value) instanceof Enum) {
                return ((Enum) unwrap(value)).name() + " (" + link + ")";
            }
            return link;
        }
        return value.toString();
    }

    private static int createLink(Object object) {
        if (linkMap == null) {
            linkMap = new WeakHashMap<>();
        }
        linkMap.put(linkID, object);
        return linkID++;
    }

    private static String getComponentName(Object o) {
        if (o != null) {
            String name;
            if (o instanceof GameObject) {
                GameObject gc = (GameObject) o;
                name = gc.unwrap().getClass().getSimpleName() + " (";
                try {
                    if (gc.hasMethod("getFullName")) {
                        name += cleanValue(gc.call("getFullName"));
                    } else if (gc.hasMethod("getName")) {
                        name += cleanValue(gc.call("getName"));
                    }
                } catch (InvocationTargetException e) {
                }
                return name + ")";
            } else {
                return o.toString();
            }
        }
        return "<null>";
    }

    private static Object unwrap(Object o) {
        return o instanceof GameObject ? ((GameObject) o).unwrap() : o;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        textPane = new EditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        textPane.setEditable(false);
        textPane.setContentType("text/html"); // NOI18N
        textPane.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        scrollPane.setViewportView(textPane);

        getContentPane().add(scrollPane, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JEditorPane textPane;
    // End of variables declaration//GEN-END:variables

    /**
     * An interface that allows interaction with the game components being
     * edited by the user. This is an abstraction that allows plug-ins to safely
     * manipulate game components even if they are not linked to the SE code
     * base at compile time.
     * <p>
     * If your code is linked to SE at compile time, it is generally easier to
     * use the actual objects directly.
     *
     * @author Chris Jennings <https://cgjennings.ca/contact>
     */
    public interface GameObject {

        /**
         * Return an array of named constants defined by the component
         * represented by this object.
         *
         * @return an array of the names of constants defined by this object
         */
        public String[] getConstantNames();

        /**
         * Returns the actual type of the value of the constant called
         * {@code name}. If the value of the constant is {@code null}, this
         * method returns {@code void.class}.
         *
         * @param name the name of the constant to determine the type of
         * @return the actual type of the constant, or {@code void.class}
         */
        public Class getConstantType(String name);

        /**
         * Returns the value of the constant called {@code name}.
         *
         * @param name the name of the constant to return the value of
         * @return the value of the named constant
         */
        public Object getConstant(String name);

        /**
         * Returns a list of supported method names that can be called through
         * this interface. The available methods will depend on which component
         * this interface represents, and may change depending on the version of
         * Strange Eons.
         *
         * @return an array of the supported method names
         */
        public abstract String[] getMethodNames();

        /**
         * Returns {@code true} if this component offers the named method.
         *
         * @param methodName the name of the method to search for
         * @return {@code true} if this component has a method named
         * {@code methodName}
         */
        public abstract boolean hasMethod(String methodName);

        /**
         * Returns the types of the arguments to one of the supported methods.
         * An array of length 0 is returned if the method takes no arguments.
         *
         * @param methodName the method to fetch an argument list for
         * @return an array of the types of the parameters to this method, in
         * call order
         * @throws IllegalArgumentException if {@code methodName} is not a
         * method of this component
         */
        public abstract Class[] getArgumentTypes(String methodName);

        /**
         * Returns the actual types of the arguments to one of the supported
         * methods. An array of length 0 is returned if the method takes no
         * arguments.
         *
         * @param methodName the method to fetch an argument list for
         * @return an array of the types of the parameters to this method, in
         * call order
         * @throws IllegalArgumentException if {@code methodName} is not a
         * method of this component
         */
        public abstract Class[] getActualArgumentTypes(String methodName);

        /**
         * Returns the return type of a given method. If the method does not
         * return a value, returns {@code void.class}.
         *
         * @param methodName the method to fetch a return type for
         * @return the class of the return type of the method
         * @throws IllegalArgumentException if {@code methodName} is not a
         * method of this component
         */
        public abstract Class getReturnType(String methodName);

        /**
         * Returns the actual return type of the method, even if it would be
         * wrapped as a {@code GameObject} if called through this interface.
         *
         * @param methodName the method to fetch a return type for
         * @return the class of the return type of the method
         * @throws IllegalArgumentException if {@code methodName} is not a
         * method of this component
         */
        public abstract Class getActualReturnType(String methodName);

        /**
         * Call one of the methods of this game component. The game component
         * implementation will ensure that the call is made in the correct
         * thread and that the editor (if any) is updated to reflect the changes
         * to the component. The arguments to the method call must match the
         * types returned by {@link #getArgumentTypes}.
         *
         * @param methodName the name of the method to call
         * @param arguments the arguments to the method
         * @return the value returned by the method, or {@code null} if it has
         * {@code void} return type
         * @throws java.lang.reflect.InvocationTargetException if the underlying
         * method throws an exception during the method call
         * @throws IllegalArgumentException if {@code methodName} is not a valid
         * method or if the arguments are not appropriate for the method
         */
        public abstract Object call(String methodName, Object... arguments) throws InvocationTargetException;

        /**
         * Returns the actual object wrapped by this adapter.
         *
         * @return the object that this adapter interfaces to
         */
        public abstract Object unwrap();
    }

    /**
     * A {@link GameObject} that represents an immutable array-like collection
     * of other {@code GameObjects}.
     *
     * @author Chris Jennings <https://cgjennings.ca/contact>
     */
    public interface GameObjectCollection extends GameObject {

        /**
         * Returns a selected {@link GameObject}s in this collection.
         *
         * @param index the index of the component to return
         * @return the component at that index
         * @throws IndexOutOfBoundsException if the index is invalid
         */
        public abstract GameObject get(int index);

        /**
         * Set the {@link GameObject} at an index in this collection.
         *
         * @param index the index to set the component of
         * @param value the new value to assign
         * @throws IndexOutOfBoundsException if the index is invalid
         */
        public abstract void set(int index, GameObject value);

        /**
         * Returns the number of {@link GameObject}s available in this
         * collection.
         *
         * @return the number of items in this collection
         */
        public abstract int length();
    }

    /**
     * Creates a {@link GameObject} view of a game component in an editor.
     *
     * @param editor the editor to create a game object view for
     * @return the game object view of the edited component
     */
    public static GameObject createGameObject(AbstractGameComponentEditor editor) {
        return GameObjectImpl.create(editor);
    }
}
