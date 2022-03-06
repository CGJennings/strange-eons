package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.diy.SettingBackedControl;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JSpinner;
import resources.ResourceKit;
import resources.Settings;

/**
 * A user interface control that cycles through a fixed set of options. This is
 * similar to a {@link JSpinner}, but the selected value cannot be edited with
 * the keyboard and the control has a button-like appearance rather than a
 * field-like appearance.
 *
 * <p>
 * The cycle control is appropriate when the user must select amongst a fixed,
 * small number of options. Two options is an ideal number, and three is
 * acceptable. More than that and it becomes awkward for the user to discover
 * the possible values.
 *
 * <p>
 * The button uses a simple model consisting of an array of the possible
 * choices. You can listen for changes in the selected item by adding an
 * {@link ActionListener} as you would for other button types.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JCycleButton<M> extends JButton implements SettingBackedControl {

    private Object[] cycleModel;
    private String[] settingValues;
    private int index = 0;
    private M sel;

    /**
     * Creates a new cycle button with an empty model.
     */
    public JCycleButton() {
        this((M[]) null);
    }

    /**
     * Creates a new cycle button with the specified model.
     *
     * @param model an array of the possible selections
     * @see #setCycleModel
     */
    public JCycleButton(M[] model) {
        this(model, null);
    }

    /**
     * Creates a new cycle button with the specified model and a matching array
     * of string values that will be used as setting proxies. The proxy values
     * are used when mapping the selected object to or from a
     * {@linkplain Settings setting value}. For example, the model could be a
     * string model with values "Female" and "Male", while the proxy setting
     * values could be "F" and "M".
     *
     * <p>
     * <b>Note:</b> This constructor is only relevant if using the button as
     * {@link SettingBackedControl}.
     *
     * @param model an array of the possible selections
     * @param settingValues an array of matching setting proxies, or
     * {@code null}
     * @see #setCycleModel
     * @see #toSetting
     * @see #fromSetting
     */
    public JCycleButton(M[] model, String[] settingValues) {
        this.settingValues = settingValues;
        setIcon(ResourceKit.getIcon("cycle"));
        initButton(this);
        setCycleModel(model);
    }

    private void initButton(JButton b) {
        b.setHorizontalTextPosition(TRAILING);
        b.setHorizontalAlignment(LEADING);
        b.putClientProperty("JButton.buttonType", "square");
        Insets m = b.getMargin();        
        if (b.getComponentOrientation() == ComponentOrientation.RIGHT_TO_LEFT) {
            m.right = Math.min(m.right, 3);
        } else {
            m.left = Math.min(m.left, 3);
        }
        b.setMargin(m);
    }

    /**
     * Returns the index of the selected item in the model. Returns -1 if the
     * model is {@code null} or empty.
     *
     * @return the index of the selected item
     */
    public int getSelectedIndex() {
        if (cycleModel == null || cycleModel.length == 0) {
            return -1;
        } else {
            return Math.max(0, Math.min(cycleModel.length - 1, index));
        }
    }

    /**
     * Sets the currently selected item. If the index is beyond the last element
     * in the model, or if it is less than zero, the first element is selected
     * (if any).
     *
     * @param index the index of the element in the model to select
     */
    public void setSelectedIndex(int index) {
        if (this.index != index) {
            this.index = index;
            updateButtonFace();
        }
    }

    /**
     * Sets the currently selected item. This has no effect if the supplied
     * value is not in the model.
     *
     * @param selection the model member to select
     */
    public void setSelectedItem(M selection) {
        if (selection == null) {
            setSelectedIndex(-1);
            index = -1;
            sel = null;
        } else if (cycleModel != null) {
            for (int i = 0; i < cycleModel.length; ++i) {
                if (selection.equals(cycleModel[i])) {
                    setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Returns the currently selected item, as displayed on the face of the
     * button.
     *
     * @return the selected item, or {@code null} if there is no model set
     */
    public M getSelectedItem() {
        return sel;
    }
    
    @Override
    public void setFont(Font font) {
        preferredSize = null;
        super.setFont(font);
    }
    
    @Override
    public void setIcon(Icon icon) {
        preferredSize = null;
        if (getIcon() == null) {
            super.setIcon(icon);
        }
    }

    /**
     * Sets the model that the button cycles through. The button will make its
     * own copy of the supplied model.
     *
     * @param model an array of the possible selections
     * @throws IllegalArgumentException if there are proxy setting values and
     * the number of elements in the model does not match the number of proxy
     * setting values
     */
    @SafeVarargs
    public final void setCycleModel(M... model) {
        if (settingValues != null) {
            if (model == null) {
                throw new IllegalArgumentException("new model is null but proxy settings are not null");
            }
            if (model.length != settingValues.length) {
                throw new IllegalArgumentException("new model size does not match number of proxy settings");
            }
        }

        if (model == null) {
            this.cycleModel = null;
        } else {
            this.cycleModel = model.clone();
        }
        preferredSize = null;
        getPreferredSize();
        setSize(preferredSize);
        updateButtonFace();
    }

    /**
     * Returns a copy of the current model.
     *
     * @return an array of the values that the button will cycle through;
     * {@code null} if no model has been set
     */
    public Object[] getCycleModel() {
        return cycleModel == null ? null : cycleModel.clone();
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        ++index;
        updateButtonFace();
        super.fireActionPerformed(event);
    }
    
    @Override
    public Dimension getPreferredSize() {
        if (preferredSize == null) {
            preferredSize = super.getPreferredSize();
            if (cycleModel != null && cycleModel.length > 0) {
                JButton b = new JButton();
                b.setIcon(getIcon());
                initButton(b);
                for (int i=0; i<cycleModel.length; ++i) {
                    b.setText(cycleModel[i] == null ? null : cycleModel[i].toString());
                    Dimension d = b.getPreferredSize();
                    preferredSize.width = Math.max(preferredSize.width, d.width);
                    preferredSize.height = Math.max(preferredSize.height, d.height);
                }
            }
            // an error margin is required in some cases, not sure why
            preferredSize.width += 4;
        }
        return preferredSize;
    }
    private Dimension preferredSize;
    
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    @Override
    public Dimension getSize() {
        return getPreferredSize();
    }

    @SuppressWarnings("unchecked")
    private void updateButtonFace() {
        sel = null;

        if (cycleModel != null) {
            if (index >= cycleModel.length) {
                index = 0;
            } else if (index < 0) {
                index = 0;
            }
            if (index < cycleModel.length) {
                sel = (M) cycleModel[index];
            }
        }

        if (sel == null) {
            setText(null);
        } else {
            setText(sel.toString());
        }
    }

    /**
     * Selects the item that matches the specified setting value. If this button
     * was created with setting proxies, the index of the proxy that equals the
     * specified value will become the selected index. Otherwise, the element in
     * the model whose {@code toString} value matches the specified value is
     * selected.
     *
     * @param v the setting value that represents the value to select
     */
    @Override
    public void fromSetting(String v) {
        if (cycleModel != null) {
            if (settingValues == null) {
                for (int i = 0; i < cycleModel.length; ++i) {
                    if (cycleModel[i] == null ? v == null : cycleModel[i].toString().equals(v)) {
                        setSelectedIndex(i);
                        return;
                    }
                }
            } else {
                for (int i = 0; i < cycleModel.length; ++i) {
                    if (settingValues[i] == null ? v == null : settingValues[i].equals(v)) {
                        setSelectedIndex(i);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Returns a setting value to represent the selected item. If this button
     * was created with setting proxies, the proxy for the selected item is
     * returned. Otherwise, the {@code toString()} value of the selected item is
     * returned.
     *
     * @return a setting value for the selected item
     */
    @Override
    public String toSetting() {
        if (settingValues == null) {
            M sel = getSelectedItem();
            return sel == null ? null : sel.toString();
        } else {
            int sel = getSelectedIndex();
            return sel < 0 ? null : settingValues[getSelectedIndex()];
        }
    }
}
