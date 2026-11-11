package com.hemonorte.integration

import com.hemonorte.TestcontainersConfiguration
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("integration")
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
class DocumentacaoOpenApiIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc

    private fun conteudo(url: String) =
        mockMvc.perform(get(url)).andExpect(status().isOk).andReturn().response.getContentAsString(Charsets.UTF_8)

    @Test
    fun `openapi yaml servido ao swagger documenta exatamente as operacoes do codigo`() {
        val arquivo = conteudo("/openapi.yaml")
        val gerado = conteudo("/v3/api-docs.yaml")

        fun operacoes(yaml: String) = Regex("""^\s+operationId: (\S+)$""", RegexOption.MULTILINE)
            .findAll(yaml).map { it.groupValues[1] }.sorted().toList()
        fun caminhos(yaml: String) = Regex("""^  (/\S*):$""", RegexOption.MULTILINE)
            .findAll(yaml).map { it.groupValues[1] }.sorted().toList()

        assertEquals(
            caminhos(gerado),
            caminhos(arquivo),
            message = "openapi.yaml desatualizado; regenere com: curl -o src/main/resources/static/openapi.yaml http://localhost:8080/v3/api-docs.yaml",
        )
        assertEquals(operacoes(gerado), operacoes(arquivo), message = "openapi.yaml desatualizado")
        assertEquals(52, operacoes(arquivo).size)
    }
}
