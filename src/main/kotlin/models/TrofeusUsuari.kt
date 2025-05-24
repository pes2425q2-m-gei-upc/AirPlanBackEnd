package org.example.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TrofeusUsuari(
    val usuari: String,
    val trofeuId: Int,
    @Contextual val dataObtencio: LocalDateTime
)