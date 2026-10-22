package com.hemonorte.controller

import com.hemonorte.dto.RegistrarColetaRequest
import com.hemonorte.model.Coleta
import com.hemonorte.service.ColetaService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/coletas")
class ColetaController(
    private val coletaService: ColetaService
) {

    @PostMapping
    fun registrarColeta(@RequestBody @Valid request: RegistrarColetaRequest): ResponseEntity<Coleta> {
        val coleta = coletaService.registrarColeta(
            triagemId = request.triagemId,
            funcionarioId = request.funcionarioId,
            volumeMl = request.volumeMl
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(coleta)
    }

    @GetMapping("/triagem/{triagemId}")
    fun buscarPorTriagem(@PathVariable triagemId: UUID): ResponseEntity<Coleta> {
        val coleta = coletaService.buscarPorTriagem(triagemId)
        return ResponseEntity.ok(coleta)
    }
}
