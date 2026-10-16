package com.hemonorte.dto

import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class RegistrarColetaRequest(
    @field:NotNull
    val triagemId: UUID,

    @field:NotNull
    val funcionarioId: UUID,

    @field:NotNull
    val volumeMl: BigDecimal,
)
