package com.hemonorte.service

import com.hemonorte.model.Doador
import com.hemonorte.repository.DoadorRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("unit")
class DoadorServiceTest {

    private val repository = Mockito.mock(DoadorRepository::class.java)
    private val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val service = DoadorService(repository, passwordEncoder)

    @Test
    fun `cadastro persiste doador`() {
        Mockito.`when`(passwordEncoder.encode("senha-segura")).thenReturn("hash-seguro")
        Mockito.`when`(repository.saveAndFlush(Mockito.any(Doador::class.java))).thenAnswer { it.arguments[0] }

        val doador = service.cadastrar(
            nome = "Ana Souza",
            email = "ana@email.com",
            cpf = "12345678901",
            senha = "senha-segura",
            tipagem = "O+",
            dataNascimento = LocalDate.now().minusYears(30),
            genero = 'F',
        )

        assertEquals("hash-seguro", doador.senhaHash)
    }

    @Test
    fun `atualizacao cadastral preserva hash existente`() {
        val doador = Doador(
            nome = "Ana Souza",
            email = "ana@email.com",
            cpf = "12345678901",
            senhaHash = "hash-original",
            tipagem = "O+",
            dataNascimento = LocalDate.now().minusYears(30),
            genero = 'F',
        )
        Mockito.`when`(repository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(repository.saveAndFlush(doador)).thenReturn(doador)

        val atualizado = service.atualizar(
            id = doador.id,
            nome = "Ana Lima",
            email = "ana.lima@email.com",
            tipagem = doador.tipagem,
            dataNascimento = doador.dataNascimento,
            genero = doador.genero,
        )

        assertEquals("hash-original", atualizado.senhaHash)
    }
}
