package org.example.config

import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

@Serializable
data class AttributeSetting(
    val enabled: Boolean,
    val threshold: Float
)

@Serializable
data class PerspectiveCurrentSettings(
    val isEnabled: Boolean,
    val doNotStore: Boolean,
    val spanAnnotations: Boolean,
    val attributeSettings: Map<String, AttributeSetting>
)

object PerspectiveSettingsManager {
    private const val SETTINGS_FILE_PATH = "perspective_settings.properties"
    // List of attributes the system knows how to manage.
    // This should align with what the admin UI will present.
    val supportedAttributes = listOf(
        "TOXICITY", "SEVERE_TOXICITY", "IDENTITY_ATTACK",
        "INSULT", "PROFANITY", "THREAT",
        "SEXUALLY_EXPLICIT", "FLIRTATION" // Ensure these are valid Perspective API attribute names
    )

    // Helper to load properties, creating file with defaults if it doesn't exist
    private fun loadProperties(): Properties {
        val properties = Properties()
        val file = File(SETTINGS_FILE_PATH)
        if (file.exists()) {
            FileInputStream(file).use { properties.load(it) }
        } else {
            println("INFO: $SETTINGS_FILE_PATH not found. Creating with default values.")
            // Set default values
            properties.setProperty("perspective.enabled", "true")
            properties.setProperty("perspective.doNotStore", "false")
            properties.setProperty("perspective.spanAnnotations", "false")
            supportedAttributes.forEach { attr ->
                // Default TOXICITY to enabled, others to disabled
                properties.setProperty("perspective.attribute.$attr.enabled", if (attr == "TOXICITY") "true" else "false")
                properties.setProperty("perspective.attribute.$attr.threshold", "0.7") // Default threshold
            }
            saveProperties(properties) // Save these defaults to the new file
        }
        return properties
    }

    private fun saveProperties(properties: Properties) {
        try {
            FileOutputStream(SETTINGS_FILE_PATH).use { output ->
                properties.store(output, "Perspective API Settings")
            }
        } catch (e: Exception) {
            println("ERROR: Could not save perspective settings to $SETTINGS_FILE_PATH: ${'$'}{e.message}")
            // Optionally re-throw or handle more gracefully
        }
    }

    fun getSettings(): PerspectiveCurrentSettings {
        val properties = loadProperties() // Ensures file exists and defaults are applied if new

        val isEnabled = properties.getProperty("perspective.enabled", "true").toBoolean()
        val doNotStore = properties.getProperty("perspective.doNotStore", "false").toBoolean()
        val spanAnnotations = properties.getProperty("perspective.spanAnnotations", "false").toBoolean()

        val attributeSettings = mutableMapOf<String, AttributeSetting>()
        supportedAttributes.forEach { attr ->
            // For attributes not explicitly in the file (e.g., if new ones are added to supportedAttributes later),
            // provide a sensible default.
            val defaultEnabled = if (attr == "TOXICITY") "true" else "false"
            val defaultThreshold = "0.7"

            val attrEnabled = properties.getProperty("perspective.attribute.$attr.enabled", defaultEnabled).toBoolean()
            val attrThreshold = properties.getProperty("perspective.attribute.$attr.threshold", defaultThreshold).toFloatOrNull() ?: 0.7f
            attributeSettings[attr] = AttributeSetting(enabled = attrEnabled, threshold = attrThreshold)
        }
        return PerspectiveCurrentSettings(isEnabled, doNotStore, spanAnnotations, attributeSettings)
    }

    fun updateSettings(newSettings: PerspectiveCurrentSettings): Boolean {
        try {
            val properties = loadProperties() // Load existing to preserve any comments or unmanaged settings

            properties.setProperty("perspective.enabled", newSettings.isEnabled.toString())
            properties.setProperty("perspective.doNotStore", newSettings.doNotStore.toString())
            properties.setProperty("perspective.spanAnnotations", newSettings.spanAnnotations.toString())

            // Update only supported attributes present in newSettings
            newSettings.attributeSettings.forEach { (attr, setting) ->
                if (supportedAttributes.contains(attr)) {
                    properties.setProperty("perspective.attribute.$attr.enabled", setting.enabled.toString())
                    properties.setProperty("perspective.attribute.$attr.threshold", setting.threshold.toString())
                } else {
                    println("WARN: Attempted to update unsupported attribute '$attr'. Skipping.")
                }
            }
            // Ensure all supported attributes are in the properties file after an update
            // This ensures that if new attributes are added to `supportedAttributes` later,
            // they get written to the file with default or provided values.
            supportedAttributes.forEach { attr ->
                 if (!properties.containsKey("perspective.attribute.$attr.enabled")) {
                    val setting = newSettings.attributeSettings[attr] ?: AttributeSetting(if (attr == "TOXICITY") true else false, 0.7f)
                    properties.setProperty("perspective.attribute.$attr.enabled", setting.enabled.toString())
                    properties.setProperty("perspective.attribute.$attr.threshold", setting.threshold.toString())
                 }
            }

            saveProperties(properties)
            return true
        } catch (e: Exception) {
            println("ERROR: Failed to update perspective settings: ${'$'}{e.message}")
            e.printStackTrace()
            return false
        }
    }
}
