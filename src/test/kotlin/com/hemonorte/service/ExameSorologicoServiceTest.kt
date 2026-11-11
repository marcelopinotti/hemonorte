package com.hemonorte.service

import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.Agendamento
import com.hemonorte.model.BolsaSangue
import com.hemonorte.model.Doador
import com.hemonorte.model.ExameSorologico
import com.hemonorte.model.Funcionario
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.model.enums.ResultadoSorologico
import com.hemonorte.model.enums.ResultadoSorologico.NEGATIVO
import com.hemonorte.model.enums.ResultadoSorologico.POSITIVO
import com.hemonorte.model.enums.StatusBolsa
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.ExameSorologicoRepository
import com.hemonorte.repository.FuncionarioRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.security.access.AccessDeniedException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Tag("unit")
class ExameSorologicoServiceTest {

    private val exameRepository = Mockito.mock(ExameSorologicoRepository::class.java)
    private val bolsaRepository = Mockito.mock(BolsaSangueRepository::class.java)
    private val funcionarioRepository = Mockito.mock(FuncionarioRepository::class.java)
    private val historicoRepository = Mockito.mock(com.hemonorte.repository.HistoricoAuditoriaRepository::class.java)
    private val hoje = LocalDate.of(2035, 11, 10)
    private val service = ExameSorologicoService(
        exameRepository,
        bolsaRepository,
        funcionarioRepository,
        historicoRepository,
        Clock.fixed(Instant.parse("2035-11-10T10:00:00Z"), ZoneOffset.UTC),
    )

    private val laboratorio = Funcionario(
        matricula = "LAB-1",
        nome = "Lia Rocha",
        email = "lia@hemonorte.br",
        senhaHash = "hash",
        perfil = PerfilFuncionario.LABORATORIO,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> qualquer(): T = Mockito.any() as T

    init {
        Mockito.`when`(funcionarioRepository.findById(laboratorio.id)).thenReturn(Optional.of(laboratorio))
        Mockito.`when`(exameRepository.saveAndFlush(qualquer<ExameSorologico>()))
            .thenAnswer { it.arguments[0] as ExameSorologico }
    }

    private fun bolsa(coleta: LocalDate = hoje.minusDays(1), validade: LocalDate = hoje.plusDays(30)): BolsaSangue {
        val agendamento = Agendamento(
            Doador(
                nome = "Ana Souza",
                email = "ana@email.com",
                senhaHash = "hash",
                tipagem = "O+",
                dataNascimento = LocalDate.of(1990, 1, 1),
                genero = 'F',
                cpf = "12345678909",
            ),
            SlotAgenda(Instant.now().plusSeconds(86_400), 5),
        )
        val bolsa = BolsaSangue(agendamento, "O+", coleta, validade)
        Mockito.`when`(bolsaRepository.findById(bolsa.id)).thenReturn(Optional.of(bolsa))
        return bolsa
    }

    private fun examinar(
        bolsa: BolsaSangue,
        hiv: ResultadoSorologico = NEGATIVO,
        hepatite: ResultadoSorologico = NEGATIVO,
        chagas: ResultadoSorologico = NEGATIVO,
    ) = service.registrarExame(bolsa.id, laboratorio.id, hiv, hepatite, chagas)

    @Test
    fun `todos negativos liberam a bolsa`() {
        val bolsa = bolsa()

        examinar(bolsa)

        assertEquals(StatusBolsa.DISPONIVEL, bolsa.status)
    }

    @Test
    fun `cada positivo isolado ou combinado descarta a bolsa`() {
        listOf(
            Triple(POSITIVO, NEGATIVO, NEGATIVO),
            Triple(NEGATIVO, POSITIVO, NEGATIVO),
            Triple(NEGATIVO, NEGATIVO, POSITIVO),
            Triple(POSITIVO, POSITIVO, POSITIVO),
        ).forEach { (hiv, hepatite, chagas) ->
            val bolsa = bolsa()

            examinar(bolsa, hiv, hepatite, chagas)

            assertEquals(StatusBolsa.DESCARTADA, bolsa.status, "$hiv/$hepatite/$chagas")
        }
    }

    @Test
    fun `bolsa vencida aprovada nao e liberada`() {
        val vencida = bolsa(coleta = hoje.minusDays(40), validade = hoje.minusDays(1))

        examinar(vencida)

        assertEquals(StatusBolsa.DESCARTADA, vencida.status)
        assertEquals("Bolsa vencida antes da liberação", motivoAuditado())
    }

    @Test
    fun `resultado do exame grava auditoria com o laboratorio responsavel`() {
        examinar(bolsa())
        assertEquals("Exame sorológico aprovado", motivoAuditado())

        Mockito.clearInvocations(historicoRepository)
        examinar(bolsa(), hiv = POSITIVO)
        assertEquals("Exame sorológico reprovado", motivoAuditado())
    }

    private fun motivoAuditado(): String? {
        val captor = org.mockito.ArgumentCaptor.forClass(com.hemonorte.model.HistoricoAuditoria::class.java)
        Mockito.verify(historicoRepository).save(captor.capture())
        assertEquals(laboratorio, captor.value.funcionario)
        return captor.value.motivo
    }

    @Test
    fun `bolsa no ultimo dia de validade aprovada e liberada`() {
        val noLimite = bolsa(coleta = hoje.minusDays(40), validade = hoje)

        examinar(noLimite)

        assertEquals(StatusBolsa.DISPONIVEL, noLimite.status)
    }

    @Test
    fun `exame duplicado ou bolsa fora de analise usam conflito`() {
        val duplicada = bolsa()
        Mockito.`when`(exameRepository.existsByBolsaId(duplicada.id)).thenReturn(true)
        assertFailsWith<ConflitoException> { examinar(duplicada) }

        val liberada = bolsa().also { it.transicionarPara(StatusBolsa.DISPONIVEL, null, null) }
        assertFailsWith<ConflitoException> { examinar(liberada) }

        Mockito.verify(exameRepository, Mockito.never()).saveAndFlush(qualquer<ExameSorologico>())
    }

    @Test
    fun `bolsa inexistente devolve nao encontrado`() {
        bolsa()
        val inexistente = UUID.randomUUID()
        Mockito.`when`(bolsaRepository.findById(inexistente)).thenReturn(Optional.empty())

        assertFailsWith<NoSuchElementException> {
            service.registrarExame(inexistente, laboratorio.id, NEGATIVO, NEGATIVO, NEGATIVO)
        }
    }

    @Test
    fun `funcionario inativo nao registra exame`() {
        val bolsa = bolsa()
        laboratorio.desativar()

        assertFailsWith<AccessDeniedException> { examinar(bolsa) }
        assertEquals(StatusBolsa.EM_ANALISE, bolsa.status)
    }
}
