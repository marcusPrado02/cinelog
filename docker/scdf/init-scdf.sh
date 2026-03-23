#!/usr/bin/env bash
# ============================================================================
# init-scdf.sh — Registra app, tasks e schedules no SCDF (idempotente)
#
# USO:
#   bash docker/scdf/init-scdf.sh
#
# VARIAVEIS DE AMBIENTE:
#   SCDF_URL        URL do SCDF Server       (default: http://localhost:9393)
#   CINELOG_IMAGE    Imagem Docker da app     (default: docker:cinelog/cinelog-app:latest)
# ============================================================================
set -euo pipefail

SCDF_URL="${SCDF_URL:-http://localhost:9393}"
CINELOG_IMAGE="${CINELOG_IMAGE:-docker:cinelog/cinelog-app:latest}"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'
BOLD='\033[1m'; NC='\033[0m'
log_ok()   { echo -e "  ${GREEN}✓${NC} $*"; }
log_skip() { echo -e "  ${YELLOW}○${NC} $*"; }
log_fail() { echo -e "  ${RED}✗${NC} $*"; }

echo -e "${BOLD}=== SCDF Init: Registrando app, tasks e schedules ===${NC}"
echo -e "  SCDF URL: $SCDF_URL"
echo -e "  Image:    $CINELOG_IMAGE"
echo ""

# ── Aguarda SCDF estar pronto ────────────────────────────────────────────────
echo -n "Aguardando SCDF..."
WAIT=0
until curl -sf "$SCDF_URL/about" > /dev/null 2>&1; do
    echo -n "."
    sleep 5
    ((WAIT+=5))
    if [[ $WAIT -ge 120 ]]; then
        echo ""
        log_fail "SCDF nao respondeu em 120s. Verifique: docker logs cinelog-dataflow"
        exit 1
    fi
done
echo -e " ${GREEN}OK${NC}"

# ── 1. Registra app ──────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}--- App Registration ---${NC}"
RESP=$(curl -sf -o /dev/null -w "%{http_code}" -X POST "$SCDF_URL/apps/task/cinelog" \
    -d "uri=$CINELOG_IMAGE" -d "force=false" -d "bootVersion=3" 2>/dev/null || echo "409")
if [[ "$RESP" == "201" || "$RESP" == "200" ]]; then
    log_ok "App 'cinelog' registrada ($CINELOG_IMAGE)"
else
    log_skip "App 'cinelog' ja registrada (ou URI igual)"
fi

# ── 2. Carrega deployer env vars ─────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOYER_ENV=""
if [[ -f "$SCRIPT_DIR/deployer-env.properties" ]]; then
    DEPLOYER_ENV=$(grep -v '^\s*#' "$SCRIPT_DIR/deployer-env.properties" \
        | grep -v '^\s*$' \
        | tr '\n' ',')
    DEPLOYER_ENV="${DEPLOYER_ENV%,}"
fi

# ── 3. Registra tasks ────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}--- Task Definitions (9) ---${NC}"

register_task() {
    local name=$1 job=$2 extra_args="${3:-}"
    EXISTS=$(curl -sf -o /dev/null -w "%{http_code}" "$SCDF_URL/tasks/definitions/$name" 2>/dev/null || echo "404")
    if [[ "$EXISTS" == "200" ]]; then
        log_skip "Task '$name' ja existe"
        return 0
    fi
    local definition="cinelog --spring.batch.job.name=$job"
    [[ -n "$extra_args" ]] && definition="$definition $extra_args"
    local result
    result=$(curl -sf -o /dev/null -w "%{http_code}" -X POST "$SCDF_URL/tasks/definitions" \
        -d "name=$name" \
        -d "definition=$definition" \
        2>/dev/null || echo "000")
    if [[ "$result" == "200" || "$result" == "201" ]]; then
        log_ok "Task '$name' registrada (job: $job${extra_args:+ [$extra_args]})"
    else
        log_fail "Task '$name' falhou (HTTP $result)"
    fi
}

register_task syncGenresJob           syncGenresJob
register_task importMoviesJob         importMoviesJob "--mediaType=MOVIE"
register_task importTvShowsJob        importTvShowsJob "--mediaType=SERIES"
register_task importCreditsJob        importCreditsJob
register_task importSeasonsJob        importSeasonsJob
register_task syncReviewsJob          syncReviewsJob
register_task enrichMediaImagesJob    enrichMediaImagesJob
register_task enrichPersonProfilesJob enrichPersonProfilesJob
register_task linkTmdbJob             linkTmdbJob
register_task sendWeeklyDigestJob     sendWeeklyDigestJob
register_task sendTrendingReportJob   sendTrendingReportJob
register_task sendPlatformReportJob   sendPlatformReportJob

# ── 4. Registra schedules (8 — linkTmdbJob e reports nao tem schedule) ────────
echo ""
echo -e "${BOLD}--- Schedules (8) ---${NC}"

register_schedule() {
    local name=$1 task=$2 cron_expr=$3
    EXISTS=$(curl -sf -o /dev/null -w "%{http_code}" "$SCDF_URL/tasks/schedules/$name" 2>/dev/null || echo "404")
    if [[ "$EXISTS" == "200" ]]; then
        log_skip "Schedule '$name' ja existe"
        return 0
    fi
    local result
    result=$(curl -sf -o /dev/null -w "%{http_code}" -X POST "$SCDF_URL/tasks/schedules" \
        -d "scheduleName=$name" \
        -d "taskDefinitionName=$task" \
        -d "properties=scheduler.cron.expression=$cron_expr" \
        2>/dev/null || echo "000")
    if [[ "$result" == "200" || "$result" == "201" ]]; then
        log_ok "Schedule '$name' ($cron_expr)"
    else
        log_skip "Schedule '$name' — scheduler pode nao estar habilitado (HTTP $result)"
    fi
}

register_schedule sched-sync-genres           syncGenresJob           "0 0 3 * * 0"
register_schedule sched-import-movies         importMoviesJob         "0 30 3 * * 0"
register_schedule sched-import-tvshows        importTvShowsJob        "0 0 4 * * 0"
register_schedule sched-import-credits        importCreditsJob        "0 30 4 * * 0"
register_schedule sched-import-seasons        importSeasonsJob        "0 0 5 * * 0"
register_schedule sched-sync-reviews          syncReviewsJob          "0 30 5 * * 0"
register_schedule sched-enrich-images         enrichMediaImagesJob    "0 0 6 * * 0"
register_schedule sched-enrich-profiles       enrichPersonProfilesJob "0 30 6 * * 0"

# ── Resumo ───────────────────────────────────────────────────────────────────
echo ""
TASK_COUNT=$(curl -sf "$SCDF_URL/tasks/definitions" 2>/dev/null \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('page',{}).get('totalElements',0))" 2>/dev/null || echo "?")

echo -e "${BOLD}=== SCDF Init: Concluido ===${NC}"
echo -e "  Tasks registradas: $TASK_COUNT"
echo -e "  Dashboard: ${GREEN}http://localhost:9393/dashboard${NC}"
