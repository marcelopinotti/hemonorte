package com.hemonorte.service

import com.hemonorte.dto.TokenResponse
import com.hemonorte.exception.AutenticacaoException
import com.hemonorte.model.enums.TipoUsuario
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.FuncionarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val doadorRepository: DoadorRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    fun autenticarDoador(cpf: String, senha: String): TokenResponse {
        val doador = doadorRepository.findByCpf(cpf) ?: credenciaisInvalidas()
        if (!validarSenha(senha, doador.senhaHash)) credenciaisInvalidas()

        val tipo = TipoUsuario.DOADOR
        return TokenResponse(
            token = jwtService.gerarToken(doador.id, tipo.name),
            tipo = tipo.name,
            expiresIn = jwtService.expiresIn,
        )
    }

    fun autenticarFuncionario(matricula: String, senha: String): TokenResponse {
        val funcionario = funcionarioRepository.findByMatricula(matricula) ?: credenciaisInvalidas()
        if (!validarSenha(senha, funcionario.senhaHash) || !rejeitarFuncionarioInativo(funcionario.ativo)) {
            credenciaisInvalidas()
        }

        val tipo = TipoUsuario.FUNCIONARIO
        return TokenResponse(
            token = jwtService.gerarToken(funcionario.id, tipo.name, funcionario.perfil),
            tipo = tipo.name,
            expiresIn = jwtService.expiresIn,
        )
    }

    fun validarSenha(senha: String, senhaHash: String): Boolean =
        passwordEncoder.matches(senha, senhaHash)

    fun rejeitarFuncionarioInativo(ativo: Boolean): Boolean = ativo

    private fun credenciaisInvalidas(): Nothing =
        throw AutenticacaoException()
}
