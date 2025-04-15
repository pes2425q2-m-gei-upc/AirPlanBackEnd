
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.ResultRow
import org.example.models.Valoracio
import org.example.models.ValoracioInput
import org.example.database.ValoracioTable
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.insert

open class ValoracioRepository {
    // Ya tienes esto:
    private fun ResultRow.toValoracio(): Valoracio {
        return Valoracio(
            username = this[ValoracioTable.username],
            idActivitat = this[ValoracioTable.id_activitat],
            valoracion = this[ValoracioTable.valoracion],
            comentario = this[ValoracioTable.comentario],
            fechaValoracion = this[ValoracioTable.fecha_valoracion].toKotlinLocalDateTime()
        )
    }

    open fun afegirValoracio(input: ValoracioInput): Boolean {
        return try {
            transaction {
                ValoracioTable.insert {
                    it[username] = input.username
                    it[id_activitat] = input.idActivitat
                    it[valoracion] = input.valoracion
                    it[comentario] = input.comentario
                    // fecha_valoracion se genera automáticamente por la base de datos
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    open fun obtenirValoracionsPerActivitat(idActivitat: Int): List<Valoracio> = transaction {
        ValoracioTable.select { ValoracioTable.id_activitat eq idActivitat }
            .map { it.toValoracio() }
    }

    open fun obtenirValoracionsPerUsuari(username: String): List<Valoracio> = transaction {
        ValoracioTable.select { ValoracioTable.username eq username }
            .map { it.toValoracio() }
    }
}
