package com.hemonorte.service

import com.hemonorte.dto.ComprovanteResponse
import com.hemonorte.repository.AgendamentoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.util.UUID

@Service
class ComprovanteService(private val agendamentoRepository: AgendamentoRepository) {

    @Transactional(readOnly = true)
    fun validar(codigo: UUID): ComprovanteResponse {
        val agendamento = agendamentoRepository.findByCodigoComprovante(codigo)
            ?: throw NoSuchElementException("Comprovante não encontrado")
        return ComprovanteResponse(
            codigo = codigo,
            valido = true,
            doador = agendamento.doador.nome,
            dataDoacao = agendamento.slot.inicio.atZone(FUSO_CLINICO).toLocalDate(),
        )
    }

    private companion object {
        val FUSO_CLINICO: ZoneId = ZoneId.of("America/Fortaleza")
    }
}
