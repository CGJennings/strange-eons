package org.strangeeons.scripts;

import java.net.URL;
import java.util.Set;

/**
 * Identifies a scripting language that Strange Eons can host and provides
 * a factory for execution sessions in that language. Plug-ins ship a
 * runtime by implementing this interface and registering the class through
 * {@link java.util.ServiceLoader}; the SE host looks up an implementation
 * by {@link #id()} when a plug-in's manifest selects a non-default
 * scripting language.
 *
 * <p>Implementations are responsible for whatever runtime setup their
 * language requires (loading a polyglot context, configuring a class
 * loader, setting up host-language bindings). The host invokes only
 * {@link #createSession()} and the methods on the returned session.
 *
 * @since 3.5
 */
public interface ScriptLanguage {

    /**
     * Returns a stable identifier for this language. Used as the value of
     * the {@code script-language} key in a plug-in's manifest, so it
     * should be lower-case ASCII and unique across registered languages
     * (e.g. {@code "groovy"}, {@code "kotlin"}).
     */
    String id();

    /**
     * Returns a short, user-facing name for this language (e.g.
     * {@code "Groovy"}). May be localized.
     */
    String displayName();

    /**
     * Returns the file extensions this language uses, each including the
     * leading dot (e.g. {@code Set.of(".groovy", ".gvy", ".gy")}). Used
     * by editor and file-routing logic; never {@code null}.
     */
    Set<String> fileExtensions();

    /**
     * Creates a new isolated execution session for this language.
     */
    ScriptSession createSession();

    /**
     * Wraps a bundle's classloader so the language can resolve types
     * from script source on demand. The default implementation returns
     * {@code baseCl} unchanged. Languages whose runtime can compile
     * source files into Java classes (Groovy, Kotlin scripting) override
     * this to return a classloader that performs that compilation when
     * a class name resolves to a script resource on the classpath.
     *
     * <p>The host calls this exactly once per bundle that declares
     * {@code script-language = <id>} in its {@code eons-plugin}
     * manifest, when constructing the bundle's per-plug-in classloader.
     * The returned classloader becomes the bundle's effective CL for
     * all subsequent class and resource lookups.
     *
     * @param bundle the URL of the bundle whose classloader is being
     *     constructed
     * @param baseCl the classloader the host would otherwise use for
     *     this bundle (typically a {@link java.net.URLClassLoader} over
     *     the bundle's URL, parented to the shared library tier)
     * @return a classloader that delegates to {@code baseCl}; the
     *     default implementation returns {@code baseCl} unchanged
     */
    default ClassLoader wrapBundleClassLoader(URL bundle, ClassLoader baseCl) {
        return baseCl;
    }
}
