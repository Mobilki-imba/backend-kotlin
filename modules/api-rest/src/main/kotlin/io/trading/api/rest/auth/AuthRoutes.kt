package io.trading.api.rest.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.trading.application.auth.AuthResult
import io.trading.application.auth.LoginCommand
import io.trading.application.auth.LoginUseCase
import io.trading.application.auth.LogoutUseCase
import io.trading.application.auth.RefreshUseCase
import io.trading.application.auth.RegisterCommand
import io.trading.application.auth.RegisterUseCase

fun Route.authRoutes(
    register: RegisterUseCase,
    login: LoginUseCase,
    refresh: RefreshUseCase,
    logout: LogoutUseCase,
) {
    route("/api/v1/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            val result = register(RegisterCommand(req.email, req.password, req.displayName))
            call.respond(HttpStatusCode.Created, result.toDto())
        }
        post("/login") {
            val req = call.receive<LoginRequest>()
            val ip = call.request.origin.remoteHost
            val result = login(LoginCommand(req.email, req.password, ip))
            call.respond(HttpStatusCode.OK, result.toDto())
        }
        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            val result = refresh(req.refreshToken)
            call.respond(HttpStatusCode.OK, result.toDto())
        }
        post("/logout") {
            val req = call.receive<LogoutRequest>()
            logout(req.refreshToken)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun AuthResult.toDto() = TokenResponse(
    userId = userId.raw.toString(),
    accessToken = accessToken,
    refreshToken = refreshToken,
)
