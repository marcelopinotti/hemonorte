package com.hemonorte.domain

import com.hemonorte.domain.enums.StatusBolsa
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "bolsa_sangue")
class BolsaSangue(
    agendamento: Agendamento,
    tipagem: String,
    dataColeta: LocalDate,
    dataValidade: LocalDate,

    @Id
    val id: UUID = UUID.randomUUID(),
) {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false, updatable = false, unique = true)
    var agendamento: Agendamento = agendamento
        protected set

    @Column(nullable = false, length = 3)
    var tipagem: String = validarTipagem(tipagem)
        protected set

    @Column(name = "data_coleta", nullable = false)
    var dataColeta: LocalDate = dataColeta
        protected set

    @Column(name = "data_validade", nullable = false)
    var dataValidade: LocalDate = validarValidade(dataColeta, dataValidade)
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StatusBolsa = StatusBolsa.EM_ANALISE
        protected set


    fun transicionarPara(novoStatus: StatusBolsa) {
        val transicoesPermitidas = mapOf(
            StatusBolsa.EM_ANALISE  to setOf(StatusBolsa.DISPONIVEL, StatusBolsa.DESCARTADA),
            StatusBolsa.DISPONIVEL  to setOf(StatusBolsa.UTILIZADA, StatusBolsa.DESCARTADA, StatusBolsa.VENCIDA),
            StatusBolsa.UTILIZADA   to emptySet(),
            StatusBolsa.DESCARTADA  to emptySet(),
            StatusBolsa.VENCIDA     to emptySet(),
        )
        require(novoStatus in (transicoesPermitidas[status] ?: emptySet())) {
            "Transição de $status para $novoStatus não é permitida"
        }
        status = novoStatus
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BolsaSangue) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "BolsaSangue(id=$id)"

    private companion object {

        val TIPAGENS_VALIDAS = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

        fun validarTipagem(valor: String): String {
            require(valor.uppercase() in TIPAGENS_VALIDAS) { "Tipagem sanguínea inválida: $valor" }
            return valor.uppercase()
        }

        fun validarValidade(coleta: LocalDate, validade: LocalDate): LocalDate {
            require(validade.isAfter(coleta)) { "Data de validade deve ser posterior à data de coleta" }
            return validade
        }
    }
}
