package org.example.database

import org.example.enums.NivellTrofeu
import org.jetbrains.exposed.sql.Table

object TrofeuTable : Table("trofeus") {
    val id = integer("id").autoIncrement()
    val nom = varchar("nom", 100)
    val descripcio = text("descripcio")
    val nivell = customEnumeration(
        "nivell",
        "nivell_trofeu",
        { value -> NivellTrofeu.valueOf(value as String) },
        { it.name }
    )
    val experiencia = integer("experiencia").check { it greaterEq 0 }
    val imatge = varchar("imatge", 255).nullable()

    override val primaryKey = PrimaryKey(id, name = "PK_Trofeus_Id")
    init {
        uniqueIndex("uq_nom_nivell", nom, nivell)
    }
}