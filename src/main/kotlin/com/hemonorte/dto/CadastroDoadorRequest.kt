package com.hemonorte.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

data class CadastroDoadorRequest(
    @field:NotBlank
    val nome: String,

    @field:NotBlank @field:Email
    val email: String,

    @field:Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos")
    val cpf: String,

    @field:NotBlank
    val senhaHash: String,

    @field:Pattern(regexp = "(?i)(A|B|AB|O)[+-]", message = "Tipagem sanguínea inválida")
    val tipagem: String,

    val dataNascimento: LocalDate,

    @field:Pattern(regexp = "[MF]", message = "Gênero deve ser M ou F")
    val genero: Char,
)
