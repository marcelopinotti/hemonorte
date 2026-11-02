package com.hemonorte.security

import com.hemonorte.model.Funcionario
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.repository.FuncionarioRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.Base64

@Component
class AdminInitializer(
    private val funcionarioRepository: FuncionarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jdbcTemplate: JdbcTemplate,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) = initialize()

    @Transactional
    fun initialize() {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock($ADMIN_LOCK_ID)")
        if (funcionarioRepository.existsByPerfil(PerfilFuncionario.ADMIN)) return

        val senha = gerarSenha()
        funcionarioRepository.saveAndFlush(
            Funcionario(
                matricula = "admin",
                nome = "Administrador inicial",
                email = "admin@hemonorte.local",
                senhaHash = passwordEncoder.encode(senha) ?: error("Falha ao codificar senha do ADMIN"),
                perfil = PerfilFuncionario.ADMIN,
            ),
        )
        logger.warn("ADMIN inicial criado. Matrícula: admin. Senha temporária: {}", senha)
    }

    private fun gerarSenha(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        val logger = LoggerFactory.getLogger(AdminInitializer::class.java)
        val secureRandom = SecureRandom()
        const val ADMIN_LOCK_ID = 0x48454D4F4E4F5254L
    }
}
