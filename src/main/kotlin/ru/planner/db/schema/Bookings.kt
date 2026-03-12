package ru.planner.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

enum class BookingStatus { CONFIRMED, CANCELLED }

object Bookings : Table("bookings") {
    val id = uuid("id").clientDefault { java.util.UUID.randomUUID() }
    val slotId = uuid("slot_id").references(Slots.id)
    val studentId = uuid("student_id").references(Students.id)
    val status = enumerationByName<BookingStatus>("status", 20)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val cancelledAt = datetime("cancelled_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
