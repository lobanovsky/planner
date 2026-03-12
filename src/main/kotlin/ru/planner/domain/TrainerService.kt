package ru.planner.domain

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.planner.db.schema.Trainers
import ru.planner.models.BadRequestException
import ru.planner.models.ConflictException
import ru.planner.models.NotFoundException
import ru.planner.models.UnauthorizedException
import ru.planner.models.requests.LoginRequest
import ru.planner.models.requests.RegisterTrainerRequest
import ru.planner.models.requests.UpdateTrainerRequest
import ru.planner.models.responses.TrainerResponse
import ru.planner.security.PasswordService
import java.util.*

class TrainerService(private val database: Database) {

    suspend fun register(req: RegisterTrainerRequest): TrainerResponse {
        if (req.name.isBlank()) throw BadRequestException("Name is required")
        if (req.email.isBlank()) throw BadRequestException("Email is required")
        if (req.password.length < 6) throw BadRequestException("Password must be at least 6 characters")

        return dbQuery(database) {
            val exists = Trainers.selectAll().where { Trainers.email eq req.email.trim().lowercase() }.count() > 0
            if (exists) throw ConflictException("Email already registered")

            val id = Trainers.insert {
                it[name] = req.name.trim()
                it[email] = req.email.trim().lowercase()
                it[passwordHash] = PasswordService.hash(req.password)
            }[Trainers.id]

            TrainerResponse(id.toString(), req.name.trim(), req.email.trim().lowercase())
        }
    }

    suspend fun login(req: LoginRequest): TrainerResponse {
        return dbQuery(database) {
            val row = Trainers.selectAll()
                .where { Trainers.email eq req.email.trim().lowercase() }
                .singleOrNull()
                ?: throw UnauthorizedException()

            if (!PasswordService.verify(req.password, row[Trainers.passwordHash])) {
                throw UnauthorizedException()
            }
            TrainerResponse(
                id = row[Trainers.id].toString(),
                name = row[Trainers.name],
                email = row[Trainers.email]
            )
        }
    }

    suspend fun findById(trainerId: UUID): TrainerResponse {
        return dbQuery(database) {
            Trainers.selectAll()
                .where { Trainers.id eq trainerId }
                .singleOrNull()
                ?.let { TrainerResponse(it[Trainers.id].toString(), it[Trainers.name], it[Trainers.email]) }
                ?: throw NotFoundException("Trainer not found")
        }
    }

    suspend fun update(trainerId: UUID, req: UpdateTrainerRequest): TrainerResponse {
        return dbQuery(database) {
            Trainers.selectAll().where { Trainers.id eq trainerId }.singleOrNull()
                ?: throw NotFoundException("Trainer not found")

            Trainers.update({ Trainers.id eq trainerId }) {
                req.name?.let { n -> it[name] = n.trim() }
                req.email?.let { e -> it[email] = e.trim().lowercase() }
                req.password?.let { p -> it[passwordHash] = PasswordService.hash(p) }
            }

            val updated = Trainers.selectAll().where { Trainers.id eq trainerId }.single()
            TrainerResponse(updated[Trainers.id].toString(), updated[Trainers.name], updated[Trainers.email])
        }
    }
}
