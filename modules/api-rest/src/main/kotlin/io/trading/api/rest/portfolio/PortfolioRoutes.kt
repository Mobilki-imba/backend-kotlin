package io.trading.api.rest.portfolio

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.trading.api.rest.auth.AUTH_SCHEME
import io.trading.api.rest.auth.UserPrincipal
import io.trading.application.portfolio.GetPortfolioUseCase

fun Route.portfolioRoutes(getPortfolio: GetPortfolioUseCase) {
    authenticate(AUTH_SCHEME) {
        get("/api/v1/portfolio") {
            val userId = call.principal<UserPrincipal>()!!.userId
            call.respond(getPortfolio(userId).toDto())
        }
    }
}
