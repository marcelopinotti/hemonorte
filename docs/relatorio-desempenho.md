# Relatório de desempenho — RNF03

**Exigência:** consulta de vagas e agendamento respondem em menos de 500 ms (p95).

## Como é medido

Teste automatizado `DesempenhoIntegrationTest` (`@Tag("integration")`), executado no CI a cada push/PR:

```bash
./mvnw test -Dtest=DesempenhoIntegrationTest -Dgroups=integration
```

- banco: PostgreSQL 16 real (Testcontainers), migrations Flyway aplicadas;
- requisições pela pilha completa do Spring (MockMvc): filtro JWT, segurança, controller, service, JPA e banco; não inclui a rede HTTP;
- massa: 70 horários (7 dias × 10), capacidade 20 cada, 300 reservas pendentes já existentes e 400 doadores com pré-triagem apta;
- `GET /slots` com janela de 7 dias: 20 chamadas de aquecimento descartadas e 200 medidas;
- `POST /agendamentos`: 100 reservas de doadores diferentes, com aptidão, pré-triagem, lock do horário e gravação;
- o teste falha se qualquer p95 for ≥ 500 ms.

## Resultado (10/11/2026)

Ambiente: WSL2, 16 CPUs, 7 GB de RAM, Docker 29.9.2, JDK 21.

| Operação | Amostras | p50 | p95 | Máximo |
|---|---|---|---|---|
| `GET /slots` (7 dias, 70 horários) | 200 | 22 ms | 31 ms | 53 ms |
| `POST /agendamentos` | 100 | 7 ms | 9 ms | 67 ms |

Os dois p95 ficam bem abaixo de 500 ms. Os números variam conforme a máquina; o valor atualizado aparece no log do teste, na linha `RNF03 desempenho`.

## Monitoramento em execução

A aplicação publica histograma de latência e SLO de 500 ms para `http.server.requests` em `/actuator/prometheus`. No Prometheus, o p95 por rota sai de:

```promql
histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket[5m])))
```
