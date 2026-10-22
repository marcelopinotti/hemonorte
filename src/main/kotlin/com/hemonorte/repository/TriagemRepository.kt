package com.hemonorte.repository

import com.hemonorte.model.Triagem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface TriagemRepository : JpaRepository<Triagem, UUID> {
    fun findByAgendamentoId(agendamentoId: UUID): Optional<Triagem>
    fun existsByAgendamentoId(agendamentoId: UUID): Boolean
}