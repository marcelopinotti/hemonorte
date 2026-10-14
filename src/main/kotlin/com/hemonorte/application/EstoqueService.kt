package com.hemonorte.application

import com.hemonorte.domain.BolsaSangue
import com.hemonorte.infra.BolsaSangueRepository
import com.hemonorte.infra.DoadorRepository
import com.hemonorte.infra.EstoqueMinimoRepository
import com.hemonorte.presentation.dto.BolsaRequest
import com.hemonorte.presentation.dto.EstoqueStatusResponse
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class EstoqueService(
    private val bolsaSangueRepository: BolsaSangueRepository,
    private val estoqueMinimoRepository: EstoqueMinimoRepository,
) {
    // Lógica de estoque será ajustada em conjunto com o grupo posteriormente
}