package org.example.controllers
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.enums.TipusVehicle
import org.example.models.Localitzacio
import org.example.models.Ruta
import org.example.repositories.RutaRepository

class ControladorRuta (private val RutaRepository: RutaRepository) {
    private val rutas = mutableListOf<Ruta>()

    fun crearRuta(rutaJson: JsonObject): Ruta {
        val origen = Localitzacio(
            latitud = rutaJson["origen"]?.jsonObject?.get("latitud")?.toString()?.toFloat() ?: 0.0f,
            longitud = rutaJson["origen"]?.jsonObject?.get("longitud")?.toString()?.toFloat() ?: 0.0f
        )
        val desti = Localitzacio(
            latitud = rutaJson["desti"]?.jsonObject?.get("latitud")?.toString()?.toFloat() ?: 0.0f,
            longitud = rutaJson["desti"]?.jsonObject?.get("longitud")?.toString()?.toFloat() ?: 0.0f
        )
        val ruta = Ruta(
            origen = origen,
            desti = desti,
            clientUsername = rutaJson["client"]!!.jsonPrimitive.content,
            data = rutaJson["data"]!!.jsonPrimitive.content.let { LocalDateTime.parse(it) },
            id = 0,
            duracioMin = rutaJson["duracioMin"]!!.jsonPrimitive.content.toInt(),
            duracioMax = rutaJson["duracioMax"]!!.jsonPrimitive.content.toInt(),
            tipusVehicle = TipusVehicle.valueOf(rutaJson["tipusVehicle"]!!.jsonPrimitive.content)
        )
        val newRuta = RutaRepository.afegirRuta(ruta)
        rutas.add(newRuta)
        return newRuta
    }

    fun eliminarRuta(id: Int): Boolean {
        val ruta = rutas.find { it.id == id }
        if (ruta != null) {
            rutas.remove(ruta)
        }
        return RutaRepository.eliminarRuta(id)
    }

    fun obtenirTotesRutesClient(clientUsername: String): List<Ruta> {
        return RutaRepository.obtenirRutesClient(clientUsername)
    }
}