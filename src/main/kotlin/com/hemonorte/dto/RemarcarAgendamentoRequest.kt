package com.hemonorte.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class RemarcarAgendamentoRequest(@field:NotNull val slotId: UUID)
