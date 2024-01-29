package resources.projects.pluginwizard;

import ca.cgjennings.ui.EditorPane;
import ca.cgjennings.ui.JIconList;
import ca.cgjennings.ui.wizard.WizardModel;
import java.util.EnumSet;
import javax.swing.DefaultListModel;
import static resources.Language.string;

/**
 * A standard plug-in wizard page that allows the user to choose a kind of
 * plug-in to create. This allows the user to select from the basic plug-in
 * types ({@code ACTIVATED}, {@code INJECTED}, or {@code EXTENSION}), and,
 * optionally, a theme or library.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public final class ContentTypePage extends javax.swing.JPanel {

    private WizardModel model;

    /**
     * Creates new plug-in type selection page that allows selection from the
     * specified types.
     *
     * @param allowedTypes a set describing the plug-in types to allow
     * @param model the model that the page will be placed in
     * @throws IllegalArgumentException if no types are allowed
     */
    public ContentTypePage(EnumSet<ContentType> allowedTypes, WizardModel model) {
        if (allowedTypes.isEmpty()) {
            throw new IllegalArgumentException("allowedTypes is empty");
        }
        initComponents();
        DefaultListModel<ContentType> m = new DefaultListModel<>();
        for (ContentType t : allowedTypes) {
            m.addElement(t);
        }
        typeList.setModel(m);
        typeList.setSelectedIndex(0);
        this.model = model;
    }

    /**
     * Creates new plug-in type selection page that includes the basic plug-in
     * types and may optionally include themes and libraries.
     *
     * @param allowThemes if {@code true}, then the user will be allowed to
     * select a theme plug-in (.setheme file type).
     * @param allowLibraries if {@code true}, then the user will be allowed to
     * select a library plug-in type (.selibrary file type).
     * @param model the model that the page will be placed in
     */
    public ContentTypePage(boolean allowThemes, boolean allowLibraries, WizardModel model) {
        this(getDefaultEnumSet(allowThemes, allowLibraries), model);
    }

    private static EnumSet<ContentType> getDefaultEnumSet(boolean allowThemes, boolean allowLibraries) {
        if (allowThemes) {
            if (allowLibraries) {
                return EnumSet.allOf(ContentType.class);
            } else {
                return EnumSet.of(ContentType.ACTIVATED, ContentType.INJECTED, ContentType.EXTENSION, ContentType.THEME);
            }
        } else {
            if (allowLibraries) {
                return EnumSet.of(ContentType.ACTIVATED, ContentType.INJECTED, ContentType.EXTENSION, ContentType.LIBRARY);
            } else {
                return EnumSet.of(ContentType.ACTIVATED, ContentType.INJECTED, ContentType.EXTENSION);
            }
        }
    }

    /**
     * Returns the selected plug-in content type.
     *
     * @return the selected content type
     */
    public ContentType getSelectedContentType() {
        return (ContentType) typeList.getSelectedValue();
    }

    /**
     * Changes the type of plug-in content that is selected.
     *
     * @param t the content type to select
     */
    public void setSelectedContentType(ContentType t) {
        typeList.setSelectedValue(t, true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        descField = new EditorPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        typeList =  new JIconList<ContentType>() ;

        jLabel1.setText(string("prj-l-plugin-wiz-choose-type")); // NOI18N

        descField.setEditable(false);
        descField.setContentType("text/html"); // NOI18N
        jScrollPane1.setViewportView(descField);

        typeList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        typeList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                typeListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(typeList);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void typeListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_typeListValueChanged
        ContentType t = (ContentType) typeList.getSelectedValue();
        if (t == null) {
            descField.setText("");
        } else {
            descField.setText(t.getDescription());
        }
        if (model != null) {
            model.setProgressBlocked(t == null);
        }
        descField.select(0, 0);
    }//GEN-LAST:event_typeListValueChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane descField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList<ContentType> typeList;
    // End of variables declaration//GEN-END:variables
}