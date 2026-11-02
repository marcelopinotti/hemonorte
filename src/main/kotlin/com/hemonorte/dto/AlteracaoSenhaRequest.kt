package com.hemonorte.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AlteracaoSenhaRequest(
    @field:NotBlank
    @field:Size(max = 72)
    val senha: String,
)
