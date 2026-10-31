package com.hemonorte.controller

import com.hemonorte.exception.RegraNegocioException
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test

@Tag("unit")
class ApiExceptionHandlerTest {

    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(RegraNegocioController())
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun `SENTINELA QA-044 - RegraNegocioException devolve 200 hoje em vez de status de erro`() {
        // O handler nao declara @ResponseStatus, portanto uma regra de negocio serializada
        // como erro ainda responde HTTP 200. Quando o mapeamento 422 for definido, inverter
        // esta sentinela para a prova do contrato corrigido.
        mockMvc.perform(get("/regra-negocio").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.erro").value("Regra de negócio não atendida"))
    }

    @RestController
    private class RegraNegocioController {

        @GetMapping("/regra-negocio")
        fun executar(): ApiError = throw RegraNegocioException("Regra de negócio não atendida")
    }
}
