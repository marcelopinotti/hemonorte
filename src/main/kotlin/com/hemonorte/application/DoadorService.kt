package com.hemonorte.application

import com.hemonorte.domain.Doador
import com.hemonorte.infra.DoadorRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class DoadorService(
    private val doadorRepository: DoadorRepository,
) {

    @Transactional
    fun cadastrar(nome: String, email: String, cpf: String): Doador {
        require(!doadorRepository.existsByCpf(cpf)) { "CPF já cadastrado" }
        require(!doadorRepository.existsByEmail(email)) { "E-mail já cadastrado" }
        return doadorRepository.save(Doador(nome = nome, email = email, cpf = cpf))
    }

    @Transactional
    fun listar(): List<Doador> = doadorRepository.findAll()
}