#!/usr/bin/env bash
# ============================================================================
# setup-real-email.sh — Configura email real para receber reports do CineLog
# ============================================================================
#
# Este script configura o Spring Mail para usar um SMTP real em vez do MailHog.
# Os emails de report (trending, weekly digest, etc.) chegam na sua caixa.
#
# OPCOES DE SMTP:
#   1. Gmail (App Password)     — gratuito, ate 500 emails/dia
#   2. Mailtrap (sandbox)       — gratuito, inbox de teste
#   3. Outlook/Hotmail          — gratuito, ate 300 emails/dia
#   4. Custom SMTP              — qualquer servidor
#
# USO:
#   ./scripts/setup-real-email.sh           # Modo interativo
#   source ./scripts/setup-real-email.sh    # Exporta variaveis para a sessao
#
# APOS CONFIGURAR:
#   # Iniciar a app com as variaveis de email:
#   ./mvnw spring-boot:run
#
#   # Enviar email de teste:
#   EMAIL_TEST_TO=seu@email.com ./scripts/test-all-changes.sh --email
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

ENV_FILE="$(cd "$(dirname "$0")/.." && pwd)/.env.email"

echo -e "${BOLD}${CYAN}"
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║         CineLog — Configuracao de Email Real                ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "Escolha o provedor SMTP:\n"
echo -e "  ${BOLD}1${NC}) Gmail (requer App Password — https://myaccount.google.com/apppasswords)"
echo -e "  ${BOLD}2${NC}) Mailtrap (sandbox — https://mailtrap.io)"
echo -e "  ${BOLD}3${NC}) Outlook / Hotmail"
echo -e "  ${BOLD}4${NC}) Custom SMTP"
echo ""
read -rp "Opcao [1-4]: " CHOICE

case "$CHOICE" in
    1)
        MAIL_HOST="smtp.gmail.com"
        MAIL_PORT="587"
        MAIL_AUTH="true"
        MAIL_STARTTLS="true"
        echo ""
        echo -e "${YELLOW}IMPORTANTE: Para Gmail, voce precisa de uma App Password.${NC}"
        echo -e "1. Va em https://myaccount.google.com/apppasswords"
        echo -e "2. Crie uma senha de app para 'Mail'"
        echo -e "3. Use essa senha (16 caracteres) abaixo"
        echo ""
        read -rp "Seu email Gmail: " MAIL_USER
        read -rsp "App Password (16 chars, sem espacos): " MAIL_PASS
        echo ""
        ;;
    2)
        MAIL_HOST="sandbox.smtp.mailtrap.io"
        MAIL_PORT="2525"
        MAIL_AUTH="true"
        MAIL_STARTTLS="true"
        echo ""
        echo -e "${YELLOW}Acesse https://mailtrap.io > Inboxes > SMTP Settings${NC}"
        echo ""
        read -rp "Mailtrap Username: " MAIL_USER
        read -rsp "Mailtrap Password: " MAIL_PASS
        echo ""
        ;;
    3)
        MAIL_HOST="smtp-mail.outlook.com"
        MAIL_PORT="587"
        MAIL_AUTH="true"
        MAIL_STARTTLS="true"
        echo ""
        read -rp "Seu email Outlook/Hotmail: " MAIL_USER
        read -rsp "Sua senha: " MAIL_PASS
        echo ""
        ;;
    4)
        echo ""
        read -rp "SMTP Host: " MAIL_HOST
        read -rp "SMTP Port: " MAIL_PORT
        read -rp "Requer auth? (true/false): " MAIL_AUTH
        read -rp "Requer STARTTLS? (true/false): " MAIL_STARTTLS
        read -rp "Username: " MAIL_USER
        read -rsp "Password: " MAIL_PASS
        echo ""
        ;;
    *)
        echo -e "${RED}Opcao invalida.${NC}"
        exit 1
        ;;
esac

read -rp "Email para receber os reports de teste: " EMAIL_TEST_TO

# Salvar em .env.email
cat > "$ENV_FILE" << EOF
# CineLog Email Configuration — gerado em $(date '+%Y-%m-%d %H:%M:%S')
# NUNCA commitar este arquivo (ja esta no .gitignore)
export MAIL_HOST="$MAIL_HOST"
export MAIL_PORT="$MAIL_PORT"
export MAIL_USER="$MAIL_USER"
export MAIL_PASS="$MAIL_PASS"
export MAIL_AUTH="$MAIL_AUTH"
export MAIL_STARTTLS="$MAIL_STARTTLS"
export EMAIL_TEST_TO="$EMAIL_TEST_TO"
EOF

# Exportar para a sessao atual
export MAIL_HOST MAIL_PORT MAIL_USER MAIL_PASS MAIL_AUTH MAIL_STARTTLS EMAIL_TEST_TO

echo ""
echo -e "${GREEN}${BOLD}Configuracao salva em .env.email${NC}"
echo ""
echo -e "Para usar:"
echo -e "  ${CYAN}# Carregar variaveis:${NC}"
echo -e "  source .env.email"
echo ""
echo -e "  ${CYAN}# Iniciar a app:${NC}"
echo -e "  ./mvnw spring-boot:run"
echo ""
echo -e "  ${CYAN}# Enviar email de teste:${NC}"
echo -e "  ./scripts/test-all-changes.sh --email"
echo ""
echo -e "  ${CYAN}# Enviar report especifico:${NC}"
echo -e "  curl -X POST http://localhost:8080/api/v1/reports/trending \\"
echo -e "    -H 'Authorization: Bearer <TOKEN>' \\"
echo -e "    -H 'Content-Type: application/json' \\"
echo -e "    -d '{\"email\":\"$EMAIL_TEST_TO\",\"limit\":5}'"
echo ""

# Verificar se .env.email esta no .gitignore
if [[ -f "$(cd "$(dirname "$0")/.." && pwd)/.gitignore" ]]; then
    if ! grep -q '.env.email' "$(cd "$(dirname "$0")/.." && pwd)/.gitignore" 2>/dev/null; then
        echo ".env.email" >> "$(cd "$(dirname "$0")/.." && pwd)/.gitignore"
        echo -e "${YELLOW}Adicionado .env.email ao .gitignore${NC}"
    fi
fi

echo -e "${GREEN}Pronto! Execute 'source .env.email' e inicie a app.${NC}"
