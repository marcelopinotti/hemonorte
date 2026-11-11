package com.hemonorte.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("unit")
class AlertaEstoqueServiceTest {

    private val estoqueService = Mockito.mock(EstoqueService::class.java)
    private val registry = SimpleMeterRegistry()
    private val service = AlertaEstoqueService(estoqueService, registry)

    private fun gauge(nome: String, tipagem: String) = registry.get(nome).tag("tipagem", tipagem).gauge().value()

    @Test
    fun `verificacao publica disponiveis e sinaliza somente tipagens criticas`() {
        Mockito.`when`(estoqueService.disponiveisPorTipagem())
            .thenReturn(EstoqueService.TIPAGENS.associateWith { 5L } + ("O-" to 1L))
        Mockito.`when`(estoqueService.listarEstoquesCriticos()).thenReturn(listOf(EstoqueCritico("O-", 1L, 3)))

        assertEquals(listOf(EstoqueCritico("O-", 1L, 3)), service.verificar())
        assertEquals(1.0, gauge("hemonorte.estoque.critico", "O-"))
        assertEquals(0.0, gauge("hemonorte.estoque.critico", "O+"))
        assertEquals(1.0, gauge("hemonorte.estoque.disponivel", "O-"))
        assertEquals(5.0, gauge("hemonorte.estoque.disponivel", "A+"))
    }

    @Test
    fun `tipagem que sai do nivel critico deixa de alertar`() {
        Mockito.`when`(estoqueService.disponiveisPorTipagem()).thenReturn(EstoqueService.TIPAGENS.associateWith { 0L })
        Mockito.`when`(estoqueService.listarEstoquesCriticos())
            .thenReturn(listOf(EstoqueCritico("A+", 0L, 2)))
            .thenReturn(emptyList())

        service.verificar()
        service.verificar()

        assertEquals(0.0, gauge("hemonorte.estoque.critico", "A+"))
    }
}
