package com.hemonorte.model

import org.junit.jupiter.api.Tag
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@Tag("unit")
class PreTriagemTest {

    private val doador = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash",
        tipagem = "O+",
        dataNascimento = LocalDate.of(1990, 1, 1),
        genero = 'F',
        cpf = "12345678909",
    )

    private fun preTriagem(
        peso: String = "70.00",
        tatuagem: Boolean = false,
        cirurgia: Boolean = false,
        febre: Boolean = false,
    ) = PreTriagem(doador, BigDecimal(peso), tatuagem, cirurgia, febre, Instant.parse("2035-11-10T10:00:00Z"))

    @Test
    fun `respostas sem impedimento resultam em apta`() {
        val p = preTriagem(peso = "50.00")
        assertEquals(true, p.apto)
        assertNull(p.motivoInaptidao)
    }

    @Test
    fun `cada impedimento aparece no motivo usando a mesma regra da triagem`() {
        val p = preTriagem(peso = "49.99", tatuagem = true, cirurgia = true, febre = true)
        assertEquals(false, p.apto)
        assertEquals(
            "peso abaixo do mínimo (50.0kg); tatuagem recente; cirurgia recente; febre recente",
            p.motivoInaptidao,
        )
    }

    @Test
    fun `peso nao positivo e rejeitado`() {
        assertFailsWith<IllegalArgumentException> { preTriagem(peso = "0") }
    }

    @Test
    fun `validade e respostas ficam disponiveis para exibir`() {
        val p = preTriagem(febre = true)
        assertEquals(Instant.parse("2035-11-17T10:00:00Z"), p.validaAte)
        assertEquals(PerguntaPreTriagem.entries.toSet(), p.respostas().keys)
        assertEquals(true, p.respostas()[PerguntaPreTriagem.TEVE_FEBRE])
    }
}
