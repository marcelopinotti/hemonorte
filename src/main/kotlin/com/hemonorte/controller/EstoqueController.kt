package com.hemonorte.controller

import com.hemonorte.dto.EstoqueMinimoRequest
import com.hemonorte.model.EstoqueMinimo
import com.hemonorte.service.EstoqueService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/estoque")
class EstoqueController(
    private val estoqueService: EstoqueService
) {

    @PutMapping("/minimos")
    fun definirEstoqueMinimo(@RequestBody @Valid request: EstoqueMinimoRequest): ResponseEntity<EstoqueMinimo> {
        val estoqueMinimo = estoqueService.definirEstoqueMinimo(request.tipagem, request.quantidadeCritica)
        return ResponseEntity.ok(estoqueMinimo)
    }

    @GetMapping("/criticos")
    fun listarEstoquesCriticos(): ResponseEntity<List<EstoqueMinimo>> {
        val criticos = estoqueService.listarEstoquesCriticos()
        return ResponseEntity.ok(criticos)
    }
}
