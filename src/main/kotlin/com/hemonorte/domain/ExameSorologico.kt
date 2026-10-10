package com.hemonorte.domain

import com.hemonorte.domain.enums.ResultadoFinal
import com.hemonorte.domain.enums.ResultadoSorologico
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
@Table(name = "exame_sorologico")
class ExameSorologico(
    bolsa: BolsaSangue,
    funcionario: Funcionario,
    hiv: ResultadoSorologico,
    hepatite: ResultadoSorologico,
    chagas: ResultadoSorologico,

    @Id
    val id: UUID = UUID.randomUUID(),
) {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bolsa_id", nullable = false, updatable = false, unique = true)
    var bolsa: BolsaSangue = bolsa
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false, updatable = false)
    var funcionario: Funcionario = funcionario
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    var hiv: ResultadoSorologico = hiv
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    var hepatite: ResultadoSorologico = hepatite
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    var chagas: ResultadoSorologico = chagas
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_final", nullable = false, length = 10)
    var resultadoFinal: ResultadoFinal = calcularResultado(hiv, hepatite, chagas)
        protected set

    @Column(name = "data_resultado", nullable = false)
    var dataResultado: Instant = Instant.now()
        protected set


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExameSorologico) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "ExameSorologico(id=$id, resultado=$resultadoFinal)"

    private companion object {

        fun calcularResultado(
            hiv: ResultadoSorologico,
            hepatite: ResultadoSorologico,
            chagas: ResultadoSorologico,
        ): ResultadoFinal =
            if (hiv == ResultadoSorologico.NEGATIVO &&
                hepatite == ResultadoSorologico.NEGATIVO &&
                chagas == ResultadoSorologico.NEGATIVO
            ) ResultadoFinal.APROVADO
            else ResultadoFinal.REPROVADO
    }
}
