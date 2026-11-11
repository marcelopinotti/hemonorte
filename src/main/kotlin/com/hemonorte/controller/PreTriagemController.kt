package com.hemonorte.controller

import com.hemonorte.dto.PerguntaPreTriagemResponse
import com.hemonorte.dto.PreTriagemRequest
import com.hemonorte.dto.PreTriagemResponse
import com.hemonorte.dto.perguntasPreTriagem
import com.hemonorte.dto.toResponse
import com.hemonorte.security.doadorAutenticado
import com.hemonorte.service.PreTriagemService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/doador/me/pre-triagem")
@PreAuthorize("hasAuthority('TIPO_DOADOR')")
class PreTriagemController(private val preTriagemService: PreTriagemService) {

    @GetMapping("/perguntas")
    fun perguntas(): List<PerguntaPreTriagemResponse> = perguntasPreTriagem()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun responder(
        authentication: Authentication,
        @Valid @RequestBody request: PreTriagemRequest,
    ): PreTriagemResponse = preTriagemService.responder(
        doadorId = authentication.doadorAutenticado().id,
        peso = request.peso,
        tatuagemRecente = request.tatuagemRecente,
        cirurgiaRecente = request.cirurgiaRecente,
        teveFebre = request.teveFebre,
    ).toResponse()

    @GetMapping
    fun ultima(authentication: Authentication): PreTriagemResponse =
        preTriagemService.ultima(authentication.doadorAutenticado().id).toResponse()
}
