package com.hemonorte.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginFuncionarioRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val matricula: String,

    @field:NotBlank
    @field:Size(max = 72)
    val senha: String,
)
