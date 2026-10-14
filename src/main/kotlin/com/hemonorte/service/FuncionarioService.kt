package com.hemonorte.service

import com.hemonorte.dto.AtualizacaoFuncionarioRequest
import com.hemonorte.dto.CadastroFuncionarioRequest
import com.hemonorte.dto.FuncionarioResponse
import com.hemonorte.dto.toResponse
import com.hemonorte.model.Funcionario
import com.hemonorte.repository.FuncionarioRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class FuncionarioService(private val funcionarioRepository: FuncionarioRepository) {

    fun cadastrar(request: CadastroFuncionarioRequest): FuncionarioResponse {
        require(!funcionarioRepository.existsByMatricula(request.matricula)) {
            "Matrícula já cadastrada"
        }
        require(!funcionarioRepository.existsByEmail(request.email)) {
            "E-mail já cadastrado"
        }

        val funcionario = Funcionario(
            matricula = request.matricula,
            nome = request.nome,
            email = request.email,
            senhaHash = request.senhaHash,
            perfil = request.perfil,
        )

        return funcionarioRepository.save(funcionario).toResponse()
    }

    fun listar(): List<FuncionarioResponse> = funcionarioRepository.findAll().map { it.toResponse() }

    fun buscar(id: UUID): FuncionarioResponse = funcionarioRepository.findById(id).orElse(null)?.toResponse()
        ?: funcionarioNaoEncontrado()
    fun atualizar(id: UUID, request: AtualizacaoFuncionarioRequest, ): FuncionarioResponse {
        val funcionario = funcionarioRepository.findById(id).orElse(null)
            ?: funcionarioNaoEncontrado()
        require(request.email == funcionario.email || !funcionarioRepository.existsByEmail(request.email)) { "E-mail já cadastrado" }

        funcionario.alterarEmail(request.email)
        funcionario.alterarSenha(request.senhaHash)

        return funcionarioRepository.save(funcionario).toResponse()
    }

    fun excluir(id: UUID) {
        val funcionario = funcionarioRepository.findById(id).orElse(null) ?: funcionarioNaoEncontrado()
        funcionarioRepository.delete(funcionario)
    }

    private fun funcionarioNaoEncontrado(): Nothing {
        throw NoSuchElementException("Funcionário não encontrado")
    }
}
