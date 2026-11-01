package com.hemonorte.controller

import com.hemonorte.dto.RealizarTriagemRequest
import com.hemonorte.dto.TriagemResponse
import com.hemonorte.dto.toResponse
import com.hemonorte.service.TriagemService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import com.hemonorte.security.funcionarioAutenticado
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/triagens")
class TriagemController(
    private val triagemService: TriagemService
) {

    @PostMapping
    @PreAuthorize("hasAnyRole('TRIAGEM', 'ADMIN')")
    fun realizarTriagem(
        @RequestBody @Valid request: RealizarTriagemRequest, authentication: Authentication,): ResponseEntity<TriagemResponse> {
        val triagem = triagemService.realizarTriagem(
            agendamentoId = request.agendamentoId,
            peso = request.peso,
            tatuagemRecente = request.tatuagemRecente,
            cirurgiaRecente = request.cirurgiaRecente,
            teveFebre = request.teveFebre,
            funcionarioId = authentication.funcionarioAutenticado().id
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(triagem.toResponse())
    }

    @GetMapping("/agendamento/{agendamentoId}")
    @PreAuthorize("hasAnyRole('TRIAGEM', 'COLETA', 'ADMIN')")
    fun buscarPorAgendamento(@PathVariable agendamentoId: UUID): ResponseEntity<TriagemResponse> {
        val triagem = triagemService.buscarPorAgendamento(agendamentoId)
        return ResponseEntity.ok(triagem.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRIAGEM', 'COLETA', 'ADMIN')")
    fun listar(): List<TriagemResponse> = triagemService.listar().map { it.toResponse() }
}
