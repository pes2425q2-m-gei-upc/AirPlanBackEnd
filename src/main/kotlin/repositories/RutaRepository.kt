package org.example.repositories

import org.example.models.Ruta
import org.example.database.RutaTable
import org.example.enums.TipusVehicle
import org.example.models.Localitzacio
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class RutaRepository {
    fun afegirRuta(ruta: Ruta): Ruta {
        return transaction {
            val statement = RutaTable.insert {
                it[duracioMin] = ruta.duracioMin
                it[duracioMax] = ruta.duracioMax
                it[tipusVehicle] = ruta.tipusVehicle.toString()
                it[latitudOrigen] = ruta.origen.latitud
                it[longitudOrigen] = ruta.origen.longitud
                it[latitudDesti] = ruta.desti.latitud
                it[longitudDesti] = ruta.desti.longitud
                it[dataRuta] = ruta.data
                it[clientUsername] = ruta.clientUsername
            }
            val generatedId = statement[RutaTable.id]
            RutaTable.select { RutaTable.id eq generatedId }.map { row ->
                Ruta(
                    origen = Localitzacio(
                        latitud = row[RutaTable.latitudOrigen],
                        longitud = row[RutaTable.longitudOrigen]
                    ),
                    desti = Localitzacio(
                        latitud = row[RutaTable.latitudDesti],
                        longitud = row[RutaTable.longitudDesti]
                    ),
                    clientUsername = row[RutaTable.clientUsername],
                    data = row[RutaTable.dataRuta],
                    id = row[RutaTable.id],
                    duracioMin = row[RutaTable.duracioMin],
                    duracioMax = row[RutaTable.duracioMax],
                    tipusVehicle = TipusVehicle.valueOf(row[RutaTable.tipusVehicle])
                )
            }.firstOrNull()!!
        }
    }

    fun getRutaPerId(id: Int): Ruta {
        return transaction {
            RutaTable.select { RutaTable.id eq id }.map { row ->
                Ruta(
                    origen = Localitzacio(
                        latitud = row[RutaTable.latitudOrigen],
                        longitud = row[RutaTable.longitudOrigen]
                    ),
                    desti = Localitzacio(
                        latitud = row[RutaTable.latitudDesti],
                        longitud = row[RutaTable.longitudDesti]
                    ),
                    clientUsername = row[RutaTable.clientUsername],
                    data = row[RutaTable.dataRuta],
                    id = row[RutaTable.id],
                    duracioMin = row[RutaTable.duracioMin],
                    duracioMax = row[RutaTable.duracioMax],
                    tipusVehicle = row[RutaTable.tipusVehicle].let { TipusVehicle.valueOf(it) }
                )
            }.firstOrNull()
        } ?: throw Exception("Ruta no trobada")
    }

    fun obtenirRutes(): List<Ruta> {
        return transaction {
            RutaTable.selectAll().map { row ->
                Ruta(
                    origen = Localitzacio(
                        latitud = row[RutaTable.latitudOrigen],
                        longitud = row[RutaTable.longitudOrigen]
                    ),
                    desti = Localitzacio(
                        latitud = row[RutaTable.latitudDesti],
                        longitud = row[RutaTable.longitudDesti]
                    ),
                    clientUsername = row[RutaTable.clientUsername],
                    data = row[RutaTable.dataRuta],
                    id = row[RutaTable.id],
                    duracioMin = row[RutaTable.duracioMin],
                    duracioMax = row[RutaTable.duracioMax],
                    tipusVehicle = TipusVehicle.valueOf(row[RutaTable.tipusVehicle])
                )
            }
        }
    }

    fun obtenirRutesClient(clientUsername: String): List<Ruta> {
        return transaction {
            RutaTable.select { RutaTable.clientUsername eq clientUsername }.map { row ->
                Ruta(
                    origen = Localitzacio(
                        latitud = row[RutaTable.latitudOrigen],
                        longitud = row[RutaTable.longitudOrigen]
                    ),
                    desti = Localitzacio(
                        latitud = row[RutaTable.latitudDesti],
                        longitud = row[RutaTable.longitudDesti]
                    ),
                    clientUsername = row[RutaTable.clientUsername],
                    data = row[RutaTable.dataRuta],
                    id = row[RutaTable.id],
                    duracioMin = row[RutaTable.duracioMin],
                    duracioMax = row[RutaTable.duracioMax],
                    tipusVehicle = TipusVehicle.valueOf(row[RutaTable.tipusVehicle])
                )
            }
        }
    }

    fun actualitzarRuta(ruta: Ruta) {
        try {
            transaction {
                val updatedRows = RutaTable.update({ RutaTable.id eq ruta.id }) {
                    it[duracioMin] = ruta.duracioMin
                    it[duracioMax] = ruta.duracioMax
                    it[tipusVehicle] = ruta.tipusVehicle.toString()
                    it[latitudOrigen] = ruta.origen.latitud
                    it[longitudOrigen] = ruta.origen.longitud
                    it[latitudDesti] = ruta.desti.latitud
                    it[longitudDesti] = ruta.desti.longitud
                    it[dataRuta] = ruta.data
                    it[clientUsername] = ruta.clientUsername
                }
                if (updatedRows == 0) {
                    throw Exception("No s'ha pogut actualitzar la ruta amb id ${ruta.id}")
                }
            }
        } catch (e: Exception) {
            println("${e.message}")
            false
        }
    }

    fun eliminarRuta(id: Int): Boolean {
        return transaction {
            val filasEliminades = RutaTable.deleteWhere { RutaTable.id eq id }
            filasEliminades > 0  // Retorna `true` si eliminó alguna ruta
        }
    }
}