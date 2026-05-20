package io.trading.infra.persistence.mappers

import io.trading.domain.event.OrderEvent
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderStatus
import io.trading.domain.user.UserId
import io.trading.infra.persistence.rows.OrderEventRow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class CreatedPayload(val nothing: Boolean = true)

@Serializable
private data class FilledPayload(
    val status: String,
    val filledLots: Long,
    val avgPriceMicro: Long,
)

@Serializable
private data class CancelledPayload(val nothing: Boolean = true)

@Serializable
private data class RejectedPayload(val reason: String)

fun OrderEvent.toRow(): OrderEventRow = when (this) {
    is OrderEvent.Created -> OrderEventRow(
        id = eventId, userId = userId.raw, orderId = orderId.raw,
        eventType = "CREATED",
        payload = json.encodeToString(CreatedPayload.serializer(), CreatedPayload()),
        createdAt = occurredAt.toJava(), dispatchedAt = null,
    )
    is OrderEvent.Filled -> OrderEventRow(
        id = eventId, userId = userId.raw, orderId = orderId.raw,
        eventType = "FILLED",
        payload = json.encodeToString(
            FilledPayload.serializer(),
            FilledPayload(status.name, filledQty.lots, avgPrice.microUnits),
        ),
        createdAt = occurredAt.toJava(), dispatchedAt = null,
    )
    is OrderEvent.Cancelled -> OrderEventRow(
        id = eventId, userId = userId.raw, orderId = orderId.raw,
        eventType = "CANCELLED",
        payload = json.encodeToString(CancelledPayload.serializer(), CancelledPayload()),
        createdAt = occurredAt.toJava(), dispatchedAt = null,
    )
    is OrderEvent.Rejected -> OrderEventRow(
        id = eventId, userId = userId.raw, orderId = orderId.raw,
        eventType = "REJECTED",
        payload = json.encodeToString(RejectedPayload.serializer(), RejectedPayload(reason)),
        createdAt = occurredAt.toJava(), dispatchedAt = null,
    )
}

fun OrderEventRow.toDomain(): OrderEvent {
    val uid = UserId(userId)
    val oid = OrderId(orderId)
    val ts = createdAt.toKt()
    return when (eventType) {
        "CREATED" -> OrderEvent.Created(id, uid, oid, ts)
        "FILLED" -> {
            val p = json.decodeFromString(FilledPayload.serializer(), payload)
            OrderEvent.Filled(id, uid, oid, ts, OrderStatus.valueOf(p.status),
                Quantity(p.filledLots), Money.ofMicroUnits(p.avgPriceMicro))
        }
        "CANCELLED" -> OrderEvent.Cancelled(id, uid, oid, ts)
        "REJECTED" -> {
            val p = json.decodeFromString(RejectedPayload.serializer(), payload)
            OrderEvent.Rejected(id, uid, oid, ts, p.reason)
        }
        else -> error("Unknown event type: $eventType")
    }
}
