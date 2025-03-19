package org.example.database

import org.example.models.Localitzacio
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ActivitatTable : Table("activitats") {
    val id_activitat = integer("id_activitat").autoIncrement()
    val nom = varchar("nom", 100)
    var latitud = float("latitud")
    var longitud = float("longitud")
    var dataInici = datetime("datainici")
    var dataFi = datetime("datafi")
    var descripcio = varchar("descripcio", 255)
    var username_creador = varchar("username_creador", 100)

    override val primaryKey = PrimaryKey(id_activitat, name = "PK_id_activitat")
}