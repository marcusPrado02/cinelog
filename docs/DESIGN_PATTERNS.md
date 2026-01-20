# Design Patterns - Guia Completo

**Versão**: 1.0  
**Data**: 04/01/2026  
**Autor**: CineLog Team  
**Status**: ✅ Implementado

---

## 📋 Sumário

1. [Visão Geral](#visão-geral)
2. [Strategy Pattern](#strategy-pattern)
3. [State Pattern](#state-pattern)
4. [Template Method Pattern](#template-method-pattern)
5. [Integração entre Patterns](#integração-entre-patterns)
6. [Benefícios Alcançados](#benefícios-alcançados)
7. [Manutenção e Evolução](#manutenção-e-evolução)
8. [Referências](#referências)

---

## Visão Geral

### Contexto

O CineLog implementa **3 Design Patterns clássicos do GoF** (Gang of Four) para resolver problemas específicos de design e manutenibilidade:

1. **Strategy Pattern**: Sistema de recomendação de mídias
2. **State Pattern**: Ciclo de vida de entradas da watchlist
3. **Template Method Pattern**: Validação específica por tipo de mídia

### Princípios SOLID Aplicados

Todos os patterns implementados seguem os princípios SOLID:

-   **S** - Single Responsibility: Cada classe tem uma responsabilidade única
-   **O** - Open/Closed: Aberto para extensão, fechado para modificação
-   **L** - Liskov Substitution: Subtipos podem substituir tipos base
-   **I** - Interface Segregation: Interfaces focadas e coesas
-   **D** - Dependency Inversion: Dependência de abstrações, não implementações

### Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  CreateMediaService (usa Template Method)            │  │
│  │  RecommendMediaUseCase (usa Strategy)                │  │
│  │  WatchEntry Services (usam State)                    │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Design Patterns:                                     │  │
│  │  • RecommendationStrategy (Strategy)                 │  │
│  │  • WatchEntryStatus (State)                          │  │
│  │  • MediaValidator (Template Method)                  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Strategy Pattern

### Propósito

Definir uma família de algoritmos de **recomendação de mídias**, encapsular cada um deles e torná-los intercambiáveis.

### Problema Resolvido

**Antes**: Lógica de recomendação monolítica no service, difícil de testar e estender.

**Depois**: Estratégias intercambiáveis, fácil adicionar novos algoritmos.

### Estrutura

```
┌─────────────────────────────────────────────────────────┐
│        <<interface>>                                     │
│     RecommendationStrategy                               │
│  + recommend(userId, limit): List<Media>                │
│  + getStrategyName(): String                            │
│  + isApplicable(userId): boolean                        │
└─────────────────────────────────────────────────────────┘
                    ▲
                    │
        ┌───────────┼───────────┐
        │           │           │
┌───────┴─────┐ ┌──┴──────┐ ┌──┴──────────────┐
│   Content   │ │  Collab │ │     Hybrid      │
│    Based    │ │ orative │ │ (70/30 mix)     │
│  Strategy   │ │ Strategy│ │   Strategy      │
└─────────────┘ └─────────┘ └─────────────────┘
```

### Implementação

#### Interface Base

```java
public interface RecommendationStrategy {
    List<Media> recommend(Long userId, int limit);
    String getStrategyName();
    boolean isApplicable(Long userId);
}
```

#### Estratégias Concretas

**1. ContentBasedRecommendationStrategy**

Filtra mídias similares baseado em:

-   Mesmo gênero
-   Mesmo tipo (MOVIE/SERIES)
-   Avaliação mínima (>= 7.0)

```java
@Component
@Primary
public class ContentBasedRecommendationStrategy
    implements RecommendationStrategy {

    @Override
    public List<Media> recommend(Long userId, int limit) {
        // Busca mídias que o usuário gostou
        // Filtra por gêneros similares
        // Ordena por popularidade
    }
}
```

**2. CollaborativeRecommendationStrategy**

Filtra baseado em usuários similares:

-   Busca usuários com gostos semelhantes
-   Recomenda o que eles gostaram
-   Evita duplicatas

```java
@Component
public class CollaborativeRecommendationStrategy
    implements RecommendationStrategy {

    @Override
    public List<Media> recommend(Long userId, int limit) {
        // Busca usuários similares
        // Agrega preferências
        // Remove já assistidas
    }
}
```

**3. HybridRecommendationStrategy**

Combina ambas as estratégias:

-   70% Content-Based
-   30% Collaborative

```java
@Component
public class HybridRecommendationStrategy
    implements RecommendationStrategy {

    private final ContentBasedRecommendationStrategy contentBased;
    private final CollaborativeRecommendationStrategy collaborative;

    @Override
    public List<Media> recommend(Long userId, int limit) {
        int contentLimit = (int) (limit * 0.7);
        int collabLimit = (int) (limit * 0.3);

        List<Media> recommendations = new ArrayList<>();
        recommendations.addAll(contentBased.recommend(userId, contentLimit));
        recommendations.addAll(collaborative.recommend(userId, collabLimit));

        return recommendations;
    }
}
```

### Uso

```java
@Service
public class RecommendMediaService implements RecommendMediaUseCase {

    private final RecommendationStrategy strategy;

    // Spring injeta estratégia configurada (@Primary ou @Qualifier)
    public RecommendMediaService(RecommendationStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public List<Media> execute(Long userId, int limit) {
        return strategy.recommend(userId, limit);
    }
}
```

### Extensibilidade

Para adicionar nova estratégia (ex: ML-Based):

1. Criar `MachineLearningStrategy implements RecommendationStrategy`
2. Implementar `recommend()`, `getStrategyName()`, `isApplicable()`
3. Anotar com `@Component`
4. Configurar com `@Qualifier` ou `@Primary`

**Nenhuma modificação em código existente!** ✅

### Benefícios

-   ✅ **Open/Closed**: Novos algoritmos sem modificar existentes
-   ✅ **Testabilidade**: Cada estratégia testada isoladamente
-   ✅ **Flexibilidade**: Troca em runtime ou configuração
-   ✅ **Separação de Responsabilidades**: Cada algoritmo em sua classe

---

## State Pattern

### Propósito

Permitir que `WatchEntry` altere seu comportamento quando seu **estado interno** muda.

### Problema Resolvido

**Antes**: Validações espalhadas com `if/else` complexos para diferentes estados.

**Depois**: Comportamento encapsulado em objetos de estado, transições controladas.

### Estrutura

```
┌──────────────────────────────────────────────────────┐
│           <<interface>>                               │
│        WatchEntryStatus                               │
│  + startWatching(): void                             │
│  + complete(rating): void                            │
│  + drop(reason): void                                │
│  + validateRating(rating): void                      │
│  + isFinal(): boolean                                │
│  + getType(): WatchEntryStatusType                   │
└──────────────────────────────────────────────────────┘
                    ▲
                    │
    ┌───────────────┼───────────────┬──────────────┐
    │               │               │              │
┌───┴────────┐ ┌───┴────────┐ ┌───┴────────┐ ┌──┴────────┐
│  Planning  │ │  Watching  │ │ Completed  │ │  Dropped  │
│   State    │ │   State    │ │   State    │ │   State   │
│ (initial)  │ │  (active)  │ │  (final)   │ │  (final)  │
└────────────┘ └────────────┘ └────────────┘ └───────────┘
```

### Diagrama de Transições

```
       [PLANNING]
          │  │
  start   │  │ drop
Watching  │  │
          ▼  ▼
    [WATCHING]────drop────▶[DROPPED]
          │                   (final)
      complete
          │
          ▼
    [COMPLETED]
       (final)
```

### Estados Concretos

#### 1. PlanningState (Estado Inicial)

```java
public class PlanningState implements WatchEntryStatus {

    @Override
    public void startWatching(WatchEntry entry) {
        // Transição permitida: PLANNING → WATCHING
        entry.setStatus(new WatchingState());
    }

    @Override
    public void complete(WatchEntry entry, Integer rating) {
        // Não pode completar sem assistir
        throw new InvalidStateTransitionException(
            ErrorCode.INVALID_STATE_TRANSITION,
            "Cannot complete without watching first"
        );
    }

    @Override
    public void validateRating(Integer rating) {
        if (rating != null) {
            throw new IllegalArgumentException(
                "Cannot have rating in PLANNING state"
            );
        }
    }

    @Override
    public boolean isFinal() {
        return false;
    }
}
```

#### 2. WatchingState (Estado Ativo)

```java
public class WatchingState implements WatchEntryStatus {

    @Override
    public void complete(WatchEntry entry, Integer rating) {
        // Transição permitida: WATCHING → COMPLETED
        if (rating == null || rating < 1 || rating > 10) {
            throw new IllegalArgumentException(
                "Rating required (1-10) to complete"
            );
        }
        entry.setStatus(new CompletedState());
    }

    @Override
    public void validateRating(Integer rating) {
        // Permite rating parcial (impressões durante exibição)
        if (rating != null && (rating < 1 || rating > 10)) {
            throw new IllegalArgumentException(
                "Rating must be between 1 and 10"
            );
        }
    }
}
```

#### 3. CompletedState (Estado Final - Sucesso)

```java
public class CompletedState implements WatchEntryStatus {

    @Override
    public void complete(WatchEntry entry, Integer rating) {
        // Permite reavaliação
        if (rating == null || rating < 1 || rating > 10) {
            throw new IllegalArgumentException(
                "Rating must be between 1 and 10"
            );
        }
        // Mantém no mesmo estado mas atualiza rating
    }

    @Override
    public boolean isFinal() {
        return true; // Não permite outras transições
    }
}
```

#### 4. DroppedState (Estado Final - Abandono)

```java
public class DroppedState implements WatchEntryStatus {

    @Override
    public void drop(WatchEntry entry, String reason) {
        // Permite atualizar motivo de desistência
        // Mas não muda de estado
    }

    @Override
    public boolean isFinal() {
        return true;
    }
}
```

### Integração com JPA

#### WatchEntry.java

```java
@Entity
public class WatchEntry {

    @Transient // Não persiste o objeto
    private WatchEntryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_type")
    private WatchEntryStatusType statusType = WatchEntryStatusType.PLANNING;

    @PostLoad // Após carregar do banco
    public void reconstructState() {
        this.status = WatchEntryStatusFactory.create(statusType);
    }

    @PrePersist @PreUpdate // Antes de salvar
    public void syncStatusType() {
        if (status != null) {
            this.statusType = status.getType();
        }
    }

    // Métodos de transição delegam para o estado
    public void startWatching() {
        status.startWatching(this);
        syncStatusType();
    }

    public void markAsCompleted(Integer rating) {
        status.complete(this, rating);
        syncStatusType();
    }
}
```

#### WatchEntryStatusFactory.java

```java
public class WatchEntryStatusFactory {

    public static WatchEntryStatus create(WatchEntryStatusType type) {
        return switch (type) {
            case PLANNING -> new PlanningState();
            case WATCHING -> new WatchingState();
            case COMPLETED -> new CompletedState();
            case DROPPED -> new DroppedState();
        };
    }

    public static WatchEntryStatus createInitial() {
        return new PlanningState();
    }
}
```

### Migração de Dados

```xml
<!-- Liquibase: 20260104200000_add_status_type_to_watch_entry.xml -->
<changeSet id="2" author="system">
    <sql>
        UPDATE watch_entry
        SET status_type = CASE
            WHEN watched_at IS NOT NULL AND rating IS NOT NULL
                THEN 'COMPLETED'
            WHEN watched_at IS NOT NULL AND rating IS NULL
                THEN 'WATCHING'
            ELSE 'PLANNING'
        END;
    </sql>
</changeSet>
```

### Benefícios

-   ✅ **Eliminação de Conditionals**: Sem `if/else` para estados
-   ✅ **Validações Contextuais**: Cada estado valida conforme seu contexto
-   ✅ **Transições Controladas**: Impossível transição inválida
-   ✅ **Open/Closed**: Novos estados sem modificar existentes
-   ✅ **Single Responsibility**: Cada estado uma responsabilidade

---

## Template Method Pattern

### Propósito

Definir o esqueleto de um algoritmo de **validação de mídia**, permitindo que subclasses redefinam certos passos sem mudar a estrutura.

### Problema Resolvido

**Antes**: Validação genérica que não distingue filmes de séries.

**Depois**: Validações específicas por tipo mantendo estrutura comum.

### Estrutura

```
┌────────────────────────────────────────────────────┐
│     <<abstract>>                                    │
│  AbstractMediaValidator                             │
│  + validate(media): void         [final]           │
│  # validateCommonRules(media)    [concrete]        │
│  # validateTypeSpecificRules()   [abstract]        │
│  # postValidation(media)         [hook]            │
│  # validateMetadata(media)       [concrete]        │
│  # getMediaTypeName(): String    [abstract]        │
└────────────────────────────────────────────────────┘
                    ▲
                    │
        ┌───────────┴───────────┐
        │                       │
┌───────┴────────┐      ┌──────┴─────────┐
│  MovieValidator│      │SeriesValidator │
│                │      │                │
│ + validate...  │      │ + validate...  │
│   TypeSpecific │      │   TypeSpecific │
└────────────────┘      └────────────────┘
```

### Algoritmo Template

```java
public abstract class AbstractMediaValidator {

    // Template Method (final - não pode ser sobrescrito)
    public final void validate(Media media) {
        // Passo 1: Validações comuns
        validateCommonRules(media);

        // Passo 2: Validações específicas (hook - deve ser implementado)
        validateTypeSpecificRules(media);

        // Passo 3: Normalização (hook - pode ser sobrescrito)
        postValidation(media);

        // Passo 4: Metadados (após normalização)
        validateMetadata(media);
    }

    // Método concreto - regras comuns
    protected void validateCommonRules(Media media) {
        if (media.getTitle() == null || media.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (media.getType() == null) {
            throw new IllegalArgumentException("Type is required");
        }
        // Ano: 1888 (início do cinema) até current + 5
        if (media.getReleaseYear() != null) {
            int minYear = 1888;
            int maxYear = java.time.Year.now().getValue() + 5;
            if (media.getReleaseYear() < minYear
                || media.getReleaseYear() > maxYear) {
                throw new IllegalArgumentException(
                    "Release year must be between " + minYear + " and " + maxYear
                );
            }
        }
    }

    // Hook method - DEVE ser implementado
    protected abstract void validateTypeSpecificRules(Media media);

    // Hook method - PODE ser sobrescrito (opcional)
    protected void postValidation(Media media) {
        // Implementação padrão vazia
        // Subclasses podem sobrescrever para normalizar
    }

    // Método concreto - validação de metadados
    protected void validateMetadata(Media media) {
        // Valida URLs, idioma, overview, TMDB ID
    }

    // Hook method - identificador
    public abstract String getMediaTypeName();
}
```

### Implementações Concretas

#### MovieValidator

```java
public class MovieValidator extends AbstractMediaValidator {

    private static final int CLASSIC_MOVIE_THRESHOLD = 1960;

    @Override
    protected void validateTypeSpecificRules(Media media) {
        // Filmes modernos (>= 1960): ano não pode ser > current + 5
        Integer year = media.getReleaseYear();
        if (year != null && year >= CLASSIC_MOVIE_THRESHOLD) {
            int currentYear = java.time.Year.now().getValue();
            if (year > currentYear + 5) {
                throw new IllegalArgumentException(
                    "Movie release year " + year + " is too far in the future"
                );
            }
        }

        // Recomendações (não obrigatórias)
        checkRecommendations(media);
    }

    private void checkRecommendations(Media media) {
        // Overview recomendado
        // Título original para filmes estrangeiros
        // TMDB ID para integração
    }

    @Override
    protected void postValidation(Media media) {
        // Normaliza título (remove espaços extras)
        if (media.getTitle() != null) {
            String normalized = media.getTitle().trim().replaceAll("\\s+", " ");
            media.setTitle(normalized);
        }

        // Trunca overview se > 5000
        if (media.getOverview() != null && media.getOverview().length() > 5000) {
            media.setOverview(media.getOverview().substring(0, 4997) + "...");
        }
    }

    @Override
    public String getMediaTypeName() {
        return "MOVIE";
    }
}
```

#### SeriesValidator

```java
public class SeriesValidator extends AbstractMediaValidator {

    private static final int MIN_REASONABLE_YEAR_FOR_SERIES = 1950;

    @Override
    protected void validateTypeSpecificRules(Media media) {
        // Ano OBRIGATÓRIO para séries (diferente de filmes clássicos)
        if (media.getReleaseYear() == null) {
            throw new IllegalArgumentException(
                "Release year is required for TV series"
            );
        }

        // Ano razoável (TV comercial começou ~1950)
        int year = media.getReleaseYear();
        if (year < MIN_REASONABLE_YEAR_FOR_SERIES) {
            throw new IllegalArgumentException(
                "Release year " + year + " is too old for a TV series. Minimum: 1950"
            );
        }

        int maxYear = java.time.Year.now().getValue() + 3;
        if (year > maxYear) {
            throw new IllegalArgumentException(
                "Release year " + year + " is too far in the future. Maximum: " + maxYear
            );
        }

        // Título original obrigatório para séries estrangeiras
        validateOriginalTitleForForeignSeries(media);
    }

    private void validateOriginalTitleForForeignSeries(Media media) {
        String language = media.getOriginalLanguage();
        if (language != null
            && !language.equalsIgnoreCase("en")
            && !language.equalsIgnoreCase("eng")
            && !language.equalsIgnoreCase("english")) {

            if (media.getOriginalTitle() == null
                || media.getOriginalTitle().isBlank()) {
                throw new IllegalArgumentException(
                    "Original title is required for non-English TV series. Language: " + language
                );
            }
        }
    }

    @Override
    protected void postValidation(Media media) {
        // Normaliza título
        // Normaliza idioma (lowercase)
        // Trunca overview
    }

    @Override
    public String getMediaTypeName() {
        return "SERIES";
    }
}
```

### Factory para Validators

```java
public final class MediaValidatorFactory {

    private static final AbstractMediaValidator MOVIE_VALIDATOR = new MovieValidator();
    private static final AbstractMediaValidator SERIES_VALIDATOR = new SeriesValidator();

    private MediaValidatorFactory() {
        throw new UnsupportedOperationException("Factory class");
    }

    public static AbstractMediaValidator getValidator(MediaType type) {
        return switch (type) {
            case MOVIE -> MOVIE_VALIDATOR;
            case SERIES -> SERIES_VALIDATOR;
        };
    }

    public static void validate(Media media) {
        getValidator(media.getType()).validate(media);
    }
}
```

### Integração com CreateMediaService

```java
@Service
public class CreateMediaService implements CreateMediaUseCase {

    private final MediaRepositoryPort repo;
    private final MediaPolicy policy;

    @Override
    public Media execute(Media media) {
        // 1. Normalização
        media.normalize();

        // 2. Validação geral (MediaPolicy)
        policy.validateInvariants(media);

        // 3. Validação específica (Template Method) ⭐ NOVO
        MediaValidatorFactory.validate(media);

        // 4. Persistência
        return repo.save(media);
    }
}
```

### Benefícios

-   ✅ **Code Reuse**: Validações comuns escritas uma vez
-   ✅ **Open/Closed**: Novos tipos sem modificar base
-   ✅ **Algoritmo Fixo**: Ordem de validação garantida
-   ✅ **Type-Safe**: Compilador garante implementação de hooks
-   ✅ **Separation of Concerns**: Cada validador trata seu tipo

---

## Integração entre Patterns

### Fluxo Completo: Criar Mídia e Recomendar

```
1. CreateMediaService (Template Method)
   └─> MediaValidatorFactory.validate(media)
       ├─> MovieValidator OU SeriesValidator
       └─> Valida e normaliza

2. repo.save(media)
   └─> Mídia salva no banco

3. RecommendMediaService (Strategy)
   └─> strategy.recommend(userId, limit)
       ├─> ContentBasedStrategy
       ├─> CollaborativeStrategy
       └─> HybridStrategy
           └─> Usa mídias validadas

4. Usuário adiciona à Watchlist (State)
   └─> watchEntry.setStatus(PLANNING)
       └─> PlanningState

5. Usuário começa a assistir
   └─> watchEntry.startWatching()
       └─> Transição: PLANNING → WATCHING

6. Usuário completa
   └─> watchEntry.markAsCompleted(rating)
       └─> Transição: WATCHING → COMPLETED
```

### Padrões Complementares

| Pattern         | Complementa     | Como                          |
| --------------- | --------------- | ----------------------------- |
| Strategy        | Template Method | Recomenda mídias validadas    |
| State           | Strategy        | Watchlist afeta recomendações |
| Template Method | State           | Validação antes de associar   |

---

## Benefícios Alcançados

### 1. Manutenibilidade

-   **Antes**: Código espalhado, difícil localizar lógica
-   **Depois**: Cada padrão em seu pacote, responsabilidade clara

### 2. Testabilidade

-   **Antes**: Testes acoplados, mocks complexos
-   **Depois**: Testes isolados por estratégia/estado/validator

### 3. Extensibilidade

-   **Antes**: Modificar código existente para adicionar features
-   **Depois**: Adicionar novas classes sem tocar nas antigas

### 4. Legibilidade

-   **Antes**: `if/else` aninhados, flags booleanas
-   **Depois**: Objetos com nomes significativos

### 5. Performance

-   **Singletons**: Validators reutilizados
-   **Lazy Loading**: Estados criados sob demanda
-   **Índices DB**: Queries otimizadas por status

---

## Manutenção e Evolução

### Adicionar Nova Estratégia de Recomendação

```java
@Component
public class PopularityBasedStrategy implements RecommendationStrategy {

    @Override
    public List<Media> recommend(Long userId, int limit) {
        // Ordena por número de visualizações
        return mediaRepository.findMostPopular(limit);
    }

    @Override
    public String getStrategyName() {
        return "POPULARITY_BASED";
    }

    @Override
    public boolean isApplicable(Long userId) {
        return true; // Sempre aplicável
    }
}
```

✅ **Nenhuma modificação em código existente!**

### Adicionar Novo Estado

```java
public class OnHoldState implements WatchEntryStatus {

    @Override
    public void resume(WatchEntry entry) {
        entry.setStatus(new WatchingState());
    }

    @Override
    public WatchEntryStatusType getType() {
        return WatchEntryStatusType.ON_HOLD;
    }
}
```

Adicionar ao enum e factory:

```java
enum WatchEntryStatusType {
    PLANNING, WATCHING, ON_HOLD, COMPLETED, DROPPED
}

// Factory
case ON_HOLD -> new OnHoldState();
```

### Adicionar Novo Tipo de Mídia

```java
public class DocumentaryValidator extends AbstractMediaValidator {

    @Override
    protected void validateTypeSpecificRules(Media media) {
        // Documentários devem ter ano
        // Recomendado ter duração
    }

    @Override
    public String getMediaTypeName() {
        return "DOCUMENTARY";
    }
}

// Factory
case DOCUMENTARY -> DOCUMENTARY_VALIDATOR;
```

---

## Referências

### Livros

-   **Design Patterns: Elements of Reusable Object-Oriented Software**  
    Gamma, Helm, Johnson, Vlissides (Gang of Four), 1994

-   **Head First Design Patterns**  
    Freeman & Freeman, O'Reilly, 2004

### Artigos

-   [Refactoring Guru - Design Patterns](https://refactoring.guru/design-patterns)
-   [Source Making - Design Patterns](https://sourcemaking.com/design_patterns)

### Código

-   `src/main/java/com/cine/cinelog/core/domain/strategy/` - Strategy Pattern
-   `src/main/java/com/cine/cinelog/core/domain/state/` - State Pattern
-   `src/main/java/com/cine/cinelog/core/domain/validator/` - Template Method Pattern

### Documentação Adicional

-   [STATE_PATTERN_INTEGRATION_COMPLETE.md](./STATE_PATTERN_INTEGRATION_COMPLETE.md)
-   [TEMPLATE_METHOD_INTEGRATION_COMPLETE.md](./TEMPLATE_METHOD_INTEGRATION_COMPLETE.md)
-   [PR5_DESIGN_PATTERNS_PROGRESS.md](./PR5_DESIGN_PATTERNS_PROGRESS.md)

---

**Versão**: 1.0  
**Última Atualização**: 04/01/2026  
**Próxima Revisão**: PR6 (Documentação e Testes)
