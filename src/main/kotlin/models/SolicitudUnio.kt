package org.example.models
import kotlinx.serialization.Serializable

@Serializable
class SolicitudUnio (
    val idActivitat: Int,
    val usernameAnfitrio: String,
    val usernameSolicitant: String,
)