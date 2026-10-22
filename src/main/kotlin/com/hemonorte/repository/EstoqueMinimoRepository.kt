package com.hemonorte.repository

import com.hemonorte.model.EstoqueMinimo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EstoqueMinimoRepository : JpaRepository<EstoqueMinimo, String> {
}