package com.hemonorte.repository

import com.hemonorte.model.SlotAgenda
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface SlotAgendaRepository : JpaRepository<SlotAgenda, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    override fun findById(id: UUID): Optional<SlotAgenda>

    fun existsByInicio(inicio: Instant): Boolean
    fun findByInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
        inicioInclusivo: Instant,
        fimExclusivo: Instant,
    ): List<SlotAgenda>
}
