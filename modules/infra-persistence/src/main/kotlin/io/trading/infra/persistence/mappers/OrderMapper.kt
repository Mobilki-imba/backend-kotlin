package io.trading.infra.persistence.mappers

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.Order
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderStatus
import io.trading.domain.order.OrderType
import io.trading.domain.order.Side
import io.trading.domain.user.UserId
import io.trading.infra.persistence.rows.OrderRow

fun OrderRow.toDomain(): Order = Order(
    id = OrderId(id),
    userId = UserId(userId),
    instrumentId = InstrumentId(instrumentId),
    side = Side.valueOf(side),
    type = OrderType.valueOf(type),
    limitPrice = limitPriceMicro?.let(Money::ofMicroUnits),
    quantity = Quantity(quantityLots),
    filledQuantity = Quantity(filledQuantityLots),
    status = OrderStatus.valueOf(status),
    avgFillPrice = avgFillPriceMicro?.let(Money::ofMicroUnits),
    commission = Money.ofMicroUnits(commissionMicro),
    idempotencyKey = idempotencyKey,
    createdAt = createdAt.toKt(),
    closedAt = closedAt?.toKt(),
)

fun Order.toRow(): OrderRow = OrderRow(
    id = id.raw,
    userId = userId.raw,
    instrumentId = instrumentId.raw,
    side = side.name,
    type = type.name,
    limitPriceMicro = limitPrice?.microUnits,
    quantityLots = quantity.lots,
    filledQuantityLots = filledQuantity.lots,
    status = status.name,
    avgFillPriceMicro = avgFillPrice?.microUnits,
    commissionMicro = commission.microUnits,
    idempotencyKey = idempotencyKey,
    createdAt = createdAt.toJava(),
    closedAt = closedAt?.toJava(),
)
