subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
