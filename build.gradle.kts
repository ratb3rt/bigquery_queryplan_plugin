plugins {
    id("org.jetbrains.intellij") version "1.3.0"
    kotlin("jvm") version "1.5.10"
    java
}

group = "net.ratb3rt.jetbrains"
version = "0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.cloud:google-cloud-bigquery:2.4.0")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
    implementation("guru.nidi:graphviz-kotlin:0.18.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    val kotlinxHtmlVersion = "0.7.2"

    // include for JVM target
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2021.2.3")
}
tasks {
    patchPluginXml {
        changeNotes.set("""
            Add change notes here.<br>
            <em>most HTML tags may be used</em>        """.trimIndent())
    }
}
tasks.getByName<Test>("test") {
    useJUnitPlatform()
}