package com.hemonorte.repository

import com.hemonorte.model.Funcionario
import com.hemonorte.model.enums.PerfilFuncionario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FuncionarioRepository: JpaRepository<Funcionario, UUID> {
    fun findByMatricula(matricula: String): Funcionario?
    fun existsByPerfil(perfil: PerfilFuncionario): Boolean
    fun countByPerfil(perfil: PerfilFuncionario): Long
    fun existsByMatricula(matricula: String): Boolean
    fun existsByEmail(email: String): Boolean
}
