ALTER TABLE slot_agenda
    ADD CONSTRAINT slot_agenda_capacidade_positiva
        CHECK (capacidade > 0);

CREATE INDEX idx_agendamento_slot_status
    ON agendamento (slot_id, status);
