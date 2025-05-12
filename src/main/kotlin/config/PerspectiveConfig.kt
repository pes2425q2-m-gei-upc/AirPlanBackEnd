package org.example.config

import java.util.Properties
import java.io.FileInputStream
import java.io.FileNotFoundException

object PerspectiveConfig {
    private val properties = Properties()
    val apiKey: String

    init {
        val propsFile = System.getProperty("user.dir") + "/secrets.properties"
        try {
            FileInputStream(propsFile).use { fis ->
                properties.load(fis)
            }
            apiKey = properties.getProperty("PERSPECTIVE_API_KEY")?.trim() ?: ""
            if (apiKey.isEmpty()) {
                println("WARN: PERSPECTIVE_API_KEY is missing or empty in secrets.properties. Perspective API will not function.")
            }
        } catch (e: FileNotFoundException) {
            println("ERROR: secrets.properties file not found at $propsFile. Perspective API will not function.")
            throw IllegalStateException("secrets.properties not found, Perspective API key cannot be loaded.", e)
        } catch (e: Exception) {
            println("ERROR: Could not load PERSPECTIVE_API_KEY from secrets.properties: ${'$'}{e.message}. Perspective API will not function.")
            throw IllegalStateException("Could not load PERSPECTIVE_API_KEY", e)
        }
    }
}
