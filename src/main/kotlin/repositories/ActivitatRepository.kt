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
            ActivitatTable.selectAll().map {
                Activitat(
                    id = it[ActivitatTable.id],
                    nom = it[ActivitatTable.nom],
                    descripcio = it[ActivitatTable.descripcio],
                    ubicacio = Localitzacio(it[ActivitatTable.latitud], it[ActivitatTable.longitud]),
                    dataInici  = it[ActivitatTable.dataInici],
                    dataFi = it[ActivitatTable.dataFi],
                    creador = it[ActivitatTable.username_creador],
                )
            }
        }
    }
}