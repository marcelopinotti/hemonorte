package com.hemonorte.infra

import com.hemonorte.domain.Agendamento
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AgendamentoRepository : JpaRepository<Agendamento, UUID> {
    fun findByDoadorId(doadorId: UUID): List<Agendamento>
}