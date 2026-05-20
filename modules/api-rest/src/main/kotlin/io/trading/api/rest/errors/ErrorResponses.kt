package io.trading.api.rest.errors

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val error: ErrorBody,
)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

object ErrorCodes {
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val FORBIDDEN = "FORBIDDEN"
    const val NOT_FOUND = "NOT_FOUND"
    const val EMAIL_TAKEN = "EMAIL_TAKEN"
    const val INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS"
    const val INSUFFICIENT_POSITION = "INSUFFICIENT_POSITION"
    const val INVALID_PRICE = "INVALID_PRICE"
    const val INVALID_QUANTITY = "INVALID_QUANTITY"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val INTERNAL = "INTERNAL"
    const val INSTRUMENT_INACTIVE = "INSTRUMENT_INACTIVE"
    const val ORDER_NOT_CANCELLABLE = "ORDER_NOT_CANCELLABLE"
    const val DUPLICATE_IDEMPOTENCY_KEY = "DUPLICATE_IDEMPOTENCY_KEY"
}
