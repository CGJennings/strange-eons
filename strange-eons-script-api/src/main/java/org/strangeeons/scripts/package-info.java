/**
 * Public API namespace for plug-ins that register a scripting language with
 * Strange Eons.
 *
 * <p>Every type in this package is a pure interface (or a small checked
 * exception) whose method signatures reference only types in {@code java.*}
 * and other types in this package. A plug-in that ships a scripting-language
 * runtime declares it by implementing {@link ScriptLanguage} and registering
 * the class through {@link java.util.ServiceLoader}; nothing about the
 * implementation is exposed to other plug-ins.
 *
 * <p>This namespace is deliberately separate from
 * {@code org.strangeeons.plugins} — that one is for general plug-in authors;
 * this one is for the narrower audience of authors who supply an alternate
 * scripting runtime. The two SPIs share no transitive types.
 *
 * @since 3.5
 */
package org.strangeeons.scripts;
