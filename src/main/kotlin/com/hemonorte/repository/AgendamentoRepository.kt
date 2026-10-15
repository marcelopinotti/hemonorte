package com.hemonorte.repository

import com.hemonorte.model.Agendamento
import com.hemonorte.model.enums.StatusAgendamento
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgendamentoRepository : JpaRepository<Agendamento, UUID> {
    fun findByDoadorIdAndStatus(doadorId: UUID, status: StatusAgendamento): List<Agendamento>
    fun findBySlotIdAndStatus(slotId: UUID, status: StatusAgendamento): List<Agendamento>
}