package com.hemonorte.repository

import com.hemonorte.model.Coleta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ColetaRepository : JpaRepository<Coleta, UUID> {
    fun findByTriagemId(triagemId: UUID): Optional<Coleta>
    fun existsByTriagemId(triagemId: UUID): Boolean
}