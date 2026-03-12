package ru.planner.domain

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import ru.planner.db.schema.*
import ru.planner.models.BadRequestException
import ru.planner.models.ConflictException
import ru.planner.models.NotFoundException
import ru.planner.models.requests.CreateSlotRequest
import ru.planner.models.requests.CreateWeekTemplateRequest
import ru.planner.models.responses.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.util.*

class ScheduleService(private val database: Database) {

    // ─── Week Templates ───────────────────────────────────────────────────────

    suspend fun createTemplate(trainerId: UUID, req: CreateWeekTemplateRequest): WeekTemplateResponse {
        val weekStart = parseDate(req.weekStart)
        if (weekStart.dayOfWeek != DayOfWeek.MONDAY) throw BadRequestException("weekStart must be a Monday")

        return dbQuery(database) {
            val conflict = WeekTemplates.selectAll().where {
                (WeekTemplates.trainerId eq trainerId) and
                (WeekTemplates.weekStart eq weekStart) and
                (WeekTemplates.status eq TemplateStatus.PUBLISHED)
            }.count() > 0
            if (conflict) throw ConflictException("A published schedule for this week already exists")

            val id = WeekTemplates.insert {
                it[WeekTemplates.trainerId] = trainerId
                it[WeekTemplates.weekStart] = weekStart
                it[status] = TemplateStatus.DRAFT
            }[WeekTemplates.id]

            WeekTemplateResponse(id.toString(), trainerId.toString(), weekStart.toString(), "DRAFT")
        }
    }

    suspend fun listTemplates(trainerId: UUID, weekStart: String?): List<WeekTemplateResponse> = dbQuery(database) {
        val query = WeekTemplates.selectAll().where { WeekTemplates.trainerId eq trainerId }
        if (weekStart != null) {
            val date = parseDate(weekStart)
            query.andWhere { WeekTemplates.weekStart eq date }
        }
        query.orderBy(WeekTemplates.weekStart, SortOrder.DESC).map { it.toTemplateResponse() }
    }

    suspend fun getTemplate(templateId: UUID, trainerId: UUID): WeekTemplateResponse = dbQuery(database) {
        WeekTemplates.selectAll()
            .where { (WeekTemplates.id eq templateId) and (WeekTemplates.trainerId eq trainerId) }
            .singleOrNull()
            ?.toTemplateResponse()
            ?: throw NotFoundException("Schedule not found")
    }

    suspend fun publishTemplate(templateId: UUID, trainerId: UUID): WeekTemplateResponse {
        return dbQuery(database) {
            val row = WeekTemplates.selectAll()
                .where { (WeekTemplates.id eq templateId) and (WeekTemplates.trainerId eq trainerId) }
                .singleOrNull() ?: throw NotFoundException("Schedule not found")

            if (row[WeekTemplates.status] == TemplateStatus.PUBLISHED)
                throw ConflictException("Schedule is already published")

            WeekTemplates.update({ WeekTemplates.id eq templateId }) {
                it[status] = TemplateStatus.PUBLISHED
            }
            WeekTemplates.selectAll().where { WeekTemplates.id eq templateId }.single().toTemplateResponse()
        }
    }

    suspend fun deleteTemplate(templateId: UUID, trainerId: UUID) {
        dbQuery(database) {
            val row = WeekTemplates.selectAll()
                .where { (WeekTemplates.id eq templateId) and (WeekTemplates.trainerId eq trainerId) }
                .singleOrNull() ?: throw NotFoundException("Schedule not found")

            if (row[WeekTemplates.status] == TemplateStatus.PUBLISHED)
                throw ConflictException("Cannot delete a published schedule")

            WeekTemplates.deleteWhere { WeekTemplates.id eq templateId }
        }
    }

    // ─── Slots ────────────────────────────────────────────────────────────────

    suspend fun createSlot(templateId: UUID, trainerId: UUID, req: CreateSlotRequest): SlotResponse {
        val slotDate = parseDate(req.slotDate)
        val startTime = parseTime(req.startTime)
        val slotType = try { SlotType.valueOf(req.slotType) } catch (e: Exception) {
            throw BadRequestException("Invalid slotType: must be INDIVIDUAL or GROUP")
        }
        val venueId = parseUuid(req.venueId)

        return dbQuery(database) {
            val template = WeekTemplates.selectAll()
                .where { (WeekTemplates.id eq templateId) and (WeekTemplates.trainerId eq trainerId) }
                .singleOrNull() ?: throw NotFoundException("Schedule not found")

            if (template[WeekTemplates.status] == TemplateStatus.PUBLISHED)
                throw ConflictException("Cannot add slots to a published schedule")

            val venue = Venues.selectAll()
                .where { (Venues.id eq venueId) and (Venues.trainerId eq trainerId) }
                .singleOrNull() ?: throw NotFoundException("Venue not found")

            val slotId = Slots.insert {
                it[Slots.weekTemplateId] = templateId
                it[Slots.venueId] = venueId
                it[Slots.slotDate] = slotDate
                it[Slots.startTime] = startTime
                it[Slots.durationMinutes] = req.durationMinutes
                it[Slots.slotType] = slotType
                it[Slots.capacity] = req.capacity
                it[Slots.bookingCount] = 0
            }[Slots.id]

            val components = if (slotType == SlotType.GROUP && !req.components.isNullOrEmpty()) {
                req.components.map { comp ->
                    val compVenueId = parseUuid(comp.venueId)
                    val compVenue = Venues.selectAll()
                        .where { (Venues.id eq compVenueId) and (Venues.trainerId eq trainerId) }
                        .singleOrNull() ?: throw NotFoundException("Component venue not found")

                    val compId = SlotComponents.insert {
                        it[SlotComponents.slotId] = slotId
                        it[SlotComponents.venueId] = compVenueId
                        it[SlotComponents.startTime] = parseTime(comp.startTime)
                        it[SlotComponents.durationMinutes] = comp.durationMinutes
                        it[SlotComponents.sequence] = comp.sequence
                    }[SlotComponents.id]

                    SlotComponentResponse(
                        id = compId.toString(),
                        venue = compVenue.toVenueResponse(trainerId),
                        startTime = comp.startTime,
                        durationMinutes = comp.durationMinutes,
                        sequence = comp.sequence
                    )
                }
            } else emptyList()

            SlotResponse(
                id = slotId.toString(),
                weekTemplateId = templateId.toString(),
                venue = venue.toVenueResponse(trainerId),
                slotDate = slotDate.toString(),
                startTime = startTime.toString(),
                durationMinutes = req.durationMinutes,
                slotType = slotType.name,
                capacity = req.capacity,
                bookingCount = 0,
                components = components
            )
        }
    }

    suspend fun deleteSlot(slotId: UUID, templateId: UUID, trainerId: UUID) {
        dbQuery(database) {
            WeekTemplates.selectAll()
                .where { (WeekTemplates.id eq templateId) and (WeekTemplates.trainerId eq trainerId) }
                .singleOrNull() ?: throw NotFoundException("Schedule not found")

            val slot = Slots.selectAll()
                .where { (Slots.id eq slotId) and (Slots.weekTemplateId eq templateId) }
                .singleOrNull() ?: throw NotFoundException("Slot not found")

            if (slot[Slots.bookingCount] > 0)
                throw ConflictException("Cannot delete a slot with confirmed bookings")

            Slots.deleteWhere { Slots.id eq slotId }
        }
    }

    // ─── Student-facing schedule ──────────────────────────────────────────────

    suspend fun getCurrentScheduleForStudent(trainerId: UUID, studentId: UUID): WeekScheduleResponse {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return getScheduleForStudent(trainerId, studentId, weekStart)
    }

    suspend fun getWeekScheduleForStudent(trainerId: UUID, studentId: UUID, weekStartStr: String): WeekScheduleResponse {
        val weekStart = parseDate(weekStartStr)
        return getScheduleForStudent(trainerId, studentId, weekStart)
    }

    private suspend fun getScheduleForStudent(
        trainerId: UUID,
        studentId: UUID,
        weekStart: LocalDate
    ): WeekScheduleResponse = dbQuery(database) {
        val template = WeekTemplates.selectAll().where {
            (WeekTemplates.trainerId eq trainerId) and
            (WeekTemplates.weekStart eq weekStart) and
            (WeekTemplates.status eq TemplateStatus.PUBLISHED)
        }.singleOrNull() ?: throw NotFoundException("No published schedule for this week")

        val templateId = template[WeekTemplates.id]

        val slots = Slots.join(Venues, JoinType.INNER, Slots.venueId, Venues.id)
            .selectAll()
            .where { Slots.weekTemplateId eq templateId }
            .orderBy(Slots.slotDate to SortOrder.ASC, Slots.startTime to SortOrder.ASC)
            .toList()

        if (slots.isEmpty()) return@dbQuery WeekScheduleResponse(
            weekStart = weekStart.toString(),
            templateId = templateId.toString(),
            slots = emptyList()
        )

        val slotIds = slots.map { it[Slots.id] }

        val componentsMap: Map<UUID, List<SlotComponentResponse>> = SlotComponents
            .join(Venues, JoinType.INNER, SlotComponents.venueId, Venues.id)
            .selectAll()
            .where { SlotComponents.slotId inList slotIds }
            .orderBy(SlotComponents.sequence to SortOrder.ASC)
            .groupBy({ it[SlotComponents.slotId] }) { row ->
                SlotComponentResponse(
                    id = row[SlotComponents.id].toString(),
                    venue = VenueResponse(
                        id = row[Venues.id].toString(),
                        trainerId = trainerId.toString(),
                        name = row[Venues.name],
                        description = row[Venues.description]
                    ),
                    startTime = row[SlotComponents.startTime].toString(),
                    durationMinutes = row[SlotComponents.durationMinutes],
                    sequence = row[SlotComponents.sequence]
                )
            }

        val bookedSlotIds: Set<UUID> = Bookings.selectAll().where {
            (Bookings.studentId eq studentId) and
            (Bookings.status eq BookingStatus.CONFIRMED) and
            (Bookings.slotId inList slotIds)
        }.map { it[Bookings.slotId] }.toSet()

        val publicSlots = slots.map { row ->
            val slotId = row[Slots.id]
            val capacity = row[Slots.capacity]
            val bookingCount = row[Slots.bookingCount]
            val isBookedByMe = slotId in bookedSlotIds

            val status = when {
                isBookedByMe -> "BOOKED_BY_ME"
                bookingCount >= capacity -> "OCCUPIED"
                else -> "FREE"
            }

            PublicSlotResponse(
                id = slotId.toString(),
                venue = VenueResponse(
                    id = row[Venues.id].toString(),
                    trainerId = trainerId.toString(),
                    name = row[Venues.name],
                    description = row[Venues.description]
                ),
                slotDate = row[Slots.slotDate].toString(),
                startTime = row[Slots.startTime].toString(),
                durationMinutes = row[Slots.durationMinutes],
                slotType = row[Slots.slotType].name,
                capacity = capacity,
                availableCount = (capacity - bookingCount).coerceAtLeast(0),
                status = status,
                components = componentsMap[slotId] ?: emptyList()
            )
        }

        WeekScheduleResponse(
            weekStart = weekStart.toString(),
            templateId = templateId.toString(),
            slots = publicSlots
        )
    }

    // ─── Trainer view of slots ────────────────────────────────────────────────

    suspend fun getSlotsForTemplate(templateId: UUID, trainerId: UUID): List<SlotResponse> = dbQuery(database) {
        WeekTemplates.selectAll()
            .where { (WeekTemplates.id eq templateId) and (WeekTemplates.trainerId eq trainerId) }
            .singleOrNull() ?: throw NotFoundException("Schedule not found")

        val slots = Slots.join(Venues, JoinType.INNER, Slots.venueId, Venues.id)
            .selectAll()
            .where { Slots.weekTemplateId eq templateId }
            .orderBy(Slots.slotDate to SortOrder.ASC, Slots.startTime to SortOrder.ASC)
            .toList()

        val slotIds = slots.map { it[Slots.id] }
        val componentsMap: Map<UUID, List<SlotComponentResponse>> = if (slotIds.isNotEmpty()) {
            SlotComponents.join(Venues, JoinType.INNER, SlotComponents.venueId, Venues.id)
                .selectAll()
                .where { SlotComponents.slotId inList slotIds }
                .orderBy(SlotComponents.sequence to SortOrder.ASC)
                .groupBy({ it[SlotComponents.slotId] }) { row ->
                    SlotComponentResponse(
                        id = row[SlotComponents.id].toString(),
                        venue = row.toVenueResponse(trainerId),
                        startTime = row[SlotComponents.startTime].toString(),
                        durationMinutes = row[SlotComponents.durationMinutes],
                        sequence = row[SlotComponents.sequence]
                    )
                }
        } else emptyMap()

        slots.map { row ->
            val slotId = row[Slots.id]
            SlotResponse(
                id = slotId.toString(),
                weekTemplateId = templateId.toString(),
                venue = row.toVenueResponse(trainerId),
                slotDate = row[Slots.slotDate].toString(),
                startTime = row[Slots.startTime].toString(),
                durationMinutes = row[Slots.durationMinutes],
                slotType = row[Slots.slotType].name,
                capacity = row[Slots.capacity],
                bookingCount = row[Slots.bookingCount],
                components = componentsMap[slotId] ?: emptyList()
            )
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun parseDate(str: String): LocalDate = try {
        LocalDate.parse(str)
    } catch (e: DateTimeParseException) {
        throw BadRequestException("Invalid date format: '$str'. Expected YYYY-MM-DD")
    }

    private fun parseTime(str: String): LocalTime = try {
        LocalTime.parse(str)
    } catch (e: DateTimeParseException) {
        throw BadRequestException("Invalid time format: '$str'. Expected HH:MM or HH:MM:SS")
    }

    private fun parseUuid(str: String): UUID = try {
        UUID.fromString(str)
    } catch (e: IllegalArgumentException) {
        throw BadRequestException("Invalid UUID: '$str'")
    }

    private fun ResultRow.toTemplateResponse() = WeekTemplateResponse(
        id = this[WeekTemplates.id].toString(),
        trainerId = this[WeekTemplates.trainerId].toString(),
        weekStart = this[WeekTemplates.weekStart].toString(),
        status = this[WeekTemplates.status].name
    )

    private fun ResultRow.toVenueResponse(trainerId: UUID) = VenueResponse(
        id = this[Venues.id].toString(),
        trainerId = trainerId.toString(),
        name = this[Venues.name],
        description = this[Venues.description]
    )
}
