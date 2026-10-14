package com.hemonorte.infra

import com.hemonorte.domain.SlotAgenda
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SlotAgendaRepository : JpaRepository<SlotAgenda, UUID> {
}