package com.hemonorte.model

import java.time.LocalDate

data class ResultadoAptidao(
    val apto: Boolean,
    val motivo: String?,
    val proximaDataPermitida: LocalDate?,
)

object RegraAptidaoDoacao {

    fun avaliar(
        genero: Char,
        historicoConcluido: List<LocalDate>,
        dataReferencia: LocalDate,
    ): ResultadoAptidao {
        val parametros = when (genero) {
            'M' -> Parametros(intervaloDias = 60, limite365Dias = 4)
            'F' -> Parametros(intervaloDias = 90, limite365Dias = 3)
            else -> return impedido("Critério pessoal não reconhecido", null)
        }
        val datas = historicoConcluido
            .filterNot { it.isAfter(dataReferencia) }
            .sorted()
        if (datas.isEmpty()) return apto()

        val liberacaoIntervalo = datas.last().plusDays(parametros.intervaloDias)
            .takeIf { dataReferencia.isBefore(it) }
        val datasNaJanela = datas.filter { it.isAfter(dataReferencia.minusDays(365)) }
        val liberacaoLimite = if (datasNaJanela.size >= parametros.limite365Dias) {
            datasNaJanela[datasNaJanela.size - parametros.limite365Dias].plusDays(365)
        } else {
            null
        }

        val proximaData = listOfNotNull(liberacaoIntervalo, liberacaoLimite).maxOrNull()
            ?: return apto()
        val motivo = when {
            liberacaoIntervalo != null && liberacaoLimite != null ->
                "Intervalo mínimo e limite de doações em 365 dias não cumpridos"
            liberacaoIntervalo != null -> "Intervalo mínimo entre doações não cumprido"
            else -> "Limite de doações em 365 dias atingido"
        }
        return impedido(motivo, proximaData)
    }

    private fun apto() = ResultadoAptidao(true, null, null)

    private fun impedido(motivo: String, proximaData: LocalDate?) =
        ResultadoAptidao(false, motivo, proximaData)

    private data class Parametros(
        val intervaloDias: Long,
        val limite365Dias: Int,
    )
}
