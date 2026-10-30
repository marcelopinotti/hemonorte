package com.hemonorte.integration

import com.hemonorte.TestcontainersConfiguration
import com.hemonorte.dto.FuncionarioResponse
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.repository.FuncionarioRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("integration")
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
class AutorizacaoCadastroIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var funcionarioRepository: FuncionarioRepository

    @BeforeEach
    fun limparBaseDeTeste() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE historico_auditoria, exame_sorologico, bolsa_sangue, triagem,
                agendamento, slot_agenda, estoque_minimo, funcionario, doador
                RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }

    @Test
    fun `SENTINELA QA-001 - sem credencial e permitido resetar senha e elevar perfil para ADMIN`() {
        val senhaOriginal = "Senha-Original-123"
        val senhaNova = "Senha-Nova-456"
        val resultado = mockMvc.perform(
            post("/api/funcionario")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"matricula":"FUNC-SENTINELA","nome":"Funcionario Sentinelela",
                     "email":"sentinela@exemplo.local","senha":"$senhaOriginal","perfil":"TRIAGEM"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val funcionario = objectMapper.readValue(
            resultado.response.contentAsString,
            FuncionarioResponse::class.java,
        )
        val hashOriginal = funcionarioRepository.findById(funcionario.id).orElseThrow().senhaHash

        // Nenhum token, papel ou senha atual acompanha as duas requisicoes. A API atual
        // aceita as duas; com a cadeia de seguranca da #16, estas chamadas devem become 401/403.
        mockMvc.perform(
            put("/api/funcionario/{id}", funcionario.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Funcionario Sentinelela","email":"sentinela@exemplo.local","perfil":"ADMIN"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.perfil").value("ADMIN"))

        mockMvc.perform(
            put("/api/funcionario/{id}/senha", funcionario.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"senha":"$senhaNova"}"""),
        ).andExpect(status().isNoContent)

        val persistido = funcionarioRepository.findById(funcionario.id).orElseThrow()
        assertEquals(PerfilFuncionario.ADMIN, persistido.perfil)
        assertFalse(passwordEncoder.matches(senhaOriginal, persistido.senhaHash))
        assertTrue(passwordEncoder.matches(senhaNova, persistido.senhaHash))
    }
}
