package com.hemonorte.infra

import com.hemonorte.domain.EstoqueMinimo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface EstoqueMinimoRepository : JpaRepository<EstoqueMinimo, UUID> {
    fun findByTipagem(tipagem: String): EstoqueMinimo?
}