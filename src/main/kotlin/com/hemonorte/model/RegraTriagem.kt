package com.hemonorte.model

import java.math.BigDecimal

data class ResultadoTriagem(
    val apto: Boolean,
    val motivoInaptidao: String?,
)

object RegraTriagem {

    val PESO_MINIMO: BigDecimal = BigDecimal("50.0")

    fun validarPeso(valor: BigDecimal): BigDecimal {
        require(valor > BigDecimal.ZERO) { "Peso deve ser positivo" }
        return valor
    }

    fun avaliar(
        peso: BigDecimal,
        tatuagemRecente: Boolean,
        cirurgiaRecente: Boolean,
        teveFebre: Boolean,
    ): ResultadoTriagem {
        val motivos = buildList {
            if (peso < PESO_MINIMO) add("peso abaixo do mínimo (${PESO_MINIMO}kg)")
            if (tatuagemRecente) add("tatuagem recente")
            if (cirurgiaRecente) add("cirurgia recente")
            if (teveFebre) add("febre recente")
        }
        return ResultadoTriagem(motivos.isEmpty(), motivos.takeIf { it.isNotEmpty() }?.joinToString("; "))
    }
}
