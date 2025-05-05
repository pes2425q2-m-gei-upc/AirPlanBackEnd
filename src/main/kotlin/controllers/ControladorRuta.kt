package org.example.controllers
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.enums.TipusVehicle
import org.example.models.Localitzacio
import org.example.models.Ruta
import org.example.repositories.RutaRepository

class ControladorRuta (private val rutaRepository: RutaRepository) {
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
        val newRuta = rutaRepository.afegirRuta(ruta)
        rutas.add(newRuta)
        return newRuta
    }

    fun actualitzarRuta(id: Int, rutaJson: JsonObject) {
        val ruta = rutaRepository.getRutaPerId(id)
        ruta.origen.latitud = rutaJson["origen"]?.jsonObject?.get("latitud")?.toString()?.toFloat() ?: 0.0f
        ruta.origen.longitud = rutaJson["origen"]?.jsonObject?.get("longitud")?.toString()?.toFloat() ?: 0.0f
        ruta.desti.latitud = rutaJson["desti"]?.jsonObject?.get("latitud")?.toString()?.toFloat() ?: 0.0f
        ruta.desti.longitud = rutaJson["desti"]?.jsonObject?.get("longitud")?.toString()?.toFloat() ?: 0.0f
        ruta.clientUsername = rutaJson["client"]!!.jsonPrimitive.content
        ruta.data = rutaJson["data"]!!.jsonPrimitive.content.let { LocalDateTime.parse(it) }
        ruta.duracioMin = rutaJson["duracioMin"]!!.jsonPrimitive.content.toInt()
        ruta.duracioMax = rutaJson["duracioMax"]!!.jsonPrimitive.content.toInt()
        ruta.tipusVehicle = TipusVehicle.valueOf(rutaJson["tipusVehicle"]!!.jsonPrimitive.content)

        rutaRepository.actualitzarRuta(ruta)

        val rutaBack = rutas.find { it.id == id }
        if (rutaBack != null) {
            ruta.origen.latitud = rutaJson["origen"]?.jsonObject?.get("latitud")?.toString()?.toFloat() ?: 0.0f
            ruta.origen.longitud = rutaJson["origen"]?.jsonObject?.get("longitud")?.toString()?.toFloat() ?: 0.0f
            ruta.desti.latitud = rutaJson["desti"]?.jsonObject?.get("latitud")?.toString()?.toFloat() ?: 0.0f
            ruta.desti.longitud = rutaJson["desti"]?.jsonObject?.get("longitud")?.toString()?.toFloat() ?: 0.0f
            ruta.clientUsername = rutaJson["client"]!!.jsonPrimitive.content
            ruta.data = rutaJson["data"]!!.jsonPrimitive.content.let { LocalDateTime.parse(it) }
            ruta.duracioMin = rutaJson["duracioMin"]!!.jsonPrimitive.content.toInt()
            ruta.duracioMax = rutaJson["duracioMax"]!!.jsonPrimitive.content.toInt()
            ruta.tipusVehicle = TipusVehicle.valueOf(rutaJson["tipusVehicle"]!!.jsonPrimitive.content)
        }
    }

    fun eliminarRuta(id: Int): Boolean {
        val ruta = rutas.find { it.id == id }
        if (ruta != null) {
            rutas.remove(ruta)
        }
        return rutaRepository.eliminarRuta(id)
    }

    fun obtenirTotesRutesClient(clientUsername: String): List<Ruta> {
        return rutaRepository.obtenirRutesClient(clientUsername)
    }
}