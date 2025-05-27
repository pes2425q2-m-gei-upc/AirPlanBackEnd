package org.example.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class TrofeusUsuari(
    val usuari: String,
    val trofeuId: Int,
    @Serializable(with = LocalDateTimeSerializer::class) val dataObtencio: LocalDateTime
) {
    object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: LocalDateTime) {
            encoder.encodeString(value.format(formatter))
        }

        override fun deserialize(decoder: Decoder): LocalDateTime {
            return LocalDateTime.parse(decoder.decodeString(), formatter)
        }
    }
}