package com.hemonorte.service

import com.hemonorte.model.Doador
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Tag("unit")
class AptidaoServiceTest {

    private val repository = Mockito.mock(AgendamentoRepository::class.java)
    private val referencia = Instant.parse("2026-03-01T12:00:00Z")
    private val service = AptidaoService(repository, Clock.fixed(referencia, ZoneOffset.UTC))

    @Test
    fun `service consulta somente doacoes concluidas e respeita fuso de Fortaleza`() {
        val doador = doador('M')
        val slot = SlotAgenda(
            Instant.parse("2026-01-02T02:30:00Z"),
            1,
            referencia = Instant.parse("2025-01-01T00:00:00Z"),
        )
        val concluido = com.hemonorte.model.Agendamento(doador, slot).also { it.concluir() }
        Mockito.`when`(
            repository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO),
        ).thenReturn(listOf(concluido))

        val resultado = service.verificarAptidao(doador)

        assertFalse(resultado.apto)
        assertEquals(LocalDate.of(2026, 3, 2), resultado.proximaDataPermitida)
    }

    private fun doador(genero: Char) = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.of(1990, 1, 1),
        genero = genero,
        cpf = "12345678909",
    )
}
