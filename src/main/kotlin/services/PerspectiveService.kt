package org.example.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable // Ensure this is imported
import org.example.config.PerspectiveConfig
import org.example.config.PerspectiveSettingsManager // Import Settings Manager
import org.example.config.PerspectiveCurrentSettings // Import Settings Data Class

@Serializable
data class AnalyzeCommentRequest(
    val comment: CommentBody,
    val languages: List<String>,
    val requestedAttributes: Map<String, AttributeParameters>
)

@Serializable
data class CommentBody(val text: String)

@Serializable
data class AttributeParameters(val scoreThreshold: Float? = null)

@Serializable
data class AnalyzeCommentResponse(
    val attributeScores: Map<String, AttributeScoreDetail>? = null // Make nullable for safety
)

@Serializable
data class AttributeScoreDetail(
    val summaryScore: Score
)

@Serializable
data class Score(val value: Float)

class PerspectiveService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true // Good for debugging
            })
        }
    }
    private val apiKeyFromConfig = PerspectiveConfig.apiKey // Corrected variable name from previous suggestion
    private val perspectiveApiUrl = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze"

    suspend fun analyzeMessage(text: String): Boolean {
        if (apiKeyFromConfig.isEmpty()) {
            println("WARN: Perspective API key is empty in secrets.properties. Skipping analysis. Message will be allowed.")
            return false 
        }

        val currentSettings = PerspectiveSettingsManager.getSettings()
        if (!currentSettings.isEnabled) {
            println("Perspective service is disabled by admin settings. Message will be allowed.")
            return false 
        }

        val activeAttributes = currentSettings.attributeSettings
            .filter { it.value.enabled }

        if (activeAttributes.isEmpty()) {
            println("No Perspective attributes enabled for analysis in admin settings. Message will be allowed.")
            return false 
        }

        val requestedAttributesJson = buildJsonObject {
            activeAttributes.keys.forEach { attrName ->
                put(attrName, buildJsonObject {})
            }
        }

        // Attributes known to be highly sensitive to language specification or have limited support for non-English languages.
        val highlySensitiveAttributes = listOf("PROFANITY", "SEXUALLY_EXPLICIT", "FLIRTATION")
        val anyHighlySensitiveActive = activeAttributes.keys.any { it in highlySensitiveAttributes }

        val languagesForRequest = if (anyHighlySensitiveActive) {
            println("INFO: Detected highly sensitive attribute(s) (${activeAttributes.keys.filter { it in highlySensitiveAttributes }.joinToString()}). Analyzing with [en] only for maximum compatibility.")
            buildJsonArray { add("en") } // Use only "en"
        } else {
            println("INFO: No highly sensitive attributes detected. Analyzing with [es, en, ca].")
            buildJsonArray { add("es"); add("en"); add("ca") } // Default to broader language set
        }

        val requestBody = buildJsonObject {
            put("comment", buildJsonObject { put("text", text) })
            put("languages", languagesForRequest)
            put("requestedAttributes", requestedAttributesJson)
            put("doNotStore", currentSettings.doNotStore)
            put("spanAnnotations", currentSettings.spanAnnotations)
        }

        try {
            // Corrected string interpolation for the log message
            val languagesString = languagesForRequest.joinToString { it.jsonPrimitive.content }
            println("Sending request to Perspective API. URL: $perspectiveApiUrl, Languages: $languagesString, DoNotStore: ${currentSettings.doNotStore}, SpanAnnotations: ${currentSettings.spanAnnotations}")
            
            val response: HttpResponse = client.post(perspectiveApiUrl) {
                parameter("key", apiKeyFromConfig) 
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString()) 
            }

            val responseBodyText = response.bodyAsText()
            
            if (response.status == HttpStatusCode.OK) {
                val jsonResponse = Json.parseToJsonElement(responseBodyText).jsonObject
                val attributeScores = jsonResponse["attributeScores"]?.jsonObject
                    ?: run {
                        println("WARN: 'attributeScores' field missing in Perspective API response.")
                        return false 
                    }

                for ((attrName, attrSetting) in activeAttributes) {
                    val scoreObject = attributeScores[attrName]?.jsonObject
                    val summaryScoreObject = scoreObject?.get("summaryScore")?.jsonObject
                    val scoreValue = summaryScoreObject?.get("value")?.jsonPrimitive?.floatOrNull

                    if (scoreValue != null) {
                        println("Message attribute '$attrName': Score = $scoreValue, Threshold = ${'$'}{attrSetting.threshold}")
                        if (scoreValue >= attrSetting.threshold) {
                            println("FLAGGED: Message is toxic for attribute '$attrName'. Score $scoreValue >= Threshold ${'$'}{attrSetting.threshold}")
                            return true 
                        }
                    } else {
                        println("WARN: Could not parse summary score for attribute '$attrName'.")
                    }
                }
                println("PASSED: Message is not considered toxic by any active attribute.")
                return false 
            } else {
                println("Error from Perspective API: Status ${'$'}{response.status} - Body: $responseBodyText")
                return false 
            }
        } catch (e: Exception) {
            println("Exception during Perspective API call for text '$text': ${'$'}{e.message}")
            e.printStackTrace() 
            return false 
        }
    }
}
