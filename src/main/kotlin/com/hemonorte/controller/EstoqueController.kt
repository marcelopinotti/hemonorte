package com.hemonorte.controller

import com.hemonorte.model.EstoqueMinimo
import com.hemonorte.service.EstoqueService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
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

data class EstoqueMinimoRequest(
    @field:NotBlank
    val tipagem: String,

    @field:NotNull
    @field:Positive
    val quantidadeCritica: Int
)