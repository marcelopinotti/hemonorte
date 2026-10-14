package com.hemonorte.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.*

data class BolsaRequest(
    @field:NotBlank
    val tipagem: String, // Ex: "O+", "A-"

    @field:NotNull
    val dataColeta: LocalDate,

    @field:NotNull
    val dataValidade: LocalDate,

    @field:NotNull
    val doadorId: UUID
)