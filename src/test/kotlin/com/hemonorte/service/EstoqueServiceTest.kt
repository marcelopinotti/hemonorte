package com.hemonorte.service

import com.hemonorte.model.EstoqueMinimo
import com.hemonorte.repository.EstoqueMinimoRepository
import com.hemonorte.repository.ExameSorologicoRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("unit")
class EstoqueServiceTest {

    private val minimoRepository = Mockito.mock(EstoqueMinimoRepository::class.java)
    private val exameRepository = Mockito.mock(ExameSorologicoRepository::class.java)
    private val service = EstoqueService(minimoRepository, exameRepository)

    @Test
    fun `lista tipagem quando quantidade disponivel e igual ao limite`() {
        val minimo = EstoqueMinimo("O+", 2)
        Mockito.`when`(minimoRepository.findAll()).thenReturn(listOf(minimo))

        assertEquals(listOf(minimo), service.listarEstoquesCriticos())
    }

}
