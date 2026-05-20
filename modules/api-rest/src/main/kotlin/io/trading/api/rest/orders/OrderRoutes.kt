package io.trading.api.rest.orders

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.trading.api.rest.auth.AUTH_SCHEME
import io.trading.api.rest.auth.UserPrincipal
import io.trading.application.orders.CancelOrderUseCase
import io.trading.application.orders.GetOrderUseCase
import io.trading.application.orders.ListOrdersUseCase
import io.trading.application.orders.PlaceLimitOrderCommand
import io.trading.application.orders.PlaceLimitOrderUseCase
import io.trading.application.orders.PlaceMarketOrderCommand
import io.trading.application.orders.PlaceMarketOrderUseCase
import io.trading.application.ports.OrderListFilter
import io.trading.domain.common.Cursor
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderType
import io.trading.domain.order.Side

fun Route.orderRoutes(
    placeMarket: PlaceMarketOrderUseCase,
    placeLimit: PlaceLimitOrderUseCase,
    cancel: CancelOrderUseCase,
    list: ListOrdersUseCase,
    get: GetOrderUseCase,
) {
    authenticate(AUTH_SCHEME) {
        route("/api/v1/orders") {
            post {
                val userId = call.principal<UserPrincipal>()!!.userId
                val req = call.receive<PlaceOrderRequest>()
                val idemKey = call.request.headers["Idempotency-Key"]
                val type = OrderType.valueOf(req.type)
                val side = Side.valueOf(req.side)
                val qty = Quantity(req.quantity)
                val instr = InstrumentId(req.instrumentId)
                val order = when (type) {
                    OrderType.MARKET -> placeMarket(
                        PlaceMarketOrderCommand(userId, instr, side, qty, idemKey),
                    )
                    OrderType.LIMIT -> {
                        val price = req.price?.let(Money::ofDecimal)
                            ?: return@post call.respond(HttpStatusCode.BadRequest)
                        placeLimit(PlaceLimitOrderCommand(userId, instr, side, qty, price, idemKey))
                    }
                }
                call.respond(HttpStatusCode.Created, order.toDto())
            }
            get {
                val userId = call.principal<UserPrincipal>()!!.userId
                val filter = when (call.parameters["status"]) {
                    "active" -> OrderListFilter.ACTIVE
                    "history" -> OrderListFilter.HISTORY
                    else -> OrderListFilter.ALL
                }
                val cursor = call.parameters["cursor"]?.let { Cursor(it) }
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                val page = list(userId, filter, cursor, limit)
                call.respond(
                    OrdersPage(
                        items = page.items.map { it.toDto() },
                        nextCursor = page.nextCursor?.value,
                    ),
                )
            }
            get("/{id}") {
                val userId = call.principal<UserPrincipal>()!!.userId
                val id = OrderId.parse(call.parameters["id"]!!)
                call.respond(get(userId, id).toDto())
            }
            delete("/{id}") {
                val userId = call.principal<UserPrincipal>()!!.userId
                val id = OrderId.parse(call.parameters["id"]!!)
                cancel(userId, id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
