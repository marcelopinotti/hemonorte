package com.hemonorte.service

import com.hemonorte.model.ExameSorologico
import com.hemonorte.model.enums.ResultadoSorologico
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.ExameSorologicoRepository
import com.hemonorte.repository.FuncionarioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExameSorologicoService(
    private val exameSorologicoRepository: ExameSorologicoRepository,
    private val bolsaSangueRepository: BolsaSangueRepository,
    private val funcionarioRepository: FuncionarioRepository
) {

    @Transactional
    fun registrarExame(
        bolsaId: UUID,
        funcionarioId: UUID,
        hiv: ResultadoSorologico,
        hepatite: ResultadoSorologico,
        chagas: ResultadoSorologico
    ): ExameSorologico {
        val bolsa = bolsaSangueRepository.findById(bolsaId)
            .orElseThrow { IllegalArgumentException("Bolsa de sangue não encontrada") }

        val funcionario = funcionarioRepository.findById(funcionarioId)
            .orElseThrow { IllegalArgumentException("Funcionário não encontrado") }

        require(!exameSorologicoRepository.existsByBolsaId(bolsaId)) {
            "Já existe um exame sorológico registrado para esta bolsa."
        }

        val exame = ExameSorologico(
            bolsa = bolsa,
            funcionario = funcionario,
            hiv = hiv,
            hepatite = hepatite,
            chagas = chagas
        )

        return exameSorologicoRepository.save(exame)
    }

    fun buscarPorBolsa(bolsaId: UUID): ExameSorologico {
        return exameSorologicoRepository.findByBolsaId(bolsaId)
            .orElseThrow { IllegalArgumentException("Exame sorológico não encontrado para esta bolsa") }
    }
}