package io.trading.api.rest.errors

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.application.Application
import io.trading.application.errors.DomainException
import io.trading.application.errors.DuplicateIdempotencyKeyException
import io.trading.application.errors.EmailAlreadyTakenException
import io.trading.application.errors.InsufficientFundsException
import io.trading.application.errors.InsufficientPositionException
import io.trading.application.errors.InstrumentInactiveException
import io.trading.application.errors.InstrumentNotFoundException
import io.trading.application.errors.InvalidCredentialsException
import io.trading.application.errors.InvalidPriceException
import io.trading.application.errors.InvalidQuantityException
import io.trading.application.errors.MarketTimeoutException
import io.trading.application.errors.MarketUnavailableException
import io.trading.application.errors.OrderNotCancellableException
import io.trading.application.errors.OrderNotFoundException
import io.trading.application.errors.OrderNotOwnedException
import io.trading.application.errors.RateLimitedException
import io.trading.application.errors.TokenExpiredException
import io.trading.application.errors.TokenInvalidException
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("StatusPages")

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val (status, body) = cause.toApiError()
            if (status.value >= 500) log.error("server error", cause)
            else log.debug("client error: {}", cause.message)
            call.respond(status, body)
        }
    }
}

private fun Throwable.toApiError(): Pair<HttpStatusCode, ApiError> = when (this) {
    is EmailAlreadyTakenException ->
        HttpStatusCode.Conflict to apiError(ErrorCodes.EMAIL_TAKEN, "Email already taken")
    is InvalidCredentialsException ->
        HttpStatusCode.Unauthorized to apiError(ErrorCodes.UNAUTHORIZED, "Invalid credentials")
    is TokenInvalidException, is TokenExpiredException ->
        HttpStatusCode.Unauthorized to apiError(ErrorCodes.UNAUTHORIZED, "Invalid or expired token")
    is RateLimitedException ->
        HttpStatusCode.TooManyRequests to apiError(ErrorCodes.RATE_LIMITED, "Rate limit exceeded",
            mapOf("retryAfterMs" to retryAfterMs.toString()))
    is InstrumentNotFoundException ->
        HttpStatusCode.NotFound to apiError(ErrorCodes.NOT_FOUND, "Instrument not found")
    is InstrumentInactiveException ->
        HttpStatusCode.UnprocessableEntity to apiError(ErrorCodes.INSTRUMENT_INACTIVE, "Instrument inactive")
    is OrderNotFoundException ->
        HttpStatusCode.NotFound to apiError(ErrorCodes.NOT_FOUND, "Order not found")
    is OrderNotOwnedException ->
        HttpStatusCode.Forbidden to apiError(ErrorCodes.FORBIDDEN, "Order does not belong to user")
    is OrderNotCancellableException ->
        HttpStatusCode.UnprocessableEntity to apiError(ErrorCodes.ORDER_NOT_CANCELLABLE, "Order cannot be cancelled")
    is InsufficientFundsException ->
        HttpStatusCode.UnprocessableEntity to apiError(ErrorCodes.INSUFFICIENT_FUNDS, "Insufficient funds")
    is InsufficientPositionException ->
        HttpStatusCode.UnprocessableEntity to apiError(ErrorCodes.INSUFFICIENT_POSITION, "Insufficient position")
    is InvalidPriceException ->
        HttpStatusCode.UnprocessableEntity to apiError(ErrorCodes.INVALID_PRICE, message ?: "Invalid price")
    is InvalidQuantityException ->
        HttpStatusCode.UnprocessableEntity to apiError(ErrorCodes.INVALID_QUANTITY, message ?: "Invalid quantity")
    is DuplicateIdempotencyKeyException ->
        HttpStatusCode.Conflict to apiError(ErrorCodes.DUPLICATE_IDEMPOTENCY_KEY, "Duplicate idempotency key")
    is MarketUnavailableException, is MarketTimeoutException ->
        HttpStatusCode.ServiceUnavailable to apiError(ErrorCodes.INTERNAL, "Market data unavailable")
    is IllegalArgumentException ->
        HttpStatusCode.BadRequest to apiError(ErrorCodes.INVALID_REQUEST, message ?: "Invalid request")
    is DomainException ->
        HttpStatusCode.UnprocessableEntity to apiError(ErrorCodes.INVALID_REQUEST, message ?: "Domain error")
    else ->
        HttpStatusCode.InternalServerError to apiError(ErrorCodes.INTERNAL, "Internal server error")
}

private fun apiError(code: String, message: String, details: Map<String, String> = emptyMap()) =
    ApiError(ErrorBody(code, message, details))
