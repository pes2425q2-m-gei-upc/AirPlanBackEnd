package org.example.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

@Serializable
data class Valoracio(
    val username: String,  // User who creates the rating
    val idActivitat: Int,  // Activity ID
    val valoracion: Int,   // Rating between 1 and 5
    val comentario: String? = null, // Optional comment
    val fechaValoracion: LocalDateTime // Default timestamp
)