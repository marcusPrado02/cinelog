#!/bin/bash

##############################################################################
# Script de Diagnóstico de E-mail
# 
# Verifica se o sistema de e-mail está funcionando e identifica problemas
##############################################################################

set -euo pipefail

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

print_section() {
    echo ""
    echo -e "${CYAN}▶ $1${NC}"
    echo -e "${CYAN}$(printf '─%.0s' {1..60})${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

print_header "DIAGNÓSTICO DO SISTEMA DE E-MAIL"

# 1. Verificar se API está rodando
print_section "1. Verificando API"
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    print_success "API está respondendo"
else
    print_error "API não está respondendo"
    print_info "Inicie a aplicação com: ./scripts/run-dev.sh"
    exit 1
fi

# 2. Obter status do serviço de e-mail
print_section "2. Verificando Serviço de E-mail"
HEALTH_JSON=$(curl -s http://localhost:8080/actuator/health)
MAIL_STATUS=$(echo "$HEALTH_JSON" | jq -r '.components.mail.status' 2>/dev/null || echo "UNKNOWN")

if [[ "$MAIL_STATUS" == "UP" ]]; then
    print_success "Serviço de e-mail está UP"
    
    MAIL_LOCATION=$(echo "$HEALTH_JSON" | jq -r '.components.mail.details.location' 2>/dev/null)
    echo "   Servidor: $MAIL_LOCATION"
    
elif [[ "$MAIL_STATUS" == "DOWN" ]]; then
    print_error "Serviço de e-mail está DOWN!"
    
    MAIL_LOCATION=$(echo "$HEALTH_JSON" | jq -r '.components.mail.details.location' 2>/dev/null)
    MAIL_ERROR=$(echo "$HEALTH_JSON" | jq -r '.components.mail.details.error' 2>/dev/null)
    
    echo ""
    echo -e "${RED}━━━ DETALHES DO ERRO ━━━${NC}"
    echo "Servidor: $MAIL_LOCATION"
    echo "Erro: $MAIL_ERROR"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
else
    print_warning "Status do e-mail desconhecido: $MAIL_STATUS"
fi

# 3. Verificar configuração atual
print_section "3. Configuração Atual de E-mail"

if [[ -f .env.mail ]]; then
    print_info "Usando .env.mail (SMTP Real)"
    echo ""
    
    MAIL_HOST=$(grep "MAIL_HOST" .env.mail | cut -d'=' -f2)
    MAIL_PORT=$(grep "MAIL_PORT" .env.mail | cut -d'=' -f2)
    MAIL_USER=$(grep "MAIL_USER" .env.mail | cut -d'=' -f2)
    
    echo "   Servidor: $MAIL_HOST:$MAIL_PORT"
    echo "   Usuário: $MAIL_USER"
    
    # Verifica se é Gmail
    if [[ "$MAIL_HOST" == *"gmail.com"* ]]; then
        echo ""
        print_warning "Usando Gmail - Verifique:"
        echo "   1. Senha de App (16 caracteres) está correta?"
        echo "   2. Senha de App foi gerada em: https://myaccount.google.com/apppasswords"
        echo "   3. NÃO use sua senha normal do Gmail!"
    fi
    
else
    print_info "Usando .env (MailHog - Desenvolvimento)"
    echo "   Servidor: localhost:1025"
    
    # Verifica se MailHog está rodando
    if curl -sf http://localhost:8025 > /dev/null 2>&1; then
        print_success "MailHog está rodando"
    else
        print_error "MailHog NÃO está rodando!"
        print_info "Inicie com: docker compose up -d mailhog"
    fi
fi

# 4. Sugestões de correção
if [[ "$MAIL_STATUS" == "DOWN" ]]; then
    print_section "4. Como Corrigir"
    echo ""
    
    if [[ "$MAIL_LOCATION" == *"gmail.com"* ]]; then
        echo -e "${YELLOW}━━━ PROBLEMA: Gmail não aceita as credenciais ━━━${NC}"
        echo ""
        echo "Soluções possíveis:"
        echo ""
        echo "A) RECONFIGURAR com Senha de App válida:"
        echo "   1. ./scripts/setup-email-real.sh"
        echo "   2. Escolha Gmail"
        echo "   3. Use Senha de App (16 caracteres)"
        echo "   4. Reinicie: ./scripts/run-dev.sh"
        echo ""
        echo "B) VOLTAR para MailHog (desenvolvimento):"
        echo "   1. mv .env.mail .env.mail.backup"
        echo "   2. docker compose up -d mailhog"
        echo "   3. Reinicie: ./scripts/run-dev.sh"
        echo ""
        echo "C) TESTAR SMTP manualmente:"
        echo "   telnet smtp.gmail.com 587"
        echo ""
        
    elif [[ -f .env.mail ]]; then
        echo -e "${YELLOW}━━━ PROBLEMA: SMTP não aceita as credenciais ━━━${NC}"
        echo ""
        echo "Soluções:"
        echo "   1. Verifique usuário e senha no .env.mail"
        echo "   2. Reconfigure: ./scripts/setup-email-real.sh"
        echo "   3. Ou volte para MailHog: mv .env.mail .env.mail.backup"
        
    else
        echo -e "${YELLOW}━━━ PROBLEMA: MailHog não está acessível ━━━${NC}"
        echo ""
        echo "Soluções:"
        echo "   1. docker compose up -d mailhog"
        echo "   2. Verifique: docker ps | grep mailhog"
        echo "   3. Teste: curl http://localhost:8025"
    fi
    
    echo ""
    print_warning "Após corrigir, reinicie a aplicação!"
fi

# 5. Teste rápido de envio (se estiver UP)
if [[ "$MAIL_STATUS" == "UP" ]]; then
    print_section "5. Teste Rápido"
    echo ""
    print_info "O serviço está UP e pronto para enviar e-mails!"
    echo ""
    echo "Execute:"
    echo "   ./scripts/test-email-reports.sh"
    echo ""
    
    if [[ -f .env.mail ]]; then
        print_warning "E-mails serão enviados DE VERDADE para marcus.prado@pitang.com"
    else
        print_info "E-mails serão capturados no MailHog: http://localhost:8025"
    fi
fi

# 6. Resumo
print_section "RESUMO"
echo ""

if [[ "$MAIL_STATUS" == "UP" ]]; then
    echo -e "${GREEN}✓ Sistema de e-mail funcionando corretamente${NC}"
    echo ""
    if [[ -f .env.mail ]]; then
        echo "Modo: SMTP Real ($MAIL_HOST)"
        echo "Destinatário: marcus.prado@pitang.com"
    else
        echo "Modo: MailHog (desenvolvimento)"
        echo "Visualize: http://localhost:8025"
    fi
    exit 0
else
    echo -e "${RED}✗ Sistema de e-mail COM PROBLEMAS${NC}"
    echo ""
    echo "Siga as instruções da seção 'Como Corrigir' acima"
    exit 1
fi
