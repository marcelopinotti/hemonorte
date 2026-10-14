# Relatório de QA — Hemonorte

Atualização de 2026-10-14, sobre a `develop` @ b5c3b0f (sincronizada com o remoto): o código novo dos developers (camada HTTP completa de doador e funcionário) e a suíte de testes desta missão. Documento vivo — o que foi corrigido e verificado sai da lista; defeitos novos entram com os próximos IDs.

## 1. Resumo executivo

Os developers entregaram a camada HTTP: CRUD de doador e de funcionário com DTOs próprios, exception handler central e a migration V2 que proíbe data de nascimento futura no banco. Com isso, cinco achados saem da lista por estarem resolvidos (QA-004, QA-005, QA-008, QA-009 e QA-013 — os resíduos viraram achados novos). O de segurança, QA-001, subiu de prioridade: a API de dados agora existe e continua sem uma linha de autenticação.

A suíte de testes também existe agora: 12 classes, 90 testes, JUnit (jupiter 6.0.3) com kotlin.test, Mockito 5.23 e MockMvc standalone na camada HTTP, rodando com `.\mvnw.cmd clean test` e reportando cobertura via JaCoCo 0.8.15. Resultado desta execução: 82 verdes e 8 vermelhos — e os vermelhos contam a história. Sete são os testes de Bean Validation do cadastro de doador, que falham todos com o mesmo `HV000030`: o `@Pattern` sobre o campo `genero: Char` quebra qualquer POST /api/doador. É o achado mais grave da rodada (QA-024, crítico). O oitavo é o contextLoads, que não sobe nesta sessão porque o Docker é inacessível daqui (seção 9) — precisa rodar no seu terminal ou no CI.

Cobertura medida de verdade pela primeira vez: 50% das linhas e 56,9% das instruções no projeto; o pacote model, dono das regras, em 60,3% de linhas e 73,2% de branches; enums em 100%.

## 2. Inventário técnico

| Item | Valor |
|---|---|
| Linguagem | Kotlin 2.3.21 (`pom.xml:31`) |
| Java | 21 (executado com Temurin 21.0.12.1) |
| Framework | Spring Boot 4.1.1 / Spring Framework 7.0.9 (`pom.xml:5-10`) |
| Build | Maven 3.9.16 via wrapper 3.3.4 (only-script) |
| Web | `spring-boot-starter-webmvc`, Jackson 3 Kotlin |
| Persistência | Data JPA, Flyway + `flyway-database-postgresql`, driver PostgreSQL |
| Validação | `spring-boot-starter-validation` (Hibernate Validator) |
| Compilador Kotlin | `-Xjsr305=strict`; plugins spring, jpa, all-open |
| Documentação | springdoc-openapi 3.1.0 |
| Testes | starters `-test`, `spring-boot-testcontainers`, Testcontainers 2.0.5 |
| Motor de teste | JUnit Jupiter 6.0.3, Mockito 5.23.0, Surefire 3.5.6 |
| Cobertura | JaCoCo 0.8.15 — adicionado nesta missão; antes não existia |
| Segurança | nenhuma dependência no pom |
| CI | não existe `.github/` |
| Banco | PostgreSQL `localhost:5432/hemonorte`; usuário e senha `postgres` fixos no arquivo (`application.yaml:6-10`) |
| JPA | `open-in-view: false`; `ddl-auto: validate`; timezone JDBC UTC; `show_sql: true` |
| Servidor | porta 8080 (default; a linha explícita saiu do application.yaml) |
| Commits | Conventional Commits (`.gitmessage`) |

## 3. Repositório e branches

A main andou sozinha: o dono do repositório fez o merge do PR #3 (a develop, incluindo o nosso "QA commit"), e main local e remota estão em 96ac045. A develop local está sincronizada com a remota em b5c3b0f, com todo o código novo. O acesso ao GitHub voltou a funcionar nesta sessão — o bloqueio anterior era do ambiente de execução, não das credenciais. Na árvore de trabalho, aguardando aprovação: os 12 arquivos de teste, o `pom.xml` com o JaCoCo e este relatório. Nada commitado; a main segue intocada por este trabalho.

## 4. Arquitetura

```
com.hemonorte
├── HemonorteApplication.kt
├── controller/     DoadorController, FuncionarioController, HelloController,
│                   ApiExceptionHandler (@RestControllerAdvice)
├── service/        DoadorService, FuncionarioService
├── repository/     DoadorRepository, FuncionarioRepository
├── dto/            Cadastro/Atualizacao e Response de Doador e Funcionario
│                   (+ FuncionarioRequest, sem uso — QA-030)
└── model/          as 9 entidades + 5 enums (o antigo domain)
```

O desenho agora é o esperado: controller fala com service, service com repository, DTOs nas bordas. Gostei de três mudanças dos developers: os relacionamentos 1-para-1 passaram a ser `@OneToOne` de verdade (bolsa↔agendamento, exame↔bolsa, triagem↔agendamento), batendo com os UNIQUE do esquema; o nascimento ganhou validação em duas camadas (entidade e CHECK no banco, na V2 — com uma sutileza de fuso que registro no QA-031); e a matrícula de funcionário ganhou o limite de 50 caracteres, alinhado ao DTO. O domínio continua sendo o dono das regras — e a suíte mostra que isso paga.

## 5. A suíte de testes

12 classes, todas marcadas `@Tag("unit")` exceto o smoke de integração:

- Model, 75 testes verdes: TriagemTest (9 — bordas do peso, motivos, peso inválido), BolsaSangueTest (14 — transições legais e ilegais, tipagem, validade), DoadorTest (16 — validações e a correção do nascimento), FuncionarioTest (11 — matrícula 50/51, desativar), AgendamentoTest (8), EstoqueMinimoTest (6 — efeito colateral do atualizarQuantidade), ExameSorologicoTest (6), SlotAgendaTest (5).
- DTO, 7 testes vermelhos por design: CadastroDoadorRequestValidationTest — cada um falha com o mesmo HV000030 e constitui a evidência do QA-024.
- Controller, 7 testes verdes com MockMvc standalone e serviços mockados: contrato do handler (404 com corpo de erro), listagens, resposta sem `senhaHash` (regressão do antigo QA-008) e duas sentinelas do QA-025, que registram o 405 atual de PUT e DELETE com id — quando a correção chegar, viram asserções de 200 e 204.
- Integração, 1 não executado daqui: o contextLoads exige o container de PostgreSQL; a sessão de QA não alcança o Docker (seção 9), então ele roda no seu terminal ou no CI.

## 6. Achados

Saíram da lista nesta atualização, resolvidos e verificados: QA-004 (nascimento futuro — teste verde comprova; a faixa etária segue na seção 11), QA-005 (a dívida de testes virou suíte), QA-008 (a resposta não expõe mais o hash — teste verifica), QA-009 (o handler existe e o contrato 404 está testado), QA-013 (a exceção de constraint é tratada; o resto virou QA-027) e QA-018/QA-019 (ambiente destravado).

### QA-024 (crítico) — o cadastro de doador pela API não funciona

`@field:Pattern(regexp = "[MF]")` sobre `val genero: Char` (`dto/CadastroDoadorRequest.kt:26`). Bean Validation só aplica `@Pattern` a CharSequence; com Char, o Hibernate Validator lança `ValidationException (HV000030)` na primeira validação, não importa o payload. Como o DoadorController usa `@Valid`, todo POST /api/doador morre com 500 — a exceção não é IllegalArgumentException nem DataIntegrityViolation, então o handler não pega. Os sete testes de Bean Validation falham exatamente com esse erro: é a evidência em execução. Correção sugerida: `genero` como `String` com o Pattern, ou remover a anotação e deixar a validação com a entidade (que já exige M ou F).

### QA-001 (crítico) — a API de dados existe e continua sem segurança

Promovido de alto pra crítico, como o relatório anterior antecipava: os endpoints de doador expõem CPF, nascimento e tipagem; os de funcionário expõem matrícula — tudo sem autenticação, sem autorização, com Swagger aberto. A recomendação não muda (decidir auth e mapear o PerfilFuncionario antes de qualquer dado real entrar), mas com o CRUD inteiro no ar, a urgência mudou.

### QA-025 (alto) — atualizar e excluir funcionário é inacessível

`@PutMapping` e `@DeleteMapping` do FuncionarioController (`controller/FuncionarioController.kt:37,41`) estão sem o `/{id}`, mas os métodos recebem `@PathVariable id`. Observado em execução: PUT e DELETE em /api/funcionario/{id} devolvem 405 (Method Not Allowed). Ninguém atualiza ou exclui funcionário por essa API. Correção: `@PutMapping("/{id}")` e `@DeleteMapping("/{id}")`; as duas sentinelas do FuncionarioControllerTest registram o comportamento atual.

### QA-026 (alto) — o hash da senha é responsabilidade do cliente

Todos os DTOs de entrada carregam `senhaHash` pronto, que vai direto pra entidade sem hashing no servidor — não existe BCrypt nem equivalente no pom. Na prática, o cliente manda o que quiser (inclusive texto puro) e o banco guarda como "hash". Pra um sistema que vai ter autenticação, isso mina a fundação: o correto é receber `senha` e derivar o hash no service com `spring-security-crypto`. Relacionado: QA-032.

### QA-002 (alto) — agenda sem enforcement

Inalterado: ninguém conta agendamentos por slot. Primeiro alvo quando o serviço de agendamento existir.

### QA-003 (alto) — auditoria modelada, nunca emitida

Inalterado: a HistoricoAuditoria continua sem emissor.

### Médios

QA-006 — a entidade ainda aceita qualquer e-mail com arroba ("a@b" passa se construir a entidade direto). O caminho da API está protegido pelo `@Email` do DTO, então o risco prático caiu; as camadas seguem discordando.

QA-007 — CPF sem dígito verificador ("00000000000" passa nas duas camadas). Decisão de produto em aberto.

QA-010 — bolsa em EM_ANALISE não vira VENCIDA e nenhuma transição olha `dataValidade`. Os 14 testes de transição estão verdes; a decisão clínica continua pendente.

QA-027 (novo) — qualquer `DataIntegrityViolationException` vira 409 com a mensagem fixa "Doador possui dados vinculados" (`controller/ApiExceptionHandler.kt:19-21`). Serve pro delete de doador com vínculos, mas mente pra conflito de unicidade de CPF/e-mail/matrícula — inclusive de funcionário — e descarta a mensagem original.

QA-028 (novo) — duplicidade de CPF/e-mail devolve 400 pelo caminho normal (`require` no service) e 409 pelo caminho da corrida (constraint). Mesma condição de negócio, dois códigos. Sugestão: 409 nos dois.

QA-029 (novo) — a senha do banco voltou hardcoded (`postgres`/`postgres`, `application.yaml:9`); a versão com variável de ambiente era melhor e saiu. Pra dev localhost é conveniência; resolver antes de qualquer ambiente compartilhado.

QA-016 — sorologia binária, sem INDETERMINADO. Pergunta clínica.

QA-020 — atualizado: das nove entidades, duas têm repository (Doador e Funcionário); sete seguem fora da integração.

QA-021 e QA-022 — genero segue um CHAR M/F sem consumidor funcional, e a nova API de cadastro reforça M/F no DTO. Análise na seção 13.

### Baixos

QA-011 — `postgres:latest` no Testcontainers segue; fixar versão quando a integração rodar no CI.

QA-012 — `show_sql: true` (`application.yaml:20`).

QA-014 — relógios acoplados (`Instant.now()` e agora `LocalDate.now()` na validação de nascimento); se doer, injetar `Clock`.

QA-015 — README de uma linha; `modelagem/` fora do versionamento.

QA-017 — nenhum logger no código.

QA-030 (novo) — `dto/FuncionarioRequest.kt` não tem uso nenhum e carrega `@NotBlank` sobre um enum (mesma família do QA-024, latente). Remover ou corrigir.

QA-031 (novo) — a validação de nascimento usa dois relógios: `LocalDate.now()` na JVM e `CURRENT_DATE` no CHECK da V2, com o JDBC em UTC. Se os fusos divergirem, a janela de meia-noite aceita na entidade o que o banco rejeita (ou vice-versa). Testável quando a integração rodar.

QA-032 (novo) — atualizar doador exige `senhaHash` preenchido (`AtualizacaoDoadorRequest`): todo update reenvia "senha". Junto com o QA-026, indica que o campo precisa mudar de papel.

QA-033 (novo) — o FuncionarioService não tem `@Transactional` (o DoadorService tem) e as listagens são `findAll` sem paginação. Consistência pra quando crescer.

## 7. Estado real da matriz de testes

| Bloco | Resultado desta execução |
|---|---|
| Regras de domínio (8 classes) | 75 testes verdes |
| Bean Validation do cadastro de doador | 7 vermelhos — evidência do QA-024 |
| API standalone (Doador e Funcionario) | 7 verdes (incl. 2 sentinelas do QA-025 e o 404 do handler) |
| Integração (contextLoads: Flyway V1→V2 + validate + wiring) | não executado nesta sessão — seu terminal ou CI |

Na fila: testes direto nos services (duplicidade e a corrida do QA-028), integração real com o container (UNIQUE de CPF, CHECK da V2, fuso do QA-031) e, corrigido o QA-024, a suíte de Bean Validation vira verde sozinha.

## 8. JUnit 5 e JaCoCo

Nada de dependência nova pros testes: o JUnit (jupiter 6.0.3) e o Mockito (5.23.0) já vinham pelos starters `-test`; kotlin.test já estava no pom. O que entrou novo foi o JaCoCo 0.8.15 no build — `prepare-agent` no início e o `report` amarrado ao ciclo de testes, gerando HTML, CSV e XML em `target/site/jacoco`. Números reais desta execução: 50% das linhas (234 de 468) e 56,9% das instruções; o pacote model com 60,3% de linhas e 73,2% de branches; enums 100%; dto 42,5%; controller 33,3%; service 0% (mockado nos testes de API — a cobertura deles chega quando os testes de service existirem). É o primeiro número da história do projeto, então sem meta mínima por enquanto; quando a suíte completar, vale conversar um limite só no pacote de domínio, que é onde coverage diz alguma coisa.

## 9. Automação via Maven

No seu terminal, com o Docker Desktop aberto:

```powershell
$env:JAVA_HOME = 'C:\Users\Thiago\Desktop\Codando Sadesgraça\Hemonorte\.qa-tools\jdk\jdk-21.0.12.1+1'
.\mvnw.cmd clean test
```

(o JDK portátil desta missão está em `.qa-tools`; se você instalar um JDK 21 seu, aponte o JAVA_HOME pro dele). A primeira execução baixa o Maven 3.9.16 e as dependências. Relatórios: `target/surefire-reports` (JUnit) e `target/site/jacoco` (cobertura). Exit 0/1 padrão do Maven, pronto pra CI.

Duas notas desta sessão: aqui o TEMP e o repositório local do Maven foram apontados pro workspace (o ambiente não deixa escrever no home), e o container do Testcontainers não sobe daqui — o acesso ao pipe do Docker é negado a processos que não a própria CLI. Por isso o contextLoads e o que depende de banco real (Flyway, validate, UNIQUE) precisam do seu terminal ou de um runner de CI com Docker nativo.

## 10. Riscos de configuração e persistência

1. V1 e V2 do Flyway já aplicadas são intocáveis — ajuste de esquema vem em migração nova.
2. Senha do banco hardcoded (QA-029) — resolver antes de ambiente compartilhado.
3. `senhaHash` confiado ao cliente (QA-026) — a maior pendência de arquitetura junto com o QA-001.
4. Contrato de duplicidade 400/409 (QA-028).
5. CPF, sorologia, nascimento e gênero são dado sensível (LGPD): coleta mínima e restrição por finalidade — hoje sem auth nenhuma.
6. Auditoria sem emissor (QA-003).
7. Hikari no default e UTC consistente — adequado por ora.

## 11. Decisões de negócio pendentes

1. CPF exige dígito verificador? (QA-007 — produto)
2. Faixa etária mínima/máxima e consentimento? (a parte de data futura os developers já implementaram; falta a regra clínica — clínica/produto)
3. VENCIDA x DESCARTADA: bolsa expirada em análise vai pra qual? `EM_ANALISE→VENCIDA` deve existir? (QA-010 — clínica/produto)
4. Sorologia admite INDETERMINADO com reteste? (QA-016 — clínica)
5. Hash de senha (BCrypt?) e mecanismo de auth (QA-001 e QA-026 — produto/tech)
6. Regras de capacidade de slot e limite por doador (QA-002 — produto)
7. Quem grava a HistoricoAuditoria, e o que é o cron citado no SQL (QA-003 — produto/tech)
8. Vocabulário de identidade de gênero, exposição na API, coleta opcional, nome social (QA-021/QA-022 — clínica/produto)

## 12. Plano por etapas

Etapa 0 (ambiente) — feita: JDK 21, Maven, Docker e acesso GitHub destravados.
Etapa 1 (testes unitários do domínio) — feita: 75 verdes.
Etapa 2 (JaCoCo + primeira medição) — feita: 50% de linhas, domínio a 60,3%.
Etapa 3 (correções dos developers) — QA-024 e QA-025 bloqueiam a API; QA-026, QA-027 e QA-028 na sequência.
Etapa 4 (services + integração real) — testes direto nos services; contextLoads no seu terminal/CI.
Etapa 5 (CI) — GitHub Actions com runner Docker, na develop.
Etapa 6 (sexo/identidade) — depois da decisão 8.

## 13. Sexo e identidade de gênero

O campo hoje: `genero CHAR(1) NOT NULL`, um Char M/F validado na entidade — e agora também no DTO de cadastro (`@Pattern [MF]`, o mesmo que causa o QA-024). Sem enum próprio, sem consumidor funcional: nenhuma regra lê o campo (a Triagem usa só peso e três booleans). A camada HTTP nova não mudou a discussão: continua tecnicamente barato mexer porque não há consumidor pra quebrar.

A recomendação não muda: separar sexo (biológico/registral, a coluna atual, que os protocolos clínicos consultam) de identidade de gênero (campo novo, opcional, com enum de vocabulário definido pelo produto e pela clínica — proposta inicial: HOMEM_CIS, HOMEM_TRANS, MULHER_CIS, MULHER_TRANS, NAO_BINARIO, OUTRO, NAO_INFORMADO). Três regras se mantêm: coleta opcional (quem não manda o campo segue funcionando), nenhum preenchimento automático pra registro existente (NULL, sem inferência) e aptidão nunca pela identidade isoladamente. A migration fica V3 agora (a V2 dos developers ocupou o número): `ALTER TABLE doador ADD COLUMN identidade_genero VARCHAR(20);` — aditiva, rollback documentado como DROP em migração futura, filtros tratando NULL explicitamente.

Testes, quando a decisão sair: identidade válida aceita, fora do enum rejeitada, ausência aceita, sexo seguindo M/F — e HOMEM_TRANS com sexo F é válido (independência semântica documentada em teste). Detalhe de ordem: a correção do QA-024 precisa vir antes, senão o cadastro nem roda pra testar.

## 14. Próximas ações

1. Sua revisão desta atualização e autorização dos commits planejados: um com `pom.xml` + os 12 arquivos de teste, outro exclusivo com `relatorio-de-testes.md` — ambos com a mensagem combinada, na develop.
2. Developers: QA-024 (crítico) e QA-025 — a suíte marca os dois sozinha.
3. Rodar `.\mvnw.cmd clean test` no seu terminal pra validar o contextLoads (Flyway V1→V2 + validate do Hibernate).
4. Decisões da seção 11 — a 8 destrava a funcionalidade de gênero.

Ressalva final: desta vez tem execução — os 90 testes correram no Maven 3.9.16 + Temurin 21.0.12.1 + Boot 4.1.1 nesta máquina, e os números da seção 8 são do JaCoCo dessa rodada. O que não rodou: o contextLoads e tudo que exige banco real, porque o Docker não é alcançável desta sessão (pipe negado a processos que não a CLI). Nada aqui é estimativa: veio de log ou de código.
