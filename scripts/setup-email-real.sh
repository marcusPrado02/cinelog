#!/bin/bash

##############################################################################
# Script de Configuração para Envio de E-mails Reais
#
# Este script ajuda a configurar as variáveis de ambiente para enviar
# e-mails REAIS para marcus.prado@pitang.com
##############################################################################

set -euo pipefail

# Cores
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}   Configuração de E-mail Real - CineLog${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}DESTINATÁRIO:${NC} marcus.prado@pitang.com (fixo)"
echo -e "${YELLOW}REMETENTE:${NC} você define abaixo"
echo ""

# Menu de opções
echo "Escolha o servidor SMTP:"
echo "1) Gmail (pessoal)"
echo "2) SendGrid (profissional)"
echo "3) SMTP Corporativo (Pitang, etc)"
echo "4) Outro servidor SMTP"
echo "5) CANCELAR - voltar para MailHog"
echo ""
read -p "Opção [1-5]: " opcao

case $opcao in
    1)
        echo ""
        echo -e "${CYAN}═══ Configuração Gmail ═══${NC}"
        echo ""
        echo -e "${YELLOW}ATENÇÃO:${NC} Você precisa criar uma Senha de App primeiro!"
        echo "1. Acesse: https://myaccount.google.com/apppasswords"
        echo "2. Crie uma senha de app (16 caracteres)"
        echo "3. Use essa senha abaixo (não a senha normal da conta)"
        echo ""

        read -p "Seu e-mail Gmail (REMETENTE): " gmail_user
        read -sp "Senha de App (16 caracteres): " gmail_pass
        echo ""

        MAIL_HOST="smtp.gmail.com"
        MAIL_PORT="587"
        MAIL_USER="$gmail_user"
        MAIL_PASS="$gmail_pass"
        ;;

    2)
        echo ""
        echo -e "${CYAN}═══ Configuração SendGrid ═══${NC}"
        echo ""
        echo "1. Acesse: https://app.sendgrid.com/settings/api_keys"
        echo "2. Crie uma API Key com permissão de envio"
        echo "3. Use essa API Key abaixo"
        echo ""

        read -sp "SendGrid API Key: " sendgrid_key
        echo ""

        MAIL_HOST="smtp.sendgrid.net"
        MAIL_PORT="587"
        MAIL_USER="apikey"
        MAIL_PASS="$sendgrid_key"
        ;;

    3)
        echo ""
        echo -e "${CYAN}═══ Configuração SMTP Corporativo ═══${NC}"
        echo ""

        read -p "Servidor SMTP (ex: smtp.pitang.com): " smtp_host
        read -p "Porta SMTP [587]: " smtp_port
        smtp_port=${smtp_port:-587}
        read -p "E-mail REMETENTE (ex: noreply@pitang.com): " smtp_user
        read -sp "Senha do e-mail: " smtp_pass
        echo ""

        MAIL_HOST="$smtp_host"
        MAIL_PORT="$smtp_port"
        MAIL_USER="$smtp_user"
        MAIL_PASS="$smtp_pass"
        ;;

    4)
        echo ""
        echo -e "${CYAN}═══ Configuração SMTP Personalizado ═══${NC}"
        echo ""

        read -p "Servidor SMTP: " smtp_host
        read -p "Porta SMTP: " smtp_port
        read -p "Usuário/E-mail: " smtp_user
        read -sp "Senha: " smtp_pass
        echo ""

        MAIL_HOST="$smtp_host"
        MAIL_PORT="$smtp_port"
        MAIL_USER="$smtp_user"
        MAIL_PASS="$smtp_pass"
        ;;

    5)
        echo ""
        echo -e "${GREEN}Mantendo configuração MailHog (desenvolvimento)${NC}"
        echo "Os e-mails ficarão em: http://localhost:8025"
        exit 0
        ;;

    *)
        echo -e "${RED}Opção inválida!${NC}"
        exit 1
        ;;
esac

# Exporta as variáveis
export MAIL_HOST="$MAIL_HOST"
export MAIL_PORT="$MAIL_PORT"
export MAIL_USER="$MAIL_USER"
export MAIL_PASS="$MAIL_PASS"
export MAIL_AUTH="true"
export MAIL_STARTTLS="true"

# Salva em arquivo para reutilização
cat > .env.mail <<EOF
# Configuração de E-mail Real - Gerado em $(date)
# IMPORTANTE: Este arquivo contém credenciais sensíveis!
# Não commite no Git (já está no .gitignore)

export MAIL_HOST=$MAIL_HOST
export MAIL_PORT=$MAIL_PORT
export MAIL_USER=$MAIL_USER
export MAIL_PASS=$MAIL_PASS
export MAIL_AUTH=true
export MAIL_STARTTLS=true

# DESTINATÁRIO DOS TESTES: marcus.prado@pitang.com
# REMETENTE: $MAIL_USER
EOF

chmod 600 .env.mail

echo ""
echo -e "${GREEN}✓ Configuração salva!${NC}"
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Resumo da Configuração${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════${NC}"
echo -e "  SERVIDOR:     $MAIL_HOST:$MAIL_PORT"
echo -e "  REMETENTE:    $MAIL_USER"
echo -e "  DESTINATÁRIO: marcus.prado@pitang.com"
echo -e "${CYAN}═══════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Próximos passos:${NC}"
echo ""
echo "1. Reinicie a aplicação (run-dev.sh carrega .env.mail automaticamente):"
echo -e "   ${GREEN}./scripts/run-dev.sh${NC}"
echo ""
echo "2. Execute o script de testes:"
echo -e "   ${GREEN}./scripts/test-email-reports.sh${NC}"
echo ""
echo "3. Verifique a caixa de entrada: marcus.prado@pitang.com"
echo ""
echo -e "${YELLOW}REUTILIZANDO A CONFIGURAÇÃO:${NC}"
echo "O arquivo .env.mail será carregado automaticamente pelo run-dev.sh"
echo "Para usar com mvn diretamente:"
echo -e "   ${GREEN}source .env.mail && mvn spring-boot:run${NC}"
echo ""
