package ca.cgjennings.apps.arkham.component.conversion;

public class ConversionException extends Exception {

    public ConversionException(String string) {
        super(string);
    }

    public ConversionException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }
}
