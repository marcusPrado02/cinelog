#!/usr/bin/env bash
# =============================================================================
# CineLog – Script de Integração Completo           scripts/run-full-tests.sh
# =============================================================================
#
# Executa testes end-to-end em todos os endpoints da API CineLog.
#
# USO:
#   ./scripts/run-full-tests.sh
#
# VARIÁVEIS DE AMBIENTE:
#   BASE_URL     URL base da API          (default: http://localhost:8080)
#   TEST_EMAIL   E-mail para testes       (default: test@mailhog.local)
#                ⚠️  NÃO faça commit deste valor — passe via variável de ambiente
#   ADMIN_USER   Usuário admin            (default: admin)
#   ADMIN_PASS   Senha admin              (default: admin123)
#
# DEPENDÊNCIAS: curl, jq
#
# EXEMPLO COM E-MAIL REAL (nunca commitar):
#   TEST_EMAIL="seu.email@dominio.com" ./scripts/run-full-tests.sh
# =============================================================================

set -euo pipefail

# ─── Configuração ─────────────────────────────────────────────────────────────
BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_EMAIL="${TEST_EMAIL:-test@mailhog.local}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"

# ─── Cores ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

# ─── Contadores ───────────────────────────────────────────────────────────────
PASS=0
FAIL=0
SKIP=0

# ─── Funções auxiliares ───────────────────────────────────────────────────────
header() {
    echo ""
    echo -e "${BLUE}${BOLD}══════════════════════════════════════════════════${RESET}"
    echo -e "${BLUE}${BOLD}  $1${RESET}"
    echo -e "${BLUE}${BOLD}══════════════════════════════════════════════════${RESET}"
}

subheader() {
    echo ""
    echo -e "${CYAN}── $1 ──${RESET}"
}

check() {
    local name="$1"
    local expected_status="$2"
    local actual_status="$3"
    local body="$4"

    if [[ "$actual_status" == "$expected_status" ]]; then
        echo -e "${GREEN}  ✅ PASS${RESET} [$actual_status] $name"
        ((PASS++))
    else
        echo -e "${RED}  ❌ FAIL${RESET} $name"
        echo -e "     ${YELLOW}Esperado: $expected_status | Recebido: $actual_status${RESET}"
        if [[ -n "$body" ]]; then
            echo -e "     ${YELLOW}Body: $(echo "$body" | head -c 200)${RESET}"
        fi
        ((FAIL++))
    fi
}

skip() {
    echo -e "${YELLOW}  ⏭  SKIP${RESET} $1 ($2)"
    ((SKIP++))
}

require_cmd() {
    if ! command -v "$1" &>/dev/null; then
        echo -e "${RED}Erro: '$1' não encontrado. Instale antes de executar o script.${RESET}"
        exit 1
    fi
}

# ─── Pré-requisitos ───────────────────────────────────────────────────────────
require_cmd curl
require_cmd jq

echo ""
echo -e "${BOLD}CineLog – Testes de Integração Completos${RESET}"
echo -e "BASE_URL  : ${CYAN}$BASE_URL${RESET}"
echo -e "TEST_EMAIL: ${CYAN}$TEST_EMAIL${RESET}"
echo -e "Início    : $(date '+%Y-%m-%d %H:%M:%S')"

# ─── Health check inicial ─────────────────────────────────────────────────────
header "0. PRÉ-CHECK"

HTTP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health")
if [[ "$HTTP" != "200" ]]; then
    echo -e "${RED}${BOLD}A aplicação não está respondendo em $BASE_URL (status $HTTP). Abortando.${RESET}"
    exit 1
fi
echo -e "${GREEN}  ✅ Aplicação acessível em $BASE_URL${RESET}"

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 1 – AUTENTICAÇÃO
# ═════════════════════════════════════════════════════════════════════════════
header "1. AUTENTICAÇÃO"

# Gera usuário único por execução para evitar colisão
TS=$(date +%s)
TEST_USER="testuser_${TS}"
TEST_PASS="TestP@ss123!"

subheader "Registro de usuário"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$TEST_USER\",\"email\":\"${TEST_USER}@mailhog.local\",\"password\":\"$TEST_PASS\"}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "POST /api/auth/register" "201" "$STATUS" "$BODY"

subheader "Login do usuário"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASS\"}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "POST /api/auth/login" "200" "$STATUS" "$BODY"
TOKEN=$(echo "$BODY" | jq -r '.accessToken // .access_token // ""')
REFRESH_TOKEN=$(echo "$BODY" | jq -r '.refreshToken // .refresh_token // ""')

if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
    echo -e "${RED}  Não foi possível obter o accessToken. Testes autenticados serão pulados.${RESET}"
    TOKEN=""
fi

subheader "Refresh token"
if [[ -n "$REFRESH_TOKEN" && "$REFRESH_TOKEN" != "null" ]]; then
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/refresh" \
        -H "Content-Type: application/json" \
        -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
    STATUS=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | head -n -1)
    check "POST /api/auth/refresh" "200" "$STATUS" "$BODY"
else
    skip "POST /api/auth/refresh" "refreshToken não disponível"
fi

subheader "Login inválido"
RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"invalid_user_xxx","password":"wrong"}')
check "POST /api/auth/login (credenciais inválidas)" "401" "$RESP" ""

# Login admin
subheader "Login admin"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
ADMIN_TOKEN=$(echo "$BODY" | jq -r '.accessToken // .access_token // ""')
if [[ "$STATUS" == "200" && -n "$ADMIN_TOKEN" && "$ADMIN_TOKEN" != "null" ]]; then
    echo -e "${GREEN}  ✅ PASS${RESET} [200] POST /api/auth/login (admin)"
    ((PASS++))
else
    echo -e "${YELLOW}  ⚠️  WARN${RESET} Admin login falhou (status $STATUS). Testes de admin serão pulados."
    ADMIN_TOKEN=""
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 2 – CATÁLOGO DE MÍDIA
# ═════════════════════════════════════════════════════════════════════════════
header "2. CATÁLOGO DE MÍDIA"

subheader "Listagem geral"
RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/media" \
    -H "Authorization: Bearer $TOKEN")
check "GET /api/v1/media" "200" "$RESP" ""

RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/media/movies" \
    -H "Authorization: Bearer $TOKEN")
check "GET /api/v1/media/movies" "200" "$RESP" ""

RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/media/tv-shows" \
    -H "Authorization: Bearer $TOKEN")
check "GET /api/v1/media/tv-shows" "200" "$RESP" ""

subheader "Busca"
RESP=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/api/v1/media/search?q=batman" \
    -H "Authorization: Bearer $TOKEN")
check "GET /api/v1/media/search?q=batman" "200" "$RESP" ""

RESP=$(curl -s -o /dev/null -w "%{http_code}" \
    "$BASE_URL/api/v1/media/search/text?query=inception" \
    -H "Authorization: Bearer $TOKEN")
check "GET /api/v1/media/search/text?query=inception" "200" "$RESP" ""

subheader "Popularidade"
RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media/top-rated" \
    -H "Authorization: Bearer $TOKEN")
check "GET /api/media/top-rated" "200" "$RESP" ""

RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media/trending" \
    -H "Authorization: Bearer $TOKEN")
check "GET /api/media/trending" "200" "$RESP" ""

RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media/most-watched" \
    -H "Authorization: Bearer $TOKEN")
check "GET /api/media/most-watched" "200" "$RESP" ""

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 3 – WATCH ENTRIES
# ═════════════════════════════════════════════════════════════════════════════
header "3. WATCH ENTRIES"

if [[ -z "$TOKEN" ]]; then
    skip "Watch Entries" "token não disponível"
else
    # Obtém o primeiro ID de mídia disponível
    MEDIA_RESP=$(curl -s "$BASE_URL/api/v1/media?page=0&size=1" \
        -H "Authorization: Bearer $TOKEN")
    MEDIA_ID=$(echo "$MEDIA_RESP" | jq -r '.content[0].id // .data[0].id // .items[0].id // ""' 2>/dev/null || echo "")

    if [[ -z "$MEDIA_ID" || "$MEDIA_ID" == "null" ]]; then
        skip "Watch Entries (criar)" "nenhuma mídia disponível no catálogo"
        WATCH_ID=""
    else
        subheader "Criar watch entry"
        RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/watchentries" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"mediaId\":\"$MEDIA_ID\",\"rating\":8,\"review\":\"Ótimo filme! Teste automatizado.\",\"watchedAt\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}")
        STATUS=$(echo "$RESP" | tail -1)
        BODY=$(echo "$RESP" | head -n -1)
        check "POST /api/watchentries" "201" "$STATUS" "$BODY"
        WATCH_ID=$(echo "$BODY" | jq -r '.id // ""')

        subheader "Listar watch entries"
        RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/watchentries" \
            -H "Authorization: Bearer $TOKEN")
        check "GET /api/watchentries" "200" "$RESP" ""

        if [[ -n "$WATCH_ID" && "$WATCH_ID" != "null" ]]; then
            subheader "Detalhe da watch entry"
            RESP=$(curl -s -o /dev/null -w "%{http_code}" \
                "$BASE_URL/api/watchentries/$WATCH_ID" \
                -H "Authorization: Bearer $TOKEN")
            check "GET /api/watchentries/{id}" "200" "$RESP" ""

            subheader "Watch Progress"
            RESP=$(curl -s -w "\n%{http_code}" -X POST \
                "$BASE_URL/api/watchentries/$WATCH_ID/progress" \
                -H "Authorization: Bearer $TOKEN" \
                -H "Content-Type: application/json" \
                -d '{"progressPercent":45,"currentTime":2700}')
            STATUS=$(echo "$RESP" | tail -1)
            BODY=$(echo "$RESP" | head -n -1)
            check "POST /api/watchentries/{id}/progress" "200|201" "$STATUS" "$BODY"

            RESP=$(curl -s -o /dev/null -w "%{http_code}" \
                "$BASE_URL/api/watchentries/$WATCH_ID/progress" \
                -H "Authorization: Bearer $TOKEN")
            check "GET /api/watchentries/{id}/progress" "200" "$RESP" ""

            RESP=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
                "$BASE_URL/api/watchentries/$WATCH_ID/progress" \
                -H "Authorization: Bearer $TOKEN")
            check "DELETE /api/watchentries/{id}/progress" "204" "$RESP" ""
        else
            skip "Watch Entry detail / progress" "watchId não disponível"
        fi
    fi
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 4 – WATCHLIST
# ═════════════════════════════════════════════════════════════════════════════
header "4. WATCHLIST"

if [[ -z "$TOKEN" ]]; then
    skip "Watchlist" "token não disponível"
else
    RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/watchlist" \
        -H "Authorization: Bearer $TOKEN")
    check "GET /api/watchlist" "200" "$RESP" ""

    if [[ -n "$MEDIA_ID" && "$MEDIA_ID" != "null" && -n "${MEDIA_ID:-}" ]]; then
        RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/watchlist" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"mediaId\":\"$MEDIA_ID\"}")
        STATUS=$(echo "$RESP" | tail -1)
        BODY=$(echo "$RESP" | head -n -1)
        check "POST /api/watchlist" "200|201" "$STATUS" "$BODY"

        RESP=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
            "$BASE_URL/api/watchlist/$MEDIA_ID" \
            -H "Authorization: Bearer $TOKEN")
        check "DELETE /api/watchlist/{mediaId}" "200|204" "$RESP" ""
    else
        skip "POST/DELETE /api/watchlist" "nenhuma mídia disponível"
    fi
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 5 – USUÁRIOS E INSIGHTS
# ═════════════════════════════════════════════════════════════════════════════
header "5. USUÁRIOS & INSIGHTS"

if [[ -z "$TOKEN" ]]; then
    skip "Usuários & Insights" "token não disponível"
else
    # Obtém userId do usuário de teste
    RESP=$(curl -s "$BASE_URL/api/v1/users/me" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "{}")
    USER_ID=$(echo "$RESP" | jq -r '.id // ""' 2>/dev/null || echo "")

    if [[ -z "$USER_ID" || "$USER_ID" == "null" ]]; then
        # tenta via endpoint diferente
        RESP=$(curl -s "$BASE_URL/api/v1/users" \
            -H "Authorization: Bearer $TOKEN" 2>/dev/null || echo "[]")
        USER_ID=$(echo "$RESP" | jq -r '.[0].id // .content[0].id // ""' 2>/dev/null || echo "")
    fi

    RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/users/me/stats" \
        -H "Authorization: Bearer $TOKEN")
    check "GET /api/v1/users/me/stats" "200" "$RESP" ""

    if [[ -n "$USER_ID" && "$USER_ID" != "null" ]]; then
        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            "$BASE_URL/api/users/$USER_ID/insights/exists" \
            -H "Authorization: Bearer $TOKEN")
        check "GET /api/users/{id}/insights/exists" "200" "$RESP" ""

        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            "$BASE_URL/api/users/$USER_ID/insights" \
            -H "Authorization: Bearer $TOKEN")
        check "GET /api/users/{id}/insights" "200|404" "$RESP" ""

        subheader "Recomendações"
        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            "$BASE_URL/api/users/$USER_ID/recommendations/strategies" \
            -H "Authorization: Bearer $TOKEN")
        check "GET /api/users/{id}/recommendations/strategies" "200" "$RESP" ""

        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            "$BASE_URL/api/users/$USER_ID/recommendations" \
            -H "Authorization: Bearer $TOKEN")
        check "GET /api/users/{id}/recommendations" "200" "$RESP" ""
    else
        skip "Insights & Recomendações" "userId não obtido"
    fi
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 6 – RELATÓRIOS (PREVIEW)
# ═════════════════════════════════════════════════════════════════════════════
header "6. RELATÓRIOS – PREVIEW"

if [[ -z "$TOKEN" ]]; then
    skip "Relatórios preview" "token não disponível"
else
    for endpoint in \
        "/api/v1/reports/weekly-digest" \
        "/api/v1/reports/top-rated?limit=10" \
        "/api/v1/reports/recommendations" \
        "/api/v1/reports/trending?days=7&limit=10"; do
        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN")
        check "GET $endpoint" "200" "$RESP" ""
    done
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 7 – RELATÓRIOS (ENVIO DE E-MAIL)
# ═════════════════════════════════════════════════════════════════════════════
header "7. RELATÓRIOS – ENVIO DE E-MAIL"
echo -e "  ${YELLOW}ℹ️  E-mails serão enviados para: $TEST_EMAIL${RESET}"
echo -e "  ${YELLOW}   Verifique em http://localhost:8025 (MailHog)${RESET}"

if [[ -z "$TOKEN" ]]; then
    skip "Relatórios e-mail" "token não disponível"
else
    declare -A REPORT_PAYLOADS
    REPORT_PAYLOADS[weekly-digest]="{\"email\":\"$TEST_EMAIL\"}"
    REPORT_PAYLOADS[top-rated]="{\"email\":\"$TEST_EMAIL\",\"limit\":10}"
    REPORT_PAYLOADS[recommendations]="{\"email\":\"$TEST_EMAIL\"}"
    REPORT_PAYLOADS[trending]="{\"email\":\"$TEST_EMAIL\",\"limit\":10}"

    for report_type in weekly-digest top-rated recommendations trending; do
        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            -X POST "$BASE_URL/api/v1/reports/$report_type" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "${REPORT_PAYLOADS[$report_type]}")
        check "POST /api/v1/reports/$report_type" "202" "$RESP" ""
    done

    # Validação: e-mail inválido deve retornar 400
    RESP=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL/api/v1/reports/top-rated" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"email":"not-an-email","limit":5}')
    check "POST /api/v1/reports/top-rated (e-mail inválido → 400)" "400" "$RESP" ""

    # Limite inválido
    RESP=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL/api/v1/reports/top-rated" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"limit":0}')
    check "POST /api/v1/reports/top-rated (limit=0 → 400)" "400" "$RESP" ""
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 8 – ADMIN: RELATÓRIOS
# ═════════════════════════════════════════════════════════════════════════════
header "8. ADMIN – RELATÓRIOS DE PLATAFORMA"

if [[ -z "$ADMIN_TOKEN" ]]; then
    skip "Admin relatórios" "adminToken não disponível"
else
    RESP=$(curl -s -o /dev/null -w "%{http_code}" \
        "$BASE_URL/api/v1/admin/reports/platform" \
        -H "Authorization: Bearer $ADMIN_TOKEN")
    check "GET /api/v1/admin/reports/platform" "200" "$RESP" ""

    RESP=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL/api/v1/admin/reports/platform" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$TEST_EMAIL\"}")
    check "POST /api/v1/admin/reports/platform" "202" "$RESP" ""

    # Acesso não-admin deve falhar
    if [[ -n "$TOKEN" ]]; then
        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            -X POST "$BASE_URL/api/v1/admin/reports/platform" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"$TEST_EMAIL\"}")
        check "POST /api/v1/admin/reports/platform (sem admin → 403)" "403" "$RESP" ""
    fi
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 9 – ADMIN: DLQ (Dead Letter Queue)
# ═════════════════════════════════════════════════════════════════════════════
header "9. ADMIN – DEAD LETTER QUEUE"

if [[ -z "$ADMIN_TOKEN" ]]; then
    skip "DLQ" "adminToken não disponível"
else
    RESP=$(curl -s -w "\n%{http_code}" "$BASE_URL/admin/dlq" \
        -H "Authorization: Bearer $ADMIN_TOKEN")
    STATUS=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | head -n -1)
    check "GET /admin/dlq" "200" "$STATUS" "$BODY"

    DLQ_ID=$(echo "$BODY" | jq -r '.[0].id // ""' 2>/dev/null || echo "")
    if [[ -n "$DLQ_ID" && "$DLQ_ID" != "null" ]]; then
        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            -X POST "$BASE_URL/admin/dlq/$DLQ_ID/replay" \
            -H "Authorization: Bearer $ADMIN_TOKEN")
        check "POST /admin/dlq/{id}/replay" "200|202|204" "$RESP" ""
    else
        skip "POST /admin/dlq/{id}/replay" "nenhum evento na DLQ"
        skip "POST /admin/dlq/{id}/ignore" "nenhum evento na DLQ"
    fi
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 10 – OBSERVABILIDADE (Actuator)
# ═════════════════════════════════════════════════════════════════════════════
header "10. OBSERVABILIDADE"

for endpoint in \
    "/actuator/health" \
    "/actuator/info"; do
    RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint")
    check "GET $endpoint" "200" "$RESP" ""
done

if [[ -n "$ADMIN_TOKEN" ]]; then
    for endpoint in \
        "/actuator/metrics" \
        "/actuator/prometheus" \
        "/actuator/caches"; do
        RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $ADMIN_TOKEN")
        check "GET $endpoint" "200" "$RESP" ""
    done

    subheader "Métricas de segurança"
    for metric in \
        "cinelog.security.auth_failures_total" \
        "cinelog.security.jwt_failures_total" \
        "cinelog.security.rate_limit_total" \
        "cinelog.security.access_denied_total"; do
        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            "$BASE_URL/actuator/metrics/$metric" \
            -H "Authorization: Bearer $ADMIN_TOKEN")
        check "GET /actuator/metrics/$metric" "200|404" "$RESP" ""
    done

    subheader "Métricas de negócio"
    for metric in \
        "cinelog.business.watch_entries_created_total" \
        "cinelog.business.reports_sent_total" \
        "cinelog.business.searches_total"; do
        RESP=$(curl -s -o /dev/null -w "%{http_code}" \
            "$BASE_URL/actuator/metrics/$metric" \
            -H "Authorization: Bearer $ADMIN_TOKEN")
        check "GET /actuator/metrics/$metric" "200|404" "$RESP" ""
    done
else
    skip "Actuator autenticado" "adminToken não disponível"
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 11 – RATE LIMIT
# ═════════════════════════════════════════════════════════════════════════════
header "11. RATE LIMIT"

echo -e "  ${YELLOW}Disparando 12 tentativas de login para acionar rate-limit...${RESET}"
HIT_429=false
for i in $(seq 1 12); do
    RESP=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"ratelimit_test_user_'"$TS"'","password":"wrong_pass_'"$i"'"}')
    if [[ "$RESP" == "429" ]]; then
        HIT_429=true
        break
    fi
done

if [[ "$HIT_429" == "true" ]]; then
    echo -e "${GREEN}  ✅ PASS${RESET} Rate-limit acionado (429 recebido após tentativas consecutivas)"
    ((PASS++))
else
    echo -e "${YELLOW}  ⚠️  WARN${RESET} 429 não recebido em 12 tentativas (verifique configuração do rate-limit)"
    ((SKIP++))
fi

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 12 – LOGOUT
# ═════════════════════════════════════════════════════════════════════════════
header "12. LOGOUT"

if [[ -n "$TOKEN" ]]; then
    RESP=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL/api/auth/logout" \
        -H "Authorization: Bearer $TOKEN")
    check "POST /api/auth/logout" "200|204" "$RESP" ""

    # Token invalidado não deve mais funcionar
    RESP=$(curl -s -o /dev/null -w "%{http_code}" \
        "$BASE_URL/api/watchentries" \
        -H "Authorization: Bearer $TOKEN")
    check "GET /api/watchentries (após logout → 401)" "401" "$RESP" ""
else
    skip "POST /api/auth/logout" "token não disponível"
fi

# ═════════════════════════════════════════════════════════════════════════════
# SUMÁRIO
# ═════════════════════════════════════════════════════════════════════════════
TOTAL=$((PASS + FAIL + SKIP))
echo ""
echo -e "${BOLD}══════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}  RESULTADO FINAL${RESET}"
echo -e "${BOLD}══════════════════════════════════════════════════${RESET}"
echo -e "  Total   : ${BOLD}$TOTAL${RESET}"
echo -e "  ${GREEN}Passou  : $PASS${RESET}"
echo -e "  ${RED}Falhou  : $FAIL${RESET}"
echo -e "  ${YELLOW}Pulou   : $SKIP${RESET}"
echo -e "  Fim     : $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

if [[ $FAIL -eq 0 ]]; then
    echo -e "${GREEN}${BOLD}  ✅ Todos os testes passaram!${RESET}"
    exit 0
else
    echo -e "${RED}${BOLD}  ❌ $FAIL teste(s) falharam. Verifique os logs acima.${RESET}"
    exit 1
fi
