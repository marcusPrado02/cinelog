# Design Spec: Spring Cloud Data Flow Integration

**Data:** 2026-03-18
**Status:** Aprovado
**Autor:** CineLog Team
**Escopo:** Integração do SCDF para gestão profissional dos Spring Batch jobs

---

## 1. Contexto e Motivação

O CineLog possui 8 Spring Batch jobs para importação de dados do TMDB. Atualmente o
agendamento e disparo são gerenciados por dois mecanismos internos à aplicação:

- `BatchSchedulerConfig` — crons via `@Scheduled` no `application.yml`
- `BatchJobController` — endpoints HTTP `/api/v1/admin/batch/*`

**Problemas do modelo atual:**
- Crons hardcoded no `application.yml` exigem redeploy para alteração
- Sem histórico centralizado de execuções persistido fora do processo
- Sem dashboard visual para monitoramento e reexecução de jobs
- Sem suporte a pause/resume de schedules sem código

**Objetivo:** Integrar Spring Cloud Data Flow (SCDF) como plataforma de gestão de
batch jobs, mantendo compatibilidade com a arquitetura existente.

---

## 2. Decisões de Design

| Dimensão | Decisão | Justificativa |
|---|---|---|
| Ambiente | Docker Compose local | Sem Kubernetes no escopo atual |
| Deployment | SCDF Server + Skipper | Suporte completo a tasks + streams |
| Deployer | Docker Deployer | Lança tasks como containers Docker |
| Empacotamento | Monolito como Task única | Zero refatoração de domínio |
| Agendamento | Migrar 100% para SCDF Scheduler | Fonte única de verdade |
| Segurança | Basic Auth default + Keycloak opcional | Consistente com IAM existente |
| Schema SCDF | Co-localizado em `cinelog` com prefixo `SCDF_TASK_` | Evita datasource duplo |
| Disparo de job | Profile `task` + `spring.batch.job.name` + `spring.batch.job.enabled=true` | Execução controlada pelo SCDF |
| Métricas de Task | OTLP push via `OtlpMeterRegistry` (flush no shutdown do contexto) | Containers efêmeros incompatíveis com Prometheus pull |
| Datasource no container | Variáveis de ambiente injetadas pelo SCDF Docker Deployer | Hostname correto (`db:3306`) sem alterar application.yml |

---

## 3. Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Compose Network                       │
│                                                                  │
│  ┌──────────────────────┐    ┌─────────────────────────────┐   │
│  │   SCDF Server :9393  │───▶│   Skipper Server :7577      │   │
│  │  Dashboard + REST    │    │   Stream lifecycle mgmt     │   │
│  │  Scheduler + Tasks   │    └─────────────────────────────┘   │
│  └──────────┬───────────┘                                       │
│             │ Docker Deployer (via /var/run/docker.sock)        │
│             ▼                                                    │
│  ┌──────────────────────────┐   ┌───────────────────────────┐  │
│  │  cinelog-app:latest      │──▶│      MySQL :3306          │  │
│  │  (Task container efêmero)│   │  schema: cinelog          │  │
│  │  profile: task           │   │  - BATCH_* (Spring Batch) │  │
│  │  SPRING_DATASOURCE_URL=  │   │  - SCDF_TASK_* (SCT)      │  │
│  │    jdbc:mysql://db:3306/ │   │  - domínio (app)          │  │
│  └──────────┬───────────────┘   └───────────────────────────┘  │
│             │ OTLP push (flush on context close)                │
│             ▼                                                    │
│  ┌──────────────────────┐                                       │
│  │  OTel Collector:4318 │──▶ Tempo + Prometheus (métricas)     │
│  └──────────────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
```

### 3.1 Decisão de schema: co-localização com prefixo

Tabelas Spring Cloud Task ficam no schema `cinelog` com prefixo `SCDF_TASK_`, configurado
via `spring.cloud.task.tablePrefix: SCDF_TASK_`. Tabelas Spring Batch mantêm o prefixo
padrão `BATCH_`. SCDF Server e Skipper usam o schema `scdf` (criado pelo `mysql-init.sql`
com grants explícitos ao usuário `cinelog`).

### 3.2 Fluxo de Execução de Task

```
SCDF Scheduler (cron)
  └─▶ SCDF Server: lança task "importMoviesJob"
        └─▶ Docker Deployer: docker run cinelog-app:latest
              --spring.profiles.active=task
              --spring.batch.job.name=importMoviesJob
              Env: SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/cinelog (host Docker)
              Env: SPRING_DATA_REDIS_HOST=redis
              Env: SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
              └─▶ Profile "task": spring.batch.job.enabled=true
              └─▶ JobLauncherApplicationRunner: executa job pelo nome
              └─▶ @EnableTask: registra em cinelog.SCDF_TASK_EXECUTION
              └─▶ BatchJobMetricsListener: acumula métricas Micrometer
              └─▶ closecontextEnabled=true: contexto Spring encerra
              └─▶ OtlpMeterRegistry.close(): flush final das métricas
        └─▶ SCDF Server: atualiza status da task
```

---

## 4. Componentes

### 4.1 Novos arquivos

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| `src/main/java/.../batch/config/TaskConfig.java` | Java | `@EnableTask` |
| `src/main/resources/application-task.yml` | YAML | Profile para execução como Task SCDF |
| `Dockerfile` | Docker | Imagem multi-stage JRE 21 para Docker Deployer |
| `docker/scdf/init-scdf.sh` | Shell | Script idempotente de registro de tasks/schedules |
| `docker/scdf/deployer-env.properties` | Properties | Variáveis de ambiente injetadas pelo Deployer |

### 4.2 Arquivos removidos

| Arquivo | Motivo |
|---------|--------|
| `BatchSchedulerConfig.java` | SCDF Scheduler assume o agendamento |
| Blocos de cron em `application.yml` | Persistidos no schema `scdf` via REST API |

### 4.3 Arquivos modificados

| Arquivo | O que muda |
|---------|------------|
| `docker-compose.yml` | Adiciona `dataflow-server` e `skipper-server` |
| `docker/mysql-init.sql` | Adiciona `CREATE DATABASE scdf` + `GRANT ... ON scdf.*` |
| `docker/keycloak/cinelog-realm.json` | Adiciona client `scdf-server` (necessário para `SCDF_AUTH_MODE=keycloak`) |
| `BatchJobController.java` | Adiciona `@Deprecated` + JavaDoc indicando SCDF REST API como preferido |
| `BatchJobMetricsListener.java` | Corrige registro como `StepExecutionListener` nos Steps |
| `BatchJobsConfig.java` | Adiciona `.listener(metricsListener)` em todos os `StepBuilder` |
| `pom.xml` | Spring Cloud BOM + `spring-cloud-starter-task` + `micrometer-registry-otlp` |
| `application.yml` | Adiciona `spring.cloud.task.*`; remove blocos de cron |

---

## 5. Correção: StepExecutionListener não registrado (bug pré-existente)

`BatchJobMetricsListener` implementa `StepExecutionListener` mas está registrado
apenas via `JobBuilder.listener()`, nunca nos `StepBuilder`. Os contadores por step
(`batch_step_items_read_total`, etc.) nunca disparam. Deve ser corrigido nesta entrega:

```java
// BatchJobsConfig.java — em cada StepBuilder (chunk-oriented e tasklet)
return new StepBuilder("importMoviesStep", jobRepository)
    .<TmdbMediaSummary, MediaWithGenres>chunk(props.getChunkSize(), txManager)
    .reader(...)
    .processor(...)
    .writer(...)
    .listener(metricsListener)   // ← adicionar em todos os steps
    .faultTolerant()
    ...
    .build();
```

---

## 6. Segurança do SCDF Server

### 6.1 Basic Auth (padrão)

```yaml
# docker-compose.yml → dataflow-server
environment:
  SPRING_SECURITY_USER_NAME: admin
  SPRING_SECURITY_USER_PASSWORD: ${SCDF_ADMIN_PASSWORD:-Admin@CineLog2025!}
```

Dashboard: `http://localhost:9393/dashboard`

### 6.2 Keycloak OAuth2 (opcional via `SCDF_AUTH_MODE=keycloak`)

```yaml
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_ID: scdf-server
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_SECRET: ${KEYCLOAK_SCDF_SECRET}
SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUER_URI: http://keycloak:8080/realms/cinelog
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/cinelog
```

**Pré-requisito:** O arquivo `docker/keycloak/cinelog-realm.json` deve incluir o client
`scdf-server` com:
- `clientId: scdf-server`
- `publicClient: false` (cliente confidencial — obrigatório para uso de secret)
- `secret: ${KEYCLOAK_SCDF_SECRET}`
- `redirectUris: ["http://localhost:9393/login/oauth2/code/keycloak"]`
- `standardFlowEnabled: true`

Sem esse client no realm, o modo Keycloak retorna 401 no SCDF Dashboard.

### 6.3 Isolamento de rede

Porta `9393` exposta apenas em `127.0.0.1:9393:9393`. Docker socket montado
sem `:ro` — o SCDF Docker Deployer precisa de acesso de escrita para criar e
iniciar containers de Task: `/var/run/docker.sock:/var/run/docker.sock`.

---

## 7. mysql-init.sql — linhas a adicionar (arquivo já existente)

O arquivo atual já contém grants para `cinelog.*` e `audit.*`. **Não substituir** — apenas
acrescentar as seguintes linhas ao final:

```sql
-- Schema para SCDF Server e Skipper (acrescentar ao final do arquivo existente)
CREATE DATABASE IF NOT EXISTS scdf
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Grant ao usuário cinelog no schema scdf
-- (grant em cinelog.* e audit.* já existem no arquivo — não duplicar)
GRANT ALL PRIVILEGES ON scdf.* TO 'cinelog'@'%';
FLUSH PRIVILEGES;
```

---

## 8. Dockerfile

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S cinelog && adduser -S cinelog -G cinelog
COPY --from=build /app/target/cinelog-*.jar app.jar
USER cinelog
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 9. Variáveis de ambiente do Docker Deployer

O SCDF Docker Deployer injeta variáveis de ambiente no container da Task via
`deployer.cinelog.environmentVariables`. Arquivo `docker/scdf/deployer-env.properties`
usado no `init-scdf.sh` ao criar as task definitions:

```properties
# Datasource: hostname Docker (db) em vez de localhost
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/cinelog?useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=cinelog
SPRING_DATASOURCE_PASSWORD=cinelog

# Redis e Kafka no Docker network
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# OTLP para OTel Collector
MANAGEMENT_OTLP_TRACING_ENDPOINT=http://otel-collector:4318/v1/traces
MANAGEMENT_OTLP_METRICS_EXPORT_URL=http://otel-collector:4318/v1/metrics

# Profile ativa o modo task
SPRING_PROFILES_ACTIVE=task
```

Essas variáveis são passadas ao registrar cada task definition via REST API ou no
`init-scdf.sh` como `deployer.*.environmentVariables`.

---

## 10. Script init-scdf.sh

Executado **no host Docker** (não dentro de um container) via `bash docker/scdf/init-scdf.sh`.
Usa `localhost:9393` (porta exposta pelo container `dataflow-server`).

```bash
#!/usr/bin/env bash
set -euo pipefail

SCDF_URL="${SCDF_URL:-http://localhost:9393}"
SCDF_USER="${SCDF_USER:-admin}"
SCDF_PASS="${SCDF_PASS:-Admin@CineLog2025!}"
CINELOG_IMAGE="${CINELOG_IMAGE:-cinelog/cinelog-app:latest}"
AUTH="-u $SCDF_USER:$SCDF_PASS"

# Aguarda SCDF estar pronto (health endpoint não requer auth por padrão)
until curl -sf $AUTH "$SCDF_URL/management/health" > /dev/null; do
  echo "Aguardando SCDF Server em $SCDF_URL..."; sleep 5
done

echo "SCDF pronto. Registrando app..."

# Registra app — force=false: não sobrescreve se URI for igual.
# Para atualizar a URI da imagem (ex: nova tag), use force=true manualmente:
#   curl -X POST .../apps/task/cinelog -d "uri=..." -d "force=true"
curl -sf $AUTH -X POST "$SCDF_URL/apps/task/cinelog" \
  -d "uri=docker:$CINELOG_IMAGE" \
  -d "force=false" 2>/dev/null || echo "App 'cinelog' já registrado."

# Lê variáveis de ambiente do arquivo deployer-env.properties
# grep -v filtra linhas de comentário (#) e linhas em branco antes de unir com vírgula
DEPLOYER_ENV=$(grep -v '^\s*#' docker/scdf/deployer-env.properties \
  | grep -v '^\s*$' \
  | tr '\n' ',')

# Helper: cria task definition (idempotente — skip se já existe)
register_task() {
  local name=$1 job=$2
  if curl -sf $AUTH "$SCDF_URL/tasks/definitions/$name" > /dev/null 2>&1; then
    echo "Task '$name' já existe."; return 0
  fi
  curl -sf $AUTH -X POST "$SCDF_URL/tasks/definitions" \
    -d "name=$name" \
    -d "definition=cinelog --spring.batch.job.name=$job" \
    -d "deployer.cinelog.environmentVariables=${DEPLOYER_ENV%,}"
  echo "Task '$name' registrada."
}

register_task syncGenresJob           syncGenresJob
register_task importMoviesJob         importMoviesJob
register_task importTvShowsJob        importTvShowsJob
register_task importCreditsJob        importCreditsJob
register_task importSeasonsJob        importSeasonsJob
register_task syncReviewsJob          syncReviewsJob
register_task enrichMediaImagesJob    enrichMediaImagesJob
register_task enrichPersonProfilesJob enrichPersonProfilesJob

# Helper: cria schedule (idempotente — skip se já existe)
register_schedule() {
  local name=$1 task=$2 cron=$3
  if curl -sf $AUTH "$SCDF_URL/tasks/schedules/$name" > /dev/null 2>&1; then
    echo "Schedule '$name' já existe."; return 0
  fi
  curl -sf $AUTH -X POST "$SCDF_URL/tasks/schedules" \
    -d "scheduleName=$name" \
    -d "taskDefinitionName=$task" \
    -d "expression=$cron"
  echo "Schedule '$name' registrado."
}

register_schedule sched-sync-genres           syncGenresJob           "0 0 3 * * 0"
register_schedule sched-import-movies         importMoviesJob         "0 30 3 * * 0"
register_schedule sched-import-tvshows        importTvShowsJob        "0 0 4 * * 0"
register_schedule sched-import-credits        importCreditsJob        "0 30 4 * * 0"
register_schedule sched-import-seasons        importSeasonsJob        "0 0 5 * * 0"
register_schedule sched-sync-reviews          syncReviewsJob          "0 30 5 * * 0"
register_schedule sched-enrich-images         enrichMediaImagesJob    "0 0 6 * * 0"
register_schedule sched-enrich-profiles       enrichPersonProfilesJob "0 30 6 * * 0"

echo "SCDF: tasks e schedules registrados com sucesso."
```

---

## 11. application-task.yml (novo profile)

```yaml
# Profile "task" ativado pelo SCDF Docker Deployer:
# SPRING_PROFILES_ACTIVE=task (via deployer-env.properties)
spring:
  batch:
    job:
      enabled: true     # re-habilita JobLauncherApplicationRunner (desabilitado no base config)
      # spring.batch.job.name injetado via --spring.batch.job.name=<jobName> pelo SCDF
  cloud:
    task:
      closecontextEnabled: true           # encerra Spring context após execução da task
      executionIdEnvVar: SPRING_CLOUD_TASK_EXECUTION_ID  # camelCase obrigatório
      tablePrefix: SCDF_TASK_             # prefixo das tabelas SCT no schema cinelog

management:
  otlp:
    metrics:
      export:
        enabled: true
        url: http://otel-collector:4318/v1/metrics
        step: 5s
        # OtlpMeterRegistry implementa Closeable — ao fechar o contexto Spring
        # (via closecontextEnabled=true), o registry é fechado e um flush final
        # é emitido antes do JVM encerrar. Jobs que completem em menos de 5s
        # ainda terão métricas enviadas pelo flush de shutdown.
```

**Por que OTLP push e não Prometheus pull:**
Containers efêmeros lançados pelo SCDF não têm porta exposta nem tempo de vida suficiente
para o Prometheus raspar via pull. `OtlpMeterRegistry` implementa `Closeable` — o Spring
context close (acionado por `closecontextEnabled=true`) invoca `close()` no registry,
disparando um flush final garantido de todas as métricas acumuladas, independente do
intervalo `step`.

---

## 12. Dependências Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <!-- Série 2024.0.x compatível com Spring Boot 3.4/3.5               -->
      <!-- Verificar release atual em: https://spring.io/projects/spring-cloud -->
      <version>2024.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-task</artifactId>
  </dependency>

  <!-- OTLP metrics push para containers efêmeros -->
  <dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-otlp</artifactId>
  </dependency>
</dependencies>
```

---

## 13. Registro de Tasks e Schedules — tabela resumo

| Task Definition | Spring Batch Job | Cron SCDF |
|-----------------|-----------------|-----------|
| `syncGenresJob` | `syncGenresJob` | `0 0 3 * * 0` |
| `importMoviesJob` | `importMoviesJob` | `0 30 3 * * 0` |
| `importTvShowsJob` | `importTvShowsJob` | `0 0 4 * * 0` |
| `importCreditsJob` | `importCreditsJob` | `0 30 4 * * 0` |
| `importSeasonsJob` | `importSeasonsJob` | `0 0 5 * * 0` |
| `syncReviewsJob` | `syncReviewsJob` | `0 30 5 * * 0` |
| `enrichMediaImagesJob` | `enrichMediaImagesJob` | `0 0 6 * * 0` |
| `enrichPersonProfilesJob` | `enrichPersonProfilesJob` | `0 30 6 * * 0` |

---

## 14. Critérios de Aceite

| Critério | Como verificar |
|----------|----------------|
| SCDF Dashboard acessível em `localhost:9393` | Browser → login Basic Auth |
| 8 task definitions registradas | Dashboard → Tasks → Definitions |
| 8 schedules ativos | Dashboard → Tasks → Schedules |
| Task execução bem-sucedida via disparo manual | Dashboard → Trigger → status COMPLETE |
| Histórico persistido no MySQL | `SELECT * FROM cinelog.SCDF_TASK_EXECUTION` |
| `BatchSchedulerConfig` removido sem regressão | Testes existentes passam |
| Basic Auth funcional | Login com admin/senha configurada |
| Keycloak OAuth2 funcional | `SCDF_AUTH_MODE=keycloak` → login via Keycloak com client `scdf-server` |
| Métricas `batch_job_*` chegam ao Prometheus | `localhost:9090` → `batch_job_duration_seconds` |
| Métricas `batch_step_*` chegam ao Prometheus | `localhost:9090` → `batch_step_items_written_total` > 0 |
| Container da Task usa `db:3306` | Logs do container mostram conexão MySQL bem-sucedida |

---

## 15. Fora do Escopo

- Kubernetes deployer
- SCDF Streams (pipelines em tempo real)
- Composed Tasks (chaining de jobs)
- Alta disponibilidade do SCDF Server

---

## 16. Referências

- [Spring Cloud Data Flow Docs](https://dataflow.spring.io/docs/)
- [Spring Cloud Task Reference](https://docs.spring.io/spring-cloud-task/docs/current/reference/)
- [SCDF Docker Compose Guide](https://dataflow.spring.io/docs/installation/local/docker/)
- [Spring Cloud Release Calendar](https://spring.io/projects/spring-cloud)
- [ADR-013: API Versioning](../adr/ADR-013-api-versioning-strategy.md)
- [BATCH-PERFORMANCE.md](../BATCH-PERFORMANCE.md)
- `BatchJobsConfig.java`, `BatchJobMetricsListener.java`
