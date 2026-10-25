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
