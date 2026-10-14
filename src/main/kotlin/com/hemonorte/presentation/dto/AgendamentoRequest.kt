package com.hemonorte.presentation.dto

import jakarta.validation.constraints.NotNull
import java.util.*

data class AgendamentoRequest(
    @field:NotNull
    val doadorId: UUID,

    @field:NotNull
    val slotId: UUID
)