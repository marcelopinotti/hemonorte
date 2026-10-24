package com.hemonorte.controller

import com.hemonorte.model.Agendamento
import com.hemonorte.model.Doador
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
    fun `SENTINELA QA-043 - resposta do agendamento expoe o hash da senha do doador`() {
        // POST /agendamentos devolve a entidade crua: o DTO de doador protege o hash, mas
        // esta resposta serializa a entidade inteira (senhaHash sem @JsonIgnore). Contrato
        // da #16: ausencia de senha/hash nas respostas. Quando corrigirem (DTO de resposta
        // ou @JsonIgnore), este teste inverte para doesNotExist.
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
            .andExpect(jsonPath("$.doador.senhaHash").value("hash-valido"))
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
    fun `SENTINELA - doador inapto devolve 400 hoje - contrato da issue 9 prevê 422 com motivo`() {
        // O handler mapeia IllegalArgumentException para 400; o contrato da #9/#17 pede 422
        // com o motivo da inaptidao. Quando o mapeamento existir, trocar a assercao.
        val doadorId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        Mockito.`when`(service.realizarAgendamento(doadorId, slotId))
            .thenThrow(IllegalArgumentException("Doador inaptado: Intervalo mínimo entre doações não respeitado."))

        mockMvc.perform(
            post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"doadorId":"$doadorId","slotId":"$slotId"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.erro").value("Doador inaptado: Intervalo mínimo entre doações não respeitado."))
    }

    @Test
    fun `SENTINELA - slot lotado devolve 400 hoje - contrato da issue 9 prevê 409`() {
        // Mesma familia da sentinela anterior: o contrato da #9 pede 409 para lotacao;
        // hoje cai no IllegalArgumentException -> 400 do handler.
        val doadorId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        Mockito.`when`(service.realizarAgendamento(doadorId, slotId))
            .thenThrow(IllegalArgumentException("Slot lotado para este horário"))

        mockMvc.perform(
            post("/agendamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"doadorId":"$doadorId","slotId":"$slotId"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.erro").value("Slot lotado para este horário"))
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
