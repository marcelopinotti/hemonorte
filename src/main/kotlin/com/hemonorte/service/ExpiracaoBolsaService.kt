package com.hemonorte.service

import com.hemonorte.model.enums.StatusBolsa
import com.hemonorte.repository.BolsaSangueRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ExpiracaoBolsaService(
    private val bolsaSangueRepository: BolsaSangueRepository
) {
    private val logger = LoggerFactory.getLogger(ExpiracaoBolsaService::class.java)

    // Executa todo dia à meia-noite (cron: segundo, minuto, hora, dia, mês, dia da semana)
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun processarExpiracaoBolsas() {
        logger.info("Iniciando rotina de verificação de bolsas vencidas...")
        
        val hoje = LocalDate.now()
        // Aqui buscamos as bolsas que estão ativas/disponíveis mas cuja validade já passou
        val bolsasVencidas = bolsaSangueRepository.findByDataValidadeBeforeAndStatus(
            hoje,
            StatusBolsa.DISPONIVEL,
        )

        for (bolsa in bolsasVencidas) {
            // Realiza a transição para expirada e registra na auditoria
            bolsa.transicionarPara(StatusBolsa.VENCIDA)
            bolsaSangueRepository.save(bolsa)
            logger.info("Bolsa ID: ${bolsa.id} marcada como vencida.")
        }

        logger.info("Rotina de expiração concluída. Total processado: ${bolsasVencidas.size}")
    }
}
