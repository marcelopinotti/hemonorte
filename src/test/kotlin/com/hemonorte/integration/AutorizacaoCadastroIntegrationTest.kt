package com.hemonorte.integration

import com.hemonorte.TestcontainersConfiguration
import com.hemonorte.model.Funcionario
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `sem credencial nao permite elevar perfil nem resetar senha de funcionario`() {
        val senhaOriginal = "hash-original"
        val funcionario = funcionarioRepository.saveAndFlush(
            Funcionario(
                matricula = "FUNC-SENTINELA",
                nome = "Funcionario Sentinela",
                email = "sentinela@exemplo.local",
                senhaHash = senhaOriginal,
                perfil = PerfilFuncionario.TRIAGEM,
            ),
        )

        mockMvc.perform(
            put("/api/funcionario/{id}", funcionario.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Funcionario Sentinelela","email":"sentinela@exemplo.local","perfil":"ADMIN"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            put("/api/funcionario/{id}/senha", funcionario.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"senha":"Senha-Nova-456"}"""),
        ).andExpect(status().isUnauthorized)

        val persistido = funcionarioRepository.findById(funcionario.id).orElseThrow()
        assertEquals(PerfilFuncionario.TRIAGEM, persistido.perfil)
        assertEquals(senhaOriginal, persistido.senhaHash)
    }
}
