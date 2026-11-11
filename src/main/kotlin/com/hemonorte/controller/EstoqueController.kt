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
import com.hemonorte.dto.HistoricoAuditoriaResponse
import com.hemonorte.security.funcionarioAutenticado
import com.hemonorte.service.EstoqueCritico
import org.springframework.security.core.Authentication
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
        authentication: Authentication,
    ): BolsaSangueResponse = bolsaSangueService.atualizarStatus(
        id = id,
        status = request.status,
        motivo = request.motivo,
        funcionarioId = authentication.funcionarioAutenticado().id,
    ).toResponse()

    @GetMapping("/{id}/historico")
    @PreAuthorize("hasAnyRole('COLETA', 'LABORATORIO', 'ADMIN')")
    fun historico(@PathVariable id: UUID): List<HistoricoAuditoriaResponse> =
        bolsaSangueService.historico(id).map { it.toResponse() }

    @PutMapping("/minimos")
    @PreAuthorize("hasAnyRole('COLETA', 'LABORATORIO', 'ADMIN')")
    fun definirEstoqueMinimo(@RequestBody @Valid request: EstoqueMinimoRequest): ResponseEntity<EstoqueMinimo> {
        val estoqueMinimo = estoqueService.definirEstoqueMinimo(request.tipagem, request.quantidadeCritica)
        return ResponseEntity.ok(estoqueMinimo)
    }

    @GetMapping("/disponiveis")
    @PreAuthorize("hasAnyRole('COLETA', 'LABORATORIO', 'ADMIN')")
    fun disponiveisPorTipagem(): Map<String, Long> = estoqueService.disponiveisPorTipagem()

    @GetMapping("/criticos")
    @PreAuthorize("hasAnyRole('COLETA', 'LABORATORIO', 'ADMIN')")
    fun listarEstoquesCriticos(): List<EstoqueCritico> = estoqueService.listarEstoquesCriticos()
}
