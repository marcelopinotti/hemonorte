package com.hemonorte.service

import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.Triagem
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.TriagemRepository
import org.springframework.security.access.AccessDeniedException
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
        val funcionario = funcionarioRepository.findById(funcionarioId)
            .orElseThrow { AccessDeniedException("Acesso negado") }
        if (!funcionario.ativo) throw AccessDeniedException("Acesso negado")

        val agendamento = agendamentoRepository.findById(agendamentoId)
            .orElseThrow { NoSuchElementException("Agendamento não encontrado") }

        if (agendamento.status != StatusAgendamento.PENDENTE) {
            throw ConflitoException("Não é possível realizar triagem para um agendamento com status: ${agendamento.status}")
        }
        if (triagemRepository.existsByAgendamentoId(agendamentoId)) {
            throw ConflitoException("Já existe uma triagem registrada para este agendamento")
        }

        return triagemRepository.saveAndFlush(
            Triagem(agendamento, funcionario, peso, tatuagemRecente, cirurgiaRecente, teveFebre),
        )
    }

    @Transactional(readOnly = true)
    fun buscarPorAgendamento(agendamentoId: UUID): Triagem =
        triagemRepository.findByAgendamentoId(agendamentoId)
            .orElseThrow { NoSuchElementException("Triagem não encontrada para este agendamento") }

    @Transactional(readOnly = true)
    fun listar(): List<Triagem> = triagemRepository.findAll()
}
