package com.hemonorte.security

import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.model.enums.TipoUsuario
import org.springframework.security.core.Authentication
import java.util.UUID

sealed interface UsuarioAutenticado {
    val id: UUID
    val tipo: TipoUsuario
}

data class DoadorAutenticado(override val id: UUID) : UsuarioAutenticado {
    override val tipo = TipoUsuario.DOADOR
}

data class FuncionarioAutenticado(
    override val id: UUID,
    val perfil: PerfilFuncionario,
) : UsuarioAutenticado {
    override val tipo = TipoUsuario.FUNCIONARIO
}

fun Authentication.usuarioAutenticado(): UsuarioAutenticado =
    principal as? UsuarioAutenticado ?: error("Principal autenticado inválido")

fun Authentication.doadorAutenticado(): DoadorAutenticado =
    principal as? DoadorAutenticado ?: error("Endpoint exclusivo para doador")

fun Authentication.funcionarioAutenticado(): FuncionarioAutenticado =
    principal as? FuncionarioAutenticado ?: error("Endpoint exclusivo para funcionário")
