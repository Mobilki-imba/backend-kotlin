package io.trading.application.errors

sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class EmailAlreadyTakenException(email: String) : DomainException("Email already taken: $email")
class InvalidCredentialsException : DomainException("Invalid credentials")
class TokenInvalidException(reason: String) : DomainException("Invalid token: $reason")
class TokenExpiredException : DomainException("Token expired")
class RateLimitedException(val retryAfterMs: Long) : DomainException("Rate limit exceeded")

class InstrumentNotFoundException(id: Int) : DomainException("Instrument not found: $id")
class InstrumentInactiveException(id: Int) : DomainException("Instrument inactive: $id")

class OrderNotFoundException(id: String) : DomainException("Order not found: $id")
class OrderNotOwnedException : DomainException("Order does not belong to user")
class OrderNotCancellableException(status: String) : DomainException("Order in status $status cannot be cancelled")

class InsufficientFundsException : DomainException("Insufficient funds")
class InsufficientPositionException : DomainException("Insufficient position")
class InvalidPriceException(reason: String) : DomainException("Invalid price: $reason")
class InvalidQuantityException(reason: String) : DomainException("Invalid quantity: $reason")
class InvalidPasswordException(reason: String) : DomainException("Invalid password: $reason")

class MarketUnavailableException(cause: Throwable? = null) : DomainException("Market data unavailable", cause)
class MarketTimeoutException : DomainException("Market data timeout")

class DuplicateIdempotencyKeyException : DomainException("Duplicate idempotency key")
