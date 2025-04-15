package org.example.models
import ch.qos.logback.core.net.server.Client
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import org.example.enums.TipusVehicle

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