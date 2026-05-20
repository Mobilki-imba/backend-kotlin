package io.trading.application.orders

import io.trading.application.errors.InstrumentInactiveException
import io.trading.application.errors.InvalidPriceException
import io.trading.application.errors.InvalidQuantityException
import io.trading.domain.instrument.Instrument
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.OrderType

object OrderValidator {
    fun validate(
        instrument: Instrument,
        type: OrderType,
        quantity: Quantity,
        limitPrice: Money?,
    ) {
        if (!instrument.isActive) throw InstrumentInactiveException(instrument.id.raw)
        if (!quantity.isPositive) throw InvalidQuantityException("quantity must be > 0")
        if (!quantity.isMultipleOf(instrument.lotSize))
            throw InvalidQuantityException("quantity must be a multiple of lot_size=${instrument.lotSize}")
        when (type) {
            OrderType.MARKET -> if (limitPrice != null) throw InvalidPriceException("MARKET must not have price")
            OrderType.LIMIT -> {
                val p = limitPrice ?: throw InvalidPriceException("LIMIT requires price")
                if (!p.isPositive) throw InvalidPriceException("price must be positive")
                if (p.microUnits % instrument.priceStep.microUnits != 0L)
                    throw InvalidPriceException("price must be a multiple of price_step")
            }
        }
    }
}
