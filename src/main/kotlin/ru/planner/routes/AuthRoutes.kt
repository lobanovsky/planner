package ru.planner.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.planner.domain.TrainerService
import ru.planner.models.requests.LoginRequest
import ru.planner.models.requests.RegisterTrainerRequest
import ru.planner.models.responses.AuthResponse
import ru.planner.security.JwtService
import java.util.*

fun Route.authRoutes(trainerService: TrainerService, jwtService: JwtService) {
    route("/auth/trainer") {
        post("/register") {
            val req = call.receive<RegisterTrainerRequest>()
            val trainer = trainerService.register(req)
            val token = jwtService.generateToken(UUID.fromString(trainer.id), trainer.email)
            call.respond(HttpStatusCode.Created, AuthResponse(token, trainer))
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val trainer = trainerService.login(req)
            val token = jwtService.generateToken(UUID.fromString(trainer.id), trainer.email)
            call.respond(HttpStatusCode.OK, AuthResponse(token, trainer))
        }
    }
}
