#!/bin/bash

##############################################################################
# Exemplo Prático: Fluxo Completo de Teste de E-mails
#
# Este script demonstra o fluxo completo desde a configuração até o teste
# Pode ser usado como referência ou executado diretamente
##############################################################################

set -euo pipefail

# Cores
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}   Exemplo Prático: Teste de E-mails CineLog${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Menu de escolha
echo "Qual fluxo você quer testar?"
echo ""
echo "1) Desenvolvimento (MailHog) - E-mails capturados localmente"
echo "2) Produção (SMTP Real) - E-mails chegam em marcus.prado@pitang.com"
echo ""
read -p "Escolha [1-2]: " choice

case $choice in
    1)
        echo ""
        echo -e "${CYAN}═══ MODO DESENVOLVIMENTO (MailHog) ═══${NC}"
        echo ""

        # Passo 1: Verificar MailHog
        echo -e "${YELLOW}[Passo 1/4]${NC} Verificando MailHog..."
        if ! docker ps | grep -q mailhog; then
            echo "Iniciando MailHog..."
            docker compose up -d mailhog
            sleep 2
        else
            echo "✓ MailHog já está rodando"
        fi

        # Passo 2: Parar aplicação se estiver rodando
        echo ""
        echo -e "${YELLOW}[Passo 2/4]${NC} Preparando ambiente..."
        if pgrep -f "cinelog.*spring-boot" > /dev/null; then
            echo "⚠️  Aplicação já está rodando. Deseja reiniciar? [s/N]"
            read -r restart
            if [[ "$restart" =~ ^[Ss]$ ]]; then
                pkill -f "cinelog.*spring-boot"
                sleep 2
            fi
        fi

        # Passo 3: Iniciar aplicação
        echo ""
        echo -e "${YELLOW}[Passo 3/4]${NC} Iniciando aplicação..."
        echo "Comando: ./scripts/run-dev.sh"
        echo ""
        echo "A aplicação será iniciada em modo DESENVOLVIMENTO (MailHog)"
        echo "Pressione Ctrl+C após ver 'Started CinelogApplication' para continuar"
        echo ""
        read -p "Pressione ENTER para iniciar..."
        ./scripts/run-dev.sh &
        APP_PID=$!

        # Aguarda aplicação iniciar
        echo "Aguardando aplicação iniciar..."
        sleep 15

        # Passo 4: Executar testes
        echo ""
        echo -e "${YELLOW}[Passo 4/4]${NC} Executando testes de e-mail..."
        ./scripts/test-email-reports.sh

        # Resultado
        echo ""
        echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  TESTE CONCLUÍDO (Modo Desenvolvimento)${NC}"
        echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
        echo ""
        echo "📧 E-mails capturados no MailHog"
        echo "🌐 Abra: http://localhost:8025"
        echo ""
        echo "Você verá 17 e-mails enviados para marcus.prado@pitang.com"
        echo "com templates HTML profissionais e dados reais!"
        echo ""
        ;;

    2)
        echo ""
        echo -e "${CYAN}═══ MODO PRODUÇÃO (SMTP Real) ═══${NC}"
        echo ""

        # Passo 1: Verificar .env.mail
        echo -e "${YELLOW}[Passo 1/5]${NC} Verificando configuração de e-mail..."
        if [[ ! -f .env.mail ]]; then
            echo "Arquivo .env.mail não encontrado!"
            echo "Executando assistente de configuração..."
            echo ""
            ./scripts/setup-email-real.sh
        else
            echo "✓ Configuração encontrada (.env.mail)"
            echo ""
            echo "Remetente configurado:"
            grep "MAIL_USER" .env.mail | cut -d'=' -f2
            echo ""
            echo "Deseja reconfigurar? [s/N]"
            read -r reconfig
            if [[ "$reconfig" =~ ^[Ss]$ ]]; then
                ./scripts/setup-email-real.sh
            fi
        fi

        # Passo 2: Parar aplicação se estiver rodando
        echo ""
        echo -e "${YELLOW}[Passo 2/5]${NC} Preparando ambiente..."
        if pgrep -f "cinelog.*spring-boot" > /dev/null; then
            echo "⚠️  Aplicação já está rodando. Deseja reiniciar? [s/N]"
            read -r restart
            if [[ "$restart" =~ ^[Ss]$ ]]; then
                pkill -f "cinelog.*spring-boot"
                sleep 2
            fi
        fi

        # Passo 3: Iniciar aplicação
        echo ""
        echo -e "${YELLOW}[Passo 3/5]${NC} Iniciando aplicação com SMTP real..."
        echo "Comando: ./scripts/run-dev.sh (detecta .env.mail automaticamente)"
        echo ""
        echo "A aplicação será iniciada em modo PRODUÇÃO (SMTP Real)"
        echo "Pressione Ctrl+C após ver 'Started CinelogApplication' para continuar"
        echo ""
        read -p "Pressione ENTER para iniciar..."
        ./scripts/run-dev.sh &
        APP_PID=$!

        # Aguarda aplicação iniciar
        echo "Aguardando aplicação iniciar..."
        sleep 15

        # Passo 4: Executar testes
        echo ""
        echo -e "${YELLOW}[Passo 4/5]${NC} Executando testes de e-mail..."
        ./scripts/test-email-reports.sh

        # Passo 5: Aguardar e-mails
        echo ""
        echo -e "${YELLOW}[Passo 5/5]${NC} Aguardando envio dos e-mails..."
        echo "E-mails são processados de forma assíncrona..."
        sleep 10

        # Resultado
        echo ""
        echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}  TESTE CONCLUÍDO (Modo Produção)${NC}"
        echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
        echo ""
        echo "📧 E-mails ENVIADOS DE VERDADE!"
        echo "📬 Destinatário: marcus.prado@pitang.com"
        echo ""
        echo "⏱️  Os e-mails podem demorar alguns minutos para chegar"
        echo "📥 Verifique a caixa de entrada e também a pasta de SPAM"
        echo ""
        echo "Você receberá 17 e-mails com:"
        echo "  • Weekly Digest"
        echo "  • Top Rated"
        echo "  • Recommendations"
        echo "  • Trending"
        echo "  • New Releases"
        echo "  • Genre Spotlight"
        echo "  • Top Actors"
        echo "  • Platform Report"
        echo "  • E mais!"
        echo ""
        ;;

    *)
        echo "Opção inválida!"
        exit 1
        ;;
esac

# Pergunta se quer parar a aplicação
echo ""
echo "A aplicação ainda está rodando (PID: $APP_PID)"
echo "Deseja parar a aplicação? [S/n]"
read -r stop_app
if [[ ! "$stop_app" =~ ^[Nn]$ ]]; then
    kill $APP_PID 2>/dev/null || true
    echo "✓ Aplicação parada"
else
    echo "Aplicação continua rodando em background (PID: $APP_PID)"
    echo "Para parar: kill $APP_PID"
fi

echo ""
echo -e "${GREEN}Teste concluído!${NC}"
