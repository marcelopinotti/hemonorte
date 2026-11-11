package com.hemonorte.model

import com.hemonorte.model.enums.PerfilFuncionario
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
        cpf = "12345678909",
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
            StatusBolsa.DISPONIVEL -> b.transicionarPara(StatusBolsa.DISPONIVEL, null, "motivo")
            StatusBolsa.UTILIZADA -> {
                b.transicionarPara(StatusBolsa.DISPONIVEL, null, "motivo")
                b.transicionarPara(StatusBolsa.UTILIZADA, null, "motivo")
            }
            StatusBolsa.DESCARTADA -> b.transicionarPara(StatusBolsa.DESCARTADA, null, "motivo")
            StatusBolsa.VENCIDA -> {
                b.transicionarPara(StatusBolsa.DISPONIVEL, null, "motivo")
                b.transicionarPara(StatusBolsa.VENCIDA, null, "motivo")
            }
        }
        return b
    }

    @Test
    fun `transicao gera registro de auditoria com estados, responsavel e motivo`() {
        val b = bolsa()
        val funcionario = Funcionario("LAB-1", "Lab", "lab@email.com", "hash", PerfilFuncionario.LABORATORIO)
        val registro = b.transicionarPara(StatusBolsa.DESCARTADA, funcionario, "  Bolsa rompida ")
        assertEquals(b, registro.bolsa)
        assertEquals(funcionario, registro.funcionario)
        assertEquals(StatusBolsa.EM_ANALISE, registro.statusAnterior)
        assertEquals(StatusBolsa.DESCARTADA, registro.statusNovo)
        assertEquals("Bolsa rompida", registro.motivo)
    }

    @Test
    fun `descarte sem motivo e rejeitado e nao altera o status`() {
        listOf(null, "", "   ").forEach { motivo ->
            val b = bolsa()
            val erro = assertFailsWith<IllegalArgumentException> { b.transicionarPara(StatusBolsa.DESCARTADA, null, motivo) }
            assertEquals("Descarte exige motivo", erro.message)
            assertEquals(StatusBolsa.EM_ANALISE, b.status)
        }
    }

    @Test
    fun `nasce em analise`() {
        assertEquals(StatusBolsa.EM_ANALISE, bolsa().status)
    }

    @Test
    fun `EM_ANALISE pode ir para DISPONIVEL`() {
        val b = bolsa()
        b.transicionarPara(StatusBolsa.DISPONIVEL, null, "motivo")
        assertEquals(StatusBolsa.DISPONIVEL, b.status)
    }

    @Test
    fun `EM_ANALISE pode ir para DESCARTADA`() {
        val b = bolsa()
        b.transicionarPara(StatusBolsa.DESCARTADA, null, "motivo")
        assertEquals(StatusBolsa.DESCARTADA, b.status)
    }

    @Test
    fun `EM_ANALISE nao pode ir direto para VENCIDA - regra de negocio em discussao (QA-010)`() {
        assertFailsWith<IllegalArgumentException> {
            bolsa().transicionarPara(StatusBolsa.VENCIDA, null, "motivo")
        }
    }

    @Test
    fun `DISPONIVEL pode ir para UTILIZADA, DESCARTADA e VENCIDA`() {
        StatusBolsa.entries.filter { it != StatusBolsa.DISPONIVEL && it != StatusBolsa.EM_ANALISE }.forEach { destino ->
            val b = bolsaNoStatus(StatusBolsa.DISPONIVEL)
            b.transicionarPara(destino, null, "motivo")
            assertEquals(destino, b.status)
        }
    }

    @Test
    fun `UTILIZADA nao aceita nenhuma transicao`() {
        StatusBolsa.entries.forEach { destino ->
            assertFailsWith<IllegalArgumentException>("de UTILIZADA para $destino deveria falhar") {
                bolsaNoStatus(StatusBolsa.UTILIZADA).transicionarPara(destino, null, "motivo")
            }
        }
    }

    @Test
    fun `DESCARTADA nao aceita nenhuma transicao`() {
        StatusBolsa.entries.forEach { destino ->
            assertFailsWith<IllegalArgumentException>("de DESCARTADA para $destino deveria falhar") {
                bolsaNoStatus(StatusBolsa.DESCARTADA).transicionarPara(destino, null, "motivo")
            }
        }
    }

    @Test
    fun `VENCIDA nao aceita nenhuma transicao`() {
        StatusBolsa.entries.forEach { destino ->
            assertFailsWith<IllegalArgumentException>("de VENCIDA para $destino deveria falhar") {
                bolsaNoStatus(StatusBolsa.VENCIDA).transicionarPara(destino, null, "motivo")
            }
        }
    }

    @Test
    fun `transicao para o mesmo estado e rejeitada`() {
        assertFailsWith<IllegalArgumentException> {
            bolsa().transicionarPara(StatusBolsa.EM_ANALISE, null, "motivo")
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
