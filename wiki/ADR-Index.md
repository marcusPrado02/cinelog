# 📋 ADR Index

> Architecture Decision Records — decisões técnicas documentadas do CineLog.

---

## O que são ADRs?

**Architecture Decision Records (ADR)** são documentos curtos que capturam decisões de arquitetura significativas, junto com seu contexto e consequências. Seguimos o formato proposto por Michael Nygard.

### Estrutura de um ADR

```
# ADR-NNN: Título

## Status
Accepted | Deprecated | Superseded

## Contexto
O problema ou necessidade que motivou a decisão.

## Decisão
A decisão tomada e suas justificativas.

## Consequências
Impactos positivos e negativos da decisão.
```

---

## Índice de ADRs

| # | Título | Status | Data |
|---|---|---|---|
| [ADR-001](#adr-001) | Arquitetura Hexagonal (Ports & Adapters) | ✅ Accepted | 2025-01 |
| [ADR-002](#adr-002) | Spring Boot 3.x + Java 21 | ✅ Accepted | 2025-01 |
| [ADR-003](#adr-003) | Liquibase para Migrações de Banco | ✅ Accepted | 2025-01 |
| [ADR-004](#adr-004) | MapStruct para Mapeamento de Objetos | ✅ Accepted | 2025-01 |
| [ADR-005](#adr-005) | JWT para Autenticação | ✅ Accepted | 2025-01 |
| [ADR-006](#adr-006) | Kafka + Outbox + Idempotent Consumer | ✅ Accepted | 2025-01 |
| [ADR-007](#adr-007) | Redis para Cache Distribuído | ✅ Accepted | 2025-01 |
| [ADR-008](#adr-008) | Implementação de Design Patterns | ✅ Accepted | 2025-01 |
| [ADR-009](#adr-009) | Event Envelope + Versionamento | ✅ Accepted | 2025-01 |
| [ADR-010](#adr-010) | Estrutura Monorepo | ✅ Accepted | 2025-01 |

---

## Resumo dos ADRs

### ADR-001

**Arquitetura Hexagonal (Ports & Adapters)**

- **Contexto**: Necessidade de isolar a lógica de domínio das dependências externas (frameworks, bancos, APIs).
- **Decisão**: Adotar Arquitetura Hexagonal com separação clara em `core` (domain + application), `infrastructure` (adapters) e `web` (controllers).
- **Consequências**: (+) Testabilidade, inversão de dependência, facilidade de trocar adapters. (−) Mais arquivos e indireção.

### ADR-002

**Spring Boot 3.x + Java 21**

- **Contexto**: Escolha de framework e runtime para a aplicação.
- **Decisão**: Spring Boot 3.5.x com Java 21 LTS (records, pattern matching, virtual threads ready).
- **Consequências**: (+) Ecossistema maduro, suporte LTS, performance. (−) Acoplamento ao ecossistema Spring.

### ADR-003

**Liquibase para Migrações de Banco**

- **Contexto**: Necessidade de versionamento de schema de banco de dados.
- **Decisão**: Liquibase com changesets XML, nomenclatura `YYYYMMDDHHmmss_description.xml`.
- **Consequências**: (+) Rastreabilidade, rollback, reprodutibilidade. (−) Complexidade de changeSets XML.

### ADR-004

**MapStruct para Mapeamento de Objetos**

- **Contexto**: Conversão entre entidades de domínio, DTOs e respostas.
- **Decisão**: MapStruct com geração em compile-time via annotation processor.
- **Consequências**: (+) Performance (zero reflection), type-safe, compile-time validation. (−) Mais interfaces de mapeamento.

### ADR-005

**JWT para Autenticação**

- **Contexto**: Autenticação stateless para API REST.
- **Decisão**: JWT (jjwt 0.12.6), access token (15min) + refresh token (7d) com rotation + token family para detecção de roubo.
- **Consequências**: (+) Stateless, escalável, seguro. (−) Sem revogação instant (mitigado com token family).

### ADR-006

**Kafka + Outbox + Idempotent Consumer**

- **Contexto**: Comunicação assíncrona com garantia de entrega.
- **Decisão**: Kafka 3.9.x, Transactional Outbox Pattern (DB + polling), Inbox Pattern para idempotência.
- **Consequências**: (+) Garantia de entrega, idempotência, resiliência. (−) Complexidade eventual consistency.

### ADR-007

**Redis para Cache Distribuído**

- **Contexto**: Performance de consultas frequentes (TMDb, mídias populares).
- **Decisão**: Redis 7 com Spring Cache abstraction, TTLs por tipo de dado, strategy cache-aside.
- **Consequências**: (+) Latência reduzida, menos carga no DB/APIs externas. (−) Complexidade de invalidação.

### ADR-008

**Implementação de Design Patterns**

- **Contexto**: Necessidade de extensibilidade e manutenibilidade em regras de negócio complexas.
- **Decisão**: Strategy (recomendações), State (ciclo de vida WatchEntry), Template Method (validação de mídia).
- **Consequências**: (+) Open/Closed Principle, flexibilidade, testabilidade. (−) Mais classes e abstrações.

### ADR-009

**Event Envelope + Versionamento**

- **Contexto**: Evolução de schemas de eventos sem breaking changes.
- **Decisão**: Envelope com metadata (eventId, type, version, timestamp, source, correlationId) + versionamento semântico.
- **Consequências**: (+) Compatibilidade, rastreabilidade, auditoria. (−) Overhead do envelope.

### ADR-010

**Estrutura Monorepo**

- **Contexto**: Organização do código-fonte do projeto.
- **Decisão**: Monorepo com módulo único Maven, bounded contexts como packages.
- **Consequências**: (+) Simplicidade, refatoração fácil, CI unificado. (−) Pode não escalar para equipes muito grandes.

---

## Como Propor um Novo ADR

1. Crie o arquivo `docs/adr/ADR-NNN-titulo.md`
2. Siga o template (Status, Contexto, Decisão, Consequências)
3. Abra um Pull Request com a proposta
4. Discuta no PR review
5. Após aprovação, atualize o status para `Accepted`
6. Atualize esta página do Wiki

---

## Referências

- [ADR Template — Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
- [adr-tools](https://github.com/npryce/adr-tools)
- [Documenting Architecture Decisions — ThoughtWorks](https://www.thoughtworks.com/radar/techniques/lightweight-architecture-decision-records)
