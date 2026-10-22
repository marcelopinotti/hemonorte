package com.hemonorte.repository

import com.hemonorte.model.SlotAgenda
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface SlotAgendaRepository : JpaRepository<SlotAgenda, UUID> {
    fun existsByInicio(inicio: Instant): Boolean
    fun findByInicioBetween(inicioInicio: Instant, inicioFim: Instant): List<SlotAgenda>
}