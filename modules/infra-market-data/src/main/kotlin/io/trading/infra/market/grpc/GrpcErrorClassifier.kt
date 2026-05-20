package io.trading.infra.market.grpc

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.trading.application.errors.InstrumentNotFoundException
import io.trading.application.errors.MarketTimeoutException
import io.trading.application.errors.MarketUnavailableException

/**
 * Маппинг gRPC Status → доменные исключения.
 */
object GrpcErrorClassifier {

    fun classify(throwable: Throwable, instrumentId: Int? = null): Throwable {
        val code: Status.Code = when (throwable) {
            is StatusException -> throwable.status.code
            is StatusRuntimeException -> throwable.status.code
            else -> return throwable
        }
        return when (code) {
            Status.Code.NOT_FOUND -> InstrumentNotFoundException(instrumentId ?: -1)
            Status.Code.DEADLINE_EXCEEDED -> MarketTimeoutException()
            Status.Code.UNAVAILABLE,
            Status.Code.INTERNAL,
            Status.Code.UNKNOWN -> MarketUnavailableException(throwable)
            else -> throwable
        }
    }

    inline fun <T> wrap(instrumentId: Int? = null, block: () -> T): T = try {
        block()
    } catch (e: StatusException) {
        throw classify(e, instrumentId)
    } catch (e: StatusRuntimeException) {
        throw classify(e, instrumentId)
    }
}
