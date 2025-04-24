package org.example.repositories

import org.example.models.Missatge
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.database.MissatgesTable
import java.time.Instant

class MissatgeRepository {
    suspend fun sendMessage(message: Missatge): Boolean {
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

    suspend fun getMessagesBetweenUsers(user1: String, user2: String): List<Missatge> {
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
}
