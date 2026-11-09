package com.hemonorte.controller

import com.hemonorte.dto.CriarSlotRequest
import com.hemonorte.dto.SlotDisponibilidadeResponse
import com.hemonorte.model.SlotAgenda
import com.hemonorte.service.SlotAgendaService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/slots")
class SlotAgendaController(
    private val slotAgendaService: SlotAgendaService,
) {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun criarSlot(@RequestBody @Valid request: CriarSlotRequest): ResponseEntity<SlotAgenda> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(slotAgendaService.criarSlot(request.inicio, request.capacidade))

    @GetMapping
    fun listarPorPeriodo(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) inicio: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fim: OffsetDateTime,
    ): ResponseEntity<List<SlotDisponibilidadeResponse>> = ResponseEntity.ok(
        slotAgendaService.listarPorPeriodo(inicio.toInstant(), fim.toInstant()),
    )

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun atualizarSlot(
        @PathVariable id: UUID,
        @RequestBody @Valid request: CriarSlotRequest,
    ): ResponseEntity<SlotAgenda> = ResponseEntity.ok(
        slotAgendaService.atualizar(id, request.inicio, request.capacidade),
    )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    fun excluirSlot(@PathVariable id: UUID) = slotAgendaService.excluir(id)
}
