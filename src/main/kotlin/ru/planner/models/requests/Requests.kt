package ru.planner.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class RegisterTrainerRequest(val name: String, val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class UpdateTrainerRequest(val name: String? = null, val email: String? = null, val password: String? = null)

@Serializable
data class CreateVenueRequest(val name: String, val description: String? = null)

@Serializable
data class UpdateVenueRequest(val name: String? = null, val description: String? = null)

@Serializable
data class CreateStudentRequest(val name: String, val telegramId: Long? = null)

@Serializable
data class UpdateStudentRequest(val name: String? = null, val telegramId: Long? = null)

@Serializable
data class CreateWeekTemplateRequest(val weekStart: String) // ISO date "2026-03-16"

@Serializable
data class CreateSlotComponentRequest(
    val venueId: String,
    val startTime: String, // "HH:MM"
    val durationMinutes: Int,
    val sequence: Int
)

@Serializable
data class CreateSlotRequest(
    val venueId: String,
    val slotDate: String,        // ISO date
    val startTime: String,       // "HH:MM"
    val durationMinutes: Int = 45,
    val slotType: String = "INDIVIDUAL",
    val capacity: Int = 1,
    val components: List<CreateSlotComponentRequest>? = null
)

@Serializable
data class BookSlotRequest(val slotId: String)
