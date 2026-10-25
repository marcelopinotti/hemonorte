package com.hemonorte.dto

import com.hemonorte.model.enums.PerfilFuncionario
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AtualizacaoFuncionarioRequest(
    @field:NotBlank
    val nome: String,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotNull
    val perfil: PerfilFuncionario,
)
