package com.hemonorte.repository

import jakarta.persistence.LockModeType
import org.junit.jupiter.api.Tag
import org.springframework.data.jpa.repository.Lock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Tag("unit")
class SlotAgendaRepositoryTest {

    @Test
    fun `busca por id usa lock pessimista de escrita`() {
        val metodo = SlotAgendaRepository::class.java.declaredMethods
            .singleOrNull { it.name == "findById" }

        assertNotNull(metodo)
        assertEquals(LockModeType.PESSIMISTIC_WRITE, metodo.getAnnotation(Lock::class.java)?.value)
    }
}
