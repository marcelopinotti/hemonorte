package com.hemonorte.controller

import com.hemonorte.exception.ConflitoException
import com.hemonorte.exception.InaptidaoDoacaoException
import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
import com.hemonorte.model.ResultadoAptidao
import com.hemonorte.model.SlotAgenda
import com.hemonorte.service.AgendamentoService
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

@Tag("unit")
class AgendamentoControllerTest {

    private val service = Mockito.mock(AgendamentoService::class.java)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(AgendamentoController(service))
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    private val doador = Doador(
        nome = "Ana Souza",
        email = "ana@email.com",
        senhaHash = "hash-valido",
        tipagem = "O+",
        dataNascimento = LocalDate.now().minusYears(30),
        genero = 'F',
        cpf = "12345678901",
    )

    private val slot = SlotAgenda(Instant.now().plusSeconds(86_400), 5)

    @Test
    fun `POST cria agendamento PENDENTE com 201 - contrato da issue 9`() {
        val doadorId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        Mockito.`when`(service.realizarAgendamento(doadorId, slotId))
            .thenReturn(Agendamento(doador = doador, slot = slot))

        mockMvc.perform(
            post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"doadorId":"$doadorId","slotId":"$slotId"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDENTE"))
    }

    @Test
    fun `resposta do agendamento nao expoe hash da senha do doador`() {
        val doadorId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        Mockito.`when`(service.realizarAgendamento(doadorId, slotId))
            .thenReturn(Agendamento(doador = doador, slot = slot))

        mockMvc.perform(
            post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"doadorId":"$doadorId","slotId":"$slotId"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.doador.senhaHash").doesNotExist())
    }

    @Test
    fun `POST sem slotId devolve 400 - Bean Validation do contrato`() {
        mockMvc.perform(
            post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"doadorId":"${UUID.randomUUID()}"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `doador inapto devolve 422 com motivo e proxima data permitida`() {
        val doadorId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        Mockito.`when`(service.realizarAgendamento(doadorId, slotId))
            .thenThrow(
                InaptidaoDoacaoException(
                    ResultadoAptidao(
                        apto = false,
                        motivo = "Intervalo mínimo entre doações não cumprido",
                        proximaDataPermitida = LocalDate.of(2036, 1, 9),
                    ),
                ),
            )

        mockMvc.perform(
            post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"doadorId":"$doadorId","slotId":"$slotId"}""")
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.apto").value(false))
            .andExpect(jsonPath("$.motivo").value("Intervalo mínimo entre doações não cumprido"))
            .andExpect(jsonPath("$.proximaDataPermitida").value("2036-01-09"))
    }

    @Test
    fun `slot lotado devolve 409`() {
        val doadorId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        Mockito.`when`(service.realizarAgendamento(doadorId, slotId))
            .thenThrow(ConflitoException("Slot lotado para este horário"))

        mockMvc.perform(
            post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"doadorId":"$doadorId","slotId":"$slotId"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.erro").value("Slot lotado para este horário"))
    }

    @Test
    fun `slot inexistente devolve 404`() {
        val doadorId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        Mockito.`when`(service.realizarAgendamento(doadorId, slotId))
            .thenThrow(NoSuchElementException("Slot de agenda não encontrado"))

        mockMvc.perform(
            post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"doadorId":"$doadorId","slotId":"$slotId"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `transicao repetida devolve 400`() {
        val id = UUID.randomUUID()
        Mockito.`when`(service.registrarFaltaResponse(id))
            .thenThrow(IllegalArgumentException("Só é possível registrar falta em agendamento PENDENTE"))

        mockMvc.perform(patch("/agendamentos/{id}/falta", id))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH cancelar devolve 200 com status CANCELADO`() {
        val id = UUID.randomUUID()
        val cancelado = Agendamento(doador = doador, slot = slot).apply { cancelar() }
        Mockito.`when`(service.cancelarAgendamento(id)).thenReturn(cancelado)

        mockMvc.perform(patch("/agendamentos/{id}/cancelar", id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELADO"))
    }
}
