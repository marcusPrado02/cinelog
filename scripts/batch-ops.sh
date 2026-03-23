#!/usr/bin/env bash
# ============================================================================
# batch-ops.sh — CineLog Batch Operations Guide & Automated Test Suite
# ============================================================================
#
# Script unificado para operar, diagnosticar e testar os Spring Batch jobs
# do CineLog, tanto no modo local (@Scheduled / BatchJobController) quanto
# no modo SCDF (Spring Cloud Data Flow).
#
# USO:
#   ./scripts/batch-ops.sh <comando> [opções]
#
# COMANDOS:
#   help          Mostra este guia completo
#   status        Status dos jobs (via Actuator ou SCDF)
#   trigger       Dispara um job manualmente
#   list-jobs     Lista todos os jobs configurados
#   metrics       Consulta métricas de batch no Prometheus
#   history       Histórico de execuções recentes
#   health        Health check do ambiente batch
#   test          Executa suite de testes automatizados do ambiente
#   test-unit     Executa apenas os testes unitários Java do batch
#
# EXEMPLOS:
#   ./scripts/batch-ops.sh help
#   ./scripts/batch-ops.sh status
#   ./scripts/batch-ops.sh trigger importMoviesJob
#   ./scripts/batch-ops.sh trigger importMoviesJob --maxPages 5
#   ./scripts/batch-ops.sh metrics importMoviesJob
#   ./scripts/batch-ops.sh history --limit 10
#   ./scripts/batch-ops.sh health
#   ./scripts/batch-ops.sh test
#   ./scripts/batch-ops.sh test-unit
#
# VARIÁVEIS DE AMBIENTE:
#   APP_URL         URL base da aplicação (default: http://localhost:8080)
#   SCDF_URL        URL do SCDF Server     (default: http://localhost:9393)
#   SCDF_USER       Usuário SCDF           (default: admin)
#   SCDF_PASS       Senha SCDF             (default: Admin@CineLog2025!)
#   PROMETHEUS_URL  URL do Prometheus       (default: http://localhost:9090)
#   AUTH_TOKEN       Bearer token JWT para endpoints admin
#   MODE            "local" ou "scdf"      (default: local)
#
# ============================================================================
set -euo pipefail

# ── Cores para output ────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# ── Configuração ─────────────────────────────────────────────────────────────
APP_URL="${APP_URL:-http://localhost:8080}"
SCDF_URL="${SCDF_URL:-http://localhost:9393}"
SCDF_USER="${SCDF_USER:-admin}"
SCDF_PASS="${SCDF_PASS:-Admin@CineLog2025!}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
MODE="${MODE:-local}"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# ── Jobs conhecidos ──────────────────────────────────────────────────────────
ALL_JOBS=(
    "syncGenresJob"
    "importMoviesJob"
    "importTvShowsJob"
    "importCreditsJob"
    "importSeasonsJob"
    "syncReviewsJob"
    "enrichMediaImagesJob"
    "enrichPersonProfilesJob"
)

# Mapeamento job → endpoint do BatchJobController
declare -A JOB_ENDPOINTS=(
    ["syncGenresJob"]="/api/v1/admin/batch/genres"
    ["importMoviesJob"]="/api/v1/admin/batch/movies"
    ["importTvShowsJob"]="/api/v1/admin/batch/tv-shows"
    ["importCreditsJob"]="/api/v1/admin/batch/credits"
    ["importSeasonsJob"]="/api/v1/admin/batch/seasons"
    ["syncReviewsJob"]="/api/v1/admin/batch/reviews"
    ["enrichMediaImagesJob"]="/api/v1/admin/batch/enrich-images"
    ["enrichPersonProfilesJob"]="/api/v1/admin/batch/enrich-profiles"
)

# ── Funções auxiliares ───────────────────────────────────────────────────────

log_info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[OK]${NC} $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $*"; }
log_header()  { echo -e "\n${BOLD}${CYAN}═══ $* ═══${NC}\n"; }

auth_header() {
    if [[ -n "$AUTH_TOKEN" ]]; then
        echo "-H 'Authorization: Bearer $AUTH_TOKEN'"
    fi
}

scdf_auth() {
    echo "-u $SCDF_USER:$SCDF_PASS"
}

curl_app() {
    local endpoint="$1"; shift
    if [[ -n "$AUTH_TOKEN" ]]; then
        curl -sf -H "Authorization: Bearer $AUTH_TOKEN" "$APP_URL$endpoint" "$@"
    else
        curl -sf "$APP_URL$endpoint" "$@"
    fi
}

curl_scdf() {
    local endpoint="$1"; shift
    curl -sf -u "$SCDF_USER:$SCDF_PASS" "$SCDF_URL$endpoint" "$@"
}

curl_prometheus() {
    local query="$1"
    curl -sf "$PROMETHEUS_URL/api/v1/query" --data-urlencode "query=$query"
}

# ============================================================================
# COMANDO: help
# ============================================================================
cmd_help() {
    cat << 'GUIDE'

╔══════════════════════════════════════════════════════════════════════════════╗
║                    CINELOG BATCH — GUIA COMPLETO DE OPERAÇÕES              ║
╚══════════════════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. ARQUITETURA                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

  O CineLog possui 8 Spring Batch jobs para importação de dados do TMDB:

  ┌────────────────────────┬───────────────────────────────────┬─────────────┐
  │ Job                    │ Finalidade                        │ Agendamento │
  ├────────────────────────┼───────────────────────────────────┼─────────────┤
  │ syncGenresJob          │ Sincroniza gêneros                │ Dom 03:00   │
  │ importMoviesJob        │ Importa filmes populares          │ Dom 03:30   │
  │ importTvShowsJob       │ Importa séries populares          │ Dom 04:00   │
  │ importCreditsJob       │ Importa elenco/equipe             │ Dom 04:30   │
  │ importSeasonsJob       │ Importa temporadas/episódios      │ Dom 05:00   │
  │ syncReviewsJob         │ Sincroniza reviews TMDB           │ Dom 05:30   │
  │ enrichMediaImagesJob   │ Enriquece imagens de mídia        │ Dom 06:00   │
  │ enrichPersonProfilesJob│ Enriquece perfis de pessoas       │ Dom 06:30   │
  └────────────────────────┴───────────────────────────────────┴─────────────┘

  Fluxo de dados:
    TMDB API → Reader → Processor → Writer → MySQL (cinelog schema)
                                           → Métricas (Micrometer/Prometheus)
                                           → Logs estruturados (JSON)

  Cada job segue o padrão chunk-oriented do Spring Batch 5:
    - chunk-size: 20 (configurável via cinelog.batch.chunk-size)
    - skip-limit: 50 por step (syncReviews: 100)
    - fault-tolerant: skip(Exception.class)

┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. MODOS DE OPERAÇÃO                                                        │
└─────────────────────────────────────────────────────────────────────────────┘

  LOCAL (MODE=local):
    - Jobs agendados via @Scheduled no BatchSchedulerConfig
    - Disparo manual via BatchJobController: POST /api/v1/admin/batch/*
    - Requer papel ADMIN no JWT

  SCDF (MODE=scdf):
    - Jobs registrados como Tasks no SCDF Server
    - Agendamento via SCDF Scheduler
    - Dashboard visual em http://localhost:9393/dashboard
    - Containers efêmeros via Docker Deployer

┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. MÉTRICAS DISPONÍVEIS                                                     │
└─────────────────────────────────────────────────────────────────────────────┘

  POR JOB:
    batch_job_duration_seconds   — Timer (duração total)
    batch_job_completed_total    — Counter (execuções por status)

  POR STEP:
    batch_step_items_read_total     — Counter (itens lidos)
    batch_step_items_written_total  — Counter (itens gravados)
    batch_step_items_skipped_total  — Counter (itens pulados)
    batch_step_duration_seconds     — Timer (duração do step)

  Tags: job_name, step_name, status

  CONSULTAS PROMETHEUS ÚTEIS:
    # Throughput de escrita por step (itens/seg)
    rate(batch_step_items_written_total[1h])

    # Taxa de falhas por job (últimos 7 dias)
    sum by (job_name) (rate(batch_job_completed_total{status="FAILED"}[7d]))

    # Razão de skips (alerta se > 5%)
    batch_step_items_skipped_total / batch_step_items_read_total

┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. ALERTAS CONFIGURADOS                                                     │
└─────────────────────────────────────────────────────────────────────────────┘

  BatchJobFailed       — batch_job_completed_total{status="FAILED"} > 0  [CRIT]
  BatchJobTooSlow      — batch_job_duration_seconds > SLA por job        [WARN]
  BatchSkipRatioHigh   — skip ratio > 5% por step                        [WARN]

┌─────────────────────────────────────────────────────────────────────────────┐
│ 5. TROUBLESHOOTING                                                          │
└─────────────────────────────────────────────────────────────────────────────┘

  Job falhou com FAILED:
    1. Verificar logs: grep "[BATCH]" logs/cinelog.log | tail -50
    2. Verificar métricas: ./batch-ops.sh metrics <jobName>
    3. Verificar skip ratio: se > 5%, investigar dados de entrada
    4. Re-executar: ./batch-ops.sh trigger <jobName>

  Job travado (STARTED por muito tempo):
    1. Verificar se o processo ainda está rodando
    2. Em último caso, marcar como ABANDONED no banco:
       UPDATE BATCH_JOB_EXECUTION SET STATUS='ABANDONED'
       WHERE JOB_EXECUTION_ID=<id> AND STATUS='STARTED';

  Métricas de step zeradas:
    - Verificar se .listener(metricsListener) está nos StepBuilders
    - O listener precisa estar no StepBuilder, NÃO apenas no JobBuilder

GUIDE
}

# ============================================================================
# COMANDO: list-jobs
# ============================================================================
cmd_list_jobs() {
    log_header "Jobs Spring Batch Configurados"

    printf "%-30s %-40s\n" "JOB NAME" "ENDPOINT (local mode)"
    printf "%-30s %-40s\n" "────────────────────────────" "────────────────────────────────────────"
    for job in "${ALL_JOBS[@]}"; do
        printf "%-30s %-40s\n" "$job" "${JOB_ENDPOINTS[$job]}"
    done
}

# ============================================================================
# COMANDO: status
# ============================================================================
cmd_status() {
    log_header "Status dos Batch Jobs"

    if [[ "$MODE" == "scdf" ]]; then
        log_info "Consultando SCDF Server em $SCDF_URL..."
        local tasks
        tasks=$(curl_scdf "/tasks/definitions" 2>/dev/null) || {
            log_error "Não foi possível conectar ao SCDF Server"
            return 1
        }
        echo "$tasks" | python3 -m json.tool 2>/dev/null || echo "$tasks"
    else
        log_info "Consultando Actuator em $APP_URL..."
        local health
        health=$(curl_app "/actuator/health" 2>/dev/null) || {
            log_error "Não foi possível conectar à aplicação"
            return 1
        }
        echo "$health" | python3 -m json.tool 2>/dev/null || echo "$health"

        echo ""
        log_info "Últimas execuções (via Spring Batch metadata):"
        local batch_health
        batch_health=$(curl_app "/actuator/health" 2>/dev/null | python3 -c "
import sys, json
data = json.load(sys.stdin)
components = data.get('components', {})
if 'db' in components:
    print('  Database:', components['db'].get('status', 'UNKNOWN'))
" 2>/dev/null) || true
        echo "$batch_health"
    fi
}

# ============================================================================
# COMANDO: trigger
# ============================================================================
cmd_trigger() {
    local job_name="${1:-}"
    shift || true

    if [[ -z "$job_name" ]]; then
        log_error "Uso: batch-ops.sh trigger <jobName> [--maxPages N]"
        log_info "Jobs disponíveis: ${ALL_JOBS[*]}"
        return 1
    fi

    # Validar nome do job
    local valid=false
    for j in "${ALL_JOBS[@]}"; do
        [[ "$j" == "$job_name" ]] && valid=true && break
    done
    if [[ "$valid" != "true" ]]; then
        log_error "Job desconhecido: $job_name"
        log_info "Jobs válidos: ${ALL_JOBS[*]}"
        return 1
    fi

    if [[ "$MODE" == "scdf" ]]; then
        log_info "Disparando task '$job_name' via SCDF..."
        local response
        response=$(curl_scdf "/tasks/executions" \
            -X POST \
            -d "name=$job_name" 2>/dev/null) || {
            log_error "Falha ao disparar task via SCDF"
            return 1
        }
        log_success "Task disparada: $response"
    else
        local endpoint="${JOB_ENDPOINTS[$job_name]}"
        local extra_params=""

        # Parse --maxPages
        while [[ $# -gt 0 ]]; do
            case "$1" in
                --maxPages)
                    extra_params="?maxPages=$2"
                    shift 2
                    ;;
                *)
                    shift
                    ;;
            esac
        done

        log_info "Disparando job '$job_name' via $APP_URL$endpoint$extra_params ..."
        local response
        response=$(curl_app "${endpoint}${extra_params}" -X POST 2>/dev/null) || {
            log_error "Falha ao disparar job. Verifique AUTH_TOKEN e se a app está rodando."
            return 1
        }
        log_success "Job disparado com sucesso:"
        echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    fi
}

# ============================================================================
# COMANDO: metrics
# ============================================================================
cmd_metrics() {
    local job_name="${1:-}"
    log_header "Métricas Batch"

    if [[ -z "$job_name" ]]; then
        log_info "Consultando métricas globais de batch..."
        local queries=(
            "batch_job_completed_total"
            "batch_job_duration_seconds_count"
            "batch_step_items_written_total"
        )
    else
        log_info "Consultando métricas para job '$job_name'..."
        local queries=(
            "batch_job_completed_total{job_name=\"$job_name\"}"
            "batch_job_duration_seconds_count{job_name=\"$job_name\"}"
            "batch_step_items_read_total{job_name=\"$job_name\"}"
            "batch_step_items_written_total{job_name=\"$job_name\"}"
            "batch_step_items_skipped_total{job_name=\"$job_name\"}"
        )
    fi

    for query in "${queries[@]}"; do
        echo -e "${CYAN}>>> $query${NC}"
        local result
        result=$(curl_prometheus "$query" 2>/dev/null) || {
            log_warn "Não foi possível consultar: $query"
            continue
        }
        echo "$result" | python3 -c "
import sys, json
data = json.load(sys.stdin)
results = data.get('data', {}).get('result', [])
if not results:
    print('  (sem dados)')
for r in results:
    metric = r.get('metric', {})
    value = r.get('value', [None, '?'])[1]
    labels = ', '.join(f'{k}={v}' for k, v in metric.items() if k != '__name__')
    print(f'  {labels}: {value}')
" 2>/dev/null || echo "  (parse error)"
        echo ""
    done
}

# ============================================================================
# COMANDO: history
# ============================================================================
cmd_history() {
    local limit="${1:-10}"
    log_header "Histórico de Execuções (últimas $limit)"

    if [[ "$MODE" == "scdf" ]]; then
        log_info "Consultando histórico via SCDF..."
        local response
        response=$(curl_scdf "/tasks/executions?size=$limit&sort=TASK_EXECUTION_ID,DESC" 2>/dev/null) || {
            log_error "Falha ao consultar histórico SCDF"
            return 1
        }
        echo "$response" | python3 -c "
import sys, json
data = json.load(sys.stdin)
embedded = data.get('_embedded', {}).get('taskExecutionResourceList', [])
if not embedded:
    print('  Nenhuma execução encontrada.')
    sys.exit(0)
print(f'{\"ID\":<6} {\"Task\":<30} {\"Status\":<12} {\"Início\":<22} {\"Fim\":<22}')
print('-' * 92)
for t in embedded:
    print(f'{t.get(\"executionId\",\"\"):<6} {t.get(\"taskName\",\"\"):<30} {str(t.get(\"exitCode\",\"\")):<12} {str(t.get(\"startTime\",\"\")):<22} {str(t.get(\"endTime\",\"\")):<22}')
" 2>/dev/null || echo "$response"
    else
        log_info "Modo local: histórico disponível via tabelas BATCH_JOB_EXECUTION no MySQL."
        echo "  Execute a query:"
        echo ""
        echo "  SELECT bje.JOB_EXECUTION_ID, bji.JOB_NAME, bje.STATUS,"
        echo "         bje.START_TIME, bje.END_TIME"
        echo "  FROM BATCH_JOB_EXECUTION bje"
        echo "  JOIN BATCH_JOB_INSTANCE bji ON bje.JOB_INSTANCE_ID = bji.JOB_INSTANCE_ID"
        echo "  ORDER BY bje.JOB_EXECUTION_ID DESC"
        echo "  LIMIT $limit;"
    fi
}

# ============================================================================
# COMANDO: health
# ============================================================================
cmd_health() {
    log_header "Health Check — Ambiente Batch"
    local errors=0

    # 1. Aplicação
    echo -e "${BOLD}1. Aplicação CineLog${NC}"
    if curl -sf "$APP_URL/actuator/health" > /dev/null 2>&1; then
        log_success "Aplicação acessível em $APP_URL"
    else
        log_error "Aplicação NÃO acessível em $APP_URL"
        ((errors++)) || true
    fi

    # 2. Prometheus
    echo -e "\n${BOLD}2. Prometheus${NC}"
    if curl -sf "$PROMETHEUS_URL/-/healthy" > /dev/null 2>&1; then
        log_success "Prometheus acessível em $PROMETHEUS_URL"
    else
        log_warn "Prometheus NÃO acessível em $PROMETHEUS_URL"
    fi

    # 3. SCDF (se modo SCDF)
    if [[ "$MODE" == "scdf" ]]; then
        echo -e "\n${BOLD}3. SCDF Server${NC}"
        if curl -sf -u "$SCDF_USER:$SCDF_PASS" "$SCDF_URL/management/health" > /dev/null 2>&1; then
            log_success "SCDF Server acessível em $SCDF_URL"
        else
            log_error "SCDF Server NÃO acessível em $SCDF_URL"
            ((errors++)) || true
        fi

        # Verificar tasks registradas
        echo -e "\n${BOLD}4. Tasks SCDF registradas${NC}"
        local task_count
        task_count=$(curl_scdf "/tasks/definitions" 2>/dev/null \
            | python3 -c "import sys,json; print(json.load(sys.stdin).get('page',{}).get('totalElements',0))" 2>/dev/null) || task_count=0

        if [[ "$task_count" -ge 8 ]]; then
            log_success "$task_count tasks registradas (esperado: 8)"
        else
            log_warn "Apenas $task_count tasks registradas (esperado: 8)"
            ((errors++)) || true
        fi
    fi

    # Resultado
    echo ""
    if [[ $errors -eq 0 ]]; then
        log_success "Health check concluído sem erros."
    else
        log_error "Health check encontrou $errors erro(s)."
        return 1
    fi
}

# ============================================================================
# COMANDO: test — Suite de Testes Automatizados
# ============================================================================

# Contadores globais de teste
TEST_PASS=0
TEST_FAIL=0
TEST_SKIP=0

assert_ok() {
    local description="$1"
    local result="$2"  # 0 = pass, 1 = fail, 2 = skip

    case "$result" in
        0) log_success "PASS: $description"; ((TEST_PASS++)) || true ;;
        1) log_error   "FAIL: $description"; ((TEST_FAIL++)) || true ;;
        2) log_warn    "SKIP: $description"; ((TEST_SKIP++)) || true ;;
    esac
}

cmd_test() {
    log_header "Suite de Testes Automatizados — CineLog Batch"
    TEST_PASS=0
    TEST_FAIL=0
    TEST_SKIP=0

    # ── T1: Verificar que todos os 8 jobs existem no código ──────────────────
    echo -e "\n${BOLD}T1. Verificação de Jobs no Código-fonte${NC}"
    for job in "${ALL_JOBS[@]}"; do
        if grep -rq "\"$job\"" "$PROJECT_ROOT/src/main/java/com/cine/cinelog/features/batch/" 2>/dev/null; then
            assert_ok "Job '$job' definido no código" 0
        else
            assert_ok "Job '$job' definido no código" 1
        fi
    done

    # ── T2: Verificar que metricsListener está registrado nos Steps ──────────
    echo -e "\n${BOLD}T2. StepBuilder com .listener(metricsListener)${NC}"
    local step_methods
    step_methods=$(grep -c '\.listener(metricsListener)' \
        "$PROJECT_ROOT/src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java" 2>/dev/null) || step_methods=0

    # Esperamos 16: 8 nos JobBuilders + 8 nos StepBuilders
    if [[ "$step_methods" -ge 16 ]]; then
        assert_ok "metricsListener registrado em todos os Jobs e Steps ($step_methods ocorrências)" 0
    else
        assert_ok "metricsListener registrado em todos os Jobs e Steps (encontrado: $step_methods, esperado: >=16)" 1
    fi

    # ── T3: Verificar que BatchJobMetricsListener implementa ambas interfaces
    echo -e "\n${BOLD}T3. BatchJobMetricsListener implementa JobExecutionListener e StepExecutionListener${NC}"
    local listener_file="$PROJECT_ROOT/src/main/java/com/cine/cinelog/features/batch/metrics/BatchJobMetricsListener.java"
    if grep -q "implements JobExecutionListener, StepExecutionListener" "$listener_file" 2>/dev/null; then
        assert_ok "Implementa ambas interfaces de listener" 0
    else
        assert_ok "Implementa ambas interfaces de listener" 1
    fi

    # ── T4: Verificar métricas nomeadas corretamente ─────────────────────────
    echo -e "\n${BOLD}T4. Nomes de métricas padronizados${NC}"
    local expected_metrics=(
        "batch_job_duration_seconds"
        "batch_job_completed_total"
        "batch_step_items_read_total"
        "batch_step_items_written_total"
        "batch_step_items_skipped_total"
        "batch_step_duration_seconds"
    )
    for metric in "${expected_metrics[@]}"; do
        if grep -rq "\"$metric\"" "$listener_file" 2>/dev/null; then
            assert_ok "Métrica '$metric' definida no listener" 0
        else
            assert_ok "Métrica '$metric' definida no listener" 1
        fi
    done

    # ── T5: Verificar tags obrigatórias ──────────────────────────────────────
    echo -e "\n${BOLD}T5. Tags de métricas (job_name, step_name, status)${NC}"
    for tag in "job_name" "step_name" "status"; do
        if grep -rq "\"$tag\"" "$listener_file" 2>/dev/null; then
            assert_ok "Tag '$tag' utilizada no listener" 0
        else
            assert_ok "Tag '$tag' utilizada no listener" 1
        fi
    done

    # ── T6: Verificar faultTolerant em todos os steps chunk-oriented ─────────
    echo -e "\n${BOLD}T6. Fault tolerance configurada nos Steps${NC}"
    local fault_tolerant_count
    fault_tolerant_count=$(grep -c '\.faultTolerant()' \
        "$PROJECT_ROOT/src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java" 2>/dev/null) || fault_tolerant_count=0

    # 7 chunk-oriented steps (excluindo syncGenresStep que é tasklet)
    if [[ "$fault_tolerant_count" -ge 7 ]]; then
        assert_ok "faultTolerant() em todos os steps chunk-oriented ($fault_tolerant_count ocorrências)" 0
    else
        assert_ok "faultTolerant() em todos os steps chunk-oriented (encontrado: $fault_tolerant_count, esperado: >=7)" 1
    fi

    # ── T7: Verificar que testes unitários existem ───────────────────────────
    echo -e "\n${BOLD}T7. Testes unitários do batch existem${NC}"
    local test_files=(
        "src/test/java/com/cine/cinelog/features/batch/metrics/BatchJobMetricsListenerTest.java"
        "src/test/java/com/cine/cinelog/features/batch/config/BatchJobsConfigTest.java"
    )
    for tf in "${test_files[@]}"; do
        if [[ -f "$PROJECT_ROOT/$tf" ]]; then
            assert_ok "Arquivo de teste existe: $tf" 0
        else
            assert_ok "Arquivo de teste existe: $tf" 1
        fi
    done

    # ── T8: Verificar estrutura BatchJobProperties ───────────────────────────
    echo -e "\n${BOLD}T8. BatchJobProperties tem 8 JobConfig entries${NC}"
    local job_config_count
    job_config_count=$(grep -c 'private JobConfig' \
        "$PROJECT_ROOT/src/main/java/com/cine/cinelog/features/batch/config/BatchJobProperties.java" 2>/dev/null) || job_config_count=0

    if [[ "$job_config_count" -ge 8 ]]; then
        assert_ok "BatchJobProperties tem $job_config_count JobConfig fields (esperado: 8)" 0
    else
        assert_ok "BatchJobProperties tem $job_config_count JobConfig fields (esperado: 8)" 1
    fi

    # ── T9: Verificar endpoints REST do BatchJobController ───────────────────
    echo -e "\n${BOLD}T9. BatchJobController endpoints${NC}"
    local controller_file="$PROJECT_ROOT/src/main/java/com/cine/cinelog/features/batch/web/BatchJobController.java"
    local endpoint_count
    endpoint_count=$(grep -c '@PostMapping' "$controller_file" 2>/dev/null) || endpoint_count=0

    if [[ "$endpoint_count" -ge 8 ]]; then
        assert_ok "BatchJobController tem $endpoint_count endpoints POST (esperado: 8)" 0
    else
        assert_ok "BatchJobController tem $endpoint_count endpoints POST (encontrado: $endpoint_count, esperado: 8)" 1
    fi

    # ── T10: Verificar segurança nos endpoints ───────────────────────────────
    echo -e "\n${BOLD}T10. Segurança (PreAuthorize) nos endpoints batch${NC}"
    if grep -q "@PreAuthorize" "$controller_file" 2>/dev/null; then
        assert_ok "BatchJobController protegido com @PreAuthorize" 0
    else
        assert_ok "BatchJobController protegido com @PreAuthorize" 1
    fi

    # ── T11: Se app está rodando, verificar Actuator ─────────────────────────
    echo -e "\n${BOLD}T11. Verificação online (se app estiver rodando)${NC}"
    if curl -sf "$APP_URL/actuator/health" > /dev/null 2>&1; then
        assert_ok "Aplicação acessível — Actuator health respondeu" 0

        # Verificar que métricas de batch estão expostas
        if curl -sf "$APP_URL/actuator/metrics" 2>/dev/null | grep -q "batch" 2>/dev/null; then
            assert_ok "Métricas de batch expostas via Actuator" 0
        else
            assert_ok "Métricas de batch expostas via Actuator" 2  # skip se app não tem dados
        fi
    else
        assert_ok "Aplicação offline — testes online pulados" 2
    fi

    # ── Resultado final ──────────────────────────────────────────────────────
    echo ""
    log_header "Resultado dos Testes"
    echo -e "  ${GREEN}PASS: $TEST_PASS${NC}  |  ${RED}FAIL: $TEST_FAIL${NC}  |  ${YELLOW}SKIP: $TEST_SKIP${NC}"
    echo -e "  Total: $((TEST_PASS + TEST_FAIL + TEST_SKIP)) testes"
    echo ""

    if [[ $TEST_FAIL -gt 0 ]]; then
        log_error "Suite de testes FALHOU com $TEST_FAIL erro(s)."
        return 1
    else
        log_success "Todos os testes passaram!"
        return 0
    fi
}

# ============================================================================
# COMANDO: test-unit — Executa testes unitários Java
# ============================================================================
cmd_test_unit() {
    log_header "Executando Testes Unitários Java — Batch"
    cd "$PROJECT_ROOT"

    log_info "Executando testes de BatchJobMetricsListenerTest e BatchJobsConfigTest..."
    mvn test \
        -pl . \
        -Dtest="com.cine.cinelog.features.batch.metrics.BatchJobMetricsListenerTest,com.cine.cinelog.features.batch.config.BatchJobsConfigTest" \
        -Dsurefire.useFile=false \
        -q

    local exit_code=$?
    if [[ $exit_code -eq 0 ]]; then
        log_success "Todos os testes unitários de batch passaram!"
    else
        log_error "Testes unitários falharam (exit code: $exit_code)"
    fi
    return $exit_code
}

# ============================================================================
# Dispatch
# ============================================================================
main() {
    local command="${1:-help}"
    shift || true

    case "$command" in
        help)       cmd_help ;;
        status)     cmd_status "$@" ;;
        trigger)    cmd_trigger "$@" ;;
        list-jobs)  cmd_list_jobs ;;
        metrics)    cmd_metrics "$@" ;;
        history)    cmd_history "$@" ;;
        health)     cmd_health "$@" ;;
        test)       cmd_test "$@" ;;
        test-unit)  cmd_test_unit "$@" ;;
        *)
            log_error "Comando desconhecido: $command"
            echo "Use './scripts/batch-ops.sh help' para ver os comandos disponíveis."
            return 1
            ;;
    esac
}

main "$@"
