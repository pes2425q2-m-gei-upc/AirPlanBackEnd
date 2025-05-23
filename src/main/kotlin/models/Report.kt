package org.example.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Report(
    val reporterUsername: String,
    val reportedUsername: String,
    val reason: String,
    @Contextual val timestamp: LocalDateTime
)