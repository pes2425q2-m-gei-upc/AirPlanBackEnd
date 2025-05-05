package org.example.models
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.example.enums.TipusVehicle

@Serializable
class Ruta (
    var origen: Localitzacio,
    var desti: Localitzacio,
    var clientUsername: String,
    var data: LocalDateTime,
    var id: Int,
    var duracioMin: Int,
    var duracioMax: Int,
    var tipusVehicle: TipusVehicle
) {
}