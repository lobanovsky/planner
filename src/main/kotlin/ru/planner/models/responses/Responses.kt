package ru.planner.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class TrainerResponse(val id: String, val name: String, val email: String)

@Serializable
data class AuthResponse(val token: String, val trainer: TrainerResponse)

@Serializable
data class VenueResponse(
    val id: String,
    val trainerId: String,
    val name: String,
    val description: String?
)

@Serializable
data class StudentResponse(
    val id: String,
    val trainerId: String,
    val name: String,
    val telegramId: Long?,
    val token: String
)

@Serializable
data class TokenResponse(val token: String)

@Serializable
data class WeekTemplateResponse(
    val id: String,
    val trainerId: String,
    val weekStart: String,
    val status: String
)

@Serializable
data class SlotComponentResponse(
    val id: String,
    val venue: VenueResponse,
    val startTime: String,
    val durationMinutes: Int,
    val sequence: Int
)

@Serializable
data class SlotResponse(
    val id: String,
    val weekTemplateId: String,
    val venue: VenueResponse,
    val slotDate: String,
    val startTime: String,
    val durationMinutes: Int,
    val slotType: String,
    val capacity: Int,
    val bookingCount: Int,
    val components: List<SlotComponentResponse>
)

@Serializable
data class PublicSlotResponse(
    val id: String,
    val venue: VenueResponse,
    val slotDate: String,
    val startTime: String,
    val durationMinutes: Int,
    val slotType: String,
    val capacity: Int,
    val availableCount: Int,
    val status: String, // FREE | OCCUPIED | BOOKED_BY_ME
    val components: List<SlotComponentResponse>
)

@Serializable
data class WeekScheduleResponse(
    val weekStart: String,
    val templateId: String,
    val slots: List<PublicSlotResponse>
)

@Serializable
data class BookingResponse(
    val id: String,
    val slotId: String,
    val venue: VenueResponse,
    val slotDate: String,
    val startTime: String,
    val durationMinutes: Int,
    val status: String,
    val createdAt: String
)
