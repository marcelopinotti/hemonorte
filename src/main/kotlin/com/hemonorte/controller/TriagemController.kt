package com.hemonorte.controller

import com.hemonorte.dto.RealizarTriagemRequest
import com.hemonorte.model.Triagem
import com.hemonorte.service.TriagemService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/triagens")
class TriagemController(
    private val triagemService: TriagemService
) {

    @PostMapping
    fun realizarTriagem(@RequestBody @Valid request: RealizarTriagemRequest): ResponseEntity<Triagem> {
        val triagem = triagemService.realizarTriagem(
            agendamentoId = request.agendamentoId,
            peso = request.peso,
            tatuagemRecente = request.tatuagemRecente,
            cirurgiaRecente = request.cirurgiaRecente,
            teveFebre = request.teveFebre,
            funcionarioId = request.funcionarioId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(triagem)
    }

    @GetMapping("/agendamento/{agendamentoId}")
    fun buscarPorAgendamento(@PathVariable agendamentoId: UUID): ResponseEntity<Triagem> {
        val triagem = triagemService.buscarPorAgendamento(agendamentoId)
        return ResponseEntity.ok(triagem)
    }
}
