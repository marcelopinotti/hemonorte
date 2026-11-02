package com.hemonorte.integration

import com.hemonorte.TestcontainersConfiguration
import com.hemonorte.model.Doador
import com.hemonorte.model.Funcionario
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.SlotAgendaRepository
import com.hemonorte.model.Agendamento
import com.hemonorte.model.SlotAgenda
import com.hemonorte.security.AdminInitializer
import com.hemonorte.service.JwtService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("integration")
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["jwt.expiration=60000"])
@AutoConfigureMockMvc
class SegurancaIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var doadorRepository: DoadorRepository
    @Autowired lateinit var funcionarioRepository: FuncionarioRepository
    @Autowired lateinit var agendamentoRepository: AgendamentoRepository
    @Autowired lateinit var slotAgendaRepository: SlotAgendaRepository
    @Autowired lateinit var adminInitializer: AdminInitializer
    @Autowired lateinit var jwtService: JwtService

    @BeforeEach
    fun limparBase() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE historico_auditoria, exame_sorologico, bolsa_sangue, triagem,
                agendamento, slot_agenda, estoque_minimo, funcionario, doador
                RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }

    @Test
    fun `logins validos emitem token para doador e funcionario ativo`() {
        doadorRepository.save(doador())
        funcionarioRepository.save(funcionario())

        mockMvc.perform(
            post("/auth/login/doador")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"cpf":"52998224725","senha":"Senha-123"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tipo").value("DOADOR"))
            .andExpect(jsonPath("$.token").isNotEmpty)

        mockMvc.perform(
            post("/auth/login/funcionario")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"matricula":"TRI-1","senha":"Senha-123"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tipo").value("FUNCIONARIO"))
            .andExpect(jsonPath("$.token").isNotEmpty)
    }

    @Test
    fun `credencial invalida e funcionario inativo retornam 401 generico`() {
        funcionarioRepository.save(funcionario(ativo = false))

        mockMvc.perform(
            post("/auth/login/funcionario")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"matricula":"TRI-1","senha":"Senha-123"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.erro").value("Credenciais inválidas"))

        mockMvc.perform(
            post("/auth/login/doador")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"cpf":"00000000000","senha":"Senha-123"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.erro").value("Credenciais inválidas"))
    }

    @Test
    fun `inicializador cria somente o primeiro admin`() {
        adminInitializer.initialize()
        adminInitializer.initialize()

        assertEquals(1, funcionarioRepository.countByPerfil(PerfilFuncionario.ADMIN))
        val admin = funcionarioRepository.findByMatricula("admin")!!
        assertEquals(PerfilFuncionario.ADMIN, admin.perfil)
        assertEquals(true, admin.ativo)
    }

    @Test
    fun `rotas protegidas rejeitam token ausente ou invalido e cadastro permanece publico`() {
        mockMvc.perform(get("/api/doador"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            get("/api/doador")
                .header("Authorization", "Bearer token-adulterado"),
        ).andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/api/doador")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `token de funcionario desativado depois da emissao retorna 401`() {
        val funcionario = funcionario()
        funcionarioRepository.saveAndFlush(funcionario)
        val token = jwtService.gerarToken(funcionario.id, "FUNCIONARIO", funcionario.perfil)
        funcionario.desativar()
        funcionarioRepository.saveAndFlush(funcionario)

        mockMvc.perform(
            get("/api/funcionario")
                .header("Authorization", "Bearer $token"),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `doador acessa somente os proprios dados`() {
        val titular = doadorRepository.saveAndFlush(doador())
        val outro = doadorRepository.saveAndFlush(
            Doador(
                nome = "Outro Doador",
                email = "outro@teste.local",
                cpf = "12345678901",
                senhaHash = passwordEncoder.encode("Senha-123")!!,
                tipagem = "A+",
                dataNascimento = LocalDate.of(1988, 2, 2),
                genero = 'M',
            ),
        )
        val token = jwtService.gerarToken(titular.id, "DOADOR")

        mockMvc.perform(get("/api/doador/me").bearer(token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(titular.id.toString()))

        mockMvc.perform(get("/api/doador/{id}", outro.id).bearer(token))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `perfis aplicam menor privilegio`() {
        val triagem = funcionarioRepository.saveAndFlush(funcionario())
        val tokenTriagem = jwtService.gerarToken(triagem.id, "FUNCIONARIO", triagem.perfil)

        mockMvc.perform(
            post("/slots")
                .bearer(tokenTriagem)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inicio":"${Instant.now().plusSeconds(86400)}","capacidade":4}"""),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/exames-sorologicos").bearer(tokenTriagem))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `matriz bloqueia recursos proibidos para coleta e laboratorio`() {
        val coleta = funcionarioRepository.saveAndFlush(funcionario("COL-1", PerfilFuncionario.COLETA))
        val laboratorio = funcionarioRepository.saveAndFlush(funcionario("LAB-1", PerfilFuncionario.LABORATORIO))
        val tokenColeta = jwtService.gerarToken(coleta.id, "FUNCIONARIO", coleta.perfil)
        val tokenLaboratorio = jwtService.gerarToken(laboratorio.id, "FUNCIONARIO", laboratorio.perfil)

        mockMvc.perform(get("/triagens").bearer(tokenColeta)).andExpect(status().isOk)
        mockMvc.perform(get("/exames-sorologicos").bearer(tokenColeta)).andExpect(status().isForbidden)
        mockMvc.perform(get("/exames-sorologicos").bearer(tokenLaboratorio)).andExpect(status().isOk)
        mockMvc.perform(get("/api/doador").bearer(tokenLaboratorio)).andExpect(status().isForbidden)
    }

    @Test
    fun `admin nao altera proprio perfil nem desativa a si mesmo`() {
        val admin = funcionarioRepository.saveAndFlush(funcionario("ADM-1", PerfilFuncionario.ADMIN))
        val token = jwtService.gerarToken(admin.id, "FUNCIONARIO", admin.perfil)

        mockMvc.perform(
            put("/api/funcionario/{id}", admin.id)
                .bearer(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"nome":"Admin Teste","email":"adm-1@teste.local","perfil":"TRIAGEM"}""",
                ),
        ).andExpect(status().isForbidden)

        mockMvc.perform(delete("/api/funcionario/{id}", admin.id).bearer(token))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `agendamento alheio ou inexistente nao revela existencia e lista propria serializa DTO`() {
        val titular = doadorRepository.saveAndFlush(doador())
        val outro = doadorRepository.saveAndFlush(
            Doador(
                nome = "Outro Doador",
                email = "outro-agendamento@teste.local",
                cpf = "12345678901",
                senhaHash = passwordEncoder.encode("Senha-123")!!,
                tipagem = "A+",
                dataNascimento = LocalDate.of(1988, 2, 2),
                genero = 'M',
            ),
        )
        val slot = slotAgendaRepository.saveAndFlush(SlotAgenda(Instant.now().plusSeconds(86400), 4))
        val proprio = agendamentoRepository.saveAndFlush(Agendamento(titular, slot))
        val alheio = agendamentoRepository.saveAndFlush(Agendamento(outro, slot))
        val token = jwtService.gerarToken(titular.id, "DOADOR")

        mockMvc.perform(get("/api/doador/me/agendamentos").bearer(token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(proprio.id.toString()))
            .andExpect(jsonPath("$[0].doadorId").value(titular.id.toString()))
            .andExpect(jsonPath("$[0].doador").doesNotExist())

        mockMvc.perform(get("/agendamentos/{id}", alheio.id).bearer(token))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/agendamentos/{id}", UUID.randomUUID()).bearer(token))
            .andExpect(status().isForbidden)
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/agendamentos/{id}/cancelar", alheio.id)
                .bearer(token),
        ).andExpect(status().isForbidden)
    }

    private fun org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder.bearer(token: String) =
        header("Authorization", "Bearer $token")

    private fun doador() = Doador(
        nome = "Doadora Teste",
        email = "doadora@teste.local",
        cpf = "52998224725",
        senhaHash = passwordEncoder.encode("Senha-123")!!,
        tipagem = "O+",
        dataNascimento = LocalDate.of(1990, 1, 1),
        genero = 'F',
    )

    private fun funcionario(ativo: Boolean = true): Funcionario =
        funcionario("TRI-1", PerfilFuncionario.TRIAGEM, ativo)

    private fun funcionario(
        matricula: String,
        perfil: PerfilFuncionario,
        ativo: Boolean = true,
    ): Funcionario {
        val funcionario = Funcionario(
            matricula = matricula,
            nome = "Funcionária Teste",
            email = "${matricula.lowercase()}@teste.local",
            senhaHash = passwordEncoder.encode("Senha-123")!!,
            perfil = perfil,
        )
        if (!ativo) funcionario.desativar()
        return funcionario
    }
}
