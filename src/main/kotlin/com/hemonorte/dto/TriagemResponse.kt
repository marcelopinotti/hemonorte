package com.hemonorte.dto

import com.hemonorte.model.Triagem
import java.math.BigDecimal
import java.util.UUID

data class TriagemResponse(
    val id: UUID,
    val agendamentoId: UUID,
    val funcionarioId: UUID,
    val peso: BigDecimal,
    val tatuagemRecente: Boolean,
    val cirurgiaRecente: Boolean,
    val teveFebre: Boolean,
    val apto: Boolean,
    val motivoInaptidao: String?,
)

fun Triagem.toResponse() = TriagemResponse(
    id = id,
    agendamentoId = agendamento.id,
    funcionarioId = funcionario.id,
    peso = peso,
    tatuagemRecente = tatuagemRecente,
    cirurgiaRecente = cirurgiaRecente,
    teveFebre = teveFebre,
    apto = apto,
    motivoInaptidao = motivoInaptidao,
)
