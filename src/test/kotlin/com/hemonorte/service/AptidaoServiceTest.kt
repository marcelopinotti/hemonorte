package com.hemonorte.service

import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class AptidaoServiceTest {

    private val repository = Mockito.mock(AgendamentoRepository::class.java)
    private val service = AptidaoService(repository)

    private fun doador(genero: Char) = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.now().minusYears(30),
        genero = genero,
        cpf = "12345678901",
    )

    // Slots reais com datas controladas: a entidade exige inicio futuro e o service recebe
    // a data de referencia como parametro - os cenarios avaliam a aptidao em datas
    // posteriores a da "doacao", sem mock de entidade (stubbing aninhado em thenReturn e
    // getters de entidade sao frageis; fixture real nao e).
    private fun slotEm(data: LocalDate): SlotAgenda =
        SlotAgenda(data.atStartOfDay(ZoneId.systemDefault()).toInstant(), 5)

    @Test
    fun `sem historico concluido devolve apto e consulta apenas concluidos`() {
        val doador = doador('M')
        Mockito.`when`(
            repository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO)
        ).thenReturn(emptyList())

        val resultado = service.verificarAptidao(doador)

        assertTrue(resultado.apto)
        assertNull(resultado.motivo)
        assertNull(resultado.proximaDataPermitida)
        // Cancelamentos e faltas nao entram no historico: o filtro por CONCLUIDO e o contrato
        Mockito.verify(repository).findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO)
    }

    @Test
    fun `homem avaliado 30 dias apos a doacao fica inapto com motivo e proxima data`() {
        val doador = doador('M')
        val dataDaDoacao = LocalDate.of(2027, 1, 5)
        val historico = listOf(Agendamento(doador = doador, slot = slotEm(dataDaDoacao)))
        Mockito.`when`(
            repository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO)
        ).thenReturn(historico)

        val resultado = service.verificarAptidao(doador, LocalDate.of(2027, 2, 4))

        assertFalse(resultado.apto)
        assertEquals("Intervalo mínimo entre doações não respeitado.", resultado.motivo)
        assertEquals(LocalDate.of(2027, 3, 6), resultado.proximaDataPermitida)
    }

    @Test
    fun `homem avaliado exatamente na data permitida fica apto - limite inclusivo`() {
        val doador = doador('M')
        val dataDaDoacao = LocalDate.of(2027, 1, 5)
        val historico = listOf(Agendamento(doador = doador, slot = slotEm(dataDaDoacao)))
        Mockito.`when`(
            repository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO)
        ).thenReturn(historico)

        val resultado = service.verificarAptidao(doador, LocalDate.of(2027, 3, 6))

        assertTrue(resultado.apto)
    }

    @Test
    fun `mulher usa intervalo de 90 dias - homem com o mesmo historico ja esta apto`() {
        val dataDaDoacao = LocalDate.of(2027, 1, 5)
        val avaliacao = LocalDate.of(2027, 3, 26) // 80 dias depois da doacao

        val mulher = doador('F')
        val historicoMulher = listOf(Agendamento(doador = mulher, slot = slotEm(dataDaDoacao)))
        Mockito.`when`(
            repository.findByDoadorIdAndStatus(mulher.id, StatusAgendamento.CONCLUIDO)
        ).thenReturn(historicoMulher)
        assertFalse(service.verificarAptidao(mulher, avaliacao).apto)

        val homem = doador('M')
        val historicoHomem = listOf(Agendamento(doador = homem, slot = slotEm(dataDaDoacao)))
        Mockito.`when`(
            repository.findByDoadorIdAndStatus(homem.id, StatusAgendamento.CONCLUIDO)
        ).thenReturn(historicoHomem)
        assertTrue(service.verificarAptidao(homem, avaliacao).apto)
    }

    @Test
    fun `considera apenas a doacao mais recente quando ha varias concluidas`() {
        val doador = doador('M')
        val maisRecente = LocalDate.of(2027, 6, 1)
        val maisAntiga = LocalDate.of(2027, 1, 5)
        val historico = listOf(
            Agendamento(doador = doador, slot = slotEm(maisAntiga)),
            Agendamento(doador = doador, slot = slotEm(maisRecente)),
        )
        Mockito.`when`(
            repository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO)
        ).thenReturn(historico)

        // 30 dias apos a mais recente: se o service considerasse a antiga, estaria apto
        val resultado = service.verificarAptidao(doador, LocalDate.of(2027, 7, 1))

        assertFalse(resultado.apto)
        assertEquals(LocalDate.of(2027, 7, 31), resultado.proximaDataPermitida)
    }
}
