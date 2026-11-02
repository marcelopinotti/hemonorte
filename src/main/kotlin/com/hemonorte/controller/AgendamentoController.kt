package com.hemonorte.controller

import com.hemonorte.dto.RealizarAgendamentoRequest
import com.hemonorte.model.Agendamento
import com.hemonorte.dto.AgendamentoResponse
import com.hemonorte.dto.toResponse
import com.hemonorte.service.AgendamentoService
import com.hemonorte.security.DoadorAutenticado
import com.hemonorte.security.FuncionarioAutenticado
import com.hemonorte.security.usuarioAutenticado
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/agendamentos")
class AgendamentoController(
    private val agendamentoService: AgendamentoService
) {

    @PostMapping
    @PreAuthorize("hasAuthority('TIPO_DOADOR') or hasRole('ADMIN')")
    fun realizarAgendamento(
        @RequestBody @Valid request: RealizarAgendamentoRequest,
        authentication: Authentication?,
    ): ResponseEntity<AgendamentoResponse> {
        val principal = authentication?.principal
        val doadorId = when (principal) {
            is DoadorAutenticado -> principal.id
            is FuncionarioAutenticado -> request.doadorId
                ?: throw IllegalArgumentException("doadorId é obrigatório para ADMIN")
            else -> requireNotNull(request.doadorId) { "doadorId é obrigatório" }
        }
        val agendamento = if (authentication == null) {
            agendamentoService.realizarAgendamento(doadorId, request.slotId).toResponse()
        } else {
            agendamentoService.realizarAgendamentoResponse(doadorId, request.slotId)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamento)
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRIAGEM', 'COLETA', 'ADMIN')")
    fun listar(): List<AgendamentoResponse> = agendamentoService.listarResponses()

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun buscar(@PathVariable id: UUID, authentication: Authentication): AgendamentoResponse {
        val principal = authentication.usuarioAutenticado()
        return when {
            principal is DoadorAutenticado -> agendamentoService.buscarResponseDoDoador(id, principal.id)
            principal is FuncionarioAutenticado && principal.perfil.name == "LABORATORIO" ->
                throw org.springframework.security.access.AccessDeniedException("Acesso negado")
            else -> agendamentoService.buscarResponse(id)
        }
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('TIPO_DOADOR') or hasRole('ADMIN')")
    fun cancelarAgendamento(@PathVariable id: UUID, authentication: Authentication?): ResponseEntity<AgendamentoResponse> {
        val principal = authentication?.principal
        val agendamento = when {
            principal is DoadorAutenticado -> agendamentoService.cancelarAgendamentoDoDoadorResponse(id, principal.id)
            authentication != null -> agendamentoService.cancelarAgendamentoResponse(id)
            else -> agendamentoService.cancelarAgendamento(id).toResponse()
        }
        return ResponseEntity.ok(agendamento)
    }

    @PatchMapping("/{id}/falta")
    @PreAuthorize("hasAnyRole('TRIAGEM', 'ADMIN')")
    fun registrarFalta(@PathVariable id: UUID): ResponseEntity<AgendamentoResponse> {
        val agendamento = agendamentoService.registrarFaltaResponse(id)
        return ResponseEntity.ok(agendamento)
    }
}
