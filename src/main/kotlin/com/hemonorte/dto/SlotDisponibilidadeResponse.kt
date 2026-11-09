package com.hemonorte.dto

import java.time.Instant
import java.util.UUID

data class SlotDisponibilidadeResponse(
    val id: UUID,
    val inicio: Instant,
    val capacidade: Int,
    val ocupacao: Int,
    val vagasDisponiveis: Int,
)
