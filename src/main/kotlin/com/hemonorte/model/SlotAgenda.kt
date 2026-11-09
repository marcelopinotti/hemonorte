package com.hemonorte.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "slot_agenda")
class SlotAgenda(
    inicio: Instant,
    capacidade: Int,

    @Id
    val id: UUID = UUID.randomUUID(),
    referencia: Instant = Instant.now(),
) {

    @Column(nullable = false, unique = true)
    var inicio: Instant = validarInicio(inicio, referencia)
        protected set

    @Column(nullable = false)
    var capacidade: Int = validarCapacidade(capacidade)
        protected set

    fun atualizar(
        novoInicio: Instant,
        novaCapacidade: Int,
        ocupacao: Long = 0,
        referencia: Instant = Instant.now(),
    ) {
        require(novaCapacidade.toLong() >= ocupacao) {
            "Capacidade não pode ser menor que as reservas pendentes"
        }
        inicio = validarInicio(novoInicio, referencia)
        capacidade = validarCapacidade(novaCapacidade)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SlotAgenda) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "SlotAgenda(id=$id)"

    private companion object {

        fun validarInicio(valor: Instant, referencia: Instant): Instant {
            require(valor.isAfter(referencia)) { "O início do slot deve ser uma data futura" }
            return valor
        }

        fun validarCapacidade(valor: Int): Int {
            require(valor > 0) { "Capacidade deve ser maior que zero" }
            return valor
        }
    }
}
