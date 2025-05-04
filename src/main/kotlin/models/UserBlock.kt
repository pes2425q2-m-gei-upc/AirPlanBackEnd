package org.example.models

import kotlinx.serialization.Serializable

@Serializable
data class UserBlock(
    val blockerUsername: String,  // User who created the block
    val blockedUsername: String   // User who is blocked
)