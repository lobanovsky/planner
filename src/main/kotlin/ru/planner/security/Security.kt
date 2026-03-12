package ru.planner.security

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ru.planner.db.DatabaseKey
import ru.planner.db.schema.Students
import java.util.*
import kotlinx.coroutines.Dispatchers

val JwtServiceKey = AttributeKey<JwtService>("JwtService")

data class StudentPrincipal(val studentId: UUID, val trainerId: UUID) : Principal

fun Application.configureSecurity() {
    val secret = environment.config.property("jwt.secret").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val domain = environment.config.property("jwt.domain").getString()
    val realm = environment.config.property("jwt.realm").getString()

    val jwtService = JwtService(secret, audience, domain)
    attributes.put(JwtServiceKey, jwtService)

    val database = attributes[DatabaseKey]

    authentication {
        jwt("trainer-jwt") {
            this.realm = realm
            verifier(jwtService.buildVerifier())
            validate { credential ->
                if (credential.payload.audience.contains(audience))
                    JWTPrincipal(credential.payload)
                else
                    null
            }
        }

        bearer("student-token") {
            this.realm = realm
            authenticate { tokenCredential ->
                findStudentByToken(database, tokenCredential.token)
            }
        }
    }
}

private suspend fun findStudentByToken(database: Database, token: String): StudentPrincipal? =
    newSuspendedTransaction(Dispatchers.IO, database) {
        Students.selectAll()
            .where { Students.token eq token }
            .singleOrNull()
            ?.let { row ->
                StudentPrincipal(
                    studentId = row[Students.id],
                    trainerId = row[Students.trainerId]
                )
            }
    }
