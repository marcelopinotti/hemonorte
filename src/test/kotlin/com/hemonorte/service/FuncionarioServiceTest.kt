package com.hemonorte.service

import com.hemonorte.dto.CadastroFuncionarioRequest
import com.hemonorte.model.Funcionario
import com.hemonorte.model.enums.PerfilFuncionario
import com.hemonorte.repository.FuncionarioRepository
import org.junit.jupiter.api.Tag
import org.mockito.Mockito
import kotlin.test.Test

@Tag("unit")
class FuncionarioServiceTest {

    private val repository = Mockito.mock(FuncionarioRepository::class.java)
    private val service = FuncionarioService(repository)

    @Test
    fun `cadastro persiste funcionario`() {
        Mockito.`when`(repository.save(Mockito.any(Funcionario::class.java))).thenAnswer { it.arguments[0] }

        service.cadastrar(
            CadastroFuncionarioRequest(
                matricula = "M123",
                nome = "Marcos Lima",
                email = "marcos@hemonorte.br",
                senhaHash = "hash-seguro",
                perfil = PerfilFuncionario.TRIAGEM,
            ),
        )

        val funcionario = Mockito.mockingDetails(repository).invocations
            .single { it.method.name == "save" }
            .arguments.single() as Funcionario
        kotlin.test.assertEquals("hash-seguro", funcionario.senhaHash)
    }
}
