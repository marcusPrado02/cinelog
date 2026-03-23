#!/bin/sh
# Lanca um batch job no SCDF via REST API.
# Projetado para rodar dentro do container scdf-scheduler (Alpine + curl).
set -eu

SCDF_URL="${SCDF_URL:-http://dataflow-server:9393}"
SCDF_USER="${SCDF_USER:-admin}"
SCDF_PASSWORD="${SCDF_PASSWORD:-Admin@CineLog2025!}"

TASK_NAME="${1:?Uso: $0 <taskName> [argumentos]}"
EXTRA_ARGS="${2:-}"

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Verifica se SCDF esta acessivel
if ! curl -sf -o /dev/null "$SCDF_URL/about" -u "$SCDF_USER:$SCDF_PASSWORD" 2>/dev/null; then
    echo "[$TIMESTAMP] ERRO: SCDF nao acessivel em $SCDF_URL" >&2
    exit 1
fi

# Monta os parametros
PARAMS="name=$TASK_NAME&properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3"
if [ -n "$EXTRA_ARGS" ]; then
    PARAMS="$PARAMS&arguments=$EXTRA_ARGS"
fi

# Lanca a task
RESPONSE=$(curl -s -X POST "$SCDF_URL/tasks/executions/launch" \
    -d "$PARAMS" \
    -u "$SCDF_USER:$SCDF_PASSWORD" 2>&1)

# Extrai execution ID
EXEC_ID=$(echo "$RESPONSE" | sed -n 's/.*"executionId":\([0-9]*\).*/\1/p')

if [ -z "$EXEC_ID" ]; then
    echo "[$TIMESTAMP] ERRO ao lancar '$TASK_NAME': $RESPONSE" >&2
    exit 1
fi

echo "[$TIMESTAMP] Lancado '$TASK_NAME' — execution ID: $EXEC_ID"
