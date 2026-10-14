package com.hemonorte.model

import com.hemonorte.model.enums.StatusBolsa
import org.junit.jupiter.api.Tag
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class BolsaSangueTest {

    private fun doador() = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.now().minusYears(30),
        genero = 'F',
        cpf = "12345678901",
    )

    private fun slot() = SlotAgenda(
        inicio = Instant.now().plus(2, ChronoUnit.HOURS),
        capacidade = 5,
    )

    private fun agendamento() = Agendamento(doador = doador(), slot = slot())

    private fun bolsa(
        tipagem: String = "O+",
        coleta: LocalDate = LocalDate.now().minusDays(1),
        validade: LocalDate = LocalDate.now().plusDays(41),
    ) = BolsaSangue(
        agendamento = agendamento(),
        tipagem = tipagem,
        dataColeta = coleta,
        dataValidade = validade,
    )

    private fun bolsaNoStatus(status: StatusBolsa): BolsaSangue {
        val b = bolsa()
        when (status) {
            StatusBolsa.EM_ANALISE -> {}
            StatusBolsa.DISPONIVEL -> b.transicionarPara(StatusBolsa.DISPONIVEL)
            StatusBolsa.UTILIZADA -> {
                b.transicionarPara(StatusBolsa.DISPONIVEL)
                b.transicionarPara(StatusBolsa.UTILIZADA)
            }
            StatusBolsa.DESCARTADA -> b.transicionarPara(StatusBolsa.DESCARTADA)
            StatusBolsa.VENCIDA -> {
                b.transicionarPara(StatusBolsa.DISPONIVEL)
                b.transicionarPara(StatusBolsa.VENCIDA)
            }
        }
        return b
    }

    @Test
    fun `nasce em analise`() {
        assertEquals(StatusBolsa.EM_ANALISE, bolsa().status)
    }

    @Test
    fun `EM_ANALISE pode ir para DISPONIVEL`() {
        val b = bolsa()
        b.transicionarPara(StatusBolsa.DISPONIVEL)
        assertEquals(StatusBolsa.DISPONIVEL, b.status)
    }

    @Test
    fun `EM_ANALISE pode ir para DESCARTADA`() {
        val b = bolsa()
        b.transicionarPara(StatusBolsa.DESCARTADA)
        assertEquals(StatusBolsa.DESCARTADA, b.status)
    }

    @Test
    fun `EM_ANALISE nao pode ir direto para VENCIDA - regra de negocio em discussao (QA-010)`() {
        assertFailsWith<IllegalArgumentException> {
            bolsa().transicionarPara(StatusBolsa.VENCIDA)
        }
    }

    @Test
    fun `DISPONIVEL pode ir para UTILIZADA, DESCARTADA e VENCIDA`() {
        StatusBolsa.entries.filter { it != StatusBolsa.DISPONIVEL && it != StatusBolsa.EM_ANALISE }.forEach { destino ->
            val b = bolsaNoStatus(StatusBolsa.DISPONIVEL)
            b.transicionarPara(destino)
            assertEquals(destino, b.status)
        }
    }

    @Test
    fun `UTILIZADA nao aceita nenhuma transicao`() {
        StatusBolsa.entries.forEach { destino ->
            assertFailsWith<IllegalArgumentException>("de UTILIZADA para $destino deveria falhar") {
                bolsaNoStatus(StatusBolsa.UTILIZADA).transicionarPara(destino)
            }
        }
    }

    @Test
    fun `DESCARTADA nao aceita nenhuma transicao`() {
        StatusBolsa.entries.forEach { destino ->
            assertFailsWith<IllegalArgumentException>("de DESCARTADA para $destino deveria falhar") {
                bolsaNoStatus(StatusBolsa.DESCARTADA).transicionarPara(destino)
            }
        }
    }

    @Test
    fun `VENCIDA nao aceita nenhuma transicao`() {
        StatusBolsa.entries.forEach { destino ->
            assertFailsWith<IllegalArgumentException>("de VENCIDA para $destino deveria falhar") {
                bolsaNoStatus(StatusBolsa.VENCIDA).transicionarPara(destino)
            }
        }
    }

    @Test
    fun `transicao para o mesmo estado e rejeitada`() {
        assertFailsWith<IllegalArgumentException> {
            bolsa().transicionarPara(StatusBolsa.EM_ANALISE)
        }
    }

    @Test
    fun `tipagem invalida e rejeitada`() {
        assertFailsWith<IllegalArgumentException> { bolsa(tipagem = "C+") }
    }

    @Test
    fun `tipagem minuscula e normalizada para maiuscula`() {
        assertEquals("AB-", bolsa(tipagem = "ab-").tipagem)
    }

    @Test
    fun `validade antes da coleta e rejeitada`() {
        assertFailsWith<IllegalArgumentException> {
            bolsa(coleta = LocalDate.now(), validade = LocalDate.now().minusDays(1))
        }
    }

    @Test
    fun `validade igual a coleta e rejeitada - borda exclusiva`() {
        val hoje = LocalDate.now()
        assertFailsWith<IllegalArgumentException> {
            bolsa(coleta = hoje, validade = hoje)
        }
    }

    @Test
    fun `validade um dia apos a coleta e aceita - borda`() {
        val hoje = LocalDate.now()
        assertEquals(hoje.plusDays(1), bolsa(coleta = hoje, validade = hoje.plusDays(1)).dataValidade)
    }
}
