package ru.planner.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time

enum class SlotType { INDIVIDUAL, GROUP }

object Slots : Table("slots") {
    val id = uuid("id").clientDefault { java.util.UUID.randomUUID() }
    val weekTemplateId = uuid("week_template_id").references(WeekTemplates.id, onDelete = ReferenceOption.CASCADE)
    val venueId = uuid("venue_id").references(Venues.id)
    val slotDate = date("slot_date")
    val startTime = time("start_time")
    val durationMinutes = integer("duration_minutes").default(45)
    val slotType = enumerationByName<SlotType>("slot_type", 20)
    val capacity = integer("capacity").default(1)
    val bookingCount = integer("booking_count").default(0)

    override val primaryKey = PrimaryKey(id)
}
