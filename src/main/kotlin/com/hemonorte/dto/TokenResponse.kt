package com.hemonorte.dto

data class TokenResponse(
    val token: String,
    val tipo: String,
    val expiresIn: Long,
)
