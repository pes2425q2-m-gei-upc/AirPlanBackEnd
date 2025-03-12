package org.example.models
import java.time.LocalDateTime

class Activitat(
    val id: Int,
    var nom: String,
    var descripcio: String,
    var ubicacio: Localitzacio,
    var dataInici: LocalDateTime,
    var dataFi: LocalDateTime,
    var creador: String,
    var participants: MutableList<String>
) {
    fun afegirActivitat() {
        // Afegir a la base de dades
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