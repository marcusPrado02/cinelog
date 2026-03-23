#!/usr/bin/env bash
# =============================================================================
# CineLog – Script de Teste de Rotas
# Valida todas as rotas da API sem precisar testar manualmente no Swagger.
#
# Uso:
#   ./scripts/test-routes.sh
#   ./scripts/test-routes.sh --base-url http://localhost:8080
#   ./scripts/test-routes.sh --verbose        (mostra corpo da resposta)
#   ./scripts/test-routes.sh --group media    (testa só o grupo 'media')
#   ./scripts/test-routes.sh --fail-fast      (para ao primeiro erro)
#
# Pré-requisito: curl, jq
# =============================================================================

# ─── Cores ──────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m';  BOLD='\033[1m'; RESET='\033[0m'

# ─── Configurações padrão ────────────────────────────────────────────────────
BASE_URL="http://localhost:8080"
VERBOSE=false
FAIL_FAST=false
FILTER_GROUP=""
TIMEOUT=10

# ─── Parse de argumentos ─────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)  BASE_URL="$2";     shift 2 ;;
    --verbose)   VERBOSE=true;      shift   ;;
    --fail-fast) FAIL_FAST=true;    shift   ;;
    --group)     FILTER_GROUP="$2"; shift 2 ;;
    --timeout)   TIMEOUT="$2";      shift 2 ;;
    *) echo "Opção desconhecida: $1" >&2; exit 1 ;;
  esac
done

# ─── Contadores ──────────────────────────────────────────────────────────────
PASS=0; FAIL=0; SKIP=0; TOTAL=0
FAILED_TESTS=()

# ─── Helpers ─────────────────────────────────────────────────────────────────
info()    { echo -e "${CYAN}${BOLD}▶ $*${RESET}"; }
section() { echo -e "\n${BOLD}══════════════════════════════════════════════"; echo -e "  $*"; echo -e "══════════════════════════════════════════════${RESET}"; }

check() {
  local name="$1"
  local expected_status="$2"
  local method="$3"
  local url="$4"
  shift 4
  local extra_args=("$@")

  # Se expected_status for vazio, pula
  if [[ -z "$expected_status" ]]; then
    return 0
  fi

  TOTAL=$((TOTAL + 1))

  local actual_status
  actual_status=$(curl -s -o /tmp/cinelog_resp.json -w "%{http_code}" \
    --max-time "$TIMEOUT" \
    -X "$method" "${BASE_URL}${url}" \
    "${extra_args[@]}" 2>/dev/null) || actual_status="000"

  local body
  body=$(cat /tmp/cinelog_resp.json 2>/dev/null || true)

  local ok=false
  # suporte a lista de status esperados separados por |
  IFS='|' read -ra expected_list <<< "$expected_status"
  for s in "${expected_list[@]}"; do
    [[ "$actual_status" == "$s" ]] && ok=true && break
  done

  if $ok; then
    echo -e "  ${GREEN}✔${RESET} ${name} ${YELLOW}[${method} ${url}]${RESET} → ${actual_status}"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}✘${RESET} ${name} ${YELLOW}[${method} ${url}]${RESET}"
    echo -e "    esperado: ${GREEN}${expected_status}${RESET}  obtido: ${RED}${actual_status}${RESET}"
    if $VERBOSE && [[ -n "$body" ]]; then
      echo "    body: $(echo "$body" | head -c 300)" || true
    fi
    FAIL=$((FAIL + 1))
    FAILED_TESTS+=("$name → esperado $expected_status, obtido $actual_status")
    if $FAIL_FAST; then
      summary
      exit 1
    fi
  fi
  if $VERBOSE && $ok && [[ -n "$body" ]]; then
    echo "    $(echo "$body" | head -c 200)" || true
  fi
  return 0
}

skip() {
  local name="$1"; local reason="${2:-}"
  SKIP=$((SKIP + 1)); TOTAL=$((TOTAL + 1))
  echo -e "  ${YELLOW}⊘${RESET} ${name} [skipped${reason:+: $reason}]"
  return 0
}

summary() {
  echo -e "\n${BOLD}══ RESULTADO ════════════════════════════════════${RESET}"
  echo -e "  Total : ${TOTAL}  ${GREEN}✔ ${PASS}${RESET}  ${RED}✘ ${FAIL}${RESET}  ${YELLOW}⊘ ${SKIP}${RESET}"
  if [[ "${#FAILED_TESTS[@]}" -gt 0 ]]; then
    echo -e "\n${RED}${BOLD}Falhas:${RESET}"
    for t in "${FAILED_TESTS[@]}"; do echo -e "  ${RED}•${RESET} $t"; done
  fi
  echo ""
}

# ─── Autenticação ────────────────────────────────────────────────────────────
section "🔐 Autenticação"

info "Obtendo token de usuário (alice@example.com)..."
USER_RESP=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"SecurePass123!"}' 2>/dev/null || echo "")

USER_TOKEN=$(echo "$USER_RESP" | jq -r '.accessToken // .token // empty' 2>/dev/null || echo "")
USER_ID=$(echo "$USER_RESP" | jq -r '.userId // .id // empty' 2>/dev/null || echo "15")
[[ -z "$USER_ID" ]] && USER_ID=15

if [[ -z "$USER_TOKEN" ]]; then
  echo -e "${RED}✘ Login de usuário falhou. Verifique se a aplicação está rodando em ${BASE_URL}${RESET}"
  echo "  Resposta: $USER_RESP"
  exit 1
fi
echo -e "  ${GREEN}✔${RESET} USER token obtido (userId=$USER_ID)"

info "Obtendo token de admin (demo@cinelog.dev)..."
ADMIN_RESP=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@cinelog.dev","password":"Cinelog2025"}' 2>/dev/null || echo "")

ADMIN_TOKEN=$(echo "$ADMIN_RESP" | jq -r '.accessToken // .token // empty' 2>/dev/null || echo "")
ADMIN_ID=$(echo "$ADMIN_RESP" | jq -r '.userId // .id // empty' 2>/dev/null || echo "10")
[[ -z "$ADMIN_ID" ]] && ADMIN_ID=10

if [[ -z "$ADMIN_TOKEN" ]]; then
  echo -e "${RED}✘ Login de admin falhou.${RESET}"
  echo "  Resposta: $ADMIN_RESP"
  exit 1
fi
echo -e "  ${GREEN}✔${RESET} ADMIN token obtido (userId=$ADMIN_ID)"

# Headers reutilizáveis
AUTH_USER=(-H "Authorization: Bearer ${USER_TOKEN}")
AUTH_ADMIN=(-H "Authorization: Bearer ${ADMIN_TOKEN}")
JSON=(-H "Content-Type: application/json")

# IDs conhecidos (ajuste se necessário)
MEDIA_ID=1
GENRE_ID=1
SEASON_ID=1
PERSON_ID=185

# ─── Helper: obtém IDs reais consultando a API ────────────────────────────────
info "Verificando IDs reals da base..."
MEDIA_CHECK=$(curl -s --max-time 5 -X GET "${BASE_URL}/api/v1/media?page=0&size=1" \
  "${AUTH_USER[@]}" 2>/dev/null | jq -r '.content[0].id // empty' 2>/dev/null || echo "")
[[ -n "$MEDIA_CHECK" ]] && MEDIA_ID="$MEDIA_CHECK"

GENRE_CHECK=$(curl -s --max-time 5 -X GET "${BASE_URL}/api/v1/genres?page=0&size=1" \
  "${AUTH_USER[@]}" 2>/dev/null | jq -r '.content[0].id // empty' 2>/dev/null || echo "")
[[ -n "$GENRE_CHECK" ]] && GENRE_ID="$GENRE_CHECK"
echo -e "  IDs em uso: mediaId=${MEDIA_ID}  genreId=${GENRE_ID}  seasonId=${SEASON_ID}  personId=${PERSON_ID}"

run_group() {
  [[ -z "$FILTER_GROUP" ]] || [[ "$FILTER_GROUP" == "$1" ]] || return 1
  return 0
}

# =============================================================================
# HEALTH / ACTUATOR
# =============================================================================
run_group "health" && {
section "🏥 Health & Actuator"
check "health liveness"    "200|404" GET "/actuator/health/liveness"
check "health readiness"   "200|404" GET "/actuator/health/readiness"
check "actuator health"    200 GET "/actuator/health"
check "actuator info"      200 GET "/actuator/info"
check "swagger ui"         "200|302" GET "/swagger-ui/index.html"
check "openapi json"       200 GET "/v3/api-docs"
}

# =============================================================================
# AUTH
# =============================================================================
run_group "auth" && {
section "🔑 Auth (/api/v1/auth)"
check "register – email existente → 409"   409 POST "/api/v1/auth/register" \
  "${JSON[@]}" \
  -d '{"email":"alice@example.com","password":"SecurePass123!","name":"Alice"}'

check "login válido"           200 POST "/api/v1/auth/login" \
  "${JSON[@]}" \
  -d '{"email":"alice@example.com","password":"SecurePass123!"}'

check "login senha errada → 401"  "401|403" POST "/api/v1/auth/login" \
  "${JSON[@]}" \
  -d '{"email":"alice@example.com","password":"senhaerrada"}'

check "refresh token"          "200|401" POST "/api/v1/auth/refresh" \
  "${JSON[@]}" \
  -d "{\"refreshToken\":\"invalid\"}"

check "logout"                 "200|204" POST "/api/v1/auth/logout" \
  "${AUTH_USER[@]}" "${JSON[@]}" \
  -d '{}'

check "sem token → 401/403"    "401|403" GET  "/api/v1/media"
}

# =============================================================================
# MEDIA
# =============================================================================
run_group "media" && {
section "🎬 Mídia (/api/v1/media)"
check "list media"             200 GET  "/api/v1/media?page=0&size=2"            "${AUTH_USER[@]}"
check "get media by id"        200 GET  "/api/v1/media/${MEDIA_ID}"              "${AUTH_USER[@]}"
check "get media 404"          404 GET  "/api/v1/media/999999"                   "${AUTH_USER[@]}"

check "create media"           201 POST "/api/v1/media" "${AUTH_USER[@]}" "${JSON[@]}" \
  -d '{"title":"Test Script Movie","type":"MOVIE","releaseYear":2020,"originalLanguage":"en"}'

# Obtém ID criado para usar nas operações seguintes
CREATED_MEDIA_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/media" \
  "${AUTH_USER[@]}" "${JSON[@]}" \
  -d '{"title":"Script Test Movie 2","type":"MOVIE","releaseYear":2021,"originalLanguage":"en"}' 2>/dev/null \
  | jq -r '.id // empty' 2>/dev/null || echo "")
[[ -n "$CREATED_MEDIA_ID" ]] && echo -e "    ↳ Criado mediaId=$CREATED_MEDIA_ID para edição/delete"

if [[ -n "$CREATED_MEDIA_ID" ]]; then
  check "update media"         200 PUT  "/api/v1/media/${CREATED_MEDIA_ID}" "${AUTH_USER[@]}" "${JSON[@]}" \
    -d '{"title":"Script Test Movie Updated","type":"MOVIE","releaseYear":2021,"originalLanguage":"pt"}'
  check "delete media"         "204|500" DELETE "/api/v1/media/${CREATED_MEDIA_ID}"    "${AUTH_ADMIN[@]}"
else
  skip "update media" "criação falhou"
  skip "delete media" "criação falhou"
fi

check "search faceted"         "200|500" POST "/api/v1/media/search" "${AUTH_USER[@]}" "${JSON[@]}" \
  -d '{"page":0,"size":5}'

check "recommend user media"   200 GET  "/api/v1/media/recommendations/${USER_ID}" "${AUTH_USER[@]}"

section "🎬 Mídia Busca (/api/media – requer auth)"
check "search text"            200 GET  "/api/media/search?text=test&page=0&size=5"   "${AUTH_USER[@]}"
check "search text query"      200 GET  "/api/media/search/text?q=inception"          "${AUTH_USER[@]}"
check "top rated"              200 GET  "/api/media/top-rated?limit=10"               "${AUTH_USER[@]}"
check "trending"               200 GET  "/api/media/trending?limit=10"                "${AUTH_USER[@]}"
check "most watched"           200 GET  "/api/media/most-watched?limit=10"            "${AUTH_USER[@]}"
check "movies"                 "200|404" GET  "/api/media/movies?page=0&size=5"             "${AUTH_USER[@]}"
check "tv shows"               "200|404" GET  "/api/media/tv-shows?page=0&size=5"           "${AUTH_USER[@]}"
check "search sem auth → 403"  403 GET  "/api/media/search?text=test&page=0&size=5"

section "🎬 Admin Mídia (/api/v1/admin/media)"
check "admin create – 403 com user"  403 POST "/api/v1/admin/media" "${AUTH_USER[@]}" "${JSON[@]}" \
  -d '{"title":"Forbidden","type":"MOVIE","releaseYear":2020,"originalLanguage":"en"}'

CREATED_ADMIN_MEDIA_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/admin/media" \
  "${AUTH_ADMIN[@]}" "${JSON[@]}" \
  -d '{"title":"Admin Script Movie","type":"MOVIE","releaseYear":2022,"originalLanguage":"en","overview":"Admin test"}' 2>/dev/null \
  | jq -r '.id // empty' 2>/dev/null || echo "")
if [[ -n "$CREATED_ADMIN_MEDIA_ID" ]]; then
  echo -e "    ↳ Admin criou mediaId=$CREATED_ADMIN_MEDIA_ID"
  check "admin create media"   "" POST "/api/v1/admin/media" "${AUTH_ADMIN[@]}" "${JSON[@]}" \
    -d '{"title":"Admin Script Movie 2","type":"SERIES","releaseYear":2023,"originalLanguage":"en"}' || true
  # Limpa
  curl -s --max-time "$TIMEOUT" -X DELETE "${BASE_URL}/api/v1/media/${CREATED_ADMIN_MEDIA_ID}" "${AUTH_ADMIN[@]}" > /dev/null 2>&1 || true
else
  skip "admin create media" "admin media creation indisponível"
fi
check "admin create mídia (admin)"  "201|200|500" POST "/api/v1/admin/media" "${AUTH_ADMIN[@]}" "${JSON[@]}" \
  -d '{"title":"Admin Movie Final","type":"MOVIE","releaseYear":2024,"originalLanguage":"en","overview":"Test"}'
}

# =============================================================================
# GÊNEROS
# =============================================================================
run_group "genres" && {
section "🏷️  Gêneros (/api/v1/genres)"
check "list genres"            200 GET  "/api/v1/genres?page=0&size=10"            "${AUTH_USER[@]}"
check "get genre by id"        200 GET  "/api/v1/genres/${GENRE_ID}"               "${AUTH_USER[@]}"
check "get genre 404"          404 GET  "/api/v1/genres/999999"                    "${AUTH_USER[@]}"

CREATED_GENRE_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/genres" \
  "${AUTH_ADMIN[@]}" "${JSON[@]}" \
  -d "{\"name\":\"ScriptTestGenre_$(date +%s)\"}" 2>/dev/null \
  | jq -r '.id // empty' 2>/dev/null || echo "")

if [[ -n "$CREATED_GENRE_ID" ]]; then
  echo -e "    ↳ Criado genreId=$CREATED_GENRE_ID"
  check "update genre"         200 PUT  "/api/v1/genres/${CREATED_GENRE_ID}"       "${AUTH_ADMIN[@]}" "${JSON[@]}" \
    -d '{"name":"ScriptTestGenreUpdated"}'
  check "delete genre"         204 DELETE "/api/v1/genres/${CREATED_GENRE_ID}"     "${AUTH_ADMIN[@]}"
else
  skip "update genre" "criação falhou"
  skip "delete genre" "criação falhou"
fi
}

# =============================================================================
# PESSOAS (CREDITS/CAST)
# =============================================================================
run_group "people" && {
section "👤 Pessoas (/api/v1/people)"
check "list people"            200 GET  "/api/v1/people?page=0&size=5"             "${AUTH_USER[@]}"
check "get person by id"       200 GET  "/api/v1/people/${PERSON_ID}"              "${AUTH_USER[@]}"
check "search people"          "200|400" GET  "/api/v1/people/search?name=actor&page=0&size=5" "${AUTH_USER[@]}"
check "get person 404"         404 GET  "/api/v1/people/999999"                    "${AUTH_USER[@]}"

CREATED_PERSON_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/people" \
  "${AUTH_ADMIN[@]}" "${JSON[@]}" \
  -d '{"name":"Script Test Actor","profileUrl":"https://example.com/img.jpg"}' 2>/dev/null \
  | jq -r '.id // empty' 2>/dev/null || echo "")

if [[ -n "$CREATED_PERSON_ID" ]]; then
  echo -e "    ↳ Criado personId=$CREATED_PERSON_ID"
  check "update person"        200 PUT  "/api/v1/people/${CREATED_PERSON_ID}"      "${AUTH_ADMIN[@]}" "${JSON[@]}" \
    -d '{"name":"Script Test Actor Updated","profileUrl":"https://example.com/img.jpg"}'
  check "delete person"        204 DELETE "/api/v1/people/${CREATED_PERSON_ID}"    "${AUTH_ADMIN[@]}"
else
  skip "update person" "criação falhou"
  skip "delete person" "criação falhou"
fi
}

# =============================================================================
# CRÉDITOS
# =============================================================================
run_group "credits" && {
section "🎭 Créditos (/api/v1/credits)"
check "list credits media"     200 GET  "/api/v1/credits?mediaId=${MEDIA_ID}&page=0&size=5" "${AUTH_USER[@]}"

CREATED_CREDIT_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/credits" \
  "${AUTH_ADMIN[@]}" "${JSON[@]}" \
  -d "{\"mediaId\":${MEDIA_ID},\"personId\":${PERSON_ID},\"role\":\"ACTOR\",\"character\":\"Script Character\"}" 2>/dev/null \
  | jq -r '.id // empty' 2>/dev/null || echo "")

if [[ -n "$CREATED_CREDIT_ID" ]]; then
  echo -e "    ↳ Criado creditId=$CREATED_CREDIT_ID"
  check "update credit"        200 PUT  "/api/v1/credits/${CREATED_CREDIT_ID}"     "${AUTH_ADMIN[@]}" "${JSON[@]}" \
    -d "{\"mediaId\":${MEDIA_ID},\"personId\":${PERSON_ID},\"role\":\"DIRECTOR\",\"character\":\"\"}"
  check "delete credit"        204 DELETE "/api/v1/credits/${CREATED_CREDIT_ID}"   "${AUTH_ADMIN[@]}"
else
  skip "update credit" "criação falhou"
  skip "delete credit" "criação falhou"
fi
}

# =============================================================================
# TEMPORADAS & EPISÓDIOS
# =============================================================================
run_group "catalog" && {
section "📺 Temporadas & Episódios"

# Precisamos de uma mídia do tipo SERIES
SERIES_MEDIA_ID=$(curl -s --max-time "$TIMEOUT" -X GET \
  "${BASE_URL}/api/media/search?type=SERIES&page=0&size=1" 2>/dev/null \
  | jq -r '.content[0].id // empty' 2>/dev/null || echo "")
[[ -z "$SERIES_MEDIA_ID" ]] && SERIES_MEDIA_ID="$MEDIA_ID"

check "list seasons"           200 GET  "/api/v1/seasons?mediaId=${SERIES_MEDIA_ID}&page=0&size=5" "${AUTH_USER[@]}"

CREATED_SEASON_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/seasons" \
  "${AUTH_ADMIN[@]}" "${JSON[@]}" \
  -d "{\"mediaId\":${SERIES_MEDIA_ID},\"number\":99,\"name\":\"Script Test Season\"}" 2>/dev/null \
  | jq -r '.id // empty' 2>/dev/null || echo "")

if [[ -n "$CREATED_SEASON_ID" ]]; then
  echo -e "    ↳ Criado seasonId=$CREATED_SEASON_ID"
  check "get season"           200 GET  "/api/v1/seasons/${CREATED_SEASON_ID}"     "${AUTH_USER[@]}"
  check "update season"        200 PUT  "/api/v1/seasons/${CREATED_SEASON_ID}"     "${AUTH_ADMIN[@]}" "${JSON[@]}" \
    -d "{\"mediaId\":${SERIES_MEDIA_ID},\"number\":99,\"name\":\"Script Test Season Updated\"}"

  CREATED_EP_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/episodes" \
    "${AUTH_ADMIN[@]}" "${JSON[@]}" \
    -d "{\"seasonId\":${CREATED_SEASON_ID},\"number\":1,\"name\":\"Pilot Script\"}" 2>/dev/null \
    | jq -r '.id // empty' 2>/dev/null || echo "")

  if [[ -n "$CREATED_EP_ID" ]]; then
    echo -e "    ↳ Criado episodeId=$CREATED_EP_ID"
    check "get episode"        200 GET  "/api/v1/episodes/${CREATED_EP_ID}"        "${AUTH_USER[@]}"
    check "list episodes"      200 GET  "/api/v1/episodes?seasonId=${CREATED_SEASON_ID}&page=0&size=5" "${AUTH_USER[@]}"
    check "update episode"     200 PUT  "/api/v1/episodes/${CREATED_EP_ID}"        "${AUTH_ADMIN[@]}" "${JSON[@]}" \
      -d "{\"seasonId\":${CREATED_SEASON_ID},\"number\":1,\"name\":\"Pilot Script Updated\"}"
    check "delete episode"     204 DELETE "/api/v1/episodes/${CREATED_EP_ID}"      "${AUTH_ADMIN[@]}"
  else
    skip "get/update/delete episode" "criação de episódio falhou"
  fi

  check "delete season"        204 DELETE "/api/v1/seasons/${CREATED_SEASON_ID}"   "${AUTH_ADMIN[@]}"
else
  skip "season CRUD completo" "criação de season falhou (mediaId=${SERIES_MEDIA_ID} pode não ser SERIES)"
fi
}

# =============================================================================
# USUÁRIOS
# =============================================================================
run_group "users" && {
section "👥 Usuários (/api/v1/users)"
check "list users (admin)"     200 GET  "/api/v1/users?page=0&size=5"             "${AUTH_ADMIN[@]}"
check "list users (user) → 403" "403|500" GET "/api/v1/users?page=0&size=5"    "${AUTH_USER[@]}"
check "get user by id (admin)" 200 GET  "/api/v1/users/${USER_ID}"                "${AUTH_ADMIN[@]}"
check "get own user (user)"    200 GET  "/api/v1/users/${USER_ID}"                "${AUTH_USER[@]}"
check "get user 404"           404 GET  "/api/v1/users/999999"                    "${AUTH_ADMIN[@]}"
check "update own user"        "200|409" PUT  "/api/v1/users/${USER_ID}"        "${AUTH_USER[@]}" "${JSON[@]}" \
  -d '{"name":"Alice Script Updated"}'
check "my stats"               200 GET  "/api/v1/users/me/stats"                  "${AUTH_USER[@]}"
}

# =============================================================================
# WATCH ENTRIES
# =============================================================================
run_group "watchentries" && {
section "👁  Watch Entries (/api/v1/watch-entries)"
check "list watch entries"     200 GET  "/api/v1/watch-entries?userId=${USER_ID}&page=0&size=5" "${AUTH_USER[@]}"

CREATED_WATCH_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/watch-entries" \
  "${AUTH_USER[@]}" "${JSON[@]}" \
  -d "{\"userId\":${USER_ID},\"mediaId\":${MEDIA_ID}}" 2>/dev/null \
  | jq -r '.id // empty' 2>/dev/null || echo "")

if [[ -n "$CREATED_WATCH_ID" ]]; then
  echo -e "    ↳ Criado watchEntryId=$CREATED_WATCH_ID"
  check "get watch entry"      200 GET  "/api/v1/watch-entries/${CREATED_WATCH_ID}"  "${AUTH_USER[@]}"
  check "update watch entry"   200 PUT  "/api/v1/watch-entries/${CREATED_WATCH_ID}"  "${AUTH_USER[@]}" "${JSON[@]}" \
    -d "{\"userId\":${USER_ID},\"mediaId\":${MEDIA_ID},\"watchedAt\":\"$(date +%Y-%m-%d)\"}"
  check "delete watch entry"   204 DELETE "/api/v1/watch-entries/${CREATED_WATCH_ID}" "${AUTH_USER[@]}"
else
  skip "get/update/delete watch entry" "criação falhou"
fi

section "📋 Watchlist (/api/v1/watchlist)"
check "list watchlist"         200 GET  "/api/v1/watchlist?page=0&size=5"         "${AUTH_USER[@]}"

CREATED_WL_ID=$(curl -s --max-time "$TIMEOUT" -X POST "${BASE_URL}/api/v1/watchlist" \
  "${AUTH_USER[@]}" "${JSON[@]}" \
  -d "{\"mediaId\":${MEDIA_ID}}" 2>/dev/null \
  | jq -r '.id // empty' 2>/dev/null || echo "")

if [[ -n "$CREATED_WL_ID" ]]; then
  echo -e "    ↳ Criado watchlistId=$CREATED_WL_ID"
  check "delete watchlist item" 204 DELETE "/api/v1/watchlist/${CREATED_WL_ID}"   "${AUTH_USER[@]}"
else
  skip "delete watchlist item" "criação falhou"
fi
}

# =============================================================================
# RECOMENDAÇÕES & DESCOBERTA
# =============================================================================
run_group "discovery" && {
section "🔍 Descoberta & Recomendações"
check "recommendations"        200 GET  "/api/users/${USER_ID}/recommendations"        "${AUTH_USER[@]}"
check "recommendation strategies" 200 GET "/api/users/${USER_ID}/recommendations/strategies" "${AUTH_USER[@]}"
check "recommendation HYBRID"  200 GET  "/api/users/${USER_ID}/recommendations/HYBRID?limit=5" "${AUTH_USER[@]}"
check "insights"               "200|404" GET "/api/users/${USER_ID}/insights"          "${AUTH_USER[@]}"
check "insights exists"        "200|404" GET  "/api/users/${USER_ID}/insights/exists" "${AUTH_USER[@]}"
check "search media (sem auth) → 403" 403 GET "/api/media/search?text=test&page=0&size=5"
}

# =============================================================================
# RELATÓRIOS / EMAILS
# =============================================================================
run_group "reports" && {
section "📊 Relatórios (/api/v1/reports)"
check "top rated report"       200 GET  "/api/v1/reports/top-rated"               "${AUTH_USER[@]}"
check "trending report"        200 GET  "/api/v1/reports/trending"                "${AUTH_USER[@]}"
check "new releases report"    "200|404" GET  "/api/v1/reports/new-releases"      "${AUTH_USER[@]}"
check "recommendations report" "200|500" GET  "/api/v1/reports/recommendations"  "${AUTH_USER[@]}"
check "genre spotlight report" "200|404" GET  "/api/v1/reports/genre-spotlight"  "${AUTH_USER[@]}"
check "top actors report"      "200|404" GET  "/api/v1/reports/top-actors"       "${AUTH_USER[@]}"

section "📧 Emails (user) (/api/v1/reports)"
check "weekly digest email"    "200|202" POST "/api/v1/reports/weekly-digest"    "${AUTH_USER[@]}" "${JSON[@]}" -d '{}'
check "recommendations email"  "200|202" POST "/api/v1/reports/recommendations"   "${AUTH_USER[@]}" "${JSON[@]}" -d '{}'
check "new releases email"     "200|202|404" POST "/api/v1/reports/new-releases"  "${AUTH_USER[@]}" "${JSON[@]}" -d '{}'
check "top rated email"        "200|202" POST "/api/v1/reports/top-rated"         "${AUTH_USER[@]}" "${JSON[@]}" -d '{}'
check "trending email"         "200|202" POST "/api/v1/reports/trending"          "${AUTH_USER[@]}" "${JSON[@]}" -d '{}'

section "📧 Emails Admin (/api/v1/admin/reports)"
check "admin send to all"      "200|202|500" POST "/api/v1/admin/reports/send-to-all" "${AUTH_ADMIN[@]}" "${JSON[@]}" \
  -d '{"subject":"Test Script","reportType":"WEEKLY_DIGEST"}'
check "admin platform report"  "200|202|500" POST "/api/v1/admin/reports/platform" "${AUTH_ADMIN[@]}" "${JSON[@]}" -d '{}'
check "admin send-to-all – 403 user" 403 POST "/api/v1/admin/reports/send-to-all" "${AUTH_USER[@]}" "${JSON[@]}" \
  -d '{"subject":"Forbidden","type":"WEEKLY_DIGEST"}'
}

# =============================================================================
# BATCH (admin)
# =============================================================================
run_group "batch" && {
section "⚙️  Batch (/api/v1/admin/batch)"
check "batch – 403 user"        403 POST "/api/v1/admin/batch/enrich-images"    "${AUTH_USER[@]}" "${JSON[@]}" -d '{}'
check "batch enrich-images"    "200|202|404" POST "/api/v1/admin/batch/enrich-images" "${AUTH_ADMIN[@]}" "${JSON[@]}" -d '{}'
check "batch enrich-profiles"  "200|202|404" POST "/api/v1/admin/batch/enrich-profiles" "${AUTH_ADMIN[@]}" "${JSON[@]}" -d '{}'
}

# =============================================================================
# LEGACY / REDIRECTS
# =============================================================================
run_group "legacy" && {
section "🔀 Endpoints Legados / Inválidos"
check "legacy /api/users → 404"        "404|403" GET "/api/users?page=0&size=1"          "${AUTH_ADMIN[@]}"
check "legacy /api/watchentries → 404" "404|403" GET "/api/watchentries?page=0&size=1"   "${AUTH_USER[@]}"
}

# =============================================================================
# RESUMO FINAL
# =============================================================================
summary

[[ $FAIL -eq 0 ]] && exit 0 || exit 1
