package repositories

import org.example.database.SolicitudsTable
import org.example.database.ActivitatTable
import org.example.models.Activitat
import org.example.models.SolicitudUnio
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class SolicitudRepository {

    fun enviarSolicitud(usernameAnfitrio: String, usernameSolicitant: String, idActivitat: Int): Boolean {
        return try {
            transaction {
                SolicitudsTable.insert {
                    it[SolicitudsTable.usernameAnfitrio] = usernameAnfitrio
                    it[SolicitudsTable.usernameSolicitant] = usernameSolicitant
                    it[SolicitudsTable.idActivitat] = idActivitat
                }
            }
            true
        } catch (e: Exception) {
            println("Error al enviar la solicitud: ${e.message}")
            false
        }
    }

    fun eliminarSolicitud(usernameSolicitant: String, idActivitat: Int): Boolean {
        return transaction {
            SolicitudsTable.deleteWhere {
                (SolicitudsTable.usernameSolicitant eq usernameSolicitant) and
                        (SolicitudsTable.idActivitat eq idActivitat)
            } > 0
        }
    }

    fun obtenirSolicitudesPerUsuari(usernameSolicitant: String): List<Activitat> {
        println("🔍 Debug: Obteniendo actividades solicitadas por el usuario: $usernameSolicitant")
        return transaction {
            (SolicitudsTable innerJoin ActivitatTable).select {
                SolicitudsTable.usernameSolicitant eq usernameSolicitant
            }.map { row ->
                Activitat(
                    id = row[ActivitatTable.id_activitat],
                    nom = row[ActivitatTable.nom],
                    descripcio = row[ActivitatTable.descripcio],
                    ubicacio = org.example.models.Localitzacio(
                        latitud = row[ActivitatTable.latitud],
                        longitud = row[ActivitatTable.longitud]
                    ),
                    dataInici = row[ActivitatTable.dataInici],
                    dataFi = row[ActivitatTable.dataFi],
                    creador = row[ActivitatTable.username_creador],
                    imatge = row[ActivitatTable.imatge]
                )
            }
        }
    }

    fun existeixSolicitud(usernameAnfitrio: String, usernameSolicitant: String, idActivitat: Int): Boolean {
        return transaction {
            SolicitudsTable.select {
                (SolicitudsTable.usernameAnfitrio eq usernameAnfitrio) and
                        (SolicitudsTable.usernameSolicitant eq usernameSolicitant) and
                        (SolicitudsTable.idActivitat eq idActivitat)
            }.count() > 0
        }
    }

    fun eliminarTodasSolicitudesEntreUsuarios(user1: String, user2: String): Boolean {
        return try {
            transaction {
                // Eliminar solicitudes donde user1 es anfitrión y user2 es solicitante
                val count1 = SolicitudsTable.deleteWhere {
                    (usernameAnfitrio eq user1) and
                    (usernameSolicitant eq user2)
                }
                
                // Eliminar solicitudes donde user2 es anfitrión y user1 es solicitante
                val count2 = SolicitudsTable.deleteWhere {
                    (usernameAnfitrio eq user2) and
                    (usernameSolicitant eq user1)
                }
                
                println("✅ Se eliminaron ${count1 + count2} solicitudes entre $user1 y $user2")
                true
            }
        } catch (e: Exception) {
            println("Error al eliminar solicitudes entre usuarios: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun obtenirSolicitudesPerActivitat(idActivitat: Int): List<SolicitudUnio> {
        println("🔍 Debug: Obteniendo solicitudes para la actividad con ID: $idActivitat")
        return transaction {
            SolicitudsTable.select {
                SolicitudsTable.idActivitat eq idActivitat
            }.map { row ->
                SolicitudUnio(
                    usernameAnfitrio = row[SolicitudsTable.usernameAnfitrio],
                    usernameSolicitant = row[SolicitudsTable.usernameSolicitant],
                    idActivitat = row[SolicitudsTable.idActivitat]
                )
            }
        }
    }
}