package ru.planner.db

import io.ktor.server.application.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ru.planner.db.schema.*

val DatabaseKey = AttributeKey<Database>("PlannerDatabase")

fun Application.configureDatabases() {
    val url = environment.config.property("postgres.url").getString()
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()

    val database = Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password
    )
    attributes.put(DatabaseKey, database)

    transaction(database) {
        SchemaUtils.create(
            Trainers, Venues, Students, WeekTemplates, Slots, SlotComponents, Bookings
        )
        // Partial unique index: one CONFIRMED booking per (slot, student)
        exec(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS bookings_slot_student_confirmed_idx
            ON bookings (slot_id, student_id)
            WHERE status = 'CONFIRMED'
            """.trimIndent()
        )
    }

    log.info("Database connected and schema created: $url")
}
