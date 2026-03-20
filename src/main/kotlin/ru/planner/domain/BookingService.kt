package ru.planner.domain

import org.jetbrains.exposed.sql.*
import ru.planner.db.schema.*
import ru.planner.models.AlreadyBookedException
import ru.planner.models.BadRequestException
import ru.planner.models.NotFoundException
import ru.planner.models.SlotFullException
import ru.planner.models.responses.BookingResponse
import ru.planner.models.responses.SlotBookingEntryResponse
import ru.planner.models.responses.VenueResponse
import java.time.LocalDateTime
import java.util.*

class BookingService(private val database: Database) {

    suspend fun book(studentId: UUID, trainerId: UUID, slotIdStr: String): BookingResponse {
        val slotId = try {
            UUID.fromString(slotIdStr)
        } catch (_: Exception) {
            throw BadRequestException("Invalid slotId")
        }

        return dbQuery(database) {
            val slot = Slots
                .join(WeekTemplates, JoinType.INNER, Slots.weekTemplateId, WeekTemplates.id)
                .join(Venues, JoinType.INNER, Slots.venueId, Venues.id)
                .selectAll()
                .where {
                    (Slots.id eq slotId) and
                            (WeekTemplates.trainerId eq trainerId) and
                            (WeekTemplates.status eq TemplateStatus.PUBLISHED)
                }
                .singleOrNull() ?: throw NotFoundException("Slot not found in published schedule")

            val alreadyBooked = Bookings.selectAll().where {
                (Bookings.slotId eq slotId) and
                        (Bookings.studentId eq studentId) and
                        (Bookings.status eq BookingStatus.CONFIRMED)
            }.count() > 0
            if (alreadyBooked) throw AlreadyBookedException()

            // Atomic: increment only if capacity available
            val updated = Slots.update({
                (Slots.id eq slotId) and (Slots.bookingCount less Slots.capacity)
            }) {
                with(SqlExpressionBuilder) {
                    it[Slots.bookingCount] = Slots.bookingCount + 1
                }
            }
            if (updated == 0) throw SlotFullException()

            val now = LocalDateTime.now()
            val bookingId = Bookings.insert {
                it[Bookings.slotId] = slotId
                it[Bookings.studentId] = studentId
                it[status] = BookingStatus.CONFIRMED
                it[createdAt] = now
            }[Bookings.id]

            BookingResponse(
                id = bookingId.toString(),
                slotId = slotId.toString(),
                venue = VenueResponse(
                    id = slot[Venues.id].toString(),
                    trainerId = trainerId.toString(),
                    name = slot[Venues.name],
                    description = slot[Venues.description]
                ),
                slotDate = slot[Slots.slotDate].toString(),
                startTime = slot[Slots.startTime].toString(),
                durationMinutes = slot[Slots.durationMinutes],
                status = "CONFIRMED",
                createdAt = now.toString()
            )
        }
    }

    suspend fun getMyBookings(studentId: UUID, trainerId: UUID): List<BookingResponse> = dbQuery(database) {
        Bookings
            .join(Slots, JoinType.INNER, Bookings.slotId, Slots.id)
            .join(Venues, JoinType.INNER, Slots.venueId, Venues.id)
            .join(WeekTemplates, JoinType.INNER, Slots.weekTemplateId, WeekTemplates.id)
            .selectAll()
            .where {
                (Bookings.studentId eq studentId) and
                        (Bookings.status eq BookingStatus.CONFIRMED) and
                        (WeekTemplates.trainerId eq trainerId)
            }
            .orderBy(Slots.slotDate to SortOrder.ASC, Slots.startTime to SortOrder.ASC)
            .map { row ->
                BookingResponse(
                    id = row[Bookings.id].toString(),
                    slotId = row[Slots.id].toString(),
                    venue = VenueResponse(
                        id = row[Venues.id].toString(),
                        trainerId = trainerId.toString(),
                        name = row[Venues.name],
                        description = row[Venues.description]
                    ),
                    slotDate = row[Slots.slotDate].toString(),
                    startTime = row[Slots.startTime].toString(),
                    durationMinutes = row[Slots.durationMinutes],
                    status = row[Bookings.status].name,
                    createdAt = row[Bookings.createdAt].toString()
                )
            }
    }

    suspend fun getSlotBookings(slotIdStr: String, trainerId: UUID): List<SlotBookingEntryResponse> {
        val slotId = try {
            UUID.fromString(slotIdStr)
        } catch (_: Exception) {
            throw BadRequestException("Invalid slotId")
        }
        return dbQuery(database) {
            val slotExists = Slots
                .join(WeekTemplates, JoinType.INNER, Slots.weekTemplateId, WeekTemplates.id)
                .selectAll()
                .where { (Slots.id eq slotId) and (WeekTemplates.trainerId eq trainerId) }
                .count() > 0
            if (!slotExists) throw NotFoundException("Slot not found")

            Bookings
                .join(Students, JoinType.INNER, Bookings.studentId, Students.id)
                .selectAll()
                .where {
                    (Bookings.slotId eq slotId) and
                            (Bookings.status eq BookingStatus.CONFIRMED)
                }
                .map {
                    SlotBookingEntryResponse(
                        bookingId = it[Bookings.id].toString(),
                        studentId = it[Students.id].toString(),
                        studentName = it[Students.name],
                        telegramId = it[Students.telegramId],
                        status = it[Bookings.status].name,
                        createdAt = it[Bookings.createdAt].toString()
                    )
                }
        }
    }

    suspend fun cancel(bookingId: UUID, studentId: UUID, trainerId: UUID) {
        dbQuery(database) {
            val booking = Bookings
                .join(Slots, JoinType.INNER, Bookings.slotId, Slots.id)
                .join(WeekTemplates, JoinType.INNER, Slots.weekTemplateId, WeekTemplates.id)
                .selectAll()
                .where {
                    (Bookings.id eq bookingId) and
                            (Bookings.studentId eq studentId) and
                            (Bookings.status eq BookingStatus.CONFIRMED) and
                            (WeekTemplates.trainerId eq trainerId)
                }
                .singleOrNull() ?: throw NotFoundException("Booking not found")

            val slotId = booking[Bookings.slotId]

            val updated = Bookings.update({
                (Bookings.id eq bookingId) and (Bookings.status eq BookingStatus.CONFIRMED)
            }) {
                it[status] = BookingStatus.CANCELLED
                it[cancelledAt] = LocalDateTime.now()
            }
            if (updated > 0) {
                Slots.update({ Slots.id eq slotId }) {
                    with(SqlExpressionBuilder) {
                        it[Slots.bookingCount] = Slots.bookingCount - 1
                    }
                }
            }
        }
    }
}
