package com.hemonorte.service

import com.hemonorte.dto.AtualizacaoFuncionarioRequest
import com.hemonorte.dto.CadastroFuncionarioRequest
import com.hemonorte.dto.FuncionarioResponse
import com.hemonorte.dto.toResponse
import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.Funcionario
import com.hemonorte.repository.FuncionarioRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FuncionarioService(
    private val funcionarioRepository: FuncionarioRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun cadastrar(request: CadastroFuncionarioRequest): FuncionarioResponse {
        if (funcionarioRepository.existsByMatricula(request.matricula)) {
            throw ConflitoException("Matrícula já cadastrada")
        }
        if (funcionarioRepository.existsByEmail(request.email)) {
            throw ConflitoException("E-mail já cadastrado")
        }

        val funcionario = Funcionario(
            matricula = request.matricula,
            nome = request.nome,
            email = request.email,
            senhaHash = codificar(request.senha),
            perfil = request.perfil,
        )

        return try {
            funcionarioRepository.saveAndFlush(funcionario).toResponse()
        } catch (_: DataIntegrityViolationException) {
            throw ConflitoException("Matrícula ou e-mail já cadastrado")
        }
    }

    fun listar(): List<FuncionarioResponse> = funcionarioRepository.findAll().map { it.toResponse() }

    fun buscar(id: UUID): FuncionarioResponse = buscarEntidade(id).toResponse()

    @Transactional
    fun atualizar(id: UUID, request: AtualizacaoFuncionarioRequest): FuncionarioResponse {
        val funcionario = buscarEntidade(id)
        if (request.email != funcionario.email && funcionarioRepository.existsByEmail(request.email)) {
            throw ConflitoException("E-mail já cadastrado")
        }

        funcionario.alterarNome(request.nome)
        funcionario.alterarEmail(request.email)
        funcionario.alterarPerfil(request.perfil)
        return try {
            funcionarioRepository.saveAndFlush(funcionario).toResponse()
        } catch (_: DataIntegrityViolationException) {
            throw ConflitoException("E-mail já cadastrado")
        }
    }

    @Transactional
    fun alterarSenha(id: UUID, senha: String) {
        val funcionario = buscarEntidade(id)
        funcionario.alterarSenha(codificar(senha))
        funcionarioRepository.saveAndFlush(funcionario)
    }

    @Transactional
    fun excluir(id: UUID) {
        val funcionario = buscarEntidade(id)
        funcionario.desativar()
        funcionarioRepository.saveAndFlush(funcionario)
    }

    private fun buscarEntidade(id: UUID): Funcionario =
        funcionarioRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Funcionário não encontrado")

    private fun codificar(senha: String): String =
        passwordEncoder.encode(senha) ?: error("Falha ao codificar senha")
}
