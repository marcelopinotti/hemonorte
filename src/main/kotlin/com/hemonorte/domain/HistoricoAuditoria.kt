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
import java.time.Instant
import java.util.*

@Entity
@Table(name = "historico_auditoria")
class HistoricoAuditoria(
    bolsa: BolsaSangue,
    funcionario: Funcionario?,
    statusAnterior: StatusBolsa,
    statusNovo: StatusBolsa,
    motivo: String?,

    @Id
    val id: UUID = UUID.randomUUID(),
) {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bolsa_id", nullable = false, updatable = false)
    val bolsa: BolsaSangue = bolsa

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "funcionario_id", nullable = true, updatable = false)
    val funcionario: Funcionario? = funcionario

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false, updatable = false, length = 20)
    val statusAnterior: StatusBolsa = statusAnterior

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, updatable = false, length = 20)
    val statusNovo: StatusBolsa = statusNovo

    @Column(name = "data_alteracao", nullable = false, updatable = false)
    val dataAlteracao: Instant = Instant.now()

    @Column(updatable = false)
    val motivo: String? = motivo


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistoricoAuditoria) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "HistoricoAuditoria(id=$id)"
}
