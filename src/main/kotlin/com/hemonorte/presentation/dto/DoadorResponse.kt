package com.hemonorte.presentation.dto

import com.hemonorte.domain.Doador
import java.util.*

data class DoadorResponse(
    val id: UUID,
    val nome: String,
    val email: String,
)

fun Doador.toResponse() = DoadorResponse(id = id, nome = nome, email = email)