package com.hemonorte.service

import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.Agendamento
import com.hemonorte.model.BolsaSangue
import com.hemonorte.model.Doador
import com.hemonorte.model.Funcionario
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.Triagem
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.model.enums.StatusBolsa
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.HistoricoAuditoriaRepository
import com.hemonorte.model.HistoricoAuditoria
import com.hemonorte.repository.TriagemRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("unit")
class BolsaSangueServiceTest {

    private val bolsaRepository = Mockito.mock(BolsaSangueRepository::class.java)
    private val agendamentoRepository = Mockito.mock(AgendamentoRepository::class.java)
    private val triagemRepository = Mockito.mock(TriagemRepository::class.java)
    private val funcionarioRepository = Mockito.mock(FuncionarioRepository::class.java)
    private val historicoRepository = Mockito.mock(HistoricoAuditoriaRepository::class.java)
    private val service = BolsaSangueService(
        bolsaRepository,
        agendamentoRepository,
        triagemRepository,
        funcionarioRepository,
        historicoRepository,
    )

    private val agendamento = Agendamento(
        Doador(
            nome = "Ana Souza",
            email = "ana@email.com",
            senhaHash = "hash",
            tipagem = "O+",
            dataNascimento = LocalDate.of(1990, 1, 1),
            genero = 'F',
            cpf = "12345678909",
        ),
        SlotAgenda(Instant.now().plusSeconds(86_400), 5),
    )
    private val funcionario = Funcionario(
        matricula = "TRI-1",
        nome = "Marcos Lima",
        email = "marcos@hemonorte.br",
        senhaHash = "hash",
        perfil = PerfilFuncionario.TRIAGEM,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> qualquer(): T = Mockito.any() as T

    private fun coletar() = service.criar(agendamento.id, "O+", LocalDate.now(), LocalDate.now().plusDays(35))

    private fun comTriagem(triagem: Triagem?) {
        Mockito.`when`(agendamentoRepository.findById(agendamento.id)).thenReturn(Optional.of(agendamento))
        Mockito.`when`(triagemRepository.findByAgendamentoId(agendamento.id)).thenReturn(Optional.ofNullable(triagem))
    }

    @Test
    fun `triagem inapta bloqueia coleta sem gerar bolsa nem comprovante`() {
        comTriagem(Triagem(agendamento, funcionario, BigDecimal("45"), false, false, false))

        assertFailsWith<ConflitoException> { coletar() }

        Mockito.verify(bolsaRepository, Mockito.never()).saveAndFlush(qualquer<BolsaSangue>())
        assertEquals(StatusAgendamento.PENDENTE, agendamento.status)
        assertNull(agendamento.codigoComprovante)
    }

    @Test
    fun `coleta sem triagem e bloqueada`() {
        comTriagem(null)

        assertFailsWith<ConflitoException> { coletar() }
        Mockito.verify(bolsaRepository, Mockito.never()).saveAndFlush(qualquer<BolsaSangue>())
    }

    @Test
    fun `agendamento cancelado apos triagem apta nao gera bolsa`() {
        comTriagem(Triagem(agendamento, funcionario, BigDecimal("70"), false, false, false))
        agendamento.cancelar()

        assertFailsWith<ConflitoException> { coletar() }
        Mockito.verify(bolsaRepository, Mockito.never()).saveAndFlush(qualquer<BolsaSangue>())
    }

    @Test
    fun `descarte manual grava auditoria com funcionario e motivo`() {
        val bolsa = BolsaSangue(agendamento, "O+", LocalDate.now(), LocalDate.now().plusDays(35))
        Mockito.`when`(bolsaRepository.findById(bolsa.id)).thenReturn(Optional.of(bolsa))
        Mockito.`when`(funcionarioRepository.findById(funcionario.id)).thenReturn(Optional.of(funcionario))
        Mockito.`when`(bolsaRepository.save(bolsa)).thenReturn(bolsa)
        val captor = org.mockito.ArgumentCaptor.forClass(HistoricoAuditoria::class.java)

        service.atualizarStatus(bolsa.id, StatusBolsa.DESCARTADA, "Bolsa rompida", funcionario.id)

        Mockito.verify(historicoRepository).save(captor.capture())
        assertEquals(StatusBolsa.DESCARTADA, bolsa.status)
        assertEquals(funcionario, captor.value.funcionario)
        assertEquals(StatusBolsa.EM_ANALISE, captor.value.statusAnterior)
        assertEquals("Bolsa rompida", captor.value.motivo)
    }

    @Test
    fun `descarte manual sem motivo nao grava nada`() {
        val bolsa = BolsaSangue(agendamento, "O+", LocalDate.now(), LocalDate.now().plusDays(35))
        Mockito.`when`(bolsaRepository.findById(bolsa.id)).thenReturn(Optional.of(bolsa))
        Mockito.`when`(funcionarioRepository.findById(funcionario.id)).thenReturn(Optional.of(funcionario))

        assertFailsWith<IllegalArgumentException> {
            service.atualizarStatus(bolsa.id, StatusBolsa.DESCARTADA, null, funcionario.id)
        }
        Mockito.verify(historicoRepository, Mockito.never()).save(qualquer<HistoricoAuditoria>())
        Mockito.verify(bolsaRepository, Mockito.never()).save(qualquer<BolsaSangue>())
    }

    @Test
    fun `coleta apta cria bolsa em analise e conclui agendamento com comprovante`() {
        comTriagem(Triagem(agendamento, funcionario, BigDecimal("70"), false, false, false))
        Mockito.`when`(bolsaRepository.saveAndFlush(qualquer<BolsaSangue>())).thenAnswer { it.arguments[0] as BolsaSangue }

        val bolsa = coletar()

        assertEquals(agendamento, bolsa.agendamento)
        assertEquals(StatusBolsa.EM_ANALISE, bolsa.status)
        assertEquals(StatusAgendamento.CONCLUIDO, agendamento.status)
        assertNotNull(agendamento.codigoComprovante)
    }
}
