package org.example.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Timestamp

@Serializable
class Activitat(
    val id: Int,
    var nom: String,
    var descripcio: String,
    @Contextual var ubicacio: Localitzacio,
    var dataInici: LocalDateTime,
    var dataFi: LocalDateTime,
    var creador: String,
) {
    fun afegirActivitat() {
        val url = "jdbc:postgresql://nattech.fib.upc.edu:40351/midb"
        val user = "airplan"
        val password = "airplan1234"

        val sql = """
            INSERT INTO activitats (nom, latitud, longitud, datainici, datafi, descripcio, username_creador)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        try {
            val conn: Connection = DriverManager.getConnection(url, user, password)
            conn.autoCommit = false
            val pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt.setString(1, nom)
            pstmt.setFloat(2, ubicacio.latitud)
            pstmt.setFloat(3, ubicacio.longitud)
            pstmt.setTimestamp(4, Timestamp.valueOf(dataInici.toJavaLocalDateTime()))
            pstmt.setTimestamp(5, Timestamp.valueOf(dataFi.toJavaLocalDateTime()))
            pstmt.setString(6, descripcio)
            pstmt.setString(7, creador)

            pstmt.executeUpdate()
            pstmt.close()
            conn.commit()
            conn.close()
        } catch (e: Exception) {
            println("Error al afegir l'activitat a la base de dades: ${e.message}")
        }
    }

    fun modificarActivitat(
        nom: String,
        descripcio: String,
        ubicacio: Localitzacio,
        dataInici: Timestamp,
        dataFi: Timestamp
    ) {
        this.nom = nom
        this.descripcio = descripcio
        this.ubicacio = ubicacio
        this.dataInici = dataInici.toLocalDateTime().toKotlinLocalDateTime()
        this.dataFi = dataFi.toLocalDateTime().toKotlinLocalDateTime()
    }

    fun eliminarActivitat() {
        // Eliminar de la base de dades
    }
}