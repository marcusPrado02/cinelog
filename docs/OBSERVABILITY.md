# 📊 Documentação de Observabilidade - CineLog

## Índice

1. [Visão Geral](#visão-geral)
2. [Três Pilares](#três-pilares)
3. [Métricas](#métricas)
4. [Logs](#logs)
5. [Tracing](#tracing)
6. [Dashboards](#dashboards)
7. [Alertas](#alertas)
8. [Troubleshooting](#troubleshooting)

---

## Visão Geral

O CineLog implementa observabilidade completa seguindo os **três pilares**:

1. **Logs**: O que aconteceu
2. **Métricas**: Quão bem está funcionando
3. **Traces**: Como as requisições fluem

### Stack de Observabilidade

```
┌─────────────────────────────────────────────────────────┐
│                    Application                          │
│          (Spring Boot + Micrometer + OTEL)              │
└─────────────┬───────────────────────┬───────────────────┘
              │                       │
              ▼                       ▼
    ┌─────────────────┐    ┌──────────────────┐
    │   Prometheus    │    │  OpenTelemetry   │
    │   (Métricas)    │    │    Collector     │
    └────────┬────────┘    └────────┬─────────┘
             │                      │
             │        ┌─────────────┴────────┐
             │        ▼                      ▼
             │  ┌──────────┐         ┌──────────┐
             │  │  Tempo   │         │ Logstash │
             │  │(Traces)  │         │  (Logs)  │
             │  └────┬─────┘         └────┬─────┘
             │       │                    │
             ▼       ▼                    ▼
    ┌────────────────────────────────────────┐
    │            Grafana                     │
    │  (Visualização e Dashboards)           │
    └────────────────────────────────────────┘
```

---

## Três Pilares

### 1. Logs

**O que**: Eventos discretos com timestamp

**Quando**: Debugging, auditoria, troubleshooting

**Formato**: JSON estruturado

**Exemplo**:

```json
{
    "timestamp": "2025-12-10T10:30:00.123Z",
    "level": "INFO",
    "logger": "com.cine.cinelog.features.media.web.controller.MediaController",
    "message": "Creating new media",
    "traceId": "abc123",
    "spanId": "def456",
    "userId": "user123",
    "mediaId": null,
    "duration": null
}
```

### 2. Métricas

**O que**: Medições numéricas ao longo do tempo

**Quando**: Monitoramento, alertas, capacity planning

**Tipos**:

-   **Counter**: Valor que só aumenta (ex: total de requisições)
-   **Gauge**: Valor que pode subir/descer (ex: memória usada)
-   **Histogram**: Distribuição de valores (ex: latência)
-   **Summary**: Similar a histogram, mas com quantis

**Exemplo**:

```
media_created_total{type="MOVIE"} 150
http_server_requests_seconds_count{uri="/api/v1/media"} 1000
jvm_memory_used_bytes{area="heap"} 524288000
```

### 3. Traces

**O que**: Jornada de uma requisição através do sistema

**Quando**: Performance, debugging distribuído

**Estrutura**:

```
Trace: POST /api/v1/media (200ms)
├── Span: MediaController.create (10ms)
├── Span: CreateMediaService.create (50ms)
│   ├── Span: MediaValidator.validate (5ms)
│   └── Span: MediaRepositoryAdapter.save (40ms)
│       └── Span: MySQL INSERT (30ms)
└── Span: MediaMapper.toResponse (5ms)
```

---

## Métricas

### Configuração

```yaml
# application.yml
management:
    endpoints:
        web:
            exposure:
                include: health,info,metrics,prometheus
    endpoint:
        health:
            show-details: always
        metrics:
            enabled: true
    metrics:
        tags:
            application: cinelog
            environment: ${SPRING_PROFILES_ACTIVE}
        export:
            prometheus:
                enabled: true
```

### Métricas Customizadas

#### Counter

```java
@Service
@RequiredArgsConstructor
public class CreateMediaService implements CreateMediaUseCase {

    private final MeterRegistry meterRegistry;
    private final Counter mediaCreatedCounter;

    @PostConstruct
    public void init() {
        mediaCreatedCounter = Counter.builder("media.created")
            .description("Total de mídias criadas")
            .tag("type", "all")
            .register(meterRegistry);
    }

    @Override
    public Media create(CreateMediaCommand command) {
        Media media = // ... criar mídia

        mediaCreatedCounter.increment();

        // Counter por tipo
        Counter.builder("media.created")
            .tag("type", media.getType().name())
            .register(meterRegistry)
            .increment();

        return media;
    }
}
```

#### Gauge

```java
@Component
public class DatabaseMetrics {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("database.connections.active", dataSource, ds -> {
            try {
                HikariDataSource hikariDS = (HikariDataSource) ds;
                return hikariDS.getHikariPoolMXBean().getActiveConnections();
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Conexões ativas com o banco")
        .register(meterRegistry);
    }
}
```

#### Timer

```java
@Service
public class MediaService {

    private final MeterRegistry meterRegistry;

    public Media findById(Long id) {
        Timer timer = Timer.builder("media.find.duration")
            .description("Tempo para buscar mídia")
            .register(meterRegistry);

        return timer.record(() -> {
            return mediaRepository.findById(id);
        });
    }
}
```

### Métricas de Negócio

```java
@Aspect
@Component
public class BusinessMetricsAspect {

    @Autowired
    private MeterRegistry meterRegistry;

    @Around("@annotation(Metered)")
    public Object measureBusinessMetric(ProceedingJoinPoint joinPoint) throws Throwable {
        String metricName = joinPoint.getSignature().getName();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            sample.stop(Timer.builder("business.operation")
                .tag("operation", metricName)
                .tag("status", "success")
                .register(meterRegistry));

            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("business.operation")
                .tag("operation", metricName)
                .tag("status", "error")
                .register(meterRegistry));

            throw e;
        }
    }
}

// Uso
@Metered
public Media createMedia(CreateMediaCommand command) {
    // ...
}
```

### Prometheus Scraping

```yaml
# prometheus.yml
global:
    scrape_interval: 15s
    evaluation_interval: 15s

scrape_configs:
    - job_name: "cinelog"
      metrics_path: "/actuator/prometheus"
      static_configs:
          - targets: ["localhost:8080"]
```

---

## Logs

### Configuração (Logback)

```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <customFields>{"app":"cinelog"}</customFields>
        </encoder>
    </appender>

    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/cinelog.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/cinelog.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

    <!-- Application logs -->
    <logger name="com.cine.cinelog" level="DEBUG"/>

    <!-- SQL logs -->
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE"/>
</configuration>
```

### Logs Estruturados

```java
@Slf4j
@Service
public class MediaService {

    public Media createMedia(CreateMediaCommand command) {
        log.info("Creating media",
            kv("title", command.getTitle()),
            kv("type", command.getType()),
            kv("year", command.getReleaseYear())
        );

        try {
            Media media = // ... criar

            log.info("Media created successfully",
                kv("mediaId", media.getId()),
                kv("title", media.getTitle())
            );

            return media;
        } catch (Exception e) {
            log.error("Failed to create media",
                kv("title", command.getTitle()),
                kv("error", e.getMessage()),
                e
            );
            throw e;
        }
    }
}
```

**Output JSON**:

```json
{
    "timestamp": "2025-12-10T10:30:00.123Z",
    "level": "INFO",
    "logger": "com.cine.cinelog.service.MediaService",
    "message": "Creating media",
    "title": "Matrix",
    "type": "MOVIE",
    "year": 1999,
    "traceId": "abc123",
    "spanId": "def456"
}
```

### MDC (Mapped Diagnostic Context)

```java
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("userId", getUserId(request));

        try {
            response.addHeader("X-Request-Id", requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### Logstash Pipeline

```conf
# logstash/pipeline/cinelog.conf
input {
  file {
    path => "/var/log/cinelog/*.log"
    codec => json
    type => "cinelog"
  }
}

filter {
  if [type] == "cinelog" {
    json {
      source => "message"
    }

    date {
      match => ["timestamp", "ISO8601"]
      target => "@timestamp"
    }

    mutate {
      add_field => {
        "[@metadata][index]" => "cinelog-%{+YYYY.MM.dd}"
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "%{[@metadata][index]}"
  }
}
```

---

## Tracing

### Configuração OpenTelemetry

```yaml
# application.yml
management:
    tracing:
        sampling:
            probability: 1.0 # 100% em dev, 0.1 (10%) em prod
        enabled: true
    otlp:
        tracing:
            endpoint: http://localhost:4318/v1/traces
```

### Instrumentação Automática

Spring Boot 3 + Micrometer Tracing instrumentam automaticamente:

-   HTTP requests/responses
-   JDBC queries
-   JPA operations
-   RestTemplate/WebClient calls
-   Kafka producers/consumers

### Spans Customizados

```java
@Service
@RequiredArgsConstructor
public class MediaService {

    private final Tracer tracer;

    public Media enrichMedia(Media media) {
        Span span = tracer.spanBuilder("enrich-media")
            .setAttribute("media.id", media.getId())
            .setAttribute("media.type", media.getType().name())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Operações de enriquecimento
            fetchExternalData(media);
            processCredits(media);

            span.addEvent("Media enriched successfully");
            return media;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Propagação de Contexto

```java
@Service
public class ExternalApiClient {

    private final RestTemplate restTemplate;

    public ExternalData fetchData(String url) {
        // Context é propagado automaticamente via headers
        // W3C Trace Context: traceparent, tracestate

        return restTemplate.getForObject(url, ExternalData.class);
    }
}
```

---

## Dashboards

### Grafana Dashboard: Overview

```json
{
    "dashboard": {
        "title": "CineLog - Overview",
        "panels": [
            {
                "title": "Request Rate",
                "targets": [
                    {
                        "expr": "rate(http_server_requests_seconds_count[5m])"
                    }
                ]
            },
            {
                "title": "Error Rate",
                "targets": [
                    {
                        "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])"
                    }
                ]
            },
            {
                "title": "Latency (p95)",
                "targets": [
                    {
                        "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
                    }
                ]
            },
            {
                "title": "JVM Memory",
                "targets": [
                    {
                        "expr": "jvm_memory_used_bytes{area=\"heap\"}"
                    }
                ]
            }
        ]
    }
}
```

### Dashboard: Business Metrics

-   Total de mídias criadas
-   Mídias por tipo (filme vs série)
-   Registros de visualização por dia
-   Usuários ativos
-   Top 10 mídias mais assistidas

---

## Alertas

### Prometheus Alerts

```yaml
# alerts.yml
groups:
    - name: cinelog
      interval: 30s
      rules:
          - alert: HighErrorRate
            expr: |
                rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
            for: 5m
            labels:
                severity: critical
            annotations:
                summary: "Taxa de erro alta (> 5%)"
                description: "{{ $value }}% de requisições falhando"

          - alert: HighLatency
            expr: |
                histogram_quantile(0.95, 
                  rate(http_server_requests_seconds_bucket[5m])
                ) > 1
            for: 5m
            labels:
                severity: warning
            annotations:
                summary: "Latência alta (p95 > 1s)"

          - alert: DatabaseConnectionPoolExhausted
            expr: |
                hikaricp_connections_active / hikaricp_connections_max > 0.9
            for: 2m
            labels:
                severity: warning
            annotations:
                summary: "Pool de conexões quase esgotado (> 90%)"
```

### Alertmanager

```yaml
# alertmanager.yml
route:
    group_by: ["alertname", "severity"]
    group_wait: 10s
    group_interval: 10s
    repeat_interval: 12h
    receiver: "team-email"
    routes:
        - match:
              severity: critical
          receiver: "pagerduty"

receivers:
    - name: "team-email"
      email_configs:
          - to: "team@cinelog.com"

    - name: "pagerduty"
      pagerduty_configs:
          - service_key: "your-pagerduty-key"
```

---

## Troubleshooting

### 1. Identificar Problema

```bash
# Verificar health
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Logs recentes
tail -f logs/cinelog.log | jq
```

### 2. Correlacionar Eventos

Usar `traceId` para correlacionar logs, métricas e traces:

```
TraceID: abc123
├── Logs: grep "abc123" logs/*.log
├── Metrics: {traceId="abc123"}
└── Traces: Query in Tempo/Jaeger
```

### 3. Analisar Performance

```promql
# Latência por endpoint
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])
) by (uri)

# Throughput
rate(http_server_requests_seconds_count[5m])

# Taxa de erro
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
 / rate(http_server_requests_seconds_count[5m])
```

---

**Última atualização**: Dezembro 2025
