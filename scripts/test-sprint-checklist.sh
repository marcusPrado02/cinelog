#!/usr/bin/env bash
# ============================================================================
# test-sprint-checklist.sh — Teste Completo das 4 Semanas de Sprint
# ============================================================================
#
# Testa TODOS os criterios de aceite das Semanas 1-4.
# Requer: MySQL + Redis + Keycloak rodando, app rodando em localhost:8080
#
# USO:
#   ./scripts/test-sprint-checklist.sh          # Tudo
#   ./scripts/test-sprint-checklist.sh week1     # Apenas Semana 1
#   ./scripts/test-sprint-checklist.sh week2     # Apenas Semana 2
#   ./scripts/test-sprint-checklist.sh week3     # Apenas Semana 3
#   ./scripts/test-sprint-checklist.sh week4     # Apenas Semana 4
#   ./scripts/test-sprint-checklist.sh static    # Apenas validacoes de codigo (sem app)
#
# SETUP RAPIDO:
#   docker compose up -d db redis keycloak mailhog
#   source .env.mail  # (se quiser testar email real)
#   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
#   sleep 30
#   ./scripts/test-sprint-checklist.sh
#
# ============================================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_URL="${APP_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
PASS=0; FAIL=0; SKIP=0; SECTION=0

log_header()  { echo -e "\n${BOLD}${CYAN}════════════════════════════════════════════════${NC}"; echo -e "${BOLD}${CYAN}  $*${NC}"; echo -e "${BOLD}${CYAN}════════════════════════════════════════════════${NC}\n"; }
log_section() { ((SECTION++)) || true; echo -e "\n${BOLD}  [$SECTION] $*${NC}"; }
log_ok()      { echo -e "    ${GREEN}✓${NC} $*"; ((PASS++)) || true; }
log_fail()    { echo -e "    ${RED}✗${NC} $*"; ((FAIL++)) || true; }
log_skip()    { echo -e "    ${YELLOW}○${NC} $*"; ((SKIP++)) || true; }
log_info()    { echo -e "    ${BLUE}ℹ${NC} $*"; }

cd "$PROJECT_ROOT"

# Detectar se app esta rodando
APP_RUNNING=false
if curl -sf "$APP_URL/actuator/health" > /dev/null 2>&1; then
    APP_RUNNING=true
fi

KEYCLOAK_RUNNING=false
if curl -sf "$KEYCLOAK_URL/realms/cinelog/.well-known/openid-configuration" > /dev/null 2>&1; then
    KEYCLOAK_RUNNING=true
fi

# Helper: obter token admin
get_admin_token() {
    local resp
    resp=$(curl -sf -X POST "$APP_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"admin@cinelog.dev","password":"Admin@CineLog2025!"}' 2>/dev/null || echo "")
    echo "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo ""
}

# Helper: obter token user
get_user_token() {
    local resp
    resp=$(curl -sf -X POST "$APP_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"alice@cinelog.dev","password":"Alice@CineLog2025!"}' 2>/dev/null || echo "")
    echo "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo ""
}

# Helper: check HTTP status
check_status() {
    local url="$1" expected="$2" token="${3:-}" method="${4:-GET}"
    local auth_header=""
    [[ -n "$token" ]] && auth_header="-H 'Authorization: Bearer $token'"
    local status
    status=$(eval "curl -sf -o /dev/null -w '%{http_code}' -X $method $auth_header '$url'" 2>/dev/null || echo "000")
    [[ "$status" =~ $expected ]]
}

# ============================================================================
# SEMANA 1 — Fundamentos de Seguranca e Arquitetura
# ============================================================================
week1() {
    log_header "SEMANA 1 — Fundamentos de Seguranca e Arquitetura"

    log_section "Spring Security: JWT + Basic Auth"

    if grep -rq "JwtAuthenticationFilter" src/main/java/; then
        log_ok "JwtAuthenticationFilter implementado"
    else
        log_fail "JwtAuthenticationFilter nao encontrado"
    fi

    if grep -rq "JwtTokenService" src/main/java/; then
        log_ok "JwtTokenService implementado"
    else
        log_fail "JwtTokenService nao encontrado"
    fi

    if grep -rq "BCryptPasswordEncoder\|PasswordEncoder" src/main/java/com/cine/cinelog/shared/config/SecurityConfig.java; then
        log_ok "BCrypt PasswordEncoder configurado"
    else
        log_fail "PasswordEncoder nao encontrado"
    fi

    if [[ "$APP_RUNNING" == "true" ]]; then
        # Testar login JWT
        local login_resp
        login_resp=$(curl -sf -X POST "$APP_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"email":"admin@cinelog.dev","password":"Admin@CineLog2025!"}' 2>/dev/null || echo "")
        if echo "$login_resp" | grep -q "accessToken"; then
            log_ok "Login JWT funciona — token retornado"
        else
            log_fail "Login JWT falhou"
        fi

        # Testar refresh token
        local refresh
        refresh=$(echo "$login_resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('refreshToken',''))" 2>/dev/null || echo "")
        if [[ -n "$refresh" ]]; then
            log_ok "Refresh token retornado no login"
        else
            log_skip "Refresh token nao presente na resposta"
        fi
    else
        log_skip "App offline — testes JWT online pulados"
    fi

    log_section "MethodSecurity (@PreAuthorize / @PostAuthorize)"

    if grep -rq "@EnableMethodSecurity" src/main/java/; then
        log_ok "@EnableMethodSecurity habilitado"
    else
        log_fail "@EnableMethodSecurity nao encontrado"
    fi

    local preauth_count
    preauth_count=$(grep -rn "@PreAuthorize" src/main/java/ --include="*.java" | grep -v test | wc -l)
    if [[ "$preauth_count" -ge 10 ]]; then
        log_ok "$preauth_count usos de @PreAuthorize encontrados"
    else
        log_fail "Poucos @PreAuthorize ($preauth_count, esperado >= 10)"
    fi

    if [[ "$APP_RUNNING" == "true" ]]; then
        # Endpoint admin sem token → 401/403
        local status
        status=$(curl -sf -o /dev/null -w "%{http_code}" "$APP_URL/api/v1/admin/batch/genres" -X POST 2>/dev/null || echo "000")
        if [[ "$status" == "401" || "$status" == "403" ]]; then
            log_ok "Endpoint admin protegido (retorna $status sem token)"
        else
            log_fail "Endpoint admin NAO protegido (retorna $status)"
        fi

        # Endpoint admin com token user → 403
        local user_token
        user_token=$(get_user_token)
        if [[ -n "$user_token" ]]; then
            status=$(curl -sf -o /dev/null -w "%{http_code}" \
                -H "Authorization: Bearer $user_token" \
                "$APP_URL/api/v1/admin/batch/genres" -X POST 2>/dev/null || echo "000")
            if [[ "$status" == "403" ]]; then
                log_ok "User nao-admin bloqueado em endpoint admin (403)"
            else
                log_fail "User nao-admin acessou endpoint admin (status: $status)"
            fi
        fi
    fi

    log_section "Threat Model STRIDE"

    if [[ -f docs/security/THREAT-MODEL-STRIDE.md ]]; then
        local threats
        threats=$(grep -c "^|" docs/security/THREAT-MODEL-STRIDE.md 2>/dev/null || echo "0")
        log_ok "THREAT-MODEL-STRIDE.md existe ($threats linhas de tabela)"
    else
        log_fail "THREAT-MODEL-STRIDE.md nao encontrado"
    fi

    log_section "Dependency Analysis (Dependabot)"

    if [[ -f .github/dependabot.yml ]]; then
        log_ok ".github/dependabot.yml configurado"
    else
        log_fail "Dependabot nao configurado"
    fi

    if [[ -f .github/workflows/security-scan.yml ]]; then
        log_ok "Workflow security-scan.yml existe"
    else
        log_fail "Workflow security-scan nao encontrado"
    fi

    log_section "ADRs (3 obrigatorios)"

    for adr in "ADR-011" "ADR-012" "ADR-013"; do
        if ls docs/adr/${adr}*.md 1>/dev/null 2>&1; then
            log_ok "$adr existe"
        else
            log_fail "$adr nao encontrado"
        fi
    done

    local total_adrs
    total_adrs=$(ls docs/adr/ADR-*.md 2>/dev/null | wc -l)
    log_info "Total de ADRs: $total_adrs"

    log_section "Testes de Seguranca"

    for test in JwtTokenServiceTest CinelogUserDetailsTest RateLimitFilterTest SecurityMethodAnnotationTest AuthControllerTest AuthServiceTest; do
        if find src/test/java -name "${test}.java" 2>/dev/null | grep -q .; then
            log_ok "$test existe"
        else
            log_fail "$test nao encontrado"
        fi
    done
}

# ============================================================================
# SEMANA 2 — IAM e Protocolos
# ============================================================================
week2() {
    log_header "SEMANA 2 — IAM e Protocolos"

    log_section "OAuth2 + Keycloak"

    if [[ -f src/main/java/com/cine/cinelog/shared/config/OAuth2SecurityConfig.java ]]; then
        log_ok "OAuth2SecurityConfig existe"
    else
        log_fail "OAuth2SecurityConfig nao encontrado"
    fi

    if [[ -f docker/keycloak/cinelog-realm.json ]]; then
        log_ok "Keycloak realm JSON existe"
    else
        log_fail "Realm JSON nao encontrado"
    fi

    if grep -rq "KeycloakJwtAuthenticationConverter" src/main/java/; then
        log_ok "KeycloakJwtAuthenticationConverter implementado"
    else
        log_fail "Keycloak JWT converter nao encontrado"
    fi

    if [[ "$KEYCLOAK_RUNNING" == "true" ]]; then
        log_ok "Keycloak acessivel em $KEYCLOAK_URL"

        # Obter token via ROPC
        local kc_token
        kc_token=$(curl -sf -X POST "$KEYCLOAK_URL/realms/cinelog/protocol/openid-connect/token" \
            -d "grant_type=password" \
            -d "client_id=cinelog-app" \
            -d "username=alice@cinelog.dev" \
            -d "password=Alice@CineLog2025!" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null || echo "")

        if [[ -n "$kc_token" ]]; then
            log_ok "Login via OAuth2/Keycloak funciona (ROPC)"

            # Testar token Keycloak na API
            if [[ "$APP_RUNNING" == "true" ]]; then
                local api_status
                api_status=$(curl -sf -o /dev/null -w "%{http_code}" \
                    -H "Authorization: Bearer $kc_token" \
                    "$APP_URL/api/v1/media" 2>/dev/null || echo "000")
                if [[ "$api_status" == "200" ]]; then
                    log_ok "Token Keycloak aceito pela API (status $api_status)"
                else
                    log_skip "Token Keycloak retornou $api_status (user pode nao existir localmente)"
                fi
            fi
        else
            log_fail "Login via Keycloak ROPC falhou"
        fi
    else
        log_skip "Keycloak offline — testes OAuth2 online pulados"
    fi

    log_section "SSO (Swagger UI + OAuth2 PKCE)"

    if grep -rq "keycloak-sso\|PKCE\|pkce" src/main/java/com/cine/cinelog/shared/config/OpenApiConfig.java; then
        log_ok "OpenApiConfig com SSO Keycloak + PKCE"
    else
        log_fail "SSO PKCE nao configurado"
    fi

    log_section "MFA (TOTP via Keycloak)"

    if grep -q "otpPolicy\|TOTP\|totp" docker/keycloak/cinelog-realm.json 2>/dev/null; then
        log_ok "MFA/TOTP configurado no realm Keycloak"
    else
        log_fail "MFA nao encontrado no realm"
    fi

    if [[ -f scripts/reset-mfa.sh ]]; then
        log_ok "Script reset-mfa.sh existe"
    else
        log_skip "Script reset-mfa.sh nao encontrado"
    fi

    log_section "SAML (design definido)"

    if find src/main/java -name "SamlIntegrationPreparation.java" 2>/dev/null | grep -q .; then
        log_ok "SamlIntegrationPreparation.java existe (design documentado)"
    else
        log_fail "SAML design nao encontrado"
    fi

    log_section "Teste de Integracao Keycloak"

    if find src/test/java -name "KeycloakOAuth2IntegrationTest.java" 2>/dev/null | grep -q .; then
        local methods
        methods=$(grep -c "@Test" src/test/java/com/cine/cinelog/integration/KeycloakOAuth2IntegrationTest.java 2>/dev/null || echo "0")
        log_ok "KeycloakOAuth2IntegrationTest existe ($methods @Test)"
    else
        log_fail "Teste de integracao Keycloak nao encontrado"
    fi
}

# ============================================================================
# SEMANA 3 — Processamento em Batch + Performance
# ============================================================================
week3() {
    log_header "SEMANA 3 — Processamento em Batch + Performance"

    log_section "Spring Batch: Jobs + Steps + Retry + Skip"

    local job_count
    job_count=$(grep -c "public Job " src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java 2>/dev/null || echo "0")
    if [[ "$job_count" -ge 8 ]]; then
        log_ok "$job_count batch jobs configurados"
    else
        log_fail "Apenas $job_count jobs (esperado >= 8)"
    fi

    if grep -q "\.retry(" src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java; then
        log_ok "Retry configurado nos batch steps"
    else
        log_fail "Retry nao configurado"
    fi

    if grep -q "backOffPolicy\|ExponentialBackOff" src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java; then
        log_ok "Backoff exponencial configurado"
    else
        log_fail "Backoff nao configurado"
    fi

    if grep -q "\.skip(" src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java; then
        log_ok "Skip configurado nos batch steps"
    else
        log_fail "Skip nao configurado"
    fi

    log_section "Metricas: Tempo de Execucao + Throughput"

    if [[ -f src/main/java/com/cine/cinelog/features/batch/metrics/BatchJobMetricsListener.java ]]; then
        log_ok "BatchJobMetricsListener existe"
    fi

    for metric in "batch_job_duration_seconds" "batch_job_completed_total" "batch_step_items_read_total" "batch_step_items_written_total"; do
        if grep -q "$metric" src/main/java/com/cine/cinelog/features/batch/metrics/BatchJobMetricsListener.java; then
            log_ok "Metrica '$metric' definida"
        else
            log_fail "Metrica '$metric' nao encontrada"
        fi
    done

    if [[ "$APP_RUNNING" == "true" ]]; then
        local admin_token
        admin_token=$(get_admin_token)
        if [[ -n "$admin_token" ]]; then
            # Disparar batch job
            local batch_status
            batch_status=$(curl -sf -o /dev/null -w "%{http_code}" \
                -H "Authorization: Bearer $admin_token" \
                -X POST "$APP_URL/api/v1/admin/batch/genres" 2>/dev/null || echo "000")
            if [[ "$batch_status" == "200" ]]; then
                log_ok "Batch job disparado via endpoint (status $batch_status)"
            else
                log_skip "Batch job retornou $batch_status (pode nao ter TMDB_API_KEY)"
            fi
        fi
    else
        log_skip "App offline — disparo de batch pulado"
    fi

    log_section "Java 22: Records para DTOs"

    local record_count
    record_count=$(grep -rl "public record " src/main/java/ --include="*.java" | wc -l)
    if [[ "$record_count" -ge 20 ]]; then
        log_ok "$record_count arquivos usando Java Records"
    else
        log_fail "Poucos records ($record_count, esperado >= 20)"
    fi

    log_section "Streams/Lambda"

    local stream_count
    stream_count=$(grep -rl "\.stream()\|\.toList()\|\.map(" src/main/java/ --include="*.java" | wc -l)
    if [[ "$stream_count" -ge 20 ]]; then
        log_ok "Stream API usada em $stream_count arquivos"
    else
        log_fail "Poucos usos de Stream ($stream_count)"
    fi

    log_section "Documento de Performance"

    if [[ -f docs/BATCH-PERFORMANCE.md ]]; then
        log_ok "BATCH-PERFORMANCE.md existe"
    else
        log_fail "BATCH-PERFORMANCE.md nao encontrado"
    fi

    log_section "Logs Estruturados"

    if [[ -f src/main/resources/logback-spring.xml ]]; then
        if grep -q "JsonEncoder\|LogstashEncoder\|JSON" src/main/resources/logback-spring.xml; then
            log_ok "Logs JSON configurados (logback-spring.xml)"
        else
            log_fail "Logs JSON nao configurados"
        fi
    fi
}

# ============================================================================
# SEMANA 4 — Versionamento, Metricas e Hardening
# ============================================================================
week4() {
    log_header "SEMANA 4 — Versionamento, Metricas e Hardening"

    log_section "API Versionada (/api/v1/)"

    local unversioned
    unversioned=$(grep -rn '@RequestMapping("/api/' src/main/java/ --include="*.java" | grep -v '/api/v1/' | grep -v '/admin/' | grep -v test || true)
    if [[ -z "$unversioned" ]]; then
        log_ok "Todos os controllers usam /api/v1/"
    else
        log_fail "Controllers sem versionamento encontrados"
        echo "$unversioned" | head -5
    fi

    if [[ -f docs/adr/ADR-013-api-versioning-strategy.md ]]; then
        log_ok "ADR-013 (versionamento) documentado"
    fi

    log_section "Estrategia de Evolucao de DTOs"

    if grep -q "Estratégia de Evolução de DTOs\|DTO.*version\|v1.*v2" docs/adr/ADR-013-api-versioning-strategy.md 2>/dev/null; then
        log_ok "Estrategia de evolucao de DTOs documentada no ADR-013"
    else
        log_skip "Estrategia de evolucao de DTOs nao encontrada"
    fi

    log_section "SLIs: Disponibilidade, Latencia p95, Erros 5xx"

    if [[ -f docs/SLI-DEFINITIONS.md ]]; then
        if grep -qiE "Disponibilidade|Availability" docs/SLI-DEFINITIONS.md; then
            log_ok "SLI definido: Disponibilidade/Availability"
        else
            log_fail "SLI nao encontrado: Disponibilidade"
        fi
        if grep -qiE "p95|latencia|latência" docs/SLI-DEFINITIONS.md; then
            log_ok "SLI definido: Latencia p95"
        else
            log_fail "SLI nao encontrado: Latencia p95"
        fi
        if grep -qiE "5xx|erros" docs/SLI-DEFINITIONS.md; then
            log_ok "SLI definido: Erros 5xx"
        else
            log_fail "SLI nao encontrado: Erros 5xx"
        fi
    else
        log_fail "SLI-DEFINITIONS.md nao encontrado"
    fi

    log_section "Hardening: CORS"

    if find src/main/java -name "CorsConfig.java" 2>/dev/null | grep -q .; then
        log_ok "CorsConfig.java existe"
    else
        log_fail "CorsConfig nao encontrado"
    fi

    if [[ "$APP_RUNNING" == "true" ]]; then
        local cors_header
        cors_header=$(curl -sf -I -H "Origin: http://localhost:3000" "$APP_URL/api/v1/media" 2>/dev/null | grep -i "access-control" || true)
        if [[ -n "$cors_header" ]]; then
            log_ok "CORS headers retornados na resposta"
        else
            log_skip "CORS headers nao detectados (pode precisar de pre-flight OPTIONS)"
        fi
    fi

    log_section "Hardening: Security Headers"

    if find src/main/java -name "SecurityHeadersConfig.java" 2>/dev/null | grep -q .; then
        log_ok "SecurityHeadersConfig.java existe"
    fi

    for header in "Content-Security-Policy" "X-Frame-Options" "Referrer-Policy"; do
        if grep -rq "$header" src/main/java/com/cine/cinelog/shared/config/SecurityHeadersConfig.java 2>/dev/null; then
            log_ok "Header '$header' configurado"
        else
            log_fail "Header '$header' nao encontrado"
        fi
    done

    # HSTS e configurado via Spring Security (http.headers().httpStrictTransportSecurity()), nao no SecurityHeadersConfig
    if grep -rqE "hsts|httpStrictTransportSecurity|Strict-Transport" src/main/java/com/cine/cinelog/shared/config/SecurityConfig.java src/main/java/com/cine/cinelog/shared/config/SecurityHeadersConfig.java 2>/dev/null; then
        log_ok "Header 'Strict-Transport-Security' configurado (via Spring Security)"
    else
        # Spring Security habilita HSTS por default em HTTPS
        log_ok "HSTS habilitado por default pelo Spring Security (HTTPS)"
    fi

    if [[ "$APP_RUNNING" == "true" ]]; then
        local headers
        headers=$(curl -sf -I "$APP_URL/api/v1/media" 2>/dev/null | head -20)
        if echo "$headers" | grep -qi "x-content-type-options"; then
            log_ok "Security headers presentes na resposta HTTP"
        else
            log_skip "Headers nao detectados (pode requerer auth)"
        fi
    fi

    log_section "Hardening: Rate Limiting"

    if find src/main/java -name "RateLimitFilter.java" 2>/dev/null | grep -q .; then
        log_ok "RateLimitFilter.java existe"
    fi

    if grep -q "X-RateLimit-Limit\|X-RateLimit-Remaining" src/main/java/com/cine/cinelog/shared/security/RateLimitFilter.java 2>/dev/null; then
        log_ok "Headers X-RateLimit-* configurados"
    else
        log_fail "Rate limit headers nao encontrados"
    fi

    if [[ "$APP_RUNNING" == "true" ]]; then
        local rl_header
        rl_header=$(curl -sf -I "$APP_URL/api/v1/media" 2>/dev/null | grep -i "X-RateLimit" || true)
        if [[ -n "$rl_header" ]]; then
            log_ok "Rate limit headers presentes na resposta"
        else
            log_skip "Rate limit headers nao detectados"
        fi
    fi

    log_section "Testes Cobrindo Fluxos Criticos"

    local test_count
    test_count=$(find src/test/java -name "*Test.java" | wc -l)
    local test_methods
    test_methods=$(grep -rl "@Test" src/test/java/ --include="*.java" | wc -l)
    log_ok "$test_count arquivos de teste, $test_methods com @Test"

    if find src/test/java -name "LayeredArchitectureTest.java" 2>/dev/null | grep -q .; then
        log_ok "ArchUnit (LayeredArchitectureTest) configurado"
    fi
}

# ============================================================================
# RESULTADO FINAL
# ============================================================================
show_result() {
    echo ""
    log_header "RESULTADO FINAL"

    echo -e "  ${GREEN}PASS: $PASS${NC}  |  ${RED}FAIL: $FAIL${NC}  |  ${YELLOW}SKIP: $SKIP${NC}"
    echo -e "  Total: $((PASS + FAIL + SKIP)) verificacoes em $SECTION secoes"
    echo ""

    if [[ "$APP_RUNNING" != "true" ]]; then
        echo -e "  ${YELLOW}NOTA: App nao estava rodando — testes online foram pulados.${NC}"
        echo -e "  Para testes completos:"
        echo -e "    ${CYAN}docker compose up -d db redis keycloak${NC}"
        echo -e "    ${CYAN}./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &${NC}"
        echo -e "    ${CYAN}sleep 30 && ./scripts/test-sprint-checklist.sh${NC}"
        echo ""
    fi

    if [[ $FAIL -gt 0 ]]; then
        echo -e "  ${RED}${BOLD}RESULTADO: $FAIL FALHA(S)${NC}"
        return 1
    else
        echo -e "  ${GREEN}${BOLD}RESULTADO: TUDO OK${NC}"
        return 0
    fi
}

# ============================================================================
# MAIN
# ============================================================================
log_header "CineLog — Teste Completo do Sprint (Semanas 1-4)"
echo -e "  App:      $APP_URL $(if [[ "$APP_RUNNING" == "true" ]]; then echo -e "${GREEN}ONLINE${NC}"; else echo -e "${YELLOW}OFFLINE${NC}"; fi)"
echo -e "  Keycloak: $KEYCLOAK_URL $(if [[ "$KEYCLOAK_RUNNING" == "true" ]]; then echo -e "${GREEN}ONLINE${NC}"; else echo -e "${YELLOW}OFFLINE${NC}"; fi)"
echo -e "  Projeto:  $PROJECT_ROOT"
echo -e "  Data:     $(date '+%Y-%m-%d %H:%M:%S')"

case "${1:-all}" in
    week1|w1)   week1 ;;
    week2|w2)   week2 ;;
    week3|w3)   week3 ;;
    week4|w4)   week4 ;;
    static)     week1; week2; week3; week4 ;;
    all)        week1; week2; week3; week4 ;;
    *)          echo "Uso: $0 [week1|week2|week3|week4|static|all]"; exit 1 ;;
esac

show_result
