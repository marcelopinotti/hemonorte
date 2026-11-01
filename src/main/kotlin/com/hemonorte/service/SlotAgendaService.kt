package com.hemonorte.service

import com.hemonorte.model.SlotAgenda
import com.hemonorte.repository.SlotAgendaRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import org.springframework.transaction.annotation.Transactional

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
