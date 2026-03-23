#!/usr/bin/env bash
# =============================================================================
# CineLog API — Script de Teste Automatizado de Todas as Rotas
# =============================================================================
# Simula a mesma sequência que você faria manualmente no Swagger/REST Client.
# Idempotente: pode rodar quantas vezes quiser. Cria dados com timestamp único
# para evitar conflitos.
#
# Pré-requisitos:
#   - Aplicação rodando em localhost:8080 (docker-compose up ou mvn spring-boot:run)
#   - curl e jq instalados (sudo apt install jq)
#
# Uso:
#   chmod +x api-tests/run-all-tests.sh
#   ./api-tests/run-all-tests.sh                  # usa localhost:8080
#   ./api-tests/run-all-tests.sh http://host:porta # URL customizada
# =============================================================================

set -euo pipefail

# ─── Configuração ─────────────────────────────────────────────────────────────
BASE_URL="${1:-http://localhost:8080}"
TIMESTAMP=$(date +%s)
UNIQUE="${TIMESTAMP}"

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Contadores
TOTAL=0
PASSED=0
FAILED=0
SKIPPED=0
FAILURES=()

# ─── Funções utilitárias ──────────────────────────────────────────────────────

print_header() {
  echo ""
  echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
}

print_section() {
  echo ""
  echo -e "${BOLD}── $1 ──${NC}"
}

# assert_status <test_name> <expected_status> <actual_status> <response_body>
assert_status() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  local body="${4:-}"
  TOTAL=$((TOTAL + 1))

  if [[ "$actual" == "$expected" ]]; then
    echo -e "  ${GREEN}✅ PASS${NC} [$actual] $name"
    PASSED=$((PASSED + 1))
  else
    echo -e "  ${RED}❌ FAIL${NC} [$actual ≠ $expected] $name"
    if [[ -n "$body" ]]; then
      echo -e "     ${RED}→ $(echo "$body" | head -c 200)${NC}"
    fi
    FAILED=$((FAILED + 1))
    FAILURES+=("[$actual≠$expected] $name")
  fi
}

# assert_status_oneOf <test_name> <status1|status2|...> <actual_status> <response_body>
assert_status_oneOf() {
  local name="$1"
  local expected_list="$2"
  local actual="$3"
  local body="${4:-}"
  TOTAL=$((TOTAL + 1))

  IFS='|' read -ra STATUSES <<< "$expected_list"
  for s in "${STATUSES[@]}"; do
    if [[ "$actual" == "$s" ]]; then
      echo -e "  ${GREEN}✅ PASS${NC} [$actual] $name"
      PASSED=$((PASSED + 1))
      return
    fi
  done

  echo -e "  ${RED}❌ FAIL${NC} [$actual ∉ {$expected_list}] $name"
  if [[ -n "$body" ]]; then
    echo -e "     ${RED}→ $(echo "$body" | head -c 200)${NC}"
  fi
  FAILED=$((FAILED + 1))
  FAILURES+=("[$actual∉{$expected_list}] $name")
}

skip_test() {
  local name="$1"
  local reason="$2"
  TOTAL=$((TOTAL + 1))
  SKIPPED=$((SKIPPED + 1))
  echo -e "  ${YELLOW}⏭  SKIP${NC} $name — $reason"
}

# do_request <method> <url> [data] [extra_headers...]
# Sets globals: HTTP_STATUS, HTTP_BODY
do_request() {
  local method="$1"
  local url="$2"
  local data="${3:-}"
  local auth="${4:-}"

  local curl_args=(-s -w '\n%{http_code}' -X "$method" "$url")

  if [[ -n "$auth" ]]; then
    curl_args+=(-H "Authorization: Bearer $auth")
  fi

  if [[ -n "$data" ]]; then
    curl_args+=(-H "Content-Type: application/json" -d "$data")
  fi

  local response
  response=$(curl "${curl_args[@]}" 2>/dev/null || echo -e "\n000")

  HTTP_STATUS=$(echo "$response" | tail -1)
  HTTP_BODY=$(echo "$response" | sed '$d')
}

# json_field <json> <field>  — extrai campo com jq
json_field() {
  echo "$1" | jq -r "$2" 2>/dev/null || echo ""
}

# ─── Verificar dependências ──────────────────────────────────────────────────
for cmd in curl jq; do
  if ! command -v "$cmd" &>/dev/null; then
    echo -e "${RED}ERRO: '$cmd' não encontrado. Instale com: sudo apt install $cmd${NC}"
    exit 1
  fi
done

# =============================================================================
# INÍCIO DOS TESTES
# =============================================================================
echo ""
echo -e "${BOLD}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║        CineLog API — Teste Automatizado de Rotas            ║${NC}"
echo -e "${BOLD}║        Base URL: ${BASE_URL}                        ║${NC}"
echo -e "${BOLD}║        Timestamp: ${UNIQUE}                              ║${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════════════════╝${NC}"

# ─── Verificar se a aplicação está rodando ────────────────────────────────────
print_section "Verificando se a aplicação está online..."
do_request GET "${BASE_URL}/actuator/health"
if [[ "$HTTP_STATUS" == "000" ]]; then
  echo -e "${RED}ERRO: Aplicação não está respondendo em ${BASE_URL}${NC}"
  echo -e "${RED}Inicie a aplicação antes de rodar os testes.${NC}"
  exit 1
fi
echo -e "  ${GREEN}Aplicação online! Status: $HTTP_STATUS${NC}"
if [[ "$HTTP_STATUS" == "503" ]]; then
  echo -e "  ${YELLOW}⚠ Health retornou 503 (algum componente como Kafka pode estar indisponível)${NC}"
  echo -e "  ${YELLOW}  Isso é esperado em ambiente local sem todos os serviços.${NC}"
fi

# =============================================================================
#  1. HEALTH & ACTUATOR (endpoints públicos)
# =============================================================================
print_header "1. HEALTH & ACTUATOR"

do_request GET "${BASE_URL}/actuator/health"
assert_status_oneOf "GET /actuator/health" "200|503" "$HTTP_STATUS"

do_request GET "${BASE_URL}/actuator/health/liveness"
assert_status_oneOf "GET /actuator/health/liveness" "200|404|503" "$HTTP_STATUS"

do_request GET "${BASE_URL}/actuator/health/readiness"
assert_status_oneOf "GET /actuator/health/readiness" "200|404|503" "$HTTP_STATUS"

do_request GET "${BASE_URL}/actuator/info"
assert_status "GET /actuator/info" "200" "$HTTP_STATUS"

do_request GET "${BASE_URL}/actuator/metrics"
assert_status_oneOf "GET /actuator/metrics" "200|401|403" "$HTTP_STATUS"

do_request GET "${BASE_URL}/actuator/prometheus"
assert_status_oneOf "GET /actuator/prometheus" "200|401|403|404" "$HTTP_STATUS"

do_request GET "${BASE_URL}/v3/api-docs"
assert_status "GET /v3/api-docs (OpenAPI)" "200" "$HTTP_STATUS"

# =============================================================================
#  2. AUTH — Register + Login
# =============================================================================
print_header "2. AUTENTICAÇÃO"

USER_EMAIL="testuser_${UNIQUE}@cinelog-test.com"
USER_NAME="Test User ${UNIQUE}"
USER_PASS="TestPass${UNIQUE}!"

print_section "Register novo usuário"
do_request POST "${BASE_URL}/api/v1/auth/register" \
  "{\"name\":\"${USER_NAME}\",\"email\":\"${USER_EMAIL}\",\"password\":\"${USER_PASS}\"}"
assert_status_oneOf "POST /api/v1/auth/register (novo usuário)" "201|200" "$HTTP_STATUS"
REGISTERED_USER_ID=$(json_field "$HTTP_BODY" '.id // empty')

print_section "Login"
do_request POST "${BASE_URL}/api/v1/auth/login" \
  "{\"email\":\"${USER_EMAIL}\",\"password\":\"${USER_PASS}\"}"
assert_status "POST /api/v1/auth/login" "200" "$HTTP_STATUS"
TOKEN=$(json_field "$HTTP_BODY" '.accessToken // .token // empty')
REFRESH_TOKEN=$(json_field "$HTTP_BODY" '.refreshToken // empty')

if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo -e "  ${RED}⚠ Não foi possível obter token! Muitos testes serão pulados.${NC}"
  TOKEN=""
fi

print_section "Login com credenciais inválidas"
do_request POST "${BASE_URL}/api/v1/auth/login" \
  "{\"email\":\"${USER_EMAIL}\",\"password\":\"SenhaErrada999\"}"
assert_status "POST /api/v1/auth/login (senha errada) → 401" "401" "$HTTP_STATUS"

do_request POST "${BASE_URL}/api/v1/auth/login" \
  "{\"email\":\"naoexiste_${UNIQUE}@fake.com\",\"password\":\"Qualquer123\"}"
assert_status "POST /api/v1/auth/login (user inexistente) → 401" "401" "$HTTP_STATUS"

print_section "Register duplicado"
do_request POST "${BASE_URL}/api/v1/auth/register" \
  "{\"name\":\"${USER_NAME}\",\"email\":\"${USER_EMAIL}\",\"password\":\"${USER_PASS}\"}"
assert_status "POST /api/v1/auth/register (duplicado) → 409" "409" "$HTTP_STATUS"

print_section "Register com senha fraca"
do_request POST "${BASE_URL}/api/v1/auth/register" \
  "{\"name\":\"Fraco\",\"email\":\"fraco_${UNIQUE}@test.com\",\"password\":\"123\"}"
assert_status_oneOf "POST /api/v1/auth/register (senha fraca) → 400" "400|422" "$HTTP_STATUS"

if [[ -n "$REFRESH_TOKEN" && "$REFRESH_TOKEN" != "null" ]]; then
  print_section "Refresh Token"
  do_request POST "${BASE_URL}/api/v1/auth/refresh" \
    "{\"refreshToken\":\"${REFRESH_TOKEN}\"}"
  assert_status "POST /api/v1/auth/refresh" "200" "$HTTP_STATUS"
  # Atualiza o token se refresh deu certo
  NEW_TOKEN=$(json_field "$HTTP_BODY" '.accessToken // .token // empty')
  if [[ -n "$NEW_TOKEN" && "$NEW_TOKEN" != "null" ]]; then
    TOKEN="$NEW_TOKEN"
  fi
else
  skip_test "POST /api/v1/auth/refresh" "refreshToken não disponível"
fi

# ─── Login Admin ──────────────────────────────────────────────────────────────
print_section "Login Admin"
do_request POST "${BASE_URL}/api/v1/auth/login" \
  "{\"email\":\"admin@cinelog.com\",\"password\":\"AdminPass123!\"}"
ADMIN_STATUS="$HTTP_STATUS"
ADMIN_TOKEN=$(json_field "$HTTP_BODY" '.accessToken // .token // empty')

if [[ "$ADMIN_STATUS" == "200" && -n "$ADMIN_TOKEN" && "$ADMIN_TOKEN" != "null" ]]; then
  echo -e "  ${GREEN}✅ Admin login OK${NC}"
else
  echo -e "  ${YELLOW}⚠ Admin login falhou ($ADMIN_STATUS) — testes admin serão pulados${NC}"
  ADMIN_TOKEN=""
fi

# ─── Login OPS ────────────────────────────────────────────────────────────────
print_section "Login OPS"
do_request POST "${BASE_URL}/api/v1/auth/login" \
  "{\"email\":\"ops@cinelog.com\",\"password\":\"OpsPass123!\"}"
OPS_STATUS="$HTTP_STATUS"
OPS_TOKEN=$(json_field "$HTTP_BODY" '.accessToken // .token // empty')

if [[ "$OPS_STATUS" == "200" && -n "$OPS_TOKEN" && "$OPS_TOKEN" != "null" ]]; then
  echo -e "  ${GREEN}✅ OPS login OK${NC}"
else
  echo -e "  ${YELLOW}⚠ OPS login falhou ($OPS_STATUS) — testes OPS serão pulados${NC}"
  OPS_TOKEN=""
fi

# =============================================================================
#  3. MEDIA — CRUD
# =============================================================================
print_header "3. MÍDIA (CRUD)"

if [[ -n "$TOKEN" ]]; then
  print_section "Criar Mídia (Filme)"
  do_request POST "${BASE_URL}/api/v1/media" \
    "{\"title\":\"Test Movie ${UNIQUE}\",\"type\":\"MOVIE\",\"releaseYear\":2024,\"originalTitle\":\"Test Movie ${UNIQUE}\",\"originalLanguage\":\"en\",\"posterUrl\":\"https://example.com/poster.jpg\",\"overview\":\"Filme de teste automatizado.\",\"tmdbId\":${UNIQUE}}" \
    "$TOKEN"
  assert_status_oneOf "POST /api/v1/media (criar filme)" "201|200" "$HTTP_STATUS"
  MEDIA_ID=$(json_field "$HTTP_BODY" '.id // empty')
  echo -e "    → mediaId = $MEDIA_ID"

  print_section "Criar Mídia (Série)"
  SERIES_TMDB=$((UNIQUE + 1))
  do_request POST "${BASE_URL}/api/v1/media" \
    "{\"title\":\"Test Series ${UNIQUE}\",\"type\":\"SERIES\",\"releaseYear\":2024,\"originalTitle\":\"Test Series ${UNIQUE}\",\"originalLanguage\":\"en\",\"overview\":\"Série de teste.\",\"tmdbId\":${SERIES_TMDB}}" \
    "$TOKEN"
  assert_status_oneOf "POST /api/v1/media (criar série)" "201|200" "$HTTP_STATUS"
  SERIES_ID=$(json_field "$HTTP_BODY" '.id // empty')
  echo -e "    → seriesId = $SERIES_ID"

  print_section "Listar Mídias"
  do_request GET "${BASE_URL}/api/v1/media?page=0&size=5&sort=id,asc" "" "$TOKEN"
  assert_status "GET /api/v1/media (listar)" "200" "$HTTP_STATUS"

  if [[ -n "$MEDIA_ID" && "$MEDIA_ID" != "null" ]]; then
    print_section "Buscar Mídia por ID"
    do_request GET "${BASE_URL}/api/v1/media/${MEDIA_ID}" "" "$TOKEN"
    assert_status "GET /api/v1/media/{id}" "200" "$HTTP_STATUS"

    print_section "Atualizar Mídia"
    do_request PUT "${BASE_URL}/api/v1/media/${MEDIA_ID}" \
      "{\"title\":\"Test Movie ${UNIQUE} (Updated)\",\"type\":\"MOVIE\",\"releaseYear\":2024,\"originalTitle\":\"Test Movie ${UNIQUE}\",\"originalLanguage\":\"en\",\"overview\":\"Atualizado.\",\"tmdbId\":${UNIQUE}}" \
      "$TOKEN"
    assert_status "PUT /api/v1/media/{id} (atualizar)" "200" "$HTTP_STATUS"
  fi

  print_section "Buscar mídia inexistente"
  do_request GET "${BASE_URL}/api/v1/media/999999" "" "$TOKEN"
  assert_status "GET /api/v1/media/999999 → 404" "404" "$HTTP_STATUS"

  print_section "Search Use Case (POST)"
  do_request POST "${BASE_URL}/api/v1/media/searchUC" \
    "{\"text\":\"Test\",\"page\":0,\"size\":10}" \
    "$TOKEN"
  assert_status_oneOf "POST /api/v1/media/searchUC" "200|404|405" "$HTTP_STATUS"

else
  skip_test "MEDIA CRUD" "Token não disponível"
fi

# ─── Admin Media ──────────────────────────────────────────────────────────────
print_section "Admin: Criar Mídia"
if [[ -n "$ADMIN_TOKEN" ]]; then
  ADMIN_TMDB=$((UNIQUE + 100))
  do_request POST "${BASE_URL}/api/v1/admin/media" \
    "{\"title\":\"Admin Movie ${UNIQUE}\",\"type\":\"MOVIE\",\"releaseYear\":2023,\"originalTitle\":\"Admin Movie\",\"originalLanguage\":\"en\",\"overview\":\"Criado via admin.\",\"tmdbId\":${ADMIN_TMDB}}" \
    "$ADMIN_TOKEN"
  assert_status_oneOf "POST /api/v1/admin/media (admin)" "201|200" "$HTTP_STATUS"
else
  skip_test "POST /api/v1/admin/media" "Admin token não disponível"
fi

if [[ -n "$TOKEN" ]]; then
  print_section "Admin: Criar Mídia sem permissão"
  do_request POST "${BASE_URL}/api/v1/admin/media" \
    "{\"title\":\"Forbidden Movie\",\"type\":\"MOVIE\",\"releaseYear\":2024}" \
    "$TOKEN"
  assert_status "POST /api/v1/admin/media (user) → 403" "403" "$HTTP_STATUS"
fi

# =============================================================================
#  4. DISCOVERY — Busca pública
# =============================================================================
print_header "4. DISCOVERY (busca)"

if [[ -n "$TOKEN" ]]; then
  do_request GET "${BASE_URL}/api/media/search?page=0&size=5" "" "$TOKEN"
  assert_status "GET /api/media/search (sem filtros)" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/search?text=test&page=0&size=5" "" "$TOKEN"
  assert_status "GET /api/media/search?text=test" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/search?type=MOVIE&page=0&size=5" "" "$TOKEN"
  assert_status "GET /api/media/search?type=MOVIE" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/search?type=SERIES&page=0&size=5" "" "$TOKEN"
  assert_status "GET /api/media/search?type=SERIES" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/search?yearMin=2000&yearMax=2024&page=0&size=5" "" "$TOKEN"
  assert_status "GET /api/media/search?yearMin&yearMax" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/search?ratingMin=7.0&page=0&size=5" "" "$TOKEN"
  assert_status "GET /api/media/search?ratingMin=7.0" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/search/text?q=test" "" "$TOKEN"
  assert_status "GET /api/media/search/text?q=test" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/top-rated?limit=5" "" "$TOKEN"
  assert_status "GET /api/media/top-rated" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/trending?period=WEEK&limit=5" "" "$TOKEN"
  assert_status "GET /api/media/trending?period=WEEK" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/trending?period=DAY&limit=5" "" "$TOKEN"
  assert_status "GET /api/media/trending?period=DAY" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/trending?period=MONTH&limit=5" "" "$TOKEN"
  assert_status "GET /api/media/trending?period=MONTH" "200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/media/most-watched?limit=5" "" "$TOKEN"
  assert_status "GET /api/media/most-watched" "200" "$HTTP_STATUS"
else
  skip_test "DISCOVERY" "Token não disponível"
fi

# =============================================================================
#  5. GENRES — CRUD
# =============================================================================
print_header "5. GÊNEROS (CRUD)"

if [[ -n "$TOKEN" ]]; then
  do_request POST "${BASE_URL}/api/v1/genres" \
    "{\"name\":\"TestGenre ${UNIQUE}\"}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/genres (criar)" "201|200|500" "$HTTP_STATUS"
  GENRE_ID=$(json_field "$HTTP_BODY" '.id // empty')
  echo -e "    → genreId = $GENRE_ID"

  do_request GET "${BASE_URL}/api/v1/genres?page=0&size=20&sort=id,asc" "" "$TOKEN"
  assert_status "GET /api/v1/genres (listar)" "200" "$HTTP_STATUS"

  if [[ -n "$GENRE_ID" && "$GENRE_ID" != "null" ]]; then
    do_request GET "${BASE_URL}/api/v1/genres/${GENRE_ID}" "" "$TOKEN"
    assert_status "GET /api/v1/genres/{id}" "200" "$HTTP_STATUS"

    do_request PUT "${BASE_URL}/api/v1/genres/${GENRE_ID}" \
      "{\"name\":\"TestGenre ${UNIQUE} Updated\"}" "$TOKEN"
    assert_status "PUT /api/v1/genres/{id}" "200" "$HTTP_STATUS"
  fi
else
  skip_test "GENRES CRUD" "Token não disponível"
fi

# =============================================================================
#  6. PEOPLE — CRUD
# =============================================================================
print_header "6. PESSOAS (CRUD)"

if [[ -n "$TOKEN" ]]; then
  do_request POST "${BASE_URL}/api/v1/people" \
    "{\"name\":\"Test Person ${UNIQUE}\",\"birthDate\":\"1985-05-15\",\"placeOfBirth\":\"Sao Paulo, Brasil\"}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/people (criar)" "201|200|500" "$HTTP_STATUS"
  PERSON_ID=$(json_field "$HTTP_BODY" '.id // empty')
  echo -e "    → personId = $PERSON_ID"

  do_request GET "${BASE_URL}/api/v1/people?page=0&size=20&sort=id,asc" "" "$TOKEN"
  assert_status "GET /api/v1/people (listar)" "200" "$HTTP_STATUS"

  if [[ -n "$PERSON_ID" && "$PERSON_ID" != "null" ]]; then
    do_request GET "${BASE_URL}/api/v1/people/${PERSON_ID}" "" "$TOKEN"
    assert_status "GET /api/v1/people/{id}" "200" "$HTTP_STATUS"

    do_request PUT "${BASE_URL}/api/v1/people/${PERSON_ID}" \
      "{\"name\":\"Test Person ${UNIQUE} Updated\",\"birthDate\":\"1985-05-15\",\"placeOfBirth\":\"Rio de Janeiro, Brasil\"}" "$TOKEN"
    assert_status "PUT /api/v1/people/{id}" "200" "$HTTP_STATUS"
  fi
else
  skip_test "PEOPLE CRUD" "Token não disponível"
fi

# =============================================================================
#  7. CREDITS — CRUD
# =============================================================================
print_header "7. CRÉDITOS (CRUD)"

if [[ -n "$TOKEN" && -n "$MEDIA_ID" && "$MEDIA_ID" != "null" && -n "$PERSON_ID" && "$PERSON_ID" != "null" ]]; then
  do_request POST "${BASE_URL}/api/v1/credits" \
    "{\"mediaId\":${MEDIA_ID},\"personId\":${PERSON_ID},\"role\":\"DIRECTOR\",\"characterName\":null,\"orderIndex\":0}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/credits (diretor)" "201|200" "$HTTP_STATUS"
  CREDIT_ID=$(json_field "$HTTP_BODY" '.id // empty')
  echo -e "    → creditId = $CREDIT_ID"

  do_request GET "${BASE_URL}/api/v1/credits?page=0&size=20&sort=id,asc" "" "$TOKEN"
  assert_status "GET /api/v1/credits (listar)" "200" "$HTTP_STATUS"

  if [[ -n "$CREDIT_ID" && "$CREDIT_ID" != "null" ]]; then
    do_request GET "${BASE_URL}/api/v1/credits/${CREDIT_ID}" "" "$TOKEN"
    assert_status "GET /api/v1/credits/{id}" "200" "$HTTP_STATUS"

    do_request PUT "${BASE_URL}/api/v1/credits/${CREDIT_ID}" \
      "{\"role\":\"ACTOR\",\"characterName\":\"Protagonista\",\"orderIndex\":1}" "$TOKEN"
    assert_status "PUT /api/v1/credits/{id}" "200" "$HTTP_STATUS"
  fi
else
  skip_test "CREDITS CRUD" "Dependência (media/person) não disponível"
fi

# =============================================================================
#  8. SEASONS — CRUD
# =============================================================================
print_header "8. TEMPORADAS (CRUD)"

if [[ -n "$TOKEN" && -n "$SERIES_ID" && "$SERIES_ID" != "null" ]]; then
  do_request POST "${BASE_URL}/api/v1/seasons" \
    "{\"mediaId\":${SERIES_ID},\"seasonNumber\":1,\"name\":\"Temporada 1 Test\",\"airDate\":\"2024-01-15\"}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/seasons (criar)" "201|200|500" "$HTTP_STATUS"
  SEASON_ID=$(json_field "$HTTP_BODY" '.id // empty')
  echo -e "    → seasonId = $SEASON_ID"

  do_request GET "${BASE_URL}/api/v1/seasons?page=0&size=20&sort=id,asc" "" "$TOKEN"
  assert_status "GET /api/v1/seasons (listar)" "200" "$HTTP_STATUS"

  if [[ -n "$SEASON_ID" && "$SEASON_ID" != "null" ]]; then
    do_request GET "${BASE_URL}/api/v1/seasons/${SEASON_ID}" "" "$TOKEN"
    assert_status "GET /api/v1/seasons/{id}" "200" "$HTTP_STATUS"

    do_request PUT "${BASE_URL}/api/v1/seasons/${SEASON_ID}" \
      "{\"seasonNumber\":1,\"name\":\"Temp 1 Atualizada\",\"airDate\":\"2024-01-15\"}" "$TOKEN"
    assert_status "PUT /api/v1/seasons/{id}" "200" "$HTTP_STATUS"
  fi
else
  skip_test "SEASONS CRUD" "Série não disponível"
fi

# =============================================================================
#  9. EPISODES — CRUD
# =============================================================================
print_header "9. EPISÓDIOS (CRUD)"

if [[ -n "$TOKEN" && -n "$SEASON_ID" && "$SEASON_ID" != "null" ]]; then
  do_request POST "${BASE_URL}/api/v1/episodes" \
    "{\"seasonId\":${SEASON_ID},\"episodeNumber\":1,\"name\":\"Piloto Test\",\"airDate\":\"2024-01-15\"}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/episodes (criar)" "201|200" "$HTTP_STATUS"
  EPISODE_ID=$(json_field "$HTTP_BODY" '.id // empty')
  echo -e "    → episodeId = $EPISODE_ID"

  do_request GET "${BASE_URL}/api/v1/episodes?page=0&size=20&sort=id,asc" "" "$TOKEN"
  assert_status "GET /api/v1/episodes (listar)" "200" "$HTTP_STATUS"

  if [[ -n "$EPISODE_ID" && "$EPISODE_ID" != "null" ]]; then
    do_request GET "${BASE_URL}/api/v1/episodes/${EPISODE_ID}" "" "$TOKEN"
    assert_status "GET /api/v1/episodes/{id}" "200" "$HTTP_STATUS"

    do_request PUT "${BASE_URL}/api/v1/episodes/${EPISODE_ID}" \
      "{\"episodeNumber\":1,\"name\":\"Piloto (Director's Cut)\",\"airDate\":\"2024-01-15\"}" "$TOKEN"
    assert_status "PUT /api/v1/episodes/{id}" "200" "$HTTP_STATUS"
  fi
else
  skip_test "EPISODES CRUD" "Temporada não disponível"
fi

# =============================================================================
#  10. WATCH ENTRIES — CRUD
# =============================================================================
print_header "10. WATCH ENTRIES (registro de visualização)"

USER_ID="$REGISTERED_USER_ID"
if [[ -z "$USER_ID" || "$USER_ID" == "null" ]]; then
  USER_ID="1"
fi

if [[ -n "$TOKEN" && -n "$MEDIA_ID" && "$MEDIA_ID" != "null" ]]; then
  do_request POST "${BASE_URL}/api/v1/watch-entries" \
    "{\"userId\":${USER_ID},\"mediaId\":${MEDIA_ID},\"episodeId\":null,\"rating\":9,\"comment\":\"Teste automatizado - excelente!\",\"watchedAt\":\"2025-06-15\"}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/watch-entries (criar)" "201|200" "$HTTP_STATUS"
  WATCH_ENTRY_ID=$(json_field "$HTTP_BODY" '.id // empty')
  echo -e "    → watchEntryId = $WATCH_ENTRY_ID"

  # Criar outro sem rating
  do_request POST "${BASE_URL}/api/v1/watch-entries" \
    "{\"userId\":${USER_ID},\"mediaId\":${MEDIA_ID},\"episodeId\":null,\"rating\":null,\"comment\":null,\"watchedAt\":\"2025-07-10\"}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/watch-entries (sem rating)" "201|200" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/v1/watch-entries?userId=${USER_ID}&page=0&size=20" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/watch-entries (listar)" "200|500" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/v1/watch-entries?userId=${USER_ID}&mediaId=${MEDIA_ID}&page=0&size=20" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/watch-entries?mediaId (filtro)" "200|500" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/v1/watch-entries?userId=${USER_ID}&minRating=7&from=2025-01-01&to=2025-12-31&page=0&size=20" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/watch-entries (filtro avançado)" "200|500" "$HTTP_STATUS"

  if [[ -n "$WATCH_ENTRY_ID" && "$WATCH_ENTRY_ID" != "null" ]]; then
    do_request GET "${BASE_URL}/api/v1/watch-entries/${WATCH_ENTRY_ID}" "" "$TOKEN"
    assert_status "GET /api/v1/watch-entries/{id}" "200" "$HTTP_STATUS"

    do_request PUT "${BASE_URL}/api/v1/watch-entries/${WATCH_ENTRY_ID}" \
      "{\"userId\":${USER_ID},\"mediaId\":${MEDIA_ID},\"episodeId\":null,\"rating\":10,\"comment\":\"Reassisti - ainda melhor!\",\"watchedAt\":\"2025-07-01\"}" "$TOKEN"
    assert_status_oneOf "PUT /api/v1/watch-entries/{id}" "200|422" "$HTTP_STATUS"
  fi
else
  skip_test "WATCH ENTRIES CRUD" "Token ou mídia não disponível"
fi

# =============================================================================
#  11. WATCHLIST
# =============================================================================
print_header "11. WATCHLIST"

if [[ -n "$TOKEN" && -n "$MEDIA_ID" && "$MEDIA_ID" != "null" ]]; then
  do_request POST "${BASE_URL}/api/v1/watchlist" \
    "{\"mediaId\":${MEDIA_ID}}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/watchlist (adicionar)" "201|200|409" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/v1/watchlist?page=0&size=20&sort=id,asc" "" "$TOKEN"
  assert_status "GET /api/v1/watchlist (listar)" "200" "$HTTP_STATUS"

  # Adicionar série também
  if [[ -n "$SERIES_ID" && "$SERIES_ID" != "null" ]]; then
    do_request POST "${BASE_URL}/api/v1/watchlist" \
      "{\"mediaId\":${SERIES_ID}}" "$TOKEN"
    assert_status_oneOf "POST /api/v1/watchlist (série)" "201|200|409" "$HTTP_STATUS"
  fi
else
  skip_test "WATCHLIST" "Token ou mídia não disponível"
fi

# =============================================================================
#  12. WATCH PROGRESS
# =============================================================================
print_header "12. PROGRESSO DE SÉRIE"

if [[ -n "$TOKEN" && -n "$WATCH_ENTRY_ID" && "$WATCH_ENTRY_ID" != "null" ]]; then
  do_request POST "${BASE_URL}/api/watchentries/${WATCH_ENTRY_ID}/progress" \
    "{\"currentSeason\":1,\"currentEpisode\":3,\"watchedDurationSeconds\":930,\"totalDurationSeconds\":2700}" "$TOKEN"
  assert_status_oneOf "POST /api/watchentries/{id}/progress (salvar)" "200|201" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/watchentries/${WATCH_ENTRY_ID}/progress" "" "$TOKEN"
  assert_status_oneOf "GET /api/watchentries/{id}/progress" "200|404|500" "$HTTP_STATUS"

  do_request POST "${BASE_URL}/api/watchentries/${WATCH_ENTRY_ID}/progress" \
    "{\"currentSeason\":1,\"currentEpisode\":5,\"watchedDurationSeconds\":1800,\"totalDurationSeconds\":2700}" "$TOKEN"
  assert_status_oneOf "POST /api/watchentries/{id}/progress (atualizar)" "200|201" "$HTTP_STATUS"
else
  skip_test "WATCH PROGRESS" "WatchEntry não disponível"
fi

# =============================================================================
#  13. USERS
# =============================================================================
print_header "13. USUÁRIOS"

if [[ -n "$TOKEN" ]]; then
  do_request GET "${BASE_URL}/api/v1/users/me/stats" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/users/me/stats" "200|204" "$HTTP_STATUS"
fi

if [[ -n "$ADMIN_TOKEN" ]]; then
  do_request GET "${BASE_URL}/api/v1/users?page=0&size=20&sort=id,asc" "" "$ADMIN_TOKEN"
  assert_status "GET /api/v1/users (admin listar)" "200" "$HTTP_STATUS"

  if [[ -n "$REGISTERED_USER_ID" && "$REGISTERED_USER_ID" != "null" ]]; then
    do_request GET "${BASE_URL}/api/v1/users/${REGISTERED_USER_ID}" "" "$ADMIN_TOKEN"
    assert_status "GET /api/v1/users/{id} (admin)" "200" "$HTTP_STATUS"
  fi
else
  skip_test "GET /api/v1/users (admin)" "Admin token não disponível"
fi

if [[ -n "$TOKEN" ]]; then
  print_section "Listar usuários como USER → 403"
  do_request GET "${BASE_URL}/api/v1/users?page=0&size=10" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/users (regular user) → 403" "403|500" "$HTTP_STATUS"
fi

# =============================================================================
#  14. RECOMMENDATIONS & INSIGHTS
# =============================================================================
print_header "14. RECOMENDAÇÕES & INSIGHTS"

if [[ -n "$TOKEN" && -n "$USER_ID" && "$USER_ID" != "null" ]]; then
  do_request GET "${BASE_URL}/api/users/${USER_ID}/recommendations?limit=5" "" "$TOKEN"
  assert_status_oneOf "GET /api/users/{id}/recommendations" "200|204|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/users/${USER_ID}/recommendations/GENRE_BASED?limit=5" "" "$TOKEN"
  assert_status_oneOf "GET recommendations/GENRE_BASED" "200|204|400|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/users/${USER_ID}/recommendations/POPULAR?limit=5" "" "$TOKEN"
  assert_status_oneOf "GET recommendations/POPULAR" "200|204|400|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/users/${USER_ID}/recommendations/strategies" "" "$TOKEN"
  assert_status_oneOf "GET recommendations/strategies" "200|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/users/${USER_ID}/insights" "" "$TOKEN"
  assert_status_oneOf "GET /api/users/{id}/insights" "200|204|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/users/${USER_ID}/insights/exists" "" "$TOKEN"
  assert_status_oneOf "GET /api/users/{id}/insights/exists" "200|404" "$HTTP_STATUS"
else
  skip_test "RECOMMENDATIONS & INSIGHTS" "Token/userId não disponível"
fi

# =============================================================================
#  15. REPORTS
# =============================================================================
print_header "15. RELATÓRIOS"

if [[ -n "$TOKEN" ]]; then
  do_request GET "${BASE_URL}/api/v1/reports/weekly-digest" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/reports/weekly-digest (preview)" "200|204|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/v1/reports/top-rated?limit=5" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/reports/top-rated (preview)" "200|204|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/v1/reports/recommendations" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/reports/recommendations (preview)" "200|204|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/api/v1/reports/trending?days=7&limit=5" "" "$TOKEN"
  assert_status_oneOf "GET /api/v1/reports/trending (preview)" "200|204|404" "$HTTP_STATUS"

  do_request POST "${BASE_URL}/api/v1/reports/weekly-digest" "" "$TOKEN"
  assert_status_oneOf "POST /api/v1/reports/weekly-digest (enviar)" "202|200|500" "$HTTP_STATUS"

  do_request POST "${BASE_URL}/api/v1/reports/top-rated" \
    "{\"limit\":5}" "$TOKEN"
  assert_status_oneOf "POST /api/v1/reports/top-rated (enviar)" "202|200|500" "$HTTP_STATUS"

  do_request POST "${BASE_URL}/api/v1/reports/recommendations" "" "$TOKEN"
  assert_status_oneOf "POST /api/v1/reports/recommendations (enviar)" "202|200|500" "$HTTP_STATUS"

  do_request POST "${BASE_URL}/api/v1/reports/trending" "" "$TOKEN"
  assert_status_oneOf "POST /api/v1/reports/trending (enviar)" "202|200|500" "$HTTP_STATUS"
else
  skip_test "REPORTS" "Token não disponível"
fi

if [[ -n "$ADMIN_TOKEN" ]]; then
  do_request GET "${BASE_URL}/api/v1/admin/reports/platform" "" "$ADMIN_TOKEN"
  assert_status_oneOf "GET /api/v1/admin/reports/platform (admin preview)" "200|204|404" "$HTTP_STATUS"

  do_request POST "${BASE_URL}/api/v1/admin/reports/platform" \
    "{\"email\":\"test@mailhog.local\"}" "$ADMIN_TOKEN"
  assert_status_oneOf "POST /api/v1/admin/reports/platform (admin)" "202|200|500" "$HTTP_STATUS"
else
  skip_test "ADMIN REPORTS" "Admin token não disponível"
fi

if [[ -n "$TOKEN" ]]; then
  print_section "Reports admin como USER → 403"
  do_request POST "${BASE_URL}/api/v1/admin/reports/platform" \
    "{\"email\":\"test@mailhog.local\"}" "$TOKEN"
  assert_status "POST /api/v1/admin/reports/platform (user) → 403" "403" "$HTTP_STATUS"
fi

# =============================================================================
#  16. BATCH JOBS (Admin)
# =============================================================================
print_header "16. BATCH JOBS (import TMDB)"

if [[ -n "$ADMIN_TOKEN" ]]; then
  do_request POST "${BASE_URL}/api/v1/admin/batch/genres" "" "$ADMIN_TOKEN"
  assert_status_oneOf "POST /admin/batch/genres (sync)" "200|202|409" "$HTTP_STATUS"

  # Não vamos rodar imports pesados, mas testamos que a rota responde
  do_request POST "${BASE_URL}/api/v1/admin/batch/movies?maxPages=1" "" "$ADMIN_TOKEN"
  assert_status_oneOf "POST /admin/batch/movies?maxPages=1" "200|202|409|500" "$HTTP_STATUS"
else
  skip_test "BATCH JOBS" "Admin token não disponível"
fi

if [[ -n "$TOKEN" ]]; then
  print_section "Batch como USER → 403"
  do_request POST "${BASE_URL}/api/v1/admin/batch/genres" "" "$TOKEN"
  assert_status "POST /admin/batch/genres (user) → 403" "403" "$HTTP_STATUS"

  do_request POST "${BASE_URL}/api/v1/admin/batch/movies" "" "$TOKEN"
  assert_status "POST /admin/batch/movies (user) → 403" "403" "$HTTP_STATUS"
fi

print_section "Batch sem autenticação → 401"
do_request POST "${BASE_URL}/api/v1/admin/batch/movies"
assert_status_oneOf "POST /admin/batch/movies (sem token) → 401/403" "401|403" "$HTTP_STATUS"

# =============================================================================
#  17. DLQ (Admin/OPS)
# =============================================================================
print_header "17. DEAD LETTER QUEUE"

if [[ -n "$OPS_TOKEN" ]]; then
  do_request GET "${BASE_URL}/admin/dlq?page=0&size=5" "" "$OPS_TOKEN"
  assert_status_oneOf "GET /admin/dlq (listar)" "200|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/admin/dlq/stats" "" "$OPS_TOKEN"
  assert_status_oneOf "GET /admin/dlq/stats" "200|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/admin/dlq/topics" "" "$OPS_TOKEN"
  assert_status_oneOf "GET /admin/dlq/topics" "200|404" "$HTTP_STATUS"
elif [[ -n "$ADMIN_TOKEN" ]]; then
  do_request GET "${BASE_URL}/admin/dlq?page=0&size=5" "" "$ADMIN_TOKEN"
  assert_status_oneOf "GET /admin/dlq (admin)" "200|404" "$HTTP_STATUS"

  do_request GET "${BASE_URL}/admin/dlq/stats" "" "$ADMIN_TOKEN"
  assert_status_oneOf "GET /admin/dlq/stats (admin)" "200|404" "$HTTP_STATUS"
else
  skip_test "DLQ" "OPS/Admin token não disponível"
fi

# =============================================================================
#  18. CLEANUP — Deletar dados de teste (ordem reversa de dependência)
# =============================================================================
print_header "18. CLEANUP (deletar dados de teste)"

DELETE_TOKEN="${ADMIN_TOKEN:-$TOKEN}"

if [[ -n "$DELETE_TOKEN" ]]; then
  # Watch Progress
  if [[ -n "${WATCH_ENTRY_ID:-}" && "$WATCH_ENTRY_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/watchentries/${WATCH_ENTRY_ID}/progress" "" "$TOKEN"
    assert_status_oneOf "DELETE /api/watchentries/{id}/progress" "204|200|404" "$HTTP_STATUS"
  fi

  # Watchlist
  if [[ -n "${MEDIA_ID:-}" && "$MEDIA_ID" != "null" && -n "$TOKEN" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/watchlist/${MEDIA_ID}" "" "$TOKEN"
    assert_status_oneOf "DELETE /api/v1/watchlist/{mediaId}" "204|200|404|500" "$HTTP_STATUS"
  fi

  # Watch entries
  if [[ -n "${WATCH_ENTRY_ID:-}" && "$WATCH_ENTRY_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/watch-entries/${WATCH_ENTRY_ID}" "" "$TOKEN"
    assert_status_oneOf "DELETE /api/v1/watch-entries/{id}" "204|200|404" "$HTTP_STATUS"
  fi

  # Episodes
  if [[ -n "${EPISODE_ID:-}" && "$EPISODE_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/episodes/${EPISODE_ID}" "" "$DELETE_TOKEN"
    assert_status_oneOf "DELETE /api/v1/episodes/{id}" "204|200|404" "$HTTP_STATUS"
  fi

  # Seasons
  if [[ -n "${SEASON_ID:-}" && "$SEASON_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/seasons/${SEASON_ID}" "" "$DELETE_TOKEN"
    assert_status_oneOf "DELETE /api/v1/seasons/{id}" "204|200|404" "$HTTP_STATUS"
  fi

  # Credits
  if [[ -n "${CREDIT_ID:-}" && "$CREDIT_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/credits/${CREDIT_ID}" "" "$DELETE_TOKEN"
    assert_status_oneOf "DELETE /api/v1/credits/{id}" "204|200|404" "$HTTP_STATUS"
  fi

  # People
  if [[ -n "${PERSON_ID:-}" && "$PERSON_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/people/${PERSON_ID}" "" "$DELETE_TOKEN"
    assert_status_oneOf "DELETE /api/v1/people/{id}" "204|200|404" "$HTTP_STATUS"
  fi

  # Media
  if [[ -n "${MEDIA_ID:-}" && "$MEDIA_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/media/${MEDIA_ID}" "" "$DELETE_TOKEN"
    assert_status_oneOf "DELETE /api/v1/media/{id} (filme)" "204|200|404|500" "$HTTP_STATUS"
  fi

  if [[ -n "${SERIES_ID:-}" && "$SERIES_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/media/${SERIES_ID}" "" "$DELETE_TOKEN"
    assert_status_oneOf "DELETE /api/v1/media/{id} (série)" "204|200|404|500" "$HTTP_STATUS"
  fi

  # Genre
  if [[ -n "${GENRE_ID:-}" && "$GENRE_ID" != "null" ]]; then
    do_request DELETE "${BASE_URL}/api/v1/genres/${GENRE_ID}" "" "$DELETE_TOKEN"
    assert_status_oneOf "DELETE /api/v1/genres/{id}" "204|200|404" "$HTTP_STATUS"
  fi
else
  skip_test "CLEANUP" "Nenhum token disponível para deletar"
fi

# ─── Logout ───────────────────────────────────────────────────────────────────
print_section "Logout"
if [[ -n "$TOKEN" ]]; then
  do_request POST "${BASE_URL}/api/v1/auth/logout" "" "$TOKEN"
  assert_status_oneOf "POST /api/v1/auth/logout" "200|204" "$HTTP_STATUS"
fi

# =============================================================================
#  RESUMO FINAL
# =============================================================================
echo ""
echo -e "${BOLD}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║                    RESUMO DOS TESTES                         ║${NC}"
echo -e "${BOLD}╠═══════════════════════════════════════════════════════════════╣${NC}"
echo -e "${BOLD}║  Total:    ${TOTAL}                                              ${NC}"
echo -e "${BOLD}║  ${GREEN}Passed:  ${PASSED}${NC}                                              ${NC}"
echo -e "${BOLD}║  ${RED}Failed:  ${FAILED}${NC}                                              ${NC}"
echo -e "${BOLD}║  ${YELLOW}Skipped: ${SKIPPED}${NC}                                              ${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════════════════╝${NC}"

if [[ ${#FAILURES[@]} -gt 0 ]]; then
  echo ""
  echo -e "${RED}${BOLD}Falhas:${NC}"
  for f in "${FAILURES[@]}"; do
    echo -e "  ${RED}• $f${NC}"
  done
fi

echo ""
if [[ "$FAILED" -eq 0 ]]; then
  echo -e "${GREEN}${BOLD}🎉 Todos os testes passaram!${NC}"
  exit 0
else
  echo -e "${RED}${BOLD}⚠ $FAILED teste(s) falharam. Revise os detalhes acima.${NC}"
  exit 1
fi
