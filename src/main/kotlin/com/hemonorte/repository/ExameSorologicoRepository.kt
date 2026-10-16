package com.hemonorte.repository

import com.hemonorte.model.ExameSorologico
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ExameSorologicoRepository : JpaRepository<ExameSorologico, UUID> {
    fun findByBolsaId(bolsaId: UUID): Optional<ExameSorologico>
    fun existsByBolsaId(bolsaId: UUID): Boolean
}