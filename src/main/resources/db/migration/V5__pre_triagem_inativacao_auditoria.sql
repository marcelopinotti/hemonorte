ALTER TABLE doador
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE pre_triagem
(
    id               UUID PRIMARY KEY,
    doador_id        UUID          NOT NULL REFERENCES doador (id),
    peso             NUMERIC(5, 2) NOT NULL CHECK (peso > 0),
    tatuagem_recente BOOLEAN       NOT NULL,
    cirurgia_recente BOOLEAN       NOT NULL,
    teve_febre       BOOLEAN       NOT NULL,
    apto             BOOLEAN       NOT NULL,
    motivo_inaptidao VARCHAR(255),
    respondida_em    TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_pre_triagem_doador_respondida ON pre_triagem (doador_id, respondida_em DESC);

CREATE FUNCTION bloquear_alteracao_auditoria() RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION 'historico_auditoria é imutável';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_historico_auditoria_imutavel
    BEFORE UPDATE OR DELETE
    ON historico_auditoria
    FOR EACH ROW
EXECUTE FUNCTION bloquear_alteracao_auditoria();
