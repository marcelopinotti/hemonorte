package com.hemonorte.service

import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.SlotAgenda
import com.hemonorte.repository.AgendamentoRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class ComprovanteServiceTest {

    private val agendamentoRepository = Mockito.mock(AgendamentoRepository::class.java)
    private val service = ComprovanteService(agendamentoRepository)

    @Test
    fun `codigo emitido na coleta confirma doador e data local da doacao`() {
        val doador = Doador("Ana Souza", "ana@email.com", "hash", "O+", LocalDate.of(1990, 1, 1), 'F', "12345678909")
        val agendamento = Agendamento(doador, SlotAgenda(Instant.parse("2035-11-11T01:00:00Z"), 5)).also { it.concluir() }
        val codigo = agendamento.codigoComprovante!!
        Mockito.`when`(agendamentoRepository.findByCodigoComprovante(codigo)).thenReturn(agendamento)

        val comprovante = service.validar(codigo)

        assertEquals(true, comprovante.valido)
        assertEquals("Ana Souza", comprovante.doador)
        assertEquals(LocalDate.of(2035, 11, 10), comprovante.dataDoacao)
    }

    @Test
    fun `codigo desconhecido devolve nao encontrado`() {
        assertFailsWith<NoSuchElementException> { service.validar(UUID.randomUUID()) }
    }
}
