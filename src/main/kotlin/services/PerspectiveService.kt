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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
        // Delegate to batch analysis to reuse settings and request setup
        return analyzeMessages(listOf(text)).firstOrNull() ?: false
    }

    /**
     * Batch analyze multiple messages efficiently by reusing settings and attributes.
     */
    suspend fun analyzeMessages(texts: List<String>): List<Boolean> {
        if (apiKeyFromConfig.isEmpty()) {
            println("WARN: Perspective API key is empty. Skipping analysis for all messages.")
            return List(texts.size) { false }
        }
        val currentSettings = PerspectiveSettingsManager.getSettings()
        if (!currentSettings.isEnabled) {
            println("INFO: Perspective service disabled. Skipping analysis for all messages.")
            return List(texts.size) { false }
        }
        val activeAttributes = currentSettings.attributeSettings.filter { it.value.enabled }
        if (activeAttributes.isEmpty()) {
            println("INFO: No Perspective attributes enabled. Skipping analysis for all messages.")
            return List(texts.size) { false }
        }
        // Prepare static JSON parts
        val requestedAttributesJson = buildJsonObject {
            activeAttributes.keys.forEach { attrName -> put(attrName, buildJsonObject {}) }
        }
        val highlySensitiveAttributes = listOf("PROFANITY", "SEXUALLY_EXPLICIT", "FLIRTATION")
        val anyHighly = activeAttributes.keys.any { it in highlySensitiveAttributes }
        val languagesForRequest = if (anyHighly) buildJsonArray { add("en") } else buildJsonArray { add("es"); add("en"); add("ca") }
        val languagesString = languagesForRequest.joinToString { it.jsonPrimitive.content }
        println("Batch analyzing ${texts.size} messages. Languages: $languagesString, DoNotStore: ${currentSettings.doNotStore}, SpanAnnotations: ${currentSettings.spanAnnotations}")
        return coroutineScope {
            texts.map { text ->
                async {
                    // Build request body per text
                    val requestBody = buildJsonObject {
                        put("comment", buildJsonObject { put("text", text) })
                        put("languages", languagesForRequest)
                        put("requestedAttributes", requestedAttributesJson)
                        put("doNotStore", currentSettings.doNotStore)
                        put("spanAnnotations", currentSettings.spanAnnotations)
                    }
                    try {
                        val response: HttpResponse = client.post(perspectiveApiUrl) {
                            parameter("key", apiKeyFromConfig)
                            contentType(ContentType.Application.Json)
                            setBody(requestBody.toString())
                        }
                        if (response.status == HttpStatusCode.OK) {
                            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                            val attrScores = json["attributeScores"]?.jsonObject ?: return@async false
                            // Check all returned attributes
                            attrScores.any { (attrName, scoreEl) ->
                                val scoreValue = scoreEl.jsonObject["summaryScore"]
                                    ?.jsonObject?.get("value")?.jsonPrimitive?.floatOrNull
                                val threshold = currentSettings.attributeSettings[attrName]?.threshold
                                scoreValue != null && threshold != null && scoreValue >= threshold
                            }
                        } else {
                            println("Error from Perspective API in batch: ${response.status}")
                            false
                        }
                    } catch (e: Exception) {
                        println("Exception during batch analysis for text '$text': ${e.message}")
                        false
                    }
                }
            }.awaitAll()
        }
    }
}
