package com.hemonorte.presentation.controller

import com.hemonorte.application.AgendamentoService
import com.hemonorte.presentation.dto.AgendamentoRequest
import com.hemonorte.presentation.dto.AgendamentoResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/agendamentos")
class AgendamentoController(
    private val agendamentoService: AgendamentoService
) {

    @PostMapping
    fun criar(@RequestBody @Valid request: AgendamentoRequest): ResponseEntity<AgendamentoResponse> {
        val novoAgendamento = agendamentoService.criar(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(novoAgendamento)
    }

    @GetMapping
    fun listar(): ResponseEntity<List<AgendamentoResponse>> {
        val lista = agendamentoService.listar()
        return ResponseEntity.ok(lista)
    }
}