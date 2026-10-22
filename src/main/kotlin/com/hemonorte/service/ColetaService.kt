package com.hemonorte.service

import com.hemonorte.model.Coleta
import com.hemonorte.repository.ColetaRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.repository.TriagemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class ColetaService(
    private val coletaRepository: ColetaRepository,
    private val triagemRepository: TriagemRepository,
    private val funcionarioRepository: FuncionarioRepository
) {

    @Transactional
    fun registrarColeta(
        triagemId: UUID,
        funcionarioId: UUID,
        volumeMl: BigDecimal
    ): Coleta {
        val triagem = triagemRepository.findById(triagemId)
            .orElseThrow { IllegalArgumentException("Triagem não encontrada") }

        val funcionario = funcionarioRepository.findById(funcionarioId)
            .orElseThrow { IllegalArgumentException("Funcionário não encontrado") }

        require(triagem.apto) {
            "Impossível realizar coleta: o doador não foi aprovado na triagem clínica."
        }

        require(!coletaRepository.existsByTriagemId(triagemId)) {
            "Já existe uma coleta registrada para esta triagem."
        }

        val coleta = Coleta(
            triagem = triagem,
            funcionario = funcionario,
            volumeMl = volumeMl
        )

        return coletaRepository.save(coleta)
    }

    fun buscarPorTriagem(triagemId: UUID): Coleta {
        return coletaRepository.findByTriagemId(triagemId)
            .orElseThrow { IllegalArgumentException("Coleta não encontrada para esta triagem.") }
    }
}