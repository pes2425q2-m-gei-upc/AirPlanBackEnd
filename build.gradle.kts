plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core:2.2.3") // Core de Ktor
    implementation("io.ktor:ktor-server-netty:2.2.3") // Motor Netty para Ktor
    implementation("io.ktor:ktor-server-content-negotiation:2.2.3") // Negociación de contenido
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.3") // Serialización JSON
    implementation("io.ktor:ktor-server-status-pages:2.2.3") // Manejo de páginas de estado
    implementation("io.ktor:ktor-server-routing:2.2.3") // Rutas de Ktor

}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("Main.kt") // Asegúrate de que el nombre de la clase sea correcto
}