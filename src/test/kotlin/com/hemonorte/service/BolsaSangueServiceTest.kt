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
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.BolsaSangueRepository
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
import kotlin.test.assertNull

@Tag("unit")
class BolsaSangueServiceTest {

    private val bolsaRepository = Mockito.mock(BolsaSangueRepository::class.java)
    private val agendamentoRepository = Mockito.mock(AgendamentoRepository::class.java)
    private val triagemRepository = Mockito.mock(TriagemRepository::class.java)
    private val service = BolsaSangueService(bolsaRepository, agendamentoRepository, triagemRepository)

    private val agendamento = Agendamento(
        Doador(
            nome = "Ana Souza",
            email = "ana@email.com",
            senhaHash = "hash",
            tipagem = "O+",
            dataNascimento = LocalDate.of(1990, 1, 1),
            genero = 'F',
            cpf = "12345678901",
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

        Mockito.verify(bolsaRepository, Mockito.never()).save(qualquer<BolsaSangue>())
        assertEquals(StatusAgendamento.PENDENTE, agendamento.status)
        assertNull(agendamento.codigoComprovante)
    }

    @Test
    fun `coleta sem triagem e bloqueada`() {
        comTriagem(null)

        assertFailsWith<ConflitoException> { coletar() }
        Mockito.verify(bolsaRepository, Mockito.never()).save(qualquer<BolsaSangue>())
    }

    @Test
    fun `agendamento cancelado apos triagem apta nao gera bolsa`() {
        comTriagem(Triagem(agendamento, funcionario, BigDecimal("70"), false, false, false))
        agendamento.cancelar()

        assertFailsWith<ConflitoException> { coletar() }
        Mockito.verify(bolsaRepository, Mockito.never()).save(qualquer<BolsaSangue>())
    }

    @Test
    fun `triagem apta libera coleta`() {
        comTriagem(Triagem(agendamento, funcionario, BigDecimal("70"), false, false, false))
        Mockito.`when`(bolsaRepository.save(qualquer<BolsaSangue>())).thenAnswer { it.arguments[0] as BolsaSangue }

        assertEquals(agendamento, coletar().agendamento)
    }
}
