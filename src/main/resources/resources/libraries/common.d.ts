declare module 'common' {
    global {
        var DEBUG: boolean;
    }
}



/**
 * The path or URL that identifies the source file of the running script.
 * If the script is being run directly from a code editor, this will be the
 * string `"Quickscript"`.
 */
declare const sourcefile: string | "Quickscript";

/**
 * Imports the specified library into the current script. The result is 
 * the same as if the code in the library was pasted at that point in your
 * script. The library can either be the name of one of the standard 
 * scripting libraries or else a `res://` URL pointing to a script 
 * resource.
 * 
 * If the same library is imported with `useLibrary` multiple times in the 
 * same script, only the first call has any effect.
 * 
 * **Examples:**
 * ```
 * // import standard library "markup"
 * useLibrary("markup");
 * // import user library from a plug-in, located at
 * // resources/myname/myplugin/mylib.js
 * useLibrary("res://myname/myplugin/mylib.js");
 * ```
 * 
 * @param libraryName name of standard library, or resource path
 */
declare function useLibrary(libraryName: string): void;

/**
 * Stops executing the script. If the script attached event listeners to objects or started other threads, they will continue to operate.
 */
declare function exit(): void;

/**
 * Pauses script execution for the specified period of time. Returns true if
 * you interrupt the sleep period from another thread; otherwise false.
 * Calling this from the main thread will cause the application
 * to be unresponsive while sleeping; use caution when choosing a sleep
 * duration.
 * 
 * @param msDelay the time to sleep, in milliseconds (default is 1000)
 * @returns true if the sleep was interrupted, otherwise false
 */
declare function sleep(msDelay?: number): boolean;

/**
 * Displays a message to the user. If clicked, the message will disappear
 * instantly. Otherwise, if it is a plain message and not a warning or error
 * message, it fades away over time. For example:
 * 
 * ```
 * alert("Caution:\nFloor is slippery when wet", false);
 * ```
 * 
 * @param message the message to display
 * @param isErrorMessage if true, the message is presented as an error; if false, as a warning; if not specified, as a plain message
 */
declare function alert(message: string, isErrorMessage: boolean): void;

/**
 * Prints an object to the script console. You may pass multiple arguments
 * to this function; the arguments will be printed in sequence. For example:
 * 
 * ```
 * print("Hello");
 * ```
 * 
 * @param obj the object to be printed
 */
declare function print(...obj: any[]): void;

/**
 * Prints an object to the script console, then starts a new line.
 * You may pass multiple arguments to this function; the arguments
 * will be printed in sequence, then a new line will be started.
 * 
 * ```
 * for(let i=1; i<=10; ++i) println(i);
 * ```
 * 
 * @param obj the object to be printed
 */
declare function println(...obj: any[]): void;

/**
 * Prints a formatted message string. The effect is the same as
 * formatting the string with [[sprintf]] and then [[print]]ing
 * the result. Formatting will be localized using the interface language.
 * For example:
 * 
 * ```
 * printf("Your lucky number is %d\n", 1 + Math.random()*100);
 * ```
 * 
 * @param format the format string
 * @param args arguments referenced by the string's format specifiers
 */
declare function printf(format, ...args: any[]): void;

/**
 * Prints a formatted message string. The effect is the same as
 * formatting the string with [[sprintf]] and then [[print]]ing
 * the result. For example:
 * 
 * ```
 * let languages = [new Language("en"), new Language("fr")];
 * for(let lang of languages) {
 *     printf(lang, "In %s, the decimal of 1/2 is %.1f\n", lang.locale.displayName, 1/2);
 * }
 * ```
 * 
 * @param language the language or locale used to localize the formatting, or null for no localization
 * @param format the format string
 * @param args arguments referenced by the string's format specifiers
 */
declare function printf(language: Language, format, ...args: any[]): void;

/**
 * Returns a formatted string using the %-format string and arguments.
 * Formatting will be localized using the interface language.
 * 
 * Formatting behaviour is similar to Java's `String.format` method,
 * but `%i` is allowed as a synonym of `%d` and numeric arguments will
 * be coerced, if necessary, to fit the conversion type. For example,
 * passing a number as the argument for a `%d` conversion will coerce
 * the number to an integer type rather than cause an error.
 * the result.
 * 
 * @param format the format string
 * @param args arguments referenced by the string's format specifiers
 */
declare function sprintf(format, ...args: any[]): void;

/**
 * Returns a formatted string using the %-format string and arguments.
 * 
 * Formatting behaviour is similar to Java's `String.format` method,
 * but `%i` is allowed as a synonym of `%d` and numeric arguments will
 * be coerced, if necessary, to fit the conversion type. For example,
 * passing a number as the argument for a `%d` conversion will coerce
 * the number to an integer type rather than cause an error.
 * 
 * @param language the language or local used to localize the formatting, or null for no localization
 * @param format the format string
 * @param args arguments referenced by the string's format specifiers
 */
declare function sprintf(language: Language, format, ...args: any[]): void;