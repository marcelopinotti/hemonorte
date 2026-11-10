CREATE UNIQUE INDEX uk_agendamento_doador_slot_pendente
    ON agendamento (doador_id, slot_id)
    WHERE status = 'PENDENTE';
