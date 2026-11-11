package com.hemonorte.service

import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.BolsaSangue
import com.hemonorte.model.HistoricoAuditoria
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.model.enums.StatusBolsa
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.HistoricoAuditoriaRepository
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
    private val funcionarioRepository: FuncionarioRepository,
    private val historicoAuditoriaRepository: HistoricoAuditoriaRepository,
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
    fun atualizarStatus(id: UUID, status: StatusBolsa, motivo: String?, funcionarioId: UUID): BolsaSangue {
        val bolsa = buscar(id)
        val funcionario = funcionarioRepository.findById(funcionarioId)
            .orElseThrow { NoSuchElementException("Funcionário não encontrado") }
        historicoAuditoriaRepository.save(bolsa.transicionarPara(status, funcionario, motivo))
        return bolsaRepository.save(bolsa)
    }

    fun historico(id: UUID): List<HistoricoAuditoria> {
        buscar(id)
        return historicoAuditoriaRepository.findByBolsaIdOrderByDataAlteracao(id)
    }

    private fun buscar(id: UUID): BolsaSangue =
        bolsaRepository.findById(id).orElseThrow { NoSuchElementException("Bolsa não encontrada") }
}
