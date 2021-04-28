package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.editors.NavigationPoint;
import ca.cgjennings.script.mozilla.javascript.Function;
import ca.cgjennings.script.mozilla.javascript.NativeGlobal;
import ca.cgjennings.script.mozilla.javascript.NativeJavaClass;
import ca.cgjennings.script.mozilla.javascript.NativeJavaMethod;
import ca.cgjennings.script.mozilla.javascript.NativeJavaObject;
import ca.cgjennings.script.mozilla.javascript.NativeJavaTopPackage;
import ca.cgjennings.script.mozilla.javascript.NativeObject;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the scope tree relevant to a character position within a script.
 * Used by {@link ScriptCodeCompleter} to determine possible completions for a
 * variable or property lookup. The children of an ID can be added manually or
 * generated automatically, depending on the type. For example, consider the
 * code:
 * <pre>
 * var p = Packages.org;
 * </pre> The root node would contain an identifier, <code>p</code>, which would
 * be added "manually" by the scope parser when it observed the assignment. The
 * return value of <code>p</code> would be an API node for the <code>ca</code>
 * package. If the children of <code>p</code> are requested, they are generated
 * automatically from the API node. Thus, parts of the tree are created lazily,
 * only if the code being expanded actually refers to it. This can be repeated
 * recursively, so if trying to complete
 * <code>p.cgjennings.SomeClass.to|</code>, the <code>cgjennings</code> and
 * <code>SomeClass</code> nodes are built on demand and used to find code
 * completions for <code>to</code> (in this case, static methods and fields).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
class ScopeID {

    private ScopeID parent;
    private Set<ScopeID> kids;

    private String name;
    private Class type;
    private Object returnType;
    //  -1 : the root, a variable, a function, or an instance of *type*;
    //       if type is an imported class or package, returnType is an API node
    // 0-n : method with n args
    //       if type is a method, returnType is an API node
    //       if type is a function, returnType is null
    private int args;

    /**
     * Type of the root ID. Return type is <code>null</code>.
     */
    public static final Class ROOT = NativeGlobal.class;
    /**
     * Type of an imported Java package; return type is API node of package.
     */
    public static final Class IMPORTED_PACKAGE = NativeJavaTopPackage.class;
    /**
     * Type of an imported Java class; return type is API node of class.
     */
    public static final Class IMPORTED_CLASS = NativeJavaClass.class;
    /**
     * Type of an instance of a Java object; return type is API node of class.
     */
    public static final Class INSTANCE = NativeJavaObject.class;
    /**
     * Type of a Java method call; return type is API node of method's return
     * type.
     */
    public static final Class METHOD = NativeJavaMethod.class;
    /**
     * Type of a script function call; return type is <code>null</code>, or
     * <code>ScopeID</code> of possible return values.
     */
    public static final Class FUNCTION = Function.class;
    /**
     * Type of an arbitrary script object (with no more specific type
     * available).
     */
    public static final Class JS_OBJECT = NativeObject.class;

    /**
     * Creates a new, empty, parentless scope root; this represents a global
     * namespace with no parent.
     *
     * @param scc a script completion context to associate with the tree
     * @see #getContext()
     */
    public ScopeID(ScriptCompletionContext scc) {
        this("", ROOT, -1, scc);

        add("Packages", APIDatabase.getPackageRoot());
    }

    /**
     * Creates a new ID with the specified parameters; used by subclasses to
     * define new node types.
     *
     * @param name the name of the ID
     * @param type the type identifier of the ID
     * @param argCount the argument count if the ID represents a method or
     * function
     * @param returnType the return type or instance type
     */
    protected ScopeID(String name, Class type, int argCount, Object returnType) {
        this.name = name;
        this.type = type;
        this.args = argCount;
        this.returnType = returnType;
    }

    /**
     * Creates an child empty ID capable with the specified name. The new child
     * is returned so that it can be expanded with children of its own.
     *
     * @param name the name of the ID
     * @return the new child ID
     */
    public ScopeID add(String name) {
        ScopeID kid = new JSObject(name);
        add(kid);
        return kid;
    }

    public void addArgument(String name) {
        add(new Arg(name));
    }

    /**
     * Adds a new child node to this node that represents a class or package and
     * that is assigned to the specified identifier.
     *
     * @param packageOrClass an API database node representing
     * @return
     */
    public ScopeID add(String name, APINode packageOrClass) {
        ScopeID node;
        if (packageOrClass instanceof ClassNode) {
            node = new PackageID(name, packageOrClass);
        } else {
            node = new ClassID(name, packageOrClass);
        }
        return node;
    }

//	public static ScopeID createPackage( String name, APINode pkg ) {
//		if( !(pkg instanceof PackageRoot || pkg instanceof PackageNode) ) {
//			throw new IllegalArgumentException( "pkg" );
//		}
//		if( name == null ) name = pkg.getName();
//		return new ScopeID( name, IMPORTED_PACKAGE, -1, pkg );
//	}
//
//	public static ScopeID createClass( String name, APINode klass ) {
//		if( klass == null ) throw new NullPointerException("klass");
//		if( name == null ) name = klass.getName();
//		return new ScopeID( name, IMPORTED_CLASS, -1, klass );
//	}
    /**
     * Returns this node's children as a set. If no children have been added by
     * calling the various <code>add</code> methods, then an attempt is first
     * made to build the node's children automatically. (This only works for
     * certain node types with a structure that is fixed at compile time, such
     * as packages and classes.)
     *
     * @return the children of this node
     */
    public Set<ScopeID> children() {
        if (kids == null) {
            kids = expand();
        }
        if (immutableSet == null) {
            immutableSet = Collections.unmodifiableSet(kids);
        }
        return immutableSet;
    }
    private Set<ScopeID> immutableSet;

    /**
     * Returns the specified node from this nodes immediate children, if
     * present.
     *
     * @param id the node to remove
     */
    public void remove(ScopeID id) {
        if (kids == null) {
            kids = expand();
        }
        kids.remove(id);
    }

    /**
     * Returns <code>true</code> if the node has at least one direct child with
     * the specified name.
     *
     * @param name the name to check
     * @return <code>true</code> if a child node has this name
     */
    public boolean containsName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        for (ScopeID id : children()) {
            if (name.equals(id.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called to give the subclass an opportunity to generate its child nodes. A
     * given node can only either have nodes added manually or generated.
     *
     * @return a set of the node's children
     */
    protected Set<ScopeID> expand() {
        HashSet<ScopeID> kids = new HashSet<>();
//		if( type == IMPORTED_PACKAGE ) {
//			for( APINode n : ((APINode) returnType).children() ) {
//				if( n instanceof PackageNode ) {
//					kids.add( createPackage( null, n ) );
//				} else if( n instanceof ClassNode ) {
//					kids.add( createClass( null, n ) );
//				} else {
//					StrangeEons.log.warning( n.toString() );
//				}
//			}
//		} else if( type == IMPORTED_CLASS ) {
//			ScriptCompletionContext scc = getContext();
//			ClassNode cn = (ClassNode) returnType;
//			
//			if( scc.insideNewOperator() ) {
//				for( Constructor c : cn.getConstructors() ) {
//					
//				}
//			} else {
//				
//			}
//			
//			
//			
////			for( APINode n : cn.)
////			for( APINode n : ((APINode) returnType).children() ) {
////				if( n instanceof PackageNode ) {
////					kids.add( createPackage( null, n ) );
////				} else if( n instanceof ClassNode ) {
////					kids.add( createClass( null, n ) );
////				} else {
////					StrangeEons.log.warning( n.toString() );
////				}
////			}	
//		} else {
//			StrangeEons.log.warning( toString() );
//		}
        return kids;
    }

    protected void add(ScopeID id) {
        if (kids == null) {
            kids = new HashSet<>();
        }
        kids.add(id);
    }

    /**
     * Returns the name of this identifier node.
     *
     * @return the identifier for the node
     */
    public String getName() {
        return name;
    }

    /**
     * Changes the name associated with this ID.
     *
     * @param name the new identifier name
     */
    public void setName(String name) {
        if (parent != null) {
            parent.kids.remove(this);
        }
        this.name = name;
        if (parent != null) {
            parent.kids.add(this);
        }
    }

    public Object getReturnType() {
        return parent == null ? null : returnType;
    }

    public int getParameterCount() {
        return args;
    }

    /**
     * Returns the root associated with the ID tree.
     *
     * @return the ID root
     */
    public ScopeID getRoot() {
        ScopeID r = this;
        while (r.parent != null) {
            r = r.parent;
        }
        return r;
    }

    /**
     * Returns the script context that the root of this tree was created for.
     *
     * @return the script context that controls the type of child nodes that are
     * generated
     */
    public ScriptCompletionContext getContext() {
        return (ScriptCompletionContext) getRoot().returnType;
    }

    /**
     * Returns a code alternative for this node that can be used to represent it
     * during code completion.
     *
     * @return a code completion alternative
     */
    public CodeAlternative toCompletion(JSourceCodeEditor target, int prefixLength) {
        throw new UnsupportedOperationException("no completion for root node");
    }

    /**
     * Returns the types that have been explicitly imported into the root space.
     * Used to guess what might be appropriate if we have no idea what type
     * something is.
     *
     * @return
     */
    public Set<ScopeID> getImportedTypes() {
        HashSet<ScopeID> ids = new HashSet<>();
        if (true) {
            throw new UnsupportedOperationException();
        }
        return ids;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.name);
        hash = 67 * hash + Objects.hashCode(this.type);
        hash = 67 * hash + this.args;
        hash = 67 * hash + Objects.hashCode(this.returnType);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ScopeID other = (ScopeID) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (this.args != other.args) {
            return false;
        }
        if (!Objects.equals(this.returnType, other.returnType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(40);
        b.append("ID{").append(name);
        if (args >= 0) {
            b.append('(').append(args).append(')');
        }
        b.append(':').append(returnType);
        b.append('}');
        return b.toString();
    }

    /**
     * Represents a static class identifier (not an instance of a class).
     */
    static class ClassID extends ScopeID {

        public ClassID(String name, APINode type) {
            super(name, IMPORTED_PACKAGE, -1, type);
        }

        @Override
        protected Set<ScopeID> expand() {
            HashSet<ScopeID> kids = new HashSet<>();

            ScriptCompletionContext scc = getContext();
            ClassNode cn = (ClassNode) getReturnType();

            if (scc.insideNewOperator()) {
                for (Constructor c : cn.getConstructors()) {

                }
            } else {

            }

            //			for( APINode n : cn.)
            //			for( APINode n : ((APINode) returnType).children() ) {
            //				if( n instanceof PackageNode ) {
            //					kids.add( createPackage( null, n ) );
            //				} else if( n instanceof ClassNode ) {
            //					kids.add( createClass( null, n ) );
            //				} else {
            //					StrangeEons.log.warning( n.toString() );
            //				}
            //			}				
            return kids;
        }
    }

    static class PackageID extends ScopeID {

        public PackageID(String name, APINode type) {
            super(name, IMPORTED_PACKAGE, -1, type);
        }

        @Override
        protected Set<ScopeID> expand() {
            HashSet<ScopeID> kids = new HashSet<>();
            for (APINode n : ((APINode) getReturnType()).children()) {
                if (n instanceof PackageNode) {
                    kids.add(new PackageID(n.getName(), n));
                } else if (n instanceof ClassNode) {
                    kids.add(new ClassID(n.getName(), n));
                } else {
                    StrangeEons.log.warning(n.toString());
                }
            }
            return kids;
        }

        @Override
        public CodeAlternative toCompletion(JSourceCodeEditor target, int prefixLength) {
            return new DefaultCodeAlternative(
                    target, getName() + '.', getName(), "package",
                    NavigationPoint.ICON_PACKAGE, 0, true, prefixLength
            );
        }
    }

    static class InstanceID extends ScopeID {

        public InstanceID(String name, ClassNode of) {
            super(name, INSTANCE, -1, of);
        }

        @Override
        protected Set<ScopeID> expand() {
            HashSet<ScopeID> kids = new HashSet<>();
            ClassNode cn = (ClassNode) getReturnType();
            for (Field f : cn.getFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {

                }
            }
            return kids;
        }

        @Override
        public CodeAlternative toCompletion(JSourceCodeEditor target, int prefixLength) {
            return new DefaultCodeAlternative(
                    target, getName(), getName(), ((ClassNode) getReturnType()).getName(),
                    NavigationPoint.ICON_CLUSTER, 0, true, prefixLength
            );
        }
    }

    static class ConstructorID extends ScopeID {

        private final Constructor c;

        public ConstructorID(String name, ClassNode of, Constructor c) {
            super(name, METHOD, c.getParameterTypes().length, of);
            this.c = c;
        }

        @Override
        public CodeAlternative toCompletion(JSourceCodeEditor target, int prefixLength) {
            return CompletionUtilities.completionForMethod(target, (ClassNode) getReturnType(), c, prefixLength);
        }
    }

    static class JSObject extends ScopeID {

        public JSObject(String name) {
            super(name, JS_OBJECT, -1, null);
        }

        @Override
        protected Set<ScopeID> expand() {
            return new HashSet<>();
        }

        @Override
        public CodeAlternative toCompletion(JSourceCodeEditor target, int prefixLength) {
            String name = getName();
            return new DefaultCodeAlternative(
                    target, name, name, "object",
                    NavigationPoint.ICON_SQUARE, 0, false, prefixLength
            );
        }
    }

    static class Arg extends JSObject {

        public Arg(String name) {
            super(name);
        }

        @Override
        public CodeAlternative toCompletion(JSourceCodeEditor target, int prefixLength) {
            String name = getName();
            return new DefaultCodeAlternative(
                    target, name, name, "argument",
                    NavigationPoint.ICON_SQUARE_ALTERNATIVE, 0, false, prefixLength
            );
        }
    }
}
