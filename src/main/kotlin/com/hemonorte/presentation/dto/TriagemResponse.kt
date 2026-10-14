package com.hemonorte.presentation.dto

import java.util.*

data class TriagemResponse(
    val id: UUID,
    val doadorId: UUID,
    val apto: Boolean,
    val motivoInaptidao: String?
)