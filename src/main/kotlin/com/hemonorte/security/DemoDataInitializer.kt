package com.hemonorte.security

import com.hemonorte.model.Doador
import com.hemonorte.model.EstoqueMinimo
import com.hemonorte.model.Funcionario
import com.hemonorte.model.SlotAgenda
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.EstoqueMinimoRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.SlotAgendaRepository
import com.hemonorte.service.EstoqueService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Component
@Profile("demo")
class DemoDataInitializer(
    private val doadorRepository: DoadorRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val slotAgendaRepository: SlotAgendaRepository,
    private val estoqueMinimoRepository: EstoqueMinimoRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (funcionarioRepository.existsByMatricula("triagem.demo")) return
        val senhaHash = passwordEncoder.encode(SENHA) ?: error("Falha ao codificar senha de demonstração")

        listOf(
            Triple("triagem.demo", "Equipe Triagem (fictícia)", PerfilFuncionario.TRIAGEM),
            Triple("coleta.demo", "Equipe Coleta (fictícia)", PerfilFuncionario.COLETA),
            Triple("laboratorio.demo", "Equipe Laboratório (fictícia)", PerfilFuncionario.LABORATORIO),
        ).forEach { (matricula, nome, perfil) ->
            funcionarioRepository.save(Funcionario(matricula, nome, "$matricula@exemplo.local", senhaHash, perfil))
        }

        listOf(
            Doador("Ana Exemplo", "ana@exemplo.local", senhaHash, "O-", LocalDate.of(1992, 4, 10), 'F', "52998224725"),
            Doador("Bruno Exemplo", "bruno@exemplo.local", senhaHash, "A+", LocalDate.of(1988, 9, 3), 'M', "11144477735"),
            Doador("Carla Exemplo", "carla@exemplo.local", senhaHash, "B+", LocalDate.of(2000, 1, 25), 'F', "22255588846"),
        ).forEach(doadorRepository::save)

        val amanha = LocalDate.now(FUSO).plusDays(1)
        (0L until 5L).forEach { dia ->
            listOf(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0)).forEach { hora ->
                val inicio = amanha.plusDays(dia).atTime(hora).atZone(FUSO).toInstant()
                if (!slotAgendaRepository.existsByInicio(inicio)) slotAgendaRepository.save(SlotAgenda(inicio, 3))
            }
        }

        EstoqueService.TIPAGENS.forEach { estoqueMinimoRepository.save(EstoqueMinimo(it, 3)) }

        logger.warn(
            "Dados FICTÍCIOS de demonstração criados. Senha de todos: {}. Funcionários: triagem.demo, coleta.demo, " +
                "laboratorio.demo. Doadores (CPF): 52998224725, 11144477735, 22255588846.",
            SENHA,
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger(DemoDataInitializer::class.java)
        val FUSO: ZoneId = ZoneId.of("America/Fortaleza")
        const val SENHA = "Demo-12345"
    }
}
