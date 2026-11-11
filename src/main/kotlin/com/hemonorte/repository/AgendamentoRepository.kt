package com.hemonorte.repository

import com.hemonorte.model.Agendamento
import com.hemonorte.model.enums.StatusAgendamento
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.Optional

@Repository
interface AgendamentoRepository : JpaRepository<Agendamento, UUID> {
    fun findByDoadorId(doadorId: UUID): List<Agendamento>
    fun findByCodigoComprovante(codigoComprovante: UUID): Agendamento?
    fun findByIdAndDoadorId(id: UUID, doadorId: UUID): Optional<Agendamento>
    fun findByDoadorIdAndStatus(doadorId: UUID, status: StatusAgendamento): List<Agendamento>
    fun findBySlotIdAndStatus(slotId: UUID, status: StatusAgendamento): List<Agendamento>
    fun countBySlotIdAndStatus(slotId: UUID, status: StatusAgendamento): Long
    fun existsByDoadorIdAndSlotIdAndStatus(doadorId: UUID, slotId: UUID, status: StatusAgendamento): Boolean
}
