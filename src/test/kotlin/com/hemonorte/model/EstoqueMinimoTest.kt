package com.hemonorte.model

import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class EstoqueMinimoTest {

    private fun estoque(
        tipagem: String = "O+",
        quantidade: Int = 10,
    ) = EstoqueMinimo(tipagem = tipagem, quantidadeCritica = quantidade)

    @Test
    fun `tipagem invalida lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { estoque(tipagem = "Z+") }
    }

    @Test
    fun `tipagem minuscula e normalizada`() {
        assertEquals("AB-", estoque(tipagem = "ab-").tipagem)
    }

    @Test
    fun `quantidade zero lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { estoque(quantidade = 0) }
    }

    @Test
    fun `quantidade negativa lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { estoque(quantidade = -5) }
    }

    @Test
    fun `quantidade um e aceita - borda`() {
        assertEquals(1, estoque(quantidade = 1).quantidadeCritica)
    }

    @Test
    fun `atualizarQuantidade invalida lanca e mantem o valor anterior - efeito colateral`() {
        val e = estoque(quantidade = 10)

        assertFailsWith<IllegalArgumentException> { e.atualizarQuantidade(0) }
        assertEquals(10, e.quantidadeCritica)
    }
}
