package com.hemonorte.model

import org.junit.jupiter.api.Tag
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class RegraAptidaoDoacaoTest {

    @Test
    fun `sem historico permite doacao`() {
        val resultado = RegraAptidaoDoacao.avaliar('F', emptyList(), LocalDate.of(2026, 3, 1))

        assertTrue(resultado.apto)
        assertNull(resultado.motivo)
        assertNull(resultado.proximaDataPermitida)
    }

    @Test
    fun `intervalo masculino bloqueia ate completar sessenta dias`() {
        val resultado = RegraAptidaoDoacao.avaliar(
            'M',
            listOf(LocalDate.of(2026, 1, 1)),
            LocalDate.of(2026, 3, 1),
        )

        assertFalse(resultado.apto)
        assertEquals("Intervalo mínimo entre doações não cumprido", resultado.motivo)
        assertEquals(LocalDate.of(2026, 3, 2), resultado.proximaDataPermitida)
    }

    @Test
    fun `data exata do intervalo e permitida`() {
        val resultado = RegraAptidaoDoacao.avaliar(
            'F',
            listOf(LocalDate.of(2026, 1, 1)),
            LocalDate.of(2026, 4, 1),
        )

        assertTrue(resultado.apto)
    }

    @Test
    fun `limite anual usa janela movel de trezentos e sessenta e cinco dias`() {
        val resultado = RegraAptidaoDoacao.avaliar(
            'M',
            listOf(
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 11, 1),
                LocalDate.of(2025, 12, 1),
            ),
            LocalDate.of(2026, 3, 1),
        )

        assertFalse(resultado.apto)
        assertEquals("Limite de doações em 365 dias atingido", resultado.motivo)
        assertEquals(LocalDate.of(2026, 7, 1), resultado.proximaDataPermitida)
    }

    @Test
    fun `limite feminino permite no maximo tres doacoes na janela`() {
        val resultado = RegraAptidaoDoacao.avaliar(
            'F',
            listOf(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 12, 1),
            ),
            LocalDate.of(2026, 3, 1),
        )

        assertFalse(resultado.apto)
        assertEquals("Limite de doações em 365 dias atingido", resultado.motivo)
        assertEquals(LocalDate.of(2026, 6, 1), resultado.proximaDataPermitida)
    }

    @Test
    fun `doacao de exatamente trezentos e sessenta e cinco dias sai da janela`() {
        val resultado = RegraAptidaoDoacao.avaliar(
            'M',
            listOf(
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 12, 1),
            ),
            LocalDate.of(2026, 3, 1),
        )

        assertTrue(resultado.apto)
    }

    @Test
    fun `intervalo atravessa virada do ano por quantidade de dias`() {
        val resultado = RegraAptidaoDoacao.avaliar(
            'M',
            listOf(LocalDate.of(2025, 12, 31)),
            LocalDate.of(2026, 2, 28),
        )

        assertFalse(resultado.apto)
        assertEquals(LocalDate.of(2026, 3, 1), resultado.proximaDataPermitida)
    }
}
