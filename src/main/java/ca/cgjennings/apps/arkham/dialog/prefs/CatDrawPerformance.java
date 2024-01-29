package ca.cgjennings.apps.arkham.dialog.prefs;

import static resources.Language.string;
import resources.Settings;

/**
 * Preference category for drawing/performance settings.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class CatDrawPerformance extends FillInPreferenceCategory {

    public CatDrawPerformance() {
        super(string("sd-l-drawing"), "icons/prefs/drawing.png");

        heading(string("sd-l-drawing"));
        subheading(string("sd-l-previews"));
        join();
        tip(string("sd-tip-frequency"));
        label(string("sd-l-frequency"));
        SBSliderKit kit = new SBSliderKit(0, 100, 10, 5, string("sd-l-freq-rare"), string("sd-l-freq-often"), true) {
            @Override
            public void fromSetting(String v) {
                int stop;
                try {
                    stop = (Settings.integer(v) + 5) / 10;
                    if (stop < 0) {
                        stop = 0;
                    } else if (stop > 100) {
                        stop = 100;
                    }
                } catch (Settings.ParseError e) {
                    stop = 100;
                }
                slider.setValue(100 - stop);
            }

            @Override
            public String toSetting() {
                int value = 100 - slider.getValue();
                if (value == 0) {
                    value = 1;
                }
                value *= 10;
                return String.valueOf(value);
            }

            @Override
            public String updateSliderValue(int value) {
                return string("sd-l-freq-label", (100 - value) * 10);
            }
        };
        add("update-rate", kit);
        addCheckBox("use-downsample-sharpening", string("sd-b-sharpen"), false);

        subheading(string("sd-l-layout"));
        label(string("sd-l-text-fitting"));
        indent();
        addDropDown("default-text-fitting",
                new String[]{string("sd-c-fit-none"), string("sd-c-fit-spacing"), string("sd-c-fit-scale"), string("sd-c-fit-both")},
                new String[]{"none", "spacing", "scaling", "both"}
        );
        note(string("sd-l-fitting-note"));
        unindent();

        addResetKey("update-rate");
        addResetKey("default-text-fitting");
    }
}
