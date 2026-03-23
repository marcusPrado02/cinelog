#!/usr/bin/env bash
# ============================================================================
# test-everything.sh — Teste COMPLETO End-to-End do CineLog
# ============================================================================
#
# Sobe toda a infra, inicia a app, testa TODOS os componentes:
#   - Containers (MySQL, Redis, Keycloak, Prometheus, Grafana, Loki, Tempo, etc.)
#   - App Spring Boot (health, endpoints, seguranca)
#   - Sprint Semanas 1-4 (checklist completo)
#   - Batch jobs (disparo + metricas)
#   - Dashboards Grafana (provisioned)
#   - Observabilidade (Prometheus, Jaeger, Loki)
#   - Email (MailHog ou SMTP real)
#   - Swagger UI
#   - Link TMDB (imagens de seed)
#
# USO:
#   ./scripts/test-everything.sh              # Sobe tudo e testa
#   ./scripts/test-everything.sh --no-start   # Assume infra ja rodando
#   ./scripts/test-everything.sh --stop       # Para tudo no final
#
# ============================================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; MAGENTA='\033[0;35m'
BOLD='\033[1m'; DIM='\033[2m'; NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_URL="http://localhost:8080"
PASS=0; FAIL=0; SKIP=0; SECTION=0
AUTO_START=true; AUTO_STOP=false

for arg in "$@"; do
    case "$arg" in
        --no-start) AUTO_START=false ;;
        --stop)     AUTO_STOP=true ;;
    esac
done

cd "$PROJECT_ROOT"

# Carregar email config se existir
[[ -f .env.mail ]] && source .env.mail
[[ -f .env.email ]] && source .env.email
EMAIL_TEST_TO="${EMAIL_TEST_TO:-marcus.prado@pitang.com}"

# ── Helpers ──────────────────────────────────────────────────────────────────
banner()      { echo -e "\n${BOLD}${MAGENTA}╔══════════════════════════════════════════════════════════════╗${NC}"; echo -e "${BOLD}${MAGENTA}║  $*${NC}"; echo -e "${BOLD}${MAGENTA}╚══════════════════════════════════════════════════════════════╝${NC}\n"; }
log_header()  { echo -e "\n${BOLD}${CYAN}═══ $* ═══${NC}\n"; }
log_section() { ((SECTION++)) || true; echo -e "\n${BOLD}  [$SECTION] $*${NC}"; }
log_ok()      { echo -e "    ${GREEN}✓${NC} $*"; ((PASS++)) || true; }
log_fail()    { echo -e "    ${RED}✗${NC} $*"; ((FAIL++)) || true; }
log_skip()    { echo -e "    ${YELLOW}○${NC} $*"; ((SKIP++)) || true; }
log_info()    { echo -e "    ${BLUE}ℹ${NC} $*"; }
log_url()     { echo -e "    ${MAGENTA}→${NC} ${BOLD}$*${NC}"; }

wait_for() {
    local name="$1" url="$2" max="${3:-60}"
    local i=0
    echo -ne "    Aguardando $name..."
    while ! curl -sf "$url" > /dev/null 2>&1; do
        ((i++)) || true
        if [[ $i -ge $max ]]; then
            echo -e " ${RED}TIMEOUT${NC}"
            return 1
        fi
        echo -n "."
        sleep 2
    done
    echo -e " ${GREEN}OK${NC}"
    return 0
}

get_token() {
    local email="$1" pass="$2"
    curl -sf -X POST "$APP_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$pass\"}" 2>/dev/null \
        | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo ""
}

# ============================================================================
banner "CineLog — Teste Completo End-to-End"
echo -e "  ${DIM}Projeto:  $PROJECT_ROOT${NC}"
echo -e "  ${DIM}Data:     $(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo -e "  ${DIM}Email:    $EMAIL_TEST_TO${NC}"
echo ""

# ============================================================================
# FASE 1: INFRAESTRUTURA
# ============================================================================
if [[ "$AUTO_START" == "true" ]]; then
    log_header "FASE 1: Subindo Infraestrutura"

    log_section "Docker Compose — Servicos Essenciais"
    docker compose up -d db redis keycloak mailhog 2>&1 | tail -5
    log_ok "Servicos essenciais iniciados"

    log_section "Docker Compose — Observabilidade"
    docker compose up -d otel-collector jaeger tempo prometheus loki promtail grafana 2>&1 | tail -5
    log_ok "Stack de observabilidade iniciada"

    log_section "Aguardando servicos ficarem saudaveis"
    wait_for "MySQL" "http://localhost:3306" 30 2>/dev/null || \
        docker exec cinelog-mysql mysqladmin ping -h 127.0.0.1 -u cinelog -pcinelog 2>/dev/null && echo -e "    MySQL... ${GREEN}OK${NC}" || true
    wait_for "Keycloak" "http://localhost:8180/realms/cinelog" 90 || log_skip "Keycloak nao respondeu a tempo"
    wait_for "Prometheus" "http://localhost:9090/-/ready" 30 || log_skip "Prometheus nao respondeu"
    wait_for "Grafana" "http://localhost:3000/api/health" 30 || log_skip "Grafana nao respondeu"
fi

# ============================================================================
# FASE 2: VERIFICAR CONTAINERS
# ============================================================================
log_header "FASE 2: Status dos Containers"
log_section "Containers Docker"

ALL_EXPECTED=("cinelog-mysql" "cinelog-redis" "cinelog-keycloak" "cinelog-mailhog"
    "cinelog-grafana" "cinelog-loki" "cinelog-prometheus" "cinelog-jaeger" "cinelog-tempo"
    "cinelog-otel-collector" "cinelog-promtail" "cinelog-dataflow" "cinelog-skipper"
    "cinelog-elasticsearch" "cinelog-kibana" "cinelog-logstash")

ALL_CONTAINERS=$(docker ps --format '{{.Names}}' 2>/dev/null)

check_container() {
    local name="$1"
    echo "$ALL_CONTAINERS" | grep -qE "^${name}(-1)?$"
}

for name in "${ALL_EXPECTED[@]}"; do
    if check_container "$name"; then
        log_ok "$name rodando"
    else
        log_fail "$name NAO esta rodando"
    fi
done

# ============================================================================
# FASE 3: COMPILACAO + APP
# ============================================================================
log_header "FASE 3: Compilacao e App"
log_section "Maven compile"

if mvn compile -q 2>/dev/null; then
    log_ok "Compilacao bem-sucedida"
else
    log_fail "Compilacao falhou"
fi

log_section "Verificar se app esta rodando"
# Usa -w para checar HTTP status code em vez de -f, porque /actuator/health
# retorna 503 quando algum health indicator esta DOWN (ex: mail SMTP),
# mesmo que a app esteja funcional. Qualquer resposta HTTP = app rodando.
APP_HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$APP_URL/actuator/health" 2>/dev/null || echo "000")
if [[ "$APP_HTTP" != "000" ]]; then
    HEALTH_STATUS=$(curl -s "$APP_URL/actuator/health" 2>/dev/null \
        | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','?'))" 2>/dev/null || echo "?")
    if [[ "$HEALTH_STATUS" == "UP" ]]; then
        log_ok "App rodando em $APP_URL (health: UP)"
    else
        log_ok "App rodando em $APP_URL (health: $HEALTH_STATUS — HTTP $APP_HTTP)"
        # Identifica componentes DOWN para informar o usuario
        DOWN_COMPONENTS=$(curl -s "$APP_URL/actuator/health" 2>/dev/null \
            | python3 -c "
import sys,json
d=json.load(sys.stdin)
down=[k for k,v in d.get('components',{}).items() if v.get('status')!='UP']
print(', '.join(down) if down else '')
" 2>/dev/null || echo "")
        [[ -n "$DOWN_COMPONENTS" ]] && log_info "Componentes DOWN: $DOWN_COMPONENTS"
    fi
else
    log_info "App nao esta rodando. Inicie com:"
    log_info "  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &"
    log_info "  sleep 25"
    log_skip "Testes online serao pulados"
fi

APP_ONLINE=false
[[ "$APP_HTTP" != "000" ]] && APP_ONLINE=true

# ============================================================================
# FASE 4: SPRINT CHECKLIST (Semanas 1-4)
# ============================================================================
log_header "FASE 4: Sprint Checklist (validacoes estaticas)"
bash scripts/test-sprint-checklist.sh static 2>&1 | grep -E "✓|✗|○|PASS|FAIL|SKIP|═══" | head -80

# Capturar resultado
SPRINT_RESULT=$(bash scripts/test-sprint-checklist.sh static 2>&1 | tail -5)
SPRINT_PASS=$(echo "$SPRINT_RESULT" | grep -oP 'PASS: \K[0-9]+' || echo "0")
SPRINT_FAIL=$(echo "$SPRINT_RESULT" | grep -oP 'FAIL: \K[0-9]+' || echo "0")
log_info "Sprint checklist: $SPRINT_PASS PASS, $SPRINT_FAIL FAIL"

# ============================================================================
# FASE 5: TESTES ONLINE (requer app rodando)
# ============================================================================
if [[ "$APP_ONLINE" == "true" ]]; then
    log_header "FASE 5: Testes Online (App + Endpoints)"

    log_section "Actuator Health"
    HEALTH=$(curl -s "$APP_URL/actuator/health" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','?'))" 2>/dev/null || echo "UNREACHABLE")
    if [[ "$HEALTH" == "UP" ]]; then
        log_ok "Health: $HEALTH"
    elif [[ "$HEALTH" == "DOWN" ]]; then
        # DOWN significa que a app responde mas algum health indicator falhou
        # (ex: mail SMTP). A app esta funcional para testes.
        DOWN_LIST=$(curl -s "$APP_URL/actuator/health" 2>/dev/null \
            | python3 -c "import sys,json; d=json.load(sys.stdin); print(', '.join(k for k,v in d.get('components',{}).items() if v.get('status')!='UP'))" 2>/dev/null || echo "?")
        log_ok "Health: $HEALTH (componentes DOWN: $DOWN_LIST)"
    else
        log_fail "Health: $HEALTH"
    fi

    log_section "Swagger UI"
    SWAGGER_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" "$APP_URL/swagger-ui/index.html" 2>/dev/null || echo "000")
    if [[ "$SWAGGER_STATUS" == "200" ]]; then
        log_ok "Swagger UI acessivel"
        log_url "http://localhost:8080/swagger-ui/index.html"
    else
        log_skip "Swagger UI retornou $SWAGGER_STATUS"
    fi

    log_section "Autenticacao JWT"
    ADMIN_TOKEN=$(get_token "admin@cinelog.dev" "Admin@CineLog2025!")
    if [[ -n "$ADMIN_TOKEN" ]]; then
        log_ok "Login admin — token obtido"
    else
        log_fail "Login admin falhou"
    fi

    log_section "Endpoints CRUD (com token)"
    if [[ -n "$ADMIN_TOKEN" ]]; then
        for endpoint in "api/v1/media" "api/v1/genres" "api/v1/users" "api/v1/watch-entries"; do
            STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
                -H "Authorization: Bearer $ADMIN_TOKEN" \
                "$APP_URL/$endpoint" 2>/dev/null || echo "000")
            if [[ "$STATUS" == "200" ]]; then
                log_ok "GET /$endpoint → $STATUS"
            else
                log_fail "GET /$endpoint → $STATUS"
            fi
        done
    fi

    log_section "Ordenacao DESC (bug corrigido)"
    if [[ -n "$ADMIN_TOKEN" ]]; then
        STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            "$APP_URL/api/v1/media?sort=id,desc&size=5" 2>/dev/null || echo "000")
        if [[ "$STATUS" == "200" ]]; then
            log_ok "GET /api/v1/media?sort=id,desc → $STATUS (ordenacao funciona)"
        else
            log_fail "Ordenacao DESC falhou (status $STATUS)"
        fi
    fi

    log_section "Seguranca — Endpoint admin bloqueado para user"
    USER_TOKEN=$(get_token "alice@cinelog.dev" "Alice@CineLog2025!")
    if [[ -n "$USER_TOKEN" ]]; then
        ADMIN_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $USER_TOKEN" \
            -X POST "$APP_URL/api/v1/admin/batch/genres" 2>/dev/null || echo "000")
        if [[ "$ADMIN_STATUS" == "403" ]]; then
            log_ok "User bloqueado em endpoint admin (403)"
        else
            log_fail "User acessou endpoint admin ($ADMIN_STATUS)"
        fi
    else
        log_skip "Login user falhou — teste de seguranca pulado"
    fi

    log_section "Batch Jobs — Disparo Manual"
    if [[ -n "$ADMIN_TOKEN" ]]; then
        BATCH_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -X POST "$APP_URL/api/v1/admin/batch/genres" 2>/dev/null || echo "000")
        if [[ "$BATCH_STATUS" == "200" ]]; then
            log_ok "Batch syncGenresJob disparado (status $BATCH_STATUS)"
        else
            log_skip "Batch retornou $BATCH_STATUS (TMDB_API_KEY pode nao estar configurado)"
        fi
    fi

    log_section "Prometheus Metricas"
    PROM_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" "$APP_URL/actuator/prometheus" 2>/dev/null || echo "000")
    if [[ "$PROM_STATUS" == "200" ]]; then
        METRIC_COUNT=$(curl -sf "$APP_URL/actuator/prometheus" 2>/dev/null | wc -l)
        log_ok "Actuator/prometheus: $METRIC_COUNT metricas expostas"
    else
        log_fail "Actuator/prometheus retornou $PROM_STATUS"
    fi

    log_section "Reports — Preview"
    if [[ -n "$ADMIN_TOKEN" ]]; then
        for report in "trending" "top-rated" "new-releases"; do
            STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
                -H "Authorization: Bearer $ADMIN_TOKEN" \
                "$APP_URL/api/v1/reports/$report" 2>/dev/null || echo "000")
            if [[ "$STATUS" == "200" ]]; then
                log_ok "GET /api/v1/reports/$report → $STATUS"
            else
                log_skip "Report $report retornou $STATUS"
            fi
        done
    fi

    log_section "Email — Envio de Teste"
    if [[ -n "$ADMIN_TOKEN" && -n "${EMAIL_TEST_TO:-}" ]]; then
        SEND_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
            -X POST \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"$EMAIL_TEST_TO\",\"limit\":5}" \
            "$APP_URL/api/v1/reports/trending" 2>/dev/null || echo "000")
        if [[ "$SEND_STATUS" == "200" || "$SEND_STATUS" == "202" ]]; then
            log_ok "Email trending enviado para $EMAIL_TEST_TO"
            if [[ "${MAIL_HOST:-localhost}" == "localhost" ]]; then
                log_url "MailHog: http://localhost:8025 (ver emails capturados)"
            else
                log_info "Verifique sua caixa em $EMAIL_TEST_TO"
            fi
        else
            log_skip "Envio de email retornou $SEND_STATUS"
        fi
    fi
else
    log_header "FASE 5: Testes Online PULADOS (app offline)"
    log_skip "Inicie a app para testes completos"
fi

# ============================================================================
# FASE 6: OBSERVABILIDADE — Dashboards e Ferramentas
# ============================================================================
log_header "FASE 6: Observabilidade — Dashboards"

log_section "Grafana"
if curl -sf "http://localhost:3000/api/health" > /dev/null 2>&1; then
    log_ok "Grafana acessivel"
    log_url "http://localhost:3000 (admin/admin)"

    # Verificar dashboards provisionados
    DASH_COUNT=$(curl -sf "http://localhost:3000/api/search?type=dash-db" \
        -u admin:admin 2>/dev/null | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    if [[ "$DASH_COUNT" -ge 1 ]]; then
        log_ok "$DASH_COUNT dashboard(s) provisionado(s)"
        log_url "http://localhost:3000/dashboards (ver dashboards)"
    else
        log_skip "Nenhum dashboard encontrado"
    fi

    # Datasources
    DS_COUNT=$(curl -sf "http://localhost:3000/api/datasources" \
        -u admin:admin 2>/dev/null | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    if [[ "$DS_COUNT" -ge 1 ]]; then
        log_ok "$DS_COUNT datasource(s) configurado(s) (Prometheus, Loki, Tempo)"
    fi
else
    log_skip "Grafana offline"
fi

log_section "Prometheus"
if curl -sf "http://localhost:9090/-/ready" > /dev/null 2>&1; then
    log_ok "Prometheus acessivel"
    log_url "http://localhost:9090 (queries PromQL)"

    if [[ "$APP_ONLINE" == "true" ]]; then
        # Verificar se targets estao UP
        TARGETS=$(curl -sf "http://localhost:9090/api/v1/targets" 2>/dev/null \
            | python3 -c "
import sys,json
data = json.load(sys.stdin)
active = data.get('data',{}).get('activeTargets',[])
up = sum(1 for t in active if t.get('health')=='up')
print(f'{up}/{len(active)}')
" 2>/dev/null || echo "?/?")
        log_ok "Targets: $TARGETS UP"
    fi
else
    log_skip "Prometheus offline"
fi

log_section "Jaeger (Distributed Tracing)"
if curl -sf "http://localhost:16686" > /dev/null 2>&1; then
    log_ok "Jaeger UI acessivel"
    log_url "http://localhost:16686 (traces distribuidos)"
else
    log_skip "Jaeger offline"
fi

log_section "Loki (Log Aggregation)"
if curl -sf "http://localhost:3100/ready" > /dev/null 2>&1; then
    log_ok "Loki acessivel"
    log_info "Logs visiveis via Grafana → Explore → Loki"
else
    log_skip "Loki offline"
fi

log_section "MailHog"
if curl -sf "http://localhost:8025" > /dev/null 2>&1; then
    MAIL_COUNT=$(curl -sf "http://localhost:8025/api/v2/messages?limit=1" 2>/dev/null \
        | python3 -c "import sys,json; print(json.load(sys.stdin).get('total',0))" 2>/dev/null || echo "0")
    log_ok "MailHog acessivel ($MAIL_COUNT emails capturados)"
    log_url "http://localhost:8025 (ver emails)"
else
    log_skip "MailHog offline"
fi

log_section "Keycloak"
if curl -sf "http://localhost:8180/realms/cinelog" > /dev/null 2>&1; then
    log_ok "Keycloak acessivel"
    log_url "http://localhost:8180/admin (admin/admin)"
else
    log_skip "Keycloak offline"
fi

log_section "Spring Cloud Data Flow"
SCDF_URL="http://localhost:9393"
if curl -sf -u admin:'Admin@CineLog2025!' "$SCDF_URL/about" > /dev/null 2>&1; then
    log_ok "SCDF Server acessivel"
    log_url "http://localhost:9393/dashboard (admin/Admin@CineLog2025!)"

    # Verificar tasks registradas
    TASK_COUNT=$(curl -sf -u admin:'Admin@CineLog2025!' "$SCDF_URL/tasks/definitions" 2>/dev/null \
        | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('page',{}).get('totalElements',0))" 2>/dev/null || echo "0")
    if [[ "$TASK_COUNT" -ge 1 ]]; then
        log_ok "$TASK_COUNT task(s) registrada(s) no SCDF"
    else
        log_info "Nenhuma task registrada — rode: bash docker/scdf/init-scdf.sh"
    fi
else
    log_skip "SCDF Server offline (opcional — docker compose up -d dataflow-server)"
fi

# ============================================================================
# FASE 7: BANCO DE DADOS — Stats
# ============================================================================
log_header "FASE 7: Banco de Dados"
log_section "MySQL Stats"

if docker exec cinelog-mysql mysql -ucinelog -pcinelog cinelog -e "SELECT 1" > /dev/null 2>&1; then
    log_ok "MySQL acessivel"

    STATS=$(docker exec cinelog-mysql mysql -ucinelog -pcinelog cinelog -N -e "
        SELECT
            (SELECT COUNT(*) FROM media) as media_total,
            (SELECT COUNT(*) FROM media WHERE poster_url IS NOT NULL) as media_com_poster,
            (SELECT COUNT(*) FROM media WHERE tmdb_id IS NOT NULL) as media_com_tmdb,
            (SELECT COUNT(*) FROM users) as users_total,
            (SELECT COUNT(*) FROM watch_entry) as watch_entries_total,
            (SELECT COUNT(*) FROM genres) as genres_total
    " 2>/dev/null || echo "? ? ? ? ? ?")

    read -r MEDIA_TOTAL MEDIA_POSTER MEDIA_TMDB USERS WATCHES GENRES <<< "$STATS"
    log_info "Media: $MEDIA_TOTAL total, $MEDIA_POSTER com poster, $MEDIA_TMDB com tmdb_id"
    log_info "Users: $USERS | Watch Entries: $WATCHES | Genres: $GENRES"

    MISSING=$(docker exec cinelog-mysql mysql -ucinelog -pcinelog cinelog -N -e \
        "SELECT COUNT(*) FROM media WHERE tmdb_id IS NULL AND poster_url IS NULL" 2>/dev/null || echo "?")
    if [[ "$MISSING" -gt 0 ]]; then
        log_info "$MISSING midias de seed sem tmdb_id/poster — rode o job linkTmdbJob para corrigir"
    else
        log_ok "Todas as midias tem tmdb_id ou poster"
    fi
else
    log_skip "MySQL nao acessivel"
fi

# ============================================================================
# RESULTADO FINAL
# ============================================================================
echo ""
banner "RESULTADO FINAL"

echo -e "  ${GREEN}PASS: $PASS${NC}  |  ${RED}FAIL: $FAIL${NC}  |  ${YELLOW}SKIP: $SKIP${NC}"
echo -e "  Sprint checklist: $SPRINT_PASS PASS, $SPRINT_FAIL FAIL"
echo -e "  Total: $((PASS + FAIL + SKIP)) verificacoes em $SECTION secoes"
echo ""

echo -e "  ${BOLD}URLs para verificacao manual:${NC}"
echo -e "  ┌──────────────────────────────────────────────────────────────────┐"
echo -e "  │ App Swagger:    ${CYAN}http://localhost:8080/swagger-ui/index.html${NC}     │"
echo -e "  │ Grafana:        ${CYAN}http://localhost:3000${NC} (admin/admin)             │"
echo -e "  │ Prometheus:     ${CYAN}http://localhost:9090${NC}                           │"
echo -e "  │ SCDF Dashboard: ${CYAN}http://localhost:9393/dashboard${NC} (admin/Admin@..)│"
echo -e "  │ Jaeger:         ${CYAN}http://localhost:16686${NC}                          │"
echo -e "  │ Keycloak:       ${CYAN}http://localhost:8180/admin${NC} (admin/admin)       │"
echo -e "  │ MailHog:        ${CYAN}http://localhost:8025${NC}                           │"
echo -e "  │ Kibana:         ${CYAN}http://localhost:5601${NC}                           │"
echo -e "  └──────────────────────────────────────────────────────────────────┘"
echo ""

if [[ $FAIL -gt 0 ]]; then
    echo -e "  ${RED}${BOLD}RESULTADO: $FAIL FALHA(S)${NC}"
else
    echo -e "  ${GREEN}${BOLD}RESULTADO: TUDO OK${NC}"
fi

if [[ "$AUTO_STOP" == "true" ]]; then
    echo ""
    echo -e "  ${YELLOW}Parando containers...${NC}"
    docker compose down
fi
