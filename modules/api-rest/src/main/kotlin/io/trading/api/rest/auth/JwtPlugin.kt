package io.trading.api.rest.auth

import com.auth0.jwt.JWTVerifier
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.jwt.jwt
import io.trading.domain.user.UserId

data class UserPrincipal(val userId: UserId) : Principal

const val AUTH_SCHEME = "access-jwt"

fun Application.installJwt(verifier: JWTVerifier, realm: String = "trading") {
    install(Authentication) {
        jwt(AUTH_SCHEME) {
            this.realm = realm
            verifier(verifier)
            validate { credential ->
                credential.payload.subject?.let { sub ->
                    runCatching { UserPrincipal(UserId.parse(sub)) }.getOrNull()
                }
            }
        }
    }
}
