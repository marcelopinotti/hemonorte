package com.hemonorte.controller

import com.hemonorte.dto.AtualizacaoFuncionarioRequest
import com.hemonorte.dto.CadastroFuncionarioRequest
import com.hemonorte.dto.FuncionarioResponse
import com.hemonorte.service.FuncionarioService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/funcionario")
class FuncionarioController(private val service: FuncionarioService) {



    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun cadastrar(@Valid @RequestBody request: CadastroFuncionarioRequest): FuncionarioResponse = service.cadastrar(request)


    @GetMapping
    fun listar(): List<FuncionarioResponse> = service.listar()

    @GetMapping("/{id}")
    fun buscar(@PathVariable id: UUID): FuncionarioResponse = service.buscar(id)

    @PutMapping("/{id}")
    fun atualizar(@PathVariable id: UUID, @Valid @RequestBody request: AtualizacaoFuncionarioRequest): FuncionarioResponse =
        service.atualizar(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun excluir(@PathVariable id: UUID) {
        service.excluir(id)
    }

}