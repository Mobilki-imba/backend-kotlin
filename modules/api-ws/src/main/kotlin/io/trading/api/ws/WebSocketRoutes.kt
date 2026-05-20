package io.trading.api.ws

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import io.trading.api.rest.auth.AUTH_SCHEME
import io.trading.api.rest.auth.UserPrincipal
import io.trading.application.ports.MarketDataPort
import io.trading.application.ports.OrderEventBus
import io.trading.application.ports.QuotesRangeRequest
import io.trading.domain.event.OrderEvent
import io.trading.domain.instrument.InstrumentId
import io.trading.infra.market.hub.MarketTickHub
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val log = LoggerFactory.getLogger("WebSocketRoutes")
private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

fun Application.installWebSockets(pingInterval: Duration, sessionTimeout: Duration) {
    install(WebSockets) {
        pingPeriod = pingInterval
        timeout = sessionTimeout
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}

fun Route.marketWebSocket(
    hub: MarketTickHub,
    marketData: MarketDataPort,
    eventBus: OrderEventBus,
) {
    authenticate(AUTH_SCHEME) {
        webSocket("/api/v1/ws/market") {
            val userId = call.principal<UserPrincipal>()?.userId
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "unauth"))
            val sessionJobs = ConcurrentHashMap<String, Job>()

            // Приватный канал orders сразу прицепляем
            val ordersJob = launch {
                eventBus.subscribeForUser(userId).collect { e ->
                    val msg: WsServerMessage = when (e) {
                        is OrderEvent.Created -> WsServerMessage.OrderUpdate(e.orderId.raw.toString(), "PENDING")
                        is OrderEvent.Filled -> WsServerMessage.OrderUpdate(
                            e.orderId.raw.toString(), e.status.name, e.filledQty.lots, e.avgPrice.toDecimalString())
                        is OrderEvent.Cancelled -> WsServerMessage.OrderUpdate(e.orderId.raw.toString(), "CANCELLED")
                        is OrderEvent.Rejected -> WsServerMessage.OrderUpdate(e.orderId.raw.toString(), "REJECTED")
                    }
                    runCatching { send(json.encodeToString(WsServerMessage.serializer(), msg)) }
                }
            }
            sessionJobs["orders"] = ordersJob

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val text = frame.readText()
                    val message = runCatching {
                        json.decodeFromString(WsClientMessage.serializer(), text)
                    }.getOrNull()
                    when (message) {
                        is WsClientMessage.Subscribe -> {
                            message.channels.forEach { ch ->
                                if (sessionJobs.containsKey(ch)) return@forEach
                                val job = launch {
                                    handleSubscribe(ch, message.lastSeenTs, hub, marketData)
                                }
                                sessionJobs[ch] = job
                            }
                        }
                        is WsClientMessage.Unsubscribe -> {
                            message.channels.forEach { ch ->
                                sessionJobs.remove(ch)?.cancel()
                            }
                        }
                        WsClientMessage.Ping -> send(json.encodeToString(WsServerMessage.serializer(), WsServerMessage.Pong))
                        null -> send(json.encodeToString(WsServerMessage.serializer(),
                            WsServerMessage.ErrorMessage("INVALID_REQUEST", "bad frame")))
                    }
                }
            } catch (e: Exception) {
                log.debug("ws session ended: {}", e.message)
            } finally {
                sessionJobs.values.forEach { runCatching { it.cancel() } }
            }
        }
    }
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.handleSubscribe(
    channel: String,
    lastSeenMs: Long?,
    hub: MarketTickHub,
    marketData: MarketDataPort,
) {
    val parts = channel.split(":")
    if (parts.size != 2) {
        send(json.encodeToString(WsServerMessage.serializer(),
            WsServerMessage.ErrorMessage("INVALID_REQUEST", "bad channel $channel")))
        return
    }
    val instrumentId = parts[1].toIntOrNull() ?: return
    val id = InstrumentId(instrumentId)

    when (parts[0]) {
        "quote" -> {
            if (lastSeenMs != null) {
                val from = Instant.fromEpochMilliseconds(lastSeenMs)
                marketData.streamQuotesRange(QuotesRangeRequest(setOf(id), from, null))
                    .takeWhile { it.timestamp <= io.trading.application.ports.SystemNow.now() }
                    .collect { t ->
                        val msg = WsServerMessage.QuoteMessage(
                            t.instrumentId.raw, t.price.toDecimalString(),
                            t.bid.toDecimalString(), t.ask.toDecimalString(),
                            t.timestamp.toEpochMilliseconds(), backfill = true,
                        )
                        send(json.encodeToString(WsServerMessage.serializer(), msg))
                    }
            }
            hub.subscribe(id).collect { t ->
                val msg = WsServerMessage.QuoteMessage(
                    t.instrumentId.raw, t.price.toDecimalString(),
                    t.bid.toDecimalString(), t.ask.toDecimalString(),
                    t.timestamp.toEpochMilliseconds(),
                )
                send(json.encodeToString(WsServerMessage.serializer(), msg))
            }
        }
        "orderbook" -> {
            while (true) {
                val ob = runCatching { marketData.getOrderBook(id) }.getOrNull()
                if (ob != null) {
                    val msg = WsServerMessage.OrderBookMessage(
                        instrumentId = ob.instrumentId.raw,
                        bids = ob.bids.map { listOf(it.price.toDecimalString(), it.quantity.toString()) },
                        asks = ob.asks.map { listOf(it.price.toDecimalString(), it.quantity.toString()) },
                        ts = ob.timestamp.toEpochMilliseconds(),
                    )
                    send(json.encodeToString(WsServerMessage.serializer(), msg))
                }
                kotlinx.coroutines.delay(500)
            }
        }
        else -> send(json.encodeToString(WsServerMessage.serializer(),
            WsServerMessage.ErrorMessage("INVALID_REQUEST", "unknown channel ${parts[0]}")))
    }
}
