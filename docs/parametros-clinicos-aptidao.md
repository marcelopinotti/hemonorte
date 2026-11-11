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

## Triagem presencial

`POST /triagens` recebe peso e indicadores; `apto` e `motivoInaptidao` são calculados pela entidade `Triagem` e qualquer valor enviado pelo cliente é ignorado.

- peso mínimo: 50 kg, inclusivo; peso deve ser positivo, com até 3 dígitos inteiros e 2 decimais;
- impedimentos: tatuagem recente, cirurgia recente e febre recente, informados como booleanos pelo funcionário;
- “recente” ainda não tem janela temporal definida no sistema: o julgamento é do profissional de triagem. Qualquer definição (ex.: meses desde a tatuagem ou cirurgia) deve ser validada com os responsáveis clínicos antes de alterar ou ampliar os critérios;
- triagem inapta impede a coleta: `POST /coletas` exige triagem apta e não gera bolsa nem comprovante.

## Pré-triagem digital

O doador responde `POST /api/doador/me/pre-triagem` (peso e indicadores de tatuagem, cirurgia e febre recentes) antes de agendar. As perguntas estão em `GET /api/doador/me/pre-triagem/perguntas`.

- usa a mesma regra da triagem presencial (`RegraTriagem`): peso mínimo de 50 kg e os três impedimentos;
- a resposta é gravada e devolvida com cada pergunta, a resposta dada, `apto` e `motivoInaptidao`;
- validade: 7 dias a partir da resposta (`PreTriagem.VALIDADE`);
- `POST /agendamentos` usa a última pré-triagem do doador: ausente ou vencida → 422 "Responda a pré-triagem antes de agendar"; inapta → 422 "Pré-triagem inapta: <motivo>";
- a verificação do intervalo entre doações acontece antes e continua informando `proximaDataPermitida`;
- a pré-triagem não substitui a triagem presencial.
