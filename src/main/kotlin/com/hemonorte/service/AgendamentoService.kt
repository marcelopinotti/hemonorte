package com.hemonorte.service

import com.hemonorte.model.Agendamento
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.SlotAgendaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AgendamentoService(
    private val agendamentoRepository: AgendamentoRepository,
    private val doadorRepository: DoadorRepository,
    private val slotAgendaRepository: SlotAgendaRepository,
    private val aptidaoService: AptidaoService
) {

    @Transactional
    fun realizarAgendamento(doadorId: UUID, slotId: UUID): Agendamento {
        val doador = doadorRepository.findById(doadorId)
            .orElseThrow { IllegalArgumentException("Doador não encontrado") }

        val slot = slotAgendaRepository.findById(slotId)
            .orElseThrow { IllegalArgumentException("Slot de agenda não encontrado") }

        // Valida se o doador está apto para doar
        val resultadoAptidao = aptidaoService.verificarAptidao(doador)
        require(resultadoAptidao.apto) {
            "Doador inaptado: ${resultadoAptidao.motivo}"
        }

        // Valida capacidade do slot (conta quantos agendamentos ativos existem para este slot)
        val agendamentosNoSlot = agendamentoRepository.findBySlotIdAndStatus(slotId, StatusAgendamento.PENDENTE)
        require(agendamentosNoSlot.size < slot.capacidade) {
            "Slot lotado para este horário"
        }

        // Cria o agendamento
        val agendamento = Agendamento(doador = doador, slot = slot)
        return agendamentoRepository.save(agendamento)
    }

    @Transactional
    fun cancelarAgendamento(agendamentoId: UUID): Agendamento {
        val agendamento = agendamentoRepository.findById(agendamentoId)
            .orElseThrow { IllegalArgumentException("Agendamento não encontrado") }

        agendamento.cancelar()
        return agendamentoRepository.save(agendamento)
    }

    @Transactional
    fun registrarFalta(agendamentoId: UUID): Agendamento {
        val agendamento = agendamentoRepository.findById(agendamentoId)
            .orElseThrow { IllegalArgumentException("Agendamento não encontrado") }

        agendamento.registrarFalta()
        return agendamentoRepository.save(agendamento)
    }
}