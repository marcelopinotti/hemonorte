# Relatório de QA — Hemonorte

Atualização de 2026-10-23, sobre a `develop` @ 8e35342 (que inclui o merge do PR #28 — a rodada anterior, aprovada pelo code owner) e o trabalho novo na branch `QA`. Duas frentes nesta rodada: a análise do marco M3 (as issues de teste #15–#19 do Dev) e a primeira leva de testes demandados por ele — services da #17 e contratos unit-testáveis da #15. Documento vivo — o que foi corrigido e verificado sai da lista; defeitos novos entram com os próximos IDs.

## 1. Resumo executivo

A análise do M3 fechou o veredito: **nenhuma das issues de teste #15–#19 foi implementada** — 5 abertas, 0 fechadas, zero critérios marcados; o único critério de aceite concluído em todo o marco foi o nosso (conversão das sentinelas, feito no PR #28). Esta rodada ataca o que não depende dos developers: a suíte cresceu de 90 para **116 testes** — services de agendamento, aptidão e slots com regras e contratos provados, POST/400/409 nos cadastros — e a cobertura de linhas saltou de 31,7% para **44,1%**, com três services a 100%.

Dois achados novos: a resposta de `/agendamentos` **expõe o hash da senha do doador** (o controller devolve a entidade crua e `senhaHash` não tem `@JsonIgnore` — QA-043, com sentinela) e o **contrato de status da issue #9 não existe no handler** (inaptidão viraria 422 e lotação 409; hoje ambos caem no 400 genérico — QA-044, com duas sentinelas). E uma correção de leitura importante: a entidade `SlotAgenda` **exige início futuro na criação** — a acusação anterior de que o `criarSlot` aceitaria passado estava errada e sai do QA-037; o que resta nele é a corrida conta-então-insere, a reserva duplicada do mesmo doador (sentinela desta rodada) e o slot que envelhece sem revalidação na reserva.

## 2. Inventário técnico

| Item | Valor |
|---|---|
| Linguagem | Kotlin 2.3.21 (`pom.xml:31`) |
| Java | alvo 17 no pom — executado com Temurin 21.0.12.1 |
| Framework | Spring Boot 4.1.1 (`pom.xml:7`) |
| Build | Maven 3.9.16 via wrapper 3.3.4 (only-script) |
| All-open | `@Entity`/`@MappedSuperclass`/`@Embeddable` abertos (`pom.xml:154-156`) |
| Testes | JUnit Jupiter 6.0.3, kotlin.test, Mockito 5.23.0, MockMvc standalone, Surefire 3.5.6 |
| Cobertura | JaCoCo 0.8.15 |
| CI | GitHub Actions: `mvn -B -ntp -DskipTests clean package`, só em PR — QA-038 |
| Banco | PostgreSQL `localhost:5432/hemonorte`; senha fixa no arquivo (QA-029) |
| Commits | Conventional Commits (`.gitmessage`); os de QA seguem a mensagem combinada |

## 3. Repositório e branches

O PR #28 (sentinelas + relatório) foi **aprovado e merged pelo code owner em 22/10** — o fluxo ruleset (PR + build + code owner) funcionou de ponta a ponta. Nota de processo: o **PR #26 mergou `feat/issue-8-disponibilidade` direto na main**, porpassando a develop — a main contém agora a feature de disponibilidade de vagas que a develop não tem (divergência confirmada: `25eca22` está em `origin/main` e fora de `origin/develop`). Contraria a arquitetura develop-first declarada no próprio repo e cria dúvida sobre qual versão do #8 é canônica para os testes da #17. Sinalizado para revisão; main segue intocada por este trabalho.

## 4. Arquitetura

Sem mudanças desde b8ed3da na develop. A leitura desta rodada cobriu: `Agendamento` (transições PENDENTE→CONCLUIDO/CANCELADO/FALTOU com comprovante só na conclusão), `SlotAgenda` (exige início futuro e capacidade positiva na construção — a guarda temporal que corrigiu a leitura do QA-037), `ApiExceptionHandler` (404/400/409; **sem mapeamento para 422**), `AgendamentoController` (base `/agendamentos`, responde a entidade crua — origem do QA-043) e `RealizarAgendamentoRequest` (apenas `@NotNull` nos dois ids).

## 5. A suíte de testes

17 classes, 116 testes, todas `@Tag("unit")` exceto o smoke:

- Model, 75 verdes (as 8 classes de sempre: Triagem 9, BolsaSangue 14, Doador 16, Funcionario 11, Agendamento 8, EstoqueMinimo 6, ExameSorologico 6, SlotAgenda 5).
- DTO, 7 verdes: Bean Validation do cadastro de doador.
- Controller, 16 verdes com MockMvc standalone: DoadorControllerTest (4 — incl. **POST com conflito de unicidade → 409 do handler**, contrato do caminho da corrida do QA-028 com a mensagem fixa do QA-027 travada conscientemente); FuncionarioControllerTest (6 — incl. **POST → 201 sem expor senhaHash** e **PUT com corpo inválido → 400**, o contrato Bean Validation da #15); AgendamentoControllerTest (6 — **POST → 201 com PENDENTE**; **PATCH cancelar → 200 CANCELADO**; **POST sem slotId → 400**; três sentinelas: vazamento do QA-043, 422 e 409 do QA-044).
- Service, 17 verdes — a nova fronteira da #17: SlotAgendaServiceTest (4 — cria e persiste, início duplicado, **início no passado rejeitado pela guarda da entidade**, consulta por período); AptidaoServiceTest (5 — sem histórico → apto consultando só CONCLUIDO, homem inapto a 30 dias com próxima data, **limite inclusivo de 60 dias**, mulher 90 com contraste do homem, doação mais recente vence); AgendamentoServiceTest (8 — PENDENTE criado quando apto e com vagas, inapto com motivo sem persistir, lotado, doador/slot inexistentes, **sentinela QA-037 da reserva duplicada**, cancelar e falta persistidos).
- Integração, 1 não executado daqui: contextLoads exige Docker (e, quando rodar, tromba no QA-035 antes de qualquer coisa).

## 6. Achados

Saem da lista nesta rodada: nenhum por correção de código — mas o QA-037 perde uma acusação por correção de leitura (a entidade SlotAgenda bloqueia início passado na criação; o texto anterior estava errado sobre o `criarSlot`).

### QA-035 (crítico) — a aplicação não sobe: JPQL malformada no repositório de bolsas

Inalterado desde a última rodada (`repository/BolsaSangueRepository.kt:13`, falta o `FROM`). Correção de uma linha; bloqueia qualquer teste "PostgreSQL real" do M3 — inclusive os que as issues #15–#19 exigem.

### QA-001 (crítico) — API de dados sem segurança

Inalterado; #7 (JWT) segue sem implementação — trava a issue de testes #16 inteira.

### QA-043 (novo, alto) — a resposta de /agendamentos expõe o hash da senha do doador

`AgendamentoController` devolve a entidade `Agendamento` crua (ResponseEntity<Agendamento>) e `Doador.senhaHash` não tem `@JsonIgnore` (`controller/AgendamentoController.kt:19-21`, `model/Doador.kt:36-38`). O DTO de doador protege o hash nas rotas de doador — mas esta resposta serializa a entidade inteira, vazando `doador.senhaHash` (e CPF/nascimento) em POST e PATCH. A sentinela `SENTINELA QA-043` no AgendamentoControllerTest trava o comportamento atual com o valor do hash no corpo da resposta; quando corrigirem (DTO de resposta ou `@JsonIgnore`), inverte para `doesNotExist`. Critério literal da #16: "ausência de senha/hash nas respostas".

### QA-034 (alto) — o BCrypt é calculado e descartado

Inalterado (`service/FuncionarioService.kt:25,31`): `senhaCriptografada` nunca usada; o hash cru do cliente persistido.

### QA-036 (alto) — estoque crítico é um stub que retorna zero

Inalterado (`service/EstoqueService.kt:39`).

### QA-037 (alto, atualizado) — capacidade admite corrida; reserva duplicada sem checagem; slot envelhecido sem revalidação

Correção de leitura: a entidade `SlotAgenda` **rejeita início no passado na criação** (`model/SlotAgenda.kt:43-45`) — agora provado pelo teste `rejeita inicio no passado`. Restam três buracos: (1) a checagem de capacidade é conta-então-insere (`service/AgendamentoService.kt:35-36`) — a corrida da última vaga exige PostgreSQL real, como a #17 especifica; (2) **reserva ativa duplicada do mesmo doador no mesmo slot é aceita** — nada checa o dono das reservas existentes; a sentinela `SENTINELA QA-037` documenta e vira asserção de rejeição quando corrigirem; (3) um slot válido, criado no futuro, envelhece — e a reserva não revalida `inicio` contra agora; este terceiro não é provável em teste unitário (a entidade bloqueia a fixture de slot passado) e fica para a integração da #17 com banco real.

### QA-039/QA-040 (alto) — fluxo de doação incompleto e sorologia sem transição

Inalterados: coleta sem bolsa/comprovante/conclusão/auditoria; exame sem transicionar a bolsa. Travam os critérios centrais das issues #18/#19.

### QA-044 (novo, médio) — contrato de status da issue #9 não existe no handler

O contrato da #9/#17 prevê **422 com motivo** para inaptidão e **409** para lotação; o handler só conhece 404/400/409-de-integridade (`controller/ApiExceptionHandler.kt`) e ambos caem no `IllegalArgumentException → 400`. Duas sentinelas no AgendamentoControllerTest registram o 400 atual com a mensagem correta no corpo; quando o mapeamento 422/409 chegar, as asserções trocam. Família do QA-027/QA-028 (mesma condição de negócio, códigos divergentes).

### Médios e baixos

QA-026/QA-032 — inalterados (hash é responsabilidade do cliente; update de doador exige senha). QA-003 — inalterado (auditoria sem emissor; única entidade sem repository). QA-010 — inalterado (expiração só pega DISPONIVEL). QA-027/QA-028 — o 409 agora está provado no teste de POST de doador (o caminho da corrida de unicidade). QA-038 — inalterado (CI sem testes; passaria direto no QA-035). QA-041 — inalterado (volumeMl negativo). QA-014 — inalterado (relógios). QA-029 — inalterado. QA-033 — FuncionarioService segue sem `@Transactional` (e a cobertura 0/29 agora o evidencia no JaCoCo). QA-042 — inalterado (políticas de delete divergentes). QA-011/012/017/031 — inalterados.

## 7. Estado real da matriz de testes

| Bloco | Resultado desta execução |
|---|---|
| Regras de domínio (8 classes) | 75 verdes |
| Bean Validation do cadastro de doador | 7 verdes |
| API standalone (Doador, Funcionario, Agendamentos) | 16 verdes — incl. contratos 201/400/404/409 e 3 sentinelas |
| Services (SlotAgenda, Aptidao, Agendamento) | 17 verdes — a fronteira #17 em nível unitário; incl. sentinela da reserva duplicada |
| Integração (contextLoads) | não executado nesta sessão; com Docker, revela o QA-035 |

Status do marco M3 após esta rodada, no que é mensurável: **#15** — contratos 400/404/409 provados e CRUD de cadastros coberto no nível standalone; pendentes: Flyway/corrida de unicidade em banco real (bloqueados pelo QA-035 e pelo CI sem testes) e hash do servidor (bloqueado pelo QA-034). **#16** — bloqueada pela #7. **#17** — regras de horário/aptidão/capacidade provadas em nível unitário; corrida real, reserva duplicada e 422/409 seguem como sentinelas, pendentes de banco e de correção. **#18/#19** — nada além do que já existia; bloqueadas pelos QA-039/QA-040/QA-003/QA-036.

## 8. JaCoCo

Números desta execução: **44,1% das linhas (334 de 757) e 49,9% das instruções**. Pacote a pacote: model 56,1% de linhas, 64,3% de instruções, 70,1% de branches; enums 100%; dto 42,9%; controller 31,6%; service **28,6%** (era 0). No service, três classes a 100% — AgendamentoService 26/26, AptidaoService 18/18, SlotAgendaService 10/10; as outras oito seguem zeradas (Triagem 0/23, Funcionario 0/29, Doador 0/25, Coleta 0/20, ExameSorologico 0/20, Estoque 0/15, Expiracao 0/13) — justamente as travadas pelos achados de fluxo ou por correções pendentes. A leitura do salto: +94 linhas cobertas contra a última medição (240→334), cobertura geral de 31,7%→44,1% sem uma linha nova de código de produção — só teste. A meta conversável continua sendo limite mínimo no domínio (56,1%).

## 9. Automação via Maven

Inalterado na essência (JDK portátil + `.\mvnw.cmd clean test`; Docker para o contextLoads). Duas lições novas desta rodada, para o livro: (1) **nome de teste entre crases não aceita `;`** — o compilador Kotlin rejeita; usar hífen. (2) **não aninhar stubbing** — criar mock/stub dentro do argumento `thenReturn(...)` de outro `when(...)` produz `UnfinishedStubbing`; fixture se constrói antes. E uma técnica que fica: fixtures reais batem mocks de entidade — getters finais e validação no construtor tornaram o mock de `SlotAgenda` inútil; datas controladas com o parâmetro `dataReferencia` provaram as mesmas regras sem fragilidade.

## 10. Riscos de configuração e persistência

1. QA-035 primeiro: boot impossível.
2. QA-043: hash vazando numa resposta que será autenticável — com a auth por vir (#7), o endpoint já nasce furado.
3. QA-026/QA-034: fundação da auth.
4. Senha do banco hardcoded (QA-029).
5. Corrida de capacidade (QA-037) e contratos 422/409 (QA-044).
6. Dados clínicos sem auth (QA-001) — LGPD.
7. Auditoria sem emissor (QA-003).
8. Divergência main×develop (PR #26 direto na main) — risco de integrar e de testar contra a versão errada.
9. V1/V2 aplicadas são intocáveis.

## 11. Decisões de negócio pendentes

Inalteradas: 1. dígito verificador de CPF (QA-007); 2. faixa etária/consentimento; 3. VENCIDA×DESCARTADA e EM_ANALISE→VENCIDA (QA-010); 4. INDETERMINADO na sorologia (QA-016); 5. BCrypt de verdade (QA-034) e auth (QA-001); 6. **chancela formal do intervalo 60/90 dias por sexo** — agora provado em teste (AptidaoServiceTest), esperando decisão clínica; 7. capacidade e limite por doador (QA-037); 8. emissor de auditoria (QA-003); 9. identidade de gênero (seção 13).

## 12. Plano por etapas

Etapa 0-2 — feitas (ambiente; domínio; JaCoCo refeito: 44,1% geral).
Etapa 3 (correções) — QA-024/QA-025 provados; pendem QA-035 (uma linha), QA-034 (uma linha), QA-036, QA-037, QA-039/QA-040, QA-043, QA-044.
Etapa 4 (services + integração) — INICIADA: três services da #17 provados; restam TriagemService/ColetaService (travados pelos QA-039 e pela falta de fluxo) e integração real (travada pelo QA-035 + Docker + CI sem testes).
Etapa 5 (CI) — pela metade (QA-038).
Etapa 6 — aguarda decisão 9.

## 13. Sexo e identidade de gênero

Sem novidades de código nesta rodada; a discussão permanece como registrada (separar sexo de identidade, coleta opcional, V3 aditiva quando a decisão sair).

## 14. Próximas ações

1. Commits desta rodada no formato combinado: um com os testes (5 classes novas + 2 ampliadas), outro exclusivo com este relatório — push na `QA`, PR para a develop, review do code owner.
2. Developers, em ordem de custo: QA-035 (uma linha, destrava o M3 inteiro), QA-034 (uma linha), QA-043 (DTO de resposta ou @JsonIgnore), QA-044 (422/409 no handler), QA-036, QA-037, QA-039/QA-040.
3. Rodar `.\mvnw.cmd clean test` com Docker no seu terminal: o contextLoads valida Flyway/JPA — e expõe o QA-035.
4. Alinhar com o code owner a divergência main×develop (PR #26) antes dos testes de integração da #17.
5. Próxima leva de QA quando as correções chegarem: conversão das sentinelas QA-037/QA-043/QA-044 e integração Testcontainers das #15/#17.

Ressalva final: execução completa da suíte unit nesta máquina — 116 testes no Maven 3.9.16 + Temurin 21.0.12.1 + Boot 4.1.1, cobertura medida do jacoco.exec desta rodada (334/757 linhas). Não rodou: o contextLoads e tudo que exige banco real (Docker inalcançável da sessão). As sentinelas apontam comportamentos verificados em execução; cada achado traz arquivo e linha. Nada aqui é estimativa.
