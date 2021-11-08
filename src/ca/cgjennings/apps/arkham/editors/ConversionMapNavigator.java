package ca.cgjennings.apps.arkham.editors;

/**
 * A {@link Navigator} implementation for conversion map files. Just a wrapper
 * around {@link PropertyNavigator} with different default values. Will treat
 * colons as part of a key instead of a deliminator.
 *
 * @author Henrik Rostedt
 */
public class ConversionMapNavigator extends PropertyNavigator {

    public ConversionMapNavigator() {
        super(true);
    }
}
