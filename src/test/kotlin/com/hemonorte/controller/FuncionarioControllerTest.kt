package com.hemonorte.controller

import com.hemonorte.dto.AtualizacaoFuncionarioRequest
import com.hemonorte.dto.CadastroFuncionarioRequest
import com.hemonorte.dto.FuncionarioResponse
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.service.FuncionarioService
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
    fun `PUT por id devolve 200 com o funcionario atualizado - contrato corrigido do QA-025`() {
        // Sentinela do 405 convertida em prova do contrato conforme a issue #15: o QA-025 foi
        // corrigido com @PutMapping("/{id}") e PUT em /api/funcionario/{id} agora alcanca o handler.
        val id = UUID.randomUUID()
        // Stub com argumentos concretos: o DTO e data class, entao a igualdade por valor
        // casa com a instancia que o controller desserializa do corpo. Sem matchers, evitando
        // o pitfall Kotlin+Mockito de eq()/any() devolverem null para parametros nao-nulos.
        val corpo = AtualizacaoFuncionarioRequest(nome = "Marcos Lima", email = "marcos@hemonorte.br")
        Mockito.`when`(service.atualizar(id, corpo)).thenReturn(resposta())

        mockMvc.perform(
            put("/api/funcionario/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nome":"Marcos Lima","email":"marcos@hemonorte.br"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.matricula").value("M123"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
    }

    @Test
    fun `DELETE por id devolve 204 - contrato corrigido do QA-025`() {
        // Sentinela do 405 convertida conforme a issue #15: o QA-025 foi corrigido com
        // @DeleteMapping("/{id}") e DELETE em /api/funcionario/{id} agora alcanca o handler.
        val id = UUID.randomUUID()

        mockMvc.perform(delete("/api/funcionario/{id}", id))
            .andExpect(status().isNoContent)

        Mockito.verify(service).excluir(id)
    }

    @Test
    fun `GET lista funcionarios com 200`() {
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

    @Test
    fun `POST cria funcionario com 201 sem expor senhaHash - contrato da issue 6`() {
        val corpo = CadastroFuncionarioRequest(
            matricula = "M123",
            nome = "Marcos Lima",
            email = "marcos@hemonorte.br",
            senhaHash = "hash",
            perfil = PerfilFuncionario.TRIAGEM,
        )
        Mockito.`when`(service.cadastrar(corpo)).thenReturn(resposta())

        mockMvc.perform(
            post("/api/funcionario")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"matricula":"M123","nome":"Marcos Lima","email":"marcos@hemonorte.br","senhaHash":"hash","perfil":"TRIAGEM"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.matricula").value("M123"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
    }

    @Test
    fun `PUT com corpo invalido devolve 400 - Bean Validation do contrato da issue 6`() {
        mockMvc.perform(
            put("/api/funcionario/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nome":"","email":"marcos@hemonorte.br"}""")
        )
            .andExpect(status().isBadRequest)
    }
}
