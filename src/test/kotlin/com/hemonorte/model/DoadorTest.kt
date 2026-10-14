package com.hemonorte.model

import org.junit.jupiter.api.Tag
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class DoadorTest {

    private fun doador(
        nome: String = "Ana Souza",
        email: String = "ana@email.com",
        senhaHash: String = "hash-valido",
        tipagem: String = "O+",
        dataNascimento: LocalDate = LocalDate.now().minusYears(30),
        genero: Char = 'F',
        cpf: String = "12345678901",
    ) = Doador(
        nome = nome,
        email = email,
        senhaHash = senhaHash,
        tipagem = tipagem,
        dataNascimento = dataNascimento,
        genero = genero,
        cpf = cpf,
    )

    // --- nome ---

    @Test
    fun `nome em branco lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { doador(nome = "   ") }
    }

    @Test
    fun `nome com espacos nas bordas e trimado`() {
        assertEquals("Ana Souza", doador(nome = "  Ana Souza  ").nome)
    }

    // --- e-mail ---

    @Test
    fun `email sem arroba lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { doador(email = "ana.email.com") }
    }

    @Test
    fun `email com arroba e trimado - regra fraca documentada (QA-006)`() {
        // A entidade só exige '@'; a validação de formato fica no DTO (@Email).
        assertEquals("a@b", doador(email = " a@b ").email)
    }

    @Test
    fun `alterarEmail revalida a regra`() {
        val d = doador()

        assertFailsWith<IllegalArgumentException> { d.alterarEmail("sem-arroba") }
    }

    // --- tipagem ---

    @Test
    fun `tipagem invalida lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { doador(tipagem = "C+") }
    }

    @Test
    fun `tipagem minuscula e normalizada`() {
        assertEquals("O+", doador(tipagem = "o+").tipagem)
    }

    @Test
    fun `todas as oito tipagens validas sao aceitas`() {
        listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-").forEach { t ->
            assertEquals(t, doador(tipagem = t).tipagem)
        }
    }

    // --- gênero ---

    @Test
    fun `genero M e F sao aceitos e outro valor lanca`() {
        assertEquals('M', doador(genero = 'M').genero)
        assertEquals('F', doador(genero = 'F').genero)
        assertFailsWith<IllegalArgumentException> { doador(genero = 'X') }
    }

    // --- CPF ---

    @Test
    fun `cpf precisa de exatamente 11 digitos`() {
        assertFailsWith<IllegalArgumentException> { doador(cpf = "1234567890") }
        assertFailsWith<IllegalArgumentException> { doador(cpf = "123456789012") }
        assertFailsWith<IllegalArgumentException> { doador(cpf = "1234567890a") }
        assertEquals("12345678901", doador().cpf)
    }

    @Test
    fun `cpf de onze zeros e aceito - ausencia de digito verificador documentada (QA-007)`() {
        assertEquals("00000000000", doador(cpf = "00000000000").cpf)
    }

    // --- data de nascimento (correção dos devs) ---

    @Test
    fun `data de nascimento futura lanca excecao - correcao dos devs verificada`() {
        assertFailsWith<IllegalArgumentException> {
            doador(dataNascimento = LocalDate.now().plusDays(1))
        }
    }

    @Test
    fun `data de nascimento de hoje e aceita - borda`() {
        assertEquals(LocalDate.now(), doador(dataNascimento = LocalDate.now()).dataNascimento)
    }

    @Test
    fun `data de nascimento passada e aceita`() {
        val nascimento = LocalDate.now().minusYears(30)
        assertEquals(nascimento, doador(dataNascimento = nascimento).dataNascimento)
    }

    // --- senha hash ---

    @Test
    fun `senha hash em branco lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { doador(senhaHash = " ") }
    }

    @Test
    fun `alterarSenha revalida`() {
        val d = doador()

        assertFailsWith<IllegalArgumentException> { d.alterarSenha("") }
    }
}
