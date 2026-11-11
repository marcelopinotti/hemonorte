package com.hemonorte.repository

import com.hemonorte.model.PreTriagem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PreTriagemRepository : JpaRepository<PreTriagem, UUID> {
    fun findFirstByDoadorIdOrderByRespondidaEmDesc(doadorId: UUID): PreTriagem?
}
