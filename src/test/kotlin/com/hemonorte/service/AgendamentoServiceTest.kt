package com.hemonorte.service

import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.SlotAgendaRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Tag("unit")
class AgendamentoServiceTest {

    private val agendamentoRepository = Mockito.mock(AgendamentoRepository::class.java)
    private val doadorRepository = Mockito.mock(DoadorRepository::class.java)
    private val slotAgendaRepository = Mockito.mock(SlotAgendaRepository::class.java)
    private val aptidaoService = Mockito.mock(AptidaoService::class.java)
    private val service = AgendamentoService(
        agendamentoRepository, doadorRepository, slotAgendaRepository, aptidaoService
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> qualquer(): T = Mockito.any() as T

    private val doador = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.now().minusYears(30),
        genero = 'F',
        cpf = "12345678901",
    )

    private val slot = SlotAgenda(Instant.now().plusSeconds(86_400), 5)

    private fun apto() = ResultadoAptidao(apto = true, motivo = null, proximaDataPermitida = null)

    @Test
    fun `cria agendamento PENDENTE quando doador apto e ha vagas`() {
        Mockito.`when`(doadorRepository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(slotAgendaRepository.findById(slot.id)).thenReturn(Optional.of(slot))
        Mockito.`when`(aptidaoService.verificarAptidao(doador)).thenReturn(apto())
        Mockito.`when`(
            agendamentoRepository.findBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
        ).thenReturn(emptyList())
        Mockito.`when`(agendamentoRepository.save(qualquer()))
            .thenAnswer { it.arguments[0] as Agendamento }

        val agendamento = service.realizarAgendamento(doador.id, slot.id)

        assertEquals(StatusAgendamento.PENDENTE, agendamento.status)
        Mockito.verify(agendamentoRepository).save(agendamento)
    }

    @Test
    fun `rejeita doador inapto com o motivo da aptidao`() {
        Mockito.`when`(doadorRepository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(slotAgendaRepository.findById(slot.id)).thenReturn(Optional.of(slot))
        Mockito.`when`(aptidaoService.verificarAptidao(doador)).thenReturn(
            ResultadoAptidao(
                apto = false,
                motivo = "Intervalo mínimo entre doações não respeitado.",
                proximaDataPermitida = LocalDate.now().plusDays(30),
            )
        )

        val erro = assertFailsWith<IllegalArgumentException> {
            service.realizarAgendamento(doador.id, slot.id)
        }
        assertTrue(erro.message!!.contains("Intervalo mínimo"))
        Mockito.verify(agendamentoRepository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `rejeita slot lotado`() {
        Mockito.`when`(doadorRepository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(slotAgendaRepository.findById(slot.id)).thenReturn(Optional.of(slot))
        Mockito.`when`(aptidaoService.verificarAptidao(doador)).thenReturn(apto())
        val ocupadas = List(5) { Agendamento(doador = doador, slot = slot) }
        Mockito.`when`(
            agendamentoRepository.findBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
        ).thenReturn(ocupadas)

        val erro = assertFailsWith<IllegalArgumentException> {
            service.realizarAgendamento(doador.id, slot.id)
        }
        assertTrue(erro.message!!.contains("Slot lotado"))
    }

    @Test
    fun `rejeita doador inexistente`() {
        val idDesconhecido = UUID.randomUUID()
        Mockito.`when`(doadorRepository.findById(idDesconhecido)).thenReturn(Optional.empty())

        val erro = assertFailsWith<IllegalArgumentException> {
            service.realizarAgendamento(idDesconhecido, slot.id)
        }
        assertTrue(erro.message!!.contains("Doador não encontrado"))
    }

    @Test
    fun `rejeita slot inexistente`() {
        val idDesconhecido = UUID.randomUUID()
        Mockito.`when`(doadorRepository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(slotAgendaRepository.findById(idDesconhecido)).thenReturn(Optional.empty())

        val erro = assertFailsWith<IllegalArgumentException> {
            service.realizarAgendamento(doador.id, idDesconhecido)
        }
        assertTrue(erro.message!!.contains("Slot de agenda não encontrado"))
    }

    @Test
    fun `SENTINELA QA-037 - reserva ativa duplicada do mesmo doador no mesmo slot e aceita`() {
        // Contrato das issues #9 e #17: impedir reserva ativa duplicada do mesmo doador no
        // mesmo slot. Hoje nada checa: com uma reserva PENDENTE existente e capacidade 5,
        // o mesmo doador reserva de novo. Quando a correcao chegar, este teste vira
        // assercao de rejeicao (IllegalArgumentException com o motivo do contrato).
        Mockito.`when`(doadorRepository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(slotAgendaRepository.findById(slot.id)).thenReturn(Optional.of(slot))
        Mockito.`when`(aptidaoService.verificarAptidao(doador)).thenReturn(apto())
        Mockito.`when`(
            agendamentoRepository.findBySlotIdAndStatus(slot.id, StatusAgendamento.PENDENTE)
        ).thenReturn(listOf(Agendamento(doador = doador, slot = slot)))
        Mockito.`when`(agendamentoRepository.save(qualquer()))
            .thenAnswer { it.arguments[0] as Agendamento }

        val segundaReserva = service.realizarAgendamento(doador.id, slot.id)

        // aceito hoje - e o defeito documentado; a contagem nao olha o dono das reservas
        assertEquals(StatusAgendamento.PENDENTE, segundaReserva.status)
        Mockito.verify(agendamentoRepository).save(segundaReserva)
    }

    @Test
    fun `cancelar persiste a transicao de PENDENTE para CANCELADO`() {
        val agendamento = Agendamento(doador = doador, slot = slot)
        Mockito.`when`(agendamentoRepository.findById(agendamento.id)).thenReturn(Optional.of(agendamento))
        Mockito.`when`(agendamentoRepository.save(qualquer()))
            .thenAnswer { it.arguments[0] as Agendamento }

        val cancelado = service.cancelarAgendamento(agendamento.id)

        assertEquals(StatusAgendamento.CANCELADO, cancelado.status)
    }

    @Test
    fun `registrar falta persiste a transicao de PENDENTE para FALTOU`() {
        val agendamento = Agendamento(doador = doador, slot = slot)
        Mockito.`when`(agendamentoRepository.findById(agendamento.id)).thenReturn(Optional.of(agendamento))
        Mockito.`when`(agendamentoRepository.save(qualquer()))
            .thenAnswer { it.arguments[0] as Agendamento }

        val comFalta = service.registrarFalta(agendamento.id)

        assertEquals(StatusAgendamento.FALTOU, comFalta.status)
    }
}
