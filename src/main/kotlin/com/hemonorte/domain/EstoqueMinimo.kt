package com.hemonorte.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "estoque_minimo")
class EstoqueMinimo(
    tipagem: String,
    quantidadeCritica: Int,
) {

    @Id
    @Column(length = 3)
    val tipagem: String = validarTipagem(tipagem)

    @Column(name = "quantidade_critica", nullable = false)
    var quantidadeCritica: Int = validarQuantidade(quantidadeCritica)
        protected set

    fun atualizarQuantidade(novaQuantidade: Int) {
        quantidadeCritica = validarQuantidade(novaQuantidade)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EstoqueMinimo) return false
        return tipagem == other.tipagem
    }

    override fun hashCode(): Int = tipagem.hashCode()

    override fun toString(): String = "EstoqueMinimo(tipagem=$tipagem)"

    private companion object {

        val TIPAGENS_VALIDAS = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

        fun validarTipagem(valor: String): String {
            require(valor.uppercase() in TIPAGENS_VALIDAS) { "Tipagem sanguínea inválida: $valor" }
            return valor.uppercase()
        }

        fun validarQuantidade(valor: Int): Int {
            require(valor > 0) { "Quantidade crítica deve ser maior que zero" }
            return valor
        }
    }
}
