package com.hemonorte.controller

import com.hemonorte.dto.FuncionarioResponse
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.service.FuncionarioService
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID
import kotlin.test.Test

@Tag("unit")
class FuncionarioControllerTest {

    private val service = Mockito.mock(FuncionarioService::class.java)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(FuncionarioController(service)).build()

    private fun resposta(matricula: String = "M123") = FuncionarioResponse(
        id = UUID.randomUUID(),
        matricula = matricula,
        nome = "Marcos Lima",
        email = "marcos@hemonorte.br",
        perfil = PerfilFuncionario.TRIAGEM,
        ativo = true,
    )

    @Test
    fun `PUT com id devolve 405 porque o mapping nao declara o segmento de id - QA-025`() {
        // Sentinela do defeito: comportamento atual observado em execucao e 405.
        // Contrato desejado apos a correcao do QA-025: 200 com o corpo atualizado (trocar a assercao aqui).
        mockMvc.perform(put("/api/funcionario/{id}", UUID.randomUUID()))
            .andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `DELETE com id devolve 405 porque o mapping nao declara o segmento de id - QA-025`() {
        // Sentinela do defeito: 405 atual; contrato desejado apos a correcao: 204.
        mockMvc.perform(delete("/api/funcionario/{id}", UUID.randomUUID()))
            .andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `GET lista funcionarios com 200 - contraste com PUT e DELETE quebrados`() {
        Mockito.`when`(service.listar()).thenReturn(listOf(resposta("M1"), resposta("M2")))

        mockMvc.perform(get("/api/funcionario"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].matricula").value("M1"))
    }

    @Test
    fun `GET por id devolve 200 com o funcionario`() {
        val id = UUID.randomUUID()
        Mockito.`when`(service.buscar(id)).thenReturn(resposta())

        mockMvc.perform(get("/api/funcionario/{id}", id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.matricula").value("M123"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
    }
}
