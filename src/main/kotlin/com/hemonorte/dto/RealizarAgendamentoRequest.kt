package com.hemonorte.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class RealizarAgendamentoRequest(
    val doadorId: UUID? = null,

    @field:NotNull
    val slotId: UUID,
)
