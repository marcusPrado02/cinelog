# ADR-001: Arquitetura Hexagonal (Ports & Adapters)

## Status

✅ **Aceito**

## Data

2025-12-01

## Contexto

Ao iniciar o projeto CineLog, precisávamos definir uma arquitetura que:

1. **Isolasse a lógica de negócio** da infraestrutura (banco de dados, APIs externas, frameworks)
2. **Facilitasse testes** através de baixo acoplamento
3. **Permitisse flexibilidade** para trocar tecnologias sem impactar o core
4. **Mantivesse o código organizado** e de fácil manutenção
5. **Suportasse evolução** sem reescritas completas

### Problema

Arquiteturas tradicionais em camadas (Controller → Service → Repository) frequentemente resultam em:

- **Alto acoplamento** entre camadas
- **Dificuldade de testar** (dependências de frameworks)
- **Lógica de negócio espalhada** entre camadas
- **Difícil troca de tecnologia** (ex: mudar de MySQL para PostgreSQL)

## Decisão

Adotamos **Arquitetura Hexagonal (Ports & Adapters)** combinada com princípios de **Clean Architecture**.

### Estrutura

```
src/main/java/com/cine/cinelog/
│
├── core/                           # ❤️ CORE (Domain + Application)
│   ├── domain/                     # Entidades e Value Objects
│   │   ├── model/
│   │   │   ├── Media.java
│   │   │   ├── User.java
│   │   │   └── WatchEntry.java
│   │   └── enums/
│   │       └── MediaType.java
│   │
│   ├── application/                # Use Cases (lógica de negócio)
│   │   ├── ports/
│   │   │   ├── in/                 # Input Ports (interfaces de use cases)
│   │   │   │   ├── CreateMediaUseCase.java
│   │   │   │   └── FindMediaUseCase.java
│   │   │   └── out/                # Output Ports (interfaces de repositories)
│   │   │       ├── MediaRepositoryPort.java
│   │   │       └── EventPublisherPort.java
│   │   │
│   │   └── usecase/                # Implementações dos use cases
│   │       ├── CreateMediaService.java
│   │       └── FindMediaService.java
│   │
│   └── shared/                     # Exceções e validações do domínio
│       ├── exception/
│       └── validation/
│
├── features/                       # 🔌 ADAPTERS (Infrastructure)
│   ├── media/
│   │   ├── persistence/            # Adapter de persistência
│   │   │   ├── entity/             # JPA Entities
│   │   │   ├── repository/         # JPA Repositories
│   │   │   └── adapter/            # Implementa RepositoryPort
│   │   │
│   │   ├── web/                    # Adapter web (controllers)
│   │   │   ├── controller/
│   │   │   └── dto/
│   │   │
│   │   └── mapper/                 # Mapeadores (Entity ↔ Domain ↔ DTO)
│   │
│   └── users/
│       └── ... (mesma estrutura)
│
└── shared/                         # Cross-cutting concerns
    ├── config/                     # Configurações Spring
    ├── exception/                  # Exception handlers globais
    └── security/                   # Segurança
```

### Princípios Aplicados

1. **Dependency Rule**: Dependências apontam para dentro (Core não depende de nada)
2. **Interface Adapters**: Adapters implementam ports definidos no core
3. **Single Responsibility**: Cada camada tem responsabilidade única
4. **Inversion of Control**: Core define interfaces, adapters implementam

### Fluxo de uma Requisição

```
HTTP Request
    ↓
[Controller] (Web Adapter)
    ↓
[Use Case] (Application Core)
    ↓
[Domain Model] (Domain Core)
    ↓
[Repository Port] (Interface)
    ↓
[Repository Adapter] (Persistence Adapter)
    ↓
Database
```

## Alternativas Consideradas

### 1. MVC Tradicional do Spring

**Prós:**

- Simplicidade inicial
- Menos código
- Familiar para maioria dos devs

**Contras:**

- Alto acoplamento com Spring
- Difícil de testar
- Lógica de negócio espalhada

**Por que não escolhemos:** Muito acoplamento com framework, dificulta testes e manutenção.

### 2. Arquitetura em Camadas Simples

**Prós:**

- Fácil de entender
- Padrão amplamente conhecido
- Setup rápido

**Contras:**

- Acoplamento entre camadas
- Dependência de frameworks
- Difícil isolar domínio

**Por que não escolhemos:** Não oferece isolamento suficiente do domínio.

### 3. Microservices desde o Início

**Prós:**

- Escalabilidade independente
- Tecnologias diferentes por serviço
- Deploy independente

**Contras:**

- Overhead operacional alto
- Complexidade de rede
- Desnecessário para tamanho atual

**Por que não escolhemos:** Over-engineering para o estágio inicial do projeto.

## Consequências

### Positivas ✅

1. **Domínio Puro e Testável**
    - Core não depende de frameworks
    - Fácil escrever testes unitários
    - 100% cobertura possível no core

2. **Flexibilidade de Infraestrutura**
    - Trocar MySQL → PostgreSQL: apenas adapter muda
    - Adicionar GraphQL: novo adapter web
    - Integrar com API externa: novo adapter

3. **Melhor Organização**
    - Código organizado por feature
    - Separação clara de responsabilidades
    - Fácil navegar e entender

4. **Evolução Facilitada**
    - Adicionar features sem impactar existentes
    - Refatorações seguras
    - Possível evoluir para microservices

5. **Onboarding Mais Fácil**
    - Estrutura previsível
    - Padrões claros
    - Documentação arquitetural

### Negativas ❌

1. **Mais Código Inicial**
    - Mais interfaces (ports)
    - Mais classes (adapters)
    - Mais mapeadores

2. **Curva de Aprendizado**
    - Devs precisam entender padrão
    - Mais complexo que MVC simples
    - Requer disciplina para manter

3. **Overhead em Features Simples**
    - CRUD simples pode parecer over-engineering
    - Mais passos para implementar

4. **Performance Mínima**
    - Mais camadas = mais calls
    - Overhead de mapeamento
    - (Na prática, negligível)

### Trade-offs Aceitáveis

| Trade-off                           | Justificativa                 |
| ----------------------------------- | ----------------------------- |
| Mais código → Melhor testabilidade  | Vale a pena para qualidade    |
| Curva de aprendizado → Código limpo | Investimento inicial compensa |
| Setup complexo → Manutenção fácil   | Benefício de longo prazo      |

## Implementação

### Exemplo: Criar uma Mídia

**1. Use Case Interface (Input Port)**

```java
package com.cine.cinelog.core.application.ports.in;

public interface CreateMediaUseCase {
    Media create(CreateMediaCommand command);
}
```

**2. Use Case Implementation**

```java
package com.cine.cinelog.core.application.usecase;

@Service
@Transactional
public class CreateMediaService implements CreateMediaUseCase {

    private final MediaRepositoryPort mediaRepository;
    private final EventPublisherPort eventPublisher;

    @Override
    public Media create(CreateMediaCommand command) {
        // Validações de domínio
        Media media = Media.create(command);

        // Persistência através do port
        Media savedMedia = mediaRepository.save(media);

        // Publica evento através do port
        eventPublisher.publish(new MediaCreatedEvent(savedMedia));

        return savedMedia;
    }
}
```

**3. Repository Port (Output Port)**

```java
package com.cine.cinelog.core.application.ports.out;

public interface MediaRepositoryPort {
    Media save(Media media);
    Optional<Media> findById(Long id);
    List<Media> findAll();
}
```

**4. Repository Adapter**

```java
package com.cine.cinelog.features.media.persistence.adapter;

@Component
public class MediaRepositoryAdapter implements MediaRepositoryPort {

    private final JpaMediaRepository jpaRepository;
    private final MediaEntityMapper mapper;

    @Override
    public Media save(Media media) {
        MediaEntity entity = mapper.toEntity(media);
        MediaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

**5. Controller (Web Adapter)**

```java
package com.cine.cinelog.features.media.web.controller;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final CreateMediaUseCase createMediaUseCase;
    private final MediaMapper mapper;

    @PostMapping
    public ResponseEntity<MediaResponse> create(@RequestBody @Valid CreateMediaRequest request) {
        CreateMediaCommand command = mapper.toCommand(request);
        Media media = createMediaUseCase.create(command);
        return ResponseEntity.status(CREATED).body(mapper.toResponse(media));
    }
}
```

## Validação

### Métricas de Sucesso

✅ **Testabilidade**: 85%+ cobertura de testes no core  
✅ **Baixo Acoplamento**: Core sem dependências de frameworks  
✅ **Manutenibilidade**: Features isoladas por bounded context  
✅ **Flexibilidade**: 3+ adapters diferentes implementados

### Lições Aprendidas

1. **Vale a pena o overhead inicial** - Depois de 3 meses, a manutenção ficou muito mais fácil
2. **Mappers são essenciais** - MapStruct ajuda muito (ver ADR-004)
3. **Disciplina é fundamental** - Evitar "atalhos" que quebram a arquitetura
4. **Documentação ajuda** - ADRs e diagramas facilitam onboarding

## Referências

- [Hexagonal Architecture - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Ports & Adapters Pattern](https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/)
- [Spring Boot + Hexagonal Architecture](https://reflectoring.io/spring-hexagonal/)

## Revisões

- **2025-12-01**: Decisão inicial aceita
- **2026-01-15**: Validado após 6 meses de uso - sucesso confirmado

---

**Mantido por:** Time CineLog  
**Próxima revisão:** Julho 2026
