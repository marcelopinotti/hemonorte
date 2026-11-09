# Parâmetros acadêmicos da pré-avaliação de aptidão

## Estado

**Ativos para simulação acadêmica.** Estes valores são fictícios e não constituem orientação médica.

## Política

- gênero `M`: intervalo mínimo de 60 dias e até 4 doações em 365 dias móveis;
- gênero `F`: intervalo mínimo de 90 dias e até 3 doações em 365 dias móveis;
- janela móvel: datas posteriores a `dataReferencia - 365 dias`;
- fuso: `America/Fortaleza`;
- data de referência: data local da avaliação;
- igualdade: a doação é permitida na data calculada de liberação;
- histórico: somente agendamentos `CONCLUIDO`;
- `CANCELADO`, `FALTOU` e `PENDENTE` não contam;
- próxima data: maior data entre a liberação do intervalo e do limite móvel.

Como o modelo atual não registra o instante real da conclusão, a data da doação concluída é a data local do início do slot.

## Exemplo da API

```http
GET /doadores/{id}/aptidao
Authorization: Bearer {token}

HTTP/1.1 200 OK
Content-Type: application/json

{"apto":false,"motivo":"Intervalo mínimo entre doações não cumprido","proximaDataPermitida":"2026-03-02"}
```

A mesma pré-avaliação antecede a reserva. Quando houver impedimento calculável:

```http
POST /agendamentos

HTTP/1.1 422 Unprocessable Content
Content-Type: application/json

{"apto":false,"motivo":"Intervalo mínimo entre doações não cumprido","proximaDataPermitida":"2026-03-02"}
```

A pré-avaliação automatizada não substitui a triagem presencial.
