package com.hemonorte.infra

import com.hemonorte.domain.Triagem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TriagemRepository : JpaRepository<Triagem, UUID> {
    fun findByDoadorId(doadorId: UUID): List<Triagem>
}