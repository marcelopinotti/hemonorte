# Modelagem do Sistema Hemonorte

Sistema de doação de sangue e agendamento hemoterápico.

Este documento consolida a modelagem arquitetural, lógica e comportamental do sistema. O diagrama de classes foi derivado das entidades Kotlin/JPA e do esquema SQL presentes no projeto.

## 1. Arquitetura em camadas

![Arquitetura em camadas](modelagem/arquitetura-em-camadas.png)

## 2. Modelo lógico do banco de dados

![Modelo lógico do banco de dados](modelagem/modelo-logico-banco.png)

## 3. Diagrama de classes

![Diagrama de classes](diagrama-de-classes.png)

O vínculo entre `EstoqueMinimo` e `BolsaSangue` é conceitual, feito pela tipagem sanguínea. O modelo atual não possui chave estrangeira ou associação JPA entre essas entidades.

## 4. Diagrama de sequência do agendamento

```mermaid
sequenceDiagram
    actor Doador
    participant SlotController
    participant SlotService
    participant AgendamentoController
    participant AgendamentoService
    participant AptidaoService
    participant AgendamentoRepository

    Doador->>SlotController: GET /slots?inicio&fim (com offset)
    SlotController->>SlotService: listarPorPeriodo(inicio, fim)
    SlotService->>AgendamentoRepository: contar PENDENTE por slot
    SlotService-->>Doador: horários, ocupação e vagas

    Doador->>AgendamentoController: POST /agendamentos
    AgendamentoController->>AgendamentoService: realizarAgendamento(doadorId, slotId)
    AgendamentoService->>AptidaoService: verificarAptidao(doador)
    AptidaoService->>AgendamentoRepository: buscar histórico CONCLUIDO
    Note over AptidaoService: avaliar intervalo e limite móvel de 365 dias<br/>em America/Fortaleza
    alt impedimento calculável
        AptidaoService-->>Doador: motivo e próxima data permitida
    else apto e há vaga
        AgendamentoService->>AgendamentoRepository: salvar PENDENTE
        AgendamentoRepository-->>Doador: 201 Created
    end
```

Os parâmetros acadêmicos estão registrados em [Parâmetros da aptidão](parametros-clinicos-aptidao.md). A pré-avaliação não substitui a triagem presencial.
