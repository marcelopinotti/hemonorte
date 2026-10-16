package com.hemonorte.service

import com.hemonorte.model.EstoqueMinimo
import com.hemonorte.repository.EstoqueMinimoRepository
import com.hemonorte.repository.ExameSorologicoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EstoqueService(
    private val estoqueMinimoRepository: EstoqueMinimoRepository,
    private val exameSorologicoRepository: ExameSorologicoRepository
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

    fun listarEstoquesCriticos(): List<EstoqueMinimo> {
        // Aqui podemos listar os itens que estão abaixo do limite crítico comparando com as bolsas aprovadas disponíveis
        val todosMinimos = estoqueMinimoRepository.findAll()
        return todosMinimos.filter { minimo ->
            val quantidadeAtual = calcularBolsasAprovadasPorTipagem(minimo.tipagem)
            quantidadeAtual <= minimo.quantidadeCritica
        }
    }

    private fun calcularBolsasAprovadasPorTipagem(tipagem: String): Int {
        // Lógica para contar bolsas aprovadas com base nos exames sorológicos e coleta
        // Retorna a contagem atual no estoque
        return 0 // Ajustaremos conforme a consulta exata das bolsas aprovadas
    }
}