package org.example.models
import java.time.LocalDateTime
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import kotlinx.serialization.Serializable

@Serializable
class Activitat(
    val id: Int,
    var nom: String,
    var descripcio: String,
    var ubicacio: Localitzacio,
    var dataInici: LocalDateTime,
    var dataFi: LocalDateTime,
    var creador: String,
    var participants: MutableList<String>
    //var imatge: String
) {
    fun afegirActivitat() {
        // Afegir a la base de dades
        val url = "jdbc:postgresql://nattech.fib.upc.edu:40351/midb"
        val user = "airplan"
        val password = "airplan1234"

        val sql = """
            INSERT INTO activitats (nom, descripcio, ubicacio, data_inici, data_fi, creador, participants)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        try {
            val conn: Connection = DriverManager.getConnection(url, user, password)
            val pstmt: PreparedStatement = conn.prepareStatement(sql)
            pstmt.setString(1, nom)
            pstmt.setString(2, descripcio)
            pstmt.setString(3, ubicacio.toString())
            pstmt.setObject(4, dataInici)
            pstmt.setObject(5, dataFi)
            pstmt.setString(6, creador)
            pstmt.setArray(7, conn.createArrayOf("VARCHAR", participants.toTypedArray()))

            pstmt.executeUpdate()
            pstmt.close()
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun modificarActivitat(
        nom: String,
        descripcio: String,
        ubicacio: Localitzacio,
        dataInici: LocalDateTime,
        dataFi: LocalDateTime
    ) {
        //Modificar de la base de dades i si no hi ha problema:
        this.nom = nom
        this.descripcio = descripcio
        this.ubicacio = ubicacio
        this.dataInici = dataInici
        this.dataFi = dataFi
    }

    fun eliminarActivitat() {
        // Eliminar de la base de dades
    }

    /*private fun generarImatgeMapa(ubicacio: String): String {
        // Aquí aniria la connexió a la base de dades per obtenir la imatge
        // Exemple de dades simulades
        return "https://www.google.com/maps/vt/data=${ubicacio}&zoom=13"
    }*/
}