package ru.planner.domain

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import ru.planner.db.schema.Venues
import ru.planner.models.BadRequestException
import ru.planner.models.NotFoundException
import ru.planner.models.requests.CreateVenueRequest
import ru.planner.models.requests.UpdateVenueRequest
import ru.planner.models.responses.VenueResponse
import java.util.*

class VenueService(private val database: Database) {

    suspend fun create(trainerId: UUID, req: CreateVenueRequest): VenueResponse {
        if (req.name.isBlank()) throw BadRequestException("Venue name is required")
        return dbQuery(database) {
            val id = Venues.insert {
                it[Venues.trainerId] = trainerId
                it[name] = req.name.trim()
                it[description] = req.description
            }[Venues.id]
            VenueResponse(id.toString(), trainerId.toString(), req.name.trim(), req.description)
        }
    }

    suspend fun findAll(trainerId: UUID): List<VenueResponse> = dbQuery(database) {
        Venues.selectAll()
            .where { Venues.trainerId eq trainerId }
            .map { it.toResponse() }
    }

    suspend fun findById(venueId: UUID, trainerId: UUID): VenueResponse = dbQuery(database) {
        Venues.selectAll()
            .where { (Venues.id eq venueId) and (Venues.trainerId eq trainerId) }
            .singleOrNull()
            ?.toResponse()
            ?: throw NotFoundException("Venue not found")
    }

    suspend fun update(venueId: UUID, trainerId: UUID, req: UpdateVenueRequest): VenueResponse {
        return dbQuery(database) {
            val count = Venues.update({ (Venues.id eq venueId) and (Venues.trainerId eq trainerId) }) {
                req.name?.let { n -> it[name] = n.trim() }
                req.description?.let { d -> it[description] = d }
            }
            if (count == 0) throw NotFoundException("Venue not found")
            Venues.selectAll().where { Venues.id eq venueId }.single().toResponse()
        }
    }

    suspend fun delete(venueId: UUID, trainerId: UUID) {
        dbQuery(database) {
            val count = Venues.deleteWhere { (Venues.id eq venueId) and (Venues.trainerId eq trainerId) }
            if (count == 0) throw NotFoundException("Venue not found")
        }
    }

    private fun ResultRow.toResponse() = VenueResponse(
        id = this[Venues.id].toString(),
        trainerId = this[Venues.trainerId].toString(),
        name = this[Venues.name],
        description = this[Venues.description]
    )
}
