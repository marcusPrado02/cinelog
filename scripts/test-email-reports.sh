#!/bin/bash

##############################################################################
# Script para Testar Envio de Relatórios por E-mail
#
# IMPORTANTE - ONDE OS E-MAILS VÃO APARECER:
# ==========================================
# Por padrão, a aplicação está configurada para usar MailHog (localhost:1025)
# Os e-mails NÃO chegarão na sua caixa do Gmail/Outlook real!
#
# Os e-mails ficam capturados no MailHog e podem ser visualizados em:
#   → http://localhost:8025
#
# Para enviar e-mails REAIS (Gmail, etc), você precisa:
# 1. Configurar as variáveis de ambiente:
#    export MAIL_HOST=smtp.gmail.com
#    export MAIL_PORT=587
#    export MAIL_USER=seu-email@gmail.com
#    export MAIL_PASS=sua-senha-app
#    export MAIL_AUTH=true
#    export MAIL_STARTTLS=true
#
# 2. Para Gmail, criar uma "Senha de App" em:
#    https://myaccount.google.com/apppasswords
#
# 3. Reiniciar a aplicação com as novas variáveis
##############################################################################

set -euo pipefail

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configurações
API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
TEST_EMAIL="${TEST_EMAIL:-marcus.prado@pitang.com}"
MAILHOG_URL="http://localhost:8025"

# Contadores
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

##############################################################################
# Funções auxiliares
##############################################################################

print_header() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_section() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
    echo -e "${BLUE}$(printf '─%.0s' {1..60})${NC}"
}

print_test() {
    echo -e "${YELLOW}[TEST] $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASSED_TESTS++))
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
    ((FAILED_TESTS++))
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

check_prerequisites() {
    print_section "Verificando Pré-requisitos"

    # Verifica se curl está instalado
    if ! command -v curl &> /dev/null; then
        print_error "curl não está instalado. Instale com: sudo apt-get install curl"
        exit 1
    fi
    print_success "curl instalado"

    # Verifica se jq está instalado
    if ! command -v jq &> /dev/null; then
        print_error "jq não está instalado. Instale com: sudo apt-get install jq"
        exit 1
    fi
    print_success "jq instalado"

    # Verifica se a API está respondendo
    if ! curl -sf "${API_BASE_URL}/health" > /dev/null 2>&1; then
        print_error "API não está respondendo em ${API_BASE_URL}"
        print_info "Inicie a aplicação primeiro com: mvn spring-boot:run"
        exit 1
    fi
    print_success "API respondendo em ${API_BASE_URL}"

    # Verifica se MailHog está rodando
    if ! curl -sf "${MAILHOG_URL}" > /dev/null 2>&1; then
        print_error "MailHog não está rodando em ${MAILHOG_URL}"
        print_info "Inicie o MailHog com: docker compose up -d mailhog"
        print_info "OU se preferir enviar e-mails reais, configure as variáveis MAIL_* (veja comentários no topo do script)"
        exit 1
    fi
    print_success "MailHog rodando em ${MAILHOG_URL}"
}

authenticate() {
    print_section "Autenticação"

    # Tenta autenticar como usuário comum
    print_test "Autenticando usuário comum (marcus)..."
    USER_RESPONSE=$(curl -s -X POST "${API_BASE_URL}/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "marcus",
            "password": "Marcus@CineLog2025!"
        }')

    USER_TOKEN=$(echo "$USER_RESPONSE" | jq -r '.token // empty')

    if [ -z "$USER_TOKEN" ]; then
        print_error "Falha na autenticação do usuário comum"
        echo "Response: $USER_RESPONSE"
        exit 1
    fi
    print_success "Token do usuário obtido"

    # Tenta autenticar como admin
    print_test "Autenticando admin..."
    ADMIN_RESPONSE=$(curl -s -X POST "${API_BASE_URL}/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "admin",
            "password": "Admin@CineLog2025!"
        }')

    ADMIN_TOKEN=$(echo "$ADMIN_RESPONSE" | jq -r '.token // empty')

    if [ -z "$ADMIN_TOKEN" ]; then
        print_error "Falha na autenticação do admin"
        echo "Response: $ADMIN_RESPONSE"
        exit 1
    fi
    print_success "Token do admin obtido"
}

test_endpoint() {
    local method=$1
    local endpoint=$2
    local token=$3
    local description=$4
    local expected_status=$5
    local data=${6:-}

    ((TOTAL_TESTS++))
    print_test "$description"

    local curl_cmd="curl -s -w '\n%{http_code}' -X $method"
    curl_cmd="$curl_cmd '$API_BASE_URL$endpoint'"
    curl_cmd="$curl_cmd -H 'Authorization: Bearer $token'"
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"

    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    local response=$(eval $curl_cmd)
    local status_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)

    if [[ "$status_code" == "$expected_status" ]]; then
        print_success "Status $status_code - OK"
        if [ -n "$body" ] && [ "$body" != "null" ]; then
            echo "   Response: $(echo "$body" | jq -c . 2>/dev/null || echo "$body")"
        fi
    else
        print_error "Esperado $expected_status, recebido $status_code"
        if [ -n "$body" ]; then
            echo "   Response: $body"
        fi
    fi
}

##############################################################################
# Testes de Relatórios por E-mail
##############################################################################

test_user_reports() {
    print_section "Relatórios de Usuário (Requerem Autenticação)"

    # Weekly Digest
    test_endpoint "GET" "/reports/weekly-digest/preview" "$USER_TOKEN" \
        "Preview do Weekly Digest" "200"

    test_endpoint "POST" "/reports/weekly-digest/send?email=${TEST_EMAIL}" "$USER_TOKEN" \
        "Envio do Weekly Digest para ${TEST_EMAIL}" "202"

    # Top Rated
    test_endpoint "GET" "/reports/top-rated/preview?period=30" "$USER_TOKEN" \
        "Preview do Top Rated (30 dias)" "200"

    test_endpoint "POST" "/reports/top-rated/send?email=${TEST_EMAIL}&period=30" "$USER_TOKEN" \
        "Envio do Top Rated para ${TEST_EMAIL}" "202"

    # Recommendations
    test_endpoint "GET" "/reports/recommendations/preview" "$USER_TOKEN" \
        "Preview de Recomendações" "200"

    test_endpoint "POST" "/reports/recommendations/send?email=${TEST_EMAIL}" "$USER_TOKEN" \
        "Envio de Recomendações para ${TEST_EMAIL}" "202"

    # Trending
    test_endpoint "GET" "/reports/trending/preview?period=7" "$USER_TOKEN" \
        "Preview de Trending (7 dias)" "200"

    test_endpoint "POST" "/reports/trending/send?email=${TEST_EMAIL}&period=7" "$USER_TOKEN" \
        "Envio de Trending para ${TEST_EMAIL}" "202"

    # New Releases
    test_endpoint "GET" "/reports/new-releases/preview" "$USER_TOKEN" \
        "Preview de New Releases" "200"

    test_endpoint "POST" "/reports/new-releases/send?email=${TEST_EMAIL}" "$USER_TOKEN" \
        "Envio de New Releases para ${TEST_EMAIL}" "202"

    # Genre Spotlight
    test_endpoint "GET" "/reports/genre-spotlight/preview?genreId=28" "$USER_TOKEN" \
        "Preview de Genre Spotlight (Ação)" "200"

    test_endpoint "POST" "/reports/genre-spotlight/send?email=${TEST_EMAIL}&genreId=28" "$USER_TOKEN" \
        "Envio de Genre Spotlight para ${TEST_EMAIL}" "202"

    # Top Actors
    test_endpoint "GET" "/reports/top-actors/preview" "$USER_TOKEN" \
        "Preview de Top Actors" "200"

    test_endpoint "POST" "/reports/top-actors/send?email=${TEST_EMAIL}" "$USER_TOKEN" \
        "Envio de Top Actors para ${TEST_EMAIL}" "202"
}

test_admin_reports() {
    print_section "Relatórios Admin (Requerem Perfil ADMIN)"

    # Platform Report
    test_endpoint "GET" "/admin/reports/platform/preview?period=30" "$ADMIN_TOKEN" \
        "Preview de Platform Report (30 dias)" "200"

    test_endpoint "POST" "/admin/reports/platform/send?email=${TEST_EMAIL}&period=30" "$ADMIN_TOKEN" \
        "Envio de Platform Report para ${TEST_EMAIL}" "202"

    # Send to All Users
    test_endpoint "POST" "/admin/reports/send-to-all?reportType=WEEKLY_DIGEST" "$ADMIN_TOKEN" \
        "Envio de Weekly Digest para todos os usuários" "202"
}

show_mailhog_info() {
    print_section "Visualizando os E-mails Enviados"

    print_info "📧 DESTINATÁRIO: ${TEST_EMAIL}"
    print_info ""

    # Detecta se está usando MailHog ou SMTP real
    if curl -sf "${MAILHOG_URL}" > /dev/null 2>&1; then
        print_info "🔴 MODO: Desenvolvimento (MailHog)"
        print_info "   Os e-mails ficam CAPTURADOS localmente e NÃO chegam no destinatário real."
        print_info ""
        print_info "   Visualize os e-mails em: ${MAILHOG_URL}"
        print_info "   • Design completo (templates HTML Thymeleaf)"
        print_info "   • Imagens, estilos CSS, layout responsivo"
        print_info "   • Dados reais da aplicação"
        print_info ""
        print_info "🚀 Para enviar e-mails REAIS para ${TEST_EMAIL}:"
        print_info "   1. Configure as variáveis MAIL_* (veja topo do script)"
        print_info "   2. Reinicie a aplicação"
        print_info "   3. Execute este script novamente"
    else
        print_info "🚀 MODO: Produção (SMTP Real)"
        print_info "   Os e-mails foram ENVIADOS de verdade!"
        print_info "   Verifique a caixa de entrada: ${TEST_EMAIL}"
        print_info ""
        print_info "   REMETENTE configurado via MAIL_USER"
        print_info "   Os e-mails podem demorar alguns minutos para chegar."
    fi
    print_header "RESUMO DOS TESTES"

    echo -e "${BLUE}Total de testes: ${TOTAL_TESTS}${NC}"
    echo -e "${GREEN}Testes passaram: ${PASSED_TESTS}${NC}"
    echo -e "${RED}Testes falharam: ${FAILED_TESTS}${NC}"
    echo ""

    if [ "$FAILED_TESTS" -eq 0 ]; then
        echo -e "${GREEN}✓ Todos os testes passaram!${NC}"
        return 0
    else
        echo -e "${RED}✗ Alguns testes falharam${NC}"
        return 1
    fi
}

##############################################################################
# Execução Principal
##############################################################################

main() {
    print_header "TESTE DE ENVIO DE RELATÓRIOS POR E-MAIL"

    echo -e "${YELLOW}Destinatário dos e-mails: ${TEST_EMAIL}${NC}"
    echo -e "${YELLOW}Visualizar e-mails em: ${MAILHOG_URL}${NC}"
    echo ""

    check_prerequisites
    authenticate
    test_user_reports
    test_admin_reports
    show_mailhog_info

    echo ""
    print_summary
}

# Executa o script
main
