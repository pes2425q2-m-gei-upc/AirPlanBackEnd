plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core:3.1.1") // Core de Ktor
    implementation("io.ktor:ktor-server-netty:3.1.1") // Motor Netty para Ktor
    implementation("io.ktor:ktor-server-content-negotiation:3.1.1") // Negociación de contenido
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1") // Serialización JSON
    implementation("io.ktor:ktor-server-status-pages:3.1.1") // Manejo de páginas de estado
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("ch.qos.logback:logback-classic:1.4.6")


}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("Main.kt") // Asegúrate de que el nombre de la clase sea correcto
}