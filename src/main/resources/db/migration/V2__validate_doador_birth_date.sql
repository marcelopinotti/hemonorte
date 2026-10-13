ALTER TABLE doador
    ADD CONSTRAINT ck_doador_data_nascimento_nao_futura
        CHECK (data_nascimento <= CURRENT_DATE) NOT VALID;

ALTER TABLE doador
    VALIDATE CONSTRAINT ck_doador_data_nascimento_nao_futura;
