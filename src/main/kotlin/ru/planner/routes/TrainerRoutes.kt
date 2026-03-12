package ru.planner.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.planner.domain.TrainerService
import ru.planner.models.requests.UpdateTrainerRequest
import java.util.*

fun Route.trainerRoutes(trainerService: TrainerService) {
    authenticate("trainer-jwt") {
        route("/trainer/me") {
            get {
                val trainerId = call.trainerId()
                call.respond(trainerService.findById(trainerId))
            }
            put {
                val trainerId = call.trainerId()
                val req = call.receive<UpdateTrainerRequest>()
                call.respond(trainerService.update(trainerId, req))
            }
        }
    }
}

fun RoutingCall.trainerId(): UUID {
    val principal = principal<JWTPrincipal>()!!
    return UUID.fromString(principal.payload.getClaim("trainer_id").asString())
}
