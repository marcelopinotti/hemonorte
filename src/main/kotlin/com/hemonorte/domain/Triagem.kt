package com.hemonorte.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "triagem")
class Triagem(
    agendamento: Agendamento,
    funcionario: Funcionario,
    peso: BigDecimal,
    tatuagemRecente: Boolean,
    cirurgiaRecente: Boolean,
    teveFebre: Boolean,

    @Id
    val id: UUID = UUID.randomUUID(),
) {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, updatable = false, unique = true)
    var agendamento: Agendamento = agendamento
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false, updatable = false)
    var funcionario: Funcionario = funcionario
        protected set

    @Column(nullable = false, precision = 5, scale = 2)
    var peso: BigDecimal = validarPeso(peso)
        protected set

    @Column(name = "tatuagem_recente", nullable = false)
    var tatuagemRecente: Boolean = tatuagemRecente
        protected set

    @Column(name = "cirurgia_recente", nullable = false)
    var cirurgiaRecente: Boolean = cirurgiaRecente
        protected set

    @Column(name = "teve_febre", nullable = false)
    var teveFebre: Boolean = teveFebre
        protected set

    @Column(nullable = false)
    var apto: Boolean = calcularAptidao(peso, tatuagemRecente, cirurgiaRecente, teveFebre)
        protected set

    @Column(name = "motivo_inaptidao")
    var motivoInaptidao: String? = gerarMotivoInaptidao(peso, tatuagemRecente, cirurgiaRecente, teveFebre)
        protected set


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Triagem) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Triagem(id=$id, apto=$apto)"

    private companion object {

        val PESO_MINIMO: BigDecimal = BigDecimal("50.0")

        fun validarPeso(valor: BigDecimal): BigDecimal {
            require(valor > BigDecimal.ZERO) { "Peso deve ser positivo" }
            return valor
        }

        fun calcularAptidao(
            peso: BigDecimal,
            tatuagemRecente: Boolean,
            cirurgiaRecente: Boolean,
            teveFebre: Boolean,
        ): Boolean =
            peso >= PESO_MINIMO && !tatuagemRecente && !cirurgiaRecente && !teveFebre

        fun gerarMotivoInaptidao(
            peso: BigDecimal,
            tatuagemRecente: Boolean,
            cirurgiaRecente: Boolean,
            teveFebre: Boolean,
        ): String? {
            val motivos = buildList {
                if (peso < PESO_MINIMO) add("peso abaixo do mínimo (${PESO_MINIMO}kg)")
                if (tatuagemRecente) add("tatuagem recente")
                if (cirurgiaRecente) add("cirurgia recente")
                if (teveFebre) add("febre recente")
            }
            return if (motivos.isEmpty()) null else motivos.joinToString("; ")
        }
    }
}
