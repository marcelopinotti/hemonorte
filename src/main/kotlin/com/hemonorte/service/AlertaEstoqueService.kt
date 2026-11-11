package com.hemonorte.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class AlertaEstoqueService(
    private val estoqueService: EstoqueService,
    meterRegistry: MeterRegistry,
) {
    private val logger = LoggerFactory.getLogger(AlertaEstoqueService::class.java)
    private val disponiveis = EstoqueService.TIPAGENS.associateWith { AtomicLong() }
    private val criticos = EstoqueService.TIPAGENS.associateWith { AtomicLong() }

    init {
        EstoqueService.TIPAGENS.forEach { tipagem ->
            Gauge.builder("hemonorte.estoque.disponivel", disponiveis.getValue(tipagem)) { it.toDouble() }
                .tag("tipagem", tipagem)
                .register(meterRegistry)
            Gauge.builder("hemonorte.estoque.critico", criticos.getValue(tipagem)) { it.toDouble() }
                .tag("tipagem", tipagem)
                .register(meterRegistry)
        }
    }

    @Scheduled(fixedDelayString = "\${hemonorte.alerta-estoque.intervalo:PT15M}")
    fun verificar(): List<EstoqueCritico> {
        estoqueService.disponiveisPorTipagem().forEach { (tipagem, total) -> disponiveis[tipagem]?.set(total) }
        val alertas = estoqueService.listarEstoquesCriticos()
        val tipagensCriticas = alertas.map { it.tipagem }.toSet()
        criticos.forEach { (tipagem, valor) -> valor.set(if (tipagem in tipagensCriticas) 1 else 0) }
        alertas.forEach {
            logger.warn(
                "ALERTA estoque crítico: tipagem={} disponiveis={} minimo={}",
                it.tipagem, it.disponiveis, it.quantidadeCritica,
            )
        }
        return alertas
    }
}
