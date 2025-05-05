package org.example.models

import kotlinx.serialization.Serializable

@Serializable
class ValoracioInput(
    val username: String,  // User who creates the rating
    val idActivitat: Int,  // Activity ID
    val valoracion: Int,   // Rating between 1 and 5
    val comentario: String? = null, // Optional comment
)