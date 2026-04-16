# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added - Relatórios & PDF (Gotenberg)

#### 📄 Geração de PDF com Gotenberg

- Serviço `GotenbergPdfService` — converte HTML (Thymeleaf) em PDF via Gotenberg 8
- Record `PdfOptions` com fábricas `a4()` e `a4Landscape()` para configuração de página
- Exceção `GotenbergException` para erros de conversão
- 8 templates PDF em `templates/pdf/` com tema dark cinema CineLog (CSS inline)
- Fragment base (`fragments/base.html`) com estilos reutilizáveis
- 9 novos endpoints de download PDF no `ReportController`:
    - `GET /api/v1/reports/{type}/pdf` — user reports (8 tipos)
    - `GET /api/v1/admin/reports/platform/pdf` — admin report (landscape)
- Suporte a parâmetros de query (`days`, `limit`, `genre`) nos endpoints PDF
- Container Gotenberg no `docker-compose.yml` (porta 3001)

#### 📧 Anexo de PDF nos E-mails

- Método `sendHtmlWithAttachment()` no `EmailService` com `ByteArrayResource`
- Helper `sendWithOptionalPdf()` no `ReportEmailService` com fail-safe
- Configuração `cinelog.reports.pdf.attach-to-email` (default: `false`)
- Se Gotenberg estiver offline, o e-mail é enviado normalmente sem anexo

#### 🎬 3 Novos Tipos de Relatório

- **Top Actors** — atores com filmes mais bem avaliados
- **New Releases** — novos títulos adicionados recentemente
- **Genre Spotlight** — análise profunda de um gênero
- Cada tipo com 3 modos: preview (JSON), e-mail (POST), PDF (GET)

#### 🧪 Testes

- `GotenbergPdfServiceTest` — 10 testes unitários com mock `ExchangeFunction`

### Added - PR6: Features de Negócio (2026-01-11)

#### 🎯 5 Novas Features de Negócio

**1. User Insights - Estatísticas Agregadas do Usuário**

- Endpoint `GET /api/users/{userId}/insights` para visualizar estatísticas consolidadas
- Read model CQRS (`user_stats`) com 7 índices para performance
- Atualização em tempo real via Kafka consumer (UserStatsUpdater)
- Inbox Pattern para processamento idempotente de eventos
- Cache 15 minutos para otimização de queries
- Métricas: total de entries, filmes, séries, média de ratings, tempo assistido

**2. Media Popularity - Rankings e Tendências**

- Endpoint `GET /api/media/popular/trending?days=7&limit=10` para mídias em tendência
- Endpoint `GET /api/media/popular/top-rated?minViews=50&limit=20` para top-rated
- Endpoint `GET /api/media/popular/{mediaId}` para estatísticas específicas
- Read model CQRS (`media_popularity`) com 6 índices
- Atualização em tempo real via Kafka consumer (MediaPopularityUpdater)
- Inbox Pattern para deduplicação automática
- Cache 30 minutos para trending/top-rated
- Cálculo de trending score baseado em recent views

**3. Media Search - Busca Avançada com Filtros**

- Endpoint `GET /api/media/search` com 7 filtros dinâmicos (title, genre, year, type, rating)
- Endpoint `GET /api/media/search/autocomplete?query=inc` para busca rápida
- Specification Pattern (DDD) para queries type-safe e compostas
- Cache 10 minutos
- Suporte a partial matching (case-insensitive)
- Validação de parâmetros (limit 1-100)

**4. Watch Progress - Progresso de Séries**

- Endpoint `GET /api/watch-progress/user/{userId}` para listar progresso
- Endpoint `POST /api/watch-progress` para atualizar (season, episode)
- Endpoint `DELETE /api/watch-progress/{progressId}` para remover
- Value Object `SeriesProgress` (imutável, self-validating)
- Validações: currentSeason > 0, currentEpisode > 0, currentSeason <= totalSeasons
- Cache 20 minutos (invalidado em POST/DELETE)
- Cálculo de percentual completo

**5. Recommendations - Sistema de Recomendações Personalizadas**

- Endpoint `GET /api/users/{userId}/recommendations?limit=20` (automático)
- Endpoint `GET /api/users/{userId}/recommendations/{strategy}` (específico)
- Endpoint `GET /api/users/{userId}/recommendations/strategies` (disponíveis)
- Strategy Pattern (GoF) com 3 algoritmos:
    - **ContentBasedStrategy**: Baseado em gêneros/atributos das mídias assistidas
    - **CollaborativeStrategy**: Baseado em usuários com gostos similares
    - **HybridStrategy**: Combinação weighted (60% content + 40% collaborative)
- Fallback automático: Hybrid → Content → Collaborative → Empty
- Cache 15 minutos
- Validação de limit (1-100)

#### 🏗️ Infraestrutura e Arquitetura

**CQRS (Command Query Responsibility Segregation)**

- Separação de read models (`user_stats`, `media_popularity`) e write model (`WatchEntry`)
- Consistência eventual via Kafka events
- Otimização de queries com índices específicos

**Inbox Pattern (Idempotência)**

- Tabela `inbox_events` para deduplicação de eventos Kafka
- Processamento transacional (atomic: Inbox + Domain update)
- Auditoria completa (eventId, eventType, processedAt)
- 2 consumers: UserStatsUpdater, MediaPopularityUpdater

**Strategy Pattern (GoF)**

- Interface `RecommendationStrategy` com 3 implementações
- Seleção dinâmica de algoritmo
- Fallback chain para robustez
- Open/Closed Principle (fácil adicionar novos algoritmos)

**Specification Pattern (DDD)**

- Queries dinâmicas type-safe para Media Search
- Composição fluent: `where().and().and()`
- Evita SQL injection e erros de compilação

**Value Object Pattern (DDD)**

- `SeriesProgress` como record imutável
- Validações no construtor (fail-fast)
- Equals/hashCode automático

#### 📊 Migrations

- `V008__create_user_stats_table.sql` - Read model para User Insights (7 índices)
- `V009__create_media_popularity_table.sql` - Read model para Media Popularity (6 índices)
- `V010__add_series_progress_to_watchentry.sql` - Embeddable para Watch Progress

#### 💾 Cache Strategy

| Feature                      | TTL   | Key Pattern                             | Eviction                |
| ---------------------------- | ----- | --------------------------------------- | ----------------------- |
| User Insights                | 15min | `userId`                                | Manual (on Kafka event) |
| Media Popularity - Trending  | 30min | `"trending_" + days + limit`            | Time-based              |
| Media Popularity - Top Rated | 30min | `"topRated_" + minViews + limit`        | Time-based              |
| Media Search                 | 10min | `hash(params)`                          | Time-based              |
| Watch Progress               | 20min | `userId`                                | Manual (POST/DELETE)    |
| Recommendations              | 15min | `userId + "_" + limit + "_" + strategy` | Time-based              |

#### 🔄 Kafka Integration

**Topics:**

- `watchentry-events` - Eventos de WatchEntry (CREATED, UPDATED, DELETED)

**Consumers com Inbox Pattern:**

- `UserStatsUpdater` - Atualiza `user_stats` table
- `MediaPopularityUpdater` - Atualiza `media_popularity` table

**Event Flow:**

```
WatchEntry CRUD → Kafka Topic → Consumer → Check Inbox (eventId)
→ [Skip if exists] → Process → Save to Inbox + Update Domain (atomic)
```

#### 🧪 Testes

**Criados (55+ testes):**

- `RecommendationServiceTest` - 25 testes (strategy selection, limit validation, fallback)
- `SeriesProgressTest` - 30 testes (validations, immutability, business logic, edge cases)
- `MediaSearchServiceTest` - 9 testes (specification pattern, query composition)

#### 📚 Documentação

**Guias Criados (11 documentos, 3600+ linhas):**

- `PR6_QUICK_REFERENCE.md` (500+ linhas) - API completa com exemplos curl
- `PR6_FINAL_SUMMARY.md` (450+ linhas) - Resumo executivo
- `PR6_FEATURES_COMPLETE.md` (750+ linhas) - Detalhamento técnico
- `PR6_PHASE1_CQRS_COMPLETE.md` (~200 linhas)
- `PR6_PHASE2_USER_INSIGHTS_COMPLETE.md` (~250 linhas)
- `PR6_PHASE3_MEDIA_POPULARITY_COMPLETE.md` (~300 linhas)
- `PR6_PHASE4_MEDIA_SEARCH_COMPLETE.md` (~280 linhas)
- `PR6_PHASE5_WATCH_PROGRESS_COMPLETE.md` (~320 linhas)
- `PR6_PHASE6_RECOMMENDATIONS_COMPLETE.md` (~550 linhas)
- `PR6_PHASE7_PROGRESS_REPORT.md` (~300 linhas)
- `INDEX.md` atualizado com seção PR6

#### 📈 Estatísticas

```
Arquivos Criados:     38 (~4500 linhas)
Endpoints REST:       16 novos
Tabelas:              3 (CQRS read models)
Migrations:           3
Kafka Consumers:      2 (com Inbox Pattern)
Design Patterns:      5 (CQRS, Inbox, Strategy, Specification, Value Object)
Testes Unitários:     55+ criados
Documentação:         11 arquivos (3600+ linhas)
Cache Layers:         6 configurados
Build Status:         ✅ SUCCESS
```

#### 🎯 Princípios Aplicados

- ✅ **Clean Architecture**: Separação clara de camadas (domain, application, infra)
- ✅ **SOLID Principles**: Especially OCP (Strategy Pattern), SRP (Value Object)
- ✅ **Domain-Driven Design**: Specification Pattern, Value Objects, Ubiquitous Language
- ✅ **Event-Driven Architecture**: Kafka + CQRS + Inbox Pattern
- ✅ **Performance Optimization**: 6 cache layers, índices estratégicos
- ✅ **Idempotency**: Inbox Pattern garante processamento único de eventos
- ✅ **Fail-Safe**: Fallback automático no sistema de recomendações

#### 🔗 Links da Documentação

- [Quick Reference Guide](./docs/PR6_QUICK_REFERENCE.md) - Exemplos práticos de uso
- [Resumo Executivo](./PR6_FINAL_SUMMARY.md) - Overview completo do PR6
- [Features Completas](./docs/PR6_FEATURES_COMPLETE.md) - Detalhamento técnico
- [Documentação Geral](./docs/INDEX.md#-pr6-features-de-negócio--novo) - Seção PR6

---

## [0.6.0] - 2026-01-10

### Added - PR5: Design Patterns Avançados

- State Pattern para gerenciamento de estados do WatchEntry
- Template Method Pattern para processamento de eventos
- Testes completos para todos os estados e transições
- Documentação detalhada dos patterns implementados

### Changed

- Refatoração do WatchEntry para usar State Pattern
- Melhoria na arquitetura de eventos

---

## [0.5.0] - 2026-01-09

### Added - PR4: Observability Stack Completa

- Integração com Prometheus para métricas
- Grafana dashboards para visualização
- Loki para agregação de logs
- Tempo para distributed tracing
- OpenTelemetry para instrumentação
- Alertas configurados

### Changed

- Logs estruturados com Logback
- Métricas customizadas por feature

---

## [0.4.0] - 2026-01-08

### Added - PR3: DLQ e Event Registry

- Dead Letter Queue (DLQ) para eventos Kafka falhados
- Event Registry para auditoria de eventos
- Retry automático com backoff exponencial
- Métricas de eventos processados/falhados

---

## [0.3.0] - 2026-01-07

### Added - PR2: Outbox Pattern Robusto

- Outbox Pattern para publicação confiável de eventos
- Scheduler para processar outbox pendentes
- Testes de integração com Testcontainers
- Documentação completa do pattern

---

## [0.2.0] - 2026-01-06

### Added - PR1: Event Envelope

- Event Envelope wrapper para eventos Kafka
- Metadata (eventId, timestamp, correlationId, userId)
- Versionamento de eventos (schemaVersion)
- Serialização/deserialização JSON

---

## [0.1.0] - 2025-12-15

### Added - Versão Inicial

- CRUD básico de WatchEntry (MOVIE/SERIES)
- Integração com MySQL
- Kafka producer básico
- Docker Compose para desenvolvimento
- Swagger UI para documentação da API

---

[Unreleased]: https://github.com/marcusPrado02/cinelog/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/marcusPrado02/cinelog/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/marcusPrado02/cinelog/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/marcusPrado02/cinelog/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/marcusPrado02/cinelog/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/marcusPrado02/cinelog/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/marcusPrado02/cinelog/releases/tag/v0.1.0
