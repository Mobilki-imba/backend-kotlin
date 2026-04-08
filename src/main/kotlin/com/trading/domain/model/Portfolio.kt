package com.trading.domain.model

data class Portfolio(
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val positions: List<Position> = emptyList()
)
