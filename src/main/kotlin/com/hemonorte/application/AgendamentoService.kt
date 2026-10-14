package com.hemonorte.application

import com.hemonorte.domain.Agendamento
import com.hemonorte.domain.enums.StatusAgendamento
import com.hemonorte.infra.AgendamentoRepository
import com.hemonorte.infra.DoadorRepository
import com.hemonorte.infra.SlotAgendaRepository
import com.hemonorte.presentation.dto.AgendamentoRequest
import com.hemonorte.presentation.dto.AgendamentoResponse
import com.hemonorte.presentation.dto.toResponse
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.temporal.ChronoUnit

@Service
class AgendamentoService(
    private val agendamentoRepository: AgendamentoRepository,
    private val doadorRepository: DoadorRepository,
    private val slotAgendaRepository: SlotAgendaRepository
) {

    @Transactional
    fun criar(request: AgendamentoRequest): AgendamentoResponse {
        val doador = doadorRepository.findById(request.doadorId)
            .orElseThrow { IllegalArgumentException("Doador não encontrado") }

        val slot = slotAgendaRepository.findById(request.slotId)
            .orElseThrow { IllegalArgumentException("Slot de agenda não encontrado") }

        // Validação da regra do Hemonorte: intervalo obrigatório entre doações[cite: 2]
        val historicoDoacoes = agendamentoRepository.findByDoadorId(doador.id)
            .filter { it.status == StatusAgendamento.CONCLUIDO }
            .maxByOrNull { it.slot.inicio }

        if (historicoDoacoes != null) {
            val diasPassados = ChronoUnit.DAYS.between(historicoDoacoes.slot.inicio, slot.inicio)
            val intervaloObrigatorio = if (doador.genero == 'M') 60 else 90

            require(diasPassados >= intervaloObrigatorio) {
                "O doador deve respeitar o intervalo obrigatório de $intervaloObrigatorio dias. Dias passados: $diasPassados."
            }
        }

        val agendamento = Agendamento(
            doador = doador,
            slot = slot
        )

        val salvo = agendamentoRepository.save(agendamento)
        return salvo.toResponse()
    }

    @Transactional
    fun listar(): List<AgendamentoResponse> {
        return agendamentoRepository.findAll().map { it.toResponse() }
    }
}