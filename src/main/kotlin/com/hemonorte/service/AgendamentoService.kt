package com.hemonorte.service

import com.hemonorte.model.Agendamento
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.exception.ConflitoException
import com.hemonorte.exception.InaptidaoDoacaoException
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.PreTriagemRepository
import com.hemonorte.model.ResultadoAptidao
import com.hemonorte.repository.SlotAgendaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID
import com.hemonorte.dto.AgendamentoResponse
import com.hemonorte.dto.toResponse
import org.springframework.security.access.AccessDeniedException

@Service
class AgendamentoService(
    private val agendamentoRepository: AgendamentoRepository,
    private val doadorRepository: DoadorRepository,
    private val slotAgendaRepository: SlotAgendaRepository,
    private val aptidaoService: AptidaoService,
    private val preTriagemRepository: PreTriagemRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    @Transactional
    fun realizarAgendamento(doadorId: UUID, slotId: UUID): Agendamento {
        val doador = doadorRepository.findById(doadorId)
            .orElseThrow { NoSuchElementException("Doador não encontrado") }

        val resultadoAptidao = aptidaoService.verificarAptidao(doador)
        if (!resultadoAptidao.apto) {
            throw InaptidaoDoacaoException(resultadoAptidao)
        }
        verificarPreTriagem(doadorId)

        val slot = slotAgendaRepository.findById(slotId)
            .orElseThrow { NoSuchElementException("Slot de agenda não encontrado") }
        require(slot.inicio.isAfter(clock.instant())) { "Não é possível agendar em horário passado" }

        if (agendamentoRepository.existsByDoadorIdAndSlotIdAndStatus(doadorId, slotId, StatusAgendamento.PENDENTE)) {
            throw ConflitoException("Doador já possui reserva ativa neste horário")
        }
        val ocupacao = agendamentoRepository.countBySlotIdAndStatus(slotId, StatusAgendamento.PENDENTE)
        if (ocupacao >= slot.capacidade.toLong()) {
            throw ConflitoException("Slot lotado para este horário")
        }

        return agendamentoRepository.save(Agendamento(doador = doador, slot = slot))
    }

    private fun verificarPreTriagem(doadorId: UUID) {
        val preTriagem = preTriagemRepository.findFirstByDoadorIdOrderByRespondidaEmDesc(doadorId)
        val motivo = when {
            preTriagem == null || !clock.instant().isBefore(preTriagem.validaAte) ->
                "Responda a pré-triagem antes de agendar"
            !preTriagem.apto -> "Pré-triagem inapta: ${preTriagem.motivoInaptidao}"
            else -> return
        }
        throw InaptidaoDoacaoException(ResultadoAptidao(false, motivo, null))
    }

    @Transactional
    fun realizarAgendamentoResponse(doadorId: UUID, slotId: UUID): AgendamentoResponse =
        realizarAgendamento(doadorId, slotId).toResponse()

    @Transactional
    fun cancelarAgendamento(agendamentoId: UUID): Agendamento {
        val agendamento = agendamentoRepository.findById(agendamentoId)
            .orElseThrow { NoSuchElementException("Agendamento não encontrado") }

        agendamento.cancelar()
        return agendamentoRepository.save(agendamento)
    }

    @Transactional
    fun cancelarAgendamentoDoDoador(agendamentoId: UUID, doadorId: UUID): Agendamento {
        val agendamento = agendamentoRepository.findByIdAndDoadorId(agendamentoId, doadorId)
            .orElseThrow { AccessDeniedException("Acesso negado") }
        agendamento.cancelar()
        return agendamentoRepository.save(agendamento)
    }

    @Transactional(readOnly = true)
    fun listar(): List<Agendamento> = agendamentoRepository.findAll()

    @Transactional(readOnly = true)
    fun listarPorDoador(doadorId: UUID): List<Agendamento> = agendamentoRepository.findByDoadorId(doadorId)

    @Transactional(readOnly = true)
    fun buscar(id: UUID): Agendamento = agendamentoRepository.findById(id)
        .orElseThrow { NoSuchElementException("Agendamento não encontrado") }

    @Transactional
    fun cancelarAgendamentoResponse(id: UUID): AgendamentoResponse = cancelarAgendamento(id).toResponse()

    @Transactional
    fun cancelarAgendamentoDoDoadorResponse(id: UUID, doadorId: UUID): AgendamentoResponse =
        cancelarAgendamentoDoDoador(id, doadorId).toResponse()

    @Transactional
    fun registrarFaltaResponse(id: UUID): AgendamentoResponse = registrarFalta(id).toResponse()

    @Transactional(readOnly = true)
    fun listarResponses(): List<AgendamentoResponse> = agendamentoRepository.findAll().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun listarPorDoadorResponses(doadorId: UUID): List<AgendamentoResponse> =
        agendamentoRepository.findByDoadorId(doadorId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun buscarResponse(id: UUID): AgendamentoResponse = buscar(id).toResponse()

    @Transactional(readOnly = true)
    fun buscarResponseDoDoador(id: UUID, doadorId: UUID): AgendamentoResponse =
        agendamentoRepository.findByIdAndDoadorId(id, doadorId)
            .orElseThrow { AccessDeniedException("Acesso negado") }
            .toResponse()

    @Transactional
    fun registrarFalta(agendamentoId: UUID): Agendamento {
        val agendamento = agendamentoRepository.findById(agendamentoId)
            .orElseThrow { NoSuchElementException("Agendamento não encontrado") }

        agendamento.registrarFalta()
        return agendamentoRepository.save(agendamento)
    }
}
