package com.hemonorte.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class RealizarAgendamentoRequest(
    @field:NotNull
    val doadorId: UUID,

    @field:NotNull
    val slotId: UUID,
)
