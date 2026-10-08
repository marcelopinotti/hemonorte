package com.hemonorte.domain

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
) {

    @Column(nullable = false, unique = true)
    var inicio: Instant = validarInicio(inicio)
        protected set

    @Column(nullable = false)
    var capacidade: Int = validarCapacidade(capacidade)
        protected set


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SlotAgenda) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "SlotAgenda(id=$id)"

    private companion object {

        fun validarInicio(valor: Instant): Instant {
            require(valor.isAfter(Instant.now())) { "O início do slot deve ser uma data futura" }
            return valor
        }

        fun validarCapacidade(valor: Int): Int {
            require(valor > 0) { "Capacidade deve ser maior que zero" }
            return valor
        }
    }
}
