# Hemonorte

## Requisitos

- JDK 17 (Temurin recomendado).
- Docker Engine/Desktop com Docker Compose v2.
- Maven Wrapper incluído no projeto (`mvnw`/`mvnw.cmd`), portanto não é necessário instalar o Maven.

## Desenvolvimento local

Para executar os testes e gerar o build:

```powershell
.\mvnw.cmd clean verify
```

Para executar somente os testes:

```powershell
.\mvnw.cmd test
```

O teste `contextLoads` inicializa um PostgreSQL real com Testcontainers usando a imagem fixa `postgres:16-alpine`. Por isso, o Docker precisa estar em execução para a suíte completa.

Para subir a aplicação e o PostgreSQL com Docker Compose:

```powershell
docker compose up --build
```

Defina um segredo próprio fora do repositório antes de uso fora do ambiente local:

```powershell
$jwtBytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$env:JWT_EXPIRATION = "86400000"
docker compose up --build
```

`JWT_EXPIRATION` usa milissegundos. O valor padrão é 24 horas. Na primeira inicialização sem funcionário `ADMIN`, a aplicação cria matrícula `admin` com senha aleatória e mostra essa senha uma vez no log. Troque a senha após entrar.

## Autenticação JWT

Login de funcionário:

```http
POST /auth/login/funcionario
Content-Type: application/json

{"matricula":"admin","senha":"senha-exibida-no-log"}
```

Login de doador:

```http
POST /auth/login/doador
Content-Type: application/json

{"cpf":"12345678901","senha":"sua-senha"}
```

Envie o token retornado em todas as rotas protegidas:

```http
Authorization: Bearer eyJ...
```

O JWT contém `sub`, `tipo`, `perfil` para funcionários, `iat`, `exp` e `iss`. Doadores usam `/api/doador/me`; a API ignora IDs externos para operações próprias. Funcionários seguem perfis `TRIAGEM`, `COLETA`, `LABORATORIO` e `ADMIN`. Token de funcionário desativado retorna 401 mesmo antes da expiração.

O esquema OpenAPI também é protegido. Consulte com `Authorization: Bearer {token}` em `/v3/api-docs`.

A API fica disponível em `http://localhost:8080` e o PostgreSQL em `localhost:5433`. Para parar os serviços:

```powershell
docker compose down
```

Os dados do PostgreSQL ficam no volume nomeado `hemonorte_postgres_data`. Para remover também os dados persistidos:

```powershell
docker compose down -v
```

Os relatórios são gerados em `target/surefire-reports/` (JUnit) e `target/site/jacoco/` (JaCoCo).

## CI

O GitHub Actions executa `./mvnw clean verify` em pull requests e pushes para `develop` e `main`. O job usa JDK 17, Docker pré-disponível no runner do GitHub para o Testcontainers e publica os relatórios JUnit e JaCoCo como artefatos da execução.

## Modelagem

[Documento completo de modelagem](docs/modelagem-hemonorte.pdf)

![Diagrama de classes do domínio](docs/diagrama-de-classes.png)

[Versão vetorial (SVG)](docs/diagrama-de-classes.svg) · [Fonte PlantUML](docs/diagrama-de-classes.puml)
