package com.hemonorte.model

import com.hemonorte.model.enums.StatusAgendamento
import org.junit.jupiter.api.Tag
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("unit")
class AgendamentoTest {

    private fun doador() = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.now().minusYears(30),
        genero = 'F',
        cpf = "12345678901",
    )

    private fun slot() = SlotAgenda(
        inicio = Instant.now().plus(2, ChronoUnit.HOURS),
        capacidade = 5,
    )

    private fun agendamento() = Agendamento(doador = doador(), slot = slot())

    @Test
    fun `nasce pendente sem comprovante`() {
        val a = agendamento()

        assertEquals(StatusAgendamento.PENDENTE, a.status)
        assertNull(a.codigoComprovante)
    }

    @Test
    fun `concluir gera comprovante`() {
        val a = agendamento()
        a.concluir()

        assertEquals(StatusAgendamento.CONCLUIDO, a.status)
        assertNotNull(a.codigoComprovante)
    }

    @Test
    fun `cancelar nao gera comprovante`() {
        val a = agendamento()
        a.cancelar()

        assertEquals(StatusAgendamento.CANCELADO, a.status)
        assertNull(a.codigoComprovante)
    }

    @Test
    fun `registrar falta move para FALTOU`() {
        val a = agendamento()
        a.registrarFalta()

        assertEquals(StatusAgendamento.FALTOU, a.status)
    }

    @Test
    fun `concluir cancelado e rejeitado`() {
        val a = agendamento()
        a.cancelar()

        assertFailsWith<IllegalArgumentException> { a.concluir() }
    }

    @Test
    fun `cancelar concluido e rejeitado`() {
        val a = agendamento()
        a.concluir()

        assertFailsWith<IllegalArgumentException> { a.cancelar() }
    }

    @Test
    fun `registrar falta em concluido e rejeitado`() {
        val a = agendamento()
        a.concluir()

        assertFailsWith<IllegalArgumentException> { a.registrarFalta() }
    }

    @Test
    fun `cancelar apos falta e rejeitado`() {
        val a = agendamento()
        a.registrarFalta()

        assertFailsWith<IllegalArgumentException> { a.cancelar() }
    }
}
