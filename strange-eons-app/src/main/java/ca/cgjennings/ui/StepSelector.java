package ca.cgjennings.ui;

import java.util.Arrays;

/**
 * Chooses the next step from a list of steps given a current value (which may
 * lie between steps) and a number of steps to move. An example use is a ZUI
 * with both freeform and selectable preset zoom values (100%, 125%, etc.).
 *
 * @author Chris
 */
public class StepSelector {

    private double[] steps;

    public StepSelector(double[] steps) {
        steps = steps.clone();
        Arrays.sort(steps);
        this.steps = steps;
    }

    public double select(double fromNearestTo, int stepValue) {
        int pos = Arrays.binarySearch(steps, fromNearestTo);
        if (pos < 0) {
            pos = (-pos) - 1;
            if (stepValue > 0) {
                --pos;
            }
        }
        pos += stepValue;
        if (pos < 0) {
            pos = 0;
        }
        if (pos >= steps.length) {
            pos = steps.length - 1;
        }
        return steps[pos];
    }
}
