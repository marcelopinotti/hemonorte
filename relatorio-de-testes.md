# Relatório de QA — Hemonorte

Atualização de 2026-10-19, sobre a `develop` @ b8ed3da (sincronizada com o remoto por fast-forward): os developers entregaram as features das issues #6 e #8–#14, o fix do nosso QA-024 veio por agente automatizado e o dono do repo subiu docs e um CI (PR #25). Documento vivo — o que foi corrigido e verificado sai da lista; defeitos novos entram com os próximos IDs.

## 1. Resumo executivo

A develop andou 19 commits desde o nosso último "QA commit" e virou outro sistema: agenda com capacidade, triagem, coleta, sorologia, estoque e expiração diária existem em código, com services, controllers e DTOs próprios. Três achados saem da lista por correção verificada em execução: QA-024 (o crítico do `@Pattern` sobre Char — `genero` virou String e os 7 testes de Bean Validation ficaram verdes), QA-025 (PUT e DELETE por id voltaram ao mapa — as sentinelas do 405 foram convertidas nas provas do contrato, PUT 200 e DELETE 204, critério literal da issue #15) e QA-020 (as entidades têm repository; a única exceção é a HistoricoAuditoria, que é a pendência do próprio QA-003). QA-002 também sai: a capacidade é conferida no AgendamentoService — o resíduo de concorrência vira o novo QA-037.

O código novo trouxe nove achados, dois graves. O mais grave é de uma linha: a query de expiração veio com JPQL malformada (sem `FROM`) e o Spring Data derruba o boot — a aplicação não sobe de jeito nenhum (QA-035); o CI deles não pega porque compila sem testes. A tentativa de corrigir o QA-026 calcula o BCrypt e joga fora: `senhaCriptografada` nunca é usada e o valor cru do cliente segue persistido (QA-034). Completam a onda: estoque crítico que é um stub retornando zero (QA-036), reserva de capacidade conta-então-insere que admite corrida (QA-037), fluxo de doação que para na coleta sem bolsa nem comprovante (QA-039), sorologia que não aplica o resultado à bolsa (QA-040), CI que não roda testes (QA-038), `volumeMl` negativo aceito (QA-041) e política de exclusão divergente entre doador e funcionário (QA-042).

A suíte desta execução: 90 testes, 89 verdes e 1 vermelho — o contextLoads, que segue sem Docker nesta sessão e, quando rodar no seu terminal, vai trombar no QA-035 antes de qualquer coisa. Cobertura medida: 31,7% das linhas — o numerador cresceu (234→240 linhas cobertas), mas entraram 289 linhas novas sem teste direto; o pacote service, agora o maior, está em zero.

## 2. Inventário técnico

| Item | Valor |
|---|---|
| Linguagem | Kotlin 2.3.21 (`pom.xml:31`) |
| Java | alvo 17 no pom (era 21; baixado nesta onda e casando com o CI) — executado com Temurin 21.0.12.1 |
| Framework | Spring Boot 4.1.1 (`pom.xml:7`) |
| Build | Maven 3.9.16 via wrapper 3.3.4 (only-script) |
| Web | `spring-boot-starter-webmvc`, Jackson 3 Kotlin, Actuator |
| Persistência | Data JPA, Flyway + `flyway-database-postgresql`, driver PostgreSQL |
| Validação | `spring-boot-starter-validation` (Hibernate Validator) |
| Segurança | `spring-security-crypto` entrou no pom (BCrypt instanciado — e descartado, QA-034); sem auth |
| Testes | starters `-test`, `spring-boot-testcontainers`, Testcontainers 2.0.5 |
| Motor de teste | JUnit Jupiter 6.0.3, Mockito 5.23.0, Surefire 3.5.6 |
| Cobertura | JaCoCo 0.8.15 |
| CI | GitHub Actions: `mvn -B -ntp -DskipTests clean package`, só em PR — QA-038 |
| Banco | PostgreSQL `localhost:5432/hemonorte`; usuário e senha `postgres` fixos no arquivo |
| JPA | `open-in-view: false`; `ddl-auto: validate`; timezone JDBC UTC; `show_sql: true` |
| Docs | README e modelagem versionados em `docs/` (QA-015 resolvido) |
| Commits | Conventional Commits (`.gitmessage`); os de QA seguem a mensagem combinada |

## 3. Repositório e branches

Develop sincronizada por fast-forward: 996681d → b8ed3da, 19 commits — 11 `feat` de Esdras Daniel (issues #6 e #8–#14), docs + CODEOWNERS + CI de Marcelo Pinotti (PR #25), o fix do QA-024 por copilot-swe-agent e o merge da main. A main andou até 861329a (PR #4, develop→main) e já contém os nossos dois "QA commit" anteriores via PR #3 — segue intocada por este trabalho. Na árvore, aguardando os commits desta rodada: a conversão das sentinelas do FuncionarioControllerTest e este relatório.

## 4. Arquitetura

```
com.hemonorte
├── HemonorteApplication.kt   +@EnableScheduling (o cron da #14 exige)
├── controller/  Doador, Funcionario, Agendamento, Aptidao, SlotAgenda,
│                Triagem, Coleta, ExameSorologico, Estoque, Hello
├── service/     Doador, Funcionario, Agendamento, Aptidao, SlotAgenda,
│                Triagem, Coleta, ExameSorologico, Estoque, ExpiracaoBolsa
├── repository/  9 interfaces — falta só HistoricoAuditoria (QA-003)
├── dto/         requests/responses das features novas + os de cadastro
└── model/       10 entidades (Coleta é nova) + 5 enums
```

O desenho em camadas se mantém. Três observações: a AptidaoService decide intervalo entre doações por sexo (60/90 dias, `AptidaoService.kt:16-34`) — regra clínica codificada como "exemplo padrão", pedindo chancela formal; a expiração é `@Scheduled(cron = "0 0 2 * * *")` com `@Transactional` e logger, horário batendo com a spec do M2 — mas o comentário diz "meia-noite" (mentira de duas horas); e a regra de triagem apta aparece duas vezes (no `init` de Coleta e no ColetaService) — coerentes entre si, duplicação a observar.

## 5. A suíte de testes

12 classes, 90 testes, todas `@Tag("unit")` exceto o smoke:

- Model, 75 verdes: TriagemTest (9), BolsaSangueTest (14), DoadorTest (16), FuncionarioTest (11), AgendamentoTest (8), EstoqueMinimoTest (6), ExameSorologicoTest (6), SlotAgendaTest (5).
- DTO, 7 verdes: CadastroDoadorRequestValidationTest — os testes que eram a evidência do QA-024 agora provam o contrato corrigido (`genero` String com `[MF]`).
- Controller, 7 verdes com MockMvc standalone e serviços mockados: DoadorControllerTest (3 — listagem; 200 sem expor `senhaHash`, a regressão do QA-008; 404 com corpo do handler); FuncionarioControllerTest (4 — **as duas sentinelas do 405 viraram as provas do contrato: PUT por id devolve 200 com o corpo atualizado e DELETE por id devolve 204 com `verify` no service**, critério literal da #15). Nota técnica da conversão: o stubbing usa argumentos concretos — o DTO é data class e a igualdade por valor casa com a instância que o controller desserializa; matchers `eq()`/`any()` em Kotlin devolvem null placeholder e NPE no parâmetro não-nulo.
- Integração, 1 não executado daqui: o contextLoads exige o container PostgreSQL; o Docker segue inalcançável desta sessão.

## 6. Achados

Saem da lista, resolvidos e verificados em execução: QA-024 (genero String; suíte Bean Validation verde), QA-025 (PUT/DELETE no mapa; provas 200/204), QA-002 (capacidade conferida no AgendamentoService; resíduo de concorrência vira QA-037), QA-015 (docs da modelagem versionadas em `docs/`) e QA-020 (toda entidade tem repository, exceto HistoricoAuditoria — que é o próprio QA-003, não uma dívida nova).

### QA-035 (crítico) — a aplicação não sobe: JPQL malformada no repositório de bolsas

`BolsaSangueRepository.findBolsasVencidasParaExpirar` (`repository/BolsaSangueRepository.kt:13`) veio com `@Query("SELECT b BolsaSangue b WHERE ...")` — faltou o `FROM`. O Spring Data valida a query na criação do bean e o contexto morre no boot: qualquer execução com banco real (o contextLoads no seu terminal; um CI que rodasse testes) falha antes de validar Flyway ou JPA. Achado binário: não existe deploy enquanto isso estiver lá. Correção numa linha: `SELECT b FROM BolsaSangue b WHERE ...`.

### QA-001 (crítico) — API de dados sem segurança

Inalterado e mais urgente a cada feature: agora existem endpoints de triagem (peso, impedimentos clínicos), coleta, sorologia (HIV, hepatite, Chagas) e estoque — tudo aberto, sem autenticação nem autorização, com Swagger público. A issue #7 (JWT, marco M1) espera; a #16 (testes de auth) depende dela.

### QA-034 (alto) — o BCrypt é calculado e descartado; o hash cru do cliente segue indo pro banco

A tentativa de correção do QA-026 adicionou `spring-security-crypto` ao pom e instanciou o encoder — mas `val senhaCriptografada = passwordEncoder.encode(request.senhaHash)` (`service/FuncionarioService.kt:25`) nunca é usado: a entidade é criada com `senhaHash = request.senhaHash` (`FuncionarioService.kt:31`), o valor cru. O QA-026 segue aberto com nova evidência, e piorou de figura: agora existe a ilusão de correção. Correção numa linha: usar a variável no construtor.

### QA-037 (alto) — a capacidade do slot admite corrida e dois buracos de contrato

`AgendamentoService.realizarAgendamento` confere capacidade contando os PENDentes e inserindo (`service/AgendamentoService.kt:35-36`) — duas requisições simultâneas pela última vaga passam juntas pela checagem e a capacidade estoura; a issue #9 pedia proteção "em transação e no banco", e o `@Version` de agendamentos distintos não protege a capacidade compartilhada. Faltam também: impedir reserva ativa duplicada do mesmo doador no mesmo slot (nada checa) e reserva em horário passado (nem `SlotAgendaService.criarSlot` nem o agendamento comparam `inicio` com agora).

### QA-036 (alto) — estoque crítico é um stub que sempre reporta zero

`EstoqueService.calcularBolsasAprovadasPorTipagem` (`service/EstoqueService.kt:39`) retorna 0 com um `// Ajustaremos`. Efeito: `listarEstoquesCriticos` compara 0 <= quantidadeCritica e devolve TODAS as tipagens como críticas, sempre. A issue #13 está referenciada como implementada no commit, mas o coração operacional não existe — pior que ausente: enganoso para quem operar por esse endpoint.

### QA-039 (alto) — o fluxo de doação para na coleta: sem bolsa, sem comprovante, sem auditoria

`ColetaService.registrarColeta` valida triagem apta e duplicidade e salva a Coleta. A issue #11 exige criar a bolsa em `EM_ANALISE`, concluir o agendamento e gerar `codigoComprovante` na MESMA transação, integrando HistoricoAuditoria — nada disso existe. Efeito em cadeia: a AptidaoService só considera agendamentos `CONCLUIDO`, que nada no sistema produz; a regra de intervalo 60/90 dias é inerte na prática.

### QA-040 (alto) — a sorologia registra o exame e não aplica o resultado à bolsa

`ExameSorologicoService.registrarExame` não checa o status da bolsa (a #12 exige `EM_ANALISE` para liberar/descartar) e não transiciona nada — nem `DISPONIVEL` nem `DESCARTADA`. Combinado com o QA-039 (nenhuma bolsa nasce do fluxo), o exame só existiria com dado plantado à mão.

### Médios

QA-026/QA-032 — atualizados: o FuncionarioService ganhou um encoder inútil (QA-034) e o `AtualizacaoFuncionarioRequest` perdeu o campo de senha (evolução boa); o doador segue exigindo `senhaHash` no update e sem hashing no DoadorService (`service/DoadorService.kt:41-47`).

QA-003 — a auditoria continua sem emissor, agora com agravante: o ExpiracaoBolsaService transiciona bolsas com um comentário dizendo que "registra na auditoria" e não grava nada; a HistoricoAuditoria segue como a única entidade sem repository.

QA-010 — a expiração só pega bolsas `DISPONIVEL` (a condição de status está na mesma query do QA-035); `EM_ANALISE→VENCIDA` segue decisão clínica aberta.

QA-027/QA-028 — contratos de erro inalterados.

QA-038 (novo) — o CI compila sem testes (`mvn -B -ntp -DskipTests clean package`, `.github/workflows/ci.yaml:30`) e só roda em `pull_request` (develop/main): push direto na develop não dispara nada, e nem JUnit nem JaCoCo são publicados — a issue #15 prevê publicar. Enquanto isso, defeitos como o QA-035 passam batido pelo CI.

QA-041 (novo) — `volumeMl` aceita negativo: só `@NotNull` no DTO (`dto/RegistrarColetaRequest.kt:14-15`) e nenhum `require` no modelo.

QA-014 — cresceu: além dos relógios da entidade, `Coleta.dataHora` usa `LocalDateTime.now()` (sem fuso) e a Aptidao converte `Instant` com `ZoneId.systemDefault()` — a issue #21 do Dev nasce daqui.

QA-029 — senha do banco segue hardcoded.

QA-016/QA-006/QA-007 — inalterados (decisões clínicas/produto).

### Baixos

QA-011 — `postgres:latest` no Testcontainers.

QA-012/QA-017 — `show_sql` e ausência de loggers (a #23 do Dev cobre); nota positiva: ExpiracaoBolsaService loga início, fim e total.

QA-031 — a V2 segue; o alvo Java caiu pra 17, sem impacto no achado.

QA-033 — FuncionarioService segue sem `@Transactional` e listagens sem paginação (a #20 cobre a paginação).

QA-042 (novo) — excluir doador é hard delete (`doadorRepository.delete`); excluir funcionário virou soft delete (`desativar`). Doador com vínculos explode em 409 com a mensagem fixa do handler; políticas de retenção divergentes merecem unificação.

## 7. Estado real da matriz de testes

| Bloco | Resultado desta execução |
|---|---|
| Regras de domínio (8 classes) | 75 verdes |
| Bean Validation do cadastro de doador | 7 verdes — prova do QA-024 corrigido |
| API standalone (Doador e Funcionario) | 7 verdes — incl. provas PUT 200 / DELETE 204 do QA-025 |
| Integração (contextLoads: Flyway V1→V2 + validate) | não executado nesta sessão; com Docker, revela o QA-035 |

Na fila, agora com roteiro oficial: as issues #15–#19 do Dev (marco M3) definem os testes por feature. A #15 pede Flyway em banco real, CPF imutável, corrida de duplicidade com 409 e a conversão das sentinelas (feito nesta rodada); a #17 pede a corrida da última vaga; a #18, rollback e comprovante; a #19, sorologia, estoque e expiração. O M3 manda reutilizar esta suíte e explicitamente não aceita teste ignorado como evidência.

## 8. JaCoCo

Números desta execução: 31,7% das linhas (240 de 757) e 38,4% das instruções. Pacote a pacote: model 55,8% de linhas, 63,1% de instruções e 70,1% de branches; enums 100%; dto 29,4%; controller 13,3%; service 0 de 203 linhas. A queda contra os 50% da última medição tem leitura simples: cobrimos MAIS linhas (234→240), mas entraram 289 linhas novas sem teste nenhum — 11 services e 7 controllers novos. As entidades sem teste puxam o model para baixo: Coleta 0/23 e HistoricoAuditoria 0/17 (60,3% → 55,8%). Quando os testes de service do M3 existirem esses números saltam; por ora, cobertura segue sendo termômetro do domínio, não da camada nova.

## 9. Automação via Maven

No seu terminal, com o Docker Desktop aberto:

```powershell
$env:JAVA_HOME = 'C:\Users\Thiago\Desktop\Codando Sadesgraça\Hemonorte\.qa-tools\jdk\jdk-21.0.12.1+1'
.\mvnw.cmd clean test
```

Duas notas operacionais desta sessão, que roda em sandbox sem acesso ao mecanismo de attach da JVM: (1) o Mockito 5 precisa anexar seu agente à JVM; onde o attach é negado, os testes de controller morrem com `Could not initialize inline Byte Buddy mock maker` — a saída é pré-carregar o agente via `$env:JAVA_TOOL_OPTIONS = '-javaagent:<caminho do mockito-core-5.23.0.jar>'` (use caminho curto 8.3 se houver espaço no path; aspas internas quebram o mvnw.cmd). (2) não passe `-DargLine` no CLI para isso: a propriedade de CLI sombreia a que o `prepare-agent` do JaCoCo define, e a cobertura some silenciosamente. O contextLoads e tudo que exige banco real seguem precisando do seu terminal ou de CI com Docker — e vão trombar no QA-035 assim que o Flyway rodar.

## 10. Riscos de configuração e persistência

1. QA-035 primeiro: boot impossível — sem deploy, nada abaixo importa.
2. QA-026/QA-034: hash de senha é responsabilidade do cliente e a "correção" é cosmética — fundação da auth.
3. Senha do banco hardcoded (QA-029).
4. Corrida de capacidade (QA-037) e contrato de duplicidade 400/409 (QA-028).
5. Dados clínicos (sorologia, triagem, CPF, nascimento, gênero) sem auth nenhuma (QA-001) — LGPD.
6. Auditoria sem emissor (QA-003), agora com comentário prometendo o que não faz.
7. V1 e V2 aplicadas são intocáveis; ajuste de esquema vem em migração nova.

## 11. Decisões de negócio pendentes

1. CPF exige dígito verificador? (QA-007 — produto)
2. Faixa etária mínima/máxima e consentimento? (clínica/produto)
3. VENCIDA x DESCARTADA: bolsa expirada em análise vai pra qual? `EM_ANALISE→VENCIDA` deve existir? (QA-010 — clínica)
4. Sorologia admite INDETERMINADO com reteste? (QA-016 — clínica)
5. Hash de senha (BCrypt de verdade, QA-034) e mecanismo de auth (QA-001 — produto/tech)
6. Intervalo entre doações: o 60/90 dias por sexo já está em código como "exemplo padrão" (`AptidaoService.kt:16`) — chancela clínica formal; a #17 testa os limites.
7. Regras de capacidade e limite por doador (QA-037 — produto)
8. Auditoria: quem grava e quando (QA-003 — produto/tech)
9. Vocabulário de identidade de gênero, exposição na API, coleta opcional, nome social (seção 13)

## 12. Plano por etapas

Etapa 0 (ambiente) — feita: JDK 21, Maven, acesso GitHub.
Etapa 1 (domínio) — feita: 75 verdes.
Etapa 2 (JaCoCo) — feita e refeita nesta rodada: 31,7% geral, model 55,8%.
Etapa 3 (correções dos developers) — QA-024 e QA-025 corrigidos e provados; restam QA-034 (uma linha), QA-035 (uma linha), QA-036, QA-037, QA-039/QA-040 e QA-027/QA-028.
Etapa 4 (services + integração real) — agora com roteiro: issues #15–#19 do M3; alvo inicial a #15, que não depende de auth.
Etapa 5 (CI) — existe pela metade: compila sem testes e só em PR (QA-038); rodar `clean verify` e publicar JUnit/JaCoCo fazem parte da #15.
Etapa 6 (sexo/identidade) — aguarda a decisão 9.

## 13. Sexo e identidade de gênero

O QA-024 está corrigido — `genero` virou String com `@Pattern [MF]` no DTO, e a entidade segue Char M/F validado — portanto o cadastro funciona e a discussão fica mais barata. O mérito não mudou: separar sexo (a coluna atual, consultada por protocolos clínicos) de identidade de gênero (campo novo, opcional, vocabulário definido por produto e clínica — proposta inicial: HOMEM_CIS, HOMEM_TRANS, MULHER_CIS, MULHER_TRANS, NAO_BINARIO, OUTRO, NAO_INFORMADO). Três regras se mantêm: coleta opcional, nenhum preenchimento automático pra registro existente (NULL, sem inferência) e aptidão nunca pela identidade isoladamente. A migration continua sendo a próxima V livre (V3): `ALTER TABLE doador ADD COLUMN identidade_genero VARCHAR(20);`. Testes quando a decisão sair: identidade válida aceita, fora do enum rejeitada, ausência aceita, sexo seguindo M/F — e HOMEM_TRANS com sexo F é válido (independência semântica documentada em teste).

## 14. Próximas ações

1. Commits desta rodada, no formato combinado: um com a conversão das sentinelas, outro exclusivo com este relatório — ambos "QA commit", na develop, push no fim.
2. Developers, em ordem de custo-benefício: QA-035 (crítico, uma linha), QA-034 (uma linha), QA-036, QA-037, QA-039/QA-040 (o fluxo que fecha o M2), QA-038 (CI rodando testes).
3. Rodar `.\mvnw.cmd clean test` no seu terminal com Docker: o contextLoads valida Flyway V1→V2 + validate — e expõe o QA-035 na sua frente.
4. Seguir o roteiro do M3 nas próximas rodadas de teste — a #15 é a primeira que não depende de auth (#16 espera a #7).
5. Decisões da seção 11 — a 6 (intervalo 60/90) já tem código em produção esperando chancela.

Ressalva final: desta vez há execução completa da suíte unit — 90 testes no Maven 3.9.16 + Temurin 21.0.12.1 + Boot 4.1.1 nesta máquina, e os números da seção 8 vêm do jacoco.exec desta rodada. Não rodou: o contextLoads e tudo que exige banco real, porque o Docker segue inalcançável da sessão. O QA-035 é achado de leitura de código com confirmação inevitável: qualquer boot com banco o reproduz. Nada aqui é estimativa: veio de log, de execução ou de código — e cada achado traz arquivo e linha.
