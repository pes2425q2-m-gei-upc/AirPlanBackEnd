package org.example.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import ControladorValoracio
import ValoracioRepository

fun Route.valoracioRoutes() {
    val controller = ControladorValoracio(ValoracioRepository())

    route("/valoracions") {
        post {
            controller.afegirValoracio(call)
        }

        get("/usuari/{username}") {
            controller.valoracionsPerUsuari(call)
        }

        get("/activitat/{idActivitat}") {
            controller.valoracionsPerActivitat(call)
        }
    }
}
