package ca.cgjennings.apps.arkham.dialog.prefs;

import static resources.Language.string;

/**
 * Design support preferences category.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class CatDesignSupport extends FillInPreferenceCategory {

    public CatDesignSupport() {
        super(string("sd-l-design-support"), "icons/prefs/design.png");

        heading(string("sd-l-design-support"));

        addCheckBox("show-consequence-displays", string("sd-b-conseq-disp"), false);

        subheading(string("sd-l-validation"));
        addCheckBox("use-validation", string("sd-b-validate"), false);
        indent();
        addCheckBox("use-strict-validation", string("sd-b-strict"), false);
        addCheckBox("less-obvious-balance-warning", string("sd-b-subtle-validation"), false);

    }
}
