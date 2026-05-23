plugins {
    `java-library`
}

// :vendor carries no Java source of its own; the bundled jars travel via the
// api(files(...)) declarations below. Skip the empty marker jar Gradle would
// otherwise emit for the java-library convention.
tasks.named<Jar>("jar") { enabled = false }

dependencies {
    api(files(
        "libs/j2k-1.0.jar",
        "libs/jar-loader-1.0.jar",
        "libs/layout-river-1.0.jar",
        "libs/spelling-1.1.jar",
        "libs/strange-rhino-1.0.jar",
        "libs/typescript-services-1.0.jar",
    ))
}

val javaagent: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = true
}

artifacts {
    add(javaagent.name, file("libs/jar-loader-1.0.jar"))
}
