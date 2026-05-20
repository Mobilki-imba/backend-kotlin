package io.trading.api.rest.instruments

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.trading.api.rest.auth.AUTH_SCHEME
import io.trading.application.market.GetCandlesUseCase
import io.trading.application.market.GetInstrumentDetailsUseCase
import io.trading.application.market.GetOrderBookUseCase
import io.trading.application.market.GetSparklineUseCase
import io.trading.application.market.ListInstrumentsUseCase
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.quote.CandleInterval
import kotlinx.datetime.Instant

fun Route.instrumentRoutes(
    list: ListInstrumentsUseCase,
    details: GetInstrumentDetailsUseCase,
    candles: GetCandlesUseCase,
    orderBook: GetOrderBookUseCase,
    sparkline: GetSparklineUseCase,
) {
    authenticate(AUTH_SCHEME) {
        route("/api/v1/instruments") {
            get {
                call.respond(HttpStatusCode.OK, list().map { it.toDto() })
            }
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val res = details(InstrumentId(id))
                call.respond(InstrumentDetailsDto(res.instrument.toDto(), res.quote.toDto()))
            }
            get("/{id}/candles") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val interval = CandleInterval.parse(call.parameters["interval"] ?: "1m")
                val from = call.parameters["from"]?.toLongOrNull()?.let(Instant::fromEpochMilliseconds)
                val to = call.parameters["to"]?.toLongOrNull()?.let(Instant::fromEpochMilliseconds)
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 100
                val includeOpen = call.parameters["includeOpen"]?.toBooleanStrictOrNull() ?: true
                val result = candles(InstrumentId(id), interval, from, to, limit, includeOpen)
                call.respond(result.map { it.toDto() })
            }
            get("/{id}/orderbook") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(orderBook(InstrumentId(id)).toDto())
            }
            get("/{id}/sparkline") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val points = call.parameters["points"]?.toIntOrNull() ?: 30
                call.respond(sparkline(InstrumentId(id), points).map { it.toDto() })
            }
        }
    }
}
