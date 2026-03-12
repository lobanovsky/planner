package ru.planner.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class JwtService(
    private val secret: String,
    private val audience: String,
    private val issuer: String
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(trainerId: UUID, email: String): String = JWT.create()
        .withSubject(trainerId.toString())
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("trainer_id", trainerId.toString())
        .withClaim("email", email)
        .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L))
        .sign(algorithm)

    fun buildVerifier(): JWTVerifier = JWT
        .require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
}
