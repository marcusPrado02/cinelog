# ⚙️ Configuration

> Todas as configurações do CineLog: variáveis de ambiente, profiles e YAML.

---

## Spring Profiles

| Profile    | Ativação                        | Uso                       |
| ---------- | ------------------------------- | ------------------------- |
| **dev**    | `SPRING_PROFILES_ACTIVE=dev`    | Desenvolvimento local     |
| **test**   | Automático em testes            | Testes com Testcontainers |
| **docker** | `SPRING_PROFILES_ACTIVE=docker` | Docker Compose            |
| **perf**   | `SPRING_PROFILES_ACTIVE=perf`   | Testes de performance     |
| **prod**   | `SPRING_PROFILES_ACTIVE=prod`   | Produção                  |

---

## Variáveis de Ambiente

### Obrigatórias

| Variável                      | Profile | Default | Descrição                     |
| ----------------------------- | ------- | ------- | ----------------------------- |
| `SPRING_PROFILES_ACTIVE`      | Todos   | dev     | Profile ativo                 |
| `CINELOG_SECURITY_JWT_SECRET` | prod    | —       | Chave secreta JWT (≥32 chars) |
| `TMDB_API_KEY`                | Todos   | —       | API key do TMDb               |

### Banco de Dados

| Variável                                     | Default                               | Descrição         |
| -------------------------------------------- | ------------------------------------- | ----------------- |
| `SPRING_DATASOURCE_URL`                      | `jdbc:mysql://localhost:3306/cinelog` | URL JDBC          |
| `SPRING_DATASOURCE_USERNAME`                 | `cinelog`                             | Usuário do banco  |
| `SPRING_DATASOURCE_PASSWORD`                 | `cinelog`                             | Senha do banco    |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `10`                                  | Max conexões      |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE`      | `2`                                   | Min conexões idle |

### Redis

| Variável                     | Default     | Descrição          |
| ---------------------------- | ----------- | ------------------ |
| `SPRING_DATA_REDIS_HOST`     | `localhost` | Host do Redis      |
| `SPRING_DATA_REDIS_PORT`     | `6379`      | Porta do Redis     |
| `SPRING_DATA_REDIS_DATABASE` | `0`         | Database index     |
| `SPRING_DATA_REDIS_TIMEOUT`  | `2000ms`    | Timeout de conexão |

### Kafka

| Variável                                  | Default          | Descrição       |
| ----------------------------------------- | ---------------- | --------------- |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`          | `localhost:9092` | Brokers Kafka   |
| `SPRING_KAFKA_CONSUMER_GROUP_ID`          | `cinelog-group`  | Consumer group  |
| `SPRING_KAFKA_CONSUMER_AUTO_OFFSET_RESET` | `earliest`       | Offset strategy |

### Segurança

| Variável                                        | Default                 | Descrição         |
| ----------------------------------------------- | ----------------------- | ----------------- |
| `CINELOG_SECURITY_JWT_SECRET`                   | `devKey...`             | Chave HMAC-SHA256 |
| `CINELOG_SECURITY_JWT_ACCESS_TOKEN_EXPIRATION`  | `15m`                   | TTL access token  |
| `CINELOG_SECURITY_JWT_REFRESH_TOKEN_EXPIRATION` | `7d`                    | TTL refresh token |
| `CINELOG_SECURITY_BCRYPT_STRENGTH`              | `12`                    | Rounds do BCrypt  |
| `CORS_ALLOWED_ORIGINS`                          | `http://localhost:3000` | Origens CORS      |

### TMDb

| Variável              | Default                        | Descrição      |
| --------------------- | ------------------------------ | -------------- |
| `TMDB_API_KEY`        | —                              | API key v3     |
| `TMDB_BASE_URL`       | `https://api.themoviedb.org/3` | URL base       |
| `TMDB_IMAGE_BASE_URL` | `https://image.tmdb.org/t/p`   | URL de imagens |

### Outbox

| Variável                          | Default | Descrição                 |
| --------------------------------- | ------- | ------------------------- |
| `CINELOG_OUTBOX_POLLING_INTERVAL` | `5000`  | Intervalo de polling (ms) |
| `CINELOG_OUTBOX_BATCH_SIZE`       | `50`    | Tamanho do batch          |
| `CINELOG_OUTBOX_MAX_RETRIES`      | `5`     | Max tentativas            |

### Observabilidade

| Variável                                    | Default                          | Descrição                   |
| ------------------------------------------- | -------------------------------- | --------------------------- |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info,metrics,prometheus` | Endpoints Actuator expostos |
| `OTEL_EXPORTER_OTLP_ENDPOINT`               | `http://localhost:4317`          | Endpoint OTLP               |
| `OTEL_SERVICE_NAME`                         | `cinelog`                        | Nome do serviço             |

---

## application.yml

### Estrutura

```
src/main/resources/
├── application.yml            # Configurações base
├── application-dev.yml        # Override para dev
├── application-test.yml       # Override para testes
├── application-docker.yml     # Override para Docker
├── application-perf.yml       # Override para performance
└── logback-spring.xml         # Configuração de logging
```

### Configurações Principais

```yaml
# application.yml
spring:
    application:
        name: cinelog

    # JPA
    jpa:
        hibernate:
            ddl-auto: none
        open-in-view: false
        properties:
            hibernate.format_sql: false

    # Liquibase
    liquibase:
        change-log: classpath:db/changelog/changelog-master.xml

    # Cache
    cache:
        type: redis

# Actuator
management:
    endpoints:
        web:
            exposure:
                include: health,info,metrics,prometheus
    endpoint:
        health:
            show-details: when-authorized
            probes:
                enabled: true

# Resilience4j
resilience4j:
    circuitbreaker:
        instances:
            tmdb:
                sliding-window-size: 10
                failure-rate-threshold: 50
                wait-duration-in-open-state: 30s
                permitted-number-of-calls-in-half-open-state: 3
                register-health-indicator: true
    retry:
        instances:
            tmdb:
                max-attempts: 3
                wait-duration: 2s
                exponential-backoff-multiplier: 2
    timelimiter:
        instances:
            tmdb:
                timeout-duration: 3s
    bulkhead:
        instances:
            tmdb:
                max-concurrent-calls: 10
                max-wait-duration: 500ms
```

---

## Resilience4j

### Circuit Breaker (TMDb)

| Parâmetro                                      | Valor | Descrição            |
| ---------------------------------------------- | ----- | -------------------- |
| `sliding-window-size`                          | 10    | Janela de avaliação  |
| `failure-rate-threshold`                       | 50%   | Threshold para abrir |
| `wait-duration-in-open-state`                  | 30s   | Tempo em OPEN        |
| `permitted-number-of-calls-in-half-open-state` | 3     | Calls em HALF_OPEN   |

### Estados do Circuit Breaker

```
CLOSED ──(50% failures)──► OPEN ──(30s)──► HALF_OPEN
  ▲                                            │
  └────────(success rate OK)───────────────────┘
```

---

## Logging

### Logback Config

```xml
<!-- logback-spring.xml -->
<configuration>
  <springProfile name="dev">
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
      <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
      </encoder>
    </appender>
  </springProfile>

  <springProfile name="docker,prod">
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
  </springProfile>
</configuration>
```

### Log Levels por Profile

| Package                        | dev   | docker/prod |
| ------------------------------ | ----- | ----------- |
| `com.cine.cinelog`             | DEBUG | INFO        |
| `org.hibernate.SQL`            | DEBUG | WARN        |
| `org.springframework.security` | DEBUG | WARN        |
| `ROOT`                         | INFO  | WARN        |

---

## Internacionalização (i18n)

Mensagens de validação e erro são externalizadas:

```
src/main/resources/i18n/
├── messages.properties          # Default (pt-BR)
└── messages_en.properties       # English
```

---

## Referências

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Resilience4j Configuration](https://resilience4j.readme.io/docs/getting-started-3)
- [Getting Started](Getting-Started)
