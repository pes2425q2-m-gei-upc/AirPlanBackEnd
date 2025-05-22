package org.example.models

import kotlinx.serialization.Serializable
import org.example.enums.NivellTrofeu

@Serializable
data class Trofeu(
    val id: Int? = null, // Nullable for cases where the ID is auto-generated
    val nom: String,
    val descripcio: String,
    val nivell: NivellTrofeu,
    val experiencia: Int,
    val imatge: String? = null // Nullable as per the database schema
)