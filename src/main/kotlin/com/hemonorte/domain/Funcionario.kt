package com.hemonorte.domain

import com.hemonorte.domain.enums.PerfilFuncionario
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "funcionario")
class Funcionario(
    matricula: String,
    nome: String,
    email: String,
    senhaHash: String,
    perfil: PerfilFuncionario,

    @Id
    val id: UUID = UUID.randomUUID(),
) {

    @Column(nullable = false, unique = true)
    var matricula: String = validarMatricula(matricula)
        protected set

    @Column(nullable = false)
    var nome: String = validarNome(nome)
        protected set

    @Column(nullable = false, unique = true)
    var email: String = validarEmail(email)
        protected set

    @Column(name = "senha_hash", nullable = false)
    var senhaHash: String = validarSenhaHash(senhaHash)
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var perfil: PerfilFuncionario = perfil
        protected set

    @Column(nullable = false)
    var ativo: Boolean = true
        protected set

    fun desativar() {
        ativo = false
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
        if (other !is Funcionario) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Funcionario(id=$id)"

    private companion object {

        fun validarMatricula(valor: String): String {
            require(valor.isNotBlank()) { "Matrícula é obrigatória" }
            return valor.trim()
        }

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
    }
}
