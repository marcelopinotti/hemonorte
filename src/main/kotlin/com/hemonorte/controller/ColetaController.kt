package com.hemonorte.controller

import com.hemonorte.dto.BolsaSangueResponse
import com.hemonorte.dto.CriarBolsaRequest
import com.hemonorte.dto.toResponse
import com.hemonorte.service.BolsaSangueService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/coletas")
@PreAuthorize("hasAnyRole('COLETA', 'ADMIN')")
class ColetaController(private val bolsaSangueService: BolsaSangueService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@Valid @RequestBody request: CriarBolsaRequest): BolsaSangueResponse =
        bolsaSangueService.criar(
            request.agendamentoId,
            request.tipagem,
            request.dataColeta,
            request.dataValidade,
        ).toResponse()

    @GetMapping
    fun listar(): List<BolsaSangueResponse> = bolsaSangueService.listar().map { it.toResponse() }
}
