package com.hemonorte.service

import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.DoadorRepository
import java.time.Instant
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("unit")
class DoadorServiceTest {

    private val repository = Mockito.mock(DoadorRepository::class.java)
    private val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val agendamentoRepository = Mockito.mock(AgendamentoRepository::class.java)
    private val service = DoadorService(repository, passwordEncoder, agendamentoRepository)

    @Test
    fun `inativacao preserva o registro e cancela reservas pendentes`() {
        val doador = Doador(
            nome = "Ana Souza",
            email = "ana@email.com",
            cpf = "12345678909",
            senhaHash = "hash",
            tipagem = "O+",
            dataNascimento = LocalDate.now().minusYears(30),
            genero = 'F',
        )
        val pendente = Agendamento(doador, SlotAgenda(Instant.now().plusSeconds(86_400), 5))
        Mockito.`when`(repository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(agendamentoRepository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.PENDENTE))
            .thenReturn(listOf(pendente))

        service.inativar(doador.id)

        assertEquals(false, doador.ativo)
        assertEquals(StatusAgendamento.CANCELADO, pendente.status)
        Mockito.verify(repository).saveAndFlush(doador)
        Mockito.verify(repository, Mockito.never()).delete(doador)
    }

    @Test
    fun `cadastro persiste doador`() {
        Mockito.`when`(passwordEncoder.encode("senha-segura")).thenReturn("hash-seguro")
        Mockito.`when`(repository.saveAndFlush(Mockito.any(Doador::class.java))).thenAnswer { it.arguments[0] }

        val doador = service.cadastrar(
            nome = "Ana Souza",
            email = "ana@email.com",
            cpf = "12345678909",
            senha = "senha-segura",
            tipagem = "O+",
            dataNascimento = LocalDate.now().minusYears(30),
            genero = 'F',
        )

        assertEquals("hash-seguro", doador.senhaHash)
    }

    @Test
    fun `atualizacao cadastral preserva hash existente`() {
        val doador = Doador(
            nome = "Ana Souza",
            email = "ana@email.com",
            cpf = "12345678909",
            senhaHash = "hash-original",
            tipagem = "O+",
            dataNascimento = LocalDate.now().minusYears(30),
            genero = 'F',
        )
        Mockito.`when`(repository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(repository.saveAndFlush(doador)).thenReturn(doador)

        val atualizado = service.atualizar(
            id = doador.id,
            nome = "Ana Lima",
            email = "ana.lima@email.com",
            tipagem = doador.tipagem,
            dataNascimento = doador.dataNascimento,
            genero = doador.genero,
        )

        assertEquals("hash-original", atualizado.senhaHash)
    }
}
