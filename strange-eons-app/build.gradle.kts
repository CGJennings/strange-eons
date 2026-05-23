import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
