package com.hemonorte.controller

import com.hemonorte.dto.AtualizacaoDoadorRequest
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
            senhaHash = request.senhaHash,
            tipagem = request.tipagem,
            dataNascimento = request.dataNascimento,
            genero = request.genero,
        ).toResponse()

    @GetMapping
    fun listar(): List<DoadorResponse> = doadorService.listar().map { it.toResponse() }

    @GetMapping("/{id}")
    fun buscar(@PathVariable id: UUID): DoadorResponse = doadorService.buscar(id).toResponse()

    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AtualizacaoDoadorRequest,
    ): DoadorResponse = doadorService.atualizar(id, request.nome, request.email, request.senhaHash).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun excluir(@PathVariable id: UUID) {
        doadorService.excluir(id)
    }
}
