package com.hemonorte.dto

import java.time.LocalDate
import java.util.UUID

data class ComprovanteResponse(
    val codigo: UUID,
    val valido: Boolean,
    val doador: String,
    val dataDoacao: LocalDate,
)
