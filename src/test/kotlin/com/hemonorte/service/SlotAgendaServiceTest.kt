package com.hemonorte.service

import com.hemonorte.dto.SlotDisponibilidadeResponse
import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.SlotAgendaRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class SlotAgendaServiceTest {

    private val repository = Mockito.mock(SlotAgendaRepository::class.java)
    private val agendamentoRepository = Mockito.mock(AgendamentoRepository::class.java)
    private val agora = Instant.parse("2035-11-10T10:00:00Z")
    private val service = SlotAgendaService(
        repository,
        agendamentoRepository,
        Clock.fixed(agora, ZoneOffset.UTC),
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> qualquer(): T = Mockito.any() as T

    @Test
    fun `consulta inclui slot lotado com zero vagas`() {
        val inicio = Instant.parse("2035-11-10T12:00:00Z")
        val fim = Instant.parse("2035-11-11T12:00:00Z")
        val lotado = SlotAgenda(inicio, 2)
        Mockito.`when`(
            repository.findByInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(inicio, fim),
        ).thenReturn(listOf(lotado))
        Mockito.`when`(
            agendamentoRepository.countBySlotIdAndStatus(lotado.id, StatusAgendamento.PENDENTE),
        ).thenReturn(2)

        assertEquals(
            listOf(
                SlotDisponibilidadeResponse(
                    id = lotado.id,
                    inicio = inicio,
                    capacidade = 2,
                    ocupacao = 2,
                    vagasDisponiveis = 0,
                ),
            ),
            service.listarPorPeriodo(inicio, fim),
        )
    }

    @Test
    fun `rejeita periodo sem duracao positiva`() {
        val instante = Instant.parse("2035-11-10T12:00:00Z")

        assertFailsWith<IllegalArgumentException> {
            service.listarPorPeriodo(instante, instante)
        }
    }

    @Test
    fun `consulta respeita o periodo solicitado mesmo quando esta no passado`() {
        val inicio = agora.minusSeconds(3_600)
        val fim = agora.plusSeconds(3_600)
        Mockito.`when`(
            repository.findByInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(inicio, fim),
        ).thenReturn(emptyList())

        service.listarPorPeriodo(inicio, fim)

        Mockito.verify(repository)
            .findByInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(inicio, fim)
    }

    @Test
    fun `criacao no instante atual e rejeitada`() {
        Mockito.`when`(repository.existsByInicio(agora)).thenReturn(false)

        assertFailsWith<IllegalArgumentException> { service.criarSlot(agora, 1) }
    }

    @Test
    fun `rejeita reducao abaixo das reservas pendentes`() {
        val slot = SlotAgenda(Instant.parse("2035-11-10T12:00:00Z"), 5)
        Mockito.`when`(repository.findById(slot.id)).thenReturn(Optional.of(slot))
        Mockito.`when`(
            agendamentoRepository.countBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE),
        ).thenReturn(2)

        assertFailsWith<ConflitoException> {
            service.atualizar(slot.id, slot.inicio, 1)
        }
    }

    @Test
    fun `aceita capacidade igual as reservas pendentes`() {
        val slot = SlotAgenda(Instant.parse("2035-11-10T12:00:00Z"), 5)
        Mockito.`when`(repository.findById(slot.id)).thenReturn(Optional.of(slot))
        Mockito.`when`(
            agendamentoRepository.countBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE),
        ).thenReturn(2)
        Mockito.`when`(repository.save(qualquer())).thenAnswer { it.arguments[0] as SlotAgenda }

        val atualizado = service.atualizar(slot.id, slot.inicio, 2)

        assertEquals(2, atualizado.capacidade)
    }

    @Test
    fun `duplicidade usa conflito`() {
        val inicio = Instant.parse("2035-11-10T12:00:00Z")
        Mockito.`when`(repository.existsByInicio(inicio)).thenReturn(true)

        assertFailsWith<ConflitoException> { service.criarSlot(inicio, 5) }
    }
}
