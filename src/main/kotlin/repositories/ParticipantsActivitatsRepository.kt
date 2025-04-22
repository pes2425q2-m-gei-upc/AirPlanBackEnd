package org.example.repositories

import org.example.database.ParticipantsActivitatsTable
import org.example.models.ParticipantsActivitats
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ParticipantsActivitatsRepository {
    fun afegirParticipant(participant: ParticipantsActivitats): Boolean {
        return transaction {
            ParticipantsActivitatsTable.insert {
                it[id_activitat] = participant.id_act
                it[username_participant] = participant.us_participant
            }.insertedCount > 0
        }
    }

    fun eliminarParticipantsPerActivitat(idAct: Int): Boolean {
        return transaction {
            ParticipantsActivitatsTable.deleteWhere { ParticipantsActivitatsTable.id_activitat eq idAct } > 0
        }
    }

    fun esCreador(idAct: Int, usParticipant: String): Boolean {
        return transaction {
            ParticipantsActivitatsTable.select {
                (ParticipantsActivitatsTable.id_activitat eq idAct) and
                        (ParticipantsActivitatsTable.username_participant eq usParticipant)
            }.count() > 0
        }
    }
}