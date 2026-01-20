# 🧪 Guia de Testes - CineLog

## Índice

1. [Visão Geral](#visão-geral)
2. [Estratégia de Testes](#estratégia-de-testes)
3. [Testes Unitários](#testes-unitários)
4. [Testes de Integração](#testes-de-integração)
5. [Testes de Performance](#testes-de-performance)
6. [Cobertura de Código](#cobertura-de-código)
7. [Mocking](#mocking)
8. [Boas Práticas](#boas-práticas)

---

## Visão Geral

O CineLog segue a **Pirâmide de Testes** com foco em qualidade e confiabilidade:

```
        /\
       /  \      E2E Tests (Poucos, Lentos, Caros)
      /────\
     /      \    Integration Tests (Alguns, Médios)
    /────────\
   /          \  Unit Tests (Muitos, Rápidos, Baratos)
  /────────────\
```

### Objetivos

-   ✅ Cobertura mínima: **80%**
-   ✅ Testes rápidos: **< 30 segundos**
-   ✅ Testes confiáveis: **0% flaky tests**
-   ✅ Manuteníveis: **DRY, SOLID**

---

## Estratégia de Testes

### 1. Testes Unitários (70%)

**O que testar**:

-   Lógica de negócio (use cases)
-   Validações
-   Mapeamentos
-   Regras de domínio

**Não testar**:

-   Getters/Setters
-   Configurações Spring
-   Código gerado (MapStruct)

### 2. Testes de Integração (25%)

**O que testar**:

-   Controllers + Use Cases + Repositories
-   Interação com banco de dados
-   Serializacao JSON
-   Autenticação/Autorização

### 3. Testes E2E (5%)

**O que testar**:

-   Fluxos críticos de usuário
-   Integração completa
-   Deploy e rollback

---

## Testes Unitários

### Estrutura Básica

```java
package com.cine.cinelog.core.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateMediaService")
class CreateMediaServiceTest {

    @Mock
    private MediaRepositoryPort mediaRepository;

    @InjectMocks
    private CreateMediaService service;

    @Test
    @DisplayName("Deve criar mídia com dados válidos")
    void shouldCreateMediaWithValidData() {
        // Given (Arrange)
        var command = new CreateMediaCommand(
            "Matrix",
            MediaType.MOVIE,
            1999,
            "The Matrix",
            "en",
            "Um programador descobre a verdade..."
        );

        var expectedMedia = new Media(
            1L,
            "Matrix",
            MediaType.MOVIE,
            1999,
            "The Matrix",
            "en",
            "Um programador descobre a verdade...",
            null,
            null,
            Set.of(),
            List.of()
        );

        when(mediaRepository.save(any(Media.class))).thenReturn(expectedMedia);

        // When (Act)
        var result = service.create(command);

        // Then (Assert)
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Matrix");
        assertThat(result.getType()).isEqualTo(MediaType.MOVIE);

        verify(mediaRepository, times(1)).save(any(Media.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando título for nulo")
    void shouldThrowExceptionWhenTitleIsNull() {
        // Given
        var command = new CreateMediaCommand(
            null,  // título nulo
            MediaType.MOVIE,
            1999,
            "The Matrix",
            "en",
            "Overview"
        );

        // When & Then
        assertThatThrownBy(() -> service.create(command))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Título é obrigatório");

        verify(mediaRepository, never()).save(any());
    }
}
```

### Testes Parametrizados

```java
@ParameterizedTest
@DisplayName("Deve validar ano de lançamento")
@CsvSource({
    "1900, false",
    "1950, true",
    "2024, true",
    "2100, false"
})
void shouldValidateReleaseYear(int year, boolean expected) {
    var command = new CreateMediaCommand("Title", MediaType.MOVIE, year, "Title", "en", "Overview");

    boolean isValid = service.validateYear(command);

    assertThat(isValid).isEqualTo(expected);
}
```

### Testes com @Nested

```java
@DisplayName("MediaService")
class MediaServiceTest {

    @Nested
    @DisplayName("Criar Mídia")
    class CreateMedia {

        @Test
        @DisplayName("Deve criar filme")
        void shouldCreateMovie() { }

        @Test
        @DisplayName("Deve criar série")
        void shouldCreateSeries() { }
    }

    @Nested
    @DisplayName("Buscar Mídia")
    class FindMedia {

        @Test
        @DisplayName("Deve retornar mídia existente")
        void shouldFindExistingMedia() { }

        @Test
        @DisplayName("Deve lançar exceção quando não encontrar")
        void shouldThrowWhenNotFound() { }
    }
}
```

---

## Testes de Integração

### Controller Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("MediaController Integration Tests")
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaRepositoryPort mediaRepository;

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/media - Deve criar nova mídia")
    void shouldCreateMedia() throws Exception {
        // Given
        var request = new CreateMediaRequest();
        request.setTitle("Matrix");
        request.setType("MOVIE");
        request.setReleaseYear(1999);
        request.setOriginalTitle("The Matrix");
        request.setOriginalLanguage("en");
        request.setOverview("Um programador...");

        // When & Then
        mockMvc.perform(post("/api/v1/media")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Matrix"))
            .andExpect(jsonPath("$.type").value("MOVIE"))
            .andExpect(jsonPath("$.releaseYear").value(1999));
    }

    @Test
    @DisplayName("GET /api/v1/media/{id} - Deve retornar mídia existente")
    void shouldGetMediaById() throws Exception {
        // Given
        var media = createTestMedia();
        var savedMedia = mediaRepository.save(media);

        // When & Then
        mockMvc.perform(get("/api/v1/media/{id}", savedMedia.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(savedMedia.getId()))
            .andExpect(jsonPath("$.title").value("Matrix"));
    }

    @Test
    @DisplayName("GET /api/v1/media/{id} - Deve retornar 404 quando não encontrar")
    void shouldReturn404WhenMediaNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/media/{id}", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Mídia não encontrada"));
    }
}
```

### Repository Tests com Testcontainers

```java
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("MediaRepository Integration Tests")
class MediaRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private JpaMediaRepository repository;

    @Test
    @DisplayName("Deve salvar e recuperar mídia")
    void shouldSaveAndRetrieveMedia() {
        // Given
        var media = new MediaEntity();
        media.setTitle("Matrix");
        media.setType(MediaType.MOVIE);
        media.setReleaseYear(1999);

        // When
        var saved = repository.save(media);
        var found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Matrix");
    }

    @Test
    @DisplayName("Deve buscar mídias por tipo")
    void shouldFindMediaByType() {
        // Given
        createTestMovie("Matrix");
        createTestMovie("Inception");
        createTestSeries("Breaking Bad");

        // When
        var movies = repository.findByType(MediaType.MOVIE);

        // Then
        assertThat(movies).hasSize(2);
        assertThat(movies).extracting(MediaEntity::getType)
            .containsOnly(MediaType.MOVIE);
    }
}
```

---

## Testes de Performance

### K6 Load Tests

```javascript
// performance/k6/load-media.js
import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    stages: [
        { duration: "30s", target: 20 }, // Ramp up
        { duration: "1m", target: 50 }, // Stay at 50 users
        { duration: "30s", target: 0 }, // Ramp down
    ],
    thresholds: {
        http_req_duration: ["p(95)<500"], // 95% abaixo de 500ms
        http_req_failed: ["rate<0.01"], // Menos de 1% de erros
    },
};

export default function () {
    const url = "http://localhost:8080/api/v1/media";

    const response = http.get(url);

    check(response, {
        "status is 200": (r) => r.status === 200,
        "response time < 500ms": (r) => r.timings.duration < 500,
    });

    sleep(1);
}
```

**Executar**:

```bash
k6 run performance/k6/load-media.js
```

### JMeter Tests

```xml
<!-- performance/jmeter/test-plan.jmx -->
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">CineLog Load Test</stringProp>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup">
        <intProp name="ThreadGroup.num_threads">100</intProp>
        <intProp name="ThreadGroup.ramp_time">30</intProp>
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/media</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
      </ThreadGroup>
    </TestPlan>
  </hashTree>
</jmeterTestPlan>
```

---

## Cobertura de Código

### JaCoCo Configuration

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.14</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Gerar Relatório

```bash
# Executar testes e gerar relatório
./mvnw clean verify

# Abrir relatório
open target/site/jacoco/index.html
```

### Excluir Classes da Cobertura

```xml
<configuration>
    <excludes>
        <exclude>**/*Config.class</exclude>
        <exclude>**/*Application.class</exclude>
        <exclude>**/*Entity.class</exclude>
        <exclude>**/dto/**</exclude>
    </excludes>
</configuration>
```

---

## Mocking

### Mockito

```java
// Mock simples
@Mock
private MediaRepositoryPort repository;

// Mock com comportamento
when(repository.findById(1L)).thenReturn(Optional.of(media));
when(repository.save(any())).thenReturn(media);

// Verificação
verify(repository).save(any());
verify(repository, times(1)).findById(1L);
verify(repository, never()).delete(any());

// Captura de argumentos
ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
verify(repository).save(captor.capture());
assertThat(captor.getValue().getTitle()).isEqualTo("Matrix");
```

### MockMvc

```java
mockMvc.perform(post("/api/v1/media")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
    .andDo(print())  // Debug
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.id").exists());
```

---

## Boas Práticas

### 1. Nomeação de Testes

```java
// ❌ Ruim
@Test
void test1() { }

// ✅ Bom
@Test
@DisplayName("Deve criar mídia com dados válidos")
void shouldCreateMediaWithValidData() { }
```

### 2. AAA Pattern (Arrange, Act, Assert)

```java
@Test
void shouldCalculateTotalDuration() {
    // Arrange (Given)
    var episodes = List.of(
        new Episode(1, 45),
        new Episode(2, 50)
    );

    // Act (When)
    int total = service.calculateTotalDuration(episodes);

    // Assert (Then)
    assertThat(total).isEqualTo(95);
}
```

### 3. Testes Independentes

```java
// ❌ Ruim - Testes dependentes
@Test
@Order(1)
void createUser() {
    user = service.create(...);
}

@Test
@Order(2)
void updateUser() {
    service.update(user.getId(), ...);  // Depende do teste anterior
}

// ✅ Bom - Testes independentes
@BeforeEach
void setUp() {
    user = createTestUser();
}

@Test
void shouldUpdateUser() {
    service.update(user.getId(), ...);
}
```

### 4. Evitar Lógica em Testes

```java
// ❌ Ruim
@Test
void test() {
    var result = service.process();
    if (result.size() > 0) {
        assertThat(result.get(0)).isNotNull();
    }
}

// ✅ Bom
@Test
void shouldReturnNonEmptyList() {
    var result = service.process();
    assertThat(result).isNotEmpty();
    assertThat(result.get(0)).isNotNull();
}
```

### 5. Usar Builders para Testes

```java
public class MediaTestBuilder {
    private String title = "Default Title";
    private MediaType type = MediaType.MOVIE;
    private Integer year = 2024;

    public MediaTestBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public MediaTestBuilder withType(MediaType type) {
        this.type = type;
        return this;
    }

    public Media build() {
        return new Media(null, title, type, year, ...);
    }
}

// Uso
@Test
void test() {
    var media = new MediaTestBuilder()
        .withTitle("Matrix")
        .withType(MediaType.MOVIE)
        .build();
}
```

---

**Última atualização**: Dezembro 2025
