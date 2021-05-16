package ca.cgjennings.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A log record formatter that is less verbose than
 * {@code SimpleFormatter}. It only prints the time/date/location header
 * when the calling method changes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class BriefLogFormatter extends Formatter {

    private final Date eventTime = new Date();
    private final static String format = "{0,date} {0,time}";
    private MessageFormat formatter;
    private final Object args[] = new Object[1];
    private String lastClass, lastMethod;
    private static final String UNKNOWN_SOURCE = "<?>"; // cannot be null
    private final StringBuilder sb = new StringBuilder(256);

    /**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public synchronized String format(LogRecord record) {
        sb.delete(0, sb.length());

        // See if this record's location is different from the last one
        String thisClass = record.getSourceClassName();
        if (thisClass == null) {
            thisClass = UNKNOWN_SOURCE;
        }
        String thisMethod = record.getSourceMethodName();
        if (thisMethod == null) {
            thisMethod = UNKNOWN_SOURCE;
        }

        if (!thisMethod.equals(lastMethod) || !thisClass.equals(lastClass)) {
            eventTime.setTime(record.getMillis());
            args[0] = eventTime;
            StringBuffer text = new StringBuffer();
            if (formatter == null) {
                formatter = new MessageFormat(format);
            }
            formatter.format(args, text, null);
            sb.append('[').append(text).append("] ")
                    .append(thisClass).append(' ').append(thisMethod).append('\n');
            lastClass = thisClass;
            lastMethod = thisMethod;
        }

        sb.append("  ");
        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");
        sb.append(formatMessage(record));
        sb.append('\n');

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    record.getThrown().printStackTrace(pw);
                }
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }
        return sb.toString();
    }
}
