package org.example.models

class Localitzacio (
    var latitud: Double,
    var longitud: Double
) {
    override fun toString(): String {
        return "($latitud, $longitud)"
    }
}