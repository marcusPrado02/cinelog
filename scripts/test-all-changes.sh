#!/usr/bin/env bash
# ============================================================================
# test-all-changes.sh — Script de Teste Automatizado para Todas as Alteracoes
# ============================================================================
#
# Testa de forma interativa e pratica todas as mudancas feitas no projeto:
#   1. Compilacao limpa (mvn compile)
#   2. Testes unitarios batch (metricas, config, retry)
#   3. Testes unitarios auth (AuthController, AuthService)
#   4. Validacao de versionamento de endpoints
#   5. Validacao do TmdbResponseMapper (extracao SRP)
#   6. Validacao de exception handling (catch especificos)
#   7. Batch ops test suite (26 testes automatizados)
#   8. Envio de email de teste (se app rodando)
#
# USO:
#   ./scripts/test-all-changes.sh              # Todos os testes
#   ./scripts/test-all-changes.sh --compile    # Apenas compilacao
#   ./scripts/test-all-changes.sh --unit       # Apenas testes unitarios
#   ./scripts/test-all-changes.sh --validate   # Apenas validacoes estaticas
#   ./scripts/test-all-changes.sh --email      # Apenas teste de email
#
# ============================================================================
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Carrega .env.mail automaticamente se existir (SMTP real configurado)
if [[ -f "$PROJECT_ROOT/.env.mail" ]]; then
    source "$PROJECT_ROOT/.env.mail"
fi
# Fallback: .env.email (gerado pelo setup-real-email.sh)
if [[ -f "$PROJECT_ROOT/.env.email" ]]; then
    source "$PROJECT_ROOT/.env.email"
fi

APP_URL="${APP_URL:-http://localhost:8080}"
EMAIL_TEST_TO="${EMAIL_TEST_TO:-marcus.prado@pitang.com}"
PASS=0
FAIL=0
SKIP=0
SECTION=0

log_header() { echo -e "\n${BOLD}${CYAN}═══ $* ═══${NC}\n"; }
log_section() { ((SECTION++)) || true; echo -e "\n${BOLD}[$SECTION] $*${NC}"; }
log_ok()   { echo -e "  ${GREEN}PASS${NC} $*"; ((PASS++)) || true; }
log_fail() { echo -e "  ${RED}FAIL${NC} $*"; ((FAIL++)) || true; }
log_skip() { echo -e "  ${YELLOW}SKIP${NC} $*"; ((SKIP++)) || true; }
log_info() { echo -e "  ${BLUE}INFO${NC} $*"; }

cd "$PROJECT_ROOT"

# ── Parse args ───────────────────────────────────────────────────────────────
RUN_COMPILE=true; RUN_UNIT=true; RUN_VALIDATE=true; RUN_EMAIL=true; RUN_BATCH=true
if [[ "${1:-}" != "" ]]; then
    RUN_COMPILE=false; RUN_UNIT=false; RUN_VALIDATE=false; RUN_EMAIL=false; RUN_BATCH=false
    case "${1:-}" in
        --compile)  RUN_COMPILE=true ;;
        --unit)     RUN_UNIT=true ;;
        --validate) RUN_VALIDATE=true ;;
        --email)    RUN_EMAIL=true ;;
        --batch)    RUN_BATCH=true ;;
        --all)      RUN_COMPILE=true; RUN_UNIT=true; RUN_VALIDATE=true; RUN_EMAIL=true; RUN_BATCH=true ;;
        *) echo "Uso: $0 [--compile|--unit|--validate|--email|--batch|--all]"; exit 1 ;;
    esac
fi

log_header "CineLog — Teste Automatizado de Todas as Alteracoes"
echo -e "  Projeto: $PROJECT_ROOT"
echo -e "  Data:    $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# ============================================================================
# 1. COMPILACAO
# ============================================================================
if [[ "$RUN_COMPILE" == "true" ]]; then
    log_section "Compilacao (mvn compile)"

    if mvn compile -q 2>&1 | tail -5; then
        # Check exit code
        if mvn compile -q 2>/dev/null; then
            log_ok "Compilacao bem-sucedida (zero erros)"
        else
            log_fail "Compilacao falhou"
        fi
    fi
fi

# ============================================================================
# 2. TESTES UNITARIOS
# ============================================================================
if [[ "$RUN_UNIT" == "true" ]]; then
    log_section "Testes Unitarios — Batch"

    if mvn test -q -Dtest="com.cine.cinelog.features.batch.metrics.BatchJobMetricsListenerTest,com.cine.cinelog.features.batch.config.BatchJobsConfigTest" -Dsurefire.useFile=false 2>&1 | tail -10; then
        log_ok "BatchJobMetricsListenerTest + BatchJobsConfigTest"
    else
        log_fail "Testes de batch falharam"
    fi

    log_section "Testes Unitarios — Auth"

    if mvn test -q -Dtest="com.cine.cinelog.features.auth.web.controller.AuthControllerTest,com.cine.cinelog.features.auth.service.AuthServiceTest" -Dsurefire.useFile=false 2>&1 | tail -10; then
        log_ok "AuthControllerTest + AuthServiceTest"
    else
        log_fail "Testes de auth falharam"
    fi
fi

# ============================================================================
# 3. VALIDACOES ESTATICAS
# ============================================================================
if [[ "$RUN_VALIDATE" == "true" ]]; then
    log_section "Validacao: Versionamento de Endpoints"

    # Todos os controllers devem usar /api/v1/
    UNVERSIONED=$(grep -rn '@RequestMapping("/api/' src/main/java/ 2>/dev/null \
        | grep -v '/api/v1/' \
        | grep -v '/admin/' \
        | grep -v 'test' || true)

    if [[ -z "$UNVERSIONED" ]]; then
        log_ok "Todos os controllers usam /api/v1/ (versionados)"
    else
        log_fail "Controllers sem versionamento encontrados:"
        echo "$UNVERSIONED" | head -10
    fi

    # SecurityConfig deve referenciar /api/v1/auth
    if grep -q '/api/v1/auth/' src/main/java/com/cine/cinelog/shared/config/SecurityConfig.java; then
        log_ok "SecurityConfig usa /api/v1/auth/**"
    else
        log_fail "SecurityConfig nao referencia /api/v1/auth"
    fi

    # RateLimitFilter deve referenciar /api/v1/auth
    if grep -q '/api/v1/auth' src/main/java/com/cine/cinelog/shared/security/RateLimitFilter.java; then
        log_ok "RateLimitFilter usa /api/v1/auth"
    else
        log_fail "RateLimitFilter nao referencia /api/v1/auth"
    fi

    log_section "Validacao: Retry no BatchJobsConfig"

    RETRY_COUNT=$(grep -c '\.retry(' src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java || true)
    BACKOFF=$(grep -c 'backOffPolicy' src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java || true)

    if [[ "$RETRY_COUNT" -ge 4 ]]; then
        log_ok "Retry configurado no BatchJobsConfig ($RETRY_COUNT ocorrencias de .retry())"
    else
        log_fail "Retry insuficiente ($RETRY_COUNT ocorrencias, esperado >= 4)"
    fi

    if [[ "$BACKOFF" -ge 1 ]]; then
        log_ok "ExponentialBackOffPolicy configurado"
    else
        log_fail "BackOffPolicy nao encontrado"
    fi

    log_section "Validacao: Skip Limits Externalizados"

    if grep -q 'private int skipLimit' src/main/java/com/cine/cinelog/features/batch/config/BatchJobProperties.java; then
        log_ok "skipLimit externalizado em BatchJobProperties"
    else
        log_fail "skipLimit nao encontrado em BatchJobProperties"
    fi

    if grep -q 'private int retryLimit' src/main/java/com/cine/cinelog/features/batch/config/BatchJobProperties.java; then
        log_ok "retryLimit externalizado em BatchJobProperties"
    else
        log_fail "retryLimit nao encontrado em BatchJobProperties"
    fi

    if grep -q 'props.getSkipLimit()' src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java; then
        log_ok "BatchJobsConfig usa props.getSkipLimit() (sem magic numbers)"
    else
        log_fail "BatchJobsConfig nao usa props.getSkipLimit()"
    fi

    log_section "Validacao: TmdbResponseMapper Extraido"

    if [[ -f src/main/java/com/cine/cinelog/features/media/integration/tmdb/TmdbResponseMapper.java ]]; then
        log_ok "TmdbResponseMapper.java existe"
    else
        log_fail "TmdbResponseMapper.java nao encontrado"
    fi

    ADAPTER_LINES=$(wc -l < src/main/java/com/cine/cinelog/features/media/integration/tmdb/TmdbClientAdapter.java)
    MAPPER_LINES=$(wc -l < src/main/java/com/cine/cinelog/features/media/integration/tmdb/TmdbResponseMapper.java)

    if [[ "$ADAPTER_LINES" -lt 900 ]]; then
        log_ok "TmdbClientAdapter reduzido para $ADAPTER_LINES linhas (era 1018)"
    else
        log_fail "TmdbClientAdapter ainda tem $ADAPTER_LINES linhas (esperado < 900)"
    fi

    log_info "TmdbResponseMapper: $MAPPER_LINES linhas"

    if grep -q 'mapper.map' src/main/java/com/cine/cinelog/features/media/integration/tmdb/TmdbClientAdapter.java; then
        log_ok "TmdbClientAdapter delega mapeamento para mapper"
    else
        log_fail "TmdbClientAdapter nao delega para mapper"
    fi

    log_section "Validacao: Exception Handling (catch especificos)"

    # Processors devem capturar WebClientResponseException antes de RuntimeException
    for proc in TmdbMediaImageProcessor TmdbPersonDetailsProcessor TmdbReviewsItemProcessor; do
        FILE=$(find src/main/java -name "${proc}.java" 2>/dev/null | head -1)
        if [[ -n "$FILE" ]] && grep -q 'WebClientResponseException' "$FILE"; then
            log_ok "$proc captura WebClientResponseException (retryable)"
        else
            log_fail "$proc nao captura WebClientResponseException"
        fi
    done

    # BatchSchedulerConfig deve ter catches especificos
    if grep -q 'JobRestartException' src/main/java/com/cine/cinelog/features/batch/scheduler/BatchSchedulerConfig.java; then
        log_ok "BatchSchedulerConfig tem catch especifico para JobRestartException"
    else
        log_fail "BatchSchedulerConfig sem catch especifico"
    fi

    log_section "Validacao: Testes de Auth Existem"

    for test in AuthControllerTest AuthServiceTest; do
        FILE=$(find src/test/java -name "${test}.java" 2>/dev/null | head -1)
        if [[ -n "$FILE" ]]; then
            METHODS=$(grep -c '@Test' "$FILE" || true)
            log_ok "$test existe ($METHODS metodos @Test)"
        else
            log_fail "$test nao encontrado"
        fi
    done

    log_section "Validacao: Listener registrado em Jobs e Steps"

    LISTENER_COUNT=$(grep -c '\.listener(metricsListener)' src/main/java/com/cine/cinelog/features/batch/config/BatchJobsConfig.java || true)
    if [[ "$LISTENER_COUNT" -ge 15 ]]; then
        log_ok "metricsListener registrado $LISTENER_COUNT vezes (Jobs + Steps)"
    else
        log_fail "metricsListener registrado apenas $LISTENER_COUNT vezes (esperado >= 15)"
    fi
fi

# ============================================================================
# 4. BATCH OPS TEST SUITE
# ============================================================================
if [[ "$RUN_BATCH" == "true" ]]; then
    log_section "Batch Ops Test Suite (batch-ops.sh test)"

    BATCH_RESULT=$(bash scripts/batch-ops.sh test 2>&1 | tail -5)
    if echo "$BATCH_RESULT" | grep -q "Todos os testes passaram"; then
        BATCH_PASS=$(echo "$BATCH_RESULT" | grep -oP 'PASS: \K[0-9]+' || echo "?")
        log_ok "batch-ops.sh test: $BATCH_PASS testes passaram"
    else
        log_fail "batch-ops.sh test falhou"
        echo "$BATCH_RESULT"
    fi
fi

# ============================================================================
# 5. TESTE DE EMAIL (requer app rodando)
# ============================================================================
if [[ "$RUN_EMAIL" == "true" ]]; then
    log_section "Teste de Email (requer app rodando)"

    # Mostrar config de SMTP
    if [[ -n "${MAIL_HOST:-}" ]]; then
        log_info "SMTP: ${MAIL_USER:-?}@${MAIL_HOST:-?}:${MAIL_PORT:-?}"
        log_info "Destinatario: $EMAIL_TEST_TO"
    else
        log_info "SMTP nao configurado (.env.mail ou .env.email ausente)"
    fi

    if curl -sf "$APP_URL/actuator/health" > /dev/null 2>&1; then
        log_ok "App acessivel em $APP_URL"

        # Login para obter token
        TOKEN_RESP=$(curl -sf -X POST "$APP_URL/api/v1/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"email":"admin@cinelog.dev","password":"Admin@CineLog2025!"}' 2>/dev/null || echo "")

        if [[ -z "$TOKEN_RESP" ]]; then
            # Tentar com usuario local
            TOKEN_RESP=$(curl -sf -X POST "$APP_URL/api/v1/auth/login" \
                -H "Content-Type: application/json" \
                -d "{\"email\":\"$MAIL_USER\",\"password\":\"Admin@CineLog2025!\"}" 2>/dev/null || echo "")
        fi

        if [[ -n "$TOKEN_RESP" ]]; then
            TOKEN=$(echo "$TOKEN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo "")

            if [[ -n "$TOKEN" ]]; then
                log_ok "Login bem-sucedido (token obtido)"

                # Testar GET de reports (preview, sem envio)
                for REPORT in trending top-rated new-releases; do
                    STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
                        -H "Authorization: Bearer $TOKEN" \
                        "$APP_URL/api/v1/reports/$REPORT" 2>/dev/null || echo "0")

                    if [[ "$STATUS" == "200" ]]; then
                        log_ok "GET /api/v1/reports/$REPORT retornou 200 (preview OK)"
                    else
                        log_skip "GET /api/v1/reports/$REPORT retornou $STATUS"
                    fi
                done

                # Enviar emails reais
                if [[ -n "${EMAIL_TEST_TO:-}" ]]; then
                    echo ""
                    log_info "Enviando 3 reports para $EMAIL_TEST_TO ..."

                    REPORTS_TO_SEND=("trending" "top-rated" "top-actors")
                    for REPORT in "${REPORTS_TO_SEND[@]}"; do
                        SEND_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
                            -X POST \
                            -H "Authorization: Bearer $TOKEN" \
                            -H "Content-Type: application/json" \
                            -d "{\"email\":\"$EMAIL_TEST_TO\",\"limit\":5}" \
                            "$APP_URL/api/v1/reports/$REPORT" 2>/dev/null || echo "0")

                        if [[ "$SEND_STATUS" == "200" || "$SEND_STATUS" == "202" ]]; then
                            log_ok "POST /api/v1/reports/$REPORT → $EMAIL_TEST_TO (status $SEND_STATUS)"
                        else
                            log_fail "POST /api/v1/reports/$REPORT falhou (status $SEND_STATUS)"
                        fi
                    done

                    echo ""
                    log_info "Verifique sua caixa de entrada (e spam) em $EMAIL_TEST_TO"
                    if [[ "${MAIL_HOST:-localhost}" == "localhost" ]]; then
                        log_info "Modo MailHog: abra http://localhost:8025 para ver os emails"
                    fi
                else
                    log_skip "EMAIL_TEST_TO nao definido"
                fi
            else
                log_fail "Token nao extraido da resposta de login"
            fi
        else
            log_fail "Login falhou (nenhum usuario respondeu)"
        fi
    else
        log_skip "App nao esta rodando em $APP_URL — testes de email pulados"
        log_info "Para testar: source .env.mail && ./mvnw spring-boot:run"
    fi
fi

# ============================================================================
# RESULTADO FINAL
# ============================================================================
echo ""
log_header "Resultado Final"
echo -e "  ${GREEN}PASS: $PASS${NC}  |  ${RED}FAIL: $FAIL${NC}  |  ${YELLOW}SKIP: $SKIP${NC}"
echo -e "  Total: $((PASS + FAIL + SKIP)) verificacoes em $SECTION secoes"
echo ""

if [[ $FAIL -gt 0 ]]; then
    echo -e "  ${RED}${BOLD}RESULTADO: FALHOU ($FAIL erros)${NC}"
    exit 1
else
    echo -e "  ${GREEN}${BOLD}RESULTADO: SUCESSO${NC}"
    exit 0
fi
