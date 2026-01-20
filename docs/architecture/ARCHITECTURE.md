# 🏗️ Documentação de Arquitetura - CineLog

## Índice

1. [Visão Geral](#visão-geral)
2. [Princípios Arquiteturais](#princípios-arquiteturais)
3. [Arquitetura em Camadas](#arquitetura-em-camadas)
4. [Domain-Driven Design (DDD)](#domain-driven-design-ddd)
5. [Clean Architecture](#clean-architecture)
6. [Padrões de Design](#padrões-de-design)
7. [Diagramas](#diagramas)
8. [Decisões Arquiteturais](#decisões-arquiteturais)

---

## Visão Geral

O **CineLog** é uma aplicação backend moderna construída com **Java 21** e **Spring Boot 3**, seguindo os princípios de **Clean Architecture** e **Domain-Driven Design (DDD)**. O sistema é projetado para ser:

-   **Modular**: Componentes independentes e reutilizáveis
-   **Testável**: Alta cobertura de testes com baixo acoplamento
-   **Escalável**: Preparado para crescimento horizontal
-   **Observável**: Instrumentação completa com métricas, logs e tracing
-   **Resiliente**: Circuit breakers, retries e timeouts configurados
-   **Manutenível**: Código limpo com separação clara de responsabilidades

### Contexto do Sistema

O CineLog é uma plataforma para:

-   Catalogar mídias (filmes, séries, episódios)
-   Registrar visualizações de usuários
-   Gerenciar informações de créditos (atores, diretores)
-   Integrar com APIs externas (TMDB)
-   Fornecer métricas e analytics de consumo

---

## Princípios Arquiteturais

### 1. Separation of Concerns (SoC)

Cada módulo tem uma responsabilidade única e bem definida:

```
core/         → Lógica de negócio pura
features/     → Adaptadores de infraestrutura
shared/       → Cross-cutting concerns
```

### 2. Dependency Inversion Principle (DIP)

O núcleo da aplicação **não depende** de frameworks ou infraestrutura:

```
Domain (core) ← Ports (interfaces) → Adapters (features)
```

### 3. Open/Closed Principle (OCP)

Extensível para novas funcionalidades sem modificar código existente:

-   Novos use cases: implementar interfaces `ports.in`
-   Novos adapters: implementar `ports.out`

### 4. Single Responsibility Principle (SRP)

Cada classe tem apenas uma razão para mudar:

-   **Entities**: Estado do domínio
-   **Use Cases**: Orquestração de lógica
-   **Repositories**: Persistência
-   **Controllers**: Mapeamento HTTP ↔ Domínio

### 5. SOLID + YAGNI + DRY

-   **SOLID**: Todos os princípios aplicados
-   **YAGNI**: Implementação incremental
-   **DRY**: Reutilização via composição

---

## Arquitetura em Camadas

### Diagrama de Camadas

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│              (features/*/web/controller)                 │
│  • REST Controllers                                      │
│  • DTOs (Request/Response)                               │
│  • Exception Handlers                                    │
│  • OpenAPI Documentation                                 │
└─────────────────────────────────────────────────────────┘
                          ↓ (Ports In)
┌─────────────────────────────────────────────────────────┐
│                   APPLICATION LAYER                      │
│               (core/application/usecase)                 │
│  • Use Cases / Services                                  │
│  • Business Logic Orchestration                          │
│  • Transaction Management                                │
│  • Validation & Security                                 │
└─────────────────────────────────────────────────────────┘
                          ↓ (Domain Models)
┌─────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                        │
│                  (core/domain/model)                     │
│  • Entities (Media, User, Genre, etc.)                   │
│  • Value Objects                                         │
│  • Domain Events                                         │
│  • Business Rules                                        │
└─────────────────────────────────────────────────────────┘
                          ↑ (Ports Out)
┌─────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE LAYER                    │
│             (features/*/persistence)                     │
│  • JPA Entities & Repositories                           │
│  • Database Configuration                                │
│  • External API Clients                                  │
│  • Messaging / Events                                    │
└─────────────────────────────────────────────────────────┘
```

### Fluxo de Dados (Request → Response)

```
1. HTTP Request
   ↓
2. Controller (validates DTO)
   ↓
3. Mapper (DTO → Domain Model)
   ↓
4. Use Case (business logic)
   ↓
5. Repository Port (interface)
   ↓
6. Repository Adapter (JPA implementation)
   ↓
7. Database (MySQL via Hibernate)
   ↓
8. Mapper (Domain Model → DTO)
   ↓
9. HTTP Response
```

---

## Domain-Driven Design (DDD)

### Bounded Contexts

O projeto está organizado em contextos delimitados:

#### 1. Media Context

-   **Agregados**: Media, Season, Episode
-   **Entidades**: Media, Season, Episode
-   **Repositórios**: MediaRepository, SeasonRepository, EpisodeRepository

#### 2. User Context

-   **Agregados**: User, WatchEntry
-   **Entidades**: User, WatchEntry, Watchlist
-   **Repositórios**: UserRepository, WatchEntryRepository

#### 3. Content Context

-   **Agregados**: Genre, Person, Credit
-   **Entidades**: Genre, Person, Credit
-   **Repositórios**: GenreRepository, PersonRepository, CreditRepository

### Estrutura de Entidades

```java
// Domain Entity (core/domain/model)
@Getter
@AllArgsConstructor
public class Media {
    private Long id;
    private String title;
    private MediaType type;
    private Integer releaseYear;
    private String originalTitle;
    private String originalLanguage;
    private String overview;
    private Set<Genre> genres;
    private List<Credit> credits;

    // Business logic methods
    public void validate() { ... }
    public boolean isMovie() { ... }
    public boolean isSeries() { ... }
}
```

### Entidades vs Value Objects

| Tipo             | Identidade             | Mutabilidade | Exemplo          |
| ---------------- | ---------------------- | ------------ | ---------------- |
| **Entity**       | Possui ID único        | Mutável      | Media, User      |
| **Value Object** | Sem identidade própria | Imutável     | MediaType, Email |

### Agregados

Um agregado é um cluster de entidades e value objects tratados como unidade:

```
Media (Root)
├── Season (Entity)
│   └── Episode (Entity)
├── Genre (Value Object Reference)
└── Credit (Entity)
    └── Person (Entity Reference)
```

**Regras**:

-   Acesso externo apenas pela raiz (Media)
-   Transações respeitam limites do agregado
-   Referências entre agregados via ID

---

## Clean Architecture

### Hexagonal Architecture (Ports & Adapters)

```
         ┌─────────────────────────────────────┐
         │         EXTERNAL WORLD              │
         │  (HTTP, Database, APIs, Events)     │
         └─────────────────┬───────────────────┘
                           │
         ┌─────────────────▼───────────────────┐
         │          ADAPTERS LAYER             │
         │    (Controllers, Repositories)      │
         └─────────────────┬───────────────────┘
                           │
         ┌─────────────────▼───────────────────┐
         │           PORTS LAYER               │
         │      (Interfaces: in / out)         │
         └─────────────────┬───────────────────┘
                           │
         ┌─────────────────▼───────────────────┐
         │       APPLICATION LAYER             │
         │         (Use Cases)                 │
         └─────────────────┬───────────────────┘
                           │
         ┌─────────────────▼───────────────────┐
         │         DOMAIN LAYER                │
         │    (Entities, Business Rules)       │
         └─────────────────────────────────────┘
```

### Ports

#### Input Ports (Driving Side)

Definem casos de uso que a aplicação expõe:

```java
// core/application/ports/in/CreateMediaUseCase.java
public interface CreateMediaUseCase {
    Media create(CreateMediaCommand command);
}
```

#### Output Ports (Driven Side)

Definem dependências que a aplicação precisa:

```java
// core/application/ports/out/MediaRepositoryPort.java
public interface MediaRepositoryPort {
    Media save(Media media);
    Optional<Media> findById(Long id);
    List<Media> findAll(Pageable pageable);
}
```

### Adapters

#### Driving Adapters (Controllers)

```java
// features/media/web/controller/MediaController.java
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final CreateMediaUseCase createMediaUseCase;
    private final MediaMapper mapper;

    @PostMapping
    public ResponseEntity<MediaResponse> create(@RequestBody CreateMediaRequest request) {
        var command = mapper.toCommand(request);
        var media = createMediaUseCase.create(command);
        return ResponseEntity.ok(mapper.toResponse(media));
    }
}
```

#### Driven Adapters (Repositories)

```java
// features/media/persistence/MediaRepositoryAdapter.java
@Component
public class MediaRepositoryAdapter implements MediaRepositoryPort {

    private final JpaMediaRepository jpaRepository;
    private final MediaEntityMapper mapper;

    @Override
    public Media save(Media media) {
        var entity = mapper.toEntity(media);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

---

## Padrões de Design

### 1. Repository Pattern

Abstrai o acesso a dados:

```java
// Interface (Port)
public interface MediaRepositoryPort {
    Media save(Media media);
    Optional<Media> findById(Long id);
}

// Implementação (Adapter)
@Component
public class MediaRepositoryAdapter implements MediaRepositoryPort {
    // JPA implementation
}
```

### 2. Mapper Pattern (MapStruct)

Conversão entre camadas:

```java
@Mapper(componentModel = "spring")
public interface MediaMapper {
    Media toDomain(MediaEntity entity);
    MediaEntity toEntity(Media domain);
    MediaResponse toResponse(Media domain);
    CreateMediaCommand toCommand(CreateMediaRequest request);
}
```

### 3. Command Pattern

Encapsula operações como objetos:

```java
@Value
public class CreateMediaCommand {
    String title;
    MediaType type;
    Integer releaseYear;
    String overview;
}
```

### 4. Factory Pattern

Criação de objetos complexos:

```java
public class MediaFactory {
    public static Media createMovie(String title, int year) {
        return new Media(null, title, MediaType.MOVIE, year, ...);
    }
}
```

### 5. Strategy Pattern

Diferentes algoritmos para mesmo comportamento:

```java
public interface ValidationStrategy {
    void validate(Media media);
}

public class MovieValidationStrategy implements ValidationStrategy { ... }
public class SeriesValidationStrategy implements ValidationStrategy { ... }
```

### 6. Observer Pattern (Events)

Comunicação desacoplada:

```java
@DomainEvent
public record MediaCreatedEvent(Long mediaId, String title) {}

@EventListener
public void onMediaCreated(MediaCreatedEvent event) {
    // Handle event
}
```

---

## Diagramas

### C4 Model - Nível 1: Contexto

```
┌──────────────┐
│    User      │
│  (Cliente)   │
└──────┬───────┘
       │ Uses
       ▼
┌──────────────────────────────────────┐
│         CineLog System               │
│  (Gerenciamento de Mídias)           │
└──────┬─────────────────┬─────────────┘
       │                 │
       │ Reads/Writes    │ Fetches data
       ▼                 ▼
┌─────────────┐   ┌──────────────┐
│   MySQL     │   │   TMDB API   │
│  Database   │   │  (External)  │
└─────────────┘   └──────────────┘
```

### C4 Model - Nível 2: Container

```
┌────────────────────────────────────────────────────────┐
│                   CineLog System                       │
│                                                        │
│  ┌──────────────┐        ┌───────────────┐           │
│  │  REST API    │        │  Background   │           │
│  │  (Spring)    │───────▶│  Jobs (Batch) │           │
│  └──────┬───────┘        └───────────────┘           │
│         │                                              │
│         │ Uses                                         │
│         ▼                                              │
│  ┌──────────────┐        ┌───────────────┐           │
│  │  Domain      │───────▶│  Persistence  │           │
│  │  Logic       │        │  (JPA/MySQL)  │           │
│  └──────────────┘        └───────────────┘           │
│                                                        │
│  ┌──────────────┐        ┌───────────────┐           │
│  │ Observability│◀───────│  Redis Cache  │           │
│  │ (OTEL/Prom)  │        │               │           │
│  └──────────────┘        └───────────────┘           │
└────────────────────────────────────────────────────────┘
```

### C4 Model - Nível 3: Componentes

```
┌────────────────────────────────────────────────────────┐
│                    REST API Container                  │
│                                                        │
│  ┌────────────────────────────────────────────────┐   │
│  │         Controllers (Web Layer)                │   │
│  │  • MediaController                             │   │
│  │  • UserController                              │   │
│  │  • WatchEntryController                        │   │
│  └────────────────┬───────────────────────────────┘   │
│                   │ Uses                               │
│                   ▼                                    │
│  ┌────────────────────────────────────────────────┐   │
│  │        Use Cases (Application Layer)           │   │
│  │  • CreateMediaService                          │   │
│  │  • ListMediaService                            │   │
│  │  • UpdateMediaService                          │   │
│  └────────────────┬───────────────────────────────┘   │
│                   │ Uses                               │
│                   ▼                                    │
│  ┌────────────────────────────────────────────────┐   │
│  │         Domain Models (Domain Layer)           │   │
│  │  • Media, Season, Episode                      │   │
│  │  • User, WatchEntry                            │   │
│  │  • Genre, Person, Credit                       │   │
│  └────────────────┬───────────────────────────────┘   │
│                   │ Persisted by                       │
│                   ▼                                    │
│  ┌────────────────────────────────────────────────┐   │
│  │       Repositories (Infrastructure Layer)      │   │
│  │  • MediaRepositoryAdapter                      │   │
│  │  • UserRepositoryAdapter                       │   │
│  └────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

### Diagrama de Sequência: Criar Média

```
Client          Controller       UseCase        Repository      Database
  │                 │               │               │               │
  │─POST /media────▶│               │               │               │
  │                 │               │               │               │
  │                 │─create(cmd)──▶│               │               │
  │                 │               │               │               │
  │                 │               │─validate()────│               │
  │                 │               │               │               │
  │                 │               │─save(media)──▶│               │
  │                 │               │               │               │
  │                 │               │               │─INSERT────────▶│
  │                 │               │               │               │
  │                 │               │               │◀──result──────│
  │                 │               │               │               │
  │                 │               │◀──media───────│               │
  │                 │               │               │               │
  │                 │◀──media───────│               │               │
  │                 │               │               │               │
  │◀──201 Created──│               │               │               │
  │                 │               │               │               │
```

---

## Decisões Arquiteturais

### ADR 001: Arquitetura Hexagonal

**Status**: Aceito

**Contexto**: Necessidade de isolar a lógica de negócio da infraestrutura.

**Decisão**: Adotar arquitetura hexagonal com ports & adapters.

**Consequências**:

-   ✅ Facilita testes unitários
-   ✅ Permite trocar infraestrutura sem impacto
-   ❌ Aumenta número de classes e interfaces

### ADR 002: Spring Boot como Framework

**Status**: Aceito

**Contexto**: Necessidade de framework maduro e bem documentado.

**Decisão**: Utilizar Spring Boot 3 com Java 21.

**Consequências**:

-   ✅ Ecossistema rico
-   ✅ Comunidade ativa
-   ❌ Learning curve para iniciantes

### ADR 003: Liquibase para Migrações

**Status**: Aceito

**Contexto**: Versionamento de schema de banco de dados.

**Decisão**: Liquibase ao invés de Flyway.

**Consequências**:

-   ✅ Suporte a rollback
-   ✅ Formato XML/YAML
-   ❌ Sintaxe mais verbosa

### ADR 004: MapStruct para Mapeamentos

**Status**: Aceito

**Contexto**: Conversão entre DTOs, Entities e Domain Models.

**Decisão**: MapStruct ao invés de ModelMapper.

**Consequências**:

-   ✅ Performance (compile-time)
-   ✅ Type-safe
-   ❌ Requer rebuild após mudanças

---

## Observabilidade

### Três Pilares

#### 1. Logs Estruturados (Logback + Logstash)

```json
{
    "timestamp": "2025-12-10T10:30:00Z",
    "level": "INFO",
    "logger": "com.cine.cinelog.features.media.web.controller.MediaController",
    "message": "Media created successfully",
    "traceId": "abc123",
    "spanId": "def456",
    "userId": "user123",
    "mediaId": "789"
}
```

#### 2. Métricas (Prometheus + Micrometer)

```
# Custom business metrics
media_created_total{type="MOVIE"} 150
media_created_total{type="SERIES"} 80

# JVM metrics
jvm_memory_used_bytes{area="heap"} 524288000
```

#### 3. Tracing (OpenTelemetry + Tempo)

```
Trace: POST /api/v1/media (200ms)
├── Span: MediaController.create (10ms)
├── Span: CreateMediaService.create (50ms)
│   └── Span: MediaRepositoryAdapter.save (40ms)
│       └── Span: MySQL INSERT (30ms)
└── Span: MediaMapper.toResponse (5ms)
```

---

## Performance e Escalabilidade

### Estratégias de Cache

1. **Application-Level Cache** (Redis)
    - Listas de gêneros (TTL: 1 hora)
    - Dados de pessoas (TTL: 30 min)
2. **Database Query Cache** (MySQL)
    - Consultas frequentes
3. **HTTP Response Cache**
    - ETags para recursos imutáveis

### Paginação

Todas as listagens suportam paginação:

```java
GET /api/v1/media?page=0&size=20&sort=title,asc
```

### Índices de Banco

```sql
CREATE INDEX idx_media_type ON media(type);
CREATE INDEX idx_media_release_year ON media(release_year);
CREATE INDEX idx_watch_entry_user_id ON watch_entry(user_id);
```

---

## Segurança

### Autenticação e Autorização

-   **JWT** (Access Token + Refresh Token)
-   **Spring Security** com filtros customizados
-   **BCrypt** para hash de senhas
-   **RBAC** (Role-Based Access Control)

### Proteções Implementadas

-   CORS configurado
-   Rate Limiting (planejado)
-   SQL Injection (JPA/Prepared Statements)
-   XSS (validação de entrada)
-   CSRF (token-based)

---

## Referências

-   [Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
-   [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/ddd/)
-   [Hexagonal Architecture - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
-   [Spring Boot Documentation](https://spring.io/projects/spring-boot)
-   [C4 Model](https://c4model.com/)

---

**Última atualização**: Dezembro 2025
