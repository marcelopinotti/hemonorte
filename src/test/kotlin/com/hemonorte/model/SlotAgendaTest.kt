package com.hemonorte.model

import org.junit.jupiter.api.Tag
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class SlotAgendaTest {

    private val agora = Instant.parse("2035-11-10T12:00:00Z")
    private val futuro = Instant.parse("2035-11-10T13:00:00Z")

    @Test
    fun `capacidade deve ser positiva`() {
        assertFailsWith<IllegalArgumentException> {
            SlotAgenda(futuro, 0, referencia = agora)
        }
    }

    @Test
    fun `inicio igual a referencia e rejeitado`() {
        assertFailsWith<IllegalArgumentException> {
            SlotAgenda(agora, 1, referencia = agora)
        }
    }

    @Test
    fun `reducao abaixo da ocupacao e rejeitada`() {
        val slot = SlotAgenda(futuro, 5, referencia = agora)

        assertFailsWith<IllegalArgumentException> {
            slot.atualizar(futuro, novaCapacidade = 1, ocupacao = 2, referencia = agora)
        }
    }

    @Test
    fun `capacidade igual a ocupacao e aceita`() {
        val slot = SlotAgenda(futuro, 5, referencia = agora)

        slot.atualizar(futuro, novaCapacidade = 2, ocupacao = 2, referencia = agora)

        assertEquals(2, slot.capacidade)
    }
}
