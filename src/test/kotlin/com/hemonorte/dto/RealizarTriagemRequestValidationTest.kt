package com.hemonorte.dto

import jakarta.validation.Validation
import org.junit.jupiter.api.Tag
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class RealizarTriagemRequestValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    private fun violacoesDePeso(peso: String) = validator.validate(
        RealizarTriagemRequest(UUID.randomUUID(), BigDecimal(peso), false, false, false),
    ).map { it.propertyPath.toString() }

    @Test
    fun `peso valido e no limite nao gera violacao`() {
        assertTrue(violacoesDePeso("50.00").isEmpty())
        assertTrue(violacoesDePeso("49.99").isEmpty())
        assertTrue(violacoesDePeso("999.99").isEmpty())
    }

    @Test
    fun `peso invalido gera violacao`() {
        listOf("0", "-1", "1000", "70.001").forEach {
            assertEquals(listOf("peso"), violacoesDePeso(it), "peso $it")
        }
    }
}
