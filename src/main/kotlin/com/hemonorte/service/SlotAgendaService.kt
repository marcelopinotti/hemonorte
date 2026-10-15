package com.hemonorte.service

import com.hemonorte.model.SlotAgenda
import com.hemonorte.repository.SlotAgendaRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SlotAgendaService(
    private val slotAgendaRepository: SlotAgendaRepository
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

    fun listarPorPeriodo(inicio: Instant, fim: Instant): List<SlotAgenda> {
        return slotAgendaRepository.findByInicioBetween(inicio, fim)
    }
}