package com.hemonorte.controller

import com.hemonorte.exception.RegraNegocioException
import org.junit.jupiter.api.Tag
import org.springframework.orm.ObjectOptimisticLockingFailureException
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
        .standaloneSetup(ExcecaoController())
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    @Test
    fun `SENTINELA QA-044 - RegraNegocioException devolve 200 hoje em vez de status de erro`() {
        mockMvc.perform(get("/regra-negocio").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.erro").value("Regra de negócio não atendida"))
    }

    @Test
    fun `conflito de versao otimista devolve 409`() {
        mockMvc.perform(get("/versao").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.erro").value("Registro alterado por outra operação; tente novamente"))
    }

    @RestController
    private class ExcecaoController {

        @GetMapping("/versao")
        fun versao(): ApiError = throw ObjectOptimisticLockingFailureException("Agendamento", "id")

        @GetMapping("/regra-negocio")
        fun regraNegocio(): ApiError = throw RegraNegocioException("Regra de negócio não atendida")
    }
}
