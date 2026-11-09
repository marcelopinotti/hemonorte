package com.hemonorte.service

import com.hemonorte.model.Doador
import com.hemonorte.model.RegraAptidaoDoacao
import com.hemonorte.model.ResultadoAptidao
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class AptidaoService(
    private val agendamentoRepository: AgendamentoRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun verificarAptidao(doador: Doador): ResultadoAptidao =
        verificarAptidao(doador, LocalDate.now(clock.withZone(FUSO_CLINICO)))

    fun verificarAptidao(doador: Doador, dataReferencia: LocalDate): ResultadoAptidao {
        val historico = agendamentoRepository
            .findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO)
            .map { it.slot.inicio.atZone(FUSO_CLINICO).toLocalDate() }
        return RegraAptidaoDoacao.avaliar(doador.genero, historico, dataReferencia)
    }

    private companion object {
        val FUSO_CLINICO: ZoneId = ZoneId.of("America/Fortaleza")
    }
}
