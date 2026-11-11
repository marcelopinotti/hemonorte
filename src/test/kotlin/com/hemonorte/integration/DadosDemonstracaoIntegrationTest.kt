package com.hemonorte.integration

import com.hemonorte.TestcontainersConfiguration
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.EstoqueMinimoRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.SlotAgendaRepository
import com.hemonorte.security.DemoDataInitializer
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test
import kotlin.test.assertEquals

@Tag("integration")
@Import(TestcontainersConfiguration::class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DadosDemonstracaoIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var demoDataInitializer: DemoDataInitializer
    @Autowired lateinit var doadorRepository: DoadorRepository
    @Autowired lateinit var funcionarioRepository: FuncionarioRepository
    @Autowired lateinit var slotAgendaRepository: SlotAgendaRepository
    @Autowired lateinit var estoqueMinimoRepository: EstoqueMinimoRepository

    @Test
    fun `perfil demo cria dados ficticios utilizaveis uma unica vez`() {
        demoDataInitializer.run(DefaultApplicationArguments())

        assertEquals(1, doadorRepository.findAll().count { it.cpf == "52998224725" })
        assertEquals(1, funcionarioRepository.findAll().count { it.matricula == "triagem.demo" })
        assertEquals(8, estoqueMinimoRepository.count())
        assertEquals(15, slotAgendaRepository.findAll().count { it.capacidade == 3 })

        mockMvc.perform(
            post("/auth/login/doador").contentType(MediaType.APPLICATION_JSON)
                .content("""{"cpf":"52998224725","senha":"Demo-12345"}"""),
        ).andExpect(status().isOk)
        mockMvc.perform(
            post("/auth/login/funcionario").contentType(MediaType.APPLICATION_JSON)
                .content("""{"matricula":"laboratorio.demo","senha":"Demo-12345"}"""),
        ).andExpect(status().isOk)
    }
}
