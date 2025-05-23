package org.example.repositories

import kotlinx.serialization.Serializable
import org.example.database.TrofeuTable
import org.example.database.TrofeusUsuariTable
import org.example.enums.NivellTrofeu
import org.example.models.Trofeu
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

@Serializable
data class TrofeuUsuariInfo(
    val trofeu: Trofeu,
    val obtingut: Boolean,
    @Serializable(with = InstantSerializer::class) val dataObtencio: Instant? = null
)

class TrofeuRepository {

    fun obtenirTrofeusPerUsuari(usuari: String): List<TrofeuUsuariInfo> {
        return transaction {
            val trofeus = TrofeuTable.selectAll().map { row ->
                Trofeu(
                    id = row[TrofeuTable.id],
                    nom = row[TrofeuTable.nom],
                    descripcio = row[TrofeuTable.descripcio],
                    nivell = NivellTrofeu.valueOf(row[TrofeuTable.nivell].toString()),
                    experiencia = row[TrofeuTable.experiencia],
                    imatge = row[TrofeuTable.imatge]
                )
            }

            val trofeusObtinguts = TrofeusUsuariTable
                .select { TrofeusUsuariTable.usuari eq usuari }
                .associateBy { it[TrofeusUsuariTable.trofeuId] }

            trofeus.map { trofeu ->
                val obtencio = trofeusObtinguts[trofeu.id]
                TrofeuUsuariInfo(
                    trofeu = trofeu,
                    obtingut = obtencio != null,
                    dataObtencio = obtencio?.get(TrofeusUsuariTable.dataObtencio)
                )
            }
        }
    }
}