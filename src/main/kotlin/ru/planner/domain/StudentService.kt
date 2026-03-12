package ru.planner.domain

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import ru.planner.db.schema.Students
import ru.planner.models.BadRequestException
import ru.planner.models.NotFoundException
import ru.planner.models.requests.CreateStudentRequest
import ru.planner.models.requests.UpdateStudentRequest
import ru.planner.models.responses.StudentResponse
import ru.planner.models.responses.TokenResponse
import java.security.SecureRandom
import java.util.*

class StudentService(private val database: Database) {

    suspend fun create(trainerId: UUID, req: CreateStudentRequest): StudentResponse {
        if (req.name.isBlank()) throw BadRequestException("Student name is required")
        return dbQuery(database) {
            val token = generateToken()
            val id = Students.insert {
                it[Students.trainerId] = trainerId
                it[name] = req.name.trim()
                it[telegramId] = req.telegramId
                it[Students.token] = token
            }[Students.id]
            StudentResponse(id.toString(), trainerId.toString(), req.name.trim(), req.telegramId, token)
        }
    }

    suspend fun findAll(trainerId: UUID): List<StudentResponse> = dbQuery(database) {
        Students.selectAll()
            .where { Students.trainerId eq trainerId }
            .map { it.toResponse() }
    }

    suspend fun findById(studentId: UUID, trainerId: UUID): StudentResponse = dbQuery(database) {
        Students.selectAll()
            .where { (Students.id eq studentId) and (Students.trainerId eq trainerId) }
            .singleOrNull()
            ?.toResponse()
            ?: throw NotFoundException("Student not found")
    }

    suspend fun update(studentId: UUID, trainerId: UUID, req: UpdateStudentRequest): StudentResponse {
        return dbQuery(database) {
            Students.selectAll()
                .where { (Students.id eq studentId) and (Students.trainerId eq trainerId) }
                .singleOrNull() ?: throw NotFoundException("Student not found")

            Students.update({ (Students.id eq studentId) and (Students.trainerId eq trainerId) }) {
                req.name?.let { n -> it[name] = n.trim() }
                req.telegramId?.let { tid -> it[telegramId] = tid }
            }
            Students.selectAll().where { Students.id eq studentId }.single().toResponse()
        }
    }

    suspend fun delete(studentId: UUID, trainerId: UUID) {
        dbQuery(database) {
            val count = Students.deleteWhere { (Students.id eq studentId) and (Students.trainerId eq trainerId) }
            if (count == 0) throw NotFoundException("Student not found")
        }
    }

    suspend fun resetToken(studentId: UUID, trainerId: UUID): TokenResponse {
        return dbQuery(database) {
            val newToken = generateToken()
            val count = Students.update({ (Students.id eq studentId) and (Students.trainerId eq trainerId) }) {
                it[token] = newToken
            }
            if (count == 0) throw NotFoundException("Student not found")
            TokenResponse(newToken)
        }
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun ResultRow.toResponse() = StudentResponse(
        id = this[Students.id].toString(),
        trainerId = this[Students.trainerId].toString(),
        name = this[Students.name],
        telegramId = this[Students.telegramId],
        token = this[Students.token]
    )
}
