package com.hemonorte.dto

import com.hemonorte.model.Agendamento
import com.hemonorte.model.enums.StatusAgendamento
import java.util.UUID

data class AgendamentoResponse(
    val id: UUID,
    val doadorId: UUID,
    val slotId: UUID,
    val status: StatusAgendamento,
    val codigoComprovante: UUID?,
)

fun Agendamento.toResponse() = AgendamentoResponse(
    id = id,
    doadorId = doador.id,
    slotId = slot.id,
    status = status,
    codigoComprovante = codigoComprovante,
)
