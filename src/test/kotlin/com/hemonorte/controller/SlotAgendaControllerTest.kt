package com.hemonorte.controller

import com.hemonorte.dto.SlotDisponibilidadeResponse
import com.hemonorte.service.SlotAgendaService
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("unit")
class SlotAgendaControllerTest {

    private val service = Mockito.mock(SlotAgendaService::class.java)
    private val mockMvc = MockMvcBuilders.standaloneSetup(SlotAgendaController(service)).build()

    @Test
    fun `periodo com offset vira instantes e retorna ocupacao`() {
        val inicio = Instant.parse("2035-11-09T22:00:00Z")
        val fim = Instant.parse("2035-11-10T22:00:00Z")
        val resposta = SlotDisponibilidadeResponse(UUID.randomUUID(), inicio, 5, 2, 3)
        Mockito.`when`(service.listarPorPeriodo(inicio, fim)).thenReturn(listOf(resposta))

        mockMvc.perform(
            get("/slots")
                .queryParam("inicio", "2035-11-10T00:00:00+02:00")
                .queryParam("fim", "2035-11-11T00:00:00+02:00"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].ocupacao").value(2))
            .andExpect(jsonPath("$[0].vagasDisponiveis").value(3))
    }

    @Test
    fun `periodo sem offset e rejeitado`() {
        mockMvc.perform(
            get("/slots")
                .queryParam("inicio", "2035-11-10T00:00:00")
                .queryParam("fim", "2035-11-11T00:00:00"),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `criacao exige perfil admin`() {
        val metodo = SlotAgendaController::class.java.methods
            .single { it.name == "criarSlot" }

        assertEquals("hasRole('ADMIN')", metodo.getAnnotation(PreAuthorize::class.java).value)
    }
}
