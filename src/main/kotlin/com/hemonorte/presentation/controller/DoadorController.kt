package com.hemonorte.presentation.controller

import com.hemonorte.application.DoadorService
import com.hemonorte.presentation.dto.CadastroDoadorRequest
import com.hemonorte.presentation.dto.DoadorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/doadores")
class DoadorController(
    private val doadorService: DoadorService
) {

    @PostMapping
    fun cadastrar(@RequestBody request: CadastroDoadorRequest): ResponseEntity<DoadorResponse> {
        val novoDoador = doadorService.cadastrar(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(novoDoador)
    }

    @GetMapping
    fun listar(): ResponseEntity<List<DoadorResponse>> {
        val doadores = doadorService.listar()
        return ResponseEntity.ok(doadores)
    }
}