package ru.planner.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Venues : Table("venues") {
    val id = uuid("id").clientDefault { java.util.UUID.randomUUID() }
    val trainerId = uuid("trainer_id").references(Trainers.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}
