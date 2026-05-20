package io.trading.api.rest.trades

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.trading.api.rest.auth.AUTH_SCHEME
import io.trading.api.rest.auth.UserPrincipal
import io.trading.api.rest.orders.TradesPage
import io.trading.api.rest.orders.toDto
import io.trading.application.portfolio.GetTradesUseCase
import io.trading.domain.common.Cursor
import io.trading.domain.instrument.InstrumentId
import kotlinx.datetime.Instant

fun Route.tradesRoutes(getTrades: GetTradesUseCase) {
    authenticate(AUTH_SCHEME) {
        get("/api/v1/trades") {
            val userId = call.principal<UserPrincipal>()!!.userId
            val instr = call.parameters["instrumentId"]?.toIntOrNull()?.let(::InstrumentId)
            val from = call.parameters["from"]?.toLongOrNull()?.let(Instant::fromEpochMilliseconds)
            val to = call.parameters["to"]?.toLongOrNull()?.let(Instant::fromEpochMilliseconds)
            val cursor = call.parameters["cursor"]?.let(::Cursor)
            val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
            val page = getTrades(userId, instr, from, to, cursor, limit)
            call.respond(
                TradesPage(
                    items = page.items.map { it.toDto() },
                    nextCursor = page.nextCursor?.value,
                ),
            )
        }
    }
}
