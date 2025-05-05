package org.example.models

import kotlinx.serialization.Serializable

@Serializable
data class UserTypeInfo(
    val tipo: String,
    val username: String,
    val nivell: Int? = null,
    val error: String? = null
)