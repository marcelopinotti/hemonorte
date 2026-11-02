package com.hemonorte.dto

import com.hemonorte.model.enums.ResultadoSorologico
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class RegistrarExameRequest(
    @field:NotNull
    val bolsaId: UUID,

    val funcionarioId: UUID? = null,

    @field:NotNull
    val hiv: ResultadoSorologico,

    @field:NotNull
    val hepatite: ResultadoSorologico,

    @field:NotNull
    val chagas: ResultadoSorologico,
)
