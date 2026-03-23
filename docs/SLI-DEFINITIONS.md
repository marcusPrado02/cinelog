# SLI Definitions — CineLog API & Batch

**Versao:** 2.0
**Data:** 2026-03-18
**Sprint:** Semana 4 — Versionamento, Metricas e Hardening
**Responsavel:** Equipe CineLog

---

## O que sao SLIs?

**Service Level Indicators (SLIs)** sao metricas quantitativas que medem a qualidade
do servico percebida pelo usuario. Cada SLI define *o que medir* e *como medir*.

> SLI → o que medimos | SLO → o alvo que queremos atingir | SLA → o compromisso contratual

---

## Parte 1: SLIs da API HTTP

### 1.1 Disponibilidade (Availability)

**Definicao:** Proporcao de requests que retornam resposta HTTP bem-sucedida (< 5xx).

```
Disponibilidade = (requests_totais - requests_5xx) / requests_totais
```

**Metrica Prometheus:**
```promql
1 - (
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  /
  sum(rate(http_server_requests_seconds_count[5m]))
)
```

**SLO Alvo:** >= 99.5% (mensal)
**Janela de medicao:** Rolling 30 dias

| Nivel     | SLO      | Downtime permitido/mes |
|-----------|----------|------------------------|
| Interno   | 99.0%    | ~7h 18min              |
| Producao  | 99.5%    | ~3h 39min              |
| Premium   | 99.9%    | ~43min (objetivo futuro)|

---

### 1.2 Latencia (p95)

**Definicao:** 95% das requisicoes de usuario completam em menos do que o threshold alvo.

```
Latencia p95 = percentil 95 da distribuicao de latencia das requests (ms)
```

**Metrica Prometheus:**
```promql
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri, method)
)
```

**SLOs por categoria de endpoint:**

| Categoria             | Exemplos                        | p95 Alvo   | p99 Alvo    |
|-----------------------|---------------------------------|------------|-------------|
| Leitura simples       | `GET /media/{id}`, `GET /genres`| <= 200ms   | <= 500ms    |
| Leitura com paginacao | `GET /media`, `GET /watch-entries` | <= 400ms | <= 800ms    |
| Escrita               | `POST /media`, `POST /watch-entries` | <= 600ms | <= 1200ms |
| Auth (login/register) | `POST /auth/login`              | <= 800ms   | <= 1500ms   |
| Recommendations       | `GET /recommendations`          | <= 1000ms  | <= 2000ms   |
| Batch (admin trigger) | `POST /admin/batch/*`           | <= 3000ms  | N/A         |

---

### 1.3 Taxa de Erros 5xx

**Definicao:** Proporcao de requisicoes que resultam em erro interno do servidor (HTTP 5xx).

```
Taxa 5xx = requests_5xx / requests_totais
```

**Metrica Prometheus:**
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))
```

**SLO Alvo:** < 0.5% (por janela de 5 minutos)

| Faixa       | Estado         | Acao                                          |
|-------------|----------------|-----------------------------------------------|
| < 0.1%      | Normal         | Nenhuma                                       |
| 0.1% - 0.5% | Atencao       | Investigar logs, verificar dependencias       |
| 0.5% - 1%   | Degradado     | Alerta para on-call, analise imediata         |
| > 1%        | Incidente      | Incident response, possivel rollback          |

---

## Parte 2: SLIs de Batch Jobs

### 2.1 Taxa de Sucesso de Jobs

**Definicao:** Proporcao de execucoes de batch que completam com status COMPLETED.

```
Taxa de Sucesso = batch_job_completed{status="COMPLETED"} / batch_job_completed{status=~".+"}
```

**Metrica Prometheus:**
```promql
sum by (job_name) (rate(batch_job_completed_total{status="COMPLETED"}[7d]))
/
sum by (job_name) (rate(batch_job_completed_total[7d]))
```

**SLO Alvo:** >= 95% por job (rolling 7 dias)

| Faixa           | Estado     | Acao                                             |
|-----------------|------------|--------------------------------------------------|
| >= 95%          | Normal     | Nenhuma                                          |
| 90% - 95%      | Atencao    | Revisar logs de skip e dados de entrada          |
| 80% - 90%      | Degradado  | Investigar imediatamente; pode ser API TMDB down |
| < 80%          | Critico    | Pausar agendamento, investigar root cause        |

---

### 2.2 Duracao de Job (SLA de tempo)

**Definicao:** Cada job deve completar dentro do tempo maximo aceitavel.

**Metrica Prometheus:**
```promql
batch_job_duration_seconds{job_name="importMoviesJob", quantile="0.95"}
```

**SLOs por job:**

| Job                      | Duracao maxima (SLO) | Alerta Warning | Alerta Critical |
|--------------------------|----------------------|----------------|-----------------|
| `syncGenresJob`          | 30s                  | > 20s          | > 45s           |
| `importMoviesJob`        | 5min                 | > 3min         | > 7min          |
| `importTvShowsJob`       | 5min                 | > 3min         | > 7min          |
| `importCreditsJob`       | 10min                | > 7min         | > 15min         |
| `importSeasonsJob`       | 15min                | > 10min        | > 20min         |
| `syncReviewsJob`         | 8min                 | > 5min         | > 12min         |
| `enrichMediaImagesJob`   | 8min                 | > 5min         | > 12min         |
| `enrichPersonProfilesJob`| 8min                 | > 5min         | > 12min         |

---

### 2.3 Taxa de Skip

**Definicao:** Proporcao de itens pulados (read_skip + write_skip) em relacao ao total lido.

```
Skip Ratio = (read_skips + write_skips) / items_read
```

**Metrica Prometheus:**
```promql
sum by (job_name, step_name) (rate(batch_step_items_skipped_total[1h]))
/
sum by (job_name, step_name) (rate(batch_step_items_read_total[1h]))
```

**SLO Alvo:** < 5% por step (rolling 1h)

| Faixa      | Estado      | Acao                                                   |
|------------|-------------|--------------------------------------------------------|
| < 1%       | Normal      | Nenhuma                                                |
| 1% - 5%   | Atencao     | Verificar dados de entrada, logs de skip               |
| 5% - 10%  | Degradado   | Revisar schema TMDB, verificar rate limits             |
| > 10%     | Critico     | Pausar job, investigar dados corrompidos ou API changes |

---

### 2.4 Throughput de Escrita

**Definicao:** Taxa de itens gravados por segundo por step.

**Metrica Prometheus:**
```promql
rate(batch_step_items_written_total[5m])
```

**SLOs por job (itens/seg):**

| Job                      | Throughput minimo |
|--------------------------|-------------------|
| `importMoviesJob`        | >= 80             |
| `importTvShowsJob`       | >= 80             |
| `importCreditsJob`       | >= 40             |
| `importSeasonsJob`       | >= 20             |
| `syncReviewsJob`         | >= 60             |
| `enrichMediaImagesJob`   | >= 50             |
| `enrichPersonProfilesJob`| >= 50             |

---

## Alertas Prometheus

### Alertas HTTP

```yaml
- alert: HighErrorRate5xx
  expr: |
    sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
    / sum(rate(http_server_requests_seconds_count[5m])) > 0.005
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "Taxa de erros 5xx acima de 0.5%"
    runbook: "docs/runbooks/high-error-rate.md"

- alert: HighLatencyP95
  expr: |
    histogram_quantile(0.95,
      sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
    ) > 1.0
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "p95 de latencia acima de 1 segundo"
    runbook: "docs/runbooks/high-latency.md"

- alert: LowAvailability
  expr: |
    (1 - (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[1h]))
      / sum(rate(http_server_requests_seconds_count[1h]))
    )) < 0.995
  for: 10m
  labels:
    severity: critical
  annotations:
    summary: "Disponibilidade abaixo de 99.5%"
    runbook: "docs/runbooks/low-availability.md"
```

### Alertas Batch

```yaml
- alert: BatchJobFailed
  expr: increase(batch_job_completed_total{status="FAILED"}[15m]) > 0
  for: 0m
  labels:
    severity: critical
  annotations:
    summary: "Batch job {{ $labels.job_name }} falhou"
    runbook: "docs/runbooks/batch-job-failed.md"

- alert: BatchJobTooSlow
  expr: |
    batch_job_duration_seconds_sum / batch_job_duration_seconds_count > 600
  for: 0m
  labels:
    severity: warning
  annotations:
    summary: "Job {{ $labels.job_name }} excedeu 10min de duracao media"

- alert: BatchSkipRatioHigh
  expr: |
    (
      sum by (job_name, step_name) (rate(batch_step_items_skipped_total[1h]))
      /
      sum by (job_name, step_name) (rate(batch_step_items_read_total[1h]))
    ) > 0.05
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Skip ratio acima de 5% no step {{ $labels.step_name }}"

- alert: BatchThroughputLow
  expr: |
    rate(batch_step_items_written_total{job_name=~"import.*"}[10m]) < 20
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Throughput abaixo de 20 itens/seg no job {{ $labels.job_name }}"

- alert: BatchJobStuck
  expr: |
    (time() - batch_job_duration_seconds_sum) > 1800
    and batch_job_completed_total == 0
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Job {{ $labels.job_name }} pode estar travado (>30min sem completar)"
```

---

## Exclusoes de Medicao

Os seguintes endpoints sao **excluidos** dos SLIs de disponibilidade e latencia:

| Endpoint              | Motivo                                                      |
|-----------------------|-------------------------------------------------------------|
| `/actuator/**`        | Endpoints de infraestrutura, nao expostos ao usuario final  |
| `/swagger-ui/**`      | Documentacao, nao fluxo de negocio                          |
| `/v3/api-docs`        | Documentacao                                                |
| `OPTIONS *`           | Pre-flight CORS, sem logica de negocio                      |

---

## Dashboards

| Dashboard          | URL Local                                     | Metricas Chave                          |
|--------------------|-----------------------------------------------|-----------------------------------------|
| Overview           | http://localhost:3000/d/cinelog-overview       | Availability, p95, 5xx rate             |
| Latencia / p95     | http://localhost:3000/d/cinelog-latency        | p50/p95/p99 por endpoint                |
| Erros & 5xx        | http://localhost:3000/d/cinelog-errors         | Error rate, error budget burn           |
| Batch Performance  | http://localhost:3000/d/cinelog-batch          | Duracao, throughput, skip ratio por job |
| Error Budget       | http://localhost:3000/d/cinelog-error-budget   | Budget restante, burn rate, forecast    |

---

## Error Budget

### API HTTP

Com SLO de disponibilidade de 99.5% em 30 dias:

```
Error Budget = (1 - SLO) x janela_em_minutos
             = 0.5% x 43.200min
             = 216 minutos/mes de indisponibilidade permitida
```

### Batch Jobs

Com SLO de sucesso de 95% em 7 dias (execucao semanal = ~8 execucoes):

```
Error Budget = (1 - SLO) x total_execucoes_esperadas
             = 5% x 8 execucoes/semana
             = 0.4 falhas permitidas/semana (arredondado: 0)
```

Na pratica: **nenhuma falha de batch e tolerada sem investigacao**. Cada falha
consome 100% do error budget semanal daquele job.

### Burn Rate Alerts

```yaml
# Multi-window burn rate (Google SRE Workbook pattern)

# Rapido: 2% do budget consumido em 5min (extrapola 100% em ~4h)
- alert: ErrorBudgetBurnFast
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
      / sum(rate(http_server_requests_seconds_count[5m]))
    ) > (14.4 * 0.005)
  for: 2m
  labels:
    severity: critical
    category: error_budget
  annotations:
    summary: "Burn rate 14.4x — error budget sera esgotado em ~4h"

# Lento: budget sendo consumido 3x mais rapido que o normal
- alert: ErrorBudgetBurnSlow
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[1h]))
      / sum(rate(http_server_requests_seconds_count[1h]))
    ) > (3 * 0.005)
  for: 15m
  labels:
    severity: warning
    category: error_budget
  annotations:
    summary: "Burn rate 3x — error budget sera esgotado em ~10 dias"
```

---

## Versionamento de Metricas

### Convencao de nomes

Todas as metricas customizadas seguem o padrao:

```
<dominio>_<entidade>_<acao>_<unidade>
```

| Prefixo         | Dominio          | Exemplo                        |
|-----------------|------------------|--------------------------------|
| `batch_job_`    | Batch jobs       | `batch_job_duration_seconds`   |
| `batch_step_`   | Batch steps      | `batch_step_items_read_total`  |
| `http_server_`  | API HTTP         | (auto-instrumentado)           |
| `cinelog_`      | Negocio          | `cinelog_watch_entries_total`   |

### Regras de evolucao

| Mudanca                          | Breaking? | Acao                              |
|----------------------------------|-----------|-----------------------------------|
| Adicionar nova metrica           | Nao       | Adicionar normalmente             |
| Adicionar tag a metrica existente| Nao       | Adicionar com valor default       |
| Remover metrica                  | Sim       | Deprecar por 2 sprints, depois remover |
| Renomear metrica                 | Sim       | Criar nova + deprecar antiga      |
| Alterar tipo (Counter→Gauge)     | Sim       | Criar nova + deprecar antiga      |
| Alterar semantica de tag         | Sim       | Nova tag + deprecar antiga        |

### Deprecacao de metricas

Metricas deprecated devem:
1. Manter emissao por pelo menos 2 sprints (4 semanas)
2. Incluir sufixo `_deprecated` no nome (ex: `batch_old_metric_deprecated`)
3. Documentar a metrica substituta no CHANGELOG

---

## Referências

- [OBSERVABILITY.md](./OBSERVABILITY.md) — stack de observabilidade
- [BATCH-PERFORMANCE.md](./BATCH-PERFORMANCE.md) — metricas de batch
- [ADR-013: API Versioning](./adr/ADR-013-api-versioning-strategy.md) — versionamento de API
- [Google SRE Book — SLIs, SLOs, SLAs](https://sre.google/sre-book/service-level-objectives/)
- [Google SRE Workbook — Alerting on SLOs](https://sre.google/workbook/alerting-on-slos/)
- Micrometer: `http_server_requests_seconds` (auto-instrumentado pelo Spring Boot Actuator)
- `BatchJobMetricsListener.java` — metricas customizadas de batch
- `scripts/batch-ops.sh` — script de operacoes e testes automatizados
