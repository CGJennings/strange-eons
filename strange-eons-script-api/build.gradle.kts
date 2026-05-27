// Scripting-language SPI. Pure-Java interfaces and value types under
// org.strangeeons.scripts; no app dependencies. The strange-eons-app module
// implements ServiceLoader-based dispatch against this surface.

plugins {
    `java-library`
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
