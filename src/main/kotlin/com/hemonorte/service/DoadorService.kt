package com.hemonorte.service

import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.Doador
import com.hemonorte.repository.DoadorRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DoadorService(
    private val doadorRepository: DoadorRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun cadastrar(
        nome: String,
        email: String,
        cpf: String,
        senha: String,
        tipagem: String,
        dataNascimento: LocalDate,
        genero: Char,
    ): Doador {
        if (doadorRepository.existsByCpf(cpf)) throw ConflitoException("CPF já cadastrado")
        if (doadorRepository.existsByEmail(email)) throw ConflitoException("E-mail já cadastrado")

        val doador = Doador(
            nome = nome,
            email = email,
            cpf = cpf,
            senhaHash = codificar(senha),
            tipagem = tipagem,
            dataNascimento = dataNascimento,
            genero = genero,
        )

        return try {
            doadorRepository.saveAndFlush(doador)
        } catch (_: DataIntegrityViolationException) {
            throw ConflitoException("CPF ou e-mail já cadastrado")
        }
    }

    fun listar(): List<Doador> = doadorRepository.findAll()

    fun buscar(id: UUID): Doador = doadorRepository.findById(id).orElse(null)
        ?: throw NoSuchElementException("Doador não encontrado")

    @Transactional
    fun atualizar(
        id: UUID,
        nome: String,
        email: String,
        tipagem: String,
        dataNascimento: LocalDate,
        genero: Char,
    ): Doador {
        val doador = buscar(id)
        if (email != doador.email && doadorRepository.existsByEmail(email)) {
            throw ConflitoException("E-mail já cadastrado")
        }

        doador.alterarNome(nome)
        doador.alterarEmail(email)
        doador.alterarTipagem(tipagem)
        doador.alterarDataNascimento(dataNascimento)
        doador.alterarGenero(genero)
        return try {
            doadorRepository.saveAndFlush(doador)
        } catch (_: DataIntegrityViolationException) {
            throw ConflitoException("E-mail já cadastrado")
        }
    }

    @Transactional
    fun alterarSenha(id: UUID, senha: String) {
        val doador = buscar(id)
        doador.alterarSenha(codificar(senha))
        doadorRepository.saveAndFlush(doador)
    }

    @Transactional
    fun excluir(id: UUID) {
        try {
            doadorRepository.delete(buscar(id))
            doadorRepository.flush()
        } catch (_: DataIntegrityViolationException) {
            throw ConflitoException("Doador possui histórico vinculado")
        }
    }

    private fun codificar(senha: String): String =
        passwordEncoder.encode(senha) ?: error("Falha ao codificar senha")
}
