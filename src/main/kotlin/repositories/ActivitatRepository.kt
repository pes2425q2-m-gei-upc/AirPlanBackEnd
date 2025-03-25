package repositories

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import org.example.database.ActivitatTable
import org.example.models.Localitzacio
import org.example.models.Activitat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.sql.Timestamp
class ActivitatRepository {
    fun afegirActivitat(Activitat: Activitat) : Boolean {
        return transaction {
            ActivitatTable.insert {
                it[nom] = Activitat.nom
                it[latitud] = Activitat.ubicacio.latitud
                it[longitud] = Activitat.ubicacio.longitud
                it[dataInici] = Activitat.dataInici
                it[dataFi] = Activitat.dataFi
                it[descripcio] = Activitat.descripcio
                it[username_creador] = Activitat.creador
            }.insertedCount > 0;
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

    fun modificarActivitat(id: Int, nom: String, descripcio: String, ubicacio: Localitzacio, dataInici: LocalDateTime, dataFi: LocalDateTime): Boolean {
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
}
