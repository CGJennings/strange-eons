package ca.cgjennings.apps.arkham.plugins.typescript;

import java.util.List;

/**
 * Encapsulates a node in a tree of points of interest in a source file,
 * with the information needed to jump to those points.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class NavigationTree {
    public NavigationTree(String name, String kind, String kindModifiers, TextSpan location) {
        this.name = name;
        this.kind = kind;
        this.kindModifiers = kindModifiers;
        this.location = location;
    }
    
   public String name;
   public String kind;
   public String kindModifiers;
   public TextSpan location;
   public List<NavigationTree> children;
   
   @Override
   public String toString() {
       StringBuilder b = new StringBuilder(1024);
       toString(b, 0, this);
       return b.toString();
   }
   
   private void toString(StringBuilder b, int level, NavigationTree root) {
       for (int i=0; i<level; ++i) {
           b.append(' ');
       }
       b.append(root.name).append(' ').append(root.location).append('\n');
       if (children != null) {
           for (NavigationTree child : children) {
               toString(b, level+1, child);
           }
       }
   }
}
