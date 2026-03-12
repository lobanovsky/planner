package ru.planner.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.planner.domain.VenueService
import ru.planner.models.requests.CreateVenueRequest
import ru.planner.models.requests.UpdateVenueRequest
import java.util.*

fun Route.venueRoutes(venueService: VenueService) {
    authenticate("trainer-jwt") {
        route("/venues") {
            get {
                val trainerId = call.trainerId()
                call.respond(venueService.findAll(trainerId))
            }
            post {
                val trainerId = call.trainerId()
                val req = call.receive<CreateVenueRequest>()
                call.respond(HttpStatusCode.Created, venueService.create(trainerId, req))
            }
            put("/{id}") {
                val trainerId = call.trainerId()
                val venueId = UUID.fromString(call.parameters["id"]!!)
                val req = call.receive<UpdateVenueRequest>()
                call.respond(venueService.update(venueId, trainerId, req))
            }
            delete("/{id}") {
                val trainerId = call.trainerId()
                val venueId = UUID.fromString(call.parameters["id"]!!)
                venueService.delete(venueId, trainerId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
