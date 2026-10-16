package com.hemonorte.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CriarSlotRequest(
    @field:NotNull
    @field:Future
    val inicio: Instant,

    @field:Min(1)
    val capacidade: Int,
)
