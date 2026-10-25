package com.hemonorte.controller

import com.hemonorte.dto.AtualizacaoDoadorRequest
import com.hemonorte.dto.AlteracaoSenhaRequest
import com.hemonorte.dto.CadastroDoadorRequest
import com.hemonorte.dto.DoadorResponse
import com.hemonorte.dto.toResponse
import com.hemonorte.service.DoadorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/doador")
class DoadorController(private val doadorService: DoadorService)
{

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun cadastrar(@Valid @RequestBody request: CadastroDoadorRequest): DoadorResponse =
        doadorService.cadastrar(
            nome = request.nome,
            email = request.email,
            cpf = request.cpf,
            senha = request.senha,
            tipagem = request.tipagem,
            dataNascimento = request.dataNascimento,
            genero = request.genero.single(),
        ).toResponse()

    @GetMapping
    fun listar(): List<DoadorResponse> = doadorService.listar().map { it.toResponse() }

    @GetMapping("/{id}")
    fun buscar(@PathVariable id: UUID): DoadorResponse = doadorService.buscar(id).toResponse()

    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AtualizacaoDoadorRequest,
    ): DoadorResponse = doadorService.atualizar(
        id = id,
        nome = request.nome,
        email = request.email,
        tipagem = request.tipagem,
        dataNascimento = request.dataNascimento,
        genero = request.genero.single(),
    ).toResponse()

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun alterarSenha(@PathVariable id: UUID, @Valid @RequestBody request: AlteracaoSenhaRequest) {
        doadorService.alterarSenha(id, request.senha)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun excluir(@PathVariable id: UUID) {
        doadorService.excluir(id)
    }
}
