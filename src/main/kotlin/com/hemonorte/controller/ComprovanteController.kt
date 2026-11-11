package com.hemonorte.controller

import com.hemonorte.dto.ComprovanteResponse
import com.hemonorte.service.ComprovanteService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/comprovantes")
class ComprovanteController(private val comprovanteService: ComprovanteService) {

    @GetMapping("/{codigo}")
    fun validar(@PathVariable codigo: UUID): ComprovanteResponse = comprovanteService.validar(codigo)
}
