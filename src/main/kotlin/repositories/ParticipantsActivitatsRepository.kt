package org.example.repositories

import org.example.database.ActivitatTable
import org.example.database.ParticipantsActivitatsTable
import org.example.models.Activitat
import org.example.models.Localitzacio
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

    fun eliminarParticipant(idAct: Int, username: String): Boolean {
        return transaction {
            ParticipantsActivitatsTable.deleteWhere {
                (ParticipantsActivitatsTable.id_activitat eq idAct) and
                        (ParticipantsActivitatsTable.username_participant eq username)
            } > 0
        }
    }

    fun obtenirParticipantsPerActivitat(idAct: Int): List<String> {
        return transaction {
            ParticipantsActivitatsTable
                .select { ParticipantsActivitatsTable.id_activitat eq idAct }
                .map { it[ParticipantsActivitatsTable.username_participant] }
        }
    }

    fun obtenirActivitatsPerParticipant(username: String): List<Activitat> {
        return transaction {
            (ParticipantsActivitatsTable innerJoin ActivitatTable)
                .select { ParticipantsActivitatsTable.username_participant eq username }
                .map { row ->
                    Activitat(
                        id = row[ActivitatTable.id_activitat],
                        nom = row[ActivitatTable.nom],
                        descripcio = row[ActivitatTable.descripcio],
                        ubicacio = Localitzacio(
                            latitud = row[ActivitatTable.latitud],
                            longitud = row[ActivitatTable.longitud]
                        ),
                        dataInici = row[ActivitatTable.dataInici],
                        dataFi = row[ActivitatTable.dataFi],
                        creador = row[ActivitatTable.username_creador]
                    )
                }
        }
    }

}