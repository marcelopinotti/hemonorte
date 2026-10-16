package com.hemonorte.repository

import com.hemonorte.model.BolsaSangue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface BolsaSangueRepository : JpaRepository<BolsaSangue, UUID> {

    @Query("SELECT b BolsaSangue b WHERE b.validade < :agora AND b.status = 'DISPONIVEL'")
    fun findBolsasVencidasParaExpirar(agora: Instant): List<BolsaSangue>
}