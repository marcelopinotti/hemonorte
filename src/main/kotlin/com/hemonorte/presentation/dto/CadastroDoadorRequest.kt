package com.hemonorte.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

data class CadastroDoadorRequest(
    @field:NotBlank
    val nome: String,

    @field:NotBlank @field:Email
    val email: String,

    @field:NotBlank
    val senha: String,

    @field:NotBlank
    val tipagem: String,

    @field:NotNull
    val dataNascimento: LocalDate,

    @field:NotNull
    val genero: Char,

    @field:Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos")
    val cpf: String,
)