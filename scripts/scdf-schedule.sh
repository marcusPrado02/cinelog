#!/usr/bin/env bash
# ============================================================================
# scdf-schedule.sh — Lanca um batch job no SCDF via REST API
#
# Uso direto:
#   bash scripts/scdf-schedule.sh sendWeeklyDigestJob
#   bash scripts/scdf-schedule.sh importMoviesJob "--cinelog.batch.max-pages=50"
#
# Uso via crontab (substitui schedules do SCDF que nao funcionam no Local Deployer):
#   crontab -e
#   # Digest semanal — segunda 08:00
#   0 8 * * 1  /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh sendWeeklyDigestJob
#   # Import filmes — domingo 03:30
#   30 3 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh importMoviesJob
#
# Variaveis de ambiente:
#   SCDF_URL       URL do SCDF Server       (default: http://localhost:9393)
#   SCDF_USER      Usuario Basic Auth        (default: admin)
#   SCDF_PASSWORD   Senha Basic Auth          (default: Admin@CineLog2025!)
# ============================================================================
set -euo pipefail

SCDF_URL="${SCDF_URL:-http://localhost:9393}"
SCDF_USER="${SCDF_USER:-admin}"
SCDF_PASSWORD="${SCDF_PASSWORD:-Admin@CineLog2025!}"

TASK_NAME="${1:?Uso: $0 <taskName> [argumentos]}"
EXTRA_ARGS="${2:-}"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
LOG_PREFIX="[scdf-schedule] [$TIMESTAMP]"

# Verifica se SCDF esta acessivel
if ! curl -sf -o /dev/null "$SCDF_URL/about" -u "$SCDF_USER:$SCDF_PASSWORD" 2>/dev/null; then
    echo "$LOG_PREFIX ERRO: SCDF nao esta acessivel em $SCDF_URL" >&2
    exit 1
fi

# Monta os parametros
PARAMS="name=$TASK_NAME"
PARAMS="$PARAMS&properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3"
if [ -n "$EXTRA_ARGS" ]; then
    PARAMS="$PARAMS&arguments=$EXTRA_ARGS"
fi

# Lanca a task
RESPONSE=$(curl -s -X POST "$SCDF_URL/tasks/executions/launch" \
    -d "$PARAMS" \
    -u "$SCDF_USER:$SCDF_PASSWORD" 2>&1)

# Extrai execution ID
EXEC_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('executionId','?'))" 2>/dev/null || echo "?")

if [ "$EXEC_ID" = "?" ]; then
    echo "$LOG_PREFIX ERRO ao lancar '$TASK_NAME': $RESPONSE" >&2
    exit 1
fi

echo "$LOG_PREFIX Lancado '$TASK_NAME' — execution ID: $EXEC_ID"
