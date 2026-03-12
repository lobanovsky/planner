package ru.planner.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

enum class TemplateStatus { DRAFT, PUBLISHED }

object WeekTemplates : Table("week_templates") {
    val id = uuid("id").clientDefault { java.util.UUID.randomUUID() }
    val trainerId = uuid("trainer_id").references(Trainers.id, onDelete = ReferenceOption.CASCADE)
    val weekStart = date("week_start")
    val status = enumerationByName<TemplateStatus>("status", 20)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}
