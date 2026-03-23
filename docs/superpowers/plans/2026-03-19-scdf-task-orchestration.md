# SCDF Task Orchestration — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Spring Cloud Data Flow the real orchestrator of CineLog's 9 batch jobs, launching the app as ephemeral Docker containers per job execution.

**Architecture:** SCDF Server uses Docker Deployer to launch `cinelog-app:latest` containers with `--spring.profiles.active=task --spring.batch.job.name=<jobName>`. Each container runs one job, registers execution in `SCDF_TASK_*` tables, pushes metrics via OTLP, then shuts down. The existing `BatchSchedulerConfig` is removed; SCDF Scheduler takes over all cron scheduling.

**Tech Stack:** Spring Cloud Task 3.x, Spring Batch 5, SCDF 2.11.5, Docker Deployer, Micrometer OTLP, MySQL 8.0

**Spec:** `docs/superpowers/specs/2026-03-18-scdf-integration-design.md`

---

## File Structure

### New Files

| File | Responsibility |
|------|----------------|
| `src/main/java/com/cine/cinelog/features/batch/config/TaskConfig.java` | `@EnableTask` + Spring Cloud Task configuration |
| `src/main/resources/application-task.yml` | Profile `task`: enables job execution, OTLP push, context close |
| `docker/scdf/init-scdf.sh` | Idempotent script to register app, tasks, and schedules in SCDF |
| `docker/scdf/deployer-env.properties` | Environment variables injected by Docker Deployer into task containers |

### Modified Files

| File | What Changes |
|------|-------------|
| `pom.xml` | Add Spring Cloud BOM + `spring-cloud-starter-task` + `micrometer-registry-otlp` |
| `Dockerfile` | Fix datasource URL (MySQL not Postgres), use alpine image, non-root user |
| `docker-compose.yml` | Add Skipper server, mount Docker socket on SCDF, add `cinelog-app` build |
| `docker/mysql-init.sql` | Already has `scdf` schema; no change needed |
| `src/main/java/.../batch/config/BatchJobsConfig.java` | Verify `.listener(metricsListener)` on all StepBuilder instances |
| `src/main/java/.../batch/scheduler/BatchSchedulerConfig.java` | Delete entirely (SCDF takes over scheduling) |
| `src/main/java/.../batch/web/BatchJobController.java` | Add `@Deprecated` annotation + JavaDoc pointing to SCDF |
| `src/main/resources/application.yml` | Remove cron blocks from `cinelog.batch.jobs.*` (SCDF manages schedules) |

---

## Task 1: Maven Dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Spring Cloud BOM to dependencyManagement**

In `pom.xml`, inside `<dependencyManagement><dependencies>`, add after the `commons-lang3` block (before closing `</dependencies>`):

```xml
<!-- Spring Cloud BOM — serie 2024.0.x compativel com Spring Boot 3.4/3.5 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-dependencies</artifactId>
    <version>2024.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

- [ ] **Step 2: Add spring-cloud-starter-task dependency**

In `pom.xml`, inside `<dependencies>`, add after `spring-boot-starter-batch`:

```xml
<!-- Spring Cloud Task — SCDF task lifecycle management -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-task</artifactId>
</dependency>
```

- [ ] **Step 3: Add micrometer-registry-otlp dependency**

In `pom.xml`, inside `<dependencies>`, add after `micrometer-registry-prometheus`:

```xml
<!-- OTLP metrics push for ephemeral task containers -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-otlp</artifactId>
</dependency>
```

- [ ] **Step 4: Verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add pom.xml
git commit -m "feat(scdf): add Spring Cloud Task and OTLP dependencies"
```

---

## Task 2: TaskConfig — @EnableTask

**Files:**
- Create: `src/main/java/com/cine/cinelog/features/batch/config/TaskConfig.java`

- [ ] **Step 1: Create TaskConfig.java**

```java
package com.cine.cinelog.features.batch.config;

import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Habilita Spring Cloud Task quando a app roda como task efemera do SCDF.
 *
 * <p>Ativado apenas com o profile {@code task} (injetado pelo SCDF Docker Deployer
 * via {@code SPRING_PROFILES_ACTIVE=task}). No modo servidor (profile {@code dev}),
 * esta configuracao nao e carregada.</p>
 *
 * <p>O @EnableTask registra a execucao nas tabelas {@code SCDF_TASK_*} do schema
 * {@code cinelog}, permitindo ao SCDF Dashboard rastrear historico de execucoes.</p>
 */
@Configuration
@EnableTask
@Profile("task")
public class TaskConfig {
}
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/cine/cinelog/features/batch/config/TaskConfig.java
git commit -m "feat(scdf): add TaskConfig with @EnableTask for SCDF integration"
```

---

## Task 3: application-task.yml — Task Profile

**Files:**
- Create: `src/main/resources/application-task.yml`

- [ ] **Step 1: Create application-task.yml**

```yaml
# Profile "task" — ativado pelo SCDF Docker Deployer via SPRING_PROFILES_ACTIVE=task
# Configura a app para rodar como container efemero executando um unico batch job.
spring:
  batch:
    job:
      enabled: true  # Habilita JobLauncherApplicationRunner (desabilitado no profile base)
      # spring.batch.job.name e injetado pelo SCDF via --spring.batch.job.name=<jobName>

  cloud:
    task:
      close-context-enabled: true                    # Encerra Spring context apos execucao
      table-prefix: SCDF_TASK_                       # Prefixo das tabelas no schema cinelog
      initialize-enabled: true                       # Cria tabelas SCDF_TASK_* se nao existirem
      execution-id-env-var: SPRING_CLOUD_TASK_EXECUTION_ID

  # Desabilita schedulers internos — SCDF gerencia os crons
  main:
    allow-bean-definition-overriding: false

# OTLP push para containers efemeros (incompativeis com Prometheus pull)
management:
  otlp:
    metrics:
      export:
        enabled: true
        url: ${MANAGEMENT_OTLP_METRICS_EXPORT_URL:http://otel-collector:4318/v1/metrics}
        step: 5s
```

- [ ] **Step 2: Verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application-task.yml
git commit -m "feat(scdf): add application-task.yml profile for SCDF task execution"
```

---

## Task 4: Fix Dockerfile

**Files:**
- Modify: `Dockerfile`

- [ ] **Step 1: Update Dockerfile**

The current Dockerfile has a wrong datasource URL pointing to PostgreSQL and uses a heavy base image. Replace the entire content:

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2: runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user
RUN addgroup -S cinelog && adduser -S cinelog -G cinelog

COPY --from=builder /app/target/*.jar app.jar
RUN chown cinelog:cinelog app.jar

USER cinelog
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Key changes from the original:
- Removed wrong PostgreSQL ENV defaults (MySQL is the actual DB)
- Switched to `eclipse-temurin:21-jre-alpine` (lighter image)
- Removed `apt-get install curl` (not needed for task containers)
- Removed HEALTHCHECK (ephemeral task containers don't need it)
- Simplified non-root user creation for Alpine

- [ ] **Step 2: Build Docker image**

Run: `docker build -t cinelog/cinelog-app:latest .`
Expected: Successfully built and tagged

- [ ] **Step 3: Commit**

```bash
git add Dockerfile
git commit -m "fix(docker): update Dockerfile to alpine, fix datasource, remove Postgres refs"
```

---

## Task 5: docker-compose.yml — Skipper + Docker Socket

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add Skipper server service**

Add after the `dataflow-server` service block, before `volumes:`:

```yaml
    # ========= SCDF Skipper Server =========
    # Gerencia deployment de tasks via Docker Deployer.
    skipper-server:
        image: springcloud/spring-cloud-skipper-server:2.11.5
        container_name: cinelog-skipper
        ports:
            - "7577:7577"
        environment:
            SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/scdf?useSSL=false&allowPublicKeyRetrieval=true&permitMysqlScheme=true
            SPRING_DATASOURCE_USERNAME: cinelog
            SPRING_DATASOURCE_PASSWORD: cinelog
            SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.mariadb.jdbc.Driver
        depends_on:
            db:
                condition: service_healthy
        volumes:
            - /var/run/docker.sock:/var/run/docker.sock
        healthcheck:
            test: ["CMD-SHELL", "bash -c 'echo > /dev/tcp/localhost/7577' || exit 1"]
            interval: 30s
            timeout: 10s
            retries: 5
            start_period: 90s
        restart: unless-stopped
```

- [ ] **Step 2: Update dataflow-server to connect to Skipper**

Add these environment variables to the `dataflow-server` service:

```yaml
            SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI: http://skipper-server:7577/api
            SPRING_CLOUD_DATAFLOW_FEATURES_TASKS_ENABLED: "true"
```

Add `skipper-server` to `depends_on`:

```yaml
        depends_on:
            db:
                condition: service_healthy
            skipper-server:
                condition: service_healthy
```

Also mount Docker socket on `dataflow-server`:

```yaml
        volumes:
            - /var/run/docker.sock:/var/run/docker.sock
```

- [ ] **Step 3: Add cinelog-app build service**

Add to the `test-everything.sh` expected containers list. Also add the `cinelog-skipper` container name.

In `scripts/test-everything.sh`, update `ALL_EXPECTED` array to include `cinelog-skipper`:

```bash
ALL_EXPECTED=("cinelog-mysql" "cinelog-redis" "cinelog-keycloak" "cinelog-mailhog"
    "cinelog-grafana" "cinelog-loki" "cinelog-prometheus" "cinelog-jaeger" "cinelog-tempo"
    "cinelog-otel-collector" "cinelog-promtail" "cinelog-dataflow" "cinelog-skipper"
    "cinelog-elasticsearch" "cinelog-kibana" "cinelog-logstash")
```

- [ ] **Step 4: Verify docker-compose is valid**

Run: `docker compose config --quiet`
Expected: No errors

- [ ] **Step 5: Start Skipper and verify**

Run: `docker compose up -d skipper-server`
Wait 90 seconds, then: `docker ps --format '{{.Names}} {{.Status}}' | grep skipper`
Expected: `cinelog-skipper Up ... (healthy)`

- [ ] **Step 6: Restart dataflow-server with Skipper connection**

Run: `docker compose up -d dataflow-server`
Wait 60 seconds, verify SCDF can see Skipper:
Run: `curl -sf http://localhost:9393/about | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['runtimeEnvironment']['taskLaunchers'][0]['deployerName'])"`
Expected: Output should show a deployer name (LocalTaskLauncher or similar)

- [ ] **Step 7: Commit**

```bash
git add docker-compose.yml scripts/test-everything.sh
git commit -m "feat(scdf): add Skipper server, Docker socket mount, wire SCDF->Skipper"
```

---

## Task 6: Docker Image Build + SCDF App Registration

**Files:**
- Create: `docker/scdf/deployer-env.properties`
- Create: `docker/scdf/init-scdf.sh`

- [ ] **Step 1: Build CineLog Docker image**

Run: `docker build -t cinelog/cinelog-app:latest .`
Expected: Successfully built

- [ ] **Step 2: Create deployer-env.properties**

```properties
# Environment variables injected by SCDF Docker Deployer into task containers.
# These override application.yml defaults for Docker network hostnames.
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/cinelog?useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=cinelog
SPRING_DATASOURCE_PASSWORD=cinelog
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
MANAGEMENT_OTLP_TRACING_ENDPOINT=http://otel-collector:4318/v1/traces
MANAGEMENT_OTLP_METRICS_EXPORT_URL=http://otel-collector:4318/v1/metrics
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_PROFILES_ACTIVE=task
```

- [ ] **Step 3: Create init-scdf.sh**

```bash
#!/usr/bin/env bash
# ============================================================================
# init-scdf.sh — Registra app, tasks e schedules no SCDF (idempotente)
# ============================================================================
set -euo pipefail

SCDF_URL="${SCDF_URL:-http://localhost:9393}"
CINELOG_IMAGE="${CINELOG_IMAGE:-docker:cinelog/cinelog-app:latest}"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log_ok()   { echo -e "  ${GREEN}✓${NC} $*"; }
log_skip() { echo -e "  ${YELLOW}○${NC} $*"; }

echo "=== SCDF Init: Registrando app, tasks e schedules ==="

# Aguarda SCDF estar pronto
echo -n "Aguardando SCDF..."
until curl -sf "$SCDF_URL/about" > /dev/null 2>&1; do
    echo -n "."; sleep 5
done
echo " OK"

# 1. Registra app
echo ""
echo "--- App Registration ---"
RESP=$(curl -sf -o /dev/null -w "%{http_code}" -X POST "$SCDF_URL/apps/task/cinelog" \
    -d "uri=$CINELOG_IMAGE" -d "force=false" 2>/dev/null || echo "409")
if [[ "$RESP" == "201" ]]; then
    log_ok "App 'cinelog' registrada ($CINELOG_IMAGE)"
else
    log_skip "App 'cinelog' ja registrada"
fi

# 2. Carrega deployer env vars
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOYER_ENV=""
if [[ -f "$SCRIPT_DIR/deployer-env.properties" ]]; then
    DEPLOYER_ENV=$(grep -v '^\s*#' "$SCRIPT_DIR/deployer-env.properties" \
        | grep -v '^\s*$' \
        | tr '\n' ',')
    DEPLOYER_ENV="${DEPLOYER_ENV%,}"
fi

# 3. Registra tasks
echo ""
echo "--- Task Definitions ---"

register_task() {
    local name=$1 job=$2
    EXISTS=$(curl -sf -o /dev/null -w "%{http_code}" "$SCDF_URL/tasks/definitions/$name" 2>/dev/null || echo "404")
    if [[ "$EXISTS" == "200" ]]; then
        log_skip "Task '$name' ja existe"
        return 0
    fi
    curl -sf -X POST "$SCDF_URL/tasks/definitions" \
        -d "name=$name" \
        -d "definition=cinelog --spring.batch.job.name=$job" \
        -d "deployer.cinelog.environmentVariables=${DEPLOYER_ENV}" \
        > /dev/null 2>&1
    log_ok "Task '$name' registrada (job: $job)"
}

register_task syncGenresJob           syncGenresJob
register_task importMoviesJob         importMoviesJob
register_task importTvShowsJob        importTvShowsJob
register_task importCreditsJob        importCreditsJob
register_task importSeasonsJob        importSeasonsJob
register_task syncReviewsJob          syncReviewsJob
register_task enrichMediaImagesJob    enrichMediaImagesJob
register_task enrichPersonProfilesJob enrichPersonProfilesJob
register_task linkTmdbJob             linkTmdbJob

# 4. Registra schedules (8 jobs — linkTmdbJob nao tem schedule)
echo ""
echo "--- Schedules ---"

register_schedule() {
    local name=$1 task=$2 cron_expr=$3
    EXISTS=$(curl -sf -o /dev/null -w "%{http_code}" "$SCDF_URL/tasks/schedules/$name" 2>/dev/null || echo "404")
    if [[ "$EXISTS" == "200" ]]; then
        log_skip "Schedule '$name' ja existe"
        return 0
    fi
    curl -sf -X POST "$SCDF_URL/tasks/schedules" \
        -d "scheduleName=$name" \
        -d "taskDefinitionName=$task" \
        -d "properties=scheduler.cron.expression=$cron_expr" \
        > /dev/null 2>&1 && log_ok "Schedule '$name' ($cron_expr)" || log_skip "Schedule '$name' falhou (scheduler pode nao estar configurado)"
}

register_schedule sched-sync-genres           syncGenresJob           "0 0 3 * * 0"
register_schedule sched-import-movies         importMoviesJob         "0 30 3 * * 0"
register_schedule sched-import-tvshows        importTvShowsJob        "0 0 4 * * 0"
register_schedule sched-import-credits        importCreditsJob        "0 30 4 * * 0"
register_schedule sched-import-seasons        importSeasonsJob        "0 0 5 * * 0"
register_schedule sched-sync-reviews          syncReviewsJob          "0 30 5 * * 0"
register_schedule sched-enrich-images         enrichMediaImagesJob    "0 0 6 * * 0"
register_schedule sched-enrich-profiles       enrichPersonProfilesJob "0 30 6 * * 0"

echo ""
echo "=== SCDF Init: Concluido ==="
echo "Dashboard: http://localhost:9393/dashboard"
```

- [ ] **Step 4: Make init-scdf.sh executable**

Run: `chmod +x docker/scdf/init-scdf.sh`

- [ ] **Step 5: Run init-scdf.sh and verify**

Run: `bash docker/scdf/init-scdf.sh`
Expected: 9 tasks registered, 8 schedules registered (or skipped if already exist)

Verify in SCDF:
Run: `curl -sf http://localhost:9393/tasks/definitions | python3 -c "import sys,json; d=json.load(sys.stdin); print(f\"{d['page']['totalElements']} tasks registered\")"`
Expected: `9 tasks registered`

- [ ] **Step 6: Commit**

```bash
git add docker/scdf/
git commit -m "feat(scdf): add init script with task definitions and schedule registration"
```

---

## Task 7: Verify BatchJobMetricsListener Registration (Already Fixed)

**Files:**
- Verify: `src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java`

The spec documented a bug where `BatchJobMetricsListener` was only registered on `JobBuilder`, not `StepBuilder`. This has already been fixed — all 14 StepBuilder instances already have `.listener(metricsListener)`.

- [ ] **Step 1: Verify all StepBuilders have the listener**

Run: `grep -c "listener(metricsListener)" src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java`

Expected: 18 or more (14 StepBuilders + JobBuilders). If any StepBuilder is missing it, add `.listener(metricsListener)` before `.build()`.

- [ ] **Step 2: No commit needed** (already in codebase)

---

## Task 8: Remove BatchSchedulerConfig + Clean Cron Config

**Files:**
- Delete: `src/main/java/com/cine/cinelog/features/batch/scheduler/BatchSchedulerConfig.java`
- Modify: `src/main/java/com/cine/cinelog/features/batch/web/BatchJobController.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Delete BatchSchedulerConfig.java**

Delete the file entirely. SCDF Scheduler replaces all `@Scheduled` cron jobs.

Run: `git rm src/main/java/com/cine/cinelog/features/batch/scheduler/BatchSchedulerConfig.java`

- [ ] **Step 2: Remove cron blocks from application.yml**

In `src/main/resources/application.yml`, remove the `cron:` properties from all `cinelog.batch.jobs.*` entries.
Keep the `enabled` and `max-pages` properties (still used by `BatchJobProperties` for runtime config),
but remove every `cron:` line since SCDF now manages scheduling.

Before:

```yaml
    batch:
        jobs:
            sync-genres:
                enabled: true
                cron: "0 0 3 * * SUN"      # REMOVE this line
            import-movies:
                enabled: true
                cron: "0 30 3 * * SUN"      # REMOVE this line
                max-pages: 10               # KEEP
```

After:

```yaml
    batch:
        jobs:
            sync-genres:
                enabled: true
            import-movies:
                enabled: true
                max-pages: 10
```

- [ ] **Step 3: Add @Deprecated to BatchJobController**

Add `@Deprecated` annotation and JavaDoc to `BatchJobController.java`:

At the class level, add:

```java
/**
 * @deprecated Usar SCDF Dashboard (http://localhost:9393/dashboard) ou SCDF REST API
 * para disparo e agendamento de jobs. Estes endpoints serao removidos em versao futura.
 */
@Deprecated(since = "1.0", forRemoval = true)
```

The endpoints continue working for backward compatibility, but are marked as deprecated.

- [ ] **Step 4: Verify compilation**

Run: `./mvnw compile -q`
Expected: BUILD SUCCESS (may show deprecation warnings, which is expected)

- [ ] **Step 5: Run all tests**

Run: `./mvnw test -q`
Expected: All tests pass. If any test depends on `BatchSchedulerConfig`, it needs to be updated.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/cine/cinelog/features/batch/web/BatchJobController.java \
    src/main/resources/application.yml
git rm src/main/java/com/cine/cinelog/features/batch/scheduler/BatchSchedulerConfig.java
git commit -m "feat(scdf): remove BatchSchedulerConfig, deprecate BatchJobController

SCDF Scheduler now manages all cron scheduling.
BatchJobController endpoints kept for backward compat but marked @Deprecated."
```

---

## Task 9: End-to-End Validation

**Files:** None (validation only)

- [ ] **Step 1: Build Docker image**

Run: `docker build -t cinelog/cinelog-app:latest .`
Expected: Successfully built

- [ ] **Step 2: Run all services**

Run: `docker compose up -d`
Wait for all services to be healthy.

- [ ] **Step 3: Register tasks in SCDF**

Run: `bash docker/scdf/init-scdf.sh`
Expected: 9 tasks, 8 schedules

- [ ] **Step 4: Launch a task manually via SCDF**

Test with `syncGenresJob` (fastest, no TMDB API key needed if genres already exist):

Run:
```bash
curl -sf -X POST "http://localhost:9393/tasks/executions" \
    -d "name=syncGenresJob" \
    -d "properties=deployer.cinelog.environmentVariables=SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/cinelog?useSSL=false&allowPublicKeyRetrieval=true,SPRING_DATASOURCE_USERNAME=cinelog,SPRING_DATASOURCE_PASSWORD=cinelog,SPRING_DATA_REDIS_HOST=redis,SPRING_PROFILES_ACTIVE=task"
```

Check execution status:
```bash
curl -sf "http://localhost:9393/tasks/executions?size=1&sort=TASK_EXECUTION_ID,desc" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); e=d['_embedded']['taskExecutionResourceList'][0]; print(f\"Task: {e['taskName']} Status: {e['exitMessage'] or 'RUNNING'}\")"
```

Expected: Task completes with exit code 0

- [ ] **Step 5: Verify task execution in MySQL**

Run:
```bash
docker exec cinelog-mysql mysql -ucinelog -pcinelog cinelog -e \
    "SELECT TASK_EXECUTION_ID, TASK_NAME, EXIT_CODE, START_TIME, END_TIME FROM SCDF_TASK_EXECUTION ORDER BY TASK_EXECUTION_ID DESC LIMIT 5;" 2>/dev/null
```

Expected: Row(s) showing the task execution with EXIT_CODE=0

- [ ] **Step 6: Verify SCDF Dashboard**

Open: http://localhost:9393/dashboard
Check:
- Tasks > Definitions: 9 tasks listed
- Tasks > Executions: Shows the manual execution from Step 4
- Tasks > Schedules: 8 schedules listed

- [ ] **Step 7: Run test-everything.sh**

Run: `./scripts/test-everything.sh --no-start`
Expected: PASS on all containers, FAIL: 0

- [ ] **Step 8: Run unit tests**

Run: `./mvnw test -q`
Expected: All tests pass

---

## Task 10: Update SCDF Guide

**Files:**
- Modify: `docs/SCDF-GUIDE.md`

- [ ] **Step 1: Update the guide**

Update `docs/SCDF-GUIDE.md` to reflect the new SCDF-managed workflow:
- Add section about launching tasks via SCDF Dashboard
- Add section about viewing task execution history
- Update the "Disparando Jobs Manualmente" section to show SCDF REST API
- Add `init-scdf.sh` usage instructions
- Document Docker image build requirement
- Update architecture diagram to show ephemeral containers

- [ ] **Step 2: Commit**

```bash
git add docs/SCDF-GUIDE.md
git commit -m "docs(scdf): update guide with SCDF task orchestration workflow"
```
