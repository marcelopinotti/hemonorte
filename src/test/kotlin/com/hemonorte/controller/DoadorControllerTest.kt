package com.hemonorte.controller

import com.hemonorte.model.Doador
import com.hemonorte.service.DoadorService
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

@Tag("unit")
class DoadorControllerTest {

    private val service = Mockito.mock(DoadorService::class.java)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(DoadorController(service))
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    private fun doador() = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.now().minusYears(30),
        genero = 'F',
        cpf = "12345678901",
    )

    @Test
    fun `GET por id inexistente devolve 404 com corpo de erro - contrato do handler`() {
        val id = UUID.randomUUID()
        Mockito.`when`(service.buscar(id)).thenThrow(NoSuchElementException("Doador não encontrado"))

        mockMvc.perform(get("/api/doador/{id}", id))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.erro").value("Doador não encontrado"))
    }

    @Test
    fun `GET por id existente devolve 200 sem expor senhaHash - regressao do QA-008`() {
        val id = UUID.randomUUID()
        Mockito.`when`(service.buscar(id)).thenReturn(doador())

        mockMvc.perform(get("/api/doador/{id}", id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cpf").value("12345678901"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
    }

    @Test
    fun `GET lista devolve 200 com os doadores`() {
        Mockito.`when`(service.listar()).thenReturn(listOf(doador()))

        mockMvc.perform(get("/api/doador"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].nome").value("Ana Souza"))
    }

    @Test
    fun `POST com conflito de unicidade devolve 409 - contrato do handler para duplicidade`() {
        // Caminho da corrida de unicidade (QA-028): quando o banco rejeita por CPF/e-mail
        // duplicados, o handler responde 409 - hoje com a mensagem fixa do QA-027. Quando
        // melhorarem a mensagem por conflito, atualizar a assercao conscientemente.
        Mockito.`when`(
            service.cadastrar(
                nome = "Ana Souza",
                email = "ana@email.com",
                cpf = "12345678901",
                senhaHash = "hash-valido",
                tipagem = "O+",
                dataNascimento = LocalDate.of(1996, 8, 15),
                genero = 'F',
            )
        ).thenThrow(DataIntegrityViolationException("duplicidade de CPF"))

        mockMvc.perform(
            post("/api/doador")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nome":"Ana Souza","email":"ana@email.com","cpf":"12345678901","senhaHash":"hash-valido","tipagem":"O+","dataNascimento":"1996-08-15","genero":"F"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.erro").value("Doador possui dados vinculados"))
    }
}
