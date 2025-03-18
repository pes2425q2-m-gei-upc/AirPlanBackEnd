plugins {
    kotlin("jvm") version "2.0.0" // Usa una versión válida de Kotlin
    id("application") // Aplicar el plugin application correctamente
}

group = "org.example"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.8") // Core de Ktor
    implementation("io.ktor:ktor-server-netty:2.3.8") // Motor Netty para Ktor
    implementation("io.ktor:ktor-server-content-negotiation:2.3.8") // Negociación de contenido
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8") // Serialización JSON
    implementation("io.ktor:ktor-server-status-pages:2.3.8") // Manejo de páginas de estado
    implementation("io.ktor:ktor-server-cors:2.3.8") // Dependencia CORS
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Exposed (ORM para Kotlin)
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")

    // Soporte para fechas en Exposed
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.41.1")

    // Driver de base de datos (elige uno)
    implementation("org.postgresql:postgresql:42.7.2")  // Para PostgreSQL
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.example.MainKt") // Asegúrate de que el nombre de la clase sea correcto
}

// Configurar el JAR ejecutable
tasks.jar {
    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Main-Class" to "org.example.MainKt"
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}