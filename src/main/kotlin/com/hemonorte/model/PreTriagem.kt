package com.hemonorte.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.*

enum class PerguntaPreTriagem(val texto: String) {
    PESO("Qual é o seu peso atual (kg)?"),
    TATUAGEM_RECENTE("Fez tatuagem, piercing ou maquiagem definitiva recentemente?"),
    CIRURGIA_RECENTE("Passou por cirurgia recentemente?"),
    TEVE_FEBRE("Teve febre recentemente?"),
}

@Entity
@Table(name = "pre_triagem")
class PreTriagem(
    doador: Doador,
    peso: BigDecimal,
    tatuagemRecente: Boolean,
    cirurgiaRecente: Boolean,
    teveFebre: Boolean,
    respondidaEm: Instant = Instant.now(),

    @Id
    val id: UUID = UUID.randomUUID(),
) {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doador_id", nullable = false, updatable = false)
    val doador: Doador = doador

    @Column(nullable = false, updatable = false, precision = 5, scale = 2)
    val peso: BigDecimal = RegraTriagem.validarPeso(peso)

    @Column(name = "tatuagem_recente", nullable = false, updatable = false)
    val tatuagemRecente: Boolean = tatuagemRecente

    @Column(name = "cirurgia_recente", nullable = false, updatable = false)
    val cirurgiaRecente: Boolean = cirurgiaRecente

    @Column(name = "teve_febre", nullable = false, updatable = false)
    val teveFebre: Boolean = teveFebre

    @Column(nullable = false, updatable = false)
    val apto: Boolean = RegraTriagem.avaliar(peso, tatuagemRecente, cirurgiaRecente, teveFebre).apto

    @Column(name = "motivo_inaptidao", updatable = false)
    val motivoInaptidao: String? =
        RegraTriagem.avaliar(peso, tatuagemRecente, cirurgiaRecente, teveFebre).motivoInaptidao

    @Column(name = "respondida_em", nullable = false, updatable = false)
    val respondidaEm: Instant = respondidaEm

    val validaAte: Instant
        get() = respondidaEm.plus(VALIDADE)

    fun respostas(): Map<PerguntaPreTriagem, Any> = mapOf(
        PerguntaPreTriagem.PESO to peso,
        PerguntaPreTriagem.TATUAGEM_RECENTE to tatuagemRecente,
        PerguntaPreTriagem.CIRURGIA_RECENTE to cirurgiaRecente,
        PerguntaPreTriagem.TEVE_FEBRE to teveFebre,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PreTriagem) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "PreTriagem(id=$id, apto=$apto)"

    companion object {
        val VALIDADE: Duration = Duration.ofDays(7)
    }
}
