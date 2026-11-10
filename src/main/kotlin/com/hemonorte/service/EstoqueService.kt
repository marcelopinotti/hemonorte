package com.hemonorte.service

import com.hemonorte.model.EstoqueMinimo
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.EstoqueMinimoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class EstoqueService(
    private val estoqueMinimoRepository: EstoqueMinimoRepository,
    private val bolsaSangueRepository: BolsaSangueRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    @Transactional
    fun definirEstoqueMinimo(tipagem: String, quantidadeCritica: Int): EstoqueMinimo {
        val estoqueMinimo = estoqueMinimoRepository.findById(tipagem)
            .map { 
                it.atualizarQuantidade(quantidadeCritica)
                it
            }
            .orElseGet { EstoqueMinimo(tipagem, quantidadeCritica) }

        return estoqueMinimoRepository.save(estoqueMinimo)
    }

    @Transactional(readOnly = true)
    fun disponiveisPorTipagem(): Map<String, Long> {
        val contagem = bolsaSangueRepository.contarDisponiveisPorTipagem(LocalDate.now(clock))
            .associate { (tipagem, total) -> tipagem as String to total as Long }
        return TIPAGENS.associateWith { contagem[it] ?: 0L }
    }

    @Transactional(readOnly = true)
    fun listarEstoquesCriticos(): List<EstoqueMinimo> {
        val disponiveis = disponiveisPorTipagem()
        return estoqueMinimoRepository.findAll().filter { minimo ->
            (disponiveis[minimo.tipagem] ?: 0L) <= minimo.quantidadeCritica
        }
    }

    private companion object {
        val TIPAGENS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    }
}
