# Architecture Decision Records (ADRs)

## O que são ADRs?

Architecture Decision Records (ADRs) são documentos que capturam decisões arquiteturais importantes, incluindo o contexto, as alternativas consideradas e as consequências.

## Por que usar ADRs?

-   📝 **Documentação**: Registra o "porquê" das decisões
-   🧠 **Conhecimento**: Preserva conhecimento institucional
-   🤝 **Comunicação**: Facilita discussões sobre arquitetura
-   🔍 **Rastreabilidade**: Permite entender evolução do sistema
-   📚 **Onboarding**: Ajuda novos membros a entender decisões passadas

## Estrutura de um ADR

```markdown
# ADR-XXX: Título da Decisão

## Status

[Proposto | Aceito | Rejeitado | Depreciado | Substituído]

## Contexto

Descreva o contexto e o problema que precisa ser resolvido.

## Decisão

A decisão tomada e como será implementada.

## Alternativas Consideradas

-   Alternativa 1: descrição
-   Alternativa 2: descrição
-   Alternativa 3: descrição

## Consequências

### Positivas

-   Benefício 1
-   Benefício 2

### Negativas

-   Trade-off 1
-   Trade-off 2

## Referências

-   Link 1
-   Link 2
```

## ADRs do Projeto

### ADR-001: Arquitetura Hexagonal

**Status**: Aceito

**Data**: 2025-12-01

**Contexto**: Precisávamos de uma arquitetura que isolasse a lógica de negócio da infraestrutura, facilitando testes e manutenção.

**Decisão**: Adotar Arquitetura Hexagonal (Ports & Adapters) com Clean Architecture.

**Alternativas**:

-   MVC tradicional do Spring
-   Arquitetura em camadas simples
-   Microservices desde o início

**Consequências**:

Positivas:

-   ✅ Domínio isolado e testável
-   ✅ Fácil troca de adapters (DB, APIs)
-   ✅ Melhor organização do código

Negativas:

-   ❌ Mais classes e interfaces
-   ❌ Curva de aprendizado maior
-   ❌ Mais boilerplate inicial

---

### ADR-002: Spring Boot 3 com Java 21

**Status**: Aceito

**Data**: 2025-12-01

**Contexto**: Escolha do framework e versão da linguagem.

**Decisão**: Spring Boot 3.5+ com Java 21.

**Alternativas**:

-   Quarkus
-   Micronaut
-   Spring Boot 2.7 com Java 17

**Consequências**:

Positivas:

-   ✅ Ecossistema maduro e rico
-   ✅ Virtual Threads (Project Loom)
-   ✅ Pattern Matching e Records
-   ✅ Suporte de longo prazo (LTS)

Negativas:

-   ❌ Maior consumo de memória vs Quarkus
-   ❌ Startup time maior vs GraalVM

---

### ADR-003: Liquibase para Migrações

**Status**: Aceito

**Data**: 2025-12-01

**Contexto**: Versionamento e gerenciamento de schema de banco de dados.

**Decisão**: Liquibase com XML changesets.

**Alternativas**:

-   Flyway (SQL-based)
-   JPA DDL auto-generation
-   Scripts SQL manuais

**Consequências**:

Positivas:

-   ✅ Suporte a rollback
-   ✅ Precondições e validações
-   ✅ Formato XML/YAML/JSON
-   ✅ Database-agnostic

Negativas:

-   ❌ Sintaxe mais verbosa que Flyway
-   ❌ Learning curve maior

---

### ADR-004: MapStruct para Mapeamentos

**Status**: Aceito

**Data**: 2025-12-01

**Contexto**: Conversão entre DTOs, Entities e Domain Models.

**Decisão**: MapStruct com geração em compile-time.

**Alternativas**:

-   ModelMapper (runtime reflection)
-   Conversões manuais
-   Dozer

**Consequências**:

Positivas:

-   ✅ Performance (compile-time)
-   ✅ Type-safe
-   ✅ Erros em tempo de compilação
-   ✅ Código gerado visível

Negativas:

-   ❌ Requer rebuild após mudanças
-   ❌ Configuração inicial mais complexa

---

### ADR-005: JWT para Autenticação

**Status**: Aceito

**Data**: 2025-12-05

**Contexto**: Mecanismo de autenticação stateless para API REST.

**Decisão**: JWT (JSON Web Tokens) com RS256.

**Alternativas**:

-   Session-based authentication
-   OAuth 2.0 com servidor dedicado
-   API Keys

**Consequências**:

Positivas:

-   ✅ Stateless (escalabilidade)
-   ✅ Suporte a refresh tokens
-   ✅ Claims customizáveis
-   ✅ Padrão da indústria

Negativas:

-   ❌ Difícil revogar tokens
-   ❌ Tamanho do token maior
-   ❌ Requer gerenciamento de secrets

---

### ADR-006: MySQL como Banco de Dados

**Status**: Aceito

**Data**: 2025-12-01

**Contexto**: Escolha do banco de dados relacional.

**Decisão**: MySQL 8.0+

**Alternativas**:

-   PostgreSQL
-   MariaDB
-   Oracle

**Consequências**:

Positivas:

-   ✅ Amplamente usado
-   ✅ Performance comprovada
-   ✅ Ferramentas maduras
-   ✅ Cloud-friendly (RDS, Azure DB)

Negativas:

-   ❌ Recursos avançados do PostgreSQL
-   ❌ Licenciamento (GPL vs MIT do PostgreSQL)

---

### ADR-007: Redis para Cache

**Status**: Aceito

**Data**: 2025-12-05

**Contexto**: Necessidade de cache distribuído.

**Decisão**: Redis 7+ para cache e sessões.

**Alternativas**:

-   Memcached
-   Hazelcast
-   Caffeine (local cache)

**Consequências**:

Positivas:

-   ✅ Performance excepcional
-   ✅ Estruturas de dados ricas
-   ✅ Suporte a pub/sub
-   ✅ Persistência opcional

Negativas:

-   ❌ Single-threaded (por instância)
-   ❌ Memória limitada

---

### ADR-008: OpenTelemetry para Observabilidade

**Status**: Aceito

**Data**: 2025-12-08

**Contexto**: Padronização de observabilidade.

**Decisão**: OpenTelemetry para logs, métricas e tracing.

**Alternativas**:

-   Zipkin/Sleuth
-   Jaeger
-   Elastic APM
-   New Relic

**Consequências**:

Positivas:

-   ✅ Vendor-neutral
-   ✅ Padrão da indústria (CNCF)
-   ✅ Suporte a múltiplos backends
-   ✅ Auto-instrumentação

Negativas:

-   ❌ Ainda em evolução
-   ❌ Overhead de performance

---

### ADR-009: Testcontainers para Testes de Integração

**Status**: Aceito

**Data**: 2025-12-08

**Contexto**: Testes de integração com banco de dados real.

**Decisão**: Testcontainers para containers efêmeros.

**Alternativas**:

-   H2/HSQLDB (in-memory)
-   Docker Compose manual
-   Banco compartilhado de testes

**Consequências**:

Positivas:

-   ✅ Ambiente idêntico a produção
-   ✅ Isolamento entre testes
-   ✅ Facilita CI/CD
-   ✅ Suporte a múltiplos serviços

Negativas:

-   ❌ Testes mais lentos
-   ❌ Requer Docker

---

### ADR-010: Monorepo

**Status**: Aceito

**Data**: 2025-12-01

**Contexto**: Organização do código-fonte.

**Decisão**: Manter tudo em um único repositório (monorepo).

**Alternativas**:

-   Multi-repo (microservices separados)
-   Mono-repo com workspaces

**Consequências**:

Positivas:

-   ✅ Refatorações atômicas
-   ✅ Versionamento simplificado
-   ✅ Melhor compartilhamento de código
-   ✅ CI/CD mais simples

Negativas:

-   ❌ Repositório pode crescer muito
-   ❌ Builds mais longos
-   ❌ Permissões mais simples

---

## Como Criar um Novo ADR

### 1. Crie um Arquivo

```bash
# Formato: ADR-XXX-título-da-decisão.md
touch docs/adr/ADR-011-grpc-para-comunicacao-interna.md
```

### 2. Use o Template

```markdown
# ADR-011: gRPC para Comunicação Interna

## Status

Proposto

## Data

2025-12-10

## Contexto

[Descreva o problema e contexto]

## Decisão

[Descreva a decisão]

## Alternativas Consideradas

1. REST
2. GraphQL
3. gRPC

## Consequências

### Positivas

-   Performance superior
-   Type-safe contracts

### Negativas

-   Curva de aprendizado
-   Debugging mais complexo

## Referências

-   https://grpc.io/
```

### 3. Discuta e Revise

-   Abra um PR
-   Solicite revisão de arquitetos
-   Discuta em reunião de arquitetura
-   Aprove ou rejeite

### 4. Atualize o Status

-   **Proposto** → **Aceito** ou **Rejeitado**
-   Se substituir outro ADR, atualize ambos

---

## Diretrizes

### ✅ Boas Práticas

-   Seja claro e conciso
-   Inclua contexto suficiente
-   Liste alternativas consideradas
-   Documente trade-offs
-   Mantenha histórico (não delete ADRs antigos)

### ❌ Evite

-   Decisões óbvias ou triviais
-   Falta de contexto
-   Ausência de alternativas
-   Decisões reversíveis facilmente

---

## Referências

-   [ADR GitHub Organization](https://adr.github.io/)
-   [Documenting Architecture Decisions - Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
-   [ADR Tools](https://github.com/npryce/adr-tools)

---

**Última atualização**: Dezembro 2025
