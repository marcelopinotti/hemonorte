package com.hemonorte.dto

import jakarta.validation.Validation
import org.junit.jupiter.api.Tag
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

@Tag("unit")
class CadastroDoadorRequestValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    private fun request(
        nome: String = "Ana Souza",
        email: String = "ana@email.com",
        cpf: String = "12345678901",
        senhaHash: String = "hash",
        tipagem: String = "O+",
        dataNascimento: LocalDate = LocalDate.now().minusYears(30),
        genero: Char = 'F',
    ) = CadastroDoadorRequest(
        nome = nome,
        email = email,
        cpf = cpf,
        senhaHash = senhaHash,
        tipagem = tipagem,
        dataNascimento = dataNascimento,
        genero = genero,
    )

    @Test
    fun `payload totalmente valido nao gera violacoes`() {
        val violations = validator.validate(request())

        assertTrue(violations.isEmpty(), "esperava zero violacoes, veio: $violations")
    }

    @Test
    fun `nome em branco gera violacao em nome`() {
        assertTrue(
            validator.validate(request(nome = "")).any { it.propertyPath.toString() == "nome" },
            "esperava violacao em 'nome'",
        )
    }

    @Test
    fun `email invalido gera violacao em email`() {
        assertTrue(
            validator.validate(request(email = "ana.email.com")).any { it.propertyPath.toString() == "email" },
            "esperava violacao em 'email'",
        )
    }

    @Test
    fun `cpf com 10 digitos gera violacao em cpf`() {
        assertTrue(
            validator.validate(request(cpf = "1234567890")).any { it.propertyPath.toString() == "cpf" },
            "esperava violacao em 'cpf'",
        )
    }

    @Test
    fun `tipagem invalida gera violacao em tipagem`() {
        assertTrue(
            validator.validate(request(tipagem = "X+")).any { it.propertyPath.toString() == "tipagem" },
            "esperava violacao em 'tipagem'",
        )
    }

    @Test
    fun `senhaHash em branco gera violacao em senhaHash`() {
        assertTrue(
            validator.validate(request(senhaHash = "")).any { it.propertyPath.toString() == "senhaHash" },
            "esperava violacao em 'senhaHash'",
        )
    }

    @Test
    fun `genero fora de M e F gera violacao em genero`() {
        // Nota de QA: @Pattern suporta apenas CharSequence. Se o Hibernate Validator
        // lancar ValidationException aqui em vez de violacao, o endpoint POST /api/doador
        // esta quebrado em todos os casos - defeito a registrar com esta execucao como evidencia.
        assertTrue(
            validator.validate(request(genero = 'X')).any { it.propertyPath.toString() == "genero" },
            "esperava violacao em 'genero'",
        )
    }
}
