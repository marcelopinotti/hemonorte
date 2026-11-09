package com.hemonorte.service

import com.hemonorte.exception.InaptidaoDoacaoException
import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.SlotAgendaRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class AgendamentoServiceTest {

    private val agendamentoRepository = Mockito.mock(AgendamentoRepository::class.java)
    private val doadorRepository = Mockito.mock(DoadorRepository::class.java)
    private val slotAgendaRepository = Mockito.mock(SlotAgendaRepository::class.java)
    private val aptidaoService = AptidaoService(
        agendamentoRepository,
        Clock.fixed(Instant.parse("2035-11-10T10:00:00Z"), ZoneOffset.UTC),
    )
    private val service = AgendamentoService(
        agendamentoRepository,
        doadorRepository,
        slotAgendaRepository,
        aptidaoService,
    )
    private val doador = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.of(1990, 1, 1),
        genero = 'F',
        cpf = "12345678901",
    )
    private val slot = SlotAgenda(Instant.parse("2035-11-10T12:00:00Z"), 5)

    @Suppress("UNCHECKED_CAST")
    private fun <T> qualquer(): T = Mockito.any() as T

    @Test
    fun `cria reserva quando apto e ha vaga`() {
        prepararEntidades()
        Mockito.`when`(
            agendamentoRepository.countBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE),
        ).thenReturn(4)
        Mockito.`when`(agendamentoRepository.save(qualquer()))
            .thenAnswer { it.arguments[0] as Agendamento }

        val resultado = service.realizarAgendamento(doador.id, slot.id)

        assertEquals(StatusAgendamento.PENDENTE, resultado.status)
    }

    @Test
    fun `lotacao usa conflito`() {
        prepararEntidades()
        Mockito.`when`(
            agendamentoRepository.countBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE),
        ).thenReturn(5)

        assertFailsWith<ConflitoException> {
            service.realizarAgendamento(doador.id, slot.id)
        }
    }

    @Test
    fun `intervalo nao cumprido impede reserva antes da contagem e persistencia`() {
        prepararEntidades()
        val doacaoAnterior = Agendamento(
            doador,
            SlotAgenda(Instant.parse("2035-10-01T12:00:00Z"), 1),
        ).also { it.concluir() }
        Mockito.`when`(
            agendamentoRepository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO),
        ).thenReturn(listOf(doacaoAnterior))
        val serviceComRegra = AgendamentoService(
            agendamentoRepository,
            doadorRepository,
            slotAgendaRepository,
            AptidaoService(
                agendamentoRepository,
                Clock.fixed(Instant.parse("2035-11-10T12:00:00Z"), ZoneOffset.UTC),
            ),
        )

        val erro = assertFailsWith<InaptidaoDoacaoException> {
            serviceComRegra.realizarAgendamento(doador.id, slot.id)
        }
        assertEquals("Intervalo mínimo entre doações não cumprido", erro.resultado.motivo)
        assertEquals(LocalDate.of(2035, 12, 30), erro.resultado.proximaDataPermitida)
        Mockito.verify(agendamentoRepository, Mockito.never())
            .countBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `cancelar persiste transicao`() {
        val agendamento = Agendamento(doador, slot)
        Mockito.`when`(agendamentoRepository.findById(agendamento.id))
            .thenReturn(Optional.of(agendamento))
        Mockito.`when`(agendamentoRepository.save(qualquer()))
            .thenAnswer { it.arguments[0] as Agendamento }

        assertEquals(
            StatusAgendamento.CANCELADO,
            service.cancelarAgendamento(agendamento.id).status,
        )
    }

    @Test
    fun `registrar falta persiste transicao`() {
        val agendamento = Agendamento(doador, slot)
        Mockito.`when`(agendamentoRepository.findById(agendamento.id))
            .thenReturn(Optional.of(agendamento))
        Mockito.`when`(agendamentoRepository.save(qualquer()))
            .thenAnswer { it.arguments[0] as Agendamento }

        assertEquals(StatusAgendamento.FALTOU, service.registrarFalta(agendamento.id).status)
    }

    private fun prepararEntidades() {
        Mockito.`when`(doadorRepository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(slotAgendaRepository.findById(slot.id)).thenReturn(Optional.of(slot))
        Mockito.`when`(
            agendamentoRepository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO),
        ).thenReturn(emptyList())
    }
}
