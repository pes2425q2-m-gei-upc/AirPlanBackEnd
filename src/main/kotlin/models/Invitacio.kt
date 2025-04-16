package org.example.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

@Serializable
class Invitacio(
    val activitat: Activitat,
    @Contextual val anfitrio: Cliente,
    @Contextual val destinatario: Cliente
)