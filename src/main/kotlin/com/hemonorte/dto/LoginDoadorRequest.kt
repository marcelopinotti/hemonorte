package com.hemonorte.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LoginDoadorRequest(
    @field:Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos")
    val cpf: String,

    @field:NotBlank
    @field:Size(max = 72)
    val senha: String,
)
