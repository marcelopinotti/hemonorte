package com.hemonorte.dto

import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class RealizarTriagemRequest(
    @field:NotNull
    val agendamentoId: UUID,

    @field:NotNull
    val peso: BigDecimal,

    @field:NotNull
    val tatuagemRecente: Boolean,

    @field:NotNull
    val cirurgiaRecente: Boolean,

    @field:NotNull
    val teveFebre: Boolean,

    @field:NotNull
    val funcionarioId: UUID,
)
