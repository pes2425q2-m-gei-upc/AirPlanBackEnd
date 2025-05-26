package repositories

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.database.ActivitatFavoritaTable
import org.example.database.ActivitatTable
import org.example.models.Activitat
import org.example.models.Localitzacio
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType

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

    fun comprovarActivitatFavorita(idActivitat: Int, username: String): Boolean {
        return try {
            transaction {
                ActivitatFavoritaTable.select {
                    (ActivitatFavoritaTable.id_activitat eq idActivitat) and (ActivitatFavoritaTable.username eq username)
                }.count() > 0
            }
        } catch (e: Exception) {
            println("Error al obtenir activitats favorites: ${e.message}")
            false
        }
    }

    fun obtenirActivitatsFavoritesPerUsuari(username: String): List<Activitat> {
        return try {
            transaction {
                val join = Join(
                    ActivitatFavoritaTable,
                    ActivitatTable,
                    JoinType.INNER,
                    additionalConstraint = {
                        ActivitatFavoritaTable.id_activitat eq ActivitatTable.id_activitat
                    }
                )

                join.select {
                    ActivitatFavoritaTable.username eq username
                }.map { row ->
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
                        creador = row[ActivitatTable.username_creador],
                    )
                }
            }
        } catch (e: Exception) {
            println("Error al obtenir activitats favorites per usuari: ${e.message}")
            emptyList()
        }
    }

}