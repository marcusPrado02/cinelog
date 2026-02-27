# 🧪 Testing

> Estratégia de testes do CineLog: pirâmide, ferramentas e boas práticas.

---

## Pirâmide de Testes

```
          ╔═══════════╗
          ║   E2E (5%) ║
          ╠═══════════╣
        ║ Integration (25%) ║
        ╠═══════════════════╣
      ║    Unit Tests (70%)    ║
      ╚════════════════════════╝
```

| Tipo | % | Ferramenta | Tempo | Escopo |
|---|---|---|---|---|
| **Unit** | 70% | JUnit 5 + Mockito | < 5s | Classes isoladas |
| **Integration** | 25% | @SpringBootTest + Testcontainers | < 30s | Camadas integradas |
| **E2E** | 5% | K6 | variável | Fluxos completos |

### Metas

| Métrica | Meta | Atual |
|---|---|---|
| **Cobertura (JaCoCo)** | ≥ 80% | 82%+ |
| **Tempo total** | < 2 min | ~90s |
| **Testes flaky** | 0% | 0% |

---

## Testes Unitários

### Ferramentas

| Ferramenta | Uso |
|---|---|
| **JUnit 5** | Framework de testes |
| **Mockito** | Mocking de dependências |
| **AssertJ** | Assertions fluentes |
| **ArchUnit** | Validação de arquitetura |

### Convenções

```java
@DisplayName("CreateMediaService")
class CreateMediaServiceTest {

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("deve criar mídia com dados válidos")
        void shouldCreateMediaWithValidData() {
            // Arrange
            var request = createMediaRequestBuilder().build();
            when(mediaRepo.save(any())).thenReturn(mediaEntity());

            // Act
            var result = service.execute(request);

            // Assert
            assertThat(result.getTitle()).isEqualTo("Inception");
            verify(mediaRepo).save(any());
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando título está vazio")
        void shouldThrowWhenTitleIsBlank() {
            var request = createMediaRequestBuilder().title("").build();

            assertThatThrownBy(() -> service.execute(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("título");
        }
    }
}
```

### Padrão AAA (Arrange-Act-Assert)

```java
// 1. Arrange — configura o cenário
var media = Media.builder().title("Inception").build();
when(repository.findById(1L)).thenReturn(Optional.of(media));

// 2. Act — executa a ação
var result = useCase.execute(1L);

// 3. Assert — verifica o resultado
assertThat(result.getTitle()).isEqualTo("Inception");
```

---

## Testes de Integração

### Ferramentas

| Ferramenta | Uso |
|---|---|
| **@SpringBootTest** | Contexto Spring completo |
| **MockMvc** | Testes de controllers HTTP |
| **Testcontainers** | MySQL e Kafka reais em Docker |
| **@Sql** | Scripts de setup/cleanup |

### Exemplo com MockMvc

```java
@SpringBootTest
@AutoConfigureMockMvc
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/media deve retornar 201")
    void shouldCreateMedia() throws Exception {
        String json = """
            {
              "title": "Inception",
              "type": "MOVIE",
              "releaseYear": 2010
            }
            """;

        mockMvc.perform(post("/api/v1/media")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + validToken)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Inception"))
            .andExpect(jsonPath("$.id").isNumber());
    }
}
```

### Testcontainers

```java
@Testcontainers
@SpringBootTest
class MediaRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("cinelog_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

---

## Testes de Arquitetura (ArchUnit)

```java
@AnalyzeClasses(packages = "com.cine.cinelog")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..core.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories =
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("..repository..");
}
```

---

## Testes de Performance (K6)

```javascript
// performance/k6/smoke.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% das requests < 500ms
    http_req_failed: ['rate<0.01'],   // < 1% de erro
  },
};

export default function () {
  const res = http.get('http://localhost:8080/api/v1/media');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'latency < 500ms': (r) => r.timings.duration < 500,
  });
  sleep(1);
}
```

Executar: `k6 run performance/k6/smoke.js`

---

## JaCoCo (Cobertura)

### Configuração

```xml
<!-- pom.xml -->
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.14</version>
</plugin>
```

### Exclusões

Classes excluídas da cobertura:
- DTOs (getters/setters gerados pelo Lombok)
- Mappers (gerados pelo MapStruct)
- Configurações Spring (`*Config.java`)
- Application main class

### Comandos

```bash
# Rodar testes com cobertura
./mvnw clean verify

# Abrir relatório HTML
open target/site/jacoco/index.html
```

---

## Boas Práticas

| Prática | Descrição |
|---|---|
| **Testes independentes** | Nenhum teste depende de outro |
| **Sem lógica em testes** | Sem if/for/while no código de teste |
| **Nomes descritivos** | `@DisplayName` em português claro |
| **Builders de teste** | Factories para objetos complexos |
| **Dados mínimos** | Só o necessário para o cenário |
| **Um assert por conceito** | Foco em um comportamento por teste |
| **Cleanup automático** | `@Transactional` em testes de integração |

---

## Comandos

```bash
# Todos os testes
./mvnw test

# Testes com cobertura
./mvnw clean verify

# Testes de um módulo específico
./mvnw test -Dtest="MediaControllerTest"

# Testes de uma classe específica
./mvnw test -Dtest="CreateMediaServiceTest#shouldCreateMediaWithValidData"

# Testes de performance
k6 run performance/k6/smoke.js
k6 run performance/k6/load-media.js
```
