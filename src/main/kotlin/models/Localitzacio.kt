package org.example.models

import kotlinx.serialization.Serializable

@Serializable
class Localitzacio (
    var latitud: Double,
    var longitud: Double
) {
    override fun toString(): String {
        return "($latitud, $longitud)"
    }
}