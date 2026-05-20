package io.trading.domain.common

@JvmInline
value class Cursor(val value: String) {
    init {
        require(value.isNotBlank()) { "Cursor cannot be blank" }
    }
}

data class Page<T>(
    val items: List<T>,
    val nextCursor: Cursor?,
) {
    val hasMore: Boolean get() = nextCursor != null
}
