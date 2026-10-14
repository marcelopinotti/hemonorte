package com.hemonorte.infra

import com.hemonorte.domain.BolsaSangue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BolsaSangueRepository : JpaRepository<BolsaSangue, UUID> {
    fun findByTipagemAndStatus(tipagem: String, status: String): List<BolsaSangue>
    fun countByTipagemAndStatus(tipagem: String, status: String): Long
}