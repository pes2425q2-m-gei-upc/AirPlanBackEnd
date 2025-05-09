package org.example.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Notificacions (
    val id: Int,
    val username: String,
    val type: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime
)