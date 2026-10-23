package com.hemonorte.service

import com.hemonorte.model.SlotAgenda
import com.hemonorte.repository.SlotAgendaRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Tag("unit")
class SlotAgendaServiceTest {

    private val repository = Mockito.mock(SlotAgendaRepository::class.java)
    private val service = SlotAgendaService(repository)

    // Matcher do Mockito com cara de nao-nulo pro compilador Kotlin: o any() bruto devolve
    // null em runtime e o check de parametro nao-nulo explode antes de alcancar o mock
    // (pitfall registrado na rodada anterior). O "as T" unchecked deixa o null passar.
    @Suppress("UNCHECKED_CAST")
    private fun <T> qualquer(): T = Mockito.any() as T

    @Test
    fun `cria slot com inicio futuro e persiste`() {
        val inicio = Instant.now().plusSeconds(86_400)
        Mockito.`when`(repository.existsByInicio(inicio)).thenReturn(false)
        Mockito.`when`(repository.save(qualquer())).thenAnswer { it.arguments[0] as SlotAgenda }

        val slot = service.criarSlot(inicio, 10)

        assertEquals(inicio, slot.inicio)
        assertEquals(10, slot.capacidade)
        Mockito.verify(repository).save(slot)
    }

    @Test
    fun `rejeita inicio duplicado`() {
        val inicio = Instant.now().plusSeconds(86_400)
        Mockito.`when`(repository.existsByInicio(inicio)).thenReturn(true)

        val erro = assertFailsWith<IllegalArgumentException> { service.criarSlot(inicio, 10) }
        assertEquals("Já existe um slot cadastrado para este horário", erro.message)
    }

    @Test
    fun `rejeita inicio no passado - guarda temporal da entidade`() {
        // O bloqueio vive na entidade (SlotAgenda exige inicio futuro na construcao) e o
        // service repassa - criterio da issue #8: nao criar horario no passado.
        val passado = Instant.now().minusSeconds(86_400)
        Mockito.`when`(repository.existsByInicio(passado)).thenReturn(false)

        val erro = assertFailsWith<IllegalArgumentException> { service.criarSlot(passado, 10) }
        assertTrue(erro.message!!.contains("data futura"))
        Mockito.verify(repository, Mockito.never()).save(qualquer())
    }

    @Test
    fun `consulta por periodo delega ao repositorio`() {
        val de = Instant.now().plusSeconds(3_600)
        val ate = de.plusSeconds(86_400)
        val esperado = listOf(SlotAgenda(de, 5))
        Mockito.`when`(repository.findByInicioBetween(de, ate)).thenReturn(esperado)

        assertEquals(esperado, service.listarPorPeriodo(de, ate))
    }
}
