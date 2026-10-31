# Relatório de QA — Hemonorte

Atualização de 2026-10-30, sobre a `develop` @ `75bfcd4` (18 commits de desenvolvimento posteriores ao PR #30) e a nova leva da branch `QA`. Documento vivo: defeitos corrigidos e verificados em execução saem da lista ativa; regressões mantêm o ID original; achados novos recebem o próximo ID estável.

## 1. Resumo executivo

A rodada cumpriu o objetivo de validar a base de persistência e os cadastros com PostgreSQL real. A suíte saiu de **120 para 135 testes**, todos verdes no `clean verify` final: **+15 provas**, das quais 14 usam Testcontainers/PostgreSQL 16 e uma é uma sentinela de contrato HTTP.

As novas evidências cobrem:

- Flyway V1→V2 em banco real, banco vazio, schema validado pelo Hibernate e preservação de registros existentes;
- criação, consulta, atualização e desativação de doadores/funcionários, recurso inexistente e preservação de vínculo histórico;
- CPF imutável, nascimento futuro, duplicidade sequencial e corrida real de CPF, e-mail e matrícula;
- BCrypt real no servidor, salts distintos, troca de senha, atualização cadastral sem reenvio de senha e ausência de hash nas respostas;
- contratos HTTP 201/204/400/404/409 no fluxo real e três defeitos vivos preservados como sentinelas.

O baseline de `75bfcd4` também passou no CI remoto (`clean verify`, run `40220462421`), confirmando que as correções dos developers não quebraram a suíte existente.

A auditoria dos 18 commits encontrou três riscos que exigem decisão rápida: as novas rotas de troca de senha e alteração de perfil permanecem sem autenticação/autorização (regressão do **QA-001**), e o handler de regra de negócio ainda pode serializar erro com HTTP 200 (família do **QA-044**). O novo **QA-045** registra que uma violação de limite dimensional no banco é convertida em falso conflito 409.

## 2. Inventário técnico

| Item | Valor |
|---|---|
| Linguagem | Kotlin 2.3.21 |
| Java | alvo 17 no projeto; execução com Temurin 21.0.12.1 |
| Framework | Spring Boot 4.1.1 |
| Build | Maven 3.9.16 via wrapper 3.3.4 |
| Testes | JUnit Jupiter 6.0.3, kotlin.test, Mockito 5.23.0, MockMvc standalone + MockMvc com contexto real |
| Banco de integração | PostgreSQL 16 Alpine via Testcontainers |
| Migrações | Flyway V1→V2; JPA com `ddl-auto: validate` |
| Cobertura | JaCoCo 0.8.15 |
| CI | GitHub Actions executa `./mvnw -B -ntp clean verify` e publica JUnit/JaCoCo |
| Branch de trabalho | `QA`, base `develop` @ `75bfcd4` |

## 3. Método e condições de execução

1. A branch existente `QA` foi alinhada à `develop` atual, sem criar branch nova e sem reescrever histórico.
2. Os 18 commits de desenvolvimento foram lidos antes da alteração de testes.
3. O baseline remoto de `75bfcd4` foi confirmado como verde.
4. Na máquina local, o build completo foi executado em runner Linux com Maven 3.9.16, Temurin 21.0.12.1, Docker/WSL e PostgreSQL via Testcontainers. O agente do Mockito foi pré-carregado no runner para contornar a restrição de self-attach do contêiner; nenhuma configuração do projeto foi alterada por isso.
5. O comando final foi `./mvnw -B -ntp clean verify`, com JaCoCo limpo e PostgreSQL 16 real.
6. Nenhum teste foi ignorado, desativado ou reenquadrado para produzir verde.

## 4. Nova leva de testes

| Classe | Testes | Evidência |
|---|---:|---|
| `CadastroPersistenciaIntegrationTest` | 13 | migrações, JPA/constraint, CRUD, hash/salt, senha, concorrência de unicidade, vínculos históricos, desativação, 400/404/409 e sentinela dimensional |
| `AutorizacaoCadastroIntegrationTest` | 1 | sentinela executável do QA-001: sem credencial, troca de senha e autoelevação para ADMIN são aceitas |
| `ApiExceptionHandlerTest` | 1 | sentinela executável do QA-044: `RegraNegocioException` responde 200 |
| **Total novo** | **15** | **15 verdes** |

### 4.1 Migração e integridade do schema

- O contexto real sobe com Flyway e Hibernate em modo `validate`.
- As versões V1 e V2 aparecem como aplicadas e bem-sucedidas.
- Um schema isolado é migrado até V1, recebe um doador e é migrado até V2; o registro permanece e a constraint de nascimento futuro fica validada.
- A constraint de banco rejeita nascimento futuro mesmo quando a validação HTTP é contornada.

### 4.2 Cadastros e senhas

- O doador e o funcionário são persistidos com BCrypt real; o valor recebido em `senha` nunca é gravado.
- Duas senhas iguais geram hashes diferentes, comprovando salt por cadastro.
- Atualizar cadastro não exige reenvio de senha e preserva CPF e hash.
- Trocar senha gera novo hash; a senha anterior deixa de validar.
- DELETE de funcionário desativa e preserva a linha; DELETE de doador com agendamento devolve 409 e preserva doador e histórico.
- Respostas de cadastro, consulta, atualização e agendamento não contêm senha nem hash.

### 4.3 Unicidade e concorrência

Foram executadas requisições HTTP simultâneas, com largada sincronizada por barreira e sem `sleep`:

| Cenário | Resultado observado |
|---|---|
| Mesmo CPF, e-mails diferentes | exatamente 201 + 409; uma linha persistida |
| CPF diferentes, mesmo e-mail | exatamente 201 + 409; uma linha persistida |
| Mesma matrícula, e-mails diferentes | exatamente 201 + 409; uma linha persistida |
| Matrículas diferentes, mesmo e-mail | exatamente 201 + 409; uma linha persistida |

A constraint `UNIQUE` do PostgreSQL é a última linha de defesa; a implementação atual combina pré-checagem e captura de `DataIntegrityViolationException`.

## 5. Achados corrigidos e verificados nesta rodada

Saem da lista de defeitos ativos:

- **QA-026/QA-032 — hash sob responsabilidade do cliente e atualização que exigia reenvio de senha:** obsoletos para novas gravações; o hash passou para o servidor e a atualização cadastral funciona sem reenvio de senha, conforme a #15.
- **QA-034 — BCrypt calculado e descartado:** corrigido para doadores e funcionários; a nova integração prova hash real, salt distinto, troca de senha e ausência do valor bruto.
- **QA-035 — JPQL malformada na expiração:** corrigido para query derivada com `LocalDate`; o contexto PostgreSQL sobe e o CI remoto passa.
- **QA-038 — CI compilava sem executar testes:** corrigido; o CI executa `clean verify` e publica JUnit/JaCoCo.
- **QA-043 — hash exposto em `/agendamentos`:** corrigido com `@JsonIgnore`; a prova de contrato continua verde com `doesNotExist`.
- **QA-027/QA-028 — conflitos de duplicidade:** o caminho 409 deixou de ser apenas de handler; corrida real de CPF/e-mail/matrícula está provada no PostgreSQL.
- **QA-033 — transação ausente em `FuncionarioService`:** corrigido; o service está transacional e os fluxos de cadastrar, atualizar, alterar senha e desativar foram executados no banco real.

Observação residual do QA-034: a correção vale para novas gravações. Não há estratégia documentada para hashes legados já persistidos nem política de rehash no primeiro login; isso deve ser tratado junto da implementação de autenticação.

## 6. Achados ativos

### QA-001 (crítico) — dados e operações sensíveis sem autenticação/autorização

A issue de origem #7 e a frente de testes #16 continuam sem implementação: não há `SecurityFilterChain`, JWT ou matriz de perfis. A rodada encontrou uma regressão material: as novas rotas permitem, sem token ou senha atual:

- trocar a senha de qualquer doador/funcionário apenas com o UUID;
- alterar o perfil de qualquer funcionário, inclusive para `ADMIN`.

A sentinela `SENTINELA QA-001` em `AutorizacaoCadastroIntegrationTest` comprova em PostgreSQL que o perfil persistido muda para `ADMIN` e que a senha troca sem credencial. Impacto: tomada de conta, autoelevação e exposição LGPD. Correção recomendada: implementar a cadeia real da #7/#16 e exigir 401 sem token, 403 fora do papel e política explícita para reset de senha.

### QA-037 (alto) — capacidade, duplicidade e validade do slot

`AgendamentoService` continua com o padrão conta-então-insere (`service/AgendamentoService.kt:35-42`): duas transações podem observar a mesma vaga antes de inserir. Também não impede reserva ativa duplicada do mesmo doador nem revalida o início do slot no momento da reserva. A sentinela unitária da reserva duplicada permanece. A prova determinística da última vaga em PostgreSQL entra assim que houver proteção transacional/constraint apropriada.

### QA-039/QA-040 (alto) — fluxo de coleta/auditoria removido ou incompleto; sorologia não transiciona

O commit `e1552b7` removeu controller, service, entidade e repository de coleta para destravar o boot, mas a V1 nunca criou a tabela correspondente. O resultado é que `POST /coletas` responde 404 e a jornada triagem→coleta→bolsa→comprovante→auditoria não existe. A auditoria segue sem emissor/rePOSITório; o exame sorológico salva resultado sem mover a bolsa. A correção exige migração aditiva e operação atômica, não a ressurreição do stub isolado.

### QA-036 (alto) — contagem de estoque sempre zero

`EstoqueService.calcularBolsasAprovadasPorTipagem()` continua retornando `0`. O teste unitário adicionado pelo developer passa apenas porque `0 <= limite`, criando falsa confiança. A agregação real de bolsas `DISPONIVEL` dentro da validade continua pendente.

### QA-010/QA-003 (alto) — expiração executável, porém sem auditoria, lock ou fuso explícito

A query da expiração foi corrigida, mas o serviço carrega `DISPONIVEL`, transiciona e salva sem `@Version`, lock ou update condicional. Uma disputa com utilização/descarte pode sobrescrever o estado terminal. O comentário de que a operação registra auditoria não corresponde ao código: não há emissor de `HistoricoAuditoria`; uma falha de auditoria também não reverteria a transição. `LocalDate.now()` usa o fuso do host, sem `Clock`/fuso aprovado.

### QA-044 (médio) — contratos 422/409 ausentes e regra de negócio pode responder 200

O handler mapeia `IllegalArgumentException` para 400; inaptidão deveria ser 422 e lotação/conflito deveria ser 409. Além disso, `RegraNegocioException` é tratada sem `@ResponseStatus`, portanto um erro serializado por esse handler sai com HTTP 200. Há três sentinelas: inaptidão 400, lotação 400 e regra de negócio 200. Quando o mapeamento tipado chegar, elas viram as provas 422/409/422.

### QA-045 (novo, médio) — violação dimensional vira falso conflito 409

Os services capturam `DataIntegrityViolationException` de forma ampla e transformam qualquer violação em “CPF/e-mail/matrícula já cadastrado”. O critério literal da #15 pede 400 para entrada inválida e 409 apenas para duplicidade. Em execução real, um nome acima de `VARCHAR(255)` passa Bean Validation, viola a dimensão no PostgreSQL e recebe 409 com mensagem de duplicidade. A sentinela `SENTINELA QA-045` fixa o comportamento atual. Correção: limitar campos com `@Size(max = 255)` e mapear constraint de unicidade separadamente das demais violações.

### QA-029 (médio) — configuração expõe banco e API sem proteção

A configuração da aplicação usa credencial fixa do PostgreSQL; o Compose publica banco e API em todas as interfaces e o Grafana não recebe senha configurada. O README também diverge da porta publicada. O achado antigo permanece e ganhou impacto maior com as rotas de escrita sem autorização.

### Demais itens sem mudança

Permanecem sob acompanhamento ou decisão de negócio: QA-003 (emissor de auditoria), QA-007 (dígito verificador de CPF), QA-010 (políticas de bolsa e expiração), QA-014 (relógios/fusos), QA-016 (sorologia indeterminada), QA-041 (volume negativo), QA-042 (políticas de exclusão divergentes) e as divergências funcionais já registradas nas issues #8–#14.

## 7. Estado do marco M3

| Issue | Veredito em 2026-10-30 |
|---|---|
| **#15** persistência, migrações e cadastros | **Avançada, ainda aberta:** migração incremental, schema JPA, CRUD, histórico, bcrypt/salt, duplicidade e corrida real estão provados; falta fechar os contratos 422, o mapeamento do erro dimensional e a política de hashes legados |
| **#16** autenticação e autorização | **Não atendida:** sem JWT/filtros; a sentinela do QA-001 registra a regressão de escrita sem credencial |
| **#17** horários, aptidão e concorrência | **Parcial:** regras unitárias existem; faltam concorrência determinística da última vaga, duplicidade, revalidação temporal e 422/409 |
| **#18** triagem, coleta e auditoria | **Não atendida:** superfície de coleta removida e fluxo atômico/auditoria inexistente |
| **#19** sorologia, estoque, expiração e jornada | **Não atendida:** estoque zero, sorologia sem transição, expiração sem auditoria/concorrência/fuso e sem jornada integrada |

As cinco issues continuam abertas no GitHub e nenhum critério de aceite foi marcado pelo time. O QA não marca critérios nas issues do marco; o veredito acima é evidência de execução.

## 8. Resultado da suíte

| Bloco | Testes | Resultado |
|---|---:|---|
| Domínio/modelo | 75 | 75 verdes |
| Bean Validation de DTO | 7 | 7 verdes |
| Contratos/controllers | 17 | 17 verdes |
| Services | 21 | 21 verdes |
| Integração PostgreSQL/Testcontainers | 15 | 15 verdes |
| **Total** | **135** | **135 verdes, 0 falhas, 0 erros, 0 ignorados** |

## 9. JaCoCo

Números desta execução: **68,8% das linhas (534/776), 68,7% das instruções (2682/3906) e 68,4% dos branches (141/206)**.

| Pacote | Linhas | Instruções | Branches |
|---|---:|---:|---:|
| Domínio (model) | 69,8% (206/295) | 73,8% | 73,9% (102/138) |
| Enums de domínio | 100% (17/17) | 100% | — |
| DTOs | 61,0% (86/141) | 60,6% | — |
| Controllers | 62,4% (63/101) | 53,0% | 40,0% (4/10) |
| Services | 73,1% (158/216) | 65,8% | 60,3% (35/58) |
| Exceções | 100% (2/2) | 100% | — |
| **Projeto** | **68,8% (534/776)** | **68,7%** | **68,4%** |

A evolução contra 2026-10-23 é **334→534 linhas cobertas** e **44,1%→68,8%**, enquanto o denominador cresceu de 757 para 776 por causa do código novo dos developers. Ou seja, a melhora não veio de reduzir a base: foram **+200 linhas de produção cobertas**, com `DoadorService` em 39/44 linhas, `FuncionarioService` em 38/41 e os controllers de cadastro em 23/25 e 10/10. As áreas ainda zeradas ou baixas continuam correspondendo aos bloqueios de fluxo: auditoria 0/17, triagem 5/23, expiração 4/15 e DTOs clínicos ainda sem integração HTTP.

## 10. Riscos e decisões pendentes

1. **QA-001 é prioridade de segurança:** escrita de senha/perfil sem autenticação deve ser bloqueada antes da exposição do Compose.
2. **QA-037:** a próxima prova de concorrência da agenda só deve ser escrita junto de uma estratégia transacional/constraint no produto.
3. **QA-039/QA-040:** não recriar a tabela de coleta sem antes definir a operação atômica e a auditoria.
4. **QA-010/QA-003:** clock/fuso, política de `EM_ANALISE` e emissor de auditoria precisam de decisão antes dos testes de expiração.
5. **QA-044/QA-045:**tipar regras de negócio e separar “entrada inválida” de “duplicidade”.
6. **QA-029:** não publicar API/banco/Grafana em todas as interfaces; usar loopback/rede privada e segredos de ambiente.
7. **QA-007/QA-014/QA-016:** não inventar regra clínica, dígito de CPF, fuso ou resultado sorológico sem decisão registrada.

## 11. Próximas ações

1. Developers: tratar QA-001, QA-044 e QA-045 antes das demais frentes; os dois últimos têm sentinelas prontas para inversão.
2. Após correção do agendamento, implementar a prova PostgreSQL determinística da última vaga e da reserva duplicada da #17.
3. Após a definição de coleta/auditoria, implementar rollback, unicidade concorrente e comprovante da #18.
4. Após definir clock/fuso e política de bolsa, implementar expiração concorrente e estoque real da #19.
5. QA: manter sentinelas com nomes/IDs estáveis; converter cada uma em prova do contrato corrigido assim que a mudança chegar.

## 12. Evidência final

- Baseline remoto: GitHub Actions run `40220462421`, `75bfcd4`, `clean verify`, sucesso.
- Execução local final: `./mvnw -B -ntp clean verify`, Maven 3.9.16, Temurin 21.0.12.1, PostgreSQL 16 via Testcontainers.
- Resultado final: **135 testes, 135 verdes, 0 ignorados**.
- Cobertura: números medidos na seção 9 a partir do `jacoco.exec` da execução final.
