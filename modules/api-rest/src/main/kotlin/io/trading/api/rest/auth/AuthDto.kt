package io.trading.api.rest.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String? = null)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class TokenResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
)
