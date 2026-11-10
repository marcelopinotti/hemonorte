package com.hemonorte.service

import com.hemonorte.exception.ConflitoException
import com.hemonorte.model.ExameSorologico
import com.hemonorte.model.enums.ResultadoFinal
import com.hemonorte.model.enums.ResultadoSorologico
import com.hemonorte.model.enums.StatusBolsa
import com.hemonorte.repository.BolsaSangueRepository
import com.hemonorte.repository.ExameSorologicoRepository
import com.hemonorte.repository.FuncionarioRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class ExameSorologicoService(
    private val exameSorologicoRepository: ExameSorologicoRepository,
    private val bolsaSangueRepository: BolsaSangueRepository,
    private val funcionarioRepository: FuncionarioRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    @Transactional
    fun registrarExame(
        bolsaId: UUID,
        funcionarioId: UUID,
        hiv: ResultadoSorologico,
        hepatite: ResultadoSorologico,
        chagas: ResultadoSorologico
    ): ExameSorologico {
        val funcionario = funcionarioRepository.findById(funcionarioId)
            .orElseThrow { AccessDeniedException("Acesso negado") }
        if (!funcionario.ativo) throw AccessDeniedException("Acesso negado")

        val bolsa = bolsaSangueRepository.findById(bolsaId)
            .orElseThrow { NoSuchElementException("Bolsa de sangue não encontrada") }

        if (exameSorologicoRepository.existsByBolsaId(bolsaId)) {
            throw ConflitoException("Já existe um exame sorológico registrado para esta bolsa.")
        }
        if (bolsa.status != StatusBolsa.EM_ANALISE) {
            throw ConflitoException("Exame exige bolsa EM_ANALISE; status atual: ${bolsa.status}")
        }

        val exame = ExameSorologico(
            bolsa = bolsa,
            funcionario = funcionario,
            hiv = hiv,
            hepatite = hepatite,
            chagas = chagas
        )

        val vencida = bolsa.dataValidade.isBefore(LocalDate.now(clock))
        bolsa.transicionarPara(
            if (exame.resultadoFinal == ResultadoFinal.APROVADO && !vencida) StatusBolsa.DISPONIVEL
            else StatusBolsa.DESCARTADA,
        )

        return exameSorologicoRepository.saveAndFlush(exame)
    }

    @Transactional(readOnly = true)
    fun buscarPorBolsa(bolsaId: UUID): ExameSorologico {
        return exameSorologicoRepository.findByBolsaId(bolsaId)
            .orElseThrow { NoSuchElementException("Exame sorológico não encontrado para esta bolsa") }
    }

    @Transactional(readOnly = true)
    fun listar(): List<ExameSorologico> = exameSorologicoRepository.findAll()

    @Transactional(readOnly = true)
    fun buscar(id: UUID): ExameSorologico = exameSorologicoRepository.findById(id)
        .orElseThrow { NoSuchElementException("Exame sorológico não encontrado") }
}
