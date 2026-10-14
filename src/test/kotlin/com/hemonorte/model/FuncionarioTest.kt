package com.hemonorte.model

import com.hemonorte.model.enums.PerfilFuncionario
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("unit")
class FuncionarioTest {

    private fun funcionario(
        matricula: String = "M123",
        nome: String = "Marcos Lima",
        email: String = "marcos@hemonorte.br",
        senhaHash: String = "hash-valido",
        perfil: PerfilFuncionario = PerfilFuncionario.TRIAGEM,
    ) = Funcionario(
        matricula = matricula,
        nome = nome,
        email = email,
        senhaHash = senhaHash,
        perfil = perfil,
    )

    @Test
    fun `nasce ativo`() {
        assertTrue(funcionario().ativo)
    }

    @Test
    fun `matricula em branco lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { funcionario(matricula = "   ") }
    }

    @Test
    fun `matricula com espacos nas bordas e trimada`() {
        assertEquals("M123", funcionario(matricula = "  M123  ").matricula)
    }

    @Test
    fun `matricula com 50 caracteres e aceita - borda`() {
        val matricula = "M".repeat(50)
        assertEquals(matricula, funcionario(matricula = matricula).matricula)
    }

    @Test
    fun `matricula com 51 caracteres lanca - borda`() {
        assertFailsWith<IllegalArgumentException> { funcionario(matricula = "M".repeat(51)) }
    }

    @Test
    fun `nome em branco lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { funcionario(nome = " ") }
    }

    @Test
    fun `email sem arroba lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { funcionario(email = "marcos.hemonorte.br") }
    }

    @Test
    fun `senha em branco lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { funcionario(senhaHash = "") }
    }

    @Test
    fun `desativar inativa o funcionario`() {
        val f = funcionario()
        f.desativar()
        assertFalse(f.ativo)
    }

    @Test
    fun `alterarNome trima e revalida`() {
        val f = funcionario()
        f.alterarNome("  Novo Nome  ")
        assertEquals("Novo Nome", f.nome)
        assertFailsWith<IllegalArgumentException> { f.alterarNome("") }
    }

    @Test
    fun `alterarSenha revalida`() {
        val f = funcionario()
        assertFailsWith<IllegalArgumentException> { f.alterarSenha(" ") }
    }
}
