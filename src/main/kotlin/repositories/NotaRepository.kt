package org.example.repositories

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
}