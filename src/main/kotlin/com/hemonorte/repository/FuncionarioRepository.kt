package com.hemonorte.repository

import com.hemonorte.model.Funcionario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FuncionarioRepository: JpaRepository<Funcionario, UUID> {


    fun existsByMatricula(matricula: String): Boolean
    fun existsByEmail(email: String): Boolean
}