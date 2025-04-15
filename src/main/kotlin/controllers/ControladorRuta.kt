package org.example.controllers
import org.example.models.Ruta

class ControladorRuta {
    private val rutas = mutableListOf<Ruta>()

    fun crearRuta(ruta: Ruta): Ruta {
        rutas.add(ruta)
        return ruta
    }

    fun eliminarRuta(id: Int): Boolean {
        val ruta = rutas.find { it.id == id }
        return if (ruta != null) {
            rutas.remove(ruta)
            true
        } else {
            false
        }
    }

    fun obtenirTotesRutes(): List<Ruta> {
        return rutas
    }
}