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
    implementation("io.ktor:ktor-server-websockets:2.3.8") // WebSockets support
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Add these Ktor client dependencies
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.8")

    // HikariCP para pooling de conexiones
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Exposed (ORM para Kotlin)
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")

    // Soporte para fechas en Exposed
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.45.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.45.0")

    // Firebase Admin SDK para manejar la autenticación desde el backend
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // JavaMail para enviar correos electrónicos personalizados
    implementation("com.sun.mail:javax.mail:1.6.2")

    // Driver de base de datos (elige uno)
    implementation("org.postgresql:postgresql:42.7.2")  // Para PostgreSQL

    // Dependencias principales de testing
    testImplementation(kotlin("test")) // Incluye las assertions básicas

    // MockK
    testImplementation("io.mockk:mockk:1.13.8") // Versión estable más reciente

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    // Para integración con Kotlin test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.0")
    testImplementation("org.jetbrains.exposed:exposed-java-time:0.44.1")
    testImplementation("org.jetbrains.exposed:exposed-core:0.44.1")
    testImplementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // WebSocket
    testImplementation("io.ktor:ktor-server-websockets:2.3.7")
    testImplementation("io.ktor:ktor-client-websockets:2.3.7")

    // Ktor Test (si necesitas probar rutas)
    testImplementation("io.ktor:ktor-server-test-host:2.3.7")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.3.7")
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