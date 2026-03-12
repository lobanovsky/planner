package ru.planner.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.planner.domain.StudentService
import ru.planner.models.requests.CreateStudentRequest
import ru.planner.models.requests.UpdateStudentRequest
import java.util.*

fun Route.studentRoutes(studentService: StudentService) {
    authenticate("trainer-jwt") {
        route("/students") {
            get {
                val trainerId = call.trainerId()
                call.respond(studentService.findAll(trainerId))
            }
            post {
                val trainerId = call.trainerId()
                val req = call.receive<CreateStudentRequest>()
                call.respond(HttpStatusCode.Created, studentService.create(trainerId, req))
            }
            get("/{id}") {
                val trainerId = call.trainerId()
                val studentId = UUID.fromString(call.parameters["id"]!!)
                call.respond(studentService.findById(studentId, trainerId))
            }
            put("/{id}") {
                val trainerId = call.trainerId()
                val studentId = UUID.fromString(call.parameters["id"]!!)
                val req = call.receive<UpdateStudentRequest>()
                call.respond(studentService.update(studentId, trainerId, req))
            }
            delete("/{id}") {
                val trainerId = call.trainerId()
                val studentId = UUID.fromString(call.parameters["id"]!!)
                studentService.delete(studentId, trainerId)
                call.respond(HttpStatusCode.NoContent)
            }
            post("/{id}/token/reset") {
                val trainerId = call.trainerId()
                val studentId = UUID.fromString(call.parameters["id"]!!)
                call.respond(studentService.resetToken(studentId, trainerId))
            }
        }
    }
}
