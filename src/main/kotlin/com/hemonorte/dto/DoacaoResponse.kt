package com.hemonorte.dto

import com.hemonorte.model.Agendamento
import java.time.Instant
import java.util.UUID

data class DoacaoResponse(
    val agendamentoId: UUID,
    val dataHora: Instant,
    val codigoComprovante: UUID?,
)

fun Agendamento.toDoacaoResponse() = DoacaoResponse(
    agendamentoId = id,
    dataHora = slot.inicio,
    codigoComprovante = codigoComprovante,
)
