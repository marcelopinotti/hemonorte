package com.hemonorte.service

import com.hemonorte.model.Doador
import com.hemonorte.repository.DoadorRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import java.time.LocalDate
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("unit")
class DoadorServiceTest {

    private val repository = Mockito.mock(DoadorRepository::class.java)
    private val service = DoadorService(repository)

    @Test
    fun `cadastro persiste doador`() {
        Mockito.`when`(repository.save(Mockito.any(Doador::class.java))).thenAnswer { it.arguments[0] }

        val doador = service.cadastrar(
            nome = "Ana Souza",
            email = "ana@email.com",
            cpf = "12345678901",
            senhaHash = "hash-seguro",
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
        val hashOriginal = doador.senhaHash
        Mockito.`when`(repository.findById(doador.id)).thenReturn(Optional.of(doador))
        Mockito.`when`(repository.save(doador)).thenReturn(doador)

        val atualizado = service.atualizar(doador.id, "Ana Lima", "ana.lima@email.com", hashOriginal)

        assertEquals(hashOriginal, atualizado.senhaHash)
    }
}
