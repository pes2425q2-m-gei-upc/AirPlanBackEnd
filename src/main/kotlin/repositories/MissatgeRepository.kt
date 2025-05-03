package org.example.repositories

import org.example.models.Missatge
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.database.MissatgesTable

class MissatgeRepository {
    fun sendMessage(message: Missatge): Boolean {
        return try {
            transaction {
                MissatgesTable.insert {
                    it[usernameSender] = message.usernameSender
                    it[usernameReceiver] = message.usernameReceiver
                    it[dataEnviament] = message.dataEnviament
                    it[missatge] = message.missatge
                }
            }
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
                        missatge = it[MissatgesTable.missatge]
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
                            missatge = it[MissatgesTable.missatge]
                        )
                    }
                    .firstOrNull()
            }.sortedByDescending { it.dataEnviament }
        }
    }
}
