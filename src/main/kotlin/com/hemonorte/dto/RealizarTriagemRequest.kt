package com.hemonorte.dto

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.util.UUID

data class RealizarTriagemRequest(
    @field:NotNull
    val agendamentoId: UUID,

    @field:NotNull
    @field:Positive
    @field:Digits(integer = 3, fraction = 2)
    val peso: BigDecimal,

    @field:NotNull
    val tatuagemRecente: Boolean,

    @field:NotNull
    val cirurgiaRecente: Boolean,

    @field:NotNull
    val teveFebre: Boolean,
)
