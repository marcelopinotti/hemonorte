package com.hemonorte.service

import com.hemonorte.model.Doador
import com.hemonorte.model.enums.StatusAgendamento
import com.hemonorte.repository.AgendamentoRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period
import java.util.UUID

@Service
class AptidaoService(
    private val agendamentoRepository: AgendamentoRepository
) {

    // Regra paramétrica (exemplo padrão: 60 dias para homens, 90 dias para mulheres)
    // Conforme a issue, isso pode ser ajustado conforme validação com os responsáveis.
    fun verificarAptidao(doador: Doador, dataReferencia: LocalDate = LocalDate.now()): ResultadoAptidao {
        // Busca os agendamentos concluídos do doador (cancelamentos e faltas são ignorados)
        val historicoConcluido = agendamentoRepository.findByDoadorIdAndStatus(doador.id, StatusAgendamento.CONCLUIDO)

        if (historicoConcluido.isEmpty()) {
            return ResultadoAptidao(apto = true, motivo = null, proximaDataPermitida = null)
        }

        // Pega a última doação concluída (ordenada pela data do slot/agendamento)
        // Supondo que tenhamos como comparar pelas datas dos slots associados
        val ultimaDoacao = historicoConcluido.maxByOrNull { it.slot.inicio }
        
        if (ultimaDoacao != null) {
            val dataUltimaDoacao = ultimaDoacao.slot.inicio.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            
            // Intervalo exigido com base no gênero (ex: Homem = 60 dias, Mulher = 90 dias)
            val diasIntervaloMinimo = if (doador.genero.uppercaseChar() == 'M') 60 else 90
            val proximaDataPermitida = dataUltimaDoacao.plusDays(diasIntervaloMinimo.toLong())

            if (dataReferencia.isBefore(proximaDataPermitida)) {
                return ResultadoAptidao(
                    apto = false,
                    motivo = "Intervalo mínimo entre doações não respeitado.",
                    proximaDataPermitida = proximaDataPermitida
                )
            }
        }

        return ResultadoAptidao(apto = true, motivo = null, proximaDataPermitida = null)
    }
}

data class ResultadoAptidao(
    val apto: Boolean,
    val motivo: String?,
    val proximaDataPermitida: LocalDate?
)