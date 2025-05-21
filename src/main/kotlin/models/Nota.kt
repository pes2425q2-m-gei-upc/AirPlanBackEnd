package org.example.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Nota(
    val id: Int? = null,
    val username: String,
    val fechaCreacion: LocalDate,
    val horaRecordatorio: LocalTime,
    val comentario: String
)