package com.hemonorte.presentation.dto

import com.hemonorte.domain.Agendamento
import com.hemonorte.domain.enums.StatusAgendamento
import java.util.*

data class AgendamentoResponse(
    val id: UUID,
    val doadorId: UUID,
    val slotId: UUID,
    val status: StatusAgendamento,
    val codigoComprovante: UUID?
)

fun Agendamento.toResponse() = AgendamentoResponse(
    id = id,
    doadorId = doador.id,
    slotId = slot.id,
    status = status,
    codigoComprovante = codigoComprovante
)