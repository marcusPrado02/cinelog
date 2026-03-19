#!/bin/bash

##############################################################################
# Script para Resetar MFA (TOTP) de Usuários no Keycloak
# 
# Este script fornece instruções para resetar o MFA quando:
# - Você perdeu acesso ao app authenticator
# - Removeu o código do Microsoft Authenticator
# - O QR code não aparece mais no login
# - Precisa reconfigurar o TOTP do zero
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

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

print_header "RESET DE MFA (TOTP) - KEYCLOAK"

echo ""
echo -e "${YELLOW}Este guia ajuda a resetar o MFA quando:${NC}"
echo "  • Você perdeu o acesso ao Microsoft Authenticator"
echo "  • Removeu o código mas ele ainda está configurado"
echo "  • O QR code não aparece mais no login"
echo "  • Precisa reconfigurar o TOTP do zero"
echo ""

# Menu de opções
echo "Escolha o método de reset:"
echo ""
echo "1) Via Admin Console do Keycloak (Recomendado)"
echo "2) Via API REST do Keycloak"
echo "3) Via Script SQL Direto (Emergência)"
echo "4) SAIR"
echo ""
read -p "Opção [1-4]: " choice

case $choice in
    1)
        print_section "MÉTODO 1: Admin Console do Keycloak"
        echo ""
        
        print_info "1. Verifique se o Keycloak está rodando:"
        echo "   docker ps | grep keycloak"
        echo ""
        
        print_info "2. Acesse o Admin Console:"
        echo "   URL: http://localhost:8180/admin/"
        echo "   Usuário: admin"
        echo "   Senha: admin"
        echo ""
        
        print_info "3. Navegue até o usuário:"
        echo "   • Sidebar → Users"
        echo "   • Busque por: marcus (ou o username desejado)"
        echo "   • Clique no usuário"
        echo ""
        
        print_info "4. Remova a credencial TOTP:"
        echo "   • Aba: Credentials"
        echo "   • Procure por: OTP (ou TOTP)"
        echo "   • Clique em: Delete (ícone de lixeira)"
        echo "   • Confirme a remoção"
        echo ""
        
        print_info "5. (OPCIONAL) Force reconfiguração no próximo login:"
        echo "   • Aba: Details ou Required Actions"
        echo "   • Marque: Configure OTP / Configure TOTP"
        echo "   • Salve"
        echo ""
        
        print_success "MFA removido! Próximos passos:"
        echo ""
        echo "Agora tente fazer login novamente:"
        echo "1. Acesse: http://localhost:8180/realms/cinelog/account"
        echo "2. Username: marcus"
        echo "3. Password: Marcus@CineLog2025!"
        echo "4. O sistema vai mostrar o QR code novamente!"
        echo "5. Escaneie com Microsoft Authenticator"
        echo "6. Insira o código de 6 dígitos"
        echo "7. MFA reconfigurado! ✅"
        ;;
        
    2)
        print_section "MÉTODO 2: API REST do Keycloak"
        echo ""
        
        print_warning "Este método requer jq instalado: sudo apt install jq"
        echo ""
        
        print_info "1. Obtenha token de admin:"
        cat << 'EOF'
ADMIN_TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  | jq -r '.access_token')

echo "Token obtido: ${ADMIN_TOKEN:0:20}..."
EOF
        
        echo ""
        print_info "2. Obtenha ID do usuário marcus:"
        cat << 'EOF'
USER_ID=$(curl -s -X GET \
  "http://localhost:8180/admin/realms/cinelog/users?username=marcus" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq -r '.[0].id')

echo "User ID: $USER_ID"
EOF
        
        echo ""
        print_info "3. Liste credenciais do usuário:"
        cat << 'EOF'
curl -s -X GET \
  "http://localhost:8180/admin/realms/cinelog/users/$USER_ID/credentials" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq '.[] | select(.type == "otp") | {id, type, userLabel}'
EOF
        
        echo ""
        print_info "4. Delete a credencial TOTP:"
        cat << 'EOF'
CREDENTIAL_ID=$(curl -s -X GET \
  "http://localhost:8180/admin/realms/cinelog/users/$USER_ID/credentials" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq -r '.[] | select(.type == "otp") | .id')

curl -X DELETE \
  "http://localhost:8180/admin/realms/cinelog/users/$USER_ID/credentials/$CREDENTIAL_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

echo "Credencial TOTP removida!"
EOF
        
        echo ""
        print_success "MFA removido via API!"
        ;;
        
    3)
        print_section "MÉTODO 3: SQL Direto (EMERGÊNCIA)"
        echo ""
        
        print_error "⚠️  CUIDADO: Este método deve ser usado apenas em EMERGÊNCIA!"
        print_warning "Você pode corromper o banco do Keycloak se errar"
        echo ""
        
        read -p "Tem certeza que quer continuar? [s/N]: " confirm
        if [[ ! "$confirm" =~ ^[Ss]$ ]]; then
            print_info "Operação cancelada. Use o Método 1 (Admin Console)."
            exit 0
        fi
        
        echo ""
        print_info "1. Conecte no banco do Keycloak:"
        cat << 'EOF'
docker exec -it keycloak-db mysql -u keycloak -pkeycloak keycloak
EOF
        
        echo ""
        print_info "2. Encontre o usuário e suas credenciais:"
        cat << 'EOF'
-- Ver ID do usuário marcus
SELECT id, username, email 
FROM USER_ENTITY 
WHERE username = 'marcus';

-- Copie o ID retornado (ex: a1b2c3d4-...)

-- Ver credenciais TOTP do usuário
SELECT id, type, user_label 
FROM CREDENTIAL 
WHERE user_id = 'a1b2c3d4-...' 
AND type = 'otp';
EOF
        
        echo ""
        print_info "3. Delete a credencial TOTP:"
        cat << 'EOF'
-- Delete a credencial
DELETE FROM CREDENTIAL 
WHERE user_id = 'a1b2c3d4-...' 
AND type = 'otp';

-- Verifique
SELECT COUNT(*) FROM CREDENTIAL 
WHERE user_id = 'a1b2c3d4-...' 
AND type = 'otp';
-- Deve retornar 0
EOF
        
        echo ""
        print_warning "Depois do DELETE, reinicie o Keycloak:"
        echo "   docker restart keycloak"
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

# Instruções finais
print_section "DEPOIS DO RESET - Testando o Login"
echo ""

print_info "1. Teste via navegador (Account Console):"
echo "   • URL: http://localhost:8180/realms/cinelog/account"
echo "   • Username: marcus"
echo "   • Password: Marcus@CineLog2025!"
echo "   • O Keycloak vai mostrar o QR code novamente!"
echo ""

print_info "2. Escaneie o QR code:"
echo "   • Abra Microsoft Authenticator"
echo "   • Adicionar conta → Outra (Google, Facebook, etc.)"
echo "   • Escaneie o QR code mostrado"
echo "   • Ou digite o secret manualmente (se preferir)"
echo ""

print_info "3. Insira o código de 6 dígitos:"
echo "   • Microsoft Authenticator gera códigos a cada 30s"
echo "   • Digite o código atual na tela do Keycloak"
echo "   • Clique em Submit"
echo ""

print_success "MFA reconfigurado com sucesso! ✅"
echo ""

print_info "4. Teste via API:"
cat << 'EOF'
# Login básico (retorna challenge)
curl -X POST http://localhost:8080/api/v1/auth/keycloak/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "marcus",
    "password": "Marcus@CineLog2025!"
  }'

# Response terá mfaRequired=true e sessionId

# Complete com TOTP
curl -X POST http://localhost:8080/api/v1/auth/keycloak/login/mfa \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "<SESSION_ID_DO_RESPONSE_ANTERIOR>",
    "totpCode": "123456"
  }'
EOF

echo ""
print_section "DICAS IMPORTANTES"
echo ""

echo "• ${CYAN}Backup do Secret:${NC}"
echo "  Quando você escaneia o QR code, o Keycloak também mostra"
echo "  o secret em texto (ex: JBSWY3DPEHPK3PXP)"
echo "  Guarde esse secret em um local seguro!"
echo ""

echo "• ${CYAN}Múltiplos Devices:${NC}"
echo "  Você pode adicionar o MESMO secret em múltiplos apps"
echo "  (Microsoft Authenticator, Google Authenticator, etc.)"
echo "  Todos vão gerar o mesmo código!"
echo ""

echo "• ${CYAN}Códigos de Backup:${NC}"
echo "  O Keycloak suporta códigos de backup (recovery codes)"
echo "  Configure isso em: Account Console → Signing in → Recovery Codes"
echo ""

echo "• ${CYAN}Se Perder Novamente:${NC}"
echo "  Simplesmente execute este script novamente!"
echo "  Não precisa remover do app, apenas resete no Keycloak"
echo "  e escaneie o novo QR code"
echo ""

print_info "Documentação completa em: docs/SECURITY-IAM-GUIDE.md"
