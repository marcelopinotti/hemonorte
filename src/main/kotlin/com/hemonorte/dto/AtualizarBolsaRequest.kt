package com.hemonorte.dto

import com.hemonorte.model.enums.StatusBolsa
import jakarta.validation.constraints.NotNull

data class AtualizarBolsaRequest(@field:NotNull val status: StatusBolsa)
