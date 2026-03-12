package ru.planner.models

import kotlinx.serialization.Serializable

sealed class PlannerException(message: String) : Exception(message)

class NotFoundException(message: String = "Not found") : PlannerException(message)
class SlotFullException : PlannerException("No available capacity in this slot")
class AlreadyBookedException : PlannerException("You have already booked this slot")
class ConflictException(message: String) : PlannerException(message)
class BadRequestException(message: String) : PlannerException(message)
class UnauthorizedException(message: String = "Invalid credentials") : PlannerException(message)

@Serializable
data class ErrorResponse(val code: String, val message: String?)
