package com.hemonorte.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "coleta")
class Coleta(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "triagem_id", nullable = false, updatable = false, unique = true)
    val triagem: Triagem,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false, updatable = false)
    val funcionario: Funcionario,

    @Column(nullable = false, precision = 5, scale = 2)
    val volumeMl: BigDecimal,

    @Column(name = "data_hora", nullable = false)
    val dataHora: LocalDateTime = LocalDateTime.now(),

    @Id
    val id: UUID = UUID.randomUUID(),
) {
    init {
        // Valida se o doador estava apto na triagem antes de permitir a coleta
        require(triagem.apto) {
            "Não é possível realizar a coleta: o doador foi considerado inapto na triagem."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Coleta) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Coleta(id=$id, volumeMl=$volumeMl, dataHora=$dataHora)"
}