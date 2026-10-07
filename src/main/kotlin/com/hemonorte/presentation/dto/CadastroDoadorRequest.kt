package com.hemonorte.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

data class CadastroDoadorRequest(
    @field:NotBlank
    val nome: String,

    @field:NotBlank @field:Email
    val email: String,

    @field:Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos")
    val cpf: String,
)