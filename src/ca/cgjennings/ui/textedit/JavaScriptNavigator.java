package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.engine.SyntaxChecker;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Icon;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.DestructuringForm;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.RegExpLiteral;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

/**
 * A {@link Navigator} for JavaScript source files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class JavaScriptNavigator extends SyntaxChecker implements Navigator {

    /**
     * Create a new script navigator.
     */
    public JavaScriptNavigator() {
    }

    @Override
    public List<NavigationPoint> getNavigationPoints(String text) {
        List<NavigationPoint> points = new LinkedList<>();

        currentPointList = points;
        parse(text);
        SyntaxError[] errors = getErrors();
        currentPointList = null;

        if (errors != null) {
            for (int i = 0; i < errors.length; ++i) {
                int offset = errors[i].offset();
                if (offset < 0) {
                    offset = 0;
                }
                Icon icon = errors[i].isWarning() ? NavigationPoint.ICON_WARNING
                        : NavigationPoint.ICON_ERROR;
                points.add(new NavigationPoint(
                        "<html><font color=red>" + errors[i].message(), null,
                        offset, 0, icon
                ));
            }
            // if there were errors, then the list is no longer guaranteed to
            // be in sorted order since they are simply tacked onto the end
            NavigationPoint.sortByOffset(points);
        }

        return points;
    }

    @Override
    protected void processTree(AstRoot node) {
        depth = 0;
        for (Node n : node) {
            visitRoot(n);
        }
    }

    private List<NavigationPoint> currentPointList;
    private int depth;

    private void visitRoot(Node node) {
        if (node instanceof FunctionNode) {
            visitFunction((FunctionNode) node, null, Token.FUNCTION);
        } else if (node instanceof VariableDeclaration) {
            visitVar((VariableDeclaration) node);
        } else if (node instanceof ObjectProperty) {
            visitObjProp((ObjectProperty) node);
        }
    }

    /**
     * Visits a property declaration in an object literal.
     *
     * @param prop
     */
    private void visitObjProp(ObjectProperty prop) {
        AstNode lhs = prop.getLeft();
        if (lhs instanceof Name) {
            String name = ((Name) lhs).getIdentifier();
            AstNode rhs = prop.getRight();
            visitRHS(name, prop, rhs, NavigationPoint.ICON_PROPERTY);
        }
    }

    /**
     * Visits a variable declaration. This will examine the child to see if
     * anything is assigned and may create more specific node types as a result.
     *
     * @param var the node to process
     */
    private void visitVar(VariableDeclaration var) {
        if (!var.isStatement()) {
            return;
        }

        Icon icon = NavigationPoint.ICON_VAR;
        if (var.isConst()) {
            icon = NavigationPoint.ICON_CONST;
        } else if (var.isLet()) {
            icon = NavigationPoint.ICON_LET;
        }

        for (Node varInit : var.getVariables()) {
            if (varInit instanceof VariableInitializer) {
                visitVarInit((VariableInitializer) varInit, icon);
            }
        }
    }

    /**
     * Visits a variable initializer; called via {@link #visitVar}. For simple
     * declarations, creates the entry itself. Otherwise, calls other visitors
     * based on the object type.
     *
     * @param vi the initializer node
     * @param icon default icon determined from the parent
     */
    private void visitVarInit(VariableInitializer vi, Icon icon) {
        String name;
        AstNode lhs = vi.getTarget();
        if (lhs instanceof DestructuringForm) {
            StringBuilder b = new StringBuilder();
            b.append('[');
            if (lhs instanceof ArrayLiteral) {
                for (Node n : ((ArrayLiteral) lhs).getElements()) {
                    if (n instanceof Name) {
                        if (b.length() > 1) {
                            b.append(", ");
                        }
                        b.append(((Name) n).getIdentifier());
                    }
                }
            }
            b.append(']');
            name = b.toString();
        } else {
            name = ((Name) lhs).getIdentifier();
        }

        AstNode rhs = vi.getInitializer();
        visitRHS(name, vi, rhs, icon);
    }

    private String composeNameAndType(String name, String value) {
        if (value != null) {
            if (value.length() > 25) {
                value = value.substring(0, 25) + "...";
            }
            name = "<html>" + name + " : <font color=gray>" + value + "</font>";
        }
        return name;
    }

    /**
     * Handles the right hand side of assignments, such as variable declarations
     * and object properties.
     *
     * @param name
     * @param parent
     * @param rhs
     * @param icon
     */
    private void visitRHS(String name, AstNode parent, AstNode rhs, Icon icon) {
        String value = null;

        if (rhs instanceof FunctionNode) {
            visitFunction((FunctionNode) rhs, name, parent.getType());
        } else if (rhs instanceof ObjectLiteral) {
            name = composeNameAndType(name, "object");
            currentPointList.add(new NavigationPoint(
                    name, name, parent.getAbsolutePosition(), depth, NavigationPoint.ICON_CLASS
            ));
            ++depth;
            for (Node n : ((ObjectLiteral) rhs).getElements()) {
                visitRoot(n);
            }
            --depth;
        } else {
            if (rhs instanceof NumberLiteral) {
                value = "number " + ((NumberLiteral) rhs).getValue();
            } else if (rhs instanceof StringLiteral) {
                value = "string " + ((StringLiteral) rhs).getValue();
            } else if (rhs instanceof RegExpLiteral) {
                value = "regexp " + ((RegExpLiteral) rhs).getValue();
            } else if (rhs instanceof ArrayLiteral) {
                value = "array[" + ((ArrayLiteral) rhs).getSize() + "]";
            } else if (rhs != null) {
                value = "expression";
            }

            name = composeNameAndType(name, value);

            currentPointList.add(new NavigationPoint(
                    name, name, parent.getAbsolutePosition(), depth, icon
            ));
        }
    }

    /**
     * Visits a function node. If called while examining an assignment, the id
     * will be the non-null name of the variable or property being assigned to.
     * Otherwise, the id will be obtained from the function (if defined).
     *
     * @param fn the function node
     * @param id the name to give the function, if the function is immediately
     * assigned
     * @param token the token type being assigned to, to determine an
     * appropriate icon
     */
    private void visitFunction(FunctionNode fn, String id, int token) {
        String value = "function";
        Icon icon = NavigationPoint.ICON_FUNCTION;
        if (id == null) {
            id = fn.getName();
            if (id.isEmpty()) {
                id = "&lt;anonymous&gt;";
            }
        }

        if (fn.isExpressionClosure()) {
            icon = NavigationPoint.ICON_FUNCTION;
            value = "closure";
        } else if (fn.isGenerator()) {
            icon = NavigationPoint.ICON_FUNCTION;
            value = "generator";
        } else {
            if (token == Token.GET) {
                icon = NavigationPoint.ICON_GETTER;
            } else if (token == Token.SET) {
                icon = NavigationPoint.ICON_SETTER;
            }
        }

        // build nav point
        List<AstNode> params = fn.getParams();
        if (!params.isEmpty()) {
            StringBuilder b = new StringBuilder(id).append("(<font color=gray>");
            int paramCount = params.size();
            for (int p = 0; p < paramCount; ++p) {
                if (p > 0) {
                    b.append(", ");
                }
                AstNode n = params.get(p);
                if (n instanceof Name) {
                    b.append(((Name) n).getIdentifier());
                } else {
                    StrangeEons.log.warning(n.debugPrint());
                }
            }
            b.append("</font>)");
            id = b.toString();
        } else {
            id += "()";
        }

        int offset = fn.getAbsolutePosition();
        if (offset < 0) {
            offset = 0;
        }

        id = composeNameAndType(id, value);

        currentPointList.add(new NavigationPoint(
                id, id, offset, depth, icon
        ));
    }
}
