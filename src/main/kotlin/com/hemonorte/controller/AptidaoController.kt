package com.hemonorte.controller

import com.hemonorte.repository.DoadorRepository
import com.hemonorte.model.ResultadoAptidao
import com.hemonorte.security.DoadorAutenticado
import com.hemonorte.security.usuarioAutenticado
import com.hemonorte.service.AptidaoService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/doadores")
class AptidaoController(
    private val aptidaoService: AptidaoService,
    private val doadorRepository: DoadorRepository
) {

    @GetMapping("/{id}/aptidao")
    @PreAuthorize("hasAuthority('TIPO_DOADOR') or hasAnyRole('TRIAGEM', 'ADMIN')")
    fun verificarAptidao(@PathVariable id: UUID, authentication: Authentication): ResponseEntity<ResultadoAptidao> {
        val principal = authentication.usuarioAutenticado()
        if (principal is DoadorAutenticado && principal.id != id) {
            throw org.springframework.security.access.AccessDeniedException("Acesso negado")
        }
        val doador = doadorRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val resultado = aptidaoService.verificarAptidao(doador)
        return ResponseEntity.ok(resultado)
    }
}
