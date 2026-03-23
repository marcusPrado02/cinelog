#!/usr/bin/env bash
# ============================================================================
# launch-task.sh — Executa um batch job CineLog via container Docker efemero
#
# USO:
#   bash docker/scdf/launch-task.sh syncGenresJob
#   bash docker/scdf/launch-task.sh importMoviesJob
#   bash docker/scdf/launch-task.sh sendWeeklyDigestJob
#   bash docker/scdf/launch-task.sh sendTrendingReportJob
#   bash docker/scdf/launch-task.sh sendPlatformReportJob
#
# MAIL: Se .env.mail existir na raiz do projeto, usa credenciais reais (Gmail).
#       Caso contrario, usa MailHog (desenvolvimento).
#
# O container roda na rede cinelog_default com acesso ao MySQL, Redis e SMTP.
# Apos execucao, o resultado fica visivel nas tabelas TASK_EXECUTION e
# BATCH_JOB_EXECUTION do MySQL, e no SCDF Dashboard (Task Executions).
# ============================================================================
set -euo pipefail

TASK_NAME="${1:?Uso: $0 <task-name> (ex: syncGenresJob, sendWeeklyDigestJob)}"

GREEN='\033[0;32m'; RED='\033[0;31m'; CYAN='\033[0;36m'
YELLOW='\033[1;33m'; BOLD='\033[1m'; NC='\033[0m'

echo -e "${BOLD}=== CineLog Batch: $TASK_NAME ===${NC}"
echo ""

# Verifica se a imagem existe
if ! docker image inspect cinelog/cinelog-app:latest > /dev/null 2>&1; then
    echo -e "${RED}Imagem cinelog/cinelog-app:latest nao encontrada.${NC}"
    echo "  Build: docker build -t cinelog/cinelog-app:latest ."
    exit 1
fi

# ── Detecta configuracao de email ──────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_MAIL="$PROJECT_ROOT/.env.mail"

MAIL_ENV_ARGS=()
if [[ -f "$ENV_MAIL" ]]; then
    _MAIL_HOST=$(grep "^export MAIL_HOST=" "$ENV_MAIL" | sed 's/^export MAIL_HOST=//')
    _MAIL_PORT=$(grep "^export MAIL_PORT=" "$ENV_MAIL" | sed 's/^export MAIL_PORT=//')
    _MAIL_USER=$(grep "^export MAIL_USER=" "$ENV_MAIL" | sed 's/^export MAIL_USER=//')
    _MAIL_PASS=$(grep "^export MAIL_PASS=" "$ENV_MAIL" | sed 's/^export MAIL_PASS=//')
    if [[ -n "${_MAIL_HOST:-}" && "$_MAIL_HOST" != "mailhog" ]]; then
        echo -e "  ${GREEN}Mail:${NC} ${_MAIL_HOST}:${_MAIL_PORT} (${_MAIL_USER})"
        MAIL_ENV_ARGS=(
            -e SPRING_MAIL_HOST="$_MAIL_HOST"
            -e SPRING_MAIL_PORT="${_MAIL_PORT:-587}"
            -e SPRING_MAIL_USERNAME="$_MAIL_USER"
            -e SPRING_MAIL_PASSWORD="$_MAIL_PASS"
            -e "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true"
            -e "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true"
        )
    fi
fi

if [[ ${#MAIL_ENV_ARGS[@]} -eq 0 ]]; then
    echo -e "  ${YELLOW}Mail:${NC} MailHog (localhost:1025)"
    MAIL_ENV_ARGS=(
        -e SPRING_MAIL_HOST=mailhog
        -e SPRING_MAIL_PORT=1025
    )
fi

echo -e "${CYAN}Lancando container efemero...${NC}"
echo ""

CONTAINER_NAME="${TASK_NAME}-$(date +%s)"

docker run --rm \
    --name "$CONTAINER_NAME" \
    --network cinelog_default \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/cinelog?useSSL=false&allowPublicKeyRetrieval=true" \
    -e SPRING_DATASOURCE_USERNAME=cinelog \
    -e SPRING_DATASOURCE_PASSWORD=cinelog \
    -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
    -e SPRING_DATA_REDIS_HOST=redis \
    "${MAIL_ENV_ARGS[@]}" \
    -e SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
    -e SPRING_PROFILES_ACTIVE=task \
    cinelog/cinelog-app:latest \
    --spring.batch.job.name="$TASK_NAME" 2>&1 | while IFS= read -r line; do
        # Filtra apenas linhas relevantes para output limpo
        if echo "$line" | grep -qiE "Started|COMPLETED|FAILED|Job \[|BATCH.*Job|exit|Error|Exception|email|mail|digest|trending|report|sending"; then
            echo "  $line"
        fi
    done

EXIT_CODE=$?

echo ""
if [[ $EXIT_CODE -eq 0 ]]; then
    echo -e "${GREEN}Job $TASK_NAME executado com sucesso!${NC}"
else
    echo -e "${RED}Job $TASK_NAME falhou (exit code: $EXIT_CODE)${NC}"
fi

# Mostra resultado do banco
echo ""
echo -e "${BOLD}--- Resultado no MySQL ---${NC}"
docker exec cinelog-mysql mysql -ucinelog -pcinelog cinelog -N -e \
    "SELECT CONCAT('  Task: ', TASK_NAME, ' | Exit: ', IFNULL(EXIT_CODE,'?'), ' | ', IFNULL(START_TIME,'?'), ' -> ', IFNULL(END_TIME,'?'))
     FROM BOOT3_TASK_EXECUTION ORDER BY TASK_EXECUTION_ID DESC LIMIT 3;" 2>/dev/null || \
docker exec cinelog-mysql mysql -ucinelog -pcinelog cinelog -N -e \
    "SELECT CONCAT('  Task: ', TASK_NAME, ' | Exit: ', IFNULL(EXIT_CODE,'?'), ' | ', IFNULL(START_TIME,'?'), ' -> ', IFNULL(END_TIME,'?'))
     FROM TASK_EXECUTION ORDER BY TASK_EXECUTION_ID DESC LIMIT 3;" 2>/dev/null || echo "  (tabelas TASK_EXECUTION nao disponiveis)"

echo ""
echo -e "${BOLD}--- Batch Job ---${NC}"
docker exec cinelog-mysql mysql -ucinelog -pcinelog cinelog -N -e \
    "SELECT CONCAT('  Job: ', bji.JOB_NAME, ' | Status: ', bje.STATUS, ' | Exit: ', bje.EXIT_CODE)
     FROM BOOT3_BATCH_JOB_EXECUTION bje
     JOIN BOOT3_BATCH_JOB_INSTANCE bji ON bje.JOB_INSTANCE_ID = bji.JOB_INSTANCE_ID
     ORDER BY bje.JOB_EXECUTION_ID DESC LIMIT 3;" 2>/dev/null || \
docker exec cinelog-mysql mysql -ucinelog -pcinelog cinelog -N -e \
    "SELECT CONCAT('  Job: ', bji.JOB_NAME, ' | Status: ', bje.STATUS, ' | Exit: ', bje.EXIT_CODE)
     FROM BATCH_JOB_EXECUTION bje
     JOIN BATCH_JOB_INSTANCE bji ON bje.JOB_INSTANCE_ID = bji.JOB_INSTANCE_ID
     ORDER BY bje.JOB_EXECUTION_ID DESC LIMIT 3;" 2>/dev/null || echo "  (tabelas BATCH nao disponiveis)"

echo ""
echo -e "${YELLOW}Container removido automaticamente (--rm)${NC}"
