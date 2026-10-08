package com.hemonorte.domain

import com.hemonorte.domain.enums.StatusAgendamento
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.*

@Entity
@Table(name = "agendamento")
class Agendamento(
    doador: Doador,
    slot: SlotAgenda,

    @Id
    val id: UUID = UUID.randomUUID(),
) {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doador_id", nullable = false, updatable = false)
    var doador: Doador = doador
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false, updatable = false)
    var slot: SlotAgenda = slot
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StatusAgendamento = StatusAgendamento.PENDENTE
        protected set

    @Column(name = "codigo_comprovante", unique = true)
    var codigoComprovante: UUID? = null
        protected set

    @Version
    @Column(nullable = false)
    var version: Int = 0
        protected set


    fun concluir() {
        require(status == StatusAgendamento.PENDENTE) {
            "Só é possível concluir um agendamento PENDENTE"
        }
        status = StatusAgendamento.CONCLUIDO
        codigoComprovante = UUID.randomUUID()
    }

    fun cancelar() {
        require(status == StatusAgendamento.PENDENTE) {
            "Só é possível cancelar um agendamento PENDENTE"
        }
        status = StatusAgendamento.CANCELADO
    }

    fun registrarFalta() {
        require(status == StatusAgendamento.PENDENTE) {
            "Só é possível registrar falta em agendamento PENDENTE"
        }
        status = StatusAgendamento.FALTOU
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Agendamento) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Agendamento(id=$id)"
}
