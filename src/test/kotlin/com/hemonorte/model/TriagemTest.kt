package com.hemonorte.model

import com.hemonorte.model.enums.PerfilFuncionario
import org.junit.jupiter.api.Tag
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class TriagemTest {

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

    private fun funcionario() = Funcionario(
        matricula = "M123",
        nome = "Marcos Lima",
        email = "marcos@hemonorte.br",
        senhaHash = "hash-valido",
        perfil = PerfilFuncionario.TRIAGEM,
    )

    private fun triagem(
        peso: String,
        tatuagem: Boolean = false,
        cirurgia: Boolean = false,
        febre: Boolean = false,
    ) = Triagem(
        agendamento = agendamento(),
        funcionario = funcionario(),
        peso = BigDecimal(peso),
        tatuagemRecente = tatuagem,
        cirurgiaRecente = cirurgia,
        teveFebre = febre,
    )

    @Test
    fun `peso abaixo do minimo deixa inapto com motivo especifico`() {
        val t = triagem("49.99")

        assertFalse(t.apto)
        assertEquals("peso abaixo do mínimo (50.0kg)", t.motivoInaptidao)
    }

    @Test
    fun `peso exatamente no minimo deixa apto - fronteira inclusiva`() {
        val t = triagem("50.00")

        assertTrue(t.apto)
        assertNull(t.motivoInaptidao)
    }

    @Test
    fun `peso acima do minimo deixa apto`() {
        val t = triagem("50.01")

        assertTrue(t.apto)
        assertNull(t.motivoInaptidao)
    }

    @Test
    fun `tatuagem recente inapta com motivo`() {
        val t = triagem("70.0", tatuagem = true)

        assertFalse(t.apto)
        assertEquals("tatuagem recente", t.motivoInaptidao)
    }

    @Test
    fun `cirurgia recente inapta com motivo`() {
        val t = triagem("70.0", cirurgia = true)

        assertFalse(t.apto)
        assertEquals("cirurgia recente", t.motivoInaptidao)
    }

    @Test
    fun `febre recente inapta com motivo`() {
        val t = triagem("70.0", febre = true)

        assertFalse(t.apto)
        assertEquals("febre recente", t.motivoInaptidao)
    }

    @Test
    fun `todas as contraindicacoes juntas concatenam motivos na ordem`() {
        val t = triagem("49.99", tatuagem = true, cirurgia = true, febre = true)

        assertFalse(t.apto)
        assertEquals(
            "peso abaixo do mínimo (50.0kg); tatuagem recente; cirurgia recente; febre recente",
            t.motivoInaptidao,
        )
    }

    @Test
    fun `peso zero lanca IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { triagem("0") }
    }

    @Test
    fun `peso negativo lanca IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { triagem("-5") }
    }
}
