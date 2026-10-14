package com.hemonorte.dto

import com.hemonorte.model.enums.PerfilFuncionario
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank


data class FuncionarioRequest(
    @field:NotBlank
    val nome: String,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val senhaHash: String,

    @field:NotBlank
    val perfil: PerfilFuncionario

)
