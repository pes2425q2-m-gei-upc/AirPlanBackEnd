package org.example.models
import java.time.LocalDateTime

class Activitat {
    val id: Int
    val nom: String
    val descripcio: String,
    val ubicacio: Localitzacio,
    val dataInici: LocalDateTime,
    val dataFi: LocalDateTime,

    //val imatgeUrl: String,
    val creador: String,
    val participants: MutableList<String>,

    constructor(
        nom: String,
        descripcio: String,
        ubicacio: Localitzacio,
        dataInici: LocalDateTime,
        dataFi: LocalDateTime,
        creador: String
    ) {

    }

    /*private fun generarImatgeMapa(ubicacio: String): String {
        // Aquí aniria la connexió a la base de dades per obtenir la imatge
        // Exemple de dades simulades
        return "https://www.google.com/maps/vt/data=${ubicacio}&zoom=13"
    }*/
}