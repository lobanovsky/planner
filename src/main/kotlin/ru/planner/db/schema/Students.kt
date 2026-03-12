package ru.planner.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Students : Table("students") {
    val id = uuid("id").clientDefault { java.util.UUID.randomUUID() }
    val trainerId = uuid("trainer_id").references(Trainers.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val telegramId = long("telegram_id").nullable()
    val token = varchar("token", 64).uniqueIndex()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}
