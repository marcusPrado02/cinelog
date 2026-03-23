# Batch Performance & Hardening — CineLog

**Versao:** 2.0
**Data:** 2026-03-18
**Sprint:** Semana 4 — Versionamento, Metricas e Hardening

---

## Visao Geral

Os jobs Spring Batch do CineLog importam dados da API TMDB de forma assincrona e agendada.
Este documento registra as metricas alvo, a instrumentacao disponivel, as decisoes de tuning
e o checklist de hardening para producao.

---

## Jobs Configurados

### Jobs de Importacao TMDB (9 jobs)

| Job                       | Finalidade                          | Agendamento     | Skip Limit |
|---------------------------|-------------------------------------|-----------------|------------|
| `syncGenresJob`           | Sincroniza generos de filmes/series | Dom 03:00       | N/A        |
| `importMoviesJob`         | Importa filmes populares do TMDB    | Dom 03:30       | 50         |
| `importTvShowsJob`        | Importa series populares do TMDB    | Dom 04:00       | 50         |
| `importCreditsJob`        | Importa elenco e equipe tecnica     | Dom 04:30       | 50         |
| `importSeasonsJob`        | Importa temporadas e episodios      | Dom 05:00       | 50         |
| `syncReviewsJob`          | Sincroniza reviews externas do TMDB | Dom 05:30       | 100        |
| `enrichMediaImagesJob`    | Enriquece midia sem imagens         | Dom 06:00       | 50         |
| `enrichPersonProfilesJob` | Enriquece perfis de pessoas sem foto| Dom 06:30       | 50         |

### Jobs de Email/Relatorios (3 jobs)

| Job                       | Finalidade                          | Agendamento     | Skip Limit |
|---------------------------|-------------------------------------|-----------------|------------|
| `sendWeeklyDigestJob`     | Envia digest semanal para usuarios  | Manual (SCDF)   | N/A        |
| `sendTrendingReportJob`   | Envia relatorio trending            | Manual (SCDF)   | N/A        |
| `sendPlatformReportJob`   | Envia relatorio admin da plataforma | Manual (SCDF)   | N/A        |

Os jobs de email usam Tasklets (nao chunk-oriented) e variantes blocking do
`ReportEmailService`. Em dev, emails chegam no MailHog (http://localhost:8025).

---

## Configuracao de Chunk

```yaml
cinelog.batch:
  chunk-size: 20       # Itens por transacao
  max-pages: 10        # Paginas TMDB por execucao (global)
  sort-by: popularity.desc
```

**Por que chunk-size=20?**
Balanceia memoria heap (evita OOM em listas grandes) com overhead transacional.
Valores maiores (50-100) aumentam throughput mas exigem mais heap e aumentam o risco
de rollback caro em caso de falha no meio do chunk.

---

## Metricas Instrumentadas

O `BatchJobMetricsListener` registra as seguintes metricas no Micrometer:

### Por Job

| Metrica                      | Tipo    | Tags                 | Descricao                        |
|------------------------------|---------|----------------------|----------------------------------|
| `batch_job_duration_seconds` | Timer   | `job_name`, `status` | Duracao total do job             |
| `batch_job_completed_total`  | Counter | `job_name`, `status` | Contador de execucoes por status |

**Status possiveis:** `COMPLETED`, `FAILED`, `STOPPED`, `ABANDONED`

### Por Step

| Metrica                           | Tipo    | Tags                              | Descricao                |
|-----------------------------------|---------|------------------------------------|--------------------------|
| `batch_step_items_read_total`     | Counter | `job_name`, `step_name`            | Itens lidos no step      |
| `batch_step_items_written_total`  | Counter | `job_name`, `step_name`            | Itens gravados no step   |
| `batch_step_items_skipped_total`  | Counter | `job_name`, `step_name`            | Itens pulados            |
| `batch_step_duration_seconds`     | Timer   | `job_name`, `step_name`, `status`  | Duracao do step          |

**Importante:** O `metricsListener` deve estar registrado tanto no `JobBuilder.listener()` quanto
no `StepBuilder.listener()`. Sem o registro no step, as metricas `batch_step_*` nao disparam.

---

## Targets de Performance

> Targets baseline medidos com dataset TMDB de ~10.000 itens no ambiente local
> (MySQL 8.0 em Docker, 4 vCPU, 8 GB RAM).

| Job                       | Throughput alvo   | Duracao maxima (SLO) | Alerta Warning | Alerta Critical |
|---------------------------|-------------------|----------------------|----------------|-----------------|
| `syncGenresJob`           | N/A (tasklet)     | 30s                  | > 20s          | > 45s           |
| `importMoviesJob`         | >= 80 itens/seg   | 5 min                | > 3min         | > 7min          |
| `importTvShowsJob`        | >= 80 itens/seg   | 5 min                | > 3min         | > 7min          |
| `importCreditsJob`        | >= 40 itens/seg   | 10 min               | > 7min         | > 15min         |
| `importSeasonsJob`        | >= 20 itens/seg   | 15 min               | > 10min        | > 20min         |
| `syncReviewsJob`          | >= 60 itens/seg   | 8 min                | > 5min         | > 12min         |
| `enrichMediaImagesJob`    | >= 50 itens/seg   | 8 min                | > 5min         | > 12min         |
| `enrichPersonProfilesJob` | >= 50 itens/seg   | 8 min                | > 5min         | > 12min         |

**Calculo de throughput:** `batch_step_items_written_total / batch_step_duration_seconds`

---

## Consultas Prometheus

```promql
# Duracao media dos jobs (ultimas 24h)
rate(batch_job_duration_seconds_sum[24h]) / rate(batch_job_duration_seconds_count[24h])

# Taxa de falhas por job (ultimos 7 dias)
sum by (job_name) (rate(batch_job_completed_total{status="FAILED"}[7d]))

# Throughput de escrita por step (itens/seg na ultima hora)
rate(batch_step_items_written_total[1h])

# Razao de skips (alerta se > 5%)
sum by (job_name, step_name) (rate(batch_step_items_skipped_total[1h]))
  /
sum by (job_name, step_name) (rate(batch_step_items_read_total[1h]))

# Jobs que falharam nas ultimas 24h
increase(batch_job_completed_total{status="FAILED"}[24h]) > 0
```

---

## Decisoes de Tuning

### Retry e Skip

- **Skip:** Itens com dados corrompidos ou inconsistencias de schema sao pulados e logados
- **Fault Tolerant:** `faultTolerant().skip(Exception.class).skipLimit(N)` por step
- **Skip Limit:** 50 por step (syncReviews: 100 devido ao volume maior de dados inconsistentes)

### JPA e N+1

- Todos os readers usam `@Query` com `JOIN FETCH` explicito onde ha relacionamentos
- Chunk writers utilizam `saveAll()` — batch insert via Hibernate `batch_size=50`
- Sem `@OneToMany` lazy em entidades processadas por batch (carregamento eager ou projecao)

### Logs Estruturados

Cada job/step emite logs JSON com campos padronizados:

```json
{
  "level": "INFO",
  "logger": "...BatchJobMetricsListener",
  "message": "[BATCH] Job concluido: job=importMoviesJob status=COMPLETED itemsRead=1200 itemsWritten=1198 itemsSkipped=2"
}
```

---

## Hardening Checklist

### Seguranca

| Item | Status | Descricao |
|------|--------|-----------|
| H-SEC-01 | Implementado | `@PreAuthorize("hasRole('ADMIN')")` em todos os endpoints batch |
| H-SEC-02 | Implementado | JWT tokens validados antes do disparo manual |
| H-SEC-03 | Planejado | Rate limiting nos endpoints `/admin/batch/*` |
| H-SEC-04 | Planejado | Audit log para cada disparo de job (quem, quando, parametros) |
| H-SEC-05 | Planejado | SCDF Dashboard protegido por Basic Auth ou Keycloak OAuth2 |

### Resiliencia

| Item | Status | Descricao |
|------|--------|-----------|
| H-RES-01 | Implementado | `faultTolerant()` com `skip(Exception.class)` em todos os steps |
| H-RES-02 | Implementado | Skip limits definidos por step |
| H-RES-03 | Planejado | Circuit breaker na chamada TMDB API (Resilience4j) |
| H-RES-04 | Planejado | Timeout por step (via `TaskExecutorRepeatTemplate`) |
| H-RES-05 | Planejado | Dead letter queue para itens que excedem skip limit |

### Observabilidade

| Item | Status | Descricao |
|------|--------|-----------|
| H-OBS-01 | Implementado | `BatchJobMetricsListener` com metricas por job e step |
| H-OBS-02 | Implementado | Listener registrado em JobBuilder E StepBuilder |
| H-OBS-03 | Implementado | Logs estruturados com `[BATCH]` prefix |
| H-OBS-04 | Implementado | Tags `job_name`, `step_name`, `status` em todas as metricas |
| H-OBS-05 | Planejado | Distributed tracing (trace_id propagado para steps) |
| H-OBS-06 | Planejado | OTLP push para containers efemeros (SCDF mode) |

### Operacional

| Item | Status | Descricao |
|------|--------|-----------|
| H-OPS-01 | Implementado | Script `batch-ops.sh` com health check, trigger, metrics |
| H-OPS-02 | Implementado | Suite de testes automatizados (`batch-ops.sh test`) |
| H-OPS-03 | Implementado | Testes unitarios para `BatchJobMetricsListener` |
| H-OPS-04 | Implementado | Testes unitarios para `BatchJobsConfig` |
| H-OPS-05 | Planejado | Alertas Prometheus para BatchJobFailed, BatchJobTooSlow, SkipRatioHigh |
| H-OPS-06 | Planejado | Grafana dashboard dedicado para batch jobs |
| H-OPS-07 | Planejado | Runbook documentado para cada tipo de falha |

### Versionamento

| Item | Status | Descricao |
|------|--------|-----------|
| H-VER-01 | Implementado | API versionada via path (`/api/v1/admin/batch/*`) |
| H-VER-02 | Implementado | ADR-013 documenta estrategia de versionamento |
| H-VER-03 | Implementado | Convencao de nomes de metricas documentada |
| H-VER-04 | Implementado | Regras de evolucao de metricas (deprecacao em 2 sprints) |
| H-VER-05 | Planejado | Headers `Deprecation` e `Sunset` em endpoints deprecated |
| H-VER-06 | Planejado | OpenAPI groups separados por versao (`v1`, `v2`) |

---

## Matriz de Escalacao — Falhas de Batch

| Cenario | Severidade | Acao Imediata | Escalacao |
|---------|------------|---------------|-----------|
| 1 job falhou, demais ok | Warning | Investigar logs, re-executar manualmente | Dev on-call |
| Multiplos jobs falharam | Critical | Verificar TMDB API status e conectividade DB | Dev + SRE |
| Skip ratio > 10% em 1 step | Warning | Investigar dados de entrada, verificar schema | Dev |
| Job travado > 30min | Critical | Verificar processo, marcar ABANDONED se necessario | SRE |
| Todas as metricas de step zeradas | Bug | Verificar `.listener(metricsListener)` nos StepBuilders | Dev |
| Error budget batch esgotado | Critical | Pausar agendamento ate root cause resolvido | Dev + PM |

---

## Operacoes via Script

```bash
# Health check completo
./scripts/batch-ops.sh health

# Executar suite de testes automatizados
./scripts/batch-ops.sh test

# Executar testes unitarios Java
./scripts/batch-ops.sh test-unit

# Disparar job manualmente
./scripts/batch-ops.sh trigger importMoviesJob --maxPages 5

# Consultar metricas de um job
./scripts/batch-ops.sh metrics importMoviesJob

# Historico de execucoes
./scripts/batch-ops.sh history --limit 10

# Listar todos os jobs
./scripts/batch-ops.sh list-jobs
```

---

## Referencias

- [SLI-DEFINITIONS.md](./SLI-DEFINITIONS.md) — SLIs, SLOs e alertas
- [ADR-013: API Versioning](./adr/ADR-013-api-versioning-strategy.md) — versionamento
- [Spring Batch 5 — Monitoring](https://docs.spring.io/spring-batch/docs/current/reference/html/monitoring-and-metrics.html)
- `BatchJobMetricsListener.java` — `src/main/java/.../batch/metrics/`
- `BatchJobsConfig.java` — `src/main/java/.../batch/config/`
- `scripts/batch-ops.sh` — script de operacoes e testes

---

## Orquestracao via SCDF

A partir da integracao com **Spring Cloud Data Flow (SCDF)**, o ciclo de vida dos batch jobs
passa a ser gerenciado centralmente pelo SCDF Server em vez do agendamento interno do Spring.

### Como funciona

- **Containers efemeros:** Cada execucao de job e lancada pelo SCDF como um container Docker
  independente, usando a imagem `cinelog/cinelog-app:latest`. O container inicia, executa o job
  e encerra automaticamente ao finalizar.
- **Historico centralizado:** Todas as execucoes sao registradas nas tabelas `TASK_EXECUTION`
  e `TASK_EXECUTION_PARAMS` do banco de dados, permitindo auditoria e rastreabilidade completas
  pelo SCDF Dashboard.
- **Skipper como deployer:** O Skipper Server gerencia o deploy dos containers via Docker Deployer,
  conectando-se ao Docker daemon do host.

### Decisoes tecnicas

| Decisao | Motivo |
|---------|--------|
| `TaskConfigurer` explicito (`CustomTaskConfigurer`) | Garante que as tabelas `TASK_EXECUTION` usem o mesmo datasource da aplicacao, evitando datasource embarcado padrao |
| `@Primary` no `TransactionManager` | Resolve conflito entre `TransactionManager` do Spring Batch e do Spring Cloud Task, evitando `NoUniqueBeanDefinitionException` |
| Profile `task` no logback | Configura logging especifico para execucoes efemeras (ex.: log direto em console JSON, sem rotacao de arquivo) |

### Mais informacoes

Para detalhes completos sobre a arquitetura SCDF, configuracao do Docker Compose, registro de tasks
e operacoes via Dashboard, consulte o [Guia SCDF](./SCDF-GUIDE.md).
