package org.example.models
import java.time.LocalDateTime
import java.util.UUID

data class Activitat(
    val id : String,
    val nom: String,
    val descripcio: String,
    val ubicacio: String,
    val dataInici: LocalDateTime,
    val dataFi: LocalDateTime,
    val qualitatAire: String,
    val contaminacio: Map<String, Int>,
    val imatgeUrl: String,
    val creador: String,
    val participants: MutableList<String>,
) {
    companion object {
        fun crearActivitat(
            nom: String,
            descripcio: String,
            ubicacio: String,
            dataInici: LocalDateTime,
            dataFi: LocalDateTime,
            creador: String
        ): Activitat {
            val contaminacio = obtenirContaminacio(ubicacio)

            return Activitat(
                id = UUID.randomUUID().toString(),
                nom = nom,
                descripcio = descripcio,
                ubicacio = ubicacio,
                dataInici = dataInici,
                dataFi = dataFi,
                qualitatAire = avaluarQualitatAire(contaminacio),
                contaminacio = contaminacio,
                imatgeUrl = generarImatgeMapa(ubicacio),
                creador = creador,
                participants = mutableListOf(creador) // El creador és l'únic participant inicial
            )
        }

        private fun obtenirContaminacio(ubicacio: String): Map<String, Int> {
            // Aquí aniria la connexió a la base de dades per obtenir la contaminació
            // Exemple de dades simulades
            return mapOf(
                "NO2" to (10..50).random(),
                "PM2.5" to (5..30).random(),
                "PM10" to (10..40).random(),
                "O3" to (5..50).random(),
                "SO2" to (1..10).random(),
                "CO" to (100..200).random()
            )
        }

        private fun avaluarQualitatAire(contaminacio: Map<String, Int>): String {
            val pm25 = contaminacio["PM2.5"] ?: 0
            val pm10 = contaminacio["PM10"] ?: 0
            val no2 = contaminacio["NO2"] ?: 0

            return when {
                pm25 > 25 || pm10 > 35 || no2 > 40 -> "Dolenta qualitat de l'aire"
                pm25 in 12..25 || pm10 in 20..35 || no2 in 20..40 -> "Qualitat de l'aire regular"
                else -> "Bona qualitat de l'aire"
            }
        }

        private fun generarImatgeMapa(ubicacio: String): String {
            // Aquí aniria la connexió a la base de dades per obtenir la imatge
            // Exemple de dades simulades
            return "https://www.google.com/maps/vt/data=${ubicacio}&zoom=13"
        }
    }
}