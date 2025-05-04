package org.example.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant

@Serializable
data class Missatge (
    val usernameSender: String,
    val usernameReceiver: String,
    val dataEnviament: LocalDateTime,
    val missatge: String,
    val isEdited: Boolean = false,
    val photoURL: String? = null // Nuevo campo para la URL de la imagen de perfil
)