package com.hemonorte.service

import com.hemonorte.dto.SlotDisponibilidadeResponse
import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.SlotAgendaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class SlotAgendaService(
    private val slotAgendaRepository: SlotAgendaRepository,
    private val agendamentoRepository: AgendamentoRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun criarSlot(inicio: Instant, capacidade: Int): SlotAgenda {
        if (slotAgendaRepository.existsByInicio(inicio)) {
            throw ConflitoException("Já existe um slot cadastrado para este horário")
        }
        return slotAgendaRepository.save(
            SlotAgenda(inicio, capacidade, referencia = clock.instant()),
        )
    }

    @Transactional(readOnly = true)
    fun listarPorPeriodo(inicio: Instant, fim: Instant): List<SlotDisponibilidadeResponse> {
        require(fim.isAfter(inicio)) { "O fim do período deve ser posterior ao início" }
        return slotAgendaRepository
            .findByInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(inicio, fim)
            .map { slot ->
                val ocupacao = agendamentoRepository
                    .countBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
                    .toInt()
                SlotDisponibilidadeResponse(
                    id = slot.id,
                    inicio = slot.inicio,
                    capacidade = slot.capacidade,
                    ocupacao = ocupacao,
                    vagasDisponiveis = (slot.capacidade - ocupacao).coerceAtLeast(0),
                )
            }
    }

    @Transactional
    fun atualizar(id: UUID, inicio: Instant, capacidade: Int): SlotAgenda {
        val slot = slotAgendaRepository.findById(id)
            .orElseThrow { NoSuchElementException("Slot não encontrado") }
        if (slot.inicio != inicio && slotAgendaRepository.existsByInicio(inicio)) {
            throw ConflitoException("Já existe um slot cadastrado para este horário")
        }
        val ocupacao = agendamentoRepository
            .countBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
        if (capacidade.toLong() < ocupacao) {
            throw ConflitoException("Capacidade não pode ser menor que as reservas pendentes")
        }
        slot.atualizar(inicio, capacidade, ocupacao, clock.instant())
        return slotAgendaRepository.save(slot)
    }

    @Transactional
    fun excluir(id: UUID) {
        val slot = slotAgendaRepository.findById(id)
            .orElseThrow { NoSuchElementException("Slot não encontrado") }
        slotAgendaRepository.delete(slot)
    }
}
