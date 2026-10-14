package com.hemonorte.presentation.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.*

data class TriagemRequest(
    @field:NotNull
    val doadorId: UUID,

    @field:NotNull
    @field:DecimalMin(value = "50.0", message = "O peso mínimo para doação é 50kg")
    val peso: BigDecimal,

    @field:NotNull
    val estaDormidoBem: Boolean, // Mínimo de 6 horas nas últimas 24h

    @field:NotNull
    val teveFebreOuGripeRecentemente: Boolean,

    @field:NotNull
    val fezTatuagemOuPircingNosUltimosMeses: Boolean, // Regra padrão: geralmente 6 a 12 meses

    @field:NotNull
    val ingeriuAlcoolNasUltimas12Horas: Boolean
)