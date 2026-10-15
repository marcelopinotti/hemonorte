package com.hemonorte.controller

import com.hemonorte.model.SlotAgenda
import com.hemonorte.service.SlotAgendaService
import jakarta.validation.Valid
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@RestController
@RequestMapping("/slots")
class SlotAgendaController(
    private val slotAgendaService: SlotAgendaService
) {

    @PostMapping
    fun criarSlot(@RequestBody @Valid request: CriarSlotRequest): ResponseEntity<SlotAgenda> {
        val slot = slotAgendaService.criarSlot(request.inicio, request.capacidade)
        return ResponseEntity.status(HttpStatus.CREATED).body(slot)
    }

    @GetMapping
    fun listarPorPeriodo(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) inicio: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fim: LocalDateTime
    ): ResponseEntity<List<SlotAgenda>> {
        val instantInicio = inicio.atZone(ZoneId.systemDefault()).toInstant()
        val instantFim = fim.atZone(ZoneId.systemDefault()).toInstant()

        val slots = slotAgendaService.listarPorPeriodo(instantInicio, instantFim)
        return ResponseEntity.ok(slots)
    }
}

data class CriarSlotRequest(
    @field:NotNull
    @field:Future
    val inicio: Instant,

    @field:Min(1)
    val capacidade: Int
)