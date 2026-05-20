package io.trading.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.trading.application.errors.TokenExpiredException
import io.trading.application.errors.TokenInvalidException
import io.trading.application.ports.Clock
import io.trading.domain.user.UserId
import java.util.Date
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.toJavaDuration

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val secret: String,
    val accessTtl: Duration,
)

class JwtService(private val cfg: JwtConfig, private val clock: Clock) {

    private val algorithm: Algorithm = Algorithm.HMAC256(cfg.secret)
    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(cfg.issuer)
        .withAudience(cfg.audience)
        .build()

    fun issueAccess(userId: UserId): String {
        val now = clock.now().toEpochMilliseconds()
        val exp = now + cfg.accessTtl.inWholeMilliseconds
        return JWT.create()
            .withIssuer(cfg.issuer)
            .withAudience(cfg.audience)
            .withSubject(userId.raw.toString())
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(exp))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
    }

    fun parseUserId(token: String): UserId = try {
        val decoded = verifier.verify(token)
        UserId.parse(decoded.subject)
    } catch (e: JWTVerificationException) {
        if (e.message?.contains("expired", ignoreCase = true) == true) throw TokenExpiredException()
        throw TokenInvalidException(e.message ?: "verification failed")
    }
}
