package com.hemonorte.service

import com.hemonorte.model.Doador
import com.hemonorte.repository.DoadorRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class DoadorService(
    private val doadorRepository: DoadorRepository,
) {

    @Transactional
    fun cadastrar(nome: String, email: String, cpf: String, senhaHash: String, tipagem: String, dataNascimento: LocalDate, genero: Char): Doador {
        require(!doadorRepository.existsByCpf(cpf)) { "CPF já cadastrado" }
        require(!doadorRepository.existsByEmail(email)) { "E-mail já cadastrado" }

        val doador = Doador(
            nome = nome,
            email = email,
            cpf = cpf,
            senhaHash = senhaHash,
            tipagem = tipagem,
            dataNascimento = dataNascimento,
            genero = genero,
        )

        return doadorRepository.save(doador)
    }

    @Transactional
    fun listar(): List<Doador> = doadorRepository.findAll()

    @Transactional
    fun buscar(id: UUID): Doador = doadorRepository.findById(id).orElse(null)
        ?: throw NoSuchElementException("Doador não encontrado")

    @Transactional
    fun atualizar(id: UUID, nome: String, email: String, senhaHash: String): Doador {
        val doador = buscar(id)
        require(email == doador.email || !doadorRepository.existsByEmail(email)) { "E-mail já cadastrado" }

        doador.alterarNome(nome)
        doador.alterarEmail(email)
        doador.alterarSenha(senhaHash)
        return doadorRepository.save(doador)
    }

    @Transactional
    fun excluir(id: UUID) {
        doadorRepository.delete(buscar(id))
    }
}
