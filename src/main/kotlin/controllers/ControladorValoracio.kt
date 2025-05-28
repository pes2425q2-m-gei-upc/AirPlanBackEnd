import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import org.example.models.ValoracioInput
import org.example.services.PerspectiveService

class ControladorValoracio(
    private val valoracioRepository: ValoracioRepository = ValoracioRepository(),
    private val perspectiveService: PerspectiveService = PerspectiveService()
) {
    suspend fun afegirValoracio(call: ApplicationCall) {
        val valoracio = call.receive<ValoracioInput>()
        // Filter comment through PerspectiveService
        valoracio.comentario?.let { comentario ->
            if (perspectiveService.analyzeMessage(comentario)) {
                call.respond(HttpStatusCode.BadRequest, "Comentari bloquejat per ser inapropiat")
                return
            }
        }
        val resultat = valoracioRepository.afegirValoracio(valoracio)

        if (resultat) {
            call.respond(HttpStatusCode.Created, "Valoració guardada correctament")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Error al guardar la valoració")
        }
    }

    suspend fun valoracionsPerUsuari(call: ApplicationCall) {
        val username = call.parameters["username"]
        if (username == null) {
            call.respond(HttpStatusCode.BadRequest, "Falta el nom d'usuari")
            return
        }
        val valoracions = valoracioRepository.obtenirValoracionsPerUsuari(username)
        call.respond(valoracions)
    }

    suspend fun valoracionsPerActivitat(call: ApplicationCall) {
        val id = call.parameters["idActivitat"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "Falta l'ID de l'activitat")
            return
        }
        val valoracions = valoracioRepository.obtenirValoracionsPerActivitat(id)
        call.respond(valoracions)
    }
}
