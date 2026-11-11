package com.hemonorte.integration

import com.hemonorte.TestcontainersConfiguration
import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.Funcionario
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.SlotAgendaRepository
import com.hemonorte.repository.TriagemRepository
import com.hemonorte.service.JwtService
import com.hemonorte.service.TriagemService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@Tag("integration")
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
class TriagemIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var doadorRepository: DoadorRepository
    @Autowired lateinit var funcionarioRepository: FuncionarioRepository
    @Autowired lateinit var slotAgendaRepository: SlotAgendaRepository
    @Autowired lateinit var agendamentoRepository: AgendamentoRepository
    @Autowired lateinit var triagemRepository: TriagemRepository
    @Autowired lateinit var bolsaSangueRepository: BolsaSangueRepository
    @Autowired lateinit var triagemService: TriagemService
    @Autowired lateinit var jwtService: JwtService

    lateinit var doador: Doador
    lateinit var agendamento: Agendamento

    @BeforeEach
    fun preparar() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE historico_auditoria, exame_sorologico, bolsa_sangue, triagem,
                agendamento, slot_agenda, estoque_minimo, funcionario, doador
                RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
        doador = doadorRepository.saveAndFlush(
            Doador(
                nome = "Doadora Triagem",
                email = "triagem@teste.local",
                senhaHash = "hash",
                tipagem = "O+",
                dataNascimento = LocalDate.of(1990, 1, 1),
                genero = 'F',
                cpf = "52998224725",
            ),
        )
        agendamento = novoAgendamento()
    }

    private fun novoAgendamento(): Agendamento {
        val slot = slotAgendaRepository.saveAndFlush(
            SlotAgenda(Instant.now().plusSeconds(86_400 + slotAgendaRepository.count()), 5),
        )
        return agendamentoRepository.saveAndFlush(Agendamento(doador, slot))
    }

    private fun tokenDe(perfil: PerfilFuncionario): Pair<UUID, String> {
        val funcionario = funcionarioRepository.saveAndFlush(
            Funcionario(
                matricula = "${perfil.name}-${UUID.randomUUID().toString().take(8)}",
                nome = "Funcionário ${perfil.name}",
                email = "${UUID.randomUUID()}@teste.local",
                senhaHash = "hash",
                perfil = perfil,
            ),
        )
        return funcionario.id to jwtService.gerarToken(funcionario.id, "FUNCIONARIO", perfil)
    }

    private fun MockHttpServletRequestBuilder.bearer(token: String) = header("Authorization", "Bearer $token")

    private fun triagem(token: String, peso: String = "70.00", agendamentoId: UUID = agendamento.id, extra: String = "") =
        mockMvc.perform(
            post("/triagens").bearer(token).contentType(MediaType.APPLICATION_JSON).content(
                """{"agendamentoId":"$agendamentoId","peso":$peso,"tatuagemRecente":false,"cirurgiaRecente":false,"teveFebre":false$extra}""",
            ),
        )

    @Test
    fun `triagem inapta registrada pelo funcionario autenticado bloqueia coleta`() {
        val (triadorId, triador) = tokenDe(PerfilFuncionario.TRIAGEM)
        val (_, coleta) = tokenDe(PerfilFuncionario.COLETA)
        val outroFuncionario = UUID.randomUUID()

        triagem(triador, peso = "45.00", extra = ""","apto":true,"motivoInaptidao":null,"funcionarioId":"$outroFuncionario"""")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.apto").value(false))
            .andExpect(jsonPath("$.motivoInaptidao").value("peso abaixo do mínimo (50.0kg)"))
            .andExpect(jsonPath("$.funcionarioId").value(triadorId.toString()))

        triagem(triador).andExpect(status().isConflict)

        mockMvc.perform(get("/triagens/agendamento/{id}", agendamento.id).bearer(coleta))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.apto").value(false))

        mockMvc.perform(
            post("/coletas").bearer(coleta).contentType(MediaType.APPLICATION_JSON).content(
                """{"agendamentoId":"${agendamento.id}","tipagem":"O+","dataColeta":"${LocalDate.now()}","dataValidade":"${LocalDate.now().plusDays(35)}"}""",
            ),
        ).andExpect(status().isConflict)

        assertEquals(0, bolsaSangueRepository.count())
        assertNull(agendamentoRepository.findById(agendamento.id).orElseThrow().codigoComprovante)
    }

    @Test
    fun `jornada apta gera comprovante libera bolsa no estoque e bloqueia nova reserva pelo intervalo`() {
        val (_, triador) = tokenDe(PerfilFuncionario.TRIAGEM)
        val (_, coleta) = tokenDe(PerfilFuncionario.COLETA)
        val (_, laboratorio) = tokenDe(PerfilFuncionario.LABORATORIO)
        val tokenDoador = jwtService.gerarToken(doador.id, "DOADOR")
        val agora = Instant.now()
        val slotDeHoje = slotAgendaRepository.saveAndFlush(
            SlotAgenda(agora.minusSeconds(60), 5, referencia = agora.minusSeconds(120)),
        )
        agendamento = agendamentoRepository.saveAndFlush(Agendamento(doador, slotDeHoje))
        val corpoColeta =
            """{"agendamentoId":"${agendamento.id}","tipagem":"O+","dataColeta":"${LocalDate.now()}","dataValidade":"${LocalDate.now().plusDays(35)}"}"""

        triagem(triador).andExpect(status().isCreated)
        val bolsaId = mockMvc.perform(
            post("/coletas").bearer(coleta).contentType(MediaType.APPLICATION_JSON).content(corpoColeta),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("EM_ANALISE"))
            .andReturn().response.contentAsString.let { Regex("\"id\":\"([^\"]+)\"").find(it)!!.groupValues[1] }

        mockMvc.perform(post("/coletas").bearer(coleta).contentType(MediaType.APPLICATION_JSON).content(corpoColeta))
            .andExpect(status().isConflict)
        assertEquals(1, bolsaSangueRepository.count())

        mockMvc.perform(get("/agendamentos/{id}", agendamento.id).bearer(tokenDoador))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONCLUIDO"))
            .andExpect(jsonPath("$.codigoComprovante").isNotEmpty)

        val codigo = agendamentoRepository.findById(agendamento.id).orElseThrow().codigoComprovante
        mockMvc.perform(get("/comprovantes/{codigo}", codigo))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valido").value(true))
            .andExpect(jsonPath("$.doador").value("Doadora Triagem"))
            .andExpect(jsonPath("$.cpf").doesNotExist())
        mockMvc.perform(get("/comprovantes/{codigo}", UUID.randomUUID())).andExpect(status().isNotFound)

        mockMvc.perform(
            post("/exames-sorologicos").bearer(laboratorio).contentType(MediaType.APPLICATION_JSON)
                .content("""{"bolsaId":"$bolsaId","hiv":"NEGATIVO","hepatite":"NEGATIVO","chagas":"NEGATIVO"}"""),
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/estoque/{id}/historico", bolsaId).bearer(laboratorio))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].statusAnterior").value("EM_ANALISE"))
            .andExpect(jsonPath("$[0].statusNovo").value("DISPONIVEL"))
            .andExpect(jsonPath("$[0].motivo").value("Exame sorológico aprovado"))
            .andExpect(jsonPath("$[0].funcionarioId").isNotEmpty)
        assertFailsWith<Exception> {
            jdbcTemplate.update("UPDATE historico_auditoria SET motivo = 'adulterado'")
        }
        assertFailsWith<Exception> { jdbcTemplate.update("DELETE FROM historico_auditoria") }

        mockMvc.perform(get("/estoque/disponiveis").bearer(laboratorio))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$['O+']").value(1))
            .andExpect(jsonPath("$['A-']").value(0))

        val proximoSlot = slotAgendaRepository.saveAndFlush(SlotAgenda(Instant.now().plusSeconds(86_400 * 3), 5))
        mockMvc.perform(
            post("/agendamentos").bearer(tokenDoador).contentType(MediaType.APPLICATION_JSON)
                .content("""{"slotId":"${proximoSlot.id}"}"""),
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.motivo").value("Intervalo mínimo entre doações não cumprido"))
    }

    @Test
    fun `reserva exige pre-triagem apta respondida pelo proprio doador`() {
        val tokenDoador = jwtService.gerarToken(doador.id, "DOADOR")
        val slot = slotAgendaRepository.saveAndFlush(SlotAgenda(Instant.now().plusSeconds(86_400 * 5), 5))
        fun reservar() = mockMvc.perform(
            post("/agendamentos").bearer(tokenDoador).contentType(MediaType.APPLICATION_JSON)
                .content("""{"slotId":"${slot.id}"}"""),
        )
        fun responder(febre: Boolean) = mockMvc.perform(
            post("/api/doador/me/pre-triagem").bearer(tokenDoador).contentType(MediaType.APPLICATION_JSON)
                .content("""{"peso":70.00,"tatuagemRecente":false,"cirurgiaRecente":false,"teveFebre":$febre}"""),
        )

        mockMvc.perform(get("/api/doador/me/pre-triagem/perguntas").bearer(tokenDoador))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(4))
        mockMvc.perform(get("/api/doador/me/pre-triagem").bearer(tokenDoador)).andExpect(status().isNotFound)
        reservar()
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.motivo").value("Responda a pré-triagem antes de agendar"))

        responder(febre = true)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.apto").value(false))
            .andExpect(jsonPath("$.motivoInaptidao").value("febre recente"))
            .andExpect(jsonPath("$.respostas[3].codigo").value("TEVE_FEBRE"))
            .andExpect(jsonPath("$.respostas[3].resposta").value(true))
        reservar()
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.motivo").value("Pré-triagem inapta: febre recente"))

        responder(febre = false).andExpect(status().isCreated).andExpect(jsonPath("$.apto").value(true))
        mockMvc.perform(get("/api/doador/me/pre-triagem").bearer(tokenDoador))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.apto").value(true))
        reservar().andExpect(status().isCreated)

        val (_, triador) = tokenDe(PerfilFuncionario.TRIAGEM)
        mockMvc.perform(get("/api/doador/me/pre-triagem/perguntas").bearer(triador)).andExpect(status().isForbidden)
    }

    @Test
    fun `entradas invalidas e vinculos inexistentes sao rejeitados`() {
        val (_, triador) = tokenDe(PerfilFuncionario.TRIAGEM)

        triagem(triador, peso = "0").andExpect(status().isBadRequest)
        triagem(triador, peso = "1000").andExpect(status().isBadRequest)
        triagem(triador, agendamentoId = UUID.randomUUID()).andExpect(status().isNotFound)
        mockMvc.perform(get("/triagens/agendamento/{id}", agendamento.id).bearer(triador))
            .andExpect(status().isNotFound)

        val cancelado = novoAgendamento().also { it.cancelar() }.let { agendamentoRepository.saveAndFlush(it) }
        triagem(triador, agendamentoId = cancelado.id).andExpect(status().isConflict)
    }

    @Test
    fun `perfis sem permissao nao registram nem consultam triagem`() {
        val (_, coleta) = tokenDe(PerfilFuncionario.COLETA)
        val (_, laboratorio) = tokenDe(PerfilFuncionario.LABORATORIO)
        val tokenDoador = jwtService.gerarToken(doador.id, "DOADOR")

        listOf(coleta, laboratorio, tokenDoador).forEach { triagem(it).andExpect(status().isForbidden) }
        listOf(laboratorio, tokenDoador).forEach {
            mockMvc.perform(get("/triagens/agendamento/{id}", agendamento.id).bearer(it))
                .andExpect(status().isForbidden)
            mockMvc.perform(get("/triagens").bearer(it)).andExpect(status().isForbidden)
        }
        mockMvc.perform(post("/triagens").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized)
        assertEquals(0, triagemRepository.count())
    }

    @Test
    fun `triagens concorrentes no mesmo agendamento criam somente uma`() {
        val (triadorId, _) = tokenDe(PerfilFuncionario.TRIAGEM)
        repeat(5) {
            val alvo = novoAgendamento()
            val executor = Executors.newFixedThreadPool(2)
            val largada = CyclicBarrier(2)
            try {
                val resultados = (1..2).map {
                    executor.submit<Boolean> {
                        largada.await(10, TimeUnit.SECONDS)
                        runCatching {
                            triagemService.realizarTriagem(alvo.id, BigDecimal("70"), false, false, false, triadorId)
                        }.isSuccess
                    }
                }.map { it.get(30, TimeUnit.SECONDS) }

                assertEquals(1, resultados.count { it })
                assertEquals(1, triagemRepository.findAll().count { it.agendamento.id == alvo.id })
            } finally {
                executor.shutdownNow()
            }
        }
    }
}
