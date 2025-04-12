package repositories

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.database.ActivitatFavoritaTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
class ActivitatFavoritaRepository {

    fun afegirActivitatFavorita(idActivitat: Int, username_fav: String, dataNova: LocalDateTime): Boolean {
        return try {
            transaction {
                ActivitatFavoritaTable.insert {
                    it[id_activitat] = idActivitat
                    it[username] = username_fav
                    it[dataAfegida] = dataNova
                }
            }
            true
        } catch (e: Exception) {
            println("Error al afegir activitat favorita: ${e.message}")
            false
        }
    }

    fun eliminarActivitatFavorita(idActivitat: Int, username: String): Boolean {
        return try {
            transaction {
                ActivitatFavoritaTable.deleteWhere {
                    (ActivitatFavoritaTable.id_activitat eq idActivitat) and (ActivitatFavoritaTable.username eq username)
                } > 0
            }
        } catch (e: Exception) {
            println("Error al eliminar activitat favorita: ${e.message}")
            false
        }
    }
}