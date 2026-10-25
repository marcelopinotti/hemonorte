package com.hemonorte.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

data class AtualizacaoDoadorRequest(
    @field:NotBlank
    val nome: String,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:Pattern(regexp = "(?i)(A|B|AB|O)[+-]", message = "Tipagem sanguínea inválida")
    val tipagem: String,

    @field:PastOrPresent
    val dataNascimento: LocalDate,

    @field:Pattern(regexp = "[MF]", message = "Gênero deve ser M ou F")
    val genero: String,
)
