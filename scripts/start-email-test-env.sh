#!/bin/bash

##############################################################################
# Script para Iniciar Ambiente de Teste de E-mails
#
# Este script garante que todos os serviços necessários estejam rodando:
# - MailHog (captura de e-mails)
# - MySQL (banco de dados)
# - Redis (cache)
# - API CineLog
##############################################################################

set -euo pipefail

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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
    echo -e "${BLUE}▶ $1${NC}"
    echo -e "${BLUE}$(printf '─%.0s' {1..60})${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

##############################################################################
# Verificações e Inicializações
##############################################################################

print_header "INICIANDO AMBIENTE DE TESTE DE E-MAILS"

# 1. Verificar Docker
print_section "Verificando Docker"
if ! command -v docker &> /dev/null; then
    print_error "Docker não está instalado!"
    exit 1
fi
print_success "Docker instalado"

if ! docker ps &> /dev/null; then
    print_error "Docker daemon não está rodando!"
    print_info "Inicie o Docker e tente novamente"
    exit 1
fi
print_success "Docker rodando"

# 2. Verificar docker-compose.yml
print_section "Verificando Arquivos de Configuração"
if [[ ! -f "docker-compose.yml" ]]; then
    print_error "docker-compose.yml não encontrado!"
    print_info "Execute este script da raiz do projeto"
    exit 1
fi
print_success "docker-compose.yml encontrado"

# 3. Iniciar MailHog
print_section "Iniciando MailHog"
if docker ps | grep -q mailhog; then
    print_success "MailHog já está rodando"
else
    print_info "Iniciando MailHog..."
    docker compose up -d mailhog
    sleep 3

    if docker ps | grep -q mailhog; then
        print_success "MailHog iniciado com sucesso"
        print_info "Interface: http://localhost:8025"
    else
        print_error "Falha ao iniciar MailHog"
        docker compose logs mailhog
        exit 1
    fi
fi

# 4. Verificar MySQL
print_section "Verificando MySQL"
if docker ps | grep -q mysql; then
    print_success "MySQL já está rodando"
else
    print_warning "MySQL não está rodando"
    print_info "Iniciando MySQL..."
    docker compose up -d db
    print_info "Aguardando MySQL inicializar (30s)..."
    sleep 30
fi

# 5. Verificar Redis
print_section "Verificando Redis"
if docker ps | grep -q redis; then
    print_success "Redis já está rodando"
else
    print_warning "Redis não está rodando"
    print_info "Iniciando Redis..."
    docker compose up -d redis
    sleep 3
fi

# 6. Verificar se API está rodando
print_section "Verificando API CineLog"
if curl -sf http://localhost:8080/api/v1/health > /dev/null 2>&1; then
    print_success "API já está rodando"
    API_RUNNING=true
else
    print_warning "API não está rodando"
    API_RUNNING=false
fi

# 7. Perguntar se quer iniciar a API
if [[ "$API_RUNNING" == "false" ]]; then
    echo ""
    echo -e "${YELLOW}A API precisa estar rodando para testar os e-mails.${NC}"
    echo ""
    echo "Como você quer iniciar a aplicação?"
    echo "1) ./scripts/run-dev.sh (recomendado - detecta .env.mail automaticamente)"
    echo "2) mvn spring-boot:run (modo básico)"
    echo "3) Eu vou iniciar manualmente"
    echo "4) CANCELAR"
    echo ""
    read -p "Opção [1-4]: " start_option

    case $start_option in
        1)
            echo ""
            print_info "Iniciando aplicação com run-dev.sh..."
            echo ""

            # Verifica se .env.mail existe
            if [[ -f .env.mail ]]; then
                print_info "Detectado .env.mail - Usará SMTP real"
                grep "MAIL_USER" .env.mail | cut -d'=' -f2 | xargs -I {} echo "   Remetente: {}"
            else
                print_info "Usará MailHog (desenvolvimento)"
            fi

            echo ""
            print_info "A aplicação será iniciada em uma nova janela de terminal"
            print_info "Aguarde até ver 'Started CinelogApplication'"
            echo ""
            read -p "Pressione ENTER para continuar..."

            # Inicia em background
            ./scripts/run-dev.sh > logs/run-dev.log 2>&1 &
            APP_PID=$!

            echo ""
            print_info "Aplicação iniciando... PID: $APP_PID"
            print_info "Aguardando inicialização (isso pode levar 30-60 segundos)..."

            # Aguarda a API responder
            for i in {1..60}; do
                if curl -sf http://localhost:8080/api/v1/health > /dev/null 2>&1; then
                    print_success "API iniciada com sucesso!"
                    break
                fi
                echo -n "."
                sleep 1
            done
            echo ""

            if ! curl -sf http://localhost:8080/api/v1/health > /dev/null 2>&1; then
                print_error "API não respondeu no tempo esperado"
                print_info "Verifique os logs: tail -f logs/run-dev.log"
                exit 1
            fi
            ;;

        2)
            echo ""
            print_info "Iniciando aplicação com mvn..."
            echo ""
            print_warning "ATENÇÃO: Este método não carrega .env.mail automaticamente!"
            print_info "Se quiser usar SMTP real, execute: source .env.mail && mvn spring-boot:run"
            echo ""
            read -p "Pressione ENTER para continuar com MailHog..."

            mvn spring-boot:run > logs/mvn-run.log 2>&1 &
            APP_PID=$!

            print_info "Aplicação iniciando... PID: $APP_PID"
            print_info "Aguardando inicialização (30-60 segundos)..."

            for i in {1..60}; do
                if curl -sf http://localhost:8080/api/v1/health > /dev/null 2>&1; then
                    print_success "API iniciada com sucesso!"
                    break
                fi
                echo -n "."
                sleep 1
            done
            echo ""

            if ! curl -sf http://localhost:8080/api/v1/health > /dev/null 2>&1; then
                print_error "API não respondeu no tempo esperado"
                print_info "Verifique os logs: tail -f logs/mvn-run.log"
                exit 1
            fi
            ;;

        3)
            echo ""
            print_info "OK, inicie a aplicação manualmente"
            print_info "Recomendado: ./scripts/run-dev.sh"
            echo ""
            read -p "Pressione ENTER após iniciar a aplicação..."

            if ! curl -sf http://localhost:8080/api/v1/health > /dev/null 2>&1; then
                print_error "API ainda não está respondendo"
                exit 1
            fi
            print_success "API detectada!"
            ;;

        4)
            print_info "Operação cancelada"
            exit 0
            ;;

        *)
            print_error "Opção inválida"
            exit 1
            ;;
    esac
fi

##############################################################################
# Resumo Final
##############################################################################

print_header "AMBIENTE PRONTO PARA TESTES!"

echo ""
echo -e "${GREEN}✓ Todos os serviços estão rodando:${NC}"
echo ""
echo "  🐳 MailHog:       http://localhost:8025"
echo "  🐬 MySQL:         localhost:3306"
echo "  🔴 Redis:         localhost:6379"
echo "  🚀 API CineLog:   http://localhost:8080"
echo ""

# Detecta modo de e-mail
if [[ -f .env.mail ]]; then
    echo -e "${CYAN}📧 Modo de E-mail: SMTP Real${NC}"
    grep "MAIL_USER" .env.mail | cut -d'=' -f2 | xargs -I {} echo "   Remetente: {}"
    grep "MAIL_HOST" .env.mail | cut -d'=' -f2 | xargs -I {} echo "   Servidor: {}"
    echo "   Destinatário: marcus.prado@pitang.com"
    echo ""
    echo -e "${YELLOW}⚠️  E-mails serão ENVIADOS DE VERDADE!${NC}"
else
    echo -e "${CYAN}📧 Modo de E-mail: MailHog (Desenvolvimento)${NC}"
    echo "   E-mails serão capturados localmente"
    echo "   Visualize em: http://localhost:8025"
fi

echo ""
echo -e "${GREEN}Próximo passo:${NC}"
echo "  ./scripts/test-email-reports.sh"
echo ""
