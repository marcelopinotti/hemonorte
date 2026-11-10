package com.hemonorte.repository

import com.hemonorte.model.BolsaSangue
import com.hemonorte.model.enums.StatusBolsa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface BolsaSangueRepository : JpaRepository<BolsaSangue, UUID> {
    fun findByDataValidadeBeforeAndStatus(data: LocalDate, status: StatusBolsa): List<BolsaSangue>
    fun findByStatus(status: StatusBolsa): List<BolsaSangue>

    @Query(
        "select b.tipagem, count(b) from BolsaSangue b " +
            "where b.status = com.hemonorte.model.enums.StatusBolsa.DISPONIVEL and b.dataValidade >= :hoje " +
            "group by b.tipagem",
    )
    fun contarDisponiveisPorTipagem(hoje: LocalDate): List<Array<Any>>
}
