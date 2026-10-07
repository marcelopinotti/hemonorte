package com.hemonorte.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table(name = "doador")
class Doador(
    nome: String,
    email: String,

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

    init {
        require(CPF_REGEX.matches(cpf)) { "CPF deve ter 11 dígitos" }
    }

    fun alterarNome(novoNome: String) {
        nome = validarNome(novoNome)
    }

    fun alterarEmail(novoEmail: String) {
        email = validarEmail(novoEmail)
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

        fun validarNome(valor: String): String {
            require(valor.isNotBlank()) { "Nome é obrigatório" }
            return valor.trim()
        }

        fun validarEmail(valor: String): String {
            require('@' in valor) { "E-mail inválido" }
            return valor.trim()
        }
    }
}