package ru.planner.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.time

object SlotComponents : Table("slot_components") {
    val id = uuid("id").clientDefault { java.util.UUID.randomUUID() }
    val slotId = uuid("slot_id").references(Slots.id, onDelete = ReferenceOption.CASCADE)
    val venueId = uuid("venue_id").references(Venues.id)
    val startTime = time("start_time")
    val durationMinutes = integer("duration_minutes")
    val sequence = integer("sequence")

    override val primaryKey = PrimaryKey(id)
}
