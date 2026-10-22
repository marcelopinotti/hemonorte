
package com.hemonorte.service

import com.hemonorte.dto.SlotDisponibilidadeResponse
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.SlotAgendaRepository
import org.springframework.stereotype.Service
import java.time.Instant

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

    fun listarPorPeriodo(
        inicio: Instant,
        fim: Instant
    ): List<SlotDisponibilidadeResponse> {
        return slotAgendaRepository.findByInicioBetween(inicio, fim)
            .map { slot ->
                val ocupados = agendamentoRepository
                    .findBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
                    .size

                SlotDisponibilidadeResponse(
                    id = slot.id,
                    inicio = slot.inicio,
                    capacidade = slot.capacidade,
                    vagasDisponiveis = (slot.capacidade - ocupados).coerceAtLeast(0)
                )
            }
            .filter { it.vagasDisponiveis > 0 }
    }
}
