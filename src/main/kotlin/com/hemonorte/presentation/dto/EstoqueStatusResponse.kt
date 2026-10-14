package com.hemonorte.presentation.dto

data class EstoqueStatusResponse(
    val tipagem: String,
    val quantidadeAtual: Long,
    val quantidadeMinima: Int,
    val alertaCritico: Boolean
)