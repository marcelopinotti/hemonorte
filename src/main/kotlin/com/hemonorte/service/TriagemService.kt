package com.hemonorte.service

import com.hemonorte.model.Triagem
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.TriagemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class TriagemService(
    private val triagemRepository: TriagemRepository,
    private val agendamentoRepository: AgendamentoRepository,
    private val funcionarioRepository: FuncionarioRepository
) {

    @Transactional
    fun realizarTriagem(
        agendamentoId: UUID,
        peso: BigDecimal,
        tatuagemRecente: Boolean,
        cirurgiaRecente: Boolean,
        teveFebre: Boolean,
        funcionarioId: UUID
    ): Triagem {

        val agendamento = agendamentoRepository.findById(agendamentoId)
            .orElseThrow { IllegalArgumentException("Agendamento não encontrado") }

        val funcionario = funcionarioRepository.findById(funcionarioId)
            .orElseThrow { IllegalArgumentException("Funcionário não encontrado") }

        require(agendamento.status == StatusAgendamento.PENDENTE) {
            "Não é possível realizar triagem para um agendamento com status: ${agendamento.status}"
        }

        require(!triagemRepository.existsByAgendamentoId(agendamentoId)) {
            "Já existe uma triagem registrada para este agendamento"
        }

        val triagem = Triagem(
            agendamento,
            funcionario,
            peso,
            tatuagemRecente,
            cirurgiaRecente,
            teveFebre
        )

        return triagemRepository.save(triagem)
    }

    fun buscarPorAgendamento(agendamentoId: UUID): Triagem {
        return triagemRepository.findByAgendamentoId(agendamentoId)
            .orElseThrow { IllegalArgumentException("Triagem não encontrada para este agendamento") }
    }
}