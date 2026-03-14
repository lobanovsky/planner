package ru.planner.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import ru.planner.domain.ScheduleService
import ru.planner.models.requests.CreateSlotRequest
import ru.planner.models.requests.CreateWeekTemplateRequest
import ru.planner.models.responses.ScheduleDetailResponse
import ru.planner.security.StudentPrincipal
import java.util.*

fun Route.scheduleRoutes(scheduleService: ScheduleService) {

    // ─── Trainer routes ────────────────────────────────────────────────────────
    authenticate("trainer-jwt") {
        route("/schedules") {
            post {
                val trainerId = call.trainerId()
                val req = call.receive<CreateWeekTemplateRequest>()
                call.respond(HttpStatusCode.Created, scheduleService.createTemplate(trainerId, req))
            }
            get {
                val trainerId = call.trainerId()
                val weekStart = call.request.queryParameters["weekStart"]
                call.respond(scheduleService.listTemplates(trainerId, weekStart))
            }
            get("/{templateId}") {
                val trainerId = call.trainerId()
                val templateId = UUID.fromString(call.parameters["templateId"]!!)
                val template = scheduleService.getTemplate(templateId, trainerId)
                val slots = scheduleService.getSlotsForTemplate(templateId, trainerId)
                call.respond(ScheduleDetailResponse(template, slots))
            }
            delete("/{templateId}") {
                val trainerId = call.trainerId()
                val templateId = UUID.fromString(call.parameters["templateId"]!!)
                scheduleService.deleteTemplate(templateId, trainerId)
                call.respond(HttpStatusCode.NoContent)
            }
            post("/{templateId}/publish") {
                val trainerId = call.trainerId()
                val templateId = UUID.fromString(call.parameters["templateId"]!!)
                val result = scheduleService.publishTemplate(templateId, trainerId)

                val botUrl = System.getenv("BOT_WEBHOOK_URL")
                if (!botUrl.isNullOrBlank()) {
                    val log = application.log
                    Thread {
                        try {
                            val ids = runBlocking { scheduleService.getStudentTelegramIds(trainerId) }
                            val payload = """{"weekStart":"${result.weekStart}","studentTelegramIds":$ids}"""
                            val req = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create("$botUrl/webhook/schedule-published"))
                                .header("Content-Type", "application/json")
                                .header("X-Bot-Secret", System.getenv("BOT_WEBHOOK_SECRET") ?: "")
                                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                                .build()
                            java.net.http.HttpClient.newHttpClient()
                                .send(req, java.net.http.HttpResponse.BodyHandlers.discarding())
                        } catch (e: Exception) {
                            log.warn("Failed to notify bot after publish", e)
                        }
                    }.start()
                }

                call.respond(result)
            }
            post("/{templateId}/slots") {
                val trainerId = call.trainerId()
                val templateId = UUID.fromString(call.parameters["templateId"]!!)
                val req = call.receive<CreateSlotRequest>()
                call.respond(HttpStatusCode.Created, scheduleService.createSlot(templateId, trainerId, req))
            }
            delete("/{templateId}/slots/{slotId}") {
                val trainerId = call.trainerId()
                val templateId = UUID.fromString(call.parameters["templateId"]!!)
                val slotId = UUID.fromString(call.parameters["slotId"]!!)
                scheduleService.deleteSlot(slotId, templateId, trainerId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    // ─── Student routes ────────────────────────────────────────────────────────
    authenticate("student-token") {
        route("/schedules") {
            get("/current") {
                val principal = call.principal<StudentPrincipal>()!!
                call.respond(
                    scheduleService.getCurrentScheduleForStudent(principal.trainerId, principal.studentId)
                )
            }
            get("/week/{weekStart}") {
                val principal = call.principal<StudentPrincipal>()!!
                val weekStart = call.parameters["weekStart"]!!
                call.respond(
                    scheduleService.getWeekScheduleForStudent(principal.trainerId, principal.studentId, weekStart)
                )
            }
        }
    }
}
