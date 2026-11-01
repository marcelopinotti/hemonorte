package com.hemonorte.dto

import com.hemonorte.model.ExameSorologico
import com.hemonorte.model.enums.ResultadoFinal
import com.hemonorte.model.enums.ResultadoSorologico
import java.time.Instant
import java.util.UUID

data class ExameSorologicoResponse(
    val id: UUID,
    val bolsaId: UUID,
    val funcionarioId: UUID,
    val hiv: ResultadoSorologico,
    val hepatite: ResultadoSorologico,
    val chagas: ResultadoSorologico,
    val resultadoFinal: ResultadoFinal,
    val dataResultado: Instant,
)

fun ExameSorologico.toResponse() = ExameSorologicoResponse(
    id = id,
    bolsaId = bolsa.id,
    funcionarioId = funcionario.id,
    hiv = hiv,
    hepatite = hepatite,
    chagas = chagas,
    resultadoFinal = resultadoFinal,
    dataResultado = dataResultado,
)
