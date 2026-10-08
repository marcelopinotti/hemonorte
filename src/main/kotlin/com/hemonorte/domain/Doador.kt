package com.hemonorte.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "doador")
class Doador(
    nome: String,
    email: String,
    senhaHash: String,
    tipagem: String,
    dataNascimento: LocalDate,
    genero: Char,

    @Column(nullable = false, unique = true, updatable = false, length = 11)
    val cpf: String,

    @Id
    val id: UUID = UUID.randomUUID(),
) {

    @Column(nullable = false)
    var nome: String = validarNome(nome)
        protected set

    @Column(nullable = false, unique = true)
    var email: String = validarEmail(email)
        protected set

    @Column(name = "senha_hash", nullable = false)
    var senhaHash: String = validarSenhaHash(senhaHash)
        protected set

    @Column(nullable = false, length = 3)
    var tipagem: String = validarTipagem(tipagem)
        protected set

    @Column(name = "data_nascimento", nullable = false)
    var dataNascimento: LocalDate = dataNascimento
        protected set

    /** M = masculino, F = feminino. */
    @Column(nullable = false, length = 1)
    var genero: Char = validarGenero(genero)
        protected set

    @Column(name = "criado_em", nullable = false, updatable = false)
    val criadoEm: Instant = Instant.now()

    init {
        require(CPF_REGEX.matches(cpf)) { "CPF deve ter 11 dígitos" }
    }

    fun alterarNome(novoNome: String) {
        nome = validarNome(novoNome)
    }

    fun alterarEmail(novoEmail: String) {
        email = validarEmail(novoEmail)
    }

    fun alterarSenha(novoHash: String) {
        senhaHash = validarSenhaHash(novoHash)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Doador) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Doador(id=$id)"

    private companion object {
        val CPF_REGEX = Regex("\\d{11}")
        val TIPAGENS_VALIDAS = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

        fun validarNome(valor: String): String {
            require(valor.isNotBlank()) { "Nome é obrigatório" }
            return valor.trim()
        }

        fun validarEmail(valor: String): String {
            require('@' in valor) { "E-mail inválido" }
            return valor.trim()
        }

        fun validarSenhaHash(valor: String): String {
            require(valor.isNotBlank()) { "Senha hash é obrigatória" }
            return valor
        }

        fun validarTipagem(valor: String): String {
            require(valor.uppercase() in TIPAGENS_VALIDAS) { "Tipagem sanguínea inválida: $valor" }
            return valor.uppercase()
        }

        fun validarGenero(valor: Char): Char {
            require(valor == 'M' || valor == 'F') { "Gênero deve ser M ou F" }
            return valor
        }
    }
}