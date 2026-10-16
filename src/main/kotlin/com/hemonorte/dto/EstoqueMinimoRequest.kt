package com.hemonorte.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class EstoqueMinimoRequest(
    @field:NotBlank
    val tipagem: String,

    @field:NotNull
    @field:Positive
    val quantidadeCritica: Int,
)
