package com.hemonorte.service

import com.hemonorte.model.EstoqueMinimo
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.EstoqueMinimoRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("unit")
class EstoqueServiceTest {

    private val minimoRepository = Mockito.mock(EstoqueMinimoRepository::class.java)
    private val bolsaRepository = Mockito.mock(BolsaSangueRepository::class.java)
    private val hoje = LocalDate.of(2035, 11, 10)
    private val service = EstoqueService(
        minimoRepository,
        bolsaRepository,
        Clock.fixed(Instant.parse("2035-11-10T10:00:00Z"), ZoneOffset.UTC),
    )

    private fun disponiveis(vararg contagem: Pair<String, Long>) {
        Mockito.`when`(bolsaRepository.contarDisponiveisPorTipagem(hoje))
            .thenReturn(contagem.map { arrayOf<Any>(it.first, it.second) })
    }

    @Test
    fun `agrega as oito tipagens preenchendo zero`() {
        disponiveis("O+" to 3L, "AB-" to 1L)

        assertEquals(
            mapOf("A+" to 0L, "A-" to 0L, "B+" to 0L, "B-" to 0L, "AB+" to 0L, "AB-" to 1L, "O+" to 3L, "O-" to 0L),
            service.disponiveisPorTipagem(),
        )
    }

    @Test
    fun `lista tipagem quando quantidade disponivel e igual ao limite`() {
        disponiveis("O+" to 2L)
        val minimo = EstoqueMinimo("O+", 2)
        Mockito.`when`(minimoRepository.findAll()).thenReturn(listOf(minimo))

        assertEquals(listOf(minimo), service.listarEstoquesCriticos())
    }

    @Test
    fun `nao lista tipagem acima do limite e considera vazio como critico`() {
        disponiveis("O+" to 3L)
        val acima = EstoqueMinimo("O+", 2)
        val vazio = EstoqueMinimo("A-", 1)
        Mockito.`when`(minimoRepository.findAll()).thenReturn(listOf(acima, vazio))

        assertEquals(listOf(vazio), service.listarEstoquesCriticos())
    }
}
