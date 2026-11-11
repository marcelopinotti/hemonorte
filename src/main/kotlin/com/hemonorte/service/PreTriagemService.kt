package com.hemonorte.service

import com.hemonorte.model.PreTriagem
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.PreTriagemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID

@Service
class PreTriagemService(
    private val preTriagemRepository: PreTriagemRepository,
    private val doadorRepository: DoadorRepository,
    private val clock: Clock,
) {

    @Transactional
    fun responder(
        doadorId: UUID,
        peso: BigDecimal,
        tatuagemRecente: Boolean,
        cirurgiaRecente: Boolean,
        teveFebre: Boolean,
    ): PreTriagem {
        val doador = doadorRepository.findById(doadorId)
            .orElseThrow { NoSuchElementException("Doador não encontrado") }
        return preTriagemRepository.save(
            PreTriagem(doador, peso, tatuagemRecente, cirurgiaRecente, teveFebre, clock.instant()),
        )
    }

    @Transactional(readOnly = true)
    fun ultima(doadorId: UUID): PreTriagem =
        preTriagemRepository.findFirstByDoadorIdOrderByRespondidaEmDesc(doadorId)
            ?: throw NoSuchElementException("Nenhuma pré-triagem respondida")
}
