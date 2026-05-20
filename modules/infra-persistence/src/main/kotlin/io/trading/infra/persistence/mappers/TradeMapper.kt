package io.trading.infra.persistence.mappers

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.OrderId
import io.trading.domain.order.Side
import io.trading.domain.trade.Trade
import io.trading.domain.trade.TradeId
import io.trading.domain.user.UserId
import io.trading.infra.persistence.rows.TradeRow

fun TradeRow.toDomain(): Trade = Trade(
    id = TradeId(id),
    orderId = OrderId(orderId),
    userId = UserId(userId),
    instrumentId = InstrumentId(instrumentId),
    side = Side.valueOf(side),
    price = Money.ofMicroUnits(priceMicro),
    quantity = Quantity(quantityLots),
    commission = Money.ofMicroUnits(commissionMicro),
    executedAt = executedAt.toKt(),
)

fun Trade.toRow(): TradeRow = TradeRow(
    id = id.raw,
    orderId = orderId.raw,
    userId = userId.raw,
    instrumentId = instrumentId.raw,
    side = side.name,
    priceMicro = price.microUnits,
    quantityLots = quantity.lots,
    commissionMicro = commission.microUnits,
    executedAt = executedAt.toJava(),
)
