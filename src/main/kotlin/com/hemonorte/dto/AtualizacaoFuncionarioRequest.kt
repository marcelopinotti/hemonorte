package com.hemonorte.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class AtualizacaoFuncionarioRequest(
    @field:NotBlank
    val nome: String,

    @field:NotBlank
    @field:Email
    val email: String,
)
