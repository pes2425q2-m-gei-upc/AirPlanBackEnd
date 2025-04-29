java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Or another LTS version like 11
    }
}

kotlin {
    jvmToolchain(17) // Match this with the Java version above
}

plugins {
    kotlin("jvm") version "2.0.0" // Usa una versión válida de Kotlin
    id("application") // Aplicar el plugin application correctamente
    kotlin("plugin.serialization") version "1.8.10"
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
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("io.ktor:ktor-client-core:2.3.2")
    implementation("io.ktor:ktor-client-cio:2.3.2") // or another engine like okhttp
    implementation("io.ktor:ktor-client-content-negotiation:2.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.2")

    // Exposed (ORM para Kotlin)
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")

    // Soporte para fechas en Exposed
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.45.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.45.0")


    // Driver de base de datos (elige uno)
    implementation("org.postgresql:postgresql:42.7.2")  // Para PostgreSQL

    // Ktor server test dependencies
    testImplementation("io.ktor:ktor-server-test-host:2.3.7")
    testImplementation("io.ktor:ktor-server-tests:2.3.7")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

// MockK for mocking
    testImplementation("io.mockk:mockk:1.13.8")

// Kotlin test
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.23")
    testImplementation("com.h2database:h2:2.2.224")  // Use the latest version
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

tasks.register("generateConfig") {
    doLast {
        val configFile = file("build/generated/kotlin/Config.kt")
        configFile.parentFile.mkdirs()
        configFile.writeText("""
            package org.example.config

            object Config {
                val HERE_API_KEY = System.getenv("HERE_API_KEY") ?: ""
                val ORS_API_KEY = System.getenv("ORS_API_KEY") ?: ""
            }
        """.trimIndent())
    }
}