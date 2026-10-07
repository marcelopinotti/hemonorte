package com.hemonorte.infra

import com.hemonorte.domain.Doador
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DoadorRepository : JpaRepository<Doador, UUID> {
    fun existsByCpf(cpf: String): Boolean
    fun existsByEmail(email: String): Boolean
}