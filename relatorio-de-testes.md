# Relatório de QA — Hemonorte

Data da análise: 2026-10-12. Base: checkout local da `main` (@24cbd91), idêntico em conteúdo à `develop` (@f3cacea) — o diff entre os dois é vazio. Este documento é a referência de qualidade do projeto: inventário, arquitetura, achados, plano de testes e a proposta de sexo x identidade de gênero.

## 1. Resumo executivo

O Hemonorte está no começo: Spring Boot 4.1.1, Kotlin 2.3.21, Java 21, Maven via wrapper, PostgreSQL com Flyway. A parte boa — e é a maior parte do código que existe — é o domínio: nove entidades com as regras validadas nos construtores (aptidão na triagem, resultado sorológico, máquina de estados da bolsa, transições de agendamento). A parte que ainda não existe é quase tudo em volta: um repository, um endpoint de hello, um service com um método ativo. E um único teste, de contexto, que exige Docker.

O que mais me preocupa, nessa ordem: não existe camada de segurança nenhuma (e o sistema vai guardar CPF, sorologia de HIV/hepatite/Chagas e hash de senha); a capacidade dos slots de agenda não é aplicada em lugar nenhum — overbooking na certa quando o fluxo de agendamento for escrito; a auditoria está modelada e nada grava nela; e a idade do doador não é validada. Somem-se os impedimentos do ambiente: sem JDK 21 na máquina, Docker sem confirmação e acesso ao GitHub quebrado, isso pode ser conferido com mais confiabilidade quando o codido estiver pronto.

Recomendo começar pelos testes unitários do domínio. Não dependem de nada disso, o JUnit 5 já está no projeto, e a regra mais valiosa (aptidão de doador) é a mais fácil de testar.

## 2. Inventário técnico

| Item | Valor |
|---|---|
| Linguagem | Kotlin 2.3.21 (`pom.xml:31`) |
| Java | 21 (`pom.xml:30`) |
| Framework | Spring Boot 4.1.1 (`pom.xml:5-10`) |
| Build | Maven 3.9.16 via wrapper 3.3.4 (only-script) |
| Web | `spring-boot-starter-webmvc`, Jackson 3 Kotlin |
| Persistência | Data JPA, Flyway + `flyway-database-postgresql`, driver PostgreSQL |
| Validação | `spring-boot-starter-validation` |
| Compilador Kotlin | `-Xjsr305=strict`; plugins spring, jpa, all-open |
| Documentação | springdoc-openapi 3.1.0 |
| Testes | `kotlin-test-junit5`, starters `-test`, `spring-boot-testcontainers`, Testcontainers PostgreSQL |
| Segurança | nenhuma dependência no pom |
| Cobertura | JaCoCo não configurado |
| CI | não existe `.github/` |
| Banco | PostgreSQL `localhost:5432/hemonorte`; senha via `POST_PASSWORD` (`application.yaml:6-10`) |
| JPA | `open-in-view: false`; `ddl-auto: validate`; timezone JDBC UTC; `show_sql: true` |
| Servidor | porta 8080; actuator só com `health` |
| Commits | Conventional Commits (`.gitmessage`) |

## 3. Repositório e branches

O checkout está na `main` (@24cbd91), que é a branch protegida — nada foi commitado nela, e nem vai. A `develop` só existe como referência remota local (f3cacea), e o diff entre `main` e `origin/develop` é vazio, com dois merge commits a mais na `main`. Ou seja: este relatório vale igualmente pra develop.



## 4. Arquitetura

```
com.hemonorte
├── HemonorteApplication.kt
├── domain/         Doador, Agendamento, BolsaSangue, Triagem, ExameSorologico,
│                   SlotAgenda, EstoqueMinimo, Funcionario, HistoricoAuditoria
│                   + 5 enums (status, perfil, resultados)
├── application/    DoadorService (listar ativo; cadastrar comentado)
├── infra/          DoadorRepository
└── presentation/   HelloController + 2 DTOs sem uso
```

A separação por camadas está correta, e o domínio é o dono das regras: setters protegidos e comportamento dentro das entidades. Gostei — isso deixa as regras testáveis sem subir nada. Os UNIQUE do esquema batem com as anotações (CPF, e-mail, slot, bolsa↔agendamento, matrícula), e o Flyway junto com o `ddl-auto: validate` pega qualquer desalinhamento entre entidade e schema no boot. Fora isso: `open-in-view` desligado, actuator só com health, senha do banco por variável de ambiente, wrapper reproduzível, `.gitattributes` cuidando do fim de linha. Escolhas certas.

## 5. Testes existentes

Existe um teste: `contextLoads` (`HemonorteApplicationTests.kt`), `@SpringBootTest` com Testcontainers. Ele sobe um PostgreSQL de container (imagem `postgres:latest`) via `@ServiceConnection`, que substitui o datasource — por isso não precisa da `POST_PASSWORD`. Só isso. Nenhuma regra de domínio tem teste, e rodar esse único teste exige Docker.

## 6. Achados

Organizei por tema; a ordem vai do que preocupa mais pro detalhe. Nenhum achado é crítico hoje porque a API ainda não expõe dado — mas o de segurança vira crítico no dia em que aparecer o primeiro endpoint com dado real.

### Segurança (QA-001)

Começo pelo que mais me preocupa. Não existe camada de segurança: nenhuma dependência de security no pom, nenhum filtro. `/api/hello`, `/actuator/health` e o Swagger estão abertos. Hoje o dano é pequeno porque quase não tem endpoint; o problema é o destino do sistema — CPF, hash de senha, resultado de HIV, hepatite e Chagas. Meu conselho: decidir o mecanismo de auth (JWT ou sessão) e mapear o `PerfilFuncionario` antes de escrever qualquer controller que exponha dado. Quando existir, os primeiros testes são 401 sem credencial e 403 pra perfil errado.

### Agenda (QA-002)

O `SlotAgenda` tem capacidade e valida até, mas ninguém consome esse número. Nenhum código conta quantos agendamentos caem num slot, e nada impede o mesmo doador de marcar duas vezes no mesmo horário. Quando o serviço de agendamento for escrito, overbooking é o resultado padrão. A correção natural é invariante no serviço (contagem por slot antes de salvar), com lock ou constraint. Antes disso, decisão de produto: capacidade exata por slot e limite por doador/dia.

### Auditoria (QA-003)

Esse é chato, mas em hemocentro é obrigatório. A `HistoricoAuditoria` está toda modelada — entidade, tabela, FK — e o `transicionarPara` da `BolsaSangue` não registra nada. Busquei usos no código: zero. Descarte e utilização de bolsa sem rastro. Resolver exige decisão de arquitetura (evento de domínio ou serviço de transição) e definir o papel do cron que o comentário do SQL menciona. O teste de integração óbvio: transição gera registro com status anterior, novo, data e funcionário.

### Idade do doador (QA-004)

`dataNascimento` entra direto no construtor, sem checagem (`Doador.kt:44-45`). Aceita data futura e qualquer idade — `LocalDate.now().plusDays(1)` passa. Num sistema de doação isso não pode ficar solto. A faixa usual de protocolo é 16 a 69 anos, mas quem define é a equipe clínica; não dá pra fechar a regra sem eles. O teste unitário fica pronto no dia em que a regra existir: nascimento futuro lança, idades-limite conferidas.

### Dívida de testes (QA-005)

A maior parte do valor deste relatório está aqui. Triagem, ExameSorologico, BolsaSangue, Agendamento e as validações do Doador concentram as regras com consequência clínica — aptidão de doador, aprovação de bolsa — e nenhuma tem um único teste. É o primeiro item da matriz (seção 7) e não depende de ambiente.

### Impedimentos de trabalho (QA-018, QA-019)

Não são defeitos de código, mas travam tudo. QA-019 é o acesso ao GitHub descrito na seção 3, somado ao checkout em `main` e à ausência da branch local da develop. QA-018: não tem JDK 21 na máquina (java fora do PATH, `JAVA_HOME` vazio, nada nos diretórios padrão) e não consegui confirmar o estado do daemon do Docker daqui — precisa checar direto no terminal. Sem essas duas coisas, nenhum teste roda.

### O fluxo de doador (QA-023, QA-008, QA-013)

Os três contam a mesma história: o fluxo está incompleto. QA-023 — o `CadastroDoadorRequest` carrega três campos (nome, email, cpf), mas o construtor do `Doador` exige sete; tipagem, nascimento, gênero e senha ficam de fora. Não tem como materializar um doador válido a partir do request atual. QA-008 — o `listar()` devolve a entidade inteira, `senhaHash` incluso, enquanto o `DoadorResponse`, que existe justamente pra evitar isso, não tem uso nenhum. Nenhum controller chama nada disso hoje, então é risco latente; mas no dia em que alguém serializar aquele retorno, o hash de senha vai pro JSON. E QA-013 — o `cadastrar()` (comentado) checa `existsByCpf` e depois salva: corrida clássica entre checagem e insert. O UNIQUE do banco segura a integridade, que é o importante, mas a exceção bruta de constraint chegaria no cliente sem tratamento. Quando ativar, capturar a `DataIntegrityViolationException` e devolver 409.

### Regras de identificação (QA-006, QA-007)

Dois furos no mesmo tema. QA-006 — a entidade aceita qualquer e-mail que tenha arroba (`Doador.kt:92-95`): `"a@b"` persiste. Enquanto isso o DTO usa `@Email` de verdade (`CadastroDoadorRequest.kt:11`). As camadas discordam entre si; unifica a regra. QA-007 — o CPF é validado como 11 dígitos e só (`Doador.kt:56-58`): `"00000000000"` passa nas duas camadas. Pra sistema de saúde, onde o CPF é a chave do doador, vale exigir o dígito verificador. Como é decisão de produto, ficou na lista de pendências (seção 11), não como bug fechado.

### Contrato de erros (QA-009)

Curto: não tem `@RestControllerAdvice` em lugar nenhum, e o domínio inteiro sinaliza erro com `require()`, que lança `IllegalArgumentException`. Na primeira controller que chamar regra de domínio, validação de negócio vai virar 500. Precisa de um advice mapeando pra 4xx antes que isso aconteça — e de um teste MockMvc garantindo que payload inválido nunca volta 500.

### Máquina de estados da bolsa (QA-010)

O mapa de transições (`BolsaSangue.kt:52-58`) tem dois problemas. Primeiro: uma bolsa em `EM_ANALISE` não pode ir pra `VENCIDA` — se expirar durante a análise, o caminho é `DESCARTADA`. Se o negócio quiser manter a distinção, a semântica dos dois estados precisa ser definida. Segundo: nenhuma transição olha `dataValidade` — dá pra marcar `VENCIDA` uma bolsa que não venceu. A tabela de transições legais/ilegais está na matriz; a regra temporal depende da decisão clínica (item 3 da seção 11).

### Gênero (QA-021, QA-022)

O campo `genero` é um `CHAR(1)` M/F, documentado como "M = masculino, F = feminino" (`Doador.kt:48-51`), sem enum, sem DTO que carregue ou exponha. Não distingue sexo de identidade e não comporta homem trans nem mulher trans (QA-021). E o mais revelador: ninguém consome esse campo — nenhum serviço, controller, regra de triagem ou teste (QA-022). É coleta de dado sem finalidade definida, e em dado sensível isso é problema de LGPD. Análise completa e proposta na seção 13; a decisão é do produto com a equipe clínica.

### Sorologia (QA-016)

Pergunta clínica: o `ResultadoSorologico` só tem POSITIVO e NEGATIVO. Triagem real devolve indeterminado, e aí o fluxo é repetir o exame. Se o laboratório confirmar a necessidade, o cálculo do resultado final (`ExameSorologico.kt:77-86`) também muda. Não mexeria sem ouvir quem interpreta os exames.

### Estágio (QA-020)

Mais retrato do momento do que defeito: das nove entidades, só Doador tem repository. O resto do domínio hoje só é exercitável por teste unitário — o que reforça a prioridade da matriz.

### Outros pontos menores

Num bloco só, porque nenhum merece seção própria. QA-011: o Testcontainers usa `postgres:latest` (`TestcontainersConfiguration.kt:15`) — imagem que deriva; fixar versão (17, por exemplo). QA-012: `show_sql: true` (`application.yaml:20`) — SQL inteiro no stdout, ruído e risco de dado sensível em log. QA-014: `Instant.now()` espalhado nos construtores (SlotAgenda, `Doador.criadoEm`, `ExameSorologico.dataResultado`) — funciona, mas dificulta teste de borda; se um dia doer, injetar `Clock`. QA-015: README de uma linha e o `.gitignore` excluindo `modelagem/`, que parece ser onde mora a documentação de domínio — contexto perdido. QA-017: nenhum logger no código; quando as transições de bolsa existirem de verdade, vai fazer falta.

## 7. Matriz de testes

Prioridade um, tudo unitário puro — sem Spring, sem Docker, sem dependência nova:

| Alvo | O que testar |
|---|---|
| Triagem | peso 49,99 inapta com motivo; 50,00 apta (a fronteira é ≥); 50,01 apta; cada contraindicação isolada inapta com o motivo certo; todas juntas concatenam com "; "; apta → motivo nulo; peso ≤ 0 lança |
| ExameSorologico | três negativos → APROVADO; qualquer positivo nas outras 7 combinações → REPROVADO |
| BolsaSangue | transições legais do mapa; ilegais lançam (inclusive EM_ANALISE→VENCIDA); tipagem inválida rejeitada; minúscula normalizada; validade ≤ coleta rejeitada; coleta+1 dia aceita |
| Agendamento | concluir/cancelar/registrarFalta só de PENDENTE; estados finais lançam; concluir gera comprovante; cancelar não gera |
| Doador | nome em branco lança e aplica trim; e-mail sem @ lança; tipagem case-insensitive válida e inválida rejeitada; gênero fora de M/F rejeitado; CPF com 10/12 dígitos ou letras rejeitado |
| Funcionario | matrícula, nome, e-mail e senha em branco lançam; `desativar()` desativa |
| SlotAgenda | capacidade 0 ou negativa lança; início no passado lança |
| EstoqueMinimo | tipagem inválida lança; quantidade 0/-5 lança; `atualizarQuantidade` inválida mantém o valor anterior (efeito colateral que vale teste) |
| CadastroDoadorRequest | Bean Validation puro: um campo inválido por vez, e o payload válido passa limpo |
| DoadorService.listar | um fake de repository resolve; Mockito já entra com os starters, se preferir |

Depois, com Docker: manter o `contextLoads` (Flyway aplica a V1 e o validate confere entidade contra schema), testar `existsByCpf`/`existsByEmail` no repository e provar que a violação do UNIQUE de CPF vira `DataIntegrityViolationException`.

De API, por enquanto só dá pra cobrir `GET /api/hello` (200 com o corpo exato) e `/actuator/health` (UP). Quando os endpoints de doador aparecerem: 201/400/409, `@Valid` disparado e resposta sem `senhaHash`.

Os unitários acima são também a suíte de regressão — todo bug que fechar ganha teste próprio.

## 8. JUnit 5 e JaCoCo

JUnit 5 já é o motor do projeto (o teste existente usa jupiter e `kotlin-test-junit5` está no pom), então a suíte começa sem mexer em dependência nenhuma. Asserções com `kotlin.test`, que já está no classpath. Marco os testes com `@Tag("unit")`/`@Tag("integration")` desde cedo, pra separar suítes depois sem retrabalho. A ordem que sugiro: Triagem, ExameSorologico, BolsaSangue, Agendamento, Doador, demais entidades, e por último DTOs e serviço — os testes de `cadastrar` dependem do QA-023 e do QA-013.

JaCoCo: nada configurado ainda. A 0.8.13 (ou a estável do dia da implementação) atende Java 21; entra no pom com `prepare-agent` e o `report` amarrado ao ciclo de testes, gerando HTML, CSV e XML em `target/site/jacoco`. Cobertura: ainda não existe número, porque nenhum teste rodou até aqui. Depois de medir do zero, se fizer sentido, limite mínimo só no pacote `domain` e com valor conversado — não global arbitrário. Cobertura é indicador; o que vale é asserção de comportamento.

## 9. Automação via Maven

```powershell
.\mvnw.cmd clean test   # Windows
./mvnw clean test      # Linux / CI
```

Na primeira execução o wrapper baixa o Maven 3.9.16 (cache em `~/.m2`, precisa de rede). O código de saída é o padrão do Maven — 0 ok, 1 falha — o que serve direto pra CI. Relatórios JUnit em `target/surefire-reports`; JaCoCo em `target/site/jacoco`. Os testes sobem o próprio container de PostgreSQL via `@ServiceConnection`: não tocam banco de produção e não pedem `POST_PASSWORD`. Separar unitário de integração mais tarde é só `@Tag` com surefire/failsafe; não vale complicar antes da hora.

## 10. Riscos de configuração e persistência

1. A V1 do Flyway já foi aplicada e é intocável — todo ajuste de esquema vem em migração nova (V2+). Editar V1 quebra o checksum.
2. `POST_PASSWORD` sem default: rodar a aplicação manualmente exige a variável e um Postgres local. Os testes não.
3. `show_sql` ligado e nenhum perfil dev/prod — a configuração única vai precisar de split antes de produção.
4. `senhaHash` aceita qualquer string e não existe dependência de crypto no pom. Quando o auth chegar, BCrypt (`spring-security-crypto`) é o caminho natural.
5. CPF, sorologia e gênero/identidade sem controle de acesso — dado sensível pra efeito de LGPD: coletar o mínimo e restringir por finalidade.
6. Auditoria sem emissor (QA-003) é risco de rastreabilidade.
7. Pool Hikari no default e timezone UTC consistente (TIMESTAMPTZ + LocalDate) — adequado por ora, só acompanhar.

## 11. Decisões de negócio pendentes

1. CPF exige dígito verificador? (QA-007 — produto)
2. Faixa etária mínima/máxima e consentimento? (QA-004 — clínica/produto)
3. VENCIDA x DESCARTADA: bolsa expirada em análise vai pra qual? `EM_ANALISE→VENCIDA` deve existir? (QA-010 — clínica/produto)
4. Sorologia admite INDETERMINADO com reteste? (QA-016 — clínica)
5. Hash de senha (BCrypt?) e mecanismo de auth (QA-001 — produto/tech)
6. Regras de capacidade de slot e limite por doador (QA-002 — produto)
7. Quem grava a HistoricoAuditoria, e o que é o cron citado no SQL (QA-003 — produto/tech)
8. Vocabulário de identidade de gênero, exposição na API, coleta opcional, nome social (QA-021/QA-022 — clínica/produto)

## 12. Plano por etapas

Etapa 0 — destravar o chão: restaurar o acesso ao GitHub, conferir a develop (fetch), criar a branch local, instalar o JDK 21 (Temurin), subir o Docker Desktop e confirmar com `docker info`.
Etapa 1 — testes unitários do domínio, na ordem da matriz. Sem dependência nova.
Etapa 2 — JaCoCo no pom e a primeira medição de cobertura.
Etapa 3 — integração (repository) e API; fixar a versão do Postgres do container.
Etapa 4 — separar suítes com `@Tag`, se o volume justificar.
Etapa 5 — CI no GitHub Actions rodando o mesmo comando, na develop.
Etapa 6 — funcional de sexo/identidade, depois que o produto responder a decisão 8.

## 13. Sexo e identidade de gênero

O campo hoje: `genero CHAR(1) NOT NULL` na tabela doador (`V1__create_schema.sql:13`), um Char M/F validado no construtor (`Doador.kt:107-110`), sem enum, sem DTO que carregue ou exponha, e nenhuma regra que leia — a Triagem usa só peso e três booleans. Ou seja: a mudança de modelo é tecnicamente barata agora, porque não tem consumidor pra quebrar. O significado clínico não está definido em código nenhum, e não vou presumir.

Dá pra modelar de três jeitos. Deixar como está não representa doadores trans — e é isso que motivou a demanda. Estufar o campo atual com mais valores mistura sexo e identidade num conceito só (e o CHAR(1) ainda limita). O terceiro jeito é separar, e é o que recomendo: manter a coluna atual com semântica explícita de sexo biológico/registral — que é a que os protocolos clínicos consultam — e criar um campo opcional de identidade de gênero, com enum próprio. O vocabulário é decisão de produto/clínica; uma proposta de trabalho seria HOMEM_CIS, HOMEM_TRANS, MULHER_CIS, MULHER_TRANS, NAO_BINARIO, OUTRO, NAO_INFORMADO — e cabe discutir também um modelo mais simples, identidade + flag trans.

Três regras que considero inegociáveis na implementação: coleta opcional (nullable; cliente que não manda o campo continua funcionando); nenhum preenchimento automático pra registro existente — quem já está no banco fica com NULL, sem inferência nenhuma; e aptidão nunca é função da identidade isoladamente — critério clínico usa o campo fisiológico. Vale ainda registrar a ideia de nome social pra discussão futura, mesmo fora deste escopo.

Migração aditiva, em arquivo novo (a V1 está aplicada e não se mexe):

```sql
-- V2 - Identidade de gênero (opcional) para Doador
ALTER TABLE doador ADD COLUMN identidade_genero VARCHAR(20);
```

Rollback documentado: V3 com `DROP COLUMN`, porque o Flyway community não tem undo. Novos filtros e relatórios terão que tratar NULL explicitamente. Testes, quando a funcionalidade for aprovada: unitário do Doador (identidade válida aceita, valor fora do enum rejeita, ausência aceita, sexo segue M/F — e HOMEM_TRANS com sexo F é válido; é exatamente a independência dos conceitos, e merece teste próprio); serialização do DTO com e sem o campo; container subindo V1→V2 com registros pré-existentes em NULL; e regressão das validações existentes do Doador.



## 14. Próximas ações


Etapa 0: acesso GitHub, conferir a develop, criar branch local, JDK 21, Docker.
Etapas 1 e 2 (testes unitários e JaCoCo) na sequência.
Responder as decisões da seção 11 — principalmente a 8, que destrava a funcionalidade de gênero.

Ressalva final, que vale pro documento inteiro: nada aqui é resultado de execução. É leitura de código e inspeção do repositório; sem JDK e sem Docker confirmado, nenhum teste rodou, então não existe métrica de cobertura ou de comportamento pra citar. Quando a Etapa 0 sair, os números entram.
