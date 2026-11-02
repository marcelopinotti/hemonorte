package com.hemonorte.controller

import com.hemonorte.dto.RegistrarExameRequest
import com.hemonorte.model.ExameSorologico
import com.hemonorte.dto.ExameSorologicoResponse
import com.hemonorte.dto.toResponse
import com.hemonorte.service.ExameSorologicoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import com.hemonorte.security.funcionarioAutenticado
import java.util.UUID

@RestController
@RequestMapping("/exames-sorologicos")
class ExameSorologicoController(
    private val exameSorologicoService: ExameSorologicoService
) {

    @PostMapping
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')")
    fun registrarExame(
        @RequestBody @Valid request: RegistrarExameRequest,
        authentication: Authentication,
    ): ResponseEntity<ExameSorologicoResponse> {
        val exame = exameSorologicoService.registrarExame(
            bolsaId = request.bolsaId,
            funcionarioId = authentication.funcionarioAutenticado().id,
            hiv = request.hiv,
            hepatite = request.hepatite,
            chagas = request.chagas
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(exame.toResponse())
    }

    @GetMapping("/bolsa/{bolsaId}")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')")
    fun buscarPorBolsa(@PathVariable bolsaId: UUID): ResponseEntity<ExameSorologicoResponse> {
        val exame = exameSorologicoService.buscarPorBolsa(bolsaId)
        return ResponseEntity.ok(exame.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')")
    fun listar(): List<ExameSorologicoResponse> = exameSorologicoService.listar().map { it.toResponse() }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')")
    fun buscar(@PathVariable id: UUID): ExameSorologicoResponse = exameSorologicoService.buscar(id).toResponse()
}
