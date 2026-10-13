package com.hemonorte.dto

import com.hemonorte.model.enums.PerfilFuncionario
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CadastroFuncionarioRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val matricula: String,

    @field:NotBlank
    val nome: String,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val senhaHash: String,

    @field:NotNull
    val perfil: PerfilFuncionario,
)
