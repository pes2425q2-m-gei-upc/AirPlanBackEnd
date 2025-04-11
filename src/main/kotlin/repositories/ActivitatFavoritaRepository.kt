package repositories

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.database.ActivitatFavoritaTable
import org.example.database.ActivitatTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


class ActivitatFavoritaRepository {
    fun afegirActivitatFavorita(idActivitat: Int, username_usuari: String, dataAddicio: LocalDateTime) : Boolean {
        return transaction {
            ActivitatFavoritaTable.insert {
                it[id_activitat] = idActivitat
                it[username] = username_usuari
                it[dataAfegida] = dataAddicio
            }
        }.insertedCount > 0;
    }

    fun eliminarActivitatFavorita(idActivitat: Int, username: String) : Boolean {
        return try {
            // Eliminar directamente y verificar filas afectadas
            transaction {
                val filasEliminadas = ActivitatTable.deleteWhere { (ActivitatFavoritaTable.id_activitat eq idActivitat) and (ActivitatFavoritaTable.username eq username)}
                filasEliminadas > 0
            }
        } catch (e: Exception) {
            println("Error al eliminar actividad: ${e.message}")
            false
        }
    }
}
