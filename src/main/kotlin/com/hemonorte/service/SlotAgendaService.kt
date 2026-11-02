package com.hemonorte.service

import com.hemonorte.dto.SlotDisponibilidadeResponse
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.SlotAgendaRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import org.springframework.transaction.annotation.Transactional

@Service
class SlotAgendaService(
    private val slotAgendaRepository: SlotAgendaRepository,
    private val agendamentoRepository: AgendamentoRepository
) {

    fun criarSlot(inicio: Instant, capacidade: Int): SlotAgenda {
        require(!slotAgendaRepository.existsByInicio(inicio)) {
            "Já existe um slot cadastrado para este horário"
        }

        val slot = SlotAgenda(
            inicio = inicio,
            capacidade = capacidade
        )

        return slotAgendaRepository.save(slot)
    }

    fun listarPorPeriodo(inicio: Instant, fim: Instant): List<SlotDisponibilidadeResponse> =
        slotAgendaRepository.findByInicioBetween(inicio, fim)
            .map { slot ->
                val ocupados = agendamentoRepository
                    .findBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
                    .size

                SlotDisponibilidadeResponse(
                    id = slot.id,
                    inicio = slot.inicio,
                    capacidade = slot.capacidade,
                    vagasDisponiveis = (slot.capacidade - ocupados).coerceAtLeast(0),
                )
            }
            .filter { it.vagasDisponiveis > 0 }

    @Transactional
    fun atualizar(id: UUID, inicio: Instant, capacidade: Int): SlotAgenda {
        val slot = slotAgendaRepository.findById(id).orElseThrow { NoSuchElementException("Slot não encontrado") }
        if (slot.inicio != inicio) {
            require(!slotAgendaRepository.existsByInicio(inicio)) { "Já existe um slot cadastrado para este horário" }
        }
        slot.atualizar(inicio, capacidade)
        return slotAgendaRepository.save(slot)
    }

    @Transactional
    fun excluir(id: UUID) {
        val slot = slotAgendaRepository.findById(id).orElseThrow { NoSuchElementException("Slot não encontrado") }
        slotAgendaRepository.delete(slot)
    }
}
