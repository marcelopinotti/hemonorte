package com.hemonorte.dto

import com.hemonorte.model.PerguntaPreTriagem
import com.hemonorte.model.PreTriagem
import java.time.Instant
import java.util.UUID

data class PerguntaPreTriagemResponse(
    val codigo: PerguntaPreTriagem,
    val pergunta: String,
)

data class RespostaPreTriagemResponse(
    val codigo: PerguntaPreTriagem,
    val pergunta: String,
    val resposta: Any,
)

data class PreTriagemResponse(
    val id: UUID,
    val respondidaEm: Instant,
    val validaAte: Instant,
    val apto: Boolean,
    val motivoInaptidao: String?,
    val respostas: List<RespostaPreTriagemResponse>,
)

fun perguntasPreTriagem() = PerguntaPreTriagem.entries.map { PerguntaPreTriagemResponse(it, it.texto) }

fun PreTriagem.toResponse() = PreTriagemResponse(
    id = id,
    respondidaEm = respondidaEm,
    validaAte = validaAte,
    apto = apto,
    motivoInaptidao = motivoInaptidao,
    respostas = respostas().map { (pergunta, resposta) -> RespostaPreTriagemResponse(pergunta, pergunta.texto, resposta) },
)
