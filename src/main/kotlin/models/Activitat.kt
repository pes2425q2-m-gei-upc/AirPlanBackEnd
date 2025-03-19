package org.example.models
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
class Activitat(
    val id: Int,
    var nom: String,
    var descripcio: String,
    var ubicacio: Localitzacio,
    var dataInici: Timestamp,
    var dataFi: Timestamp,
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
            pstmt.setTimestamp(4, dataInici)
            pstmt.setTimestamp(5, dataFi)
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