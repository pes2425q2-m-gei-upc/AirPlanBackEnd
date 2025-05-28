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
            val timeZone = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
                .plus(2, DateTimeUnit.HOUR)
                .toLocalDateTime(timeZone)
            val currentDate = now.date
            val currentTime = now.time

            // Manejar el desbordamiento de minutos de forma segura
            var nextHours = currentTime.hour + ((currentTime.minute + minutosMargen) / 60).toInt()
            var nextMinutes = (currentTime.minute + minutosMargen) % 60

            // Si las horas se desbordan al día siguiente, limitamos a 23:59
            if (nextHours >= 24) {
                nextHours = 23
                nextMinutes = 59
            }

            val limitTime = LocalTime(nextHours, nextMinutes.toInt())

            println("Buscando notas entre ${currentTime} y ${limitTime} para fecha ${currentDate}")

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