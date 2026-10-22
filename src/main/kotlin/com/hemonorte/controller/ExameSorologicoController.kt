package com.hemonorte.controller

import com.hemonorte.dto.RegistrarExameRequest
import com.hemonorte.model.ExameSorologico
import com.hemonorte.service.ExameSorologicoService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/exames-sorologicos")
class ExameSorologicoController(
    private val exameSorologicoService: ExameSorologicoService
) {

    @PostMapping
    fun registrarExame(@RequestBody @Valid request: RegistrarExameRequest): ResponseEntity<ExameSorologico> {
        val exame = exameSorologicoService.registrarExame(
            bolsaId = request.bolsaId,
            funcionarioId = request.funcionarioId,
            hiv = request.hiv,
            hepatite = request.hepatite,
            chagas = request.chagas
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(exame)
    }

    @GetMapping("/bolsa/{bolsaId}")
    fun buscarPorBolsa(@PathVariable bolsaId: UUID): ResponseEntity<ExameSorologico> {
        val exame = exameSorologicoService.buscarPorBolsa(bolsaId)
        return ResponseEntity.ok(exame)
    }
}
