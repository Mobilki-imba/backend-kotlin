package io.trading.api.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WsClientMessage {
    @Serializable
    @SerialName("subscribe")
    data class Subscribe(
        val channels: List<String>,
        val lastSeenTs: Long? = null,
    ) : WsClientMessage

    @Serializable
    @SerialName("unsubscribe")
    data class Unsubscribe(val channels: List<String>) : WsClientMessage

    @Serializable
    @SerialName("ping")
    data object Ping : WsClientMessage
}

@Serializable
sealed interface WsServerMessage {
    @Serializable
    @SerialName("quote")
    data class QuoteMessage(
        val instrumentId: Int,
        val price: String,
        val bid: String,
        val ask: String,
        val ts: Long,
        val backfill: Boolean = false,
    ) : WsServerMessage

    @Serializable
    @SerialName("orderbook")
    data class OrderBookMessage(
        val instrumentId: Int,
        val bids: List<List<String>>,
        val asks: List<List<String>>,
        val ts: Long,
    ) : WsServerMessage

    @Serializable
    @SerialName("order_update")
    data class OrderUpdate(
        val orderId: String,
        val status: String,
        val filledQty: Long? = null,
        val avgPrice: String? = null,
    ) : WsServerMessage

    @Serializable
    @SerialName("error")
    data class ErrorMessage(val code: String, val message: String? = null) : WsServerMessage

    @Serializable
    @SerialName("pong")
    data object Pong : WsServerMessage
}
