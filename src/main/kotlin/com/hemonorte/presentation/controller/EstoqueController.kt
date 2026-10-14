package com.hemonorte.presentation.controller

import com.hemonorte.application.EstoqueService
import com.hemonorte.domain.BolsaSangue
import com.hemonorte.presentation.dto.BolsaRequest
import com.hemonorte.presentation.dto.EstoqueStatusResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/estoque")
class EstoqueController(
    private val estoqueService: EstoqueService
) {
    // Endpoints de estoque serão ativados em breve junto com o grupo
}