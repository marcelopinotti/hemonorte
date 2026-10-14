package com.hemonorte.dto

import com.hemonorte.model.Doador
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DoadorResponse(
    val id: UUID,
    val nome: String,
    val email: String,
    val cpf: String,
    val tipagem: String,
    val dataNascimento: LocalDate,
    val genero: Char,
    val criadoEm: Instant,
)

fun Doador.toResponse() = DoadorResponse(
    id = id,
    nome = nome,
    email = email,
    cpf = cpf,
    tipagem = tipagem,
    dataNascimento = dataNascimento,
    genero = genero,
    criadoEm = criadoEm,
)
