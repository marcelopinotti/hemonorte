package com.hemonorte.repository

import com.hemonorte.model.Doador
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DoadorRepository : JpaRepository<Doador, UUID> {
    fun existsByCpf(cpf: String): Boolean
    fun existsByEmail(email: String): Boolean
}
