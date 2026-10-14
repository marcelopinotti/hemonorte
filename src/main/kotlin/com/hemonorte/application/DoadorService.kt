package com.hemonorte.application

import com.hemonorte.presentation.dto.CadastroDoadorRequest
import com.hemonorte.presentation.dto.DoadorResponse
import com.hemonorte.domain.Doador
import com.hemonorte.infra.DoadorRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class DoadorService(
    private val doadorRepository: DoadorRepository,
) {

@Transactional
    fun cadastrar(request: CadastroDoadorRequest): DoadorResponse {
        require(!doadorRepository.existsByCpf(request.cpf)) { "CPF já cadastrado" }
        require(!doadorRepository.existsByEmail(request.email)) { "E-mail já cadastrado" }
        
        val doador = Doador(
            nome = request.nome,
            email = request.email,
            senhaHash = request.senha,
            tipagem = request.tipagem,
            dataNascimento = request.dataNascimento,
            genero = request.genero,
            cpf = request.cpf
        )
        
        val salvo = doadorRepository.save(doador)
        
        return DoadorResponse(
            id = salvo.id,
            nome = salvo.nome,
            email = salvo.email,
            tipagem = salvo.tipagem,
            dataNascimento = salvo.dataNascimento,
            genero = salvo.genero
        )
    }

    @Transactional
    fun listar(): List<DoadorResponse> {
        return doadorRepository.findAll().map { doador ->
            DoadorResponse(
                id = doador.id,
                nome = doador.nome,
                email = doador.email,
                tipagem = doador.tipagem,
                dataNascimento = doador.dataNascimento,
                genero = doador.genero
            )
        }
    }
}