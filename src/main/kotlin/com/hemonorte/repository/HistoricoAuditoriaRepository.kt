package com.hemonorte.repository

import com.hemonorte.model.HistoricoAuditoria
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HistoricoAuditoriaRepository : JpaRepository<HistoricoAuditoria, UUID> {
    fun findByBolsaIdOrderByDataAlteracao(bolsaId: UUID): List<HistoricoAuditoria>
}
