package com.hemonorte.service

import com.hemonorte.model.enums.PerfilFuncionario
import org.junit.jupiter.api.Tag
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class JwtServiceTest {

    @Test
    fun `chave menor que 64 bytes impede a aplicacao de subir com mensagem clara`() {
        val erro = assertFailsWith<IllegalArgumentException> { JwtService("a".repeat(32), 60_000) }
        assertEquals("JWT_SECRET deve ter pelo menos 64 bytes (HS512); recebido: 32", erro.message)
    }

    @Test
    fun `chave de 64 bytes emite e valida token`() {
        val servico = JwtService("a".repeat(64), 60_000)
        val id = UUID.randomUUID()

        val claims = servico.validarToken(servico.gerarToken(id, "FUNCIONARIO", PerfilFuncionario.ADMIN))

        assertEquals(id.toString(), claims.subject)
        assertEquals("ADMIN", claims["perfil"])
    }
}
