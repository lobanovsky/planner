package ru.planner.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.planner.db.DatabaseKey
import ru.planner.domain.*
import ru.planner.models.*
import ru.planner.security.JwtServiceKey

fun Application.configureRouting() {
    val database = attributes[DatabaseKey]
    val jwtService = attributes[JwtServiceKey]

    val trainerService = TrainerService(database)
    val venueService = VenueService(database)
    val studentService = StudentService(database)
    val scheduleService = ScheduleService(database)
    val bookingService = BookingService(database)

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    install(StatusPages) {
        exception<NotFoundException> { call, e ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", e.message))
        }
        exception<SlotFullException> { call, e ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("SLOT_FULL", e.message))
        }
        exception<AlreadyBookedException> { call, e ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("ALREADY_BOOKED", e.message))
        }
        exception<ConflictException> { call, e ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", e.message))
        }
        exception<BadRequestException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message))
        }
        exception<UnauthorizedException> { call, e ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", e.message))
        }
        exception<Throwable> { call, e ->
            call.application.log.error("Unhandled exception", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
        }
    }

    routing {
        get("/") {
            call.respondText("Planner API is running")
        }
        route("/api") {
            authRoutes(trainerService, jwtService)
            trainerRoutes(trainerService)
            venueRoutes(venueService)
            studentRoutes(studentService)
            scheduleRoutes(scheduleService)
            bookingRoutes(bookingService)
        }
    }
}
