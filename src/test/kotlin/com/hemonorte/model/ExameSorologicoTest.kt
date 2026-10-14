package com.hemonorte.model

import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.model.enums.ResultadoFinal
import com.hemonorte.model.enums.ResultadoSorologico
import com.hemonorte.model.enums.ResultadoSorologico.NEGATIVO
import com.hemonorte.model.enums.ResultadoSorologico.POSITIVO
import org.junit.jupiter.api.Tag
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("unit")
class ExameSorologicoTest {

    private fun doador() = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.now().minusYears(30),
        genero = 'F',
        cpf = "12345678901",
    )

    private fun slot() = SlotAgenda(
        inicio = Instant.now().plus(2, ChronoUnit.HOURS),
        capacidade = 5,
    )

    private fun agendamento() = Agendamento(doador = doador(), slot = slot())

    private fun bolsa() = BolsaSangue(
        agendamento = agendamento(),
        tipagem = "O+",
        dataColeta = LocalDate.now().minusDays(1),
        dataValidade = LocalDate.now().plusDays(41),
    )

    private fun funcionario() = Funcionario(
        matricula = "M456",
        nome = "Bia Lab",
        email = "bia@hemonorte.br",
        senhaHash = "hash-valido",
        perfil = PerfilFuncionario.LABORATORIO,
    )

    private fun exame(
        hiv: ResultadoSorologico = NEGATIVO,
        hepatite: ResultadoSorologico = NEGATIVO,
        chagas: ResultadoSorologico = NEGATIVO,
    ) = ExameSorologico(
        bolsa = bolsa(),
        funcionario = funcionario(),
        hiv = hiv,
        hepatite = hepatite,
        chagas = chagas,
    )

    @Test
    fun `tres resultados negativos aprovam a bolsa`() {
        assertEquals(ResultadoFinal.APROVADO, exame().resultadoFinal)
    }

    @Test
    fun `hiv positivo reprova mesmo com os demais negativos`() {
        assertEquals(ResultadoFinal.REPROVADO, exame(hiv = POSITIVO).resultadoFinal)
    }

    @Test
    fun `hepatite positiva reprova mesmo com os demais negativos`() {
        assertEquals(ResultadoFinal.REPROVADO, exame(hepatite = POSITIVO).resultadoFinal)
    }

    @Test
    fun `chagas positivo reprova mesmo com os demais negativos`() {
        assertEquals(ResultadoFinal.REPROVADO, exame(chagas = POSITIVO).resultadoFinal)
    }

    @Test
    fun `dois resultados positivos reprovam`() {
        assertEquals(
            ResultadoFinal.REPROVADO,
            exame(hiv = POSITIVO, hepatite = POSITIVO).resultadoFinal,
        )
    }

    @Test
    fun `tres resultados positivos reprovam`() {
        assertEquals(
            ResultadoFinal.REPROVADO,
            exame(hiv = POSITIVO, hepatite = POSITIVO, chagas = POSITIVO).resultadoFinal,
        )
    }
}
