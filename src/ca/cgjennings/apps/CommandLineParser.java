package ca.cgjennings.apps;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Level;
import resources.Language;

/**
 * Parses command line options for an application. To use a
 * {@code CommandLineParser}, provide a class (possibly a subclass of this
 * class) with public fields whose names match the desired options. The parser
 * will search for options in the command line arguments and match them against
 * fields in the class of the supplied object. When an option name matches a
 * field name, the value of the field will be set to reflect the value of the
 * option.
 *
 * <p>
 * Depending on the type of the field, an option takes either 0 or 1 parameters.
 * If the type of the field is {@code boolean} or {@link Boolean}, it will take
 * no parameter. The value of the field will set to {@code true} if the option
 * is found, and otherwise it will not be changed. For other types, the argument
 * immediately following the option is taken to be its parameter. The following
 * field types can all be filled in automatically from this argument:
 * {@link String}, {@link File}, int, {@link Integer}, double, {@link Double},
 * {@link Class}, {@link Locale}, and {@link Enum}. Strings are copied as-is.
 * Files are filled in as if by {@code new File( argument )}; classes as if by
 * {@code Class.forName( argument )}. Numeric types are filled in as if by the
 * appropriate {@code valueOf} or {@code parseInt} method. If a class is not
 * found or there is a syntax error in the format of a numeric parameter, a
 * suitable error message is printed. {@code Locales} are interpreted using the
 * same mechanism as that used for resource bundles, e.g., en_CA would refer to
 * Canadian English. {@code Enum} types will be assigned a value by converting
 * the parameter to upper case and invoking the enum's {@code valueOf} method on
 * the result.
 *
 * <p>
 * If the type of a field is not one of the predefined types above, then the
 * type will be checked for a constructor that takes a single {@code String}
 * argument. If one exists, then it will be used to create the value to be
 * filled in the field by calling the constructor with the parameter string.
 * Otherwise, an {@code AssertionError} will be thrown.
 *
 * <p>
 * Any arguments that are neither an option nor a parameter for an option will
 * be collected and, after parsing is completed, may be fetched using
 * {@link #getPlainArguments()} or {@link #getPlainFiles()}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class CommandLineParser {

    /**
     * Holds the help/banner text.
     */
    private String usageText;

    /**
     * Holds the "plain" arguments after parsing completes.
     */
    private String[] leftOvers;

    /**
     * Creates a new command line parser that matches the public fields of the
     * subclass.
     */
    public CommandLineParser() {
    }

    /**
     * Sets the usage information to display when command line help is
     * requested.
     *
     * @param commandHelp the banner and help information to display
     */
    public void setUsageText(String commandHelp) {
        usageText = commandHelp;
    }

    /**
     * Returns the usage text to display when command line help is requested.
     *
     * @return the help information to display, or {@code null} if none set
     */
    public String getUsageText() {
        return usageText;
    }

    /**
     * Returns a copy of all of the arguments that were not used as options or
     * the value of an option. For a typical command line utility, this would be
     * the list of files on which the command is to operate. If the parser has
     * not parsed a command line, or if an error occurred during the last parse,
     * this will return {@code null}.
     *
     * @return the unused command line arguments, or {@code null}
     */
    public String[] getPlainArguments() {
        if (leftOvers != null) {
            return leftOvers.clone();
        } else {
            return null;
        }
    }

    /**
     * Returns a copy of all of the arguments that were not used as options or
     * the value of an option as {@code File} objects. For a typical command
     * line utility, this would be the list of files on which the command is to
     * operate. If the parser has not parsed a command line, or if an error
     * occurred during the last parse, this will return {@code null}.
     *
     * @return the unused command line arguments, or {@code null}
     * @see	#getPlainArguments()
     */
    public File[] getPlainFiles() {
        if (leftOvers != null) {
            File[] files = new File[leftOvers.length];
            for (int i = 0; i < files.length; ++i) {
                files[i] = new File(leftOvers[i]);
            }
            return files;
        } else {
            return null;
        }
    }

    /**
     * Returns the name of an option in a command line argument, stripped of its
     * prefix; if the argument does not represent an option, returns
     * {@code null}. The base class checks for a prefix of either "--" or "-".
     * If present, it returns the remainder of the argument. Otherwise, it
     * returns {@code null}. Subclasses may override this to enforce other
     * conventions.
     *
     * @param argument the argument which might represent an option name
     * @return the option name represented, or {@code null} if the argument is
     * not an option
     */
    protected String getOptionName(String argument) {
        String option = null;
        if (!argument.isEmpty()) {
            if (argument.charAt(0) == '-') {
                if (argument.length() > 1 && argument.charAt(1) == '-') {
                    option = argument.substring(2);
                } else {
                    option = argument.substring(1);
                }
            }
        }
        return option;
    }

    /**
     * If this returns {@code true}, then the matching of option names to fields
     * will be case sensitive. The base class returns {@code false}.
     *
     * @return {@code true} to make option names case sensitive
     */
    protected boolean isCaseSensitive() {
        return false;
    }

    /**
     * Returns {@code true} if and only if the supplied argument should be
     * interpreted as a request for help. The base class returns {@code true}
     * for any of: "--help", "-help", "--h", "-?", or "/?".
     *
     * @param argument the argument to test
     * @return {@code true} if help should be displayed
     */
    protected boolean isHelpOption(String argument) {
        return argument.equalsIgnoreCase("--help")
                || argument.equalsIgnoreCase("-help")
                || argument.equalsIgnoreCase("--h")
                || argument.equals("-?")
                || argument.equals("/?");
    }

    /**
     * Called to display usage information when help is requested. The base
     * class prints a message to the output stream and exits. If a
     * non-{@code null} usage text has been set with {@link #setUsageText}, then
     * this will be used as the text to display. If no other usage text is
     * defined, the base class will display a default message that is generated
     * from the fields in the target.
     *
     * @param target the object being used as a parsing template
     */
    protected void displayUsageText(Object target) {
        String text = getUsageText();
        if (text == null) {
            StringBuilder b = new StringBuilder("Options:\n");
            Field[] options = target.getClass().getFields();
            for (int i = 0; i < options.length; ++i) {
                b.append("  --").append(options[i].getName());
                Class<?> c = options[i].getType();
                if (c == String.class) {
                    b.append(" string");
                } else if (c == File.class) {
                    b.append(" file");
                } else if (c == int.class || c == Integer.class) {
                    b.append(" integer");
                } else if (c == double.class || c == Double.class) {
                    b.append(" number");
                } else if (c == Class.class) {
                    b.append(" class name");
                } else if (c != boolean.class && c != Boolean.class) {
                    b.append(' ').append(c.getSimpleName().toLowerCase());
                }
                b.append('\n');
            }
            text = b.toString();
        }
        System.out.println(text);
        System.exit(0);
    }

    /**
     * Called when there is an error with the command line syntax. The base
     * class prints the supplied message, then prints a line inviting the user
     * to use the {@code --help} option for more information, and then exits the
     * application with a return code of 20 (indicating an abnormal
     * termination).
     *
     * @param message the message to display, or {@code null} for the default
     * message "The command line arguments are incorrect"
     */
    protected void handleParsingError(String message) {
        if (message == null) {
            message = "The command line arguments are incorrect";
        }
        System.err.println(message);
        System.err.println("Use --help for more information");
        System.exit(20);
    }

    /**
     * Parses an application's command line arguments, filling in the matching
     * public fields of {@code target} as appropriate.
     *
     * @param target the object whose fields will be filled in, or {@code null}
     * to use this object
     * @param args the command line arguments to parse
     */
    public void parse(Object target, String... args) {
        boolean sense = isCaseSensitive();

        leftOvers = null;
        if (target == null) {
            target = this;
        }
        // All the options that we accept are the fields of this class
        Field[] objectFields = target.getClass().getFields();
        // This is where we stuff all the values not associated with an option
        LinkedList<String> files = new LinkedList<>();
        // When not null at the start of the loop, this is the field for an
        // option that is waiting to take the next argument as its value
        Field lastOption = null;

        for (int a = 0; a < args.length; ++a) {
            if (isHelpOption(args[a])) {
                displayUsageText(target);
                continue;
            }
            String option = getOptionName(args[a]);
            if (option != null) {
                if (lastOption != null) {
                    handleParsingError("Missing value for option " + lastOption.getName());
                    lastOption = null;
                }
                // Match the option name against the field names
                int op = 0;
                for (; op < objectFields.length; ++op) {
                    if (sense) {
                        if (objectFields[op].getName().equals(option)) {
                            break;
                        }
                    } else {
                        if (objectFields[op].getName().equalsIgnoreCase(option)) {
                            break;
                        }
                    }
                }
                if (op == objectFields.length) {
                    handleParsingError("Unknown option " + option);
                    continue;
                }
                lastOption = objectFields[op];
                // Boolean options take no argument but are just set to true;
                // otherwise, we will try to fill in a value on the next pass
                if (lastOption.getType() == boolean.class || lastOption.getType() == Boolean.class) {
                    setOption(target, option, lastOption, null);
                    lastOption = null;
                }
            } else if (lastOption != null) {
                // we found an option on the last pass; this pass we have the
                // value for the option and need to fill in the field
                setOption(target, lastOption.getName(), lastOption, args[a]);
                lastOption = null;
            } else {
                files.add(args[a]);
            }
        }
        // check for a hanging option without a value at the end of the command line
        if (lastOption != null) {
            handleParsingError("Missing value for option " + lastOption.getName());
        }
        leftOvers = files.toArray(new String[0]);
    }

    /**
     * This is called by the parser to set the value for an option in its
     * associated field.
     *
     * @param target the object whose fields should be modified
     * @param name the name of the option
     * @param field the field in which the option's value should be placed
     * @param value the argument from which the option's value must be derived;
     * {@code null} if the option takes no value
     */
    protected void setOption(Object target, String name, Field field, String value) {
        Object v; // the object to write into the field
        Class<?> type = field.getType();

        if (type == boolean.class || type == Boolean.class) {
            v = true;
        } else if (type == String.class) {
            v = value;
        } else if (type == File.class) {
            v = new File(value);
        } else if (type == int.class || type == Integer.class) {
            try {
                v = Integer.valueOf(value);
            } catch (NumberFormatException nfe) {
                handleParsingError("Expected an integer value for option " + name + ", but found " + value);
                return;
            }
        } else if (type == double.class || type == Double.class) {
            try {
                v = Double.valueOf(value);
            } catch (NumberFormatException nfe) {
                handleParsingError("Expected a numeric value for option " + name + ", but found " + value);
                return;
            }
        } else if (type == Class.class) {
            try {
                v = Class.forName(value);
            } catch (ClassNotFoundException cnf) {
                handleParsingError("No such class as " + value);
                return;
            }
        } else if (type == Locale.class) {
            v = Language.parseLocaleDescription(value);
            // This code provides a version not tied to the Strange Eons code base:
            /*
			String[] tokens = value.split( "_" );
			switch( tokens.length ) {
				case 1:
					v = new Locale( tokens[0].toLowerCase( Locale.CANADA ) );
					break;
				case 2:
					v = new Locale( tokens[0].toLowerCase( Locale.CANADA ), tokens[1].toUpperCase( Locale.CANADA ) );
					break;
				case 3:
					v = new Locale( tokens[0].toLowerCase( Locale.CANADA ), tokens[1].toUpperCase( Locale.CANADA ), tokens[2] );
					break;
				default:
					handleParsingError( "Invalid locale description for option " + name );
					return;
			}
             */
        } else if (type == Level.class) {
            try {
                v = Level.parse(value.toUpperCase(Locale.CANADA));
            } catch (IllegalArgumentException | NullPointerException e) {
                handleParsingError("Not a valid log level parameter for option " + name);
                return;
            }
        } else if (type.isEnum()) {
            try {
                v = null;
                Object[] enums = type.getEnumConstants();
                for (int i = 0; i < enums.length; ++i) {
                    String n = ((Enum) enums[i]).name();
                    if (n.equalsIgnoreCase(value)) {
                        v = enums[i];
                        break;
                    }
                }
                if (v == null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                handleParsingError("Not a valid parameter for option " + name);
                return;
            }
        } else {
            try {
                Constructor c = type.getConstructor(String.class);
                try {
                    v = c.newInstance(value);
                } catch (InvocationTargetException ite) {
                    final Throwable trueEx = ite.getCause();
                    handleParsingError("Exception \"" + trueEx + "\" while parsing option " + name);
                    return;
                }
            } catch (RuntimeException | ReflectiveOperationException e) {
                throw new AssertionError("unsupprted option field class: " + type);
            }
        }

        try {
            field.set(target, v);
        } catch (Exception e) {
            // the fields are already known to be public and the types are verified
            // before writing to the field, so no exception should be possible
            throw new AssertionError(e);
        }
    }
}
