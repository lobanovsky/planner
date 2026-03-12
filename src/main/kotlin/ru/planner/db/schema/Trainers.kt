package ru.planner.db.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Trainers : Table("trainers") {
    val id = uuid("id").clientDefault { java.util.UUID.randomUUID() }
    val name = varchar("name", 100)
    val email = varchar("email", 200).uniqueIndex()
    val passwordHash = varchar("password_hash", 200)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}
