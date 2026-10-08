-- V1 - Schema inicial do Hemonorte


CREATE TABLE doador
(
    id              UUID PRIMARY KEY,
    cpf             VARCHAR(11)  NOT NULL UNIQUE,
    nome            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    senha_hash      VARCHAR(255) NOT NULL,
    tipagem         VARCHAR(3)   NOT NULL,
    data_nascimento DATE         NOT NULL,
    genero          CHAR(1)      NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


CREATE TABLE slot_agenda
(
    id         UUID PRIMARY KEY,
    inicio     TIMESTAMPTZ NOT NULL UNIQUE,
    capacidade INT         NOT NULL
);


CREATE TABLE agendamento
(
    id                 UUID PRIMARY KEY,
    doador_id          UUID        NOT NULL REFERENCES doador (id),
    slot_id            UUID        NOT NULL REFERENCES slot_agenda (id),
    status             VARCHAR(20) NOT NULL,
    codigo_comprovante UUID UNIQUE,
    version            INT         NOT NULL DEFAULT 0
);


CREATE TABLE funcionario
(
    id         UUID PRIMARY KEY,
    matricula  VARCHAR(50)  NOT NULL UNIQUE,
    nome       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil     VARCHAR(20)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE
);


CREATE TABLE bolsa_sangue
(
    id             UUID PRIMARY KEY,
    agendamento_id UUID        NOT NULL UNIQUE REFERENCES agendamento (id),
    tipagem        VARCHAR(3)  NOT NULL,
    data_coleta    DATE        NOT NULL,
    data_validade  DATE        NOT NULL,
    status         VARCHAR(20) NOT NULL
);


CREATE TABLE triagem
(
    id               UUID PRIMARY KEY,
    agendamento_id   UUID          NOT NULL UNIQUE REFERENCES agendamento (id),
    funcionario_id   UUID          NOT NULL REFERENCES funcionario (id),
    peso             DECIMAL(5, 2) NOT NULL,
    tatuagem_recente BOOLEAN       NOT NULL,
    cirurgia_recente BOOLEAN       NOT NULL,
    teve_febre       BOOLEAN       NOT NULL,
    apto             BOOLEAN       NOT NULL,
    motivo_inaptidao VARCHAR(255)
);


CREATE TABLE exame_sorologico
(
    id              UUID PRIMARY KEY,
    bolsa_id        UUID        NOT NULL UNIQUE REFERENCES bolsa_sangue (id),
    funcionario_id  UUID        NOT NULL REFERENCES funcionario (id),
    hiv             VARCHAR(8)  NOT NULL,
    hepatite        VARCHAR(8)  NOT NULL,
    chagas          VARCHAR(8)  NOT NULL,
    resultado_final VARCHAR(10) NOT NULL,
    data_resultado  TIMESTAMPTZ NOT NULL
);


CREATE TABLE historico_auditoria
(
    id              UUID PRIMARY KEY,
    bolsa_id        UUID        NOT NULL REFERENCES bolsa_sangue (id),
    funcionario_id  UUID REFERENCES funcionario (id), -- NULLABLE = sistema (cron)
    status_anterior VARCHAR(20) NOT NULL,
    status_novo     VARCHAR(20) NOT NULL,
    data_alteracao  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    motivo          VARCHAR(255)
);


CREATE TABLE estoque_minimo
(
    tipagem            VARCHAR(3) PRIMARY KEY,
    quantidade_critica INT NOT NULL
);
