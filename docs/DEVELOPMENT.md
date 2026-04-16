# 🔧 Guia de Desenvolvimento - CineLog

## Índice

1. [Setup do Ambiente](#setup-do-ambiente)
2. [Estrutura do Projeto](#estrutura-do-projeto)
3. [Convenções de Código](#convenções-de-código)
4. [Desenvolvimento de Features](#desenvolvimento-de-features)
5. [Testes](#testes)
6. [Debugging](#debugging)
7. [Performance](#performance)
8. [Segurança](#segurança)

---

## Setup do Ambiente

### Pré-requisitos

- Java 21 (JDK)
- Maven 3.9+
- Docker Desktop
- IDE (IntelliJ IDEA recomendado)
- Git

### Configuração da IDE

#### IntelliJ IDEA

1. **Instale Plugins**:
    - Lombok
    - MapStruct Support
    - SonarLint
    - Docker
    - Database Tools

2. **Configure Annotation Processing**:

    ```
    Settings → Build → Compiler → Annotation Processors
    ☑ Enable annotation processing
    ```

3. **Configure Code Style**:

    ```
    Settings → Editor → Code Style → Java
    Import: config/intellij-code-style.xml
    ```

4. **Configure Lombok**:
    ```
    Settings → Plugins → Install Lombok Plugin
    Settings → Build → Compiler → Annotation Processors → Enable
    ```

#### VS Code

1. **Instale Extensions**:
    - Extension Pack for Java
    - Spring Boot Extension Pack
    - Lombok Annotations Support
    - Docker

2. **Configure settings.json**:
    ```json
    {
        "java.configuration.updateBuildConfiguration": "automatic",
        "java.compile.nullAnalysis.mode": "automatic",
        "spring-boot.ls.java.heap-size": 2048
    }
    ```

---

## Estrutura do Projeto

### Organização de Pacotes

```
com.cine.cinelog/
│
├── core/
│   ├── domain/
│   │   ├── model/              # Entidades de domínio
│   │   │   ├── Media.java
│   │   │   ├── User.java
│   │   │   └── ...
│   │   └── enums/              # Enumerações
│   │       ├── MediaType.java
│   │       └── Role.java
│   │
│   ├── application/
│   │   ├── ports/
│   │   │   ├── in/             # Use case interfaces
│   │   │   │   ├── CreateMediaUseCase.java
│   │   │   │   └── ...
│   │   │   └── out/            # Repository interfaces
│   │   │       ├── MediaRepositoryPort.java
│   │   │       └── ...
│   │   │
│   │   ├── usecase/            # Implementações de use cases
│   │   │   ├── CreateMediaService.java
│   │   │   └── ...
│   │   │
│   │   └── config/             # Configuração de beans
│   │       └── UseCaseConfig.java
│   │
│   └── shared/                 # Código compartilhado do core
│       ├── exception/
│       └── validation/
│
├── features/                   # Features organizadas por domínio
│   ├── media/
│   │   ├── persistence/
│   │   │   ├── entity/         # JPA Entities
│   │   │   ├── repository/     # JPA Repositories
│   │   │   └── adapter/        # Repository Adapters
│   │   │
│   │   ├── web/
│   │   │   ├── controller/     # REST Controllers
│   │   │   └── dto/            # DTOs (Request/Response)
│   │   │
│   │   └── mapper/             # MapStruct mappers
│   │       ├── MediaMapper.java
│   │       └── MediaEntityMapper.java
│   │
│   ├── users/
│   ├── genres/
│   └── ...
│
├── reports/                    # Relatórios, e-mail e PDF
│   ├── controller/             # ReportController (preview, email, PDF)
│   ├── service/                # Services de geração de relatórios
│   ├── email/                  # EmailService, ReportEmailService
│   └── pdf/                    # GotenbergPdfService, PdfOptions
│
└── shared/                     # Cross-cutting concerns
    ├── config/                 # Configurações globais
    ├── exception/              # Exception handlers
    ├── security/               # Segurança
    └── observability/          # Logs, metrics, tracing
```

---

## Convenções de Código

### Nomenclatura

#### Classes

```java
// Entidades de domínio (substantivos)
public class Media { }
public class User { }

// Use Cases (verbo + substantivo + UseCase)
public interface CreateMediaUseCase { }
public class CreateMediaService implements CreateMediaUseCase { }

// Repositories
public interface MediaRepositoryPort { }
public class MediaRepositoryAdapter implements MediaRepositoryPort { }

// Controllers
public class MediaController { }

// DTOs
public class CreateMediaRequest { }
public class MediaResponse { }

// Mappers
public interface MediaMapper { }
```

#### Métodos

```java
// Use cases (verbo no infinitivo)
Media create(CreateMediaCommand command);
Optional<Media> findById(Long id);
void delete(Long id);

// Boolean methods (is/has/can)
boolean isMovie();
boolean hasSeasons();
boolean canBeDeleted();
```

#### Variáveis

```java
// camelCase
private String firstName;
private List<Media> mediaList;

// Constants (UPPER_SNAKE_CASE)
public static final String API_VERSION = "v1";
public static final int MAX_PAGE_SIZE = 100;
```

### Formatação

```java
// Indentação: 4 espaços
public class Example {

    private String field;

    public void method() {
        if (condition) {
            // code
        }
    }
}

// Linha máxima: 120 caracteres
// Quebra de linha em chamadas longas
service.createMedia(
    title,
    type,
    releaseYear
);
```

### Comentários

```java
/**
 * Javadoc para classes e métodos públicos
 *
 * @param command dados para criação
 * @return mídia criada
 * @throws ValidationException se dados inválidos
 */
public Media create(CreateMediaCommand command) {
    // Comentários inline explicam o "porquê", não o "o quê"
    // Evite comentários óbvios
}
```

---

## Desenvolvimento de Features

### Passo a Passo: Nova Feature

#### 1. Criar Entidade de Domínio

```java
// core/domain/model/Review.java
package com.cine.cinelog.core.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Review {
    private Long id;
    private Long userId;
    private Long mediaId;
    private String content;
    private Integer rating;

    public void validate() {
        if (rating < 1 || rating > 10) {
            throw new IllegalArgumentException("Rating deve estar entre 1 e 10");
        }
    }
}
```

#### 2. Criar Port de Saída (Repository)

```java
// core/application/ports/out/ReviewRepositoryPort.java
package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Review;
import java.util.Optional;

public interface ReviewRepositoryPort {
    Review save(Review review);
    Optional<Review> findById(Long id);
    void deleteById(Long id);
}
```

#### 3. Criar Port de Entrada (Use Case)

```java
// core/application/ports/in/CreateReviewUseCase.java
package com.cine.cinelog.core.application.ports.in;

import com.cine.cinelog.core.domain.model.Review;

public interface CreateReviewUseCase {
    Review create(CreateReviewCommand command);
}

// Command DTO
@Value
public class CreateReviewCommand {
    Long userId;
    Long mediaId;
    String content;
    Integer rating;
}
```

#### 4. Implementar Use Case

```java
// core/application/usecase/CreateReviewService.java
package com.cine.cinelog.core.application.usecase;

import com.cine.cinelog.core.application.ports.in.CreateReviewUseCase;
import com.cine.cinelog.core.application.ports.out.ReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CreateReviewService implements CreateReviewUseCase {

    private final ReviewRepositoryPort reviewRepository;

    @Override
    public Review create(CreateReviewCommand command) {
        log.info("Creating review for media {} by user {}",
            command.getMediaId(), command.getUserId());

        var review = new Review(
            null,
            command.getUserId(),
            command.getMediaId(),
            command.getContent(),
            command.getRating()
        );

        review.validate();

        return reviewRepository.save(review);
    }
}
```

#### 5. Criar JPA Entity

```java
// features/reviews/persistence/entity/ReviewEntity.java
package com.cine.cinelog.features.reviews.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long mediaId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer rating;
}
```

#### 6. Criar Repository JPA

```java
// features/reviews/persistence/repository/JpaReviewRepository.java
package com.cine.cinelog.features.reviews.persistence.repository;

import com.cine.cinelog.features.reviews.persistence.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReviewRepository extends JpaRepository<ReviewEntity, Long> {
}
```

#### 7. Criar Repository Adapter

```java
// features/reviews/persistence/adapter/ReviewRepositoryAdapter.java
package com.cine.cinelog.features.reviews.persistence.adapter;

import com.cine.cinelog.core.application.ports.out.ReviewRepositoryPort;
import com.cine.cinelog.core.domain.model.Review;
import com.cine.cinelog.features.reviews.mapper.ReviewEntityMapper;
import com.cine.cinelog.features.reviews.persistence.repository.JpaReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepositoryPort {

    private final JpaReviewRepository jpaRepository;
    private final ReviewEntityMapper mapper;

    @Override
    public Review save(Review review) {
        var entity = mapper.toEntity(review);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Review> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
```

#### 8. Criar Mappers (MapStruct)

```java
// features/reviews/mapper/ReviewEntityMapper.java
@Mapper(componentModel = "spring")
public interface ReviewEntityMapper {
    Review toDomain(ReviewEntity entity);
    ReviewEntity toEntity(Review domain);
}

// features/reviews/mapper/ReviewMapper.java
@Mapper(componentModel = "spring")
public interface ReviewMapper {
    ReviewResponse toResponse(Review domain);
    CreateReviewCommand toCommand(CreateReviewRequest request);
}
```

#### 9. Criar DTOs

```java
// features/reviews/web/dto/CreateReviewRequest.java
@Data
public class CreateReviewRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Long mediaId;

    @NotBlank
    @Size(min = 10, max = 5000)
    private String content;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer rating;
}

// features/reviews/web/dto/ReviewResponse.java
@Data
public class ReviewResponse {
    private Long id;
    private Long userId;
    private Long mediaId;
    private String content;
    private Integer rating;
}
```

#### 10. Criar Controller

```java
// features/reviews/web/controller/ReviewController.java
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    private final ReviewMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@Valid @RequestBody CreateReviewRequest request) {
        var command = mapper.toCommand(request);
        var review = createReviewUseCase.create(command);
        return mapper.toResponse(review);
    }
}
```

#### 11. Configurar Bean do Use Case

```java
// core/application/config/UseCaseConfig.java
@Configuration
public class UseCaseConfig {

    @Bean
    public CreateReviewUseCase createReviewUseCase(ReviewRepositoryPort repository) {
        return new CreateReviewService(repository);
    }
}
```

#### 12. Criar Migração Liquibase

```xml
<!-- src/main/resources/liquibase/changes/20251210_create_reviews_table.xml -->
<databaseChangeLog>
    <changeSet id="20251210-create-reviews-table" author="dev">
        <createTable tableName="reviews">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true"/>
            </column>
            <column name="user_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="media_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="content" type="TEXT"/>
            <column name="rating" type="INT">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <rollback>
            <dropTable tableName="reviews"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

---

## Testes

### Testes Unitários

```java
// core/application/usecase/CreateReviewServiceTest.java
@ExtendWith(MockitoExtension.class)
class CreateReviewServiceTest {

    @Mock
    private ReviewRepositoryPort reviewRepository;

    @InjectMocks
    private CreateReviewService service;

    @Test
    void shouldCreateReview() {
        // Given
        var command = new CreateReviewCommand(1L, 1L, "Ótimo filme!", 9);
        var expectedReview = new Review(1L, 1L, 1L, "Ótimo filme!", 9);

        when(reviewRepository.save(any())).thenReturn(expectedReview);

        // When
        var result = service.create(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(9);
        verify(reviewRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenRatingInvalid() {
        // Given
        var command = new CreateReviewCommand(1L, 1L, "Teste", 11);

        // When & Then
        assertThatThrownBy(() -> service.create(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rating deve estar entre 1 e 10");
    }
}
```

### Testes de Integração

```java
// features/reviews/web/controller/ReviewControllerTest.java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateReview() throws Exception {
        // Given
        var request = new CreateReviewRequest();
        request.setUserId(1L);
        request.setMediaId(1L);
        request.setContent("Ótimo filme!");
        request.setRating(9);

        // When & Then
        mockMvc.perform(post("/api/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.rating").value(9));
    }
}
```

### Cobertura de Testes

```bash
# Executar testes com cobertura
./mvnw clean verify

# Visualizar relatório
open target/site/jacoco/index.html
```

**Meta**: ≥ 80% de cobertura

---

## Debugging

### Logs

```java
@Slf4j
public class MyService {

    public void process() {
        log.trace("Trace level - muito detalhado");
        log.debug("Debug level - informações de debug");
        log.info("Info level - fluxo da aplicação");
        log.warn("Warn level - avisos");
        log.error("Error level - erros", exception);
    }
}
```

### IntelliJ Debugger

1. **Breakpoints**: Click na margem esquerda
2. **Conditional Breakpoints**: Right-click no breakpoint
3. **Evaluate Expression**: Alt+F8
4. **Step Over**: F8
5. **Step Into**: F7

### Remote Debugging

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar
```

Configure IDE: Run → Edit Configurations → Remote JVM Debug

---

## Performance

### Otimizações

#### 1. Use @Transactional corretamente

```java
@Transactional(readOnly = true)  // Para consultas
public List<Media> findAll() { }

@Transactional  // Para operações de escrita
public Media save(Media media) { }
```

#### 2. Evite N+1 Queries

```java
// ❌ Ruim - N+1 queries
@Query("SELECT m FROM MediaEntity m")
List<MediaEntity> findAll();

// ✅ Bom - Fetch join
@Query("SELECT m FROM MediaEntity m LEFT JOIN FETCH m.genres")
List<MediaEntity> findAllWithGenres();
```

#### 3. Use Cache

```java
@Cacheable("genres")
public List<Genre> findAll() { }

@CacheEvict("genres")
public void save(Genre genre) { }
```

#### 4. Paginação

```java
public Page<Media> findAll(Pageable pageable) {
    return repository.findAll(pageable);
}
```

---

## Segurança

### Checklist

- [ ] Validar todos os inputs
- [ ] Usar @Valid em DTOs
- [ ] Sanitizar SQL (usar JPA/Prepared Statements)
- [ ] Não expor stack traces
- [ ] Usar HTTPS em produção
- [ ] Implementar rate limiting
- [ ] Habilitar CORS apenas para domínios conhecidos
- [ ] Criptografar senhas (BCrypt)
- [ ] Validar tokens JWT
- [ ] Logs não devem conter dados sensíveis

---

---

## Workflow com SCDF (Spring Cloud Data Flow)

O CineLog integra-se com o SCDF para orquestrar batch jobs. Esta secao descreve o fluxo
de desenvolvimento ao trabalhar com essa integracao.

### Executando batch jobs localmente vs via SCDF

| Modo                 | Quando usar                         | Como executar                                                                                                       |
| -------------------- | ----------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| **Local (sem SCDF)** | Desenvolvimento e debug rapido      | `./mvnw spring-boot:run` com agendamento habilitado ou disparo via endpoint `/api/v1/admin/batch/trigger/{jobName}` |
| **Via SCDF**         | Testar o fluxo real de orquestracao | Subir o stack SCDF com `docker-compose up -d`, registrar tasks e lancar pelo Dashboard                              |

### Configuracao inicial do SCDF

```bash
# 1. Subir toda a stack (inclui skipper-server e dataflow-server)
docker-compose up -d

# 2. Registrar as tasks no SCDF (executar uma vez apos subir)
bash docker/scdf/init-scdf.sh
```

### SCDF Dashboard

Apos subir o stack, acesse o Dashboard em:

- **URL:** http://localhost:9393/dashboard
- **Funcionalidades:** Registrar tasks, lancar execucoes, ver historico, inspecionar logs

### Arquivos-chave da integracao SCDF

| Arquivo                                          | Descricao                                                                                 |
| ------------------------------------------------ | ----------------------------------------------------------------------------------------- |
| `src/main/java/.../batch/config/TaskConfig.java` | Configuracao do Spring Cloud Task (`CustomTaskConfigurer`, `@Primary TransactionManager`) |
| `src/main/resources/application-task.yml`        | Configuracoes especificas do profile `task` (logging, desabilita scheduler)               |
| `Dockerfile`                                     | Imagem Docker usada pelo SCDF para lancar containers efemeros                             |
| `docker/scdf/init-scdf.sh`                       | Script para registrar tasks no SCDF via REST API                                          |
| `docker-compose.yml`                             | Define os servicos `skipper-server` e `dataflow-server`                                   |

### Dicas de desenvolvimento

- Ao alterar codigo de batch jobs, reconstrua a imagem Docker antes de lancar pelo SCDF:
  `docker build -t cinelog/cinelog-app:latest .`
- Use o profile `dev` para desenvolvimento local e o profile `task` e ativado automaticamente
  pelo SCDF em execucoes orquestradas.
- Logs dos containers efemeros podem ser consultados via `docker logs` ou pelo Dashboard SCDF.

Para documentacao completa da integracao, consulte o [Guia SCDF](./SCDF-GUIDE.md).

---

**Ultima atualizacao**: Marco 2026
