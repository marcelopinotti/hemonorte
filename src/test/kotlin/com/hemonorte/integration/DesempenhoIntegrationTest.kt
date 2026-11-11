package com.hemonorte.integration

import com.hemonorte.TestcontainersConfiguration
import com.hemonorte.cpfDeTeste
import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.PreTriagem
import com.hemonorte.model.SlotAgenda
import com.hemonorte.repository.AgendamentoRepository
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.PreTriagemRepository
import com.hemonorte.repository.SlotAgendaRepository
import com.hemonorte.service.JwtService
import org.junit.jupiter.api.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertTrue

@Tag("integration")
@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["spring.jpa.properties.hibernate.show_sql=false"])
@AutoConfigureMockMvc
class DesempenhoIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var doadorRepository: DoadorRepository
    @Autowired lateinit var preTriagemRepository: PreTriagemRepository
    @Autowired lateinit var slotAgendaRepository: SlotAgendaRepository
    @Autowired lateinit var agendamentoRepository: AgendamentoRepository
    @Autowired lateinit var jwtService: JwtService

    @Test
    fun `consulta de vagas e reserva respondem com p95 abaixo de 500 ms`() {
        jdbcTemplate.execute(
            """
            TRUNCATE TABLE historico_auditoria, exame_sorologico, bolsa_sangue, triagem, pre_triagem,
                agendamento, slot_agenda, estoque_minimo, funcionario, doador
                RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
        val primeiroDia = Instant.now().truncatedTo(ChronoUnit.DAYS).plus(Duration.ofDays(1))
        val slots = (0 until DIAS * SLOTS_POR_DIA).map { i ->
            SlotAgenda(primeiroDia.plus(Duration.ofDays((i / SLOTS_POR_DIA).toLong())).plus(Duration.ofHours(8L + i % SLOTS_POR_DIA)), CAPACIDADE)
        }.let(slotAgendaRepository::saveAll)
        val doadores = (1..RESERVAS + OCUPACAO_PREVIA).map {
            Doador("Doador $it", "perf$it@exemplo.local", "hash", "O+", LocalDate.of(1990, 1, 1), 'M', cpfDeTeste(50_000L + it))
        }.let(doadorRepository::saveAll)
        preTriagemRepository.saveAll(doadores.map { PreTriagem(it, BigDecimal("70.00"), false, false, false) })
        agendamentoRepository.saveAll(doadores.takeLast(OCUPACAO_PREVIA).mapIndexed { i, d -> Agendamento(d, slots[i % slots.size]) })

        val inicio = primeiroDia.atOffset(ZoneOffset.UTC)
        val fim = inicio.plusDays(DIAS.toLong())
        fun consultar() = mockMvc.perform(get("/slots").param("inicio", "$inicio").param("fim", "$fim"))
            .andExpect(status().isOk)

        repeat(AQUECIMENTO) { consultar() }
        val consultas = (1..AMOSTRAS).map { medir { consultar() } }
        val reservas = doadores.take(RESERVAS).mapIndexed { i, doador ->
            val token = jwtService.gerarToken(doador.id, "DOADOR")
            medir {
                mockMvc.perform(
                    post("/agendamentos").header("Authorization", "Bearer $token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"slotId":"${slots[(i * 7) % slots.size].id}"}"""),
                ).andExpect(status().isCreated)
            }
        }

        val p95Consulta = p95(consultas)
        val p95Reserva = p95(reservas)
        logger.info(
            "RNF03 desempenho: {} slots, {} reservas prévias | GET /slots ({} amostras) p50={} ms p95={} ms max={} ms | " +
                "POST /agendamentos ({} amostras) p50={} ms p95={} ms max={} ms",
            slots.size, OCUPACAO_PREVIA,
            consultas.size, p50(consultas), p95Consulta, consultas.max(),
            reservas.size, p50(reservas), p95Reserva, reservas.max(),
        )
        assertTrue(p95Consulta < LIMITE_MS, "p95 de GET /slots = $p95Consulta ms")
        assertTrue(p95Reserva < LIMITE_MS, "p95 de POST /agendamentos = $p95Reserva ms")
    }

    private inline fun medir(bloco: () -> Unit): Long {
        val inicio = System.nanoTime()
        bloco()
        return (System.nanoTime() - inicio) / 1_000_000
    }

    private fun p50(amostras: List<Long>) = amostras.sorted()[amostras.size / 2]

    private fun p95(amostras: List<Long>) = amostras.sorted()[(amostras.size * 95 + 99) / 100 - 1]

    private companion object {
        val logger = LoggerFactory.getLogger(DesempenhoIntegrationTest::class.java)
        const val DIAS = 7
        const val SLOTS_POR_DIA = 10
        const val CAPACIDADE = 20
        const val OCUPACAO_PREVIA = 300
        const val RESERVAS = 100
        const val AQUECIMENTO = 20
        const val AMOSTRAS = 200
        const val LIMITE_MS = 500
    }
}
