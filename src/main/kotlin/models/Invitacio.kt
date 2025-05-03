package org.example.models

import kotlinx.serialization.Serializable

@Serializable
class Invitacio(
    val id_act: Int,
    val us_anfitrio: String,
    val us_destinatari: String,
)