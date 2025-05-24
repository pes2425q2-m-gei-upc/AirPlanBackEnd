package org.example.repositories

import org.example.database.InvitacioTable
import org.example.database.ActivitatTable
import org.example.models.Activitat
import org.example.models.Invitacio
import org.example.models.Localitzacio
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class InvitacioRepository {
    fun afegirInvitacio(invitacio: Invitacio): Boolean {
        return transaction {
            InvitacioTable.insert {
                it[id_activitat] = invitacio.id_act
                it[username_anfitrio] = invitacio.us_anfitrio
                it[username_convidat] = invitacio.us_destinatari
            }.insertedCount > 0
        }
    }

    fun eliminarInvitacio(invitacio: Invitacio): Boolean {
        return transaction {
            InvitacioTable.deleteWhere {
                (InvitacioTable.id_activitat eq invitacio.id_act) and
                (InvitacioTable.username_anfitrio eq invitacio.us_anfitrio) and
                (InvitacioTable.username_convidat eq invitacio.us_destinatari)
            } > 0
        }
    }

    fun obtenirTotesInvitacions(): List<Invitacio> {
        return transaction {
            InvitacioTable.selectAll().map { row ->
                Invitacio(
                    id_act = row[InvitacioTable.id_activitat],
                    us_anfitrio = row[InvitacioTable.username_anfitrio],
                    us_destinatari = row[InvitacioTable.username_convidat]
                )
            }
        }
    }

    fun obtenirActivitatsAmbInvitacionsPerUsuari(username: String) : List<Activitat> {
        return transaction {
            InvitacioTable
                .join(ActivitatTable, JoinType.INNER, InvitacioTable.id_activitat, ActivitatTable.id_activitat)
                .select { InvitacioTable.username_convidat eq username }
                .map {
                    Activitat(
                        id = it[ActivitatTable.id_activitat],
                        nom = it[ActivitatTable.nom],
                        descripcio = it[ActivitatTable.descripcio],
                        ubicacio = Localitzacio(
                            latitud = it[ActivitatTable.latitud],
                            longitud = it[ActivitatTable.longitud]
                        ),
                        dataInici = it[ActivitatTable.dataInici],
                        dataFi = it[ActivitatTable.dataFi],
                        creador = it[ActivitatTable.username_creador]
                    )
                }
        }
    }
}