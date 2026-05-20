package io.trading.api.rest.orders

import io.trading.domain.order.Order
import io.trading.domain.trade.Trade
import kotlinx.serialization.Serializable

@Serializable
data class PlaceOrderRequest(
    val instrumentId: Int,
    val side: String,
    val type: String,
    val price: String? = null,
    val quantity: Long,
)

@Serializable
data class OrderDto(
    val id: String,
    val instrumentId: Int,
    val side: String,
    val type: String,
    val price: String?,
    val quantity: Long,
    val filledQuantity: Long,
    val status: String,
    val avgFillPrice: String?,
    val commission: String,
    val createdAt: Long,
    val closedAt: Long?,
)

@Serializable
data class OrdersPage(val items: List<OrderDto>, val nextCursor: String?)

@Serializable
data class TradeDto(
    val id: String,
    val orderId: String,
    val instrumentId: Int,
    val side: String,
    val price: String,
    val quantity: Long,
    val commission: String,
    val executedAt: Long,
)

@Serializable
data class TradesPage(val items: List<TradeDto>, val nextCursor: String?)

fun Order.toDto() = OrderDto(
    id = id.raw.toString(),
    instrumentId = instrumentId.raw,
    side = side.name,
    type = type.name,
    price = limitPrice?.toDecimalString(),
    quantity = quantity.lots,
    filledQuantity = filledQuantity.lots,
    status = status.name,
    avgFillPrice = avgFillPrice?.toDecimalString(),
    commission = commission.toDecimalString(),
    createdAt = createdAt.toEpochMilliseconds(),
    closedAt = closedAt?.toEpochMilliseconds(),
)

fun Trade.toDto() = TradeDto(
    id = id.raw.toString(),
    orderId = orderId.raw.toString(),
    instrumentId = instrumentId.raw,
    side = side.name,
    price = price.toDecimalString(),
    quantity = quantity.lots,
    commission = commission.toDecimalString(),
    executedAt = executedAt.toEpochMilliseconds(),
)
