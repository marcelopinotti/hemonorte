package com.hemonorte.dto

import com.hemonorte.model.Funcionario
import com.hemonorte.model.enums.PerfilFuncionario
import java.util.UUID

data class FuncionarioResponse(
    val id: UUID,
    val matricula: String,
    val nome: String,
    val email: String,
    val perfil: PerfilFuncionario,
    val ativo: Boolean,
)

fun Funcionario.toResponse() = FuncionarioResponse(
    id = id,
    matricula = matricula,
    nome = nome,
    email = email,
    perfil = perfil,
    ativo = ativo,
)
