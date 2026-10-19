subprojects {
    plugins.withId("java") {
        apply(plugin = "jacoco")
        extensions.configure<JacocoPluginExtension> {
            toolVersion = "0.8.14"
        }
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
            finalizedBy("jacocoTestReport")
        }
        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.required = true
                csv.required = true
            }
        }
    }
}
