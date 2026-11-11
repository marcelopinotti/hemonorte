package com.hemonorte.integration

import com.hemonorte.TestcontainersConfiguration
import com.hemonorte.cpfDeTeste
import com.hemonorte.model.Agendamento
import com.hemonorte.model.BolsaSangue
import com.hemonorte.model.Doador
import com.hemonorte.model.Funcionario
import com.hemonorte.model.PreTriagem
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.Triagem
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.model.enums.ResultadoSorologico.NEGATIVO
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.model.enums.StatusBolsa
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.ExameSorologicoRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.HistoricoAuditoriaRepository
import com.hemonorte.repository.PreTriagemRepository
import com.hemonorte.repository.SlotAgendaRepository
import com.hemonorte.repository.TriagemRepository
import com.hemonorte.service.ExameSorologicoService
import com.hemonorte.service.JwtService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Tag("integration")
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
class FluxoDoadorIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var doadorRepository: DoadorRepository
    @Autowired lateinit var funcionarioRepository: FuncionarioRepository
    @Autowired lateinit var slotAgendaRepository: SlotAgendaRepository
    @Autowired lateinit var agendamentoRepository: AgendamentoRepository
    @Autowired lateinit var preTriagemRepository: PreTriagemRepository
    @Autowired lateinit var triagemRepository: TriagemRepository
    @Autowired lateinit var bolsaSangueRepository: BolsaSangueRepository
    @Autowired lateinit var exameSorologicoRepository: ExameSorologicoRepository
    @Autowired lateinit var historicoAuditoriaRepository: HistoricoAuditoriaRepository
    @Autowired lateinit var exameSorologicoService: ExameSorologicoService
    @Autowired lateinit var jwtService: JwtService

    @BeforeEach
    fun limparBase() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE historico_auditoria, exame_sorologico, bolsa_sangue, triagem, pre_triagem,
                agendamento, slot_agenda, estoque_minimo, funcionario, doador
                RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }

    private var sequencia = 0L

    private fun doadorApto(): Doador {
        sequencia++
        val doador = doadorRepository.saveAndFlush(
            Doador("Doador $sequencia", "d$sequencia@exemplo.local", "hash", "O+", LocalDate.of(1990, 1, 1), 'M', cpfDeTeste(30_000L + sequencia)),
        )
        preTriagemRepository.saveAndFlush(PreTriagem(doador, BigDecimal("70.00"), false, false, false))
        return doador
    }

    private fun slot(horasAFrente: Long, capacidade: Int = 1) =
        slotAgendaRepository.saveAndFlush(SlotAgenda(Instant.now().plusSeconds(horasAFrente * 3_600), capacidade))

    private fun funcionario(perfil: PerfilFuncionario) = funcionarioRepository.saveAndFlush(
        Funcionario("${perfil.name}-$sequencia", perfil.name, "${perfil.name}-$sequencia@exemplo.local", "hash", perfil),
    )

    private fun token(doador: Doador) = jwtService.gerarToken(doador.id, "DOADOR")

    private fun MockHttpServletRequestBuilder.bearer(token: String) = header("Authorization", "Bearer $token")

    private fun remarcar(token: String, agendamento: Agendamento, novoSlot: SlotAgenda) = mockMvc.perform(
        patch("/agendamentos/{id}/remarcar", agendamento.id).bearer(token).contentType(MediaType.APPLICATION_JSON)
            .content("""{"slotId":"${novoSlot.id}"}"""),
    )

    @Test
    fun `remarcar move a reserva e libera a vaga antiga`() {
        val doador = doadorApto()
        val origem = slot(24)
        val destino = slot(48)
        val atual = agendamentoRepository.saveAndFlush(Agendamento(doador, origem))

        remarcar(token(doador), atual, destino)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDENTE"))
            .andExpect(jsonPath("$.slotId").value(destino.id.toString()))

        assertEquals(StatusAgendamento.CANCELADO, agendamentoRepository.findById(atual.id).orElseThrow().status)
        assertEquals(0, agendamentoRepository.countBySlotIdAndStatus(origem.id, StatusAgendamento.PENDENTE))
        assertEquals(1, agendamentoRepository.countBySlotIdAndStatus(destino.id, StatusAgendamento.PENDENTE))
    }

    @Test
    fun `remarcar para horario lotado desfaz o cancelamento e mantem a reserva original`() {
        val doador = doadorApto()
        val origem = slot(24)
        val lotado = slot(48)
        val atual = agendamentoRepository.saveAndFlush(Agendamento(doador, origem))
        agendamentoRepository.saveAndFlush(Agendamento(doadorApto(), lotado))

        remarcar(token(doador), atual, lotado).andExpect(status().isConflict)

        assertEquals(StatusAgendamento.PENDENTE, agendamentoRepository.findById(atual.id).orElseThrow().status)
        assertEquals(1, agendamentoRepository.countBySlotIdAndStatus(origem.id, StatusAgendamento.PENDENTE))
    }

    @Test
    fun `remarcar agendamento de outro doador e negado`() {
        val dono = doadorApto()
        val atual = agendamentoRepository.saveAndFlush(Agendamento(dono, slot(24)))

        remarcar(token(doadorApto()), atual, slot(48)).andExpect(status().isForbidden)
        assertEquals(StatusAgendamento.PENDENTE, agendamentoRepository.findById(atual.id).orElseThrow().status)
    }

    @Test
    fun `historico de doacoes mostra somente as concluidas com comprovante`() {
        val doador = doadorApto()
        val concluido = agendamentoRepository.saveAndFlush(Agendamento(doador, slot(24)).also { it.concluir() })
        agendamentoRepository.saveAndFlush(Agendamento(doador, slot(48)).also { it.cancelar() })
        agendamentoRepository.saveAndFlush(Agendamento(doador, slot(72)))

        mockMvc.perform(get("/api/doador/me/doacoes").bearer(token(doador)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].agendamentoId").value(concluido.id.toString()))
            .andExpect(jsonPath("$[0].codigoComprovante").value(concluido.codigoComprovante.toString()))
    }

    @Test
    fun `coleta que falha desfaz conclusao do agendamento e comprovante`() {
        val agendamento = agendamentoRepository.saveAndFlush(Agendamento(doadorApto(), slot(24)))
        triagemRepository.saveAndFlush(
            Triagem(agendamento, funcionario(PerfilFuncionario.TRIAGEM), BigDecimal("70.00"), false, false, false),
        )
        val coleta = jwtService.gerarToken(funcionario(PerfilFuncionario.COLETA).id, "FUNCIONARIO", PerfilFuncionario.COLETA)
        val hoje = LocalDate.now()

        mockMvc.perform(
            post("/coletas").bearer(coleta).contentType(MediaType.APPLICATION_JSON).content(
                """{"agendamentoId":"${agendamento.id}","tipagem":"O+","dataColeta":"$hoje","dataValidade":"$hoje"}""",
            ),
        ).andExpect(status().isBadRequest)

        val persistido = agendamentoRepository.findById(agendamento.id).orElseThrow()
        assertEquals(StatusAgendamento.PENDENTE, persistido.status)
        assertNull(persistido.codigoComprovante)
        assertEquals(0, bolsaSangueRepository.count())
    }

    @Test
    fun `exames concorrentes na mesma bolsa gravam um unico resultado e uma unica auditoria`() {
        val agendamento = agendamentoRepository.saveAndFlush(Agendamento(doadorApto(), slot(24)).also { it.concluir() })
        val bolsa = bolsaSangueRepository.saveAndFlush(
            BolsaSangue(agendamento, "O+", LocalDate.now(), LocalDate.now().plusDays(35)),
        )
        val laboratorio = funcionario(PerfilFuncionario.LABORATORIO)
        val executor = Executors.newFixedThreadPool(2)
        val largada = CyclicBarrier(2)

        try {
            val resultados = (1..2).map {
                executor.submit<Boolean> {
                    largada.await(10, TimeUnit.SECONDS)
                    runCatching { exameSorologicoService.registrarExame(bolsa.id, laboratorio.id, NEGATIVO, NEGATIVO, NEGATIVO) }
                        .isSuccess
                }
            }.map { it.get(30, TimeUnit.SECONDS) }

            assertEquals(1, resultados.count { it })
        } finally {
            executor.shutdownNow()
        }
        assertEquals(1, exameSorologicoRepository.count())
        assertEquals(1, historicoAuditoriaRepository.count())
        assertEquals(StatusBolsa.DISPONIVEL, bolsaSangueRepository.findById(bolsa.id).orElseThrow().status)
    }
}
