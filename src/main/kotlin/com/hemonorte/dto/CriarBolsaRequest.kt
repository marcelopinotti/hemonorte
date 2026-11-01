package com.hemonorte.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import java.time.LocalDate
import java.util.UUID

data class CriarBolsaRequest(
    @field:NotNull val agendamentoId: UUID,
    @field:NotBlank
    @field:Pattern(regexp = "(?i)(A|B|AB|O)[+-]", message = "Tipagem sanguínea inválida")
    val tipagem: String,
    @field:NotNull val dataColeta: LocalDate,
    @field:Future val dataValidade: LocalDate,
)
