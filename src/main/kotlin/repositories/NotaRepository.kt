package org.example.repositories

import kotlinx.datetime.*
import org.example.models.Nota
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.database.NotesTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class NotaRepository {
    fun afegirNota(nota: Nota): Boolean {
        return try {
            transaction {
                NotesTable.insert {
                    it[username] = nota.username
                    it[fechaCreacion] = nota.fechaCreacion
                    it[horaRecordatorio] = nota.horaRecordatorio
                    it[comentario] = nota.comentario
                }.insertedCount > 0
            }
            true
        } catch (e: Exception) {
            println("Error al afegir la nota: ${e.message}")
            return false
        }
    }

    fun eliminarNota(id: Int): Boolean {
        return transaction {
            NotesTable.deleteWhere { NotesTable.id eq id } > 0
        }
    }

    fun obtenirNotesPerUsuari(username: String): List<Nota> {
        return transaction {
            NotesTable
                .select { NotesTable.username eq username }
                .map { row ->
                    Nota(
                        id = row[NotesTable.id],
                        username = row[NotesTable.username],
                        fechaCreacion = row[NotesTable.fechaCreacion],
                        horaRecordatorio = row[NotesTable.horaRecordatorio],
                        comentario = row[NotesTable.comentario]
                    )
                }
        }
    }

    fun editarNota(id: Int, novaNota: Nota): Boolean {
        return transaction {
            NotesTable.update({ NotesTable.id eq id }) {
                it[username] = novaNota.username
                it[fechaCreacion] = novaNota.fechaCreacion
                it[horaRecordatorio] = novaNota.horaRecordatorio
                it[comentario] = novaNota.comentario
            } > 0
        }
    }

    fun obtenirNotaPerId(id: Int): Nota? {
        return transaction {
            NotesTable.select { NotesTable.id eq id }
                .mapNotNull { row ->
                    Nota(
                        id = row[NotesTable.id],
                        username = row[NotesTable.username],
                        fechaCreacion = row[NotesTable.fechaCreacion],
                        horaRecordatorio = row[NotesTable.horaRecordatorio],
                        comentario = row[NotesTable.comentario]
                    )
                }.singleOrNull()
        }
    }

    fun obtenirNotesProperes(minutosMargen: Long): List<Nota> {
        return transaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Madrid"))
            val currentDate = now.date
            val currentTime = now.time

            // Convertir tiempo actual a minutos desde medianoche
            val currentMinutesFromMidnight = currentTime.hour * 60 + currentTime.minute
            val limitMinutesFromMidnight = currentMinutesFromMidnight + minutosMargen.toInt()

            // Crear tiempo límite (máximo 23:59)
            val limitHour = minOf(limitMinutesFromMidnight / 60, 23)
            val limitMinute = if (limitHour == 23) 59 else limitMinutesFromMidnight % 60
            val limitTime = LocalTime(limitHour, limitMinute)

            println("Buscando notas entre ${currentTime} y ${limitTime} para fecha ${currentDate}")

            // Buscar notas de hoy que estén entre la hora actual y la hora límite
            NotesTable.select {
                (NotesTable.fechaCreacion eq currentDate) and
                        (NotesTable.horaRecordatorio greater currentTime) and
                        (NotesTable.horaRecordatorio lessEq limitTime)
            }.map { row ->
                Nota(
                    id = row[NotesTable.id],
                    username = row[NotesTable.username],
                    fechaCreacion = row[NotesTable.fechaCreacion],
                    horaRecordatorio = row[NotesTable.horaRecordatorio],
                    comentario = row[NotesTable.comentario]
                )
            }
        }
    }
}