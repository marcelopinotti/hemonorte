package com.hemonorte.dto

import com.hemonorte.model.BolsaSangue
import com.hemonorte.model.enums.StatusBolsa
import java.time.LocalDate
import java.util.UUID

data class BolsaSangueResponse(
    val id: UUID,
    val agendamentoId: UUID,
    val tipagem: String,
    val dataColeta: LocalDate,
    val dataValidade: LocalDate,
    val status: StatusBolsa,
)

fun BolsaSangue.toResponse() = BolsaSangueResponse(
    id = id,
    agendamentoId = agendamento.id,
    tipagem = tipagem,
    dataColeta = dataColeta,
    dataValidade = dataValidade,
    status = status,
)
