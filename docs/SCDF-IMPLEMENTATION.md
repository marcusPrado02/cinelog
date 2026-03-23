# SCDF — Implementacao Tecnica Detalhada

**Versao:** 1.0
**Data:** 2026-03-22
**Escopo:** Arquitetura, decisoes tecnicas e fluxo completo da integracao SCDF no CineLog

---

## Sumario

1. [Visao Geral da Arquitetura](#1-visao-geral-da-arquitetura)
2. [Componentes e Responsabilidades](#2-componentes-e-responsabilidades)
3. [Os 12 Batch Jobs](#3-os-12-batch-jobs)
4. [Ciclo de Vida do Container Efemero](#4-ciclo-de-vida-do-container-efemero)
5. [Entrypoint: Correcoes do SCDF 2.11.x](#5-entrypoint-correcoes-do-scdf-211x)
6. [Docker Wrapper: Rede e Limpeza de Containers](#6-docker-wrapper-rede-e-limpeza-de-containers)
7. [Profile `task` e application-task.yml](#7-profile-task-e-application-taskyml)
8. [TaskConfig: Spring Cloud Task Manual](#8-taskconfig-spring-cloud-task-manual)
9. [BatchJobsConfig: Definicao dos Jobs](#9-batchjobsconfig-definicao-dos-jobs)
10. [Email Batch Jobs: Implementacao](#10-email-batch-jobs-implementacao)
11. [Unicidade de Execucoes (run.id)](#11-unicidade-de-execucoes-runid)
12. [Tabelas do Banco de Dados](#12-tabelas-do-banco-de-dados)
13. [Metricas e Observabilidade](#13-metricas-e-observabilidade)
14. [Registro de Tasks e Schedules](#14-registro-de-tasks-e-schedules)
15. [Agendamento: Container Cron (scdf-scheduler)](#15-agendamento-container-cron-scdf-scheduler)
16. [Decisoes Tecnicas e Justificativas](#16-decisoes-tecnicas-e-justificativas)
17. [Mapa de Arquivos](#17-mapa-de-arquivos)

---

## 1. Visao Geral da Arquitetura

O CineLog utiliza o **Spring Cloud Data Flow (SCDF) 2.11.5** como orquestrador central de
batch jobs. A abordagem substitui completamente o modelo anterior baseado em `@Scheduled`,
trazendo isolamento via containers efemeros, historico persistido e uma interface visual
para operacoes.

### Diagrama de Alto Nivel

```
 ┌──────────────────────────────────────────────────────────────┐
 │                    SCDF Dashboard (:9393)                    │
 │              Interface web para operadores                   │
 └──────────────────────┬───────────────────────────────────────┘
                        │ REST API
                        v
 ┌──────────────────────────────────────────────────────────────┐
 │                   SCDF Server (:9393)                        │
 │  - Gerencia task definitions e schedules                    │
 │  - Registra execucoes no MySQL (schema cinelog)             │
 │  - Delega deploy ao Skipper                                 │
 └──────────────────────┬───────────────────────────────────────┘
                        │ /api/deployers
                        v
 ┌──────────────────────────────────────────────────────────────┐
 │                  Skipper Server (:7577)                      │
 │  - Recebe instrucao de deploy via REST                      │
 │  - Usa Docker Deployer (docker.sock)                        │
 │  - Chama docker-wrapper.sh → docker-real                    │
 │  - Injeta --rm para auto-limpeza do container               │
 └──────────────────────┬───────────────────────────────────────┘
                        │ docker run --rm --network cinelog_default
                        v
 ┌──────────────────────────────────────────────────────────────┐
 │            Container Efemero (cinelog-app:latest)            │
 │                                                              │
 │  entrypoint.sh                                              │
 │    ├── Corrige SPRING_APPLICATION_JSON (driver, prefixos)   │
 │    ├── Injeta profile=task, redis, mail config              │
 │    ├── Adiciona --run.id=<timestamp> (unicidade)            │
 │    └── exec java -jar app.jar <args>                        │
 │                                                              │
 │  Spring Boot (profile: task)                                │
 │    ├── Conecta ao MySQL (db:3306/cinelog)                   │
 │    ├── Conecta ao Redis (redis:6379)                        │
 │    ├── Executa o batch job (Spring Batch 5)                 │
 │    ├── Registra resultado em BOOT3_TASK_*/BOOT3_BATCH_*     │
 │    ├── Push de metricas via OTLP                            │
 │    └── close-context → shutdown → exit 0|1                  │
 └──────┬──────────┬──────────┬──────────┬─────────────────────┘
        │          │          │          │
        v          v          v          v
   ┌────────┐ ┌────────┐ ┌────────┐ ┌──────────┐
   │ MySQL  │ │ Redis  │ │MailHog │ │   OTLP   │
   │ :3306  │ │ :6379  │ │ :1025  │ │  :4318   │
   └────────┘ └────────┘ └────────┘ └──────────┘
```

### Principio: Container Efemero

Cada batch job roda em um container Docker **isolado e descartavel**:

- **Nao afeta a API principal** — Se um job consumir muita memoria ou travar, a API
  continua servindo requests normalmente.
- **Sem estado persistente** — O container nao armazena nada localmente; tudo vai
  para MySQL, Redis ou MailHog.
- **Auto-destruicao** — O container e removido automaticamente apos o termino
  (via flag `--rm` injetada pelo `docker-wrapper.sh`).

---

## 2. Componentes e Responsabilidades

| Componente | Imagem Docker | Porta | Schema MySQL | Responsabilidade |
|---|---|---|---|---|
| **SCDF Server** | `springcloud/spring-cloud-dataflow-server:2.11.5` | 9393 | `cinelog` | Orquestracao, Dashboard, REST API, historico |
| **Skipper Server** | `springcloud/spring-cloud-skipper-server:2.11.5` | 7577 | `scdf` | Deploy de containers via Docker Deployer |
| **SCDF Scheduler** | `alpine:3.20` | — | — | Agendamento cron dos batch jobs via REST API |
| **Container Efemero** | `cinelog/cinelog-app:latest` | dinamica | `cinelog` | Executa um unico batch job e encerra |
| **MySQL** | `mysql:8.0` | 3306 | `cinelog` + `scdf` | Persistencia de dados e metadados batch |
| **Redis** | `redis:7-alpine` | 6379 | — | Cache de respostas TMDB |
| **MailHog** | `mailhog/mailhog:latest` | 1025/8025 | — | SMTP fake para emails em dev |
| **OTel Collector** | `otel/opentelemetry-collector-contrib` | 4318 | — | Recebe metricas push de containers efemeros |

### Separacao de Schemas MySQL

```
┌─────────────────────────────────────────────────┐
│                   MySQL :3306                    │
│                                                  │
│  Schema: cinelog                                 │
│  ├── Tabelas da app (media, users, etc.)        │
│  ├── BOOT3_BATCH_JOB_INSTANCE                   │
│  ├── BOOT3_BATCH_JOB_EXECUTION                  │
│  ├── BOOT3_BATCH_JOB_EXECUTION_PARAMS            │
│  ├── BOOT3_BATCH_STEP_EXECUTION                  │
│  ├── BOOT3_BATCH_JOB_EXECUTION_CONTEXT           │
│  ├── BOOT3_BATCH_STEP_EXECUTION_CONTEXT           │
│  ├── BOOT3_TASK_EXECUTION                        │
│  ├── BOOT3_TASK_EXECUTION_PARAMS                 │
│  └── BOOT3_TASK_TASK_BATCH                       │
│                                                  │
│  Schema: scdf                                    │
│  ├── Flyway migrations do SCDF                   │
│  ├── app_registration (apps registradas)         │
│  ├── task_definitions                            │
│  ├── audit_records                               │
│  └── skipper_* (metadados do Skipper)            │
└─────────────────────────────────────────────────┘
```

**Decisao:** O SCDF Server usa o schema `cinelog` (junto com a app) para tabelas
Spring Batch e Task, enquanto o Skipper usa o schema `scdf` separado. Isso evita
conflito na tabela `app_registration` que ambos tentariam criar.

---

## 3. Os 12 Batch Jobs

### Jobs de Importacao TMDB (9 jobs)

| Job | Tipo | Descricao | Schedule |
|---|---|---|---|
| `syncGenresJob` | Tasklet | Sincroniza generos de filmes/series | Dom 03:00 |
| `importMoviesJob` | Chunk (20) | Importa filmes populares | Dom 03:30 |
| `importTvShowsJob` | Chunk (20) | Importa series populares | Dom 04:00 |
| `linkTmdbJob` | Tasklet | Vincula midias seed ao TMDB | Somente manual |
| `importCreditsJob` | Chunk (20) | Importa elenco/equipe | Dom 04:30 |
| `importSeasonsJob` | Chunk (20) | Importa temporadas/episodios | Dom 05:00 |
| `syncReviewsJob` | Chunk (20) | Sincroniza reviews do TMDB | Dom 05:30 |
| `enrichMediaImagesJob` | Chunk (20) | Enriquece midias sem imagem | Dom 06:00 |
| `enrichPersonProfilesJob` | Chunk (20) | Enriquece perfis de pessoas | Dom 06:30 |

### Jobs de Email/Relatorios (3 jobs)

| Job | Tipo | Descricao | Schedule |
|---|---|---|---|
| `sendWeeklyDigestJob` | Tasklet | Envia digest semanal para todos os usuarios | Manual (via SCDF) |
| `sendTrendingReportJob` | Tasklet | Envia relatorio de trending para todos | Manual (via SCDF) |
| `sendPlatformReportJob` | Tasklet | Envia relatorio da plataforma para admin | Manual (via SCDF) |

Os jobs de email usam variantes **blocking** do `ReportEmailService` — metodos
sincronos que garantem que todos os emails sao enviados antes do container encerrar.
Isso contrasta com os metodos `@Async` usados pela API principal, onde os emails
sao disparados em background.

---

## 4. Ciclo de Vida do Container Efemero

```
 Operador clica "Launch" no Dashboard
            │
            v
 ┌──────────────────────┐
 │ 1. SCDF Server       │  Registra BOOT3_TASK_EXECUTION no MySQL
 │    recebe request     │  Status: RUNNING
 └──────────┬───────────┘
            v
 ┌──────────────────────┐
 │ 2. Skipper recebe    │  Constroi comando docker run com:
 │    instrucao deploy  │  - SPRING_APPLICATION_JSON (datasource, driver, prefixos)
 └──────────┬───────────┘  - --spring.batch.job.name=<job>
            v              - --spring.cloud.task.executionid=<id>
 ┌──────────────────────┐
 │ 3. docker-wrapper.sh │  Intercepta o comando e:
 │    (no Skipper)      │  - Substitui --network bridge → cinelog_default
 └──────────┬───────────┘  - Adiciona --rm (auto-remocao)
            v
 ┌──────────────────────┐
 │ 4. entrypoint.sh     │  Corrige SPRING_APPLICATION_JSON:
 │    (no container)    │  - Driver: MariaDB → MySQL
 └──────────┬───────────┘  - Prefixos: TASK_/BATCH_ → BOOT3_TASK_/BOOT3_BATCH_
            │              - Adiciona profile=task, redis, mail
            │              - Adiciona --run.id=<timestamp> (unicidade)
            v
 ┌──────────────────────┐
 │ 5. Spring Boot sobe  │  Profile: task
 │    (application-task) │  Kafka listeners: desabilitados
 └──────────┬───────────┘  Batch job enabled: true
            v
 ┌──────────────────────┐
 │ 6. Job executa       │  Conecta ao MySQL, Redis, TMDB/MailHog
 │    (Spring Batch)    │  Grava resultados em BOOT3_BATCH_*
 └──────────┬───────────┘
            v
 ┌──────────────────────┐
 │ 7. close-context     │  Spring Cloud Task encerra ApplicationContext
 │    (TaskLifecycle)   │  Atualiza BOOT3_TASK_EXECUTION com exit code
 └──────────┬───────────┘
            v
 ┌──────────────────────┐
 │ 8. Container exit    │  Exit code 0 (sucesso) ou 1 (falha)
 │    --rm auto-remove  │  Docker daemon remove o container
 └──────────────────────┘
```

---

## 5. Entrypoint: Correcoes do SCDF 2.11.x

O SCDF 2.11.x tem incompatibilidades com Spring Boot 3 que precisam ser corrigidas
em runtime pelo `entrypoint.sh`:

### Arquivo: `docker/scdf/entrypoint.sh`

#### Correcao 1: Driver JDBC

O SCDF injeta `org.mariadb.jdbc.Driver` como driver JDBC (porque e o que ele usa
internamente), mas o container da app precisa de `com.mysql.cj.jdbc.Driver`:

```bash
SPRING_APPLICATION_JSON=$(echo "$SPRING_APPLICATION_JSON" | \
    sed 's/"spring.datasource.driverClassName":"org.mariadb.jdbc.Driver"/"spring.datasource.driverClassName":"com.mysql.cj.jdbc.Driver"/g')
```

#### Correcao 2: Prefixos Boot 3

O SCDF 2.11.x envia prefixos `TASK_` e `BATCH_` (formato Boot 2), mas Spring Boot 3
usa `BOOT3_TASK_` e `BOOT3_BATCH_`:

```bash
SPRING_APPLICATION_JSON=$(echo "$SPRING_APPLICATION_JSON" | \
    sed 's/"spring.cloud.task.tablePrefix":"TASK_"/"spring.cloud.task.tablePrefix":"BOOT3_TASK_"/g' | \
    sed 's/"spring.batch.jdbc.table-prefix":"BATCH_"/"spring.batch.jdbc.table-prefix":"BOOT3_BATCH_"/g')
```

#### Correcao 3: Task Initialize

O SCDF envia `spring.cloud.task.initialize-enabled=false`, mas precisamos `true`
para que as tabelas `BOOT3_TASK_*` sejam criadas:

```bash
SPRING_APPLICATION_JSON=$(echo "$SPRING_APPLICATION_JSON" | \
    sed 's/"spring.cloud.task.initialize-enabled":"false"/"spring.cloud.task.initialize-enabled":"true"/g')
```

#### Correcao 4: Profile, Redis, Mail

O entrypoint injeta configuracoes de rede Docker (hostnames internos):

```bash
# Dev (MailHog):
EXTRA='"spring.profiles.active":"task","spring.data.redis.host":"redis","spring.mail.host":"mailhog","spring.mail.port":"1025"'

# Prod (Gmail/SMTP) — se .env.mail existe com MAIL_HOST != mailhog:
EXTRA='"spring.profiles.active":"task","spring.data.redis.host":"redis"'
EXTRA="${EXTRA},\"spring.mail.host\":\"${MAIL_HOST}\""
EXTRA="${EXTRA},\"spring.mail.port\":\"${MAIL_PORT:-587}\""
# ... (username, password, starttls)
```

#### Correcao 5: Unicidade de run.id

Cada lancamento recebe um `run.id` unico baseado em timestamp (milissegundos):

```bash
FIXED_ARGS+=("--run.id=$(date +%s%3N)")
```

Isso garante que o Spring Batch crie um novo `JobInstance` a cada lancamento,
evitando erros de `JobExecutionAlreadyRunningException` ou `NOOP`.

#### Correcao 6: Argumentos SCDF

O SCDF prefixa argumentos com `--app.cinelog.`. O entrypoint remove esse prefixo
e descarta argumentos de deployer que ja foram tratados:

```bash
--app.cinelog.spring.cloud.*  →  Descartado (ja corrigido no JSON)
--app.cinelog.cinelog.tmdb.*  →  Convertido para --cinelog.tmdb.*
*schemaTarget=boot2*          →  Descartado
```

---

## 6. Docker Wrapper: Rede e Limpeza de Containers

### Problema

O SCDF Local Deployer 2.11.x tem duas limitacoes:

1. **Rede hardcoded:** Em certas versoes, hardcoda `--network bridge`, impedindo
   que o container acesse servicos na rede `cinelog_default`.
2. **Limpeza assincrona:** O setting `DELETE_CONTAINER_ON_EXIT=true` depende de
   cleanup assincrono pelo Skipper, que pode falhar e deixar containers orfanaos.

### Solucao: `docker/scdf/docker-wrapper.sh`

Um script wrapper montado como `/usr/local/bin/docker` nos containers SCDF e Skipper.
O binario real do Docker e montado como `/usr/local/bin/docker-real`:

```yaml
# docker-compose.yml
volumes:
  - ./docker/scdf/docker-cli:/usr/local/bin/docker-real:ro      # Binario real
  - ./docker/scdf/docker-wrapper.sh:/usr/local/bin/docker:ro    # Wrapper
```

O wrapper faz duas coisas:

1. **Substitui `--network bridge`** por `--network cinelog_default`
2. **Injeta `--rm`** em comandos `docker run` (se nao presente)

```bash
# Trecho simplificado do wrapper
case "$arg" in
    run)
        ARGS+=("$arg")
        if $IS_RUN && ! $HAS_RM; then
            ARGS+=("--rm")  # Garante auto-remocao
        fi
        ;;
esac
```

**Por que `--rm` e mais confiavel:** O Docker daemon remove o container atomicamente
quando o processo principal encerra. Nao depende de nenhum processo externo (SCDF/Skipper)
para fazer cleanup — a remocao acontece mesmo se o Skipper estiver indisponivel.

---

## 7. Profile `task` e application-task.yml

### Arquivo: `src/main/resources/application-task.yml`

```yaml
spring:
  main:
    allow-bean-definition-overriding: false

  autoconfigure:
    exclude:
      - org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration

  kafka:
    listener:
      auto-startup: false    # Impede threads non-daemon que bloqueiam shutdown

  batch:
    job:
      enabled: true          # JobLauncherApplicationRunner executa o job no startup
    jdbc:
      table-prefix: BOOT3_BATCH_

  cloud:
    task:
      close-context-enabled: true    # Encerra o Spring Context apos job terminar
      table-prefix: BOOT3_TASK_
      initialize-enabled: true       # Cria tabelas BOOT3_TASK_* se necessario

  liquibase:
    default-schema: cinelog          # Garante que migrations rodam no schema correto

management:
  otlp:
    metrics:
      export:
        enabled: true
        url: ${MANAGEMENT_OTLP_METRICS_EXPORT_URL:http://otel-collector:4318/v1/metrics}
        step: 5s                     # Push de metricas (nao pull via Prometheus)
```

### Decisoes de Configuracao

| Configuracao | Valor | Motivo |
|---|---|---|
| `SimpleTaskAutoConfiguration` excluido | — | Evita referencia circular com `entityManagerFactory` |
| `kafka.listener.auto-startup: false` | — | Threads non-daemon do Kafka impedem shutdown do container |
| `batch.job.enabled: true` | — | Faz o `JobLauncherApplicationRunner` executar o job automaticamente |
| `close-context-enabled: true` | — | Container encerra apos job terminar (sem ficar pendurado) |
| OTLP push (nao pull) | — | Container efemero morre antes do Prometheus conseguir fazer scrape |

---

## 8. TaskConfig: Spring Cloud Task Manual

### Arquivo: `src/main/java/.../batch/config/TaskConfig.java`

O `SimpleTaskAutoConfiguration` e excluido para evitar referencia circular.
Em seu lugar, `TaskConfig` registra manualmente todos os beans necessarios:

```java
@Configuration
@Profile("task")
@Import(TaskLifecycleConfiguration.class)
public class TaskConfig implements TaskConfigurer {

    // 1. TaskRepository, TaskExplorer, TaskNameResolver
    //    → Usam prefix BOOT3_TASK_

    // 2. springCloudTaskTransactionManager
    //    → DataSourceTransactionManager (para Spring Cloud Task)

    // 3. transactionManager (@Primary)
    //    → JpaTransactionManager (para Spring Batch + JPA repositories)

    // 4. TaskRepositoryInitializer
    //    → Cria tabelas BOOT3_TASK_* no primeiro startup

    // 5. TaskObservationCloudKeyValues
    //    → Dependencia do TaskLifecycleConfiguration
}
```

### Dois TransactionManagers

```
┌───────────────────────────────────────────────────────┐
│           springCloudTaskTransactionManager            │
│           (DataSourceTransactionManager)               │
│           → Usado por Spring Cloud Task internamente   │
├───────────────────────────────────────────────────────┤
│           transactionManager (@Primary)                │
│           (JpaTransactionManager)                      │
│           → Usado por BatchJobsConfig, JPA repos       │
└───────────────────────────────────────────────────────┘
```

A separacao e necessaria porque Spring Cloud Task precisa de um
`DataSourceTransactionManager` puro (sem JPA), enquanto o Spring Batch e os
repositories JPA precisam de `JpaTransactionManager`.

---

## 9. BatchJobsConfig: Definicao dos Jobs

### Arquivo: `src/main/java/.../batch/config/BatchJobsConfig.java`

Todos os 12 jobs sao definidos neste arquivo usando Spring Batch 5 `JobBuilder`/`StepBuilder`.

### Jobs Chunk-Oriented (TMDB)

```java
@Bean
public Job importMoviesJob(JobRepository jobRepository, Step importMoviesStep) {
    return new JobBuilder("importMoviesJob", jobRepository)
            .listener(metricsListener)
            .start(importMoviesStep)
            .build();
}

@Bean
public Step importMoviesStep(...) {
    return applyFaultTolerance(
            new StepBuilder("importMoviesStep", jobRepository)
                .<TmdbMediaSummary, MediaWithGenres>chunk(props.getChunkSize(), txManager)
                .reader(tmdbMediaPageReader)
                .processor(tmdbMediaItemProcessor)
                .writer(mediaItemWriter)
                .listener(metricsListener)
                .faultTolerant(),
            props.getSkipLimit())
            .build();
}
```

**Tolerancia a falhas (compartilhada):**
- Retry: 3 tentativas com backoff exponencial (1s → 2s → 4s, max 10s)
- Skip: Ate 50 itens com erro por job (100 para reviews)
- Excecoes retentadas: `WebClientResponseException`, `RestClientException`,
  `ConnectException`, `SocketTimeoutException`

### Jobs Tasklet (Email)

```java
@Bean
public Job sendWeeklyDigestJob(JobRepository jobRepository, Step sendWeeklyDigestStep) {
    return new JobBuilder("sendWeeklyDigestJob", jobRepository)
            .listener(metricsListener)
            .start(sendWeeklyDigestStep)
            .build();
}

@Bean
public Step sendWeeklyDigestStep(..., WeeklyDigestTasklet tasklet) {
    return new StepBuilder("sendWeeklyDigestStep", jobRepository)
            .tasklet(tasklet, txManager)
            .allowStartIfComplete(true)    // Permite re-execucao
            .listener(metricsListener)
            .build();
}
```

**Nota:** Os email jobs NAO usam `RunIdIncrementer`. A unicidade e garantida pelo
`--run.id=<timestamp>` injetado pelo `entrypoint.sh`. Isso evita conflitos entre
o incrementer e parametros de linha de comando.

---

## 10. Email Batch Jobs: Implementacao

### Arquitetura

```
 ┌───────────────────────┐     ┌──────────────────────┐
 │   WeeklyDigestTasklet │     │  ReportEmailService   │
 │   TrendingReportTask. │────>│                       │
 │   PlatformReportTask. │     │  sendWeeklyDigest     │
 └───────────────────────┘     │  ToAllBlocking()      │
                               │                       │
                               │  sendTrendingTo       │
                               │  AllBlocking()        │
                               │                       │
                               │  sendPlatformReport   │
                               │  Blocking(email)      │
                               └──────────┬────────────┘
                                          │
                                          v
                               ┌──────────────────────┐
                               │    EmailService       │
                               │                       │
                               │  sendHtml(to, subj,   │
                               │    template, vars)    │
                               │                       │
                               │  → JavaMailSender     │
                               │  → Thymeleaf render   │
                               └──────────┬────────────┘
                                          │ SMTP
                                          v
                               ┌──────────────────────┐
                               │  MailHog (dev)        │
                               │  :1025 SMTP           │
                               │  :8025 Web UI         │
                               │                       │
                               │  Gmail/SMTP (prod)    │
                               └──────────────────────┘
```

### Blocking vs Async

O `ReportEmailService` expoe dois conjuntos de metodos:

| Metodo | Tipo | Uso |
|---|---|---|
| `sendWeeklyDigestToAll()` | `@Async` | Chamado pela API (retorna imediatamente) |
| `sendWeeklyDigestToAllBlocking()` | Sincrono | Chamado pelo Tasklet (espera completar) |

Os Tasklets de batch **devem** usar as variantes blocking porque o container
encerra apos `RepeatStatus.FINISHED`. Se usassem `@Async`, o container encerraria
antes dos emails serem enviados.

### Configuracao de Email por Ambiente

| Ambiente | SMTP Host | SMTP Port | Auth | Verificacao |
|---|---|---|---|---|
| Dev (MailHog) | `mailhog` | `1025` | Nao | http://localhost:8025 |
| Prod (Gmail) | `smtp.gmail.com` | `587` | Sim (STARTTLS) | Inbox do destinatario |

O entrypoint.sh detecta o ambiente automaticamente:
- Se `.env.mail` contiver `MAIL_HOST` diferente de `mailhog`, usa Gmail/SMTP.
- Caso contrario, usa MailHog.

---

## 11. Unicidade de Execucoes (run.id)

### Problema Original

O Spring Batch identifica `JobInstance` por `jobName + jobParameters`. Sem parametros
unicos, cada lancamento reutiliza a mesma instancia:

```
Lancamento 1: sendWeeklyDigestJob + params={} → Instance 18 → COMPLETED
Lancamento 2: sendWeeklyDigestJob + params={} → Instance 18 → NOOP (ja completou)
```

### Solucao: Timestamp como run.id

O `entrypoint.sh` injeta `--run.id=$(date +%s%3N)` em cada lancamento:

```
Lancamento 1: params={run.id=1774232944549} → Instance 21 → COMPLETED
Lancamento 2: params={run.id=1774233001234} → Instance 22 → COMPLETED
```

Cada lancamento cria uma instancia unica, permitindo re-execucoes ilimitadas.

### Por que NAO usar RunIdIncrementer

O `RunIdIncrementer` do Spring Batch conflita com parametros de linha de comando:

1. O entrypoint passa `--run.id=<timestamp>` como argumento
2. `JobLauncherApplicationRunner` converte isso em job parameter `run.id`
3. `RunIdIncrementer.getNext()` SOBRESCREVE esse valor com `run.id=1`
4. Resultado: conflito de parametros e possivel `JobExecutionAlreadyRunningException`

Solucao adotada: remover `RunIdIncrementer` e usar apenas o timestamp do entrypoint.

---

## 12. Tabelas do Banco de Dados

O CineLog utiliza tres conjuntos de tabelas para persistir metadados de batch:
tabelas **Spring Batch** (execucao de jobs), tabelas **Spring Cloud Task** (execucao
de containers) e tabelas **SCDF/Skipper** (orquestracao). Todas usam o prefixo
`BOOT3_` para compatibilidade com Spring Boot 3.

### Visao Geral dos Schemas

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          MySQL :3306                                    │
│                                                                         │
│  ┌─── Schema: cinelog ───────────────────────────────────────────────┐  │
│  │                                                                   │  │
│  │  Tabelas da Aplicacao                                            │  │
│  │  ├── media, users, watch_entries, credits, ...                   │  │
│  │                                                                   │  │
│  │  Tabelas Spring Batch (prefixo BOOT3_BATCH_)                    │  │
│  │  ├── BOOT3_BATCH_JOB_INSTANCE          (instancias de jobs)     │  │
│  │  ├── BOOT3_BATCH_JOB_EXECUTION         (execucoes de jobs)      │  │
│  │  ├── BOOT3_BATCH_JOB_EXECUTION_PARAMS  (parametros por exec)    │  │
│  │  ├── BOOT3_BATCH_JOB_EXECUTION_CONTEXT (contexto serializado)   │  │
│  │  ├── BOOT3_BATCH_STEP_EXECUTION        (execucoes de steps)     │  │
│  │  ├── BOOT3_BATCH_STEP_EXECUTION_CONTEXT(contexto do step)       │  │
│  │  └── BOOT3_BATCH_*_SEQ                 (sequencias de IDs)      │  │
│  │                                                                   │  │
│  │  Tabelas Spring Cloud Task (prefixo BOOT3_TASK_)                │  │
│  │  ├── BOOT3_TASK_EXECUTION              (execucoes de tasks)     │  │
│  │  ├── BOOT3_TASK_EXECUTION_PARAMS       (parametros da task)     │  │
│  │  ├── BOOT3_TASK_TASK_BATCH             (vinculo task↔batch)     │  │
│  │  ├── BOOT3_TASK_EXECUTION_METADATA     (manifesto de deploy)    │  │
│  │  ├── BOOT3_TASK_LOCK                   (lock distribuido)       │  │
│  │  └── BOOT3_TASK_SEQ                    (sequencia de IDs)       │  │
│  │                                                                   │  │
│  │  Tabelas Legacy (prefixo sem BOOT3_ — criadas pelo SCDF 2.11)  │  │
│  │  ├── BATCH_*, TASK_*, AGGREGATE_TASK_*                          │  │
│  │  └── (NAO usadas pela app — vestigos do setup SCDF)             │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─── Schema: scdf ─────────────────────────────────────────────────┐  │
│  │  Tabelas do SCDF Server e Skipper                                │  │
│  │  ├── flyway_schema_history     (migrations do SCDF)              │  │
│  │  ├── skipper_release           (deployments do Skipper)          │  │
│  │  ├── skipper_manifest          (manifestos de deploy)            │  │
│  │  ├── skipper_app_deployer_data (dados do deployer)               │  │
│  │  ├── state, state_machine, transition (maquina de estados)      │  │
│  │  └── hibernate_sequence        (sequencias do Skipper)           │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### Tabelas Spring Batch (Schema: cinelog)

Gerenciadas pelo Spring Batch 5. Registram **o que o job fez** — instancias,
execucoes, steps e contadores de itens.

#### BOOT3_BATCH_JOB_INSTANCE

Representa uma **instancia logica** de um job. Cada combinacao unica de
`JOB_NAME + JOB_KEY` (hash dos parametros) cria uma nova instancia.

| Coluna | Tipo | Descricao |
|---|---|---|
| `JOB_INSTANCE_ID` | BIGINT PK | ID unico da instancia |
| `VERSION` | BIGINT | Versao para controle de concorrencia otimista |
| `JOB_NAME` | VARCHAR(100) | Nome do job (ex: `sendWeeklyDigestJob`) |
| `JOB_KEY` | VARCHAR(32) | Hash MD5 dos parametros identificadores do job |

**Regra de unicidade:** O par `(JOB_NAME, JOB_KEY)` e unico. O `run.id` injetado
pelo `entrypoint.sh` garante que cada lancamento via SCDF gera um `JOB_KEY`
diferente, criando uma nova instancia.

```sql
-- Exemplo de consulta
SELECT JOB_INSTANCE_ID, JOB_NAME, JOB_KEY
FROM BOOT3_BATCH_JOB_INSTANCE
ORDER BY JOB_INSTANCE_ID DESC LIMIT 5;
```

#### BOOT3_BATCH_JOB_EXECUTION

Representa uma **execucao** de um job. Uma instancia pode ter multiplas execucoes
(ex: primeira falhou, segunda completou no restart).

| Coluna | Tipo | Descricao |
|---|---|---|
| `JOB_EXECUTION_ID` | BIGINT PK | ID unico da execucao |
| `VERSION` | BIGINT | Controle de concorrencia (1=started, 2=finished) |
| `JOB_INSTANCE_ID` | BIGINT FK | Referencia a instancia do job |
| `CREATE_TIME` | DATETIME(6) | Quando a execucao foi criada |
| `START_TIME` | DATETIME(6) | Quando o job comecou a executar |
| `END_TIME` | DATETIME(6) | Quando o job terminou (`NULL` = ainda rodando) |
| `STATUS` | VARCHAR(10) | Status: `COMPLETED`, `FAILED`, `STARTED`, `ABANDONED` |
| `EXIT_CODE` | VARCHAR(2500) | Codigo de saida: `COMPLETED`, `FAILED`, `NOOP` |
| `EXIT_MESSAGE` | VARCHAR(2500) | Mensagem de erro/descricao (stack trace em caso de falha) |
| `LAST_UPDATED` | DATETIME(6) | Ultimo update do registro |

**Status possiveis:**

| Status | Significado | VERSION |
|---|---|---|
| `STARTING` | Job esta sendo inicializado | 0 |
| `STARTED` | Job esta em execucao | 1 |
| `COMPLETED` | Job terminou com sucesso | 2 |
| `FAILED` | Job terminou com erro | 2 |
| `STOPPED` | Job foi parado manualmente | 2 |
| `ABANDONED` | Job foi abandonado (nao pode ser restartado) | 2 |

```sql
-- Historico de execucoes com duracao
SELECT
    je.JOB_EXECUTION_ID,
    ji.JOB_NAME,
    je.STATUS,
    je.START_TIME,
    je.END_TIME,
    TIMESTAMPDIFF(SECOND, je.START_TIME, je.END_TIME) AS duracao_seg
FROM BOOT3_BATCH_JOB_EXECUTION je
JOIN BOOT3_BATCH_JOB_INSTANCE ji ON je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
ORDER BY je.JOB_EXECUTION_ID DESC
LIMIT 10;
```

#### BOOT3_BATCH_JOB_EXECUTION_PARAMS

Armazena os **parametros** passados para cada execucao do job.

| Coluna | Tipo | Descricao |
|---|---|---|
| `JOB_EXECUTION_ID` | BIGINT FK | Referencia a execucao |
| `PARAMETER_NAME` | VARCHAR(100) | Nome do parametro (ex: `run.id`) |
| `PARAMETER_TYPE` | VARCHAR(100) | Tipo Java (ex: `java.lang.String`, `java.lang.Long`) |
| `PARAMETER_VALUE` | VARCHAR(2500) | Valor do parametro |
| `IDENTIFYING` | CHAR(1) | `Y` = parametro identifica a instancia, `N` = nao |

**No CineLog, os parametros tipicos sao:**
- `run.id` = timestamp em milissegundos (garante unicidade)
- `spring.cloud.task.executionid` = ID da task execution no SCDF
- `mediaType` = `MOVIE` ou `SERIES` (para importMoviesJob/importTvShowsJob)

```sql
-- Ver parametros de uma execucao especifica
SELECT PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING
FROM BOOT3_BATCH_JOB_EXECUTION_PARAMS
WHERE JOB_EXECUTION_ID = 31;
```

#### BOOT3_BATCH_STEP_EXECUTION

Registra a execucao de cada **step** dentro de um job. Para jobs chunk-oriented,
esta e a tabela mais importante — contem os contadores de itens.

| Coluna | Tipo | Descricao |
|---|---|---|
| `STEP_EXECUTION_ID` | BIGINT PK | ID unico do step |
| `VERSION` | BIGINT | Controle de concorrencia |
| `STEP_NAME` | VARCHAR(100) | Nome do step (ex: `importMoviesStep`) |
| `JOB_EXECUTION_ID` | BIGINT FK | Referencia a execucao do job |
| `CREATE_TIME` | DATETIME(6) | Criacao do step |
| `START_TIME` | DATETIME(6) | Inicio da execucao |
| `END_TIME` | DATETIME(6) | Termino da execucao |
| `STATUS` | VARCHAR(10) | `COMPLETED`, `FAILED`, etc. |
| `COMMIT_COUNT` | BIGINT | Numero de transacoes commitadas |
| `READ_COUNT` | BIGINT | Itens lidos do reader (TMDB API, DB, etc.) |
| `FILTER_COUNT` | BIGINT | Itens filtrados pelo processor (retorno null) |
| `WRITE_COUNT` | BIGINT | Itens escritos pelo writer (salvos no MySQL) |
| `READ_SKIP_COUNT` | BIGINT | Itens pulados na leitura (erro no reader) |
| `WRITE_SKIP_COUNT` | BIGINT | Itens pulados na escrita (erro no writer) |
| `PROCESS_SKIP_COUNT` | BIGINT | Itens pulados no processamento |
| `ROLLBACK_COUNT` | BIGINT | Transacoes revertidas (erro no chunk) |
| `EXIT_CODE` | VARCHAR(2500) | Codigo de saida do step |
| `EXIT_MESSAGE` | VARCHAR(2500) | Mensagem descritiva |

**Como interpretar os contadores:**

```
READ_COUNT = 200     → 200 filmes lidos da TMDB API
FILTER_COUNT = 2     → 2 filmes rejeitados pelo processor (ex: duplicados)
WRITE_COUNT = 195    → 195 filmes salvos no MySQL
SKIP_COUNT = 3       → 3 filmes com erro (rede, dados invalidos)
COMMIT_COUNT = 10    → 10 chunks de 20 itens commitados
ROLLBACK_COUNT = 1   → 1 chunk revertido (reprocessado com skip)
```

```sql
-- Detalhamento por step com contadores
SELECT
    se.STEP_NAME,
    se.STATUS,
    se.READ_COUNT    AS lidos,
    se.WRITE_COUNT   AS escritos,
    se.READ_SKIP_COUNT + se.WRITE_SKIP_COUNT + se.PROCESS_SKIP_COUNT AS pulados,
    se.COMMIT_COUNT  AS commits,
    se.ROLLBACK_COUNT AS rollbacks,
    TIMESTAMPDIFF(SECOND, se.START_TIME, se.END_TIME) AS duracao_seg
FROM BOOT3_BATCH_STEP_EXECUTION se
ORDER BY se.START_TIME DESC
LIMIT 10;
```

#### BOOT3_BATCH_JOB_EXECUTION_CONTEXT / BOOT3_BATCH_STEP_EXECUTION_CONTEXT

Armazenam o **contexto serializado** de cada execucao (job e step). Usado pelo
Spring Batch para manter estado entre restarts.

| Coluna | Tipo | Descricao |
|---|---|---|
| `JOB_EXECUTION_ID` ou `STEP_EXECUTION_ID` | BIGINT PK | Referencia |
| `SHORT_CONTEXT` | VARCHAR(2500) | Versao curta do contexto (JSON) |
| `SERIALIZED_CONTEXT` | TEXT | Contexto completo serializado |

**Exemplo de contexto de step (importMoviesStep):**
```json
{"@class":"java.util.HashMap","batch.taskletType":"...TmdbMediaPageReader",
 "batch.stepType":"org.springframework.batch.core.step.tasklet.TaskletStep",
 "currentPage":5,"totalProcessed":100}
```

#### BOOT3_BATCH_*_SEQ

Tres tabelas de sequencia para geracao de IDs:

| Tabela | Gera IDs para |
|---|---|
| `BOOT3_BATCH_JOB_SEQ` | `JOB_INSTANCE_ID` |
| `BOOT3_BATCH_JOB_EXECUTION_SEQ` | `JOB_EXECUTION_ID` |
| `BOOT3_BATCH_STEP_EXECUTION_SEQ` | `STEP_EXECUTION_ID` |

---

### Tabelas Spring Cloud Task (Schema: cinelog)

Gerenciadas pelo Spring Cloud Task. Registram **o ciclo de vida do container** —
quando iniciou, quando encerrou, com qual exit code.

#### BOOT3_TASK_EXECUTION

Registro principal de cada execucao de container efemero.

| Coluna | Tipo | Descricao |
|---|---|---|
| `TASK_EXECUTION_ID` | BIGINT PK | ID unico da execucao |
| `START_TIME` | DATETIME(6) | Quando o container iniciou |
| `END_TIME` | DATETIME(6) | Quando o container encerrou |
| `TASK_NAME` | VARCHAR(100) | Nome da task (ex: `sendWeeklyDigestJob`) |
| `EXIT_CODE` | INT | Codigo de saida: `0` = sucesso, `1` = erro |
| `EXIT_MESSAGE` | VARCHAR(2500) | Mensagem descritiva |
| `ERROR_MESSAGE` | VARCHAR(2500) | Stack trace completo em caso de erro |
| `LAST_UPDATED` | TIMESTAMP | Ultimo update |
| `EXTERNAL_EXECUTION_ID` | VARCHAR(255) | ID externo do container Docker (UUID) |
| `PARENT_EXECUTION_ID` | BIGINT FK | ID da execucao pai (para composed tasks) |

```sql
-- Historico de containers efemeros
SELECT
    TASK_EXECUTION_ID,
    TASK_NAME,
    EXIT_CODE,
    START_TIME,
    END_TIME,
    TIMESTAMPDIFF(SECOND, START_TIME, END_TIME) AS duracao_seg,
    SUBSTRING(ERROR_MESSAGE, 1, 100) AS erro
FROM BOOT3_TASK_EXECUTION
ORDER BY TASK_EXECUTION_ID DESC
LIMIT 10;
```

#### BOOT3_TASK_EXECUTION_PARAMS

Parametros passados para o container efemero (argumentos de linha de comando).

| Coluna | Tipo | Descricao |
|---|---|---|
| `TASK_EXECUTION_ID` | BIGINT FK | Referencia a execucao |
| `TASK_PARAM` | VARCHAR(2500) | Parametro completo (ex: `--spring.batch.job.name=syncGenresJob`) |

```sql
-- Ver argumentos passados para um container
SELECT TASK_PARAM
FROM BOOT3_TASK_EXECUTION_PARAMS
WHERE TASK_EXECUTION_ID = 49;

-- Resultado tipico:
-- --spring.cloud.task.executionid=49
-- --run.id=1774233001234
```

#### BOOT3_TASK_TASK_BATCH

Tabela de **vinculo** entre execucoes de task (container) e execucoes de job
(Spring Batch). Permite navegar de uma task execution para os jobs que rodaram
dentro daquele container.

| Coluna | Tipo | Descricao |
|---|---|---|
| `TASK_EXECUTION_ID` | BIGINT FK | ID da task execution (container) |
| `JOB_EXECUTION_ID` | BIGINT FK | ID do job execution (Spring Batch) |

**Esta e a tabela que conecta os dois mundos:**

```
SCDF Dashboard                          Spring Batch
┌─────────────────┐                     ┌──────────────────┐
│ TASK_EXECUTION   │   TASK_TASK_BATCH   │ JOB_EXECUTION    │
│ ID=49            │──────────────────>  │ ID=33            │
│ name=sendWeekly  │                     │ name=sendWeekly  │
│ exit=0           │                     │ status=COMPLETED │
└─────────────────┘                     └──────────────────┘
```

```sql
-- Consulta que une os dois mundos
SELECT
    te.TASK_EXECUTION_ID,
    te.TASK_NAME,
    te.EXIT_CODE        AS task_exit,
    tb.JOB_EXECUTION_ID,
    ji.JOB_NAME,
    je.STATUS           AS job_status
FROM BOOT3_TASK_TASK_BATCH tb
JOIN BOOT3_TASK_EXECUTION te ON tb.TASK_EXECUTION_ID = te.TASK_EXECUTION_ID
JOIN BOOT3_BATCH_JOB_EXECUTION je ON tb.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
JOIN BOOT3_BATCH_JOB_INSTANCE ji ON je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
ORDER BY te.TASK_EXECUTION_ID DESC
LIMIT 10;
```

#### BOOT3_TASK_EXECUTION_METADATA

Armazena o **manifesto de deploy** usado pelo SCDF para lancar o container.

| Coluna | Tipo | Descricao |
|---|---|---|
| `ID` | BIGINT PK | ID do registro |
| `TASK_EXECUTION_ID` | BIGINT FK | Referencia a execucao |
| `TASK_EXECUTION_MANIFEST` | TEXT | JSON com imagem Docker, argumentos, properties |

#### BOOT3_TASK_LOCK

Lock distribuido para evitar execucoes concorrentes da mesma task.

| Coluna | Tipo | Descricao |
|---|---|---|
| `LOCK_KEY` | CHAR(36) PK | Chave do lock (UUID) |
| `REGION` | VARCHAR(100) PK | Regiao do lock |
| `CLIENT_ID` | CHAR(36) | ID do cliente que possui o lock |
| `CREATED_DATE` | DATETIME(6) | Quando o lock foi adquirido |

#### BOOT3_TASK_SEQ

Sequencia para geracao de `TASK_EXECUTION_ID`.

---

### Tabelas SCDF/Skipper (Schema: scdf)

Gerenciadas pelo SCDF Server e Skipper. Armazenam definicoes de apps, releases,
e maquina de estados interna.

| Tabela | Descricao |
|---|---|
| `flyway_schema_history` | Historico de migrations do SCDF |
| `skipper_release` | Releases de deploy (historico de lancamentos) |
| `skipper_manifest` | Manifestos de deploy com config completa |
| `skipper_app_deployer_data` | Dados do deployer (container IDs, status) |
| `skipper_info` | Informacoes gerais do Skipper |
| `skipper_status` | Status de cada release |
| `skipper_repository` | Repositorios de apps registrados |
| `skipper_package_metadata` | Metadados de pacotes de apps |
| `state`, `state_machine`, `transition` | Maquina de estados Spring Statemachine |
| `hibernate_sequence` | Sequencia de IDs do Hibernate |

---

### Tabelas Legacy (Schema: cinelog)

O SCDF 2.11.x cria automaticamente tabelas com prefixo `BATCH_` e `TASK_` (formato
Boot 2) e tambem `AGGREGATE_TASK_*`. **Essas tabelas NAO sao usadas pela aplicacao
CineLog**, que usa exclusivamente o prefixo `BOOT3_`. Elas podem ser ignoradas ou
removidas com seguranca:

| Tabela Legacy | Equivalente Boot 3 |
|---|---|
| `BATCH_JOB_INSTANCE` | `BOOT3_BATCH_JOB_INSTANCE` |
| `BATCH_JOB_EXECUTION` | `BOOT3_BATCH_JOB_EXECUTION` |
| `TASK_EXECUTION` | `BOOT3_TASK_EXECUTION` |
| `TASK_TASK_BATCH` | `BOOT3_TASK_TASK_BATCH` |
| `AGGREGATE_TASK_EXECUTION` | `BOOT3_TASK_EXECUTION` |
| `AGGREGATE_TASK_BATCH` | `BOOT3_TASK_TASK_BATCH` |

---

### Diagrama de Relacionamento (ER)

```
BOOT3_BATCH_JOB_INSTANCE
  │ PK: JOB_INSTANCE_ID
  │ JOB_NAME + JOB_KEY (unique)
  │
  ├──< BOOT3_BATCH_JOB_EXECUTION (1:N)
  │      │ PK: JOB_EXECUTION_ID
  │      │ FK: JOB_INSTANCE_ID
  │      │ STATUS, START_TIME, END_TIME
  │      │
  │      ├──< BOOT3_BATCH_JOB_EXECUTION_PARAMS (1:N)
  │      │      PARAMETER_NAME, PARAMETER_VALUE
  │      │
  │      ├──< BOOT3_BATCH_JOB_EXECUTION_CONTEXT (1:1)
  │      │      SHORT_CONTEXT, SERIALIZED_CONTEXT
  │      │
  │      ├──< BOOT3_BATCH_STEP_EXECUTION (1:N)
  │      │      │ PK: STEP_EXECUTION_ID
  │      │      │ STEP_NAME, STATUS
  │      │      │ READ_COUNT, WRITE_COUNT, SKIP_COUNT
  │      │      │
  │      │      └──< BOOT3_BATCH_STEP_EXECUTION_CONTEXT (1:1)
  │      │             SHORT_CONTEXT, SERIALIZED_CONTEXT
  │      │
  │      └──< BOOT3_TASK_TASK_BATCH (N:1) ─── vinculo ──>
  │                                                        │
BOOT3_TASK_EXECUTION                                       │
  │ PK: TASK_EXECUTION_ID                                  │
  │ TASK_NAME, EXIT_CODE, ERROR_MESSAGE          <─────────┘
  │
  ├──< BOOT3_TASK_EXECUTION_PARAMS (1:N)
  │      TASK_PARAM
  │
  └──< BOOT3_TASK_EXECUTION_METADATA (1:1)
         TASK_EXECUTION_MANIFEST
```

---

### Queries Uteis para Operacao

```sql
-- ============================================================================
-- Resumo geral: quantas vezes cada job rodou e taxa de sucesso
-- ============================================================================
SELECT
    ji.JOB_NAME                                                     AS job,
    COUNT(*)                                                        AS total,
    SUM(CASE WHEN je.STATUS = 'COMPLETED' THEN 1 ELSE 0 END)       AS sucesso,
    SUM(CASE WHEN je.STATUS = 'FAILED'    THEN 1 ELSE 0 END)       AS falha,
    ROUND(AVG(TIMESTAMPDIFF(SECOND, je.START_TIME, je.END_TIME)),1) AS duracao_media_seg
FROM BOOT3_BATCH_JOB_EXECUTION je
JOIN BOOT3_BATCH_JOB_INSTANCE ji ON je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
GROUP BY ji.JOB_NAME
ORDER BY ji.JOB_NAME;

-- ============================================================================
-- Steps com falha (para debug rapido)
-- ============================================================================
SELECT
    se.STEP_NAME,
    se.STATUS,
    se.READ_COUNT  AS lidos,
    se.WRITE_COUNT AS escritos,
    se.READ_SKIP_COUNT + se.WRITE_SKIP_COUNT + se.PROCESS_SKIP_COUNT AS pulados,
    SUBSTRING(se.EXIT_MESSAGE, 1, 200) AS erro
FROM BOOT3_BATCH_STEP_EXECUTION se
WHERE se.STATUS = 'FAILED'
ORDER BY se.START_TIME DESC
LIMIT 10;

-- ============================================================================
-- Containers que falharam (com stack trace)
-- ============================================================================
SELECT
    TASK_EXECUTION_ID,
    TASK_NAME,
    EXIT_CODE,
    START_TIME,
    SUBSTRING(ERROR_MESSAGE, 1, 300) AS erro
FROM BOOT3_TASK_EXECUTION
WHERE EXIT_CODE != 0
ORDER BY TASK_EXECUTION_ID DESC
LIMIT 10;

-- ============================================================================
-- Navegacao completa: Task → Job → Steps
-- ============================================================================
SELECT
    te.TASK_EXECUTION_ID  AS task_id,
    te.TASK_NAME,
    je.JOB_EXECUTION_ID  AS job_id,
    je.STATUS             AS job_status,
    se.STEP_NAME,
    se.READ_COUNT         AS lidos,
    se.WRITE_COUNT        AS escritos,
    TIMESTAMPDIFF(SECOND, se.START_TIME, se.END_TIME) AS step_seg
FROM BOOT3_TASK_TASK_BATCH tb
JOIN BOOT3_TASK_EXECUTION te ON tb.TASK_EXECUTION_ID = te.TASK_EXECUTION_ID
JOIN BOOT3_BATCH_JOB_EXECUTION je ON tb.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
LEFT JOIN BOOT3_BATCH_STEP_EXECUTION se ON je.JOB_EXECUTION_ID = se.JOB_EXECUTION_ID
ORDER BY te.TASK_EXECUTION_ID DESC, se.STEP_EXECUTION_ID
LIMIT 20;
```

---

## 13. Metricas e Observabilidade

### Push vs Pull

```
┌─────────────────────────┐     ┌──────────────────────┐
│ App de Longa Duracao    │     │ Container Efemero     │
│                         │     │                       │
│ Prometheus ──scrape──>  │     │    ──push──> OTLP     │
│ /actuator/prometheus    │     │    Collector :4318    │
│                         │     │                       │
│ (container permanente)  │     │ (morre em segundos)   │
└─────────────────────────┘     └──────────────────────┘
```

Containers efemeros usam OTLP push (`OtlpMeterRegistry`) porque morrem antes
do Prometheus conseguir fazer scrape. O `BatchJobMetricsListener` registra metricas
em ambos os cenarios.

### Metricas Registradas

| Metrica | Tipo | Tags |
|---|---|---|
| `batch_job_duration_seconds` | Timer | `job_name`, `status` |
| `batch_job_completed_total` | Counter | `job_name`, `status` |
| `batch_step_items_read_total` | Counter | `job_name`, `step_name` |
| `batch_step_items_written_total` | Counter | `job_name`, `step_name` |
| `batch_step_items_skipped_total` | Counter | `job_name`, `step_name` |
| `batch_step_duration_seconds` | Timer | `job_name`, `step_name`, `status` |

---

## 14. Registro de Tasks e Schedules

### Script: `docker/scdf/init-scdf.sh`

Script idempotente que registra a app, cria task definitions e schedules no SCDF:

```bash
bash docker/scdf/init-scdf.sh
```

**Fluxo:**

1. Aguarda SCDF estar saudavel (ate 120s)
2. Registra a app `cinelog` → `docker:cinelog/cinelog-app:latest`
3. Cria 12 task definitions (9 TMDB + 3 email)
4. Cria 8 schedules CRON (domingos 03:00-06:30)

**Nota:** Os 3 jobs de email e o `linkTmdbJob` NAO tem schedule — sao executados
manualmente via Dashboard ou API REST.

---

## 15. Agendamento: Container Cron (scdf-scheduler)

### Limitacao do SCDF Local Deployer

O SCDF Local Deployer (Docker) **nao implementa scheduling nativo**. O botao
"Create Schedule" no Dashboard retorna:

```
"Scheduling is not implemented for local platform."
```

Isso ocorre porque a interface Java `Scheduler` do SPI (`spring-cloud-deployer-spi`)
so possui implementacoes para Kubernetes (`KubernetesScheduler` → cria CronJobs)
e Cloud Foundry (`CloudFoundryScheduler`). Para o Local Deployer, a Spring nunca
criou uma implementacao — o codigo simplesmente nao existe na imagem oficial.

### Solucao: Container `scdf-scheduler`

Um container Alpine leve (~8MB) com `curl` e `crond` que executa os batch jobs
nos horarios configurados, chamando a REST API do SCDF:

```
┌─────────────────────────────────────────────────────────┐
│              scdf-scheduler (Alpine + crond)             │
│                                                         │
│  /app/schedules.cron        /app/scdf-schedule.sh       │
│  ┌───────────────────┐      ┌───────────────────────┐   │
│  │ 0 8 * * 1 ...     │─────>│ curl -X POST          │   │
│  │ 0 3 * * 0 ...     │      │ SCDF:9393/tasks/      │   │
│  │ ...                │      │ executions/launch     │   │
│  └───────────────────┘      └───────────┬───────────┘   │
│                                         │               │
└─────────────────────────────────────────┼───────────────┘
                                          │ REST API
                                          v
                              ┌───────────────────────┐
                              │ SCDF Server (:9393)   │
                              │ → Skipper → docker run│
                              └───────────────────────┘
```

### Arquivos

| Arquivo | Funcao |
|---|---|
| `docker/scdf/schedules.cron` | Crontab com todos os agendamentos. Edite e reinicie para alterar. |
| `docker/scdf/scdf-schedule.sh` | Script que chama a REST API com auth e `bootVersion=3`. |

### Como funciona no docker-compose

```yaml
scdf-scheduler:
    image: alpine:3.20
    container_name: cinelog-scdf-scheduler
    environment:
        SCDF_URL: http://dataflow-server:9393
        SCDF_USER: admin
        SCDF_PASSWORD: ${SCDF_ADMIN_PASSWORD:-Admin@CineLog2025!}
    volumes:
        - ./docker/scdf/schedules.cron:/app/schedules.cron:ro
        - ./docker/scdf/scdf-schedule.sh:/app/scdf-schedule.sh:ro
```

O entrypoint do container:
1. Instala `curl` (unica dependencia)
2. Exporta as env vars para um arquivo (crond do Alpine nao herda env do pai)
3. Gera o crontab final com `. /app/env.sh;` prefixado em cada linha
4. Inicia `crond` em foreground

### Alterando Agendamentos

```bash
# Editar horarios
vim docker/scdf/schedules.cron

# Aplicar mudancas
docker compose restart scdf-scheduler

# Verificar se carregou
docker logs cinelog-scdf-scheduler
# → SCDF Scheduler started — 12 schedules loaded
```

### Agendamentos Configurados

| Cron | Job | Quando |
|---|---|---|
| `0 3 * * 0` | syncGenresJob | Domingo 03:00 |
| `30 3 * * 0` | importMoviesJob | Domingo 03:30 |
| `0 4 * * 0` | importTvShowsJob | Domingo 04:00 |
| `30 4 * * 0` | importCreditsJob | Domingo 04:30 |
| `0 5 * * 0` | importSeasonsJob | Domingo 05:00 |
| `30 5 * * 0` | syncReviewsJob | Domingo 05:30 |
| `0 6 * * 0` | enrichMediaImagesJob | Domingo 06:00 |
| `30 6 * * 0` | enrichPersonProfilesJob | Domingo 06:30 |
| `0 8 * * 1` | sendWeeklyDigestJob | Segunda 08:00 |
| `0 18 * * 5` | sendTrendingReportJob | Sexta 18:00 |
| `0 6 * * 0` | sendPlatformReportJob | Domingo 06:00 |

### Migracao para Kubernetes

Quando o projeto migrar para Kubernetes, o container `scdf-scheduler` pode ser
removido do docker-compose. O SCDF Kubernetes Deployer implementa `KubernetesScheduler`
nativamente, e o botao "Create Schedule" no Dashboard passara a funcionar sem
nenhuma mudanca no codigo da aplicacao.

---

## 16. Decisoes Tecnicas e Justificativas

| Decisao | Motivo |
|---|---|
| **MariaDB Connector/J no SCDF** | Imagem oficial do SCDF nao inclui MySQL Connector/J. MariaDB Connector e compativel com MySQL 8. |
| **`permitMysqlScheme=true`** | MariaDB Connector aceitar URLs `jdbc:mysql://` para que SCDF detecte MySQL e aplique migrations corretas. |
| **Docker CLI estatico + wrapper** | SCDF/Skipper nao incluem Docker CLI. Binario estatico montado como volume + wrapper para corrigir rede e adicionar `--rm`. |
| **Resilience4j 2.2.0** | Versao 2.3.0 removeu `RxJava3FallbackDecorator`, causando `ClassNotFoundException`. |
| **TaskConfigurer explicito** | Evita `NoUniqueBeanDefinitionException` com multiplos DataSources (datasource-proxy). |
| **@Primary JpaTransactionManager** | Spring Batch precisa de um TxManager primario quando ha ambiguidade. |
| **close-context-enabled: true** | Container encerra automaticamente apos job terminar. |
| **Kafka auto-startup: false** | Threads non-daemon do Kafka impedem shutdown. |
| **OTLP push** | Container efemero morre antes de Prometheus fazer scrape. |
| **run.id via timestamp** | Garante unicidade de JobInstance sem depender de RunIdIncrementer. |
| **Sem RunIdIncrementer** | Conflita com parametros de linha de comando, causando NOOP ou exceptions. |
| **Crontab do host (nao SCDF scheduler)** | SCDF Local Deployer nao implementa scheduling nativo. Script `scdf-schedule.sh` + crontab do Linux substitui. |

---

## 17. Mapa de Arquivos

```
docker-compose.yml                                    # SCDF + Skipper + infra
Dockerfile                                             # Build da imagem cinelog-app
docker/scdf/
├── entrypoint.sh                                      # Correcoes SCDF 2.11.x
├── docker-cli                                         # Binario Docker CLI (38.5MB)
├── docker-wrapper.sh                                  # Wrapper: --rm + network fix
├── init-scdf.sh                                       # Registro de tasks e schedules
├── launch-task.sh                                     # Lancamento manual via CLI
└── deployer-env.properties                            # Env vars para containers de task

docker/scdf/
├── schedules.cron                                     # Crontab com agendamentos (editavel)
└── scdf-schedule.sh                                   # Script de lancamento via REST API (usado pelo cron)

scripts/
└── scdf-schedule.sh                                   # Versao host do script (para uso fora do container)

src/main/resources/
├── application.yml                                    # Config principal
└── application-task.yml                               # Profile task (container efemero)

src/main/java/.../features/batch/
├── config/
│   ├── BatchJobsConfig.java                           # 12 job definitions
│   ├── BatchJobProperties.java                        # Config externalizada
│   └── TaskConfig.java                                # Spring Cloud Task manual
├── jobs/
│   ├── genres/SyncGenresTasklet.java
│   ├── media/LinkTmdbTasklet.java
│   ├── media/TmdbMediaPageReader.java
│   ├── credits/TmdbCreditsItemProcessor.java
│   ├── seasons/TmdbSeasonsItemProcessor.java
│   ├── reviews/TmdbReviewsItemProcessor.java
│   ├── images/TmdbMediaImageProcessor.java
│   ├── people/TmdbPersonDetailsProcessor.java
│   └── reports/
│       ├── WeeklyDigestTasklet.java                   # Email: digest semanal
│       ├── TrendingReportTasklet.java                 # Email: trending
│       └── PlatformReportTasklet.java                 # Email: relatorio admin
├── metrics/
│   └── BatchJobMetricsListener.java                   # Metricas Micrometer
└── web/
    └── BatchJobController.java                        # REST API (deprecated)

src/main/java/.../features/reports/
├── email/
│   ├── ReportEmailService.java                        # Orquestrador de emails
│   └── EmailService.java                              # Envio SMTP + Thymeleaf
├── query/
│   ├── WeeklyDigestQueryService.java
│   ├── TrendingQueryService.java
│   └── PlatformReportQueryService.java
└── config/
    └── ReportProperties.java

docs/
├── SCDF-GUIDE.md                                      # Guia completo do SCDF
├── SCDF-IMPLEMENTATION.md                             # Este documento
├── SCDF-DASHBOARD-GUIDE.md                            # Guia do Dashboard
├── BATCH-PERFORMANCE.md                               # Metricas e SLOs
├── SLI-DEFINITIONS.md                                 # SLIs e alertas
└── adr/ADR-014-scdf-batch-orchestration.md            # ADR da decisao
```
