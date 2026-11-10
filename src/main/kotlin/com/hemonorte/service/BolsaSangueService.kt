package com.hemonorte.service

import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.BolsaSangue
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.model.enums.StatusBolsa
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.TriagemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BolsaSangueService(
    private val bolsaRepository: BolsaSangueRepository,
    private val agendamentoRepository: AgendamentoRepository,
    private val triagemRepository: TriagemRepository,
) {
    fun listar(): List<BolsaSangue> = bolsaRepository.findAll()

    fun listarPendentes(): List<BolsaSangue> = bolsaRepository.findByStatus(StatusBolsa.EM_ANALISE)

    @Transactional
    fun criar(
        agendamentoId: UUID,
        tipagem: String,
        dataColeta: LocalDate,
        dataValidade: LocalDate,
    ): BolsaSangue {
        val agendamento = agendamentoRepository.findById(agendamentoId)
            .orElseThrow { NoSuchElementException("Agendamento não encontrado") }
        if (agendamento.status != StatusAgendamento.PENDENTE) {
            throw ConflitoException("Coleta exige agendamento PENDENTE; status atual: ${agendamento.status}")
        }
        val triagem = triagemRepository.findByAgendamentoId(agendamentoId)
            .orElseThrow { ConflitoException("Coleta exige triagem registrada") }
        if (!triagem.apto) throw ConflitoException("Triagem inapta impede a coleta")
        agendamento.concluir()
        return bolsaRepository.saveAndFlush(BolsaSangue(agendamento, tipagem, dataColeta, dataValidade))
    }

    @Transactional
    fun atualizarStatus(id: UUID, status: StatusBolsa): BolsaSangue {
        val bolsa = bolsaRepository.findById(id).orElseThrow { NoSuchElementException("Bolsa não encontrada") }
        bolsa.transicionarPara(status)
        return bolsaRepository.save(bolsa)
    }
}
