# ❓ FAQ (Perguntas Frequentes) - CineLog

## Índice

1. [Geral](#geral)
2. [Instalação e Configuração](#instalação-e-configuração)
3. [Desenvolvimento](#desenvolvimento)
4. [API](#api)
5. [Deployment](#deployment)
6. [Troubleshooting](#troubleshooting)
7. [Contribuição](#contribuição)

---

## Geral

### O que é o CineLog?

O CineLog é uma plataforma backend moderna para gerenciamento de mídias (filmes, séries) e registros de visualização. É construído com Java 21, Spring Boot 3, seguindo Clean Architecture e Domain-Driven Design.

### Qual é a licença do projeto?

O CineLog é licenciado sob a **MIT License**, permitindo uso comercial e modificações.

### O projeto está pronto para produção?

O projeto está em desenvolvimento ativo. A arquitetura e fundações estão sólidas, mas algumas features (como autenticação completa) estão em andamento.

### Posso usar o CineLog comercialmente?

Sim! A licença MIT permite uso comercial sem restrições.

---

## Instalação e Configuração

### Quais são os pré-requisitos?

- Java 21 (JDK)
- Maven 3.9+
- Docker e Docker Compose
- Git

### Como instalo o Java 21?

```bash
# macOS (Homebrew)
brew install openjdk@21

# Linux (Ubuntu/Debian)
sudo apt-get install openjdk-21-jdk

# Windows
# Baixe do https://adoptium.net/
```

### A aplicação funciona em Windows?

Sim! O CineLog funciona em Windows, macOS e Linux. Use o `mvnw.cmd` no Windows ao invés de `./mvnw`.

### Posso usar uma versão diferente do Java?

O projeto requer Java 21 devido ao uso de features modernas (Virtual Threads, Pattern Matching, Records). Java 17 pode funcionar, mas não é suportado oficialmente.

### Como configuro variáveis de ambiente?

Crie um arquivo `.env` na raiz do projeto:

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=cinelog
DB_USER=cinelog
DB_PASSWORD=cinelog
JWT_SECRET=your-secret-here
TMDB_API_KEY=your-tmdb-key
```

### Preciso do TMDB API Key?

Não é obrigatório para desenvolvimento básico, mas é necessário para integração com The Movie Database (busca de metadados de filmes).

---

## Desenvolvimento

### Como começo a desenvolver?

1. Clone o repositório
2. Execute `docker-compose up -d`
3. Execute `./mvnw spring-boot:run`
4. Acesse http://localhost:8080/swagger-ui/index.html

### Qual IDE vocês recomendam?

**IntelliJ IDEA** (Community ou Ultimate) é altamente recomendado. Também funcionamos bem com Eclipse e VS Code.

### Como executo os testes?

```bash
# Todos os testes
./mvnw test

# Testes de integração
./mvnw verify

# Com cobertura
./mvnw clean verify
open target/site/jacoco/index.html
```

### Como adiciono uma nova feature?

Siga o [guia passo a passo](./DEVELOPMENT.md#passo-a-passo-nova-feature) na documentação de desenvolvimento.

### Preciso seguir algum padrão de código?

Sim! Usamos:

- Checkstyle (estilo de código)
- PMD (análise estática)
- SpotBugs (detecção de bugs)

Execute `./mvnw verify` para validar.

### Como faço commit das minhas mudanças?

Siga o padrão [Conventional Commits](https://www.conventionalcommits.org/):

```bash
git commit -m "feat(media): adiciona busca por título"
git commit -m "fix(auth): corrige expiração do token"
git commit -m "docs(api): atualiza exemplos"
```

### O projeto usa Lombok?

Sim! Certifique-se de habilitar Annotation Processing na sua IDE.

### Como depuro a aplicação?

**IntelliJ IDEA**:

1. Adicione breakpoints
2. Execute em modo Debug
3. Use "Evaluate Expression" (Alt+F8)

**VS Code**:

1. Instale Java Extension Pack
2. Use debug configuration
3. F5 para iniciar debug

### Posso usar Kotlin ao invés de Java?

O projeto é 100% Java. Kotlin não é suportado no momento, mas PRs são bem-vindos se você quiser adicionar suporte.

---

## API

### Como acesso a documentação da API?

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Guia da API**: [docs/api/API_GUIDE.md](./api/API_GUIDE.md)

### Como me autentico na API?

```bash
# 1. Faça login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "senha123"}'

# 2. Use o token retornado
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/media
```

### A API suporta paginação?

Sim! Use os parâmetros `page`, `size` e `sort`:

```
GET /api/v1/media?page=0&size=20&sort=title,asc
```

### Qual é o formato de data/hora?

ISO 8601: `2025-12-10T10:30:00Z`

### Como trato erros da API?

Todos os erros retornam:

```json
{
    "timestamp": "2025-12-10T10:30:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Título é obrigatório",
    "path": "/api/v1/media",
    "traceId": "abc123"
}
```

Use o `traceId` para correlacionar logs.

### A API tem rate limiting?

Sim, em produção:

- 100 requisições/minuto (autenticado)
- 20 requisições/minuto (não autenticado)

### Há suporte para GraphQL?

Não no momento, mas está no roadmap.

---

## Deployment

### Como faço deploy em produção?

Consulte o [Guia de Deployment](./DEPLOYMENT.md) para instruções detalhadas por plataforma (AWS, GCP, Azure, K8s).

### Posso usar Docker?

Sim! Temos um `Dockerfile` otimizado:

```bash
docker build -t cinelog:latest .
docker run -p 8080:8080 cinelog:latest
```

### Quais variáveis de ambiente preciso configurar?

Mínimo para produção:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://host:3306/cinelog
DB_USER=cinelog
DB_PASSWORD=secure-password
JWT_SECRET=your-secure-secret-32-chars-min
```

### Como configuro HTTPS?

```yaml
server:
    ssl:
        enabled: true
        key-store: classpath:keystore.p12
        key-store-password: ${KEYSTORE_PASSWORD}
        key-store-type: PKCS12
```

### Preciso de um banco dedicado?

Para desenvolvimento, pode usar Docker. Para produção, recomendamos:

- AWS RDS
- Azure Database for MySQL
- Google Cloud SQL

### Como monitoro a aplicação?

Usamos três pilares:

- **Logs**: Logstash/ELK
- **Métricas**: Prometheus/Grafana
- **Tracing**: OpenTelemetry/Tempo

Veja [OBSERVABILITY.md](./OBSERVABILITY.md).

### Há CI/CD configurado?

Temos GitHub Actions configurado. Veja `.github/workflows/ci-cd.yml`.

---

## Troubleshooting

### Porta 8080 já está em uso

```bash
# Descubra qual processo está usando
lsof -i :8080

# Mate o processo ou use outra porta
./mvnw spring-boot:run -Dserver.port=8081
```

### MySQL não conecta

```bash
# Verifique se o container está rodando
docker ps

# Verifique os logs
docker logs cinelog-mysql

# Reinicie os containers
docker-compose down
docker-compose up -d
```

### Testes falhando

```bash
# Limpe e reconstrua
./mvnw clean test

# Se usar Testcontainers, verifique Docker
docker ps
```

### Build muito lento

```bash
# Use Maven daemon
./mvnw clean install -T 1C  # 1 thread por core

# Ou pule testes em dev
./mvnw package -DskipTests
```

### Erro de permissão no mvnw

```bash
chmod +x mvnw
```

### Como vejo os logs?

```bash
# Em desenvolvimento
tail -f logs/cinelog.log | jq

# Docker
docker logs -f cinelog-app

# Estruturados (JSON)
docker logs cinelog-app | jq .
```

### Memoria heap estourou

Ajuste a heap size:

```bash
java -Xms512m -Xmx2g -jar cinelog.jar

# Ou no Docker
docker run -m 2g cinelog:latest
```

### Como depuro queries SQL lentas?

```yaml
# application.yml
spring:
    jpa:
        show-sql: true
        properties:
            hibernate:
                format_sql: true
                use_sql_comments: true
logging:
    level:
        org.hibernate.SQL: DEBUG
        org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## Contribuição

### Como contribuo?

1. Fork o repositório
2. Crie uma branch: `git checkout -b feature/nova-feature`
3. Faça commit: `git commit -m "feat: adiciona nova feature"`
4. Push: `git push origin feature/nova-feature`
5. Abra um Pull Request

Leia o [Guia de Contribuição](../CONTRIBUTING.md) completo.

### Preciso assinar um CLA?

Não, o projeto é open source sem restrições.

### Como reporto bugs?

Abra uma [issue no GitHub](https://github.com/marcusPrado02/cinelog/issues) com:

- Descrição do bug
- Passos para reproduzir
- Comportamento esperado vs atual
- Logs relevantes
- Ambiente (OS, Java version)

### Como sugiro features?

Use [GitHub Discussions](https://github.com/marcusPrado02/cinelog/discussions) para discutir ideias antes de implementar.

### Meu PR foi rejeitado, e agora?

- Leia os comentários do revisor
- Faça as mudanças solicitadas
- Peça esclarecimentos se necessário
- Seja receptivo ao feedback

### Como me torno um maintainer?

Contribua consistentemente e demonstre:

- Conhecimento técnico
- Qualidade de código
- Comunicação efetiva
- Alinhamento com valores do projeto

### Posso traduzir a documentação?

Sim! Contribuições de tradução são muito bem-vindas.

---

## Dúvidas Técnicas Avançadas

### Por que Clean Architecture?

Para isolar lógica de negócio da infraestrutura, facilitando testes e manutenção. Benefícios:

- **Testabilidade** - Fácil mockar dependências
- **Manutenibilidade** - Mudanças isoladas por camada
- **Independência** - Core não depende de frameworks
- **Flexibilidade** - Troca de tecnologias sem impacto no core

Veja [ADR-001](./architecture/adr/ADR-001-clean-architecture.md).

### Por que MapStruct e não ModelMapper?

**Performance e type-safety.** MapStruct gera código em compile-time:

| Aspecto      | MapStruct              | ModelMapper      |
| ------------ | ---------------------- | ---------------- |
| Performance  | Rápido (código gerado) | Lento (reflexão) |
| Type-safety  | Sim (compile-time)     | Não (runtime)    |
| Debugging    | Fácil                  | Difícil          |
| Configuração | Explícita              | Mágica           |

Veja [ADR-004](./architecture/adr/ADR-004-mapstruct.md).

### Por que MySQL e não PostgreSQL?

Ambos são excelentes. Escolhemos MySQL por:

- ✅ Familiaridade da equipe
- ✅ Excelente suporte em cloud (AWS RDS, Azure, GCP)
- ✅ Performance comprovada
- ✅ Comunidade ativa

PostgreSQL seria igualmente viável. Veja [ADR-006](./architecture/adr/ADR-006-mysql.md).

### Vocês planejam microservices?

**Não no curto prazo.** Monolito modular é mais apropriado para:

- Equipe pequena/média
- Domínio não muito complexo
- Simplicidade de deploy
- Menor overhead operacional

Se necessário, a arquitetura permite evoluir para microservices:

```
Monolito Modular → Modular por Contextos → Microservices
```

### Há suporte para multi-tenancy?

Não no momento, mas a arquitetura permite adicionar facilmente com:

- **Tenant ID em cada entidade**
- **Filtro automático nas queries**
- **Schema isolado por tenant** (se necessário)

### Como vocês lidam com migrações de schema?

**Liquibase** com versionamento rigoroso:

```yaml
# Cada changeset é idempotente
- changeSet:
      id: 001-create-media-table
      author: cinelog
      changes:
          - createTable:
                tableName: media
```

Benefícios:

- ✅ Rollback support
- ✅ Versionamento automático
- ✅ Auditoria de mudanças
- ✅ Funciona em CI/CD

### Como é tratado o versionamento da API?

Seguimos **semantic versioning** na URL:

```
/api/v1/media    # Versão 1 (atual)
/api/v2/media    # Versão 2 (futura)
```

Breaking changes exigem nova versão. Mudanças compatíveis podem ser additive.

### Qual é a estratégia de cache?

**Multi-layer caching:**

1. **Application Cache** - Caffeine/Redis para objetos
2. **HTTP Cache** - ETags e Cache-Control headers
3. **Database Cache** - Query cache do MySQL
4. **CDN Cache** - CloudFront/Cloudflare para assets

```java
@Cacheable(value = "media", key = "#id")
public Media findById(Long id) {
    return mediaRepository.findById(id)
        .orElseThrow(() -> new MediaNotFoundException(id));
}
```

### Como vocês garantem idempotência em eventos?

**Inbox Pattern** com deduplicação:

```java
@Transactional
public void processEvent(EventEnvelope event) {
    // 1. Verifica se já processou
    if (inboxRepository.existsByEventId(event.getEventId())) {
        log.debug("Event {} already processed", event.getEventId());
        return;
    }

    // 2. Processa evento
    handleEvent(event);

    // 3. Registra no inbox (atomic)
    inboxRepository.save(new InboxEvent(event.getEventId()));
}
```

### Qual é o tamanho máximo de payload na API?

```yaml
spring:
    servlet:
        multipart:
            max-file-size: 10MB # Arquivo individual
            max-request-size: 50MB # Requisição completa
```

Para uploads maiores, use **presigned URLs** (S3) ou **chunked upload**.

### Como vocês testam eventos Kafka?

**Testcontainers** com Kafka real:

```java
@Testcontainers
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    @Test
    void shouldPublishAndConsumeEvent() {
        // Given
        var event = new WatchEntryCreatedEvent(...);

        // When
        kafkaTemplate.send("watchentry-events", event);

        // Then
        await().atMost(5, SECONDS)
            .until(() -> eventReceived);
    }
}
```

### Vocês usam feature flags?

Não no momento, mas planejamos adicionar com **Unleash** ou **LaunchDarkly** para:

- Releases graduais
- A/B testing
- Kill switches
- Configuração dinâmica

---

## Performance e Escalabilidade

### Quantas requisições por segundo a API aguenta?

**Depende da infraestrutura**, mas em testes:

| Cenário     | RPS    | Latência p95 | Setup                  |
| ----------- | ------ | ------------ | ---------------------- |
| Dev (local) | ~500   | 50ms         | 2 cores, 4GB RAM       |
| Staging     | ~2000  | 30ms         | 4 cores, 8GB RAM       |
| Prod        | ~5000+ | 20ms         | Auto-scaling, 8+ cores |

### Como otimizar queries lentas?

1. **Adicionar índices**

    ```sql
    CREATE INDEX idx_media_title ON media(title);
    ```

2. **Usar projections**

    ```java
    @Query("SELECT new MediaDTO(m.id, m.title) FROM Media m")
    List<MediaDTO> findAllProjected();
    ```

3. **Batch queries**

    ```java
    @QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "50"))
    ```

4. **Cache agressivamente**
    ```java
    @Cacheable(value = "popularMedia", unless = "#result.isEmpty()")
    ```

### A aplicação suporta horizontal scaling?

**Sim!** É stateless e pode escalar horizontalmente:

```yaml
# Kubernetes deployment
replicas: 5 # Múltiplas instâncias


# Load balancer distribui tráfego
# Sessões em Redis (stateless)
# Conexões do DB em pool
```

### Como monitorar performance em produção?

**3 pilares de observabilidade:**

1. **Logs** - JSON estruturado com correlationId
2. **Métricas** - Prometheus + Grafana
3. **Traces** - OpenTelemetry + Tempo

Dashboard essencial:

- Request rate (RPS)
- Error rate (%)
- Response time (p50, p95, p99)
- JVM metrics (heap, GC, threads)
- Database connections
- Kafka lag

Veja [OBSERVABILITY.md](./OBSERVABILITY.md).

---

## Segurança

### Como proteger contra SQL Injection?

**JPA/Hibernate** com **prepared statements**:

```java
// ✅ SEGURO - Prepared statement
@Query("SELECT m FROM Media m WHERE m.title = :title")
Media findByTitle(@Param("title") String title);

// ❌ INSEGURO - String concatenation
@Query("SELECT m FROM Media m WHERE m.title = '" + title + "'")  // NÃO FAÇA ISSO!
```

### Como implementar rate limiting?

**Bucket4j** com Redis:

```java
@RateLimiter(
    name = "api",
    fallbackMethod = "rateLimitFallback"
)
@GetMapping("/media")
public List<Media> getMedia() {
    return mediaService.findAll();
}

public List<Media> rateLimitFallback(RateLimitExceededException e) {
    throw new TooManyRequestsException("Rate limit exceeded. Try again later.");
}
```

### Tokens JWT expiram?

**Sim**, configurável:

```yaml
cinelog:
    security:
        jwt:
            expiration-seconds: 3600 # 1 hora
            refresh-expiration-seconds: 604800 # 7 dias
```

Implementamos **refresh tokens** para renovação automática.

### Como proteger endpoints sensíveis?

**Spring Security** com roles:

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    mediaService.delete(id);
    return ResponseEntity.noContent().build();
}
```

### Senhas são hasheadas?

**Sim**, com **BCrypt** (10 rounds):

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}
```

Nunca armazenamos senhas em plain text.

### Há proteção contra CSRF?

**Sim** para formulários web. **Não necessário** para API REST stateless com JWT.

```yaml
security:
    csrf:
        enabled: false # API REST stateless
```

### Como auditar ações de usuários?

**Spring Data Envers** para auditoria completa:

```java
@Audited
@Entity
public class Media {
    // Todas as mudanças são auditadas
}

// Consultar histórico
AuditReader reader = AuditReaderFactory.get(entityManager);
List<Number> revisions = reader.getRevisions(Media.class, mediaId);
```

---

## DevOps e Infraestrutura

### Qual é o tamanho da imagem Docker?

**~200MB** (Alpine + JDK 21 + JAR)

```dockerfile
# Multi-stage build para reduzir tamanho
FROM eclipse-temurin:21-jdk-alpine AS builder
# ... build ...

FROM eclipse-temurin:21-jre-alpine
# ... apenas JRE + JAR (menor)
```

### Como fazer backup do banco?

**Automático com cron** (produção):

```bash
# Diário às 2AM
0 2 * * * mysqldump -u$DB_USER -p$DB_PASS cinelog > /backups/cinelog-$(date +\%Y\%m\%d).sql

# Retenção de 30 dias
find /backups -name "*.sql" -mtime +30 -delete
```

**Manual**:

```bash
docker exec cinelog-mysql mysqldump -ucinelog -pcinelog cinelog > backup.sql
```

### Como restaurar backup?

```bash
# Parar aplicação
docker-compose stop app

# Restaurar
docker exec -i cinelog-mysql mysql -ucinelog -pcinelog cinelog < backup.sql

# Reiniciar
docker-compose start app
```

### Há configuração para diferentes ambientes?

**Sim**, profiles do Spring:

```
src/main/resources/
├── application.yml           # Base
├── application-dev.yml       # Desenvolvimento
├── application-test.yml      # Testes
├── application-staging.yml   # Staging
└── application-prod.yml      # Produção
```

Ative com: `SPRING_PROFILES_ACTIVE=prod`

### Como fazer rollback de deploy?

**Kubernetes:**

```bash
kubectl rollout undo deployment/cinelog
```

**Docker:**

```bash
docker tag cinelog:previous cinelog:latest
docker-compose up -d
```

**Liquibase (database):**

```bash
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

### Qual é a estratégia de blue-green deployment?

```yaml
# Blue (produção atual)
deployment-blue:
    replicas: 3
    image: cinelog:v1.0.0

# Green (nova versão)
deployment-green:
    replicas: 3
    image: cinelog:v1.1.0

# Trocar serviço de blue → green
service:
    selector:
        version: green # Muda aqui
```

### Como configurar auto-scaling?

**Kubernetes HPA:**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
    name: cinelog-hpa
spec:
    scaleTargetRef:
        apiVersion: apps/v1
        kind: Deployment
        name: cinelog
    minReplicas: 2
    maxReplicas: 10
    metrics:
        - type: Resource
          resource:
              name: cpu
              target:
                  type: Utilization
                  averageUtilization: 70
```

---

## Integração e Extensibilidade

### Como integrar com TMDB (The Movie Database)?

Usamos **RestTemplate** com circuit breaker:

```java
@Service
public class TmdbService {

    @CircuitBreaker(name = "tmdb", fallbackMethod = "fallback")
    @Retry(name = "tmdb", fallbackMethod = "fallback")
    public TmdbMovie fetchMovie(String tmdbId) {
        return restTemplate.getForObject(
            "https://api.themoviedb.org/3/movie/{id}?api_key={key}",
            TmdbMovie.class,
            tmdbId,
            apiKey
        );
    }

    public TmdbMovie fallback(String tmdbId, Exception e) {
        log.warn("TMDB unavailable, using cache", e);
        return cache.get(tmdbId);
    }
}
```

### Posso adicionar novos tipos de mídia?

**Sim!** Usando **enum extensível**:

```java
public enum MediaType {
    MOVIE,
    SERIES,
    DOCUMENTARY,  // ← Novo tipo
    PODCAST,      // ← Novo tipo
    AUDIOBOOK;    // ← Novo tipo
}
```

Adicione validadores específicos no Template Method Pattern.

### Como adicionar um novo provedor de autenticação?

Implemente `AuthenticationProvider`:

```java
@Component
public class GoogleAuthProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication auth) {
        // Valida com Google OAuth
        GoogleUser user = googleOAuthService.validate(auth.getCredentials());
        return new UsernamePasswordAuthenticationToken(user, ...);
    }
}
```

### Posso usar o CineLog como biblioteca?

Não foi projetado para isso, mas você pode:

1. **Forkar** e customizar
2. **Contribuir** com features genéricas
3. **Usar a API REST** como serviço

---

## Ainda tem dúvidas?

### Canais de Suporte

- **GitHub Issues**: Bugs e problemas técnicos
- **GitHub Discussions**: Perguntas e ideias
- **Email**: contato@cinelog.com
- **Stack Overflow**: Use a tag `cinelog`

### Recursos Úteis

- [Documentação Completa](./INDEX.md)
- [Guia de Início Rápido](./GETTING_STARTED.md)
- [Guia da API](./api/API_GUIDE.md)
- [Exemplos de Código](../src/test/java)

---

**Não encontrou sua pergunta?**

Abra uma [discussion](https://github.com/marcusPrado02/cinelog/discussions) ou entre em contato!

---

**Última atualização**: Dezembro 2025
