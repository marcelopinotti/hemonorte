package com.hemonorte.controller

import com.hemonorte.dto.RealizarAgendamentoRequest
import com.hemonorte.model.Agendamento
import com.hemonorte.service.AgendamentoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/agendamentos")
class AgendamentoController(
    private val agendamentoService: AgendamentoService
) {

    @PostMapping
    fun realizarAgendamento(@RequestBody @Valid request: RealizarAgendamentoRequest): ResponseEntity<Agendamento> {
        val agendamento = agendamentoService.realizarAgendamento(request.doadorId, request.slotId)
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamento)
    }

    @PatchMapping("/{id}/cancelar")
    fun cancelarAgendamento(@PathVariable id: UUID): ResponseEntity<Agendamento> {
        val agendamento = agendamentoService.cancelarAgendamento(id)
        return ResponseEntity.ok(agendamento)
    }

    @PatchMapping("/{id}/falta")
    fun registrarFalta(@PathVariable id: UUID): ResponseEntity<Agendamento> {
        val agendamento = agendamentoService.registrarFalta(id)
        return ResponseEntity.ok(agendamento)
    }
}
