package org.example.database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object RutaTable : Table("ruta") {
    val id = integer("id").autoIncrement()
    val duracioMin = integer("duraciomin")
    val duracioMax = integer("duraciomax")
    val tipusVehicle = varchar("tipusvehicle", 20)
    var latitudOrigen = float("latitudorigen")
    var longitudOrigen = float("longitudorigen")
    var latitudDesti = float("latituddesti")
    var longitudDesti = float("longituddesti")
    var clientUsername = varchar("clientusername", 100)
    var dataRuta = datetime("dataruta")

    override val primaryKey = PrimaryKey(id, name = "PK_id_ruta")

}