package repositories

import kotlinx.datetime.LocalDateTime
import org.example.database.ActivitatTable
import org.example.database.UserBlockTable
import org.example.models.Localitzacio
import org.example.models.Activitat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import java.sql.Timestamp

class ActivitatRepository {
    fun afegirActivitat(activitat: Activitat): Int? {
        return try {
            transaction {
                // Perform the insert operation and return the generated ID
                val generatedId = ActivitatTable.insert {
                    it[nom] = activitat.nom
                    it[latitud] = activitat.ubicacio.latitud
                    it[longitud] = activitat.ubicacio.longitud
                    it[dataInici] = activitat.dataInici
                    it[dataFi] = activitat.dataFi
                    it[descripcio] = activitat.descripcio
                    it[username_creador] = activitat.creador
                } get ActivitatTable.id_activitat // Use `get` to retrieve the generated ID

                println("Generated activity ID: $generatedId") // Debugging
                generatedId
            }
        } catch (e: Exception) {
            println("Error while adding activity: ${e.message}")
            null // Return null in case of an error
        }
    }

    fun obtenirActivitats(): List<Activitat> {
        return transaction {
            ActivitatTable.selectAll().map { row ->
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

    fun eliminarActividad(id: Int): Boolean {
        return try {
            // Eliminar directamente y verificar filas afectadas
            transaction {
                val filasEliminadas = ActivitatTable.deleteWhere { ActivitatTable.id_activitat eq id }
                filasEliminadas > 0
            }
        } catch (e: Exception) {
            println("Error al eliminar actividad: ${e.message}")
            false
        }
    }

    fun modificarActivitat(
        id: Int,
        nom: String,
        descripcio: String,
        ubicacio: Localitzacio,
        dataInici: LocalDateTime,
        dataFi: LocalDateTime
    ): Boolean {
        return try {
            transaction {
                // Actualizar directamente y verificar filas afectadas
                val filasActualizadas = ActivitatTable.update({ ActivitatTable.id_activitat eq id }) {
                    it[ActivitatTable.nom] = nom
                    it[ActivitatTable.latitud] = ubicacio.latitud
                    it[ActivitatTable.longitud] = ubicacio.longitud
                    it[ActivitatTable.dataInici] = dataInici
                    it[ActivitatTable.dataFi] = dataFi
                    it[ActivitatTable.descripcio] = descripcio
                }
                filasActualizadas > 0
            }
        } catch (e: Exception) {
            println("Error al modificar actividad: ${e.message}")
            false
        }
    }

    fun getActivitatPerId(id: Int): Activitat {
        return transaction {
            ActivitatTable.select { ActivitatTable.id_activitat eq id }.map { row ->
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
            }.firstOrNull()
        } ?: throw Exception("Activitat no trobada")
    }

    /**
     * Obtiene todas las actividades excluyendo aquellas creadas por usuarios en la lista de bloqueados
     */
    fun obtenirActivitatsExcluintUsuaris(usuarisBloqueados: List<String>): List<Activitat> {
        return transaction {
            val query = if (usuarisBloqueados.isEmpty()) {
                ActivitatTable.selectAll()
            } else {
                ActivitatTable.select {
                    ActivitatTable.username_creador notInList usuarisBloqueados
                }
            }

            query.map { row ->
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

    /**
     * Obtiene actividades filtrando en una única consulta las que pertenecen a usuarios bloqueados o que han bloqueado al usuario
     * Esta es la versión más optimizada que realiza todo el filtrado en SQL con JOIN
     */
    fun obtenirActivitatsPerUsuariSenseBloquejos(username: String): List<Activitat> {
        return transaction {
            // Subconsulta para obtener usuarios que el usuario ha bloqueado o que lo han bloqueado a él
            val blockedUserSubQuery = UserBlockTable
                .slice(UserBlockTable.blockedUsername)
                .select { UserBlockTable.blockerUsername eq username }

            val blockerUserSubQuery = UserBlockTable
                .slice(UserBlockTable.blockerUsername)
                .select { UserBlockTable.blockedUsername eq username }

            // Consulta principal que excluye las actividades de usuarios bloqueados
            ActivitatTable
                .select {
                    // Excluimos actividades de usuarios que el usuario ha bloqueado
                    not(ActivitatTable.username_creador inSubQuery blockedUserSubQuery) and
                    // Excluimos actividades de usuarios que han bloqueado al usuario
                    not(ActivitatTable.username_creador inSubQuery blockerUserSubQuery)
                }
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
