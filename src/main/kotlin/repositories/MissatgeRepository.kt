package org.example.repositories

import kotlinx.datetime.toLocalDateTime
import org.example.models.Missatge
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.database.MissatgesTable
import org.example.database.UsuarioTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class MissatgeRepository {
    suspend fun sendMessage(message: Missatge, notify: suspend (Missatge) -> Unit): Boolean {
        return try {
            transaction {
                MissatgesTable.insert {
                    it[usernameSender] = message.usernameSender
                    it[usernameReceiver] = message.usernameReceiver
                    it[dataEnviament] = message.dataEnviament
                    it[missatge] = message.missatge
                    it[isEdited] = message.isEdited
                }
            }
            notify(message)

            true
        } catch (e: Exception) {
            false
        }
    }

    fun getMessagesBetweenUsers(user1: String, user2: String): List<Missatge> {
        return transaction {
            MissatgesTable
                .select { ((MissatgesTable.usernameSender eq user1) and (MissatgesTable.usernameReceiver eq user2)) or
                        ((MissatgesTable.usernameSender eq user2) and (MissatgesTable.usernameReceiver eq user1)) }
                .orderBy(MissatgesTable.dataEnviament to SortOrder.ASC)
                .map {
                    Missatge(
                        usernameSender = it[MissatgesTable.usernameSender],
                        usernameReceiver = it[MissatgesTable.usernameReceiver],
                        dataEnviament = it[MissatgesTable.dataEnviament],
                        missatge = it[MissatgesTable.missatge],
                        isEdited = it[MissatgesTable.isEdited]
                    )
                }
        }
    }

    fun getLatestChatsForUser(currentUsername: String): List<Missatge> {
        return transaction {
            // Subquery: obtener la lista de usuarios con los que ha hablado
            val subquery = MissatgesTable
                .select {
                    (MissatgesTable.usernameSender eq currentUsername) or
                            (MissatgesTable.usernameReceiver eq currentUsername)
                }
                .map {
                    val otherUser = if (it[MissatgesTable.usernameSender] == currentUsername)
                        it[MissatgesTable.usernameReceiver]
                    else
                        it[MissatgesTable.usernameSender]
                    otherUser
                }
                .distinct()

            // Para cada usuario con quien ha hablado, buscamos el último mensaje
            subquery.mapNotNull { otherUser ->
                // Obtener la URL de la imagen de perfil del otro usuario
                val photoURL = UsuarioTable
                    .select { UsuarioTable.username eq otherUser }
                    .map { it[UsuarioTable.photourl] }
                    .firstOrNull()

                MissatgesTable
                    .select {
                        ((MissatgesTable.usernameSender eq currentUsername) and (MissatgesTable.usernameReceiver eq otherUser)) or
                                ((MissatgesTable.usernameSender eq otherUser) and (MissatgesTable.usernameReceiver eq currentUsername))
                    }
                    .orderBy(MissatgesTable.dataEnviament to SortOrder.DESC)
                    .limit(1)
                    .map {
                        Missatge(
                            usernameSender = it[MissatgesTable.usernameSender],
                            usernameReceiver = it[MissatgesTable.usernameReceiver],
                            dataEnviament = it[MissatgesTable.dataEnviament],
                            missatge = it[MissatgesTable.missatge],
                            isEdited = it[MissatgesTable.isEdited],
                            photoURL = photoURL // Incluir la URL de la foto de perfil
                        )
                    }
                    .firstOrNull()
            }.sortedByDescending { it.dataEnviament }
        }
    }
    suspend fun editMessage(sender: String, originalTimestamp: String, newContent: String): Boolean {
        return try {
            transaction {
                val rowsUpdated = MissatgesTable.update({
                    (MissatgesTable.usernameSender eq sender) and
                            (MissatgesTable.dataEnviament eq originalTimestamp.toLocalDateTime())
                }) {
                    it[missatge] = newContent
                    it[isEdited] = true
                }
                rowsUpdated > 0
            }
        } catch (e: Exception) {
            println("Error editing message: ${e.message}")
            false
        }
    }

    suspend fun deleteMessage(sender: String, originalTimestamp: String): Boolean {
        return try {
            transaction {
                MissatgesTable.deleteWhere {
                    (MissatgesTable.usernameSender eq sender) and
                            (MissatgesTable.dataEnviament eq originalTimestamp.toLocalDateTime())
                } > 0
            }
        } catch (e: Exception) {
            println("Error deleting message: ${e.message}")
            false
        }
    }
}
