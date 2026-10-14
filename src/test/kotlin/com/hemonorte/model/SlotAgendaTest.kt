package com.hemonorte.model

import org.junit.jupiter.api.Tag
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

@Tag("unit")
class SlotAgendaTest {

    @Test
    fun `capacidade zero lanca excecao`() {
        assertFailsWith<IllegalArgumentException> {
            SlotAgenda(inicio = Instant.now().plusSeconds(3600), capacidade = 0)
        }
    }

    @Test
    fun `capacidade negativa lanca excecao`() {
        assertFailsWith<IllegalArgumentException> {
            SlotAgenda(inicio = Instant.now().plusSeconds(3600), capacidade = -1)
        }
    }

    @Test
    fun `capacidade um e aceita - borda`() {
        SlotAgenda(inicio = Instant.now().plusSeconds(3600), capacidade = 1)
    }

    @Test
    fun `inicio no passado lanca excecao`() {
        assertFailsWith<IllegalArgumentException> {
            SlotAgenda(inicio = Instant.now().minusSeconds(3600), capacidade = 5)
        }
    }

    @Test
    fun `inicio futuro e aceito`() {
        SlotAgenda(inicio = Instant.now().plusSeconds(3600), capacidade = 5)
    }
}
