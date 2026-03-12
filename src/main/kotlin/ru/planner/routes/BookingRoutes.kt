package ru.planner.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.planner.domain.BookingService
import ru.planner.models.requests.BookSlotRequest
import ru.planner.security.StudentPrincipal
import java.util.*

fun Route.bookingRoutes(bookingService: BookingService) {
    authenticate("student-token") {
        route("/bookings") {
            post {
                val principal = call.principal<StudentPrincipal>()!!
                val req = call.receive<BookSlotRequest>()
                call.respond(
                    HttpStatusCode.Created,
                    bookingService.book(principal.studentId, principal.trainerId, req.slotId)
                )
            }
            get {
                val principal = call.principal<StudentPrincipal>()!!
                call.respond(bookingService.getMyBookings(principal.studentId, principal.trainerId))
            }
            delete("/{id}") {
                val principal = call.principal<StudentPrincipal>()!!
                val bookingId = UUID.fromString(call.parameters["id"]!!)
                bookingService.cancel(bookingId, principal.studentId, principal.trainerId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
