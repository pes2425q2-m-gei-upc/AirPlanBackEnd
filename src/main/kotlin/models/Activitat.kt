package org.example.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Timestamp

@Serializable
class Activitat(
    var id: Int,
    var nom: String,
    var descripcio: String,
    @Contextual var ubicacio: Localitzacio,
    var dataInici: LocalDateTime,
    var dataFi: LocalDateTime,
    var creador: String,
    var imatge: String
) {

    fun modificarActivitat(
        nom: String,
        descripcio: String,
        ubicacio: Localitzacio,
        dataInici: Timestamp,
        dataFi: Timestamp,
        imatge: String
    ) {
        this.nom = nom
        this.descripcio = descripcio
        this.ubicacio = ubicacio
        this.dataInici = dataInici.toLocalDateTime().toKotlinLocalDateTime()
        this.dataFi = dataFi.toLocalDateTime().toKotlinLocalDateTime()
        this.imatge = imatge
    }

    fun eliminarActivitat() {
        // Eliminar de la base de dades
    }
}