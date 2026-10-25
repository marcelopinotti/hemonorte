package com.hemonorte.repository

import com.hemonorte.model.BolsaSangue
import com.hemonorte.model.enums.StatusBolsa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface BolsaSangueRepository : JpaRepository<BolsaSangue, UUID> {
    fun findByDataValidadeBeforeAndStatus(data: LocalDate, status: StatusBolsa): List<BolsaSangue>
}
