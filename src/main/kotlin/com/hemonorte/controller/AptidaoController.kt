package com.hemonorte.controller

import com.hemonorte.repository.DoadorRepository
import com.hemonorte.service.AptidaoService
import com.hemonorte.service.ResultadoAptidao
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/doadores")
class AptidaoController(
    private val aptidaoService: AptidaoService,
    private val doadorRepository: DoadorRepository
) {

    @GetMapping("/{id}/aptidao")
    fun verificarAptidao(@PathVariable id: UUID): ResponseEntity<ResultadoAptidao> {
        val doador = doadorRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val resultado = aptidaoService.verificarAptidao(doador)
        return ResponseEntity.ok(resultado)
    }
}