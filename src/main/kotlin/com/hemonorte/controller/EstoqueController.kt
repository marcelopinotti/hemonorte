package com.hemonorte.controller

import com.hemonorte.dto.EstoqueMinimoRequest
import com.hemonorte.model.EstoqueMinimo
import com.hemonorte.service.EstoqueService
import com.hemonorte.service.BolsaSangueService
import com.hemonorte.dto.CriarBolsaRequest
import com.hemonorte.dto.AtualizarBolsaRequest
import com.hemonorte.model.BolsaSangue
import com.hemonorte.dto.BolsaSangueResponse
import com.hemonorte.dto.toResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.http.HttpStatus
import java.util.UUID

@RestController
@RequestMapping("/estoque")
class EstoqueController(
    private val estoqueService: EstoqueService,
    private val bolsaSangueService: BolsaSangueService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('COLETA', 'ADMIN')")
    fun criarBolsa(@RequestBody @Valid request: CriarBolsaRequest): BolsaSangueResponse =
        bolsaSangueService.criar(
            request.agendamentoId,
            request.tipagem,
            request.dataColeta,
            request.dataValidade,
        ).toResponse()

    @GetMapping
    @PreAuthorize("hasAnyRole('COLETA', 'LABORATORIO', 'ADMIN')")
    fun listarBolsas(): List<BolsaSangueResponse> = bolsaSangueService.listar().map { it.toResponse() }

    @GetMapping("/pendentes")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')")
    fun listarPendentes(): List<BolsaSangueResponse> = bolsaSangueService.listarPendentes().map { it.toResponse() }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COLETA', 'LABORATORIO', 'ADMIN')")
    fun atualizarBolsa(
        @PathVariable id: UUID,
        @RequestBody @Valid request: AtualizarBolsaRequest,
    ): BolsaSangueResponse = bolsaSangueService.atualizarStatus(id, request.status).toResponse()

    @PutMapping("/minimos")
    @PreAuthorize("hasAnyRole('COLETA', 'LABORATORIO', 'ADMIN')")
    fun definirEstoqueMinimo(@RequestBody @Valid request: EstoqueMinimoRequest): ResponseEntity<EstoqueMinimo> {
        val estoqueMinimo = estoqueService.definirEstoqueMinimo(request.tipagem, request.quantidadeCritica)
        return ResponseEntity.ok(estoqueMinimo)
    }

    @GetMapping("/criticos")
    @PreAuthorize("hasAnyRole('COLETA', 'LABORATORIO', 'ADMIN')")
    fun listarEstoquesCriticos(): ResponseEntity<List<EstoqueMinimo>> {
        val criticos = estoqueService.listarEstoquesCriticos()
        return ResponseEntity.ok(criticos)
    }
}
