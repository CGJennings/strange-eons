import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem

plugins {
    java
    application
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":vendor"))

    implementation("com.formdev:flatlaf:3.4.1")
    implementation("com.fifesoft:rsyntaxtextarea:3.4.0")
    implementation("com.fifesoft:autocomplete:3.3.1")
    implementation("com.fifesoft:languagesupport:3.3.0") {
        exclude(group = "org.mozilla", module = "rhino")
    }
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.22.0")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:0.22.0")
    implementation("org.commonmark:commonmark-ext-image-attributes:0.22.0")
    implementation("com.miglayout:miglayout-swing:11.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "ca.cgjennings.apps.arkham.StrangeEons"
}

// Pull jar-loader through a dedicated configuration so we can pin it onto the
// -javaagent line for both run and the packaged jar.
val javaagent: Configuration by configurations.creating
dependencies {
    javaagent(project(mapOf("path" to ":vendor", "configuration" to "javaagent")))
}

// Filter src/build/templates/rev with the app.* properties from gradle.properties
// (overrideable from the CLI). Writes the resolved file into the layout the
// runtime expects: ca/cgjennings/apps/arkham/rev on the classpath.
val revOutputDir = layout.buildDirectory.dir("generated/sources/rev")
val generateRev = tasks.register<Copy>("generateRev") {
    from(layout.projectDirectory.file("src/build/templates/rev")) {
        into("ca/cgjennings/apps/arkham")
    }
    expand(
        "app" to mapOf(
            "build" to (project.findProperty("app.build") ?: "99999"),
            "major" to (project.findProperty("app.major") ?: "3"),
            "minor" to (project.findProperty("app.minor") ?: "6"),
            "patch" to (project.findProperty("app.patch") ?: "0"),
            "type"  to (project.findProperty("app.type")  ?: "DEVELOPMENT"),
        ),
    )
    into(revOutputDir)
}
sourceSets.named("main") {
    resources.srcDir(generateRev)
}

tasks.named<JavaExec>("run") {
    jvmArgs(
        "-Xmx4g", "-Xms1g", "-Xss1m",
        // JDK 17+ encapsulates internals strongly; a few helpers still touch
        // javax.swing.table internals. JDK 22+ gates FlatLaf's native window-
        // decoration loader behind native-access opt-in. Mirror these flags
        // into any future jpackage java-options when those profiles port over.
        "--add-opens=java.desktop/javax.swing.table=ALL-UNNAMED",
        "--enable-native-access=ALL-UNNAMED",
    )
    doFirst {
        jvmArgs("-javaagent:${javaagent.singleFile.absolutePath}")
    }
    args("--xDisableJreCheck")
}

tasks.register<JavaExec>("runDebugger") {
    description = "Runs the Strange Eons script debugging client."
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "ca.cgjennings.apps.arkham.plugins.debugging.Client"
}

tasks.register<JavaExec>("iconDesigner") {
    description = "Runs the themed glyph icon designer tool."
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "ca.cgjennings.ui.theme.ThemedGlyphIconDesigner"
}

// Pre-load Mockito as a JVM agent so JDK doesn't warn (and eventually refuse
// per JEP 451) on dynamic agent attachment. -Xshare:off mutes the CDS warning
// caused by the agent appending to the bootstrap classpath.
tasks.named<Test>("test") {
    val mockitoJar = configurations.testRuntimeClasspath.get()
        .resolvedConfiguration.resolvedArtifacts
        .first { it.moduleVersion.id.group == "org.mockito" && it.moduleVersion.id.name == "mockito-core" }
        .file
    jvmArgs("-Xshare:off", "-javaagent:${mockitoJar.absolutePath}")
}

// Single fat jar at deployment/assembled/strange-eons.jar with the
// PreMain/Agent-Class entries so it doubles as the runtime -javaagent.
tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName = "strange-eons"
    archiveClassifier = ""
    archiveVersion = ""
    destinationDirectory = rootProject.layout.projectDirectory.dir("deployment/assembled")
    manifest {
        attributes(
            "Main-Class" to "ca.cgjennings.apps.arkham.StrangeEons",
            "PreMain-Class" to "ca.cgjennings.jvm.JarLoader",
            "Agent-Class" to "ca.cgjennings.jvm.JarLoader",
        )
    }
    exclude(
        "**/license-metadata/**",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/maven/**",
    )
}

// ---------------------------------------------------------------------------
// jpackage tasks
//
// Mirror the old Maven jpackage-{linux,mac,win} profiles: each task invokes
// the JDK 25 toolchain's jpackage tool against the shaded jar from shadowJar.
// CI activates one task per matrix runner (e.g. ./gradlew jpackageDeb on
// Linux, jpackageDmg on macOS). Output lands in deployment/installers/.
// ---------------------------------------------------------------------------

val javaToolchains = the<JavaToolchainService>()
val jpackageExecutable = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(25)
}.map {
    it.executablePath.asFile.parentFile.resolve("jpackage").absolutePath
}

val assembledDir = rootProject.layout.projectDirectory.dir("deployment/assembled")
val installersDir = rootProject.layout.projectDirectory.dir("deployment/installers")
val iconsDir = rootProject.layout.projectDirectory.dir("deployment/assets/icons/app")
val entitlementsPlist = rootProject.layout.projectDirectory.file("deployment/entitlements.plist")

fun appVersionString(): String =
    "${project.findProperty("app.major") ?: 3}." +
    "${project.findProperty("app.minor") ?: 6}." +
    "${project.findProperty("app.patch") ?: 0}"

fun commonJpackageArgs(): List<String> = listOf(
    "--name", "Strange Eons",
    "--app-version", appVersionString(),
    "--vendor", (project.findProperty("app.vendor") as String?) ?: "Christopher Currie",
    "--copyright", (project.findProperty("app.copyright") as String?)
        ?: "Copyright Christopher Jennings and contributors",
    "--input", assembledDir.asFile.absolutePath,
    "--dest", installersDir.asFile.absolutePath,
    "--main-jar", "strange-eons.jar",
    "--main-class", "ca.cgjennings.apps.arkham.StrangeEons",
    "--runtime-image", System.getProperty("java.home"),
    "--java-options", "-javaagent:\$APPDIR/strange-eons.jar",
    "--java-options", "--add-opens=java.desktop/javax.swing.table=ALL-UNNAMED",
    "--java-options", "--enable-native-access=ALL-UNNAMED",
    "--java-options", "-Xmx4g",
    "--java-options", "-Xms1g",
    "--java-options", "-Xss1m",
)

// Per-platform asset helpers. icon= paths inside the .properties files are
// resolved by jpackage relative to its CWD, so each jpackage Exec task must
// set workingDir = rootProject.projectDir for these references to find their
// targets under deployment/assets/.
val jpackageAssetsDir = rootProject.layout.projectDirectory.dir("deployment/jpackage")

fun fileAssociationArgs(platform: String): List<String> {
    val dir = jpackageAssetsDir.dir("file-associations/$platform").asFile
    return listOf("eon", "seplugin", "seext", "setheme", "selibrary", "seproject").flatMap {
        listOf("--file-associations", dir.resolve("$it.properties").absolutePath)
    }
}

fun launcherArg(name: String, platform: String, file: String): List<String> =
    listOf("--add-launcher", "$name=" + jpackageAssetsDir.file("launchers/$platform/$file").asFile.absolutePath)

// jpackage refuses to write into a non-empty --dest dir; clear it first.
val cleanInstallers = tasks.register<Delete>("cleanInstallers") {
    delete(installersDir)
}

// --- Linux ---------------------------------------------------------------

val linuxIcon = iconsDir.file("48.png").asFile.absolutePath
val linuxPackageArgs = listOf(
    "--linux-package-name", "strange-eons",
    "--linux-app-category", "Graphics",
    "--linux-menu-group", "Graphics",
    "--linux-shortcut",
)

tasks.register<Exec>("jpackageDeb") {
    group = "distribution"
    description = "Build a .deb installer (Linux only)."
    dependsOn("shadowJar", cleanInstallers)
    onlyIf { OperatingSystem.current().isLinux }
    doFirst { installersDir.asFile.mkdirs() }
    workingDir = rootProject.projectDir
    executable = jpackageExecutable.get()
    args(
        commonJpackageArgs()
            + listOf("--type", "deb", "--icon", linuxIcon)
            + linuxPackageArgs
            + fileAssociationArgs("linux")
            + launcherArg("sedebug", "linux", "debugger.properties"),
    )
}

tasks.register<Exec>("jpackageRpm") {
    group = "distribution"
    description = "Build a .rpm installer (Linux only)."
    dependsOn("shadowJar", cleanInstallers)
    onlyIf { OperatingSystem.current().isLinux }
    doFirst { installersDir.asFile.mkdirs() }
    workingDir = rootProject.projectDir
    executable = jpackageExecutable.get()
    args(
        commonJpackageArgs()
            + listOf("--type", "rpm", "--icon", linuxIcon)
            + linuxPackageArgs
            + fileAssociationArgs("linux")
            + launcherArg("sedebug", "linux", "debugger.properties"),
    )
}

tasks.register<Exec>("jpackageAppImage") {
    group = "distribution"
    description = "Build a Linux app-image directory."
    dependsOn("shadowJar", cleanInstallers)
    onlyIf { OperatingSystem.current().isLinux }
    doFirst { installersDir.asFile.mkdirs() }
    executable = jpackageExecutable.get()
    args(commonJpackageArgs() + listOf("--type", "app-image", "--icon", linuxIcon))
}

// --- macOS ---------------------------------------------------------------

// Pre-sign Mach-O dylibs (FlatLaf natives) embedded in the shaded jar before
// jpackage bundles them — Apple notarization rejects unsigned natives. The
// shell script no-ops when not on macOS or when no identity is provided, so
// it's safe to wire unconditionally.
val signJarNatives = tasks.register<Exec>("signJarNatives") {
    group = "distribution"
    description = "Sign Mach-O dylibs embedded in deployment/assembled/strange-eons.jar."
    dependsOn("shadowJar")
    workingDir = rootProject.projectDir
    val identity = (project.findProperty("strange-eons.apple.signing.identity") as String?) ?: ""
    onlyIf {
        OperatingSystem.current().isMacOsX && identity.isNotBlank()
    }
    commandLine(
        "deployment/scripts/sign-jar-natives.sh",
        "deployment/assembled/strange-eons.jar",
        identity,
        entitlementsPlist.asFile.absolutePath,
    )
}

tasks.register<Exec>("jpackageDmg") {
    group = "distribution"
    description = "Build a .dmg installer (macOS only)."
    dependsOn("shadowJar", signJarNatives, cleanInstallers)
    onlyIf { OperatingSystem.current().isMacOsX }
    doFirst { installersDir.asFile.mkdirs() }
    workingDir = rootProject.projectDir
    executable = jpackageExecutable.get()
    val identity = (project.findProperty("strange-eons.apple.signing.identity") as String?) ?: ""
    val macArgs = mutableListOf(
        "--type", "dmg",
        "--icon", iconsDir.file("application.icns").asFile.absolutePath,
        "--mac-package-name", "Strange Eons",
        "--mac-package-identifier", "ca.cgjennings.strange-eons",
    )
    if (identity.isNotBlank()) {
        // jpackage prepends "Developer ID Application: " before keychain
        // lookup, so callers pass just the team/user fragment here.
        macArgs += listOf(
            "--mac-sign",
            "--mac-signing-key-user-name", identity,
            "--mac-entitlements", entitlementsPlist.asFile.absolutePath,
            "--mac-package-signing-prefix", "ca.cgjennings.strange-eons.",
        )
    }
    args(
        commonJpackageArgs()
            + macArgs
            + fileAssociationArgs("mac")
            + launcherArg("sedebug", "mac", "debugger.properties"),
    )
}

// --- Windows -------------------------------------------------------------

tasks.register<Exec>("jpackageMsi") {
    group = "distribution"
    description = "Build a .msi installer (Windows only)."
    dependsOn("shadowJar", cleanInstallers)
    onlyIf { OperatingSystem.current().isWindows }
    doFirst { installersDir.asFile.mkdirs() }
    workingDir = rootProject.projectDir
    executable = jpackageExecutable.get()
    args(
        commonJpackageArgs()
            + listOf(
                "--type", "msi",
                "--icon", iconsDir.file("application.ico").asFile.absolutePath,
                "--win-shortcut",
                "--win-menu",
                "--win-menu-group", "Strange Eons",
                "--win-dir-chooser",
                "--win-upgrade-uuid", "9f5b1c30-0d2e-4f7a-8e1b-3a4d6b8e9c10",
            )
            + fileAssociationArgs("win")
            + launcherArg("sedebug", "win", "debugger.properties")
            + launcherArg("eons", "win", "eons-console.properties"),
    )
}
