package com.hemonorte.service

import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.Funcionario
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.Triagem
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.TriagemRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class TriagemServiceTest {

    private val triagemRepository = Mockito.mock(TriagemRepository::class.java)
    private val agendamentoRepository = Mockito.mock(AgendamentoRepository::class.java)
    private val funcionarioRepository = Mockito.mock(FuncionarioRepository::class.java)
    private val service = TriagemService(triagemRepository, agendamentoRepository, funcionarioRepository)

    private val funcionario = Funcionario(
        matricula = "TRI-1",
        nome = "Marcos Lima",
        email = "marcos@hemonorte.br",
        senhaHash = "hash",
        perfil = PerfilFuncionario.TRIAGEM,
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

    @Suppress("UNCHECKED_CAST")
    private fun <T> qualquer(): T = Mockito.any() as T

    private fun preparar() {
        Mockito.`when`(funcionarioRepository.findById(funcionario.id)).thenReturn(Optional.of(funcionario))
        Mockito.`when`(agendamentoRepository.findById(agendamento.id)).thenReturn(Optional.of(agendamento))
        Mockito.`when`(triagemRepository.saveAndFlush(qualquer<Triagem>()))
            .thenAnswer { it.arguments[0] as Triagem }
    }

    private fun realizar(peso: String = "70.00", tatuagem: Boolean = false, cirurgia: Boolean = false, febre: Boolean = false) =
        service.realizarTriagem(agendamento.id, BigDecimal(peso), tatuagem, cirurgia, febre, funcionario.id)

    @Test
    fun `triagem apta vincula agendamento e funcionario autenticado`() {
        preparar()

        val triagem = realizar()

        assertTrue(triagem.apto)
        assertNull(triagem.motivoInaptidao)
        assertEquals(agendamento, triagem.agendamento)
        assertEquals(funcionario, triagem.funcionario)
    }

    @Test
    fun `triagem inapta e calculada no dominio`() {
        preparar()

        val triagem = realizar(peso = "49.99", febre = true)

        assertFalse(triagem.apto)
        assertEquals("peso abaixo do mínimo (50.0kg); febre recente", triagem.motivoInaptidao)
    }

    @Test
    fun `triagem duplicada usa conflito`() {
        preparar()
        Mockito.`when`(triagemRepository.existsByAgendamentoId(agendamento.id)).thenReturn(true)

        assertFailsWith<ConflitoException> { realizar() }
        Mockito.verify(triagemRepository, Mockito.never()).saveAndFlush(qualquer<Triagem>())
    }

    @Test
    fun `agendamento inexistente devolve nao encontrado`() {
        Mockito.`when`(funcionarioRepository.findById(funcionario.id)).thenReturn(Optional.of(funcionario))
        Mockito.`when`(agendamentoRepository.findById(agendamento.id)).thenReturn(Optional.empty())

        assertFailsWith<NoSuchElementException> { realizar() }
    }

    @Test
    fun `agendamento cancelado concluido ou com falta nao recebe triagem`() {
        preparar()
        val transicoes = listOf<(Agendamento) -> Unit>({ it.cancelar() }, { it.concluir() }, { it.registrarFalta() })

        transicoes.forEach { transicao ->
            val encerrado = Agendamento(agendamento.doador, agendamento.slot).also(transicao)
            Mockito.`when`(agendamentoRepository.findById(encerrado.id)).thenReturn(Optional.of(encerrado))

            assertFailsWith<ConflitoException> {
                service.realizarTriagem(encerrado.id, BigDecimal("70"), false, false, false, funcionario.id)
            }
        }
        Mockito.verify(triagemRepository, Mockito.never()).saveAndFlush(qualquer<Triagem>())
    }

    @Test
    fun `funcionario inativo ou inexistente nao registra triagem`() {
        preparar()
        funcionario.desativar()

        assertFailsWith<AccessDeniedException> { realizar() }

        val desconhecido = UUID.randomUUID()
        Mockito.`when`(funcionarioRepository.findById(desconhecido)).thenReturn(Optional.empty())
        assertFailsWith<AccessDeniedException> {
            service.realizarTriagem(agendamento.id, BigDecimal("70"), false, false, false, desconhecido)
        }
    }

    @Test
    fun `consulta sem triagem devolve nao encontrado`() {
        Mockito.`when`(triagemRepository.findByAgendamentoId(agendamento.id)).thenReturn(Optional.empty())

        assertFailsWith<NoSuchElementException> { service.buscarPorAgendamento(agendamento.id) }
    }
}
