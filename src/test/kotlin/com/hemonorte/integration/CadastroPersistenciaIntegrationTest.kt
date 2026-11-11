package com.hemonorte.integration

import com.hemonorte.exception.ConflitoException
import com.hemonorte.TestcontainersConfiguration
import com.hemonorte.dto.DoadorResponse
import com.hemonorte.dto.FuncionarioResponse
import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.Funcionario
import com.hemonorte.model.SlotAgenda
import com.hemonorte.cpfDeTeste
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.SlotAgendaRepository
import com.hemonorte.service.AgendamentoService
import com.hemonorte.service.JwtService
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("integration")
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
class CadastroPersistenciaIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var dataSource: DataSource

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var doadorRepository: DoadorRepository

    @Autowired
    lateinit var funcionarioRepository: FuncionarioRepository

    @Autowired
    lateinit var slotAgendaRepository: SlotAgendaRepository

    @Autowired
    lateinit var agendamentoRepository: AgendamentoRepository

    @Autowired
    lateinit var agendamentoService: AgendamentoService

    @Autowired
    lateinit var jwtService: JwtService

    lateinit var adminToken: String

    @BeforeEach
    fun limparBaseDeTeste() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE historico_auditoria, exame_sorologico, bolsa_sangue, triagem,
                agendamento, slot_agenda, estoque_minimo, funcionario, doador
                RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
        val admin = funcionarioRepository.saveAndFlush(
            Funcionario(
                matricula = "ADMIN-TESTE",
                nome = "Admin Teste",
                email = "admin-teste@exemplo.local",
                senhaHash = passwordEncoder.encode("Senha-Admin-123")!!,
                perfil = PerfilFuncionario.ADMIN,
            ),
        )
        adminToken = jwtService.gerarToken(admin.id, "FUNCIONARIO", PerfilFuncionario.ADMIN)
    }

    @Test
    fun `Flyway aplica todas as migrations e o JPA valida o schema PostgreSQL`() {
        val versoes = jdbcTemplate.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
            String::class.java,
        )

        assertEquals(listOf("1", "2", "3", "4", "5"), versoes)
        assertEquals(
            true,
            constraintValidada("ck_doador_data_nascimento_nao_futura", "public.doador"),
        )
        assertEquals(
            true,
            constraintValidada("slot_agenda_capacidade_positiva", "public.slot_agenda"),
        )
    }

    @Test
    fun `migracao incremental V1 para V2 preserva doadores existentes`() {
        val schema = "qa_migracao_${UUID.randomUUID().toString().replace("-", "")}"

        try {
            val resultadoV1 = flyway(schema).target("1").load().migrate()
            assertEquals(1, resultadoV1.migrationsExecuted)

            jdbcTemplate.update(
                """
                INSERT INTO "$schema".doador
                    (id, cpf, nome, email, senha_hash, tipagem, data_nascimento, genero, criado_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent(),
                UUID.randomUUID(),
                "52998224725",
                "Doador Preservado",
                "preservado@exemplo.local",
                "hash-antigo",
                "O+",
                LocalDate.of(1990, 1, 1),
                "F",
            )

            val resultadoV2 = flyway(schema).target("2").load().migrate()
            assertEquals(1, resultadoV2.migrationsExecuted)
            assertEquals(
                1,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"$schema\".doador", Int::class.java),
            )
            assertEquals(
                true,
                constraintValidada(
                    "ck_doador_data_nascimento_nao_futura",
                    "$schema.doador",
                ),
            )
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"$schema\" CASCADE")
        }
    }

    @Test
    fun `duas reservas concorrentes na ultima vaga criam somente um agendamento`() {
        repeat(5) { tentativa ->
            val slot = slotAgendaRepository.saveAndFlush(
                SlotAgenda(Instant.now().plusSeconds(86_400 + tentativa.toLong()), 1),
            )
            val doadores = listOf(1, 2).map { numero ->
                doadorRepository.saveAndFlush(
                    Doador(
                        nome = "Doador $tentativa-$numero",
                        email = "doador-$tentativa-$numero@exemplo.local",
                        senhaHash = "hash",
                        tipagem = "O+",
                        dataNascimento = LocalDate.of(1990, 1, 1),
                        genero = 'M',
                        cpf = cpfDeTeste(10_000L + tentativa * 10 + numero),
                    ),
                )
            }
            val executor = Executors.newFixedThreadPool(2)
            val largada = CyclicBarrier(2)

            try {
                val resultados = doadores.map { doador ->
                    executor.submit<String> {
                        largada.await(10, TimeUnit.SECONDS)
                        runCatching {
                            agendamentoService.realizarAgendamento(doador.id, slot.id)
                        }.fold(
                            onSuccess = { "SUCESSO" },
                            onFailure = { "${it::class.simpleName}: ${it.message}" },
                        )
                    }
                }.map { it.get(30, TimeUnit.SECONDS) }

                assertEquals(1, resultados.count { it == "SUCESSO" }, resultados.joinToString())
                assertEquals(
                    1,
                    agendamentoRepository.countBySlotIdAndStatus(
                        slot.id,
                        com.hemonorte.model.enums.StatusAgendamento.PENDENTE,
                    ),
                )
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun `cancelamento libera vaga e banco impede reserva ativa duplicada`() {
        val slot = slotAgendaRepository.saveAndFlush(SlotAgenda(Instant.now().plusSeconds(86_400), 1))
        val (primeiro, segundo) = listOf(1, 2).map { numero ->
            doadorRepository.saveAndFlush(
                Doador(
                    nome = "Doador Vaga $numero",
                    email = "vaga-$numero@exemplo.local",
                    senhaHash = "hash",
                    tipagem = "O+",
                    dataNascimento = LocalDate.of(1990, 1, 1),
                    genero = 'M',
                    cpf = cpfDeTeste(20_000L + numero),
                ),
            )
        }

        val reserva = agendamentoService.realizarAgendamento(primeiro.id, slot.id)
        assertFailsWith<ConflitoException> { agendamentoService.realizarAgendamento(segundo.id, slot.id) }

        agendamentoService.cancelarAgendamentoDoDoador(reserva.id, primeiro.id)
        agendamentoService.realizarAgendamento(segundo.id, slot.id)

        assertFailsWith<DataIntegrityViolationException> {
            agendamentoRepository.saveAndFlush(Agendamento(segundo, slot))
        }
    }

    @Test
    fun `cadastro de doador grava BCrypt real com salts distintos e nao expoe credencial`() {
        val senha = "Senha-Segura-123"
        val primeiro = cadastrarDoadorPorApi("11144477735", "primeiro@exemplo.local", senha)
        val segundo = cadastrarDoadorPorApi("22255588846", "segundo@exemplo.local", senha)

        val hashPrimeiro = doadorRepository.findById(primeiro.id).orElseThrow().senhaHash
        val hashSegundo = doadorRepository.findById(segundo.id).orElseThrow().senhaHash

        assertNotEquals(senha, hashPrimeiro)
        assertNotEquals(senha, hashSegundo)
        assertNotEquals(hashPrimeiro, hashSegundo)
        assertTrue(hashPrimeiro.startsWith("$2"))
        assertTrue(passwordEncoder.matches(senha, hashPrimeiro))
        assertTrue(passwordEncoder.matches(senha, hashSegundo))
    }

    @Test
    fun `atualizacao de doador preserva CPF e hash existente`() {
        val senha = "Senha-Original-123"
        val doador = cadastrarDoadorPorApi("33366699957", "atualizavel@exemplo.local", senha)
        val hashOriginal = doadorRepository.findById(doador.id).orElseThrow().senhaHash

        mockMvc.perform(
            put("/api/doador/{id}", doador.id)
                .bearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Nome Atualizado","email":"atualizado@exemplo.local","tipagem":"A+",
                     "dataNascimento":"1992-05-20","genero":"M"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(doador.id.toString()))
            .andExpect(jsonPath("$.cpf").value("33366699957"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist())

        val persistido = doadorRepository.findById(doador.id).orElseThrow()
        assertEquals("33366699957", persistido.cpf)
        assertEquals(hashOriginal, persistido.senhaHash)
        assertTrue(passwordEncoder.matches(senha, persistido.senhaHash))

        mockMvc.perform(get("/api/doador/{id}", doador.id).bearer(adminToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(doador.id.toString()))
            .andExpect(jsonPath("$.nome").value("Nome Atualizado"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
    }

    @Test
    fun `alteracao de senha gera novo BCrypt e invalida a senha anterior`() {
        val senhaAntiga = "Senha-Antiga-123"
        val senhaNova = "Senha-Nova-456"
        val doador = cadastrarDoadorPorApi("44477700083", "senha@exemplo.local", senhaAntiga)

        mockMvc.perform(
            put("/api/doador/{id}/senha", doador.id)
                .bearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"senha":"$senhaNova"}"""),
        ).andExpect(status().isNoContent)

        val hashNovo = doadorRepository.findById(doador.id).orElseThrow().senhaHash
        assertNotEquals(senhaAntiga, hashNovo)
        assertFalse(passwordEncoder.matches(senhaAntiga, hashNovo))
        assertTrue(passwordEncoder.matches(senhaNova, hashNovo))
    }

    @Test
    fun `concorrencia de CPF ou e-mail duplicado devolve exatamente uma criacao e um conflito`() {
        val cenarios = listOf(
            Par("55588811194", "66699922203", "mesmo-cpf@exemplo.local", "mesmo-cpf@exemplo.local"),
            Par("77700033340", "88811144450", "mesmo-email@exemplo.local", "mesmo-email@exemplo.local"),
        )

        cenarios.forEach { cenario ->
            val respostas = executarEmConcorrencia(
                corpoDoador(cenario.cpfPrimeiro, cenario.emailPrimeiro, "Senha-Concorrencia-1"),
                corpoDoador(cenario.cpfSegundo, cenario.emailSegundo, "Senha-Concorrencia-2"),
            )

            assertEquals(listOf(201, 409), respostas.sorted())
        }

        assertEquals(2, doadorRepository.count())
    }

    @Test
    fun `duplicidade de matricula ou e-mail de funcionario devolve 409`() {
        val primeiro = corpoFuncionario("FUNC-DUP-1", "funcionario-dup@exemplo.local", "Senha-Dup-1")
        mockMvc.perform(
            post("/api/funcionario")
                .bearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(primeiro),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/funcionario")
                .bearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    corpoFuncionario(
                        "FUNC-DUP-1",
                        "outro-funcionario@exemplo.local",
                        "Senha-Dup-2",
                    ),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.erro").value("Matrícula já cadastrada"))

        mockMvc.perform(
            post("/api/funcionario")
                .bearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    corpoFuncionario(
                        "FUNC-DUP-2",
                        "funcionario-dup@exemplo.local",
                        "Senha-Dup-3",
                    ),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.erro").value("E-mail já cadastrado"))
    }

    @Test
    fun `concorrencia de matricula ou e-mail de funcionario devolve uma criacao e um conflito`() {
        val cenarios = listOf(
            ParFuncionario(
                "FUNC-RACE-1",
                "FUNC-RACE-1",
                "funcionario-race-1@exemplo.local",
                "funcionario-race-2@exemplo.local",
            ),
            ParFuncionario(
                "FUNC-RACE-3",
                "FUNC-RACE-4",
                "funcionario-race-comum@exemplo.local",
                "funcionario-race-comum@exemplo.local",
            ),
        )

        cenarios.forEach { cenario ->
            val respostas = executarFuncionarioEmConcorrencia(
                corpoFuncionario(
                    cenario.matriculaPrimeira,
                    cenario.emailPrimeiro,
                    "Senha-Race-1",
                ),
                corpoFuncionario(
                    cenario.matriculaSegunda,
                    cenario.emailSegundo,
                    "Senha-Race-2",
                ),
            )

            assertEquals(listOf(201, 409), respostas.sorted())
        }

        assertEquals(3, funcionarioRepository.count())
    }

    @Test
    fun `exclusao inativa o doador preservando historico e bloqueando acesso`() {
        val doador = doadorRepository.save(
            Doador(
                nome = "Doador com Histórico",
                email = "historico@exemplo.local",
                senhaHash = "hash-sem-historico",
                tipagem = "O+",
                dataNascimento = LocalDate.of(1990, 3, 15),
                genero = 'F',
                cpf = "99922255561",
            ),
        )
        val slot = slotAgendaRepository.save(
            SlotAgenda(inicio = Instant.now().plusSeconds(3_600), capacidade = 1),
        )
        agendamentoRepository.save(Agendamento(doador = doador, slot = slot))

        val tokenDoador = jwtService.gerarToken(doador.id, "DOADOR")

        mockMvc.perform(delete("/api/doador/{id}", doador.id).bearer(adminToken))
            .andExpect(status().isNoContent)

        assertEquals(false, doadorRepository.findById(doador.id).orElseThrow().ativo)
        assertEquals(StatusAgendamento.CANCELADO, agendamentoRepository.findAll().single().status)
        mockMvc.perform(get("/api/doador/me").bearer(tokenDoador)).andExpect(status().isUnauthorized)
    }

    @Test
    fun `funcionario e desativado sem apagar registros e sem expor hash`() {
        val matricula = "FUNC-2026"
        val email = "funcionario@exemplo.local"
        val senha = "Senha-Funcionario-123"

        val resultado = mockMvc.perform(
            post("/api/funcionario")
                .bearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"matricula":"$matricula","nome":"Funcionario QA","email":"$email",
                     "senha":"$senha","perfil":"TRIAGEM"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andReturn()

        val funcionario = objectMapper.readValue(
            resultado.response.contentAsString,
            FuncionarioResponse::class.java,
        )
        val hash = funcionarioRepository.findById(funcionario.id).orElseThrow().senhaHash
        assertNotEquals(senha, hash)
        assertTrue(passwordEncoder.matches(senha, hash))

        mockMvc.perform(
            put("/api/funcionario/{id}", funcionario.id)
                .bearer(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Funcionario Atualizado","email":"funcionario-atualizado@exemplo.local",
                     "perfil":"TRIAGEM"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nome").value("Funcionario Atualizado"))
            .andExpect(jsonPath("$.email").value("funcionario-atualizado@exemplo.local"))
            .andExpect(jsonPath("$.perfil").value("TRIAGEM"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist())

        mockMvc.perform(delete("/api/funcionario/{id}", funcionario.id).bearer(adminToken))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/funcionario/{id}", funcionario.id).bearer(adminToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ativo").value(false))
            .andExpect(jsonPath("$.senhaHash").doesNotExist())

        assertNotNull(funcionarioRepository.findById(funcionario.id).orElse(null))
    }

    @Test
    fun `nascimento futuro e rejeitado pela API e pela constraint do banco`() {
        mockMvc.perform(
            post("/api/doador")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Doador Futuro","email":"futuro@exemplo.local","cpf":"12131415168",
                     "senha":"Senha-Futura-123","tipagem":"O+",
                     "dataNascimento":"2999-01-01","genero":"F"}
                    """.trimIndent(),
                ),
        ).andExpect(status().isBadRequest)

        assertFailsWith<DataIntegrityViolationException> {
            jdbcTemplate.update(
                """
                INSERT INTO doador
                    (id, cpf, nome, email, senha_hash, tipagem, data_nascimento, genero, criado_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent(),
                UUID.randomUUID(),
                "13141516146",
                "Doador Bypass",
                "bypass@exemplo.local",
                "hash-bypass",
                "O+",
                LocalDate.of(2999, 1, 1),
                "F",
            )
        }
    }

    @Test
    fun `SENTINELA QA-045 - campo acima do limite dimensional devolve 409 hoje em vez de 400`() {
        // O catch amplo de DataIntegrityViolationException transforma "value too long" em
        // conflito de unicidade. Quando a validacao dimensional e o mapeamento preciso
        // chegarem, este teste vira prova de 400 para entrada invalida.
        val nomeLongo = "N".repeat(260)

        mockMvc.perform(
            post("/api/doador")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"$nomeLongo","email":"longo@exemplo.local","cpf":"14151617124",
                     "senha":"Senha-Longa-123","tipagem":"O+","dataNascimento":"1990-01-01",
                     "genero":"F"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.erro").value("CPF ou e-mail já cadastrado"))
    }

    @Test
    fun `recurso inexistente devolve 404 no contrato HTTP real`() {
        mockMvc.perform(get("/api/doador/{id}", UUID.randomUUID()).bearer(adminToken))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.erro").value("Doador não encontrado"))

        mockMvc.perform(get("/api/funcionario/{id}", UUID.randomUUID()).bearer(adminToken))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.erro").value("Funcionário não encontrado"))
    }

    private fun cadastrarDoadorPorApi(cpf: String, email: String, senha: String): DoadorResponse {
        val resultado = mockMvc.perform(
            post("/api/doador")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDoador(cpf, email, senha)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andReturn()

        return objectMapper.readValue(resultado.response.contentAsString, DoadorResponse::class.java)
    }

    private fun corpoDoador(cpf: String, email: String, senha: String): String =
        """
        {"nome":"Doador QA","email":"$email","cpf":"$cpf","senha":"$senha","tipagem":"O+",
         "dataNascimento":"1990-01-01","genero":"F"}
        """.trimIndent()

    private fun corpoFuncionario(
        matricula: String,
        email: String,
        senha: String,
        perfil: String = "TRIAGEM",
    ): String =
        """
        {"matricula":"$matricula","nome":"Funcionario QA","email":"$email",
         "senha":"$senha","perfil":"$perfil"}
        """.trimIndent()

    private fun executarEmConcorrencia(primeiroCorpo: String, segundoCorpo: String): List<Int> {
        val executor = Executors.newFixedThreadPool(2)
        val largada = CyclicBarrier(2)

        try {
            val futures = listOf(primeiroCorpo, segundoCorpo).map { corpo ->
                executor.submit<Int> {
                    largada.await(10, TimeUnit.SECONDS)
                    mockMvc.perform(
                        post("/api/doador")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo),
                    ).andReturn().response.status
                }
            }
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun executarFuncionarioEmConcorrencia(
        primeiroCorpo: String,
        segundoCorpo: String,
    ): List<Int> {
        val executor = Executors.newFixedThreadPool(2)
        val largada = CyclicBarrier(2)

        try {
            val futures = listOf(primeiroCorpo, segundoCorpo).map { corpo ->
                executor.submit<Int> {
                    largada.await(10, TimeUnit.SECONDS)
                    mockMvc.perform(
                        post("/api/funcionario")
                            .bearer(adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo),
                    ).andReturn().response.status
                }
            }
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun flyway(schema: String) = Flyway.configure()
        .dataSource(dataSource)
        .schemas(schema)
        .defaultSchema(schema)
        .createSchemas(true)
        .locations("classpath:db/migration")

    private fun constraintValidada(nome: String, tabela: String): Boolean =
        jdbcTemplate.queryForObject(
            """
            SELECT convalidated
            FROM pg_constraint
            WHERE conname = ? AND conrelid = ?::regclass
            """.trimIndent(),
            Boolean::class.java,
            nome,
            tabela,
        ) ?: false

    private fun org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder.bearer(token: String) =
        header("Authorization", "Bearer $token")

    private data class Par(
        val cpfPrimeiro: String,
        val cpfSegundo: String,
        val emailPrimeiro: String,
        val emailSegundo: String,
    )

    private data class ParFuncionario(
        val matriculaPrimeira: String,
        val matriculaSegunda: String,
        val emailPrimeiro: String,
        val emailSegundo: String,
    )
}
