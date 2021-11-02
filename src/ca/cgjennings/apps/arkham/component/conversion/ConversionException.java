package ca.cgjennings.apps.arkham.component.conversion;

/**
 * This exception is thrown when a conversion can not be completed for any
 * reason.
 *
 * @see ConversionSession#convertGameComponent(ConversionTrigger,
 * ca.cgjennings.apps.arkham.component.GameComponent)
 * @author Henrik Rostedt
 */
public class ConversionException extends Exception {

    public ConversionException(String string) {
        super(string);
    }

    public ConversionException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }
}
