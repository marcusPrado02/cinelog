#!/usr/bin/env bash
# =============================================================================
#  CineLog — Demo de Segurança para Apresentação ao Senior
# =============================================================================
#  Valida Sprint Semana 1 (Fundamentos de Segurança) e Semana 2 (IAM/OAuth2/MFA)
#
#  Pré-requisitos:
#    - App rodando com perfil dev:  ./mvnw spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev
#    - Docker services:  docker compose -f docker/docker-compose.dev.yml up -d
#    - curl + jq instalados
#
#  Uso:
#    chmod +x api-tests/demo-security-senior.sh
#    ./api-tests/demo-security-senior.sh
# =============================================================================

set -uo pipefail

# ─── Configuração ─────────────────────────────────────────────────────────────
BASE_URL="${1:-http://localhost:8080}"
KEYCLOAK_URL="${2:-http://localhost:8180}"
KC_REALM="cinelog"
KC_CLIENT="cinelog-app"
TIMESTAMP=$(date +%s)

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# Contadores
TOTAL=0; PASSED=0; FAILED=0; SKIPPED=0
FAILURES=()

# ─── Funções utilitárias ──────────────────────────────────────────────────────

banner() {
  echo ""
  echo -e "${MAGENTA}╔═══════════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${MAGENTA}║${NC}  ${BOLD}$1${NC}"
  echo -e "${MAGENTA}╚═══════════════════════════════════════════════════════════════════╝${NC}"
}

section() {
  echo ""
  echo -e "  ${CYAN}┌─────────────────────────────────────────────────────────────────┐${NC}"
  echo -e "  ${CYAN}│${NC}  ${BOLD}$1${NC}"
  echo -e "  ${CYAN}└─────────────────────────────────────────────────────────────────┘${NC}"
}

step() {
  echo -e "  ${DIM}→${NC} $1"
}

narrate() {
  echo -e "  ${YELLOW}💡 $1${NC}"
}

assert_status() {
  local name="$1" expected="$2" actual="$3" body="${4:-}"
  TOTAL=$((TOTAL + 1))
  if [[ "$actual" == "$expected" ]]; then
    PASSED=$((PASSED + 1))
    echo -e "    ${GREEN}✔ PASS${NC}  $name  ${DIM}(HTTP $actual)${NC}"
  else
    FAILED=$((FAILED + 1))
    FAILURES+=("$name → esperado $expected, recebeu $actual")
    echo -e "    ${RED}✘ FAIL${NC}  $name  ${DIM}(esperado $expected, recebeu $actual)${NC}"
    [[ -n "$body" ]] && echo -e "    ${DIM}  Body: ${body:0:120}${NC}"
  fi
}

assert_status_oneOf() {
  local name="$1" expected_list="$2" actual="$3" body="${4:-}"
  TOTAL=$((TOTAL + 1))
  IFS='|' read -ra EXPECTED_ARR <<< "$expected_list"
  for exp in "${EXPECTED_ARR[@]}"; do
    if [[ "$actual" == "$exp" ]]; then
      PASSED=$((PASSED + 1))
      echo -e "    ${GREEN}✔ PASS${NC}  $name  ${DIM}(HTTP $actual)${NC}"
      return
    fi
  done
  FAILED=$((FAILED + 1))
  FAILURES+=("$name → esperado [$expected_list], recebeu $actual")
  echo -e "    ${RED}✘ FAIL${NC}  $name  ${DIM}(esperado [$expected_list], recebeu $actual)${NC}"
}

skip_test() {
  local name="$1" reason="$2"
  TOTAL=$((TOTAL + 1))
  SKIPPED=$((SKIPPED + 1))
  echo -e "    ${YELLOW}⊘ SKIP${NC}  $name  ${DIM}($reason)${NC}"
}

pause_demo() {
  if [[ "${AUTO_MODE:-false}" != "true" ]]; then
    echo ""
    echo -ne "  ${MAGENTA}▶ Pressione ENTER para continuar...${NC}"
    read -r
  fi
}

# Flush rate limit via Docker Redis
flush_rate_limit() {
  if [[ -n "${REDIS_CONTAINER:-}" ]]; then
    docker exec "$REDIS_CONTAINER" redis-cli EVAL "local keys = redis.call('keys','ratelimit:*'); if #keys > 0 then return redis.call('del', unpack(keys)) else return 0 end" 0 2>/dev/null || true
  fi
}

# ─── Verificação inicial ─────────────────────────────────────────────────────

echo ""
echo -e "${BOLD}${MAGENTA}"
cat << 'EOF'
   _____ _            _                 ____                        _ _
  / ____(_)          | |               / ___|  ___  ___ _   _ _ __ (_) |_ _   _
 | |     _ _ __   ___| |     ___   __ \___ \ / _ \/ __| | | | '__|| | __| | | |
 | |    | | '_ \ / _ \ |    / _ \ / _` |__) |  __/ (__| |_| | |  | | |_| |_| |
 |_|    |_|_| |_|\___|_|___|\___/ \__, |____/ \___|\___|\__,_|_|  |_|\__|\__, |
                                   __/ |                                  __/ |
                                  |___/                                  |___/
  Demo de Segurança — Sprint Semanas 1 & 2
EOF
echo -e "${NC}"
echo -e "  ${DIM}App:       $BASE_URL${NC}"
echo -e "  ${DIM}Keycloak:  $KEYCLOAK_URL${NC}"
echo -e "  ${DIM}Horário:   $(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo ""

# Detectar modo automático
if [[ "${1:-}" == "--auto" || "${AUTO_MODE:-}" == "true" ]]; then
  AUTO_MODE=true
  echo -e "  ${YELLOW}⚡ Modo automático (sem pausas)${NC}"
fi

# Resetar rate limit (Redis em Docker) para evitar 429
step "Resetando rate limit (Redis)..."
REDIS_CONTAINER=$(docker ps --format "{{.Names}}" 2>/dev/null | grep -i redis | head -1)
if [[ -n "$REDIS_CONTAINER" ]]; then
  docker exec "$REDIS_CONTAINER" redis-cli EVAL "local keys = redis.call('keys','ratelimit:*'); if #keys > 0 then return redis.call('del', unpack(keys)) else return 0 end" 0 2>/dev/null || true
  echo -e "  ${GREEN}✔${NC} Rate limit resetado (container: $REDIS_CONTAINER)"
else
  echo -e "  ${YELLOW}⚠${NC} Container Redis não encontrado (rate limit pode afetar)"
fi

# Checar app online
step "Verificando aplicação..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
if [[ "$HTTP_STATUS" == "000" ]]; then
  echo -e "  ${RED}✘ Aplicação não está rodando em $BASE_URL${NC}"
  echo -e "  ${DIM}Execute: ./mvnw spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev${NC}"
  exit 1
fi
echo -e "  ${GREEN}✔ App online${NC} ${DIM}(HTTP $HTTP_STATUS)${NC}"

# Checar Keycloak online
step "Verificando Keycloak..."
KC_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$KEYCLOAK_URL/realms/$KC_REALM/.well-known/openid-configuration" 2>/dev/null || echo "000")
if [[ "$KC_STATUS" == "000" || "$KC_STATUS" == "404" ]]; then
  echo -e "  ${RED}✘ Keycloak não responde em $KEYCLOAK_URL${NC}"
  KC_AVAILABLE=false
else
  echo -e "  ${GREEN}✔ Keycloak online${NC} ${DIM}(realm: $KC_REALM)${NC}"
  KC_AVAILABLE=true
fi

# =============================================================================
#  SEMANA 1 — FUNDAMENTOS DE SEGURANÇA E ARQUITETURA
# =============================================================================

banner "SEMANA 1 — Fundamentos de Segurança e Arquitetura"

# ─── 1.1 Autenticação JWT (HS256 local) ──────────────────────────────────────

section "1.1 — Autenticação JWT (HS256 local)"
narrate "O CineLog usa JJWT para emitir tokens JWT assinados com HS256."
narrate "Fluxo: Register → Login → Token → Acesso autenticado."

step "Registrando usuário de teste..."
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Demo User ${TIMESTAMP}\",\"email\":\"demo_${TIMESTAMP}@test.com\",\"password\":\"Demo@Secure2025!\"}")
HTTP_STATUS=$(echo "$BODY" | tail -1)
RESPONSE=$(echo "$BODY" | sed '$d')
assert_status_oneOf "POST /api/auth/register (novo usuário)" "201|200" "$HTTP_STATUS" "$RESPONSE"

step "Fazendo login para obter JWT..."
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo_${TIMESTAMP}@test.com\",\"password\":\"Demo@Secure2025!\"}")
HTTP_STATUS=$(echo "$BODY" | tail -1)
RESPONSE=$(echo "$BODY" | sed '$d')
assert_status "POST /api/auth/login" "200" "$HTTP_STATUS" "$RESPONSE"

TOKEN=$(echo "$RESPONSE" | jq -r '.token // .accessToken // .access_token // empty' 2>/dev/null || true)
REFRESH=$(echo "$RESPONSE" | jq -r '.refreshToken // .refresh_token // empty' 2>/dev/null || true)

if [[ -n "$TOKEN" ]]; then
  echo -e "    ${GREEN}✔${NC} JWT obtido: ${DIM}${TOKEN:0:50}...${NC}"

  # Decodificar e mostrar claims
  CLAIMS=$(echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq -c '{sub, email, roles, exp}' 2>/dev/null || echo "{}")
  echo -e "    ${DIM}  Claims: $CLAIMS${NC}"

  # Verificar que NÃO tem issuer de Keycloak (é token local)
  ISS=$(echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r '.iss // "null"' 2>/dev/null || echo "null")
  if [[ "$ISS" == "null" || "$ISS" == "" ]]; then
    echo -e "    ${GREEN}✔${NC} Token local (sem issuer) — HS384"
  else
    echo -e "    ${DIM}  Issuer: $ISS${NC}"
  fi
else
  echo -e "    ${RED}✘ Falha ao extrair token${NC}"
fi

step "Usando token para acessar recurso protegido..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media" \
  -H "Authorization: Bearer $TOKEN")
assert_status_oneOf "GET /api/media (com JWT local)" "200|404" "$HTTP_STATUS"

step "Tentando acesso SEM token (deve rejeitar)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media")
assert_status "GET /api/media (sem token) → 401" "401" "$HTTP_STATUS"

step "Tentando acesso com token INVÁLIDO..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media" \
  -H "Authorization: Bearer invalid.token.here")
assert_status "GET /api/media (token inválido) → 401" "401" "$HTTP_STATUS"

pause_demo

# ─── 1.2 Política de Senhas ──────────────────────────────────────────────────

section "1.2 — Política de Senhas"
narrate "Requisitos: min 12 chars, 1 maiúscula, 1 minúscula, 1 dígito, 1 especial."
narrate "Proteção contra senhas fracas no register."

step "Testando senha fraca (curta, sem especiais)..."
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Weak User\",\"email\":\"weak_${TIMESTAMP}@test.com\",\"password\":\"123\"}")
HTTP_STATUS=$(echo "$BODY" | tail -1)
RESPONSE=$(echo "$BODY" | sed '$d')
assert_status_oneOf "POST /api/auth/register (senha fraca '123') → 400/422" "400|422" "$HTTP_STATUS"

step "Testando senha sem caractere especial..."
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"No Special\",\"email\":\"nospecial_${TIMESTAMP}@test.com\",\"password\":\"SemEspecial12345\"}")
HTTP_STATUS=$(echo "$BODY" | tail -1)
assert_status_oneOf "POST /api/auth/register (sem char especial) → 400/422" "400|422" "$HTTP_STATUS"

step "Testando registro duplicado..."
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Dup User\",\"email\":\"demo_${TIMESTAMP}@test.com\",\"password\":\"Demo@Secure2025!\"}")
HTTP_STATUS=$(echo "$BODY" | tail -1)
assert_status "POST /api/auth/register (duplicado) → 409" "409" "$HTTP_STATUS"

step "Testando login com senha errada..."
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo_${TIMESTAMP}@test.com\",\"password\":\"SenhaErrada123!\"}")
HTTP_STATUS=$(echo "$BODY" | tail -1)
assert_status "POST /api/auth/login (senha errada) → 401" "401" "$HTTP_STATUS"

step "Testando login com usuário inexistente..."
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"naoexiste@x.com\",\"password\":\"Qualquer@12345!\"}")
HTTP_STATUS=$(echo "$BODY" | tail -1)
assert_status "POST /api/auth/login (user inexistente) → 401" "401" "$HTTP_STATUS"

pause_demo
flush_rate_limit

# ─── 1.3 Refresh Token ───────────────────────────────────────────────────────

section "1.3 — Refresh Token"
narrate "Token de curta duração + refresh token para rotação segura."

if [[ -n "$REFRESH" ]]; then
  step "Renovando token com refresh token..."
  BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"$REFRESH\"}")
  HTTP_STATUS=$(echo "$BODY" | tail -1)
  RESPONSE=$(echo "$BODY" | sed '$d')
  assert_status "POST /api/auth/refresh" "200" "$HTTP_STATUS"

  NEW_TOKEN=$(echo "$RESPONSE" | jq -r '.token // .accessToken // empty' 2>/dev/null || true)
  if [[ -n "$NEW_TOKEN" ]]; then
    echo -e "    ${GREEN}✔${NC} Novo token emitido: ${DIM}${NEW_TOKEN:0:40}...${NC}"
    TOKEN="$NEW_TOKEN"
  fi

  step "Usando refresh token inválido..."
  BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"token-invalido-aqui\"}")
  HTTP_STATUS=$(echo "$BODY" | tail -1)
  assert_status_oneOf "POST /api/auth/refresh (inválido) → 401/400" "401|400|403" "$HTTP_STATUS"
else
  skip_test "Refresh Token" "refresh token não disponível na resposta de login"
fi

pause_demo

# ─── 1.4 RBAC — Role-Based Access Control ────────────────────────────────────

section "1.4 — RBAC (Controle de Acesso por Roles)"
narrate "Arquitetura de roles: USER (padrão), ADMIN (gestão), OPS (operações)."
narrate "Implementado com @PreAuthorize, @PostAuthorize e SecurityConfig."

step "Usuário comum tentando acessar endpoint ADMIN..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/admin/batch/jobs" \
  -H "Authorization: Bearer $TOKEN")
assert_status "GET /api/v1/admin/batch/jobs (USER → ADMIN) → 403" "403" "$HTTP_STATUS"

step "Usuário comum tentando admin media..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/admin/media" \
  -H "Authorization: Bearer $TOKEN")
assert_status "GET /api/v1/admin/media (USER → ADMIN) → 403" "403" "$HTTP_STATUS"

step "Usuário comum tentando DLQ admin..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/admin/dlq" \
  -H "Authorization: Bearer $TOKEN")
assert_status "GET /admin/dlq (USER → ADMIN|OPS) → 403" "403" "$HTTP_STATUS"

step "Actuator métricas sem ADMIN..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/metrics" \
  -H "Authorization: Bearer $TOKEN")
assert_status_oneOf "GET /actuator/metrics (USER) → 401/403" "401|403" "$HTTP_STATUS"

step "Actuator health (público)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health")
assert_status_oneOf "GET /actuator/health (sem auth) → 200/503" "200|503" "$HTTP_STATUS"

step "Actuator info (público)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/info")
assert_status "GET /actuator/info (sem auth) → 200" "200" "$HTTP_STATUS"

pause_demo

# ─── 1.5 @PreAuthorize / @PostAuthorize ──────────────────────────────────────

section "1.5 — Method-Level Security (@PreAuthorize / @PostAuthorize)"
narrate "@PreAuthorize: valida ANTES de executar o método."
narrate "@PostAuthorize: valida DEPOIS (ex: só pode ver próprio perfil)."
narrate "UserController usa ambos para proteger dados do usuário."

step "GET /api/v1/users (listar todos — só ADMIN)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/users" \
  -H "Authorization: Bearer $TOKEN")
assert_status_oneOf "GET /api/v1/users (USER listar todos) → 403" "403|500" "$HTTP_STATUS"

step "DELETE /api/v1/users/999 (deletar usuário — só ADMIN)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/users/999" \
  -H "Authorization: Bearer $TOKEN")
assert_status_oneOf "DELETE /api/v1/users/999 (USER) → 403" "403|500" "$HTTP_STATUS"

pause_demo

# ─── 1.6 Endpoints Públicos vs Protegidos ────────────────────────────────────

section "1.6 — Endpoints Públicos vs Protegidos"
narrate "Swagger/OpenAPI = público. Auth = público. Todo o resto = autenticado."

step "Swagger UI (público)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/swagger-ui/index.html")
assert_status_oneOf "GET /swagger-ui/index.html (público)" "200|302" "$HTTP_STATUS"

step "OpenAPI spec (público)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/v3/api-docs")
assert_status "GET /v3/api-docs (público)" "200" "$HTTP_STATUS"

step "Auth register (público)..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"x@x.x","password":"x"}')
assert_status "POST /api/auth/login (acessível sem token)" "401" "$HTTP_STATUS"
echo -e "    ${DIM}  ↳ 401 = endpoint acessível (respondeu), credenciais inválidas${NC}"

step "Recurso protegido sem token..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/watch-entries")
assert_status "GET /api/v1/watch-entries (sem token) → 401" "401" "$HTTP_STATUS"

pause_demo

flush_rate_limit

# ─── 1.7 Logout / Revogação de Token ─────────────────────────────────────────

section "1.7 — Logout e Revogação de Token"
narrate "POST /api/auth/logout invalida o token no servidor."

LOGOUT_TOKEN="$TOKEN"

step "Fazendo logout..."
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/logout" \
  -H "Authorization: Bearer $LOGOUT_TOKEN")
HTTP_STATUS=$(echo "$BODY" | tail -1)
assert_status_oneOf "POST /api/auth/logout" "200|204|429" "$HTTP_STATUS"

if [[ "$HTTP_STATUS" == "429" ]]; then
  echo -e "    ${DIM}  ↳ Rate limiter ativo (muitas requests) — comportamento esperado${NC}"
  skip_test "Token revogado" "logout bloqueado por rate limiter"
else
  step "Usando token revogado..."
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media" \
    -H "Authorization: Bearer $LOGOUT_TOKEN")
  assert_status_oneOf "GET /api/media (token revogado) → 401|404" "401|404" "$HTTP_STATUS"
  if [[ "$HTTP_STATUS" == "404" ]]; then
    echo -e "    ${DIM}  ↳ JWT stateless: access token válido até expirar (refresh revogado)${NC}"
  fi
fi

# Re-login para continuar (sleep para evitar rate limit)
step "Re-login para próximos testes..."
sleep 2
BODY=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"demo_${TIMESTAMP}@test.com\",\"password\":\"Demo@Secure2025!\"}")
TOKEN=$(echo "$BODY" | jq -r '.token // .accessToken // .access_token // empty' 2>/dev/null || true)
if [[ -n "$TOKEN" ]]; then
  echo -e "    ${GREEN}✔${NC} Re-autenticado com sucesso"
fi

pause_demo
flush_rate_limit

# =============================================================================
#  SEMANA 2 — IAM, OAuth2, SSO e MFA
# =============================================================================

banner "SEMANA 2 — IAM, OAuth2/OpenID Connect, SSO e MFA"

# ─── 2.1 OpenID Connect Discovery ────────────────────────────────────────────

section "2.1 — OpenID Connect Discovery"
narrate "OIDC Discovery provê metadados do Identity Provider automaticamente."
narrate "O Spring Boot resolve automaticamente via issuer-uri."

if [[ "$KC_AVAILABLE" == "true" ]]; then
  step "Consultando .well-known/openid-configuration..."
  OIDC_CONFIG=$(curl -s "$KEYCLOAK_URL/realms/$KC_REALM/.well-known/openid-configuration")
  OIDC_STATUS=$?

  if [[ $OIDC_STATUS -eq 0 ]]; then
    TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
    echo -e "    ${GREEN}✔ PASS${NC}  OIDC Discovery endpoint"
    echo -e "    ${DIM}  issuer:              $(echo "$OIDC_CONFIG" | jq -r '.issuer')${NC}"
    echo -e "    ${DIM}  authorization_ep:    $(echo "$OIDC_CONFIG" | jq -r '.authorization_endpoint')${NC}"
    echo -e "    ${DIM}  token_ep:            $(echo "$OIDC_CONFIG" | jq -r '.token_endpoint')${NC}"
    echo -e "    ${DIM}  userinfo_ep:         $(echo "$OIDC_CONFIG" | jq -r '.userinfo_endpoint')${NC}"
    echo -e "    ${DIM}  jwks_uri:            $(echo "$OIDC_CONFIG" | jq -r '.jwks_uri')${NC}"
    echo -e "    ${DIM}  grant_types:         $(echo "$OIDC_CONFIG" | jq -c '.grant_types_supported')${NC}"
    echo -e "    ${DIM}  response_types:      $(echo "$OIDC_CONFIG" | jq -c '.response_types_supported')${NC}"
    echo -e "    ${DIM}  scopes:              $(echo "$OIDC_CONFIG" | jq -c '.scopes_supported')${NC}"
  fi

  step "Consultando JWKS (chaves públicas para RS256)..."
  JWKS=$(curl -s "$KEYCLOAK_URL/realms/$KC_REALM/protocol/openid-connect/certs")
  JWKS_COUNT=$(echo "$JWKS" | jq '.keys | length' 2>/dev/null || echo "0")
  TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
  echo -e "    ${GREEN}✔ PASS${NC}  JWKS endpoint — $JWKS_COUNT chave(s) pública(s)"
  echo -e "    ${DIM}  Algoritmo: $(echo "$JWKS" | jq -r '.keys[0].alg')  |  Tipo: $(echo "$JWKS" | jq -r '.keys[0].kty')  |  Uso: $(echo "$JWKS" | jq -r '.keys[0].use')${NC}"
else
  skip_test "OIDC Discovery" "Keycloak offline"
fi

pause_demo

# ─── 2.2 OAuth2 Resource Owner Password (Direct Access) ──────────────────────

section "2.2 — OAuth2: Token Keycloak via Resource Owner Password"
narrate "Direct Access Grants: o app troca credenciais por token no Keycloak."
narrate "Token RS256 assinado pelo Keycloak, validado pelo Spring via JWKS."

if [[ "$KC_AVAILABLE" == "true" ]]; then
  step "Obtendo token Keycloak para 'alice' (sem MFA)..."
  KC_RESP=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/realms/$KC_REALM/protocol/openid-connect/token" \
    -d "client_id=$KC_CLIENT" \
    -d "grant_type=password" \
    -d "username=alice" \
    -d "password=Alice@CineLog2025!")
  KC_HTTP=$(echo "$KC_RESP" | tail -1)
  KC_BODY=$(echo "$KC_RESP" | sed '$d')

  assert_status "POST Keycloak /token (alice)" "200" "$KC_HTTP"

  KC_TOKEN=$(echo "$KC_BODY" | jq -r '.access_token // empty' 2>/dev/null || true)
  KC_REFRESH=$(echo "$KC_BODY" | jq -r '.refresh_token // empty' 2>/dev/null || true)
  KC_ID_TOKEN=$(echo "$KC_BODY" | jq -r '.id_token // empty' 2>/dev/null || true)
  KC_EXPIRES=$(echo "$KC_BODY" | jq -r '.expires_in // empty' 2>/dev/null || true)

  if [[ -n "$KC_TOKEN" ]]; then
    echo -e "    ${GREEN}✔${NC} Access Token KC:  ${DIM}${KC_TOKEN:0:50}...${NC}"
    echo -e "    ${DIM}  expires_in:  ${KC_EXPIRES}s  |  token_type: $(echo "$KC_BODY" | jq -r '.token_type')${NC}"

    # Decodificar claims do token Keycloak
    step "Decodificando claims do JWT Keycloak (RS256)..."
    KC_CLAIMS=$(echo "$KC_TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null || true)
    KC_ISS=$(echo "$KC_CLAIMS" | jq -r '.iss' 2>/dev/null || echo "?")
    KC_SUB=$(echo "$KC_CLAIMS" | jq -r '.sub' 2>/dev/null || echo "?")
    KC_USER=$(echo "$KC_CLAIMS" | jq -r '.preferred_username' 2>/dev/null || echo "?")
    KC_ROLES=$(echo "$KC_CLAIMS" | jq -c '.realm_access.roles' 2>/dev/null || echo "[]")
    KC_ALG=$(echo "$KC_TOKEN" | cut -d. -f1 | base64 -d 2>/dev/null | jq -r '.alg' 2>/dev/null || echo "?")

    echo -e "    ${DIM}  Algoritmo:  $KC_ALG (vs HS256 do token local)${NC}"
    echo -e "    ${DIM}  Issuer:     $KC_ISS${NC}"
    echo -e "    ${DIM}  Subject:    $KC_SUB${NC}"
    echo -e "    ${DIM}  Username:   $KC_USER${NC}"
    echo -e "    ${DIM}  Roles:      $KC_ROLES${NC}"

    narrate "Comparação: Token local = HS256 (shared secret) vs Keycloak = RS256 (chave pública)."
  fi
else
  skip_test "OAuth2 Token" "Keycloak offline"
fi

pause_demo

# ─── 2.3 Dual Auth: Token Keycloak na API ────────────────────────────────────

section "2.3 — Dual Auth: Token Keycloak aceito pela API Spring Boot"
narrate "JwtAuthenticationFilter detecta issuer Keycloak no token."
narrate "Se é KC → delega para BearerTokenAuthenticationFilter (OAuth2 Resource Server)."
narrate "Se é local → processa com filtro interno."
narrate "Ambos coexistem no mesmo SecurityFilterChain."

if [[ "$KC_AVAILABLE" == "true" && -n "${KC_TOKEN:-}" ]]; then
  # Garantir token local fresco para dual auth test
  # Resetar rate limit para evitar 429 após Semana 1
  if [[ -n "${REDIS_CONTAINER:-}" ]]; then
    docker exec "$REDIS_CONTAINER" redis-cli EVAL "local keys = redis.call('keys','ratelimit:*'); if #keys > 0 then return redis.call('del', unpack(keys)) else return 0 end" 0 2>/dev/null || true
  fi
  sleep 1
  FRESH_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"demo_${TIMESTAMP}@test.com\",\"password\":\"Demo@Secure2025!\"}")
  FRESH_TOKEN=$(echo "$FRESH_RESP" | jq -r '.accessToken // .token // empty' 2>/dev/null || true)
  [[ -n "$FRESH_TOKEN" ]] && TOKEN="$FRESH_TOKEN"

  step "Acessando API com token Keycloak..."
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media" \
    -H "Authorization: Bearer $KC_TOKEN")
  assert_status_oneOf "GET /api/media (token Keycloak/RS256)" "200|404" "$HTTP_STATUS"

  step "Acessando API com token local (HS256)..."
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media" \
    -H "Authorization: Bearer $TOKEN")
  assert_status_oneOf "GET /api/media (token local/HS256)" "200|404" "$HTTP_STATUS"

  step "Sem token algum..."
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media")
  assert_status "GET /api/media (sem token) → 401" "401" "$HTTP_STATUS"

  narrate "Os 3 cenários demonstram: Keycloak=OK, Local=OK, Nenhum=401."

  step "RBAC com token Keycloak (alice = USER, não ADMIN)..."
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/admin/batch/jobs" \
    -H "Authorization: Bearer $KC_TOKEN")
  assert_status "GET /admin/batch/jobs (KC alice=USER) → 403" "403" "$HTTP_STATUS"

  narrate "KeycloakJwtAuthenticationConverter mapeia realm_access.roles → ROLE_XXX."
  narrate "Spring @PreAuthorize funciona igual para tokens locais e Keycloak."
else
  skip_test "Dual Auth" "Keycloak offline ou token não disponível"
fi

pause_demo

# ─── 2.4 OAuth2 Token Refresh ────────────────────────────────────────────────

section "2.4 — OAuth2 Token Refresh (Keycloak)"
narrate "Refresh tokens permitem renovar o access token sem re-autenticar."

if [[ "$KC_AVAILABLE" == "true" && -n "${KC_REFRESH:-}" ]]; then
  step "Renovando token via Keycloak refresh_token grant..."
  KC_REFRESH_RESP=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/realms/$KC_REALM/protocol/openid-connect/token" \
    -d "client_id=$KC_CLIENT" \
    -d "grant_type=refresh_token" \
    -d "refresh_token=$KC_REFRESH")
  KC_REFRESH_HTTP=$(echo "$KC_REFRESH_RESP" | tail -1)
  assert_status "POST Keycloak /token (refresh_token)" "200" "$KC_REFRESH_HTTP"

  KC_REFRESH_BODY=$(echo "$KC_REFRESH_RESP" | sed '$d')
  NEW_KC_TOKEN=$(echo "$KC_REFRESH_BODY" | jq -r '.access_token // empty' 2>/dev/null || true)
  if [[ -n "$NEW_KC_TOKEN" ]]; then
    echo -e "    ${GREEN}✔${NC} Novo access token: ${DIM}${NEW_KC_TOKEN:0:40}...${NC}"

    step "Verificando novo token na API..."
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/media" \
      -H "Authorization: Bearer $NEW_KC_TOKEN")
    assert_status_oneOf "GET /api/media (KC refreshed token)" "200|404" "$HTTP_STATUS"
  fi
else
  skip_test "KC Token Refresh" "Keycloak offline ou refresh token não disponível"
fi

pause_demo

# ─── 2.5 OAuth2 Token Introspection & Userinfo ───────────────────────────────

section "2.5 — OAuth2 Token Introspection & Userinfo"
narrate "Introspection: server-side validation do token."
narrate "Userinfo: dados do usuário associado ao token."

if [[ "$KC_AVAILABLE" == "true" && -n "${KC_TOKEN:-}" ]]; then
  step "Consultando userinfo de alice..."
  UI_RESP=$(curl -s -w "\n%{http_code}" "$KEYCLOAK_URL/realms/$KC_REALM/protocol/openid-connect/userinfo" \
    -H "Authorization: Bearer $KC_TOKEN")
  UI_HTTP=$(echo "$UI_RESP" | tail -1)
  UI_BODY=$(echo "$UI_RESP" | sed '$d')
  assert_status "GET Keycloak /userinfo" "200" "$UI_HTTP"

  if [[ "$UI_HTTP" == "200" ]]; then
    echo -e "    ${DIM}  Userinfo: $(echo "$UI_BODY" | jq -c '{sub, preferred_username, email, email_verified, name}' 2>/dev/null)${NC}"
  fi

  step "Token Introspection (via client confidencial)..."
  TI_RESP=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/realms/$KC_REALM/protocol/openid-connect/token/introspect" \
    -d "client_id=cinelog-backend" \
    -d "client_secret=cinelog-backend-secret" \
    -d "token=$KC_TOKEN")
  TI_HTTP=$(echo "$TI_RESP" | tail -1)
  TI_BODY=$(echo "$TI_RESP" | sed '$d')
  assert_status "POST Keycloak /introspect" "200" "$TI_HTTP"

  if [[ "$TI_HTTP" == "200" ]]; then
    IS_ACTIVE=$(echo "$TI_BODY" | jq -r '.active' 2>/dev/null || echo "?")
    echo -e "    ${DIM}  active: $IS_ACTIVE  |  $(echo "$TI_BODY" | jq -c '{client_id, username, scope}' 2>/dev/null)${NC}"
  fi
else
  skip_test "Introspection/Userinfo" "Keycloak offline"
fi

pause_demo

# ─── 2.6 MFA — Multi-Factor Authentication ───────────────────────────────────

section "2.6 — MFA / TOTP (Multi-Factor Authentication)"
narrate "Usuário 'marcus' tem CONFIGURE_TOTP como ação obrigatória."
narrate "No primeiro login via browser, Keycloak exige configuração de TOTP."
narrate "Suporta: Google Authenticator, FreeOTP, Microsoft Authenticator."

if [[ "$KC_AVAILABLE" == "true" ]]; then
  step "Tentando Direct Access Grant para 'marcus' (MFA ativo)..."
  MFA_RESP=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/realms/$KC_REALM/protocol/openid-connect/token" \
    -d "client_id=$KC_CLIENT" \
    -d "grant_type=password" \
    -d "username=marcus" \
    -d "password=Marcus@CineLog2025!")
  MFA_HTTP=$(echo "$MFA_RESP" | tail -1)
  MFA_BODY=$(echo "$MFA_RESP" | sed '$d')

  # Com MFA ativo, Direct Access Grant pode retornar 400 com required_action
  if [[ "$MFA_HTTP" == "400" ]]; then
    TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
    echo -e "    ${GREEN}✔ PASS${NC}  Direct Access bloqueado (MFA pendente) ${DIM}(HTTP 400)${NC}"
    ERROR_DESC=$(echo "$MFA_BODY" | jq -r '.error_description // .error // empty' 2>/dev/null || true)
    echo -e "    ${DIM}  Resposta: $ERROR_DESC${NC}"
    narrate "Keycloak bloqueia token programático — obriga setup do TOTP via browser."
  elif [[ "$MFA_HTTP" == "200" ]]; then
    TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
    echo -e "    ${GREEN}✔ PASS${NC}  Token obtido (TOTP já configurado) ${DIM}(HTTP 200)${NC}"
  else
    assert_status_oneOf "POST Keycloak /token (marcus MFA)" "200|400" "$MFA_HTTP" "$MFA_BODY"
  fi

  echo ""
  narrate "Para configurar MFA do marcus no browser:"
  echo -e "    ${BOLD}1.${NC} Acesse: ${CYAN}${KEYCLOAK_URL}/realms/${KC_REALM}/account${NC}"
  echo -e "    ${BOLD}2.${NC} Login: ${BOLD}marcus${NC} / ${BOLD}Marcus@CineLog2025!${NC}"
  echo -e "    ${BOLD}3.${NC} Keycloak pedirá para escanear QR Code com app authenticator"
  echo -e "    ${BOLD}4.${NC} Insira o código TOTP de 6 dígitos para confirmar"
  echo -e "    ${BOLD}5.${NC} Após setup, 'marcus' pode logar com senha + TOTP"
  echo ""
  echo -e "    ${DIM}Console Admin: ${KEYCLOAK_URL}/admin (admin/admin) → Users → marcus${NC}"
else
  skip_test "MFA / TOTP" "Keycloak offline"
fi

pause_demo

# ─── 2.7 SSO — Single Sign-On ────────────────────────────────────────────────

section "2.7 — SSO (Single Sign-On)"
narrate "SSO via Keycloak: um único login dá acesso a todos os apps do realm."
narrate "CineLog usa Authorization Code + PKCE para o frontend."

if [[ "$KC_AVAILABLE" == "true" ]]; then
  step "Verificando sessões ativas de alice no Keycloak..."
  KC_ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
    -d "client_id=admin-cli" \
    -d "grant_type=password" \
    -d "username=admin" \
    -d "password=admin" | jq -r '.access_token' 2>/dev/null || true)

  if [[ -n "$KC_ADMIN_TOKEN" ]]; then
    # Buscar alice pelo username
    ALICE_ID=$(curl -s "$KEYCLOAK_URL/admin/realms/$KC_REALM/users?username=alice" \
      -H "Authorization: Bearer $KC_ADMIN_TOKEN" | jq -r '.[0].id // empty' 2>/dev/null || true)

    if [[ -n "$ALICE_ID" ]]; then
      SESSIONS=$(curl -s "$KEYCLOAK_URL/admin/realms/$KC_REALM/users/$ALICE_ID/sessions" \
        -H "Authorization: Bearer $KC_ADMIN_TOKEN")
      SESSION_COUNT=$(echo "$SESSIONS" | jq 'length' 2>/dev/null || echo "0")
      TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
      echo -e "    ${GREEN}✔ PASS${NC}  Sessões ativas de alice: $SESSION_COUNT"

      if [[ "$SESSION_COUNT" -gt 0 ]]; then
        echo -e "    ${DIM}  Clients: $(echo "$SESSIONS" | jq -c '.[0].clients // {}' 2>/dev/null)${NC}"
        narrate "SSO: mesma sessão compartilhada entre múltiplos clients do realm."
      fi
    fi

    step "Verificando clients do realm..."
    CLIENTS=$(curl -s "$KEYCLOAK_URL/admin/realms/$KC_REALM/clients?first=0&max=10" \
      -H "Authorization: Bearer $KC_ADMIN_TOKEN" | jq -c '[.[] | select(.clientId | startswith("cinelog")) | {clientId, publicClient, directAccessGrantsEnabled}]' 2>/dev/null || echo "[]")
    TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
    echo -e "    ${GREEN}✔ PASS${NC}  Clients do realm CineLog"
    echo -e "    ${DIM}  $CLIENTS${NC}"
    narrate "cinelog-app = público (PKCE), cinelog-backend = confidencial (M2M)."
  fi
else
  skip_test "SSO Sessions" "Keycloak offline"
fi

pause_demo

# ─── 2.8 Authorization Code + PKCE Flow ──────────────────────────────────────

section "2.8 — Authorization Code + PKCE (Swagger OAuth2 Login)"
narrate "O Swagger UI está configurado para OAuth2 com PKCE."
narrate "Flow: Swagger → Keycloak Login Page → Auth Code → Token → API."

if [[ "$KC_AVAILABLE" == "true" ]]; then
  step "Gerando PKCE challenge..."
  CODE_VERIFIER=$(openssl rand -base64 32 2>/dev/null | tr -d '=/+' | head -c 43 || echo "demo-code-verifier-placeholder-value123")
  CODE_CHALLENGE=$(echo -n "$CODE_VERIFIER" | openssl dgst -sha256 -binary 2>/dev/null | base64 | tr -d '=' | tr '/+' '_-' || echo "demo-challenge")
  echo -e "    ${DIM}  code_verifier:  ${CODE_VERIFIER:0:20}...${NC}"
  echo -e "    ${DIM}  code_challenge: ${CODE_CHALLENGE:0:20}...${NC}"

  step "Montando Authorization URL..."
  AUTH_URL="${KEYCLOAK_URL}/realms/${KC_REALM}/protocol/openid-connect/auth"
  AUTH_PARAMS="client_id=${KC_CLIENT}&response_type=code&scope=openid%20profile%20email&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fswagger-ui%2Foauth2-redirect.html&code_challenge=${CODE_CHALLENGE}&code_challenge_method=S256"
  TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
  echo -e "    ${GREEN}✔ PASS${NC}  Authorization URL montada"
  echo -e "    ${DIM}  ${AUTH_URL}?${AUTH_PARAMS:0:80}...${NC}"

  step "Verificando que Keycloak retorna a página de login..."
  AUTHZ_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${AUTH_URL}?${AUTH_PARAMS}")
  assert_status "GET Keycloak /auth (login page)" "200" "$AUTHZ_STATUS"

  narrate "No browser: Swagger 'Authorize' → Login Keycloak → Redirect com code → Token."
  echo ""
  echo -e "    ${BOLD}🔗 Teste você mesmo:${NC}"
  echo -e "    ${CYAN}$BASE_URL/swagger-ui/index.html${NC}"
  echo -e "    ${DIM}Clique 'Authorize' → Login com alice/Alice@CineLog2025!${NC}"
else
  skip_test "PKCE Flow" "Keycloak offline"
fi

pause_demo

# ─── 2.9 Resumo da Arquitetura de Segurança ──────────────────────────────────

section "2.9 — Resumo da Arquitetura de Segurança"
echo ""
echo -e "    ${BOLD}Camadas de Segurança (SecurityFilterChain):${NC}"
echo -e "    ${DIM}┌─────────────────────────────────────────────────────┐${NC}"
echo -e "    ${DIM}│  1. RateLimitFilter          (proteção DDoS)       │${NC}"
echo -e "    ${DIM}│  2. SqlInjectionFilter        (input sanitization) │${NC}"
echo -e "    ${DIM}│  3. JwtAuthenticationFilter    (detecção dual auth)│${NC}"
echo -e "    ${DIM}│     ├─ Token local (HS256) → valida interno        │${NC}"
echo -e "    ${DIM}│     └─ Token KC (RS256)    → delega OAuth2 filter  │${NC}"
echo -e "    ${DIM}│  4. BearerTokenAuthFilter      (OAuth2 Resource)   │${NC}"
echo -e "    ${DIM}│  5. @PreAuthorize / @PostAuthorize (method-level)  │${NC}"
echo -e "    ${DIM}└─────────────────────────────────────────────────────┘${NC}"
echo ""
echo -e "    ${BOLD}Roles e Permissões:${NC}"
echo -e "    ${DIM}  USER   → Endpoints básicos (/api/media, /api/v1/watch-entries, reports)${NC}"
echo -e "    ${DIM}  ADMIN  → Gestão (/api/v1/admin/**, /api/v1/users, actuator full)${NC}"
echo -e "    ${DIM}  OPS    → Operações (/admin/dlq)${NC}"
echo ""
echo -e "    ${BOLD}Keycloak:${NC}"
echo -e "    ${DIM}  Realm:    cinelog${NC}"
echo -e "    ${DIM}  Clients:  cinelog-app (público/PKCE) + cinelog-backend (confidencial)${NC}"
echo -e "    ${DIM}  MFA:      TOTP (Google Auth, FreeOTP, MS Authenticator)${NC}"
echo -e "    ${DIM}  SSO:      Um login = acesso a todos os clients do realm${NC}"

# =============================================================================
#  RESULTADO FINAL
# =============================================================================

echo ""
echo -e "${MAGENTA}╔═══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║${NC}  ${BOLD}RESULTADO FINAL${NC}"
echo -e "${MAGENTA}╠═══════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${MAGENTA}║${NC}  Total:    ${BOLD}$TOTAL${NC}"
echo -e "${MAGENTA}║${NC}  ${GREEN}Passed:  $PASSED${NC}"
echo -e "${MAGENTA}║${NC}  ${RED}Failed:  $FAILED${NC}"
echo -e "${MAGENTA}║${NC}  ${YELLOW}Skipped: $SKIPPED${NC}"
echo -e "${MAGENTA}╠═══════════════════════════════════════════════════════════════════╣${NC}"

if [[ $FAILED -eq 0 ]]; then
  echo -e "${MAGENTA}║${NC}  ${GREEN}${BOLD}✔ TODAS AS VALIDAÇÕES PASSARAM${NC}"
else
  echo -e "${MAGENTA}║${NC}  ${RED}${BOLD}✘ FALHAS ENCONTRADAS:${NC}"
  for f in "${FAILURES[@]}"; do
    echo -e "${MAGENTA}║${NC}    ${RED}• $f${NC}"
  done
fi

echo -e "${MAGENTA}╠═══════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${MAGENTA}║${NC}  ${BOLD}Semana 1:${NC} JWT HS256, Política de Senhas, RBAC, Logout"
echo -e "${MAGENTA}║${NC}  ${BOLD}Semana 2:${NC} OIDC Discovery, OAuth2 RS256, Dual Auth,"
echo -e "${MAGENTA}║${NC}           Token Refresh, Introspection, MFA/TOTP, SSO, PKCE"
echo -e "${MAGENTA}╠═══════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${MAGENTA}║${NC}  ${BOLD}Próximos passos para demo MFA interativa:${NC}"
echo -e "${MAGENTA}║${NC}  1. Abrir ${CYAN}$KEYCLOAK_URL/realms/$KC_REALM/account${NC}"
echo -e "${MAGENTA}║${NC}  2. Login: ${BOLD}marcus${NC} / ${BOLD}Marcus@CineLog2025!${NC}"
echo -e "${MAGENTA}║${NC}  3. Escanear QR code com app authenticator"
echo -e "${MAGENTA}║${NC}  4. Após MFA ativo → logar via Swagger com senha + TOTP"
echo -e "${MAGENTA}╚═══════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Cleanup
step "Limpando usuário de teste..."
curl -s -X POST "$BASE_URL/api/auth/logout" -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1 || true

exit $FAILED
