package com.hemonorte.dto

import com.hemonorte.model.HistoricoAuditoria
import com.hemonorte.model.enums.StatusBolsa
import java.time.Instant
import java.util.UUID

data class HistoricoAuditoriaResponse(
    val id: UUID,
    val bolsaId: UUID,
    val funcionarioId: UUID?,
    val statusAnterior: StatusBolsa,
    val statusNovo: StatusBolsa,
    val motivo: String?,
    val dataAlteracao: Instant,
)

fun HistoricoAuditoria.toResponse() = HistoricoAuditoriaResponse(
    id = id,
    bolsaId = bolsa.id,
    funcionarioId = funcionario?.id,
    statusAnterior = statusAnterior,
    statusNovo = statusNovo,
    motivo = motivo,
    dataAlteracao = dataAlteracao,
)
