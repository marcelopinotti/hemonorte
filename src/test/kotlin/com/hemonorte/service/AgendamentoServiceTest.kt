package com.hemonorte.service

import com.hemonorte.exception.InaptidaoDoacaoException
import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.PreTriagem
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.PreTriagemRepository
import com.hemonorte.repository.SlotAgendaRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
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
    private val preTriagemRepository = Mockito.mock(PreTriagemRepository::class.java)
    private val agora = Instant.parse("2035-11-10T10:00:00Z")
    private val relogio = Clock.fixed(agora, ZoneOffset.UTC)
    private val aptidaoService = AptidaoService(agendamentoRepository, relogio)
    private val service = AgendamentoService(
        agendamentoRepository,
        doadorRepository,
        slotAgendaRepository,
        aptidaoService,
        preTriagemRepository,
        relogio,
    )
    private val doador = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.of(1990, 1, 1),
        genero = 'F',
        cpf = "12345678909",
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
            preTriagemRepository,
            relogio,
        )

        val erro = assertFailsWith<InaptidaoDoacaoException> {
            serviceComRegra.realizarAgendamento(doador.id, slot.id)
        }
        assertEquals("Intervalo mínimo entre doações não cumprido", erro.resultado.motivo)
        assertEquals(LocalDate.of(2035, 12, 30), erro.resultado.proximaDataPermitida)
        Mockito.verify(agendamentoRepository, Mockito.never())
            .countBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
        Mockito.verify(slotAgendaRepository, Mockito.never()).findById(slot.id)
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

    @Test
    fun `doador inexistente devolve nao encontrado`() {
        Mockito.`when`(doadorRepository.findById(doador.id)).thenReturn(Optional.empty())

        assertFailsWith<NoSuchElementException> { service.realizarAgendamento(doador.id, slot.id) }
    }

    @Test
    fun `slot inexistente devolve nao encontrado`() {
        prepararEntidades()
        Mockito.`when`(slotAgendaRepository.findById(slot.id)).thenReturn(Optional.empty())

        assertFailsWith<NoSuchElementException> { service.realizarAgendamento(doador.id, slot.id) }
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `slot em horario passado e rejeitado`() {
        prepararEntidades()
        val servicoNoFuturo = AgendamentoService(
            agendamentoRepository,
            doadorRepository,
            slotAgendaRepository,
            aptidaoService,
            preTriagemRepository,
            Clock.fixed(Instant.parse("2035-11-10T12:00:00Z"), ZoneOffset.UTC),
        )

        assertFailsWith<IllegalArgumentException> { servicoNoFuturo.realizarAgendamento(doador.id, slot.id) }
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `reserva ativa duplicada no mesmo slot usa conflito`() {
        prepararEntidades()
        Mockito.`when`(
            agendamentoRepository.existsByDoadorIdAndSlotIdAndStatus(doador.id, slot.id, StatusAgendamento.PENDENTE),
        ).thenReturn(true)

        assertFailsWith<ConflitoException> { service.realizarAgendamento(doador.id, slot.id) }
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `agendamento inexistente devolve nao encontrado ao cancelar ou registrar falta`() {
        val id = java.util.UUID.randomUUID()
        Mockito.`when`(agendamentoRepository.findById(id)).thenReturn(Optional.empty())

        assertFailsWith<NoSuchElementException> { service.cancelarAgendamento(id) }
        assertFailsWith<NoSuchElementException> { service.registrarFalta(id) }
    }

    @Test
    fun `doador nao cancela agendamento de outro doador`() {
        val id = java.util.UUID.randomUUID()
        val outroDoador = java.util.UUID.randomUUID()
        Mockito.`when`(agendamentoRepository.findByIdAndDoadorId(id, outroDoador)).thenReturn(Optional.empty())

        assertFailsWith<AccessDeniedException> { service.cancelarAgendamentoDoDoador(id, outroDoador) }
    }

    @Test
    fun `transicoes repetidas sao rejeitadas`() {
        val cancelado = Agendamento(doador, slot).also { it.cancelar() }
        val faltou = Agendamento(doador, slot).also { it.registrarFalta() }
        Mockito.`when`(agendamentoRepository.findById(cancelado.id)).thenReturn(Optional.of(cancelado))
        Mockito.`when`(agendamentoRepository.findById(faltou.id)).thenReturn(Optional.of(faltou))

        assertFailsWith<IllegalArgumentException> { service.cancelarAgendamento(cancelado.id) }
        assertFailsWith<IllegalArgumentException> { service.registrarFalta(cancelado.id) }
        assertFailsWith<IllegalArgumentException> { service.registrarFalta(faltou.id) }
        assertFailsWith<IllegalArgumentException> { service.cancelarAgendamento(faltou.id) }
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `sem pre-triagem a reserva e recusada antes de buscar o slot`() {
        prepararEntidades(preTriagem = null)

        val erro = assertFailsWith<InaptidaoDoacaoException> { service.realizarAgendamento(doador.id, slot.id) }

        assertEquals("Responda a pré-triagem antes de agendar", erro.resultado.motivo)
        Mockito.verify(slotAgendaRepository, Mockito.never()).findById(slot.id)
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `pre-triagem vencida exige nova resposta`() {
        prepararEntidades(preTriagem = preTriagem(respondidaEm = agora.minus(PreTriagem.VALIDADE)))

        val erro = assertFailsWith<InaptidaoDoacaoException> { service.realizarAgendamento(doador.id, slot.id) }

        assertEquals("Responda a pré-triagem antes de agendar", erro.resultado.motivo)
    }

    @Test
    fun `pre-triagem inapta informa o motivo e impede a reserva`() {
        prepararEntidades(preTriagem = preTriagem(teveFebre = true))

        val erro = assertFailsWith<InaptidaoDoacaoException> { service.realizarAgendamento(doador.id, slot.id) }

        assertEquals("Pré-triagem inapta: febre recente", erro.resultado.motivo)
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `pre-triagem apta dentro da validade libera a reserva`() {
        prepararEntidades(preTriagem = preTriagem(respondidaEm = agora.minus(PreTriagem.VALIDADE).plusSeconds(1)))
        Mockito.`when`(agendamentoRepository.save(qualquer())).thenAnswer { it.arguments[0] as Agendamento }

        assertEquals(StatusAgendamento.PENDENTE, service.realizarAgendamento(doador.id, slot.id).status)
    }

    @Test
    fun `remarcar cancela o horario atual e reserva o novo`() {
        prepararEntidades()
        val atual = Agendamento(doador, SlotAgenda(Instant.parse("2035-11-12T12:00:00Z"), 5))
        Mockito.`when`(agendamentoRepository.findByIdAndDoadorId(atual.id, doador.id)).thenReturn(Optional.of(atual))
        Mockito.`when`(agendamentoRepository.save(qualquer())).thenAnswer { it.arguments[0] as Agendamento }

        val novo = service.remarcar(atual.id, doador.id, slot.id)

        assertEquals(StatusAgendamento.CANCELADO, atual.status)
        assertEquals(StatusAgendamento.PENDENTE, novo.status)
        assertEquals(slot, novo.slot)
    }

    @Test
    fun `remarcar para o mesmo horario ou agendamento de outro doador e rejeitado`() {
        val atual = Agendamento(doador, slot)
        Mockito.`when`(agendamentoRepository.findByIdAndDoadorId(atual.id, doador.id)).thenReturn(Optional.of(atual))

        assertFailsWith<IllegalArgumentException> { service.remarcar(atual.id, doador.id, slot.id) }
        assertFailsWith<AccessDeniedException> { service.remarcar(atual.id, java.util.UUID.randomUUID(), slot.id) }
        assertEquals(StatusAgendamento.PENDENTE, atual.status)
    }

    @Test
    fun `remarcar agendamento ja concluido e rejeitado`() {
        val concluido = Agendamento(doador, SlotAgenda(Instant.parse("2035-11-12T12:00:00Z"), 5)).also { it.concluir() }
        Mockito.`when`(agendamentoRepository.findByIdAndDoadorId(concluido.id, doador.id)).thenReturn(Optional.of(concluido))

        assertFailsWith<IllegalArgumentException> { service.remarcar(concluido.id, doador.id, slot.id) }
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `historico de doacoes lista concluidas da mais recente para a mais antiga`() {
        val antiga = Agendamento(doador, SlotAgenda(Instant.parse("2035-01-10T12:00:00Z"), 5)).also { it.concluir() }
        val recente = Agendamento(doador, SlotAgenda(Instant.parse("2035-06-10T12:00:00Z"), 5)).also { it.concluir() }
        Mockito.`when`(agendamentoRepository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO))
            .thenReturn(listOf(antiga, recente))

        assertEquals(listOf(recente.id, antiga.id), service.listarDoacoes(doador.id).map { it.agendamentoId })
    }

    private fun preTriagem(
        respondidaEm: Instant = agora.minus(Duration.ofDays(1)),
        teveFebre: Boolean = false,
    ) = PreTriagem(doador, BigDecimal("70.00"), false, false, teveFebre, respondidaEm)

    private fun prepararEntidades(preTriagem: PreTriagem? = preTriagem()) {
        Mockito.`when`(preTriagemRepository.findFirstByDoadorIdOrderByRespondidaEmDesc(doador.id))
            .thenReturn(preTriagem)
        Mockito.`when`(doadorRepository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(slotAgendaRepository.findById(slot.id)).thenReturn(Optional.of(slot))
        Mockito.`when`(
            agendamentoRepository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO),
        ).thenReturn(emptyList())
    }
}
