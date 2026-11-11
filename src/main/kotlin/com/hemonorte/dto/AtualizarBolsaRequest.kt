package com.hemonorte.dto

import com.hemonorte.model.enums.StatusBolsa
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AtualizarBolsaRequest(
    @field:NotNull val status: StatusBolsa,
    @field:Size(max = 255) val motivo: String? = null,
)
