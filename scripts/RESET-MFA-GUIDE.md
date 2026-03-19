# 🔐 Reset de MFA (TOTP) - Guia Rápido

## ❌ Problema

Você configurou MFA no Keycloak, mas:
- Removeu do Microsoft Authenticator
- Perdeu acesso ao app
- O QR code não aparece mais no login
- Não consegue completar o login

## ✅ Solução Rápida (5 minutos)

### Método 1: Admin Console (Mais Fácil)

```bash
# 1. Acesse o Keycloak Admin Console
URL: http://localhost:8180/admin/
Usuário: admin
Senha: admin 

# 2. Navegue até o usuário
Sidebar → Users → Buscar: marcus → Clicar no usuário

# 3. Remova a credencial TOTP
Aba: Credentials
Procure: OTP ou TOTP
Clique em: Delete (ícone de lixeira)
Confirme

# 4. Faça login novamente
URL: http://localhost:8180/realms/cinelog/account
Username: marcus
Password: Marcus@CineLog2025!

# 5. O QR code vai aparecer novamente!
Escaneie com Microsoft Authenticator
Digite o código de 6 dígitos
Pronto! ✅
```

### Método 2: Script Automático

```bash
# Execute o script interativo
./scripts/reset-mfa.sh

# Escolha: Opção 1 (Admin Console)
# Siga as instruções na tela
```

---

## 📋 Passo a Passo Detalhado

### 1. Acesse o Keycloak Admin Console

Abra o navegador:
```
http://localhost:8180/admin/
```

**Credenciais de Admin:**
- Usuário: `admin`
- Senha: `admin`

### 2. Encontre o Usuário

1. Na sidebar esquerda, clique em **"Users"**
2. No campo de busca, digite: `marcus`
3. Clique no usuário **marcus** na lista

### 3. Remova a Credencial TOTP

1. Clique na aba **"Credentials"**
2. Na lista de credenciais, procure por:
   - **OTP** ou
   - **TOTP** ou  
   - **Authenticator**
3. Clique no ícone de **lixeira** (Delete) ao lado
4. Confirme a remoção

**Resultado:** O MFA foi removido do usuário!

### 4. Teste o Login

#### Via Account Console do Keycloak:

```
URL: http://localhost:8180/realms/cinelog/account
Username: marcus
Password: Marcus@CineLog2025!
```

**O que acontece:**
1. Você digita username e senha
2. Keycloak detecta: "TOTP não configurado"
3. **Mostra o QR code novamente!** 🎉
4. Mostra também o secret em texto (ex: `JBSWY3DPEHPK3PXP`)

#### Via API do CineLog:

```bash
curl -X POST http://localhost:8080/api/v1/auth/keycloak/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "marcus",
    "password": "Marcus@CineLog2025!"
  }'
```

### 5. Reconfigure o MFA

**No Microsoft Authenticator:**

1. Abra o app
2. Clique em **"+"** (Adicionar conta)
3. Escolha: **"Outra (Google, Facebook, etc.)"**
4. **Opção A:** Escaneie o QR code mostrado no navegador
5. **Opção B:** Digite o secret manualmente
6. O app gera um código de 6 dígitos

**No Keycloak:**

1. Digite o código de 6 dígitos atual (muda a cada 30s)
2. Clique em **"Submit"**
3. **Sucesso!** MFA reconfigurado ✅

---

## 🔄 Método Alternativo: API REST

Se preferir usar API ao invés da interface web:

```bash
# 1. Obtenha token de admin
ADMIN_TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  | jq -r '.access_token')

# 2. Obtenha ID do usuário
USER_ID=$(curl -s -X GET \
  "http://localhost:8180/admin/realms/cinelog/users?username=marcus" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq -r '.[0].id')

# 3. Obtenha ID da credencial TOTP
CREDENTIAL_ID=$(curl -s -X GET \
  "http://localhost:8180/admin/realms/cinelog/users/$USER_ID/credentials" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq -r '.[] | select(.type == "otp") | .id')

# 4. Delete a credencial
curl -X DELETE \
  "http://localhost:8180/admin/realms/cinelog/users/$USER_ID/credentials/$CREDENTIAL_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

echo "MFA removido! Faça login novamente para ver o QR code."
```

---

## 💡 Dicas Importantes

### Backup do Secret

Quando o QR code aparecer, o Keycloak também mostra o **secret em texto**:

```
Exemplo: JBSWY3DPEHPK3PXP
```

**Guarde esse secret com segurança!**  
Se perder o acesso novamente, pode inserir manualmente no authenticator.

### Múltiplos Devices

Você pode adicionar o **MESMO secret** em vários apps:
- Microsoft Authenticator (celular)
- Google Authenticator (tablet)
- FreeOTP (backup)

Todos vão gerar o **mesmo código** ao mesmo tempo!

### Códigos de Recuperação

Configure códigos de backup no Keycloak:

```
1. Login: http://localhost:8180/realms/cinelog/account
2. Menu lateral: Signing in
3. Seção: Two-factor authentication
4. Clique: Configure Recovery Codes
5. Guarde os códigos gerados
```

Esses códigos podem ser usados se você perder o authenticator.

---

## 🐛 Troubleshooting

### "Credencial não aparece na lista"

Pode ser que o MFA já esteja resetado. Tente fazer login para confirmar.

### "QR code não aparece após remover"

1. Limpe cache do navegador
2. Use navegador anônimo/privado
3. Ou force reconfiguração:
   - Admin Console → Users → marcus
   - Aba: Details ou Required Actions
   - Marque: **Configure OTP**
   - Save

### "Keycloak não está rodando"

```bash
# Inicie o Keycloak
docker compose up -d keycloak

# Verifique status
docker ps | grep keycloak

# Veja logs se houver erro
docker logs keycloak
```

### "Código de 6 dígitos não funciona"

- ⏰ Códigos expiram a cada **30 segundos**
- 🕐 Verifique se o horário do celular está sincronizado
- 🔄 Aguarde o próximo código ser gerado
- ✅ Use o código **atual**, não o anterior

---

## 📚 Referências

- **Documentação completa:** [docs/SECURITY-IAM-GUIDE.md](../docs/SECURITY-IAM-GUIDE.md)
- **Script interativo:** `./scripts/reset-mfa.sh`
- **Keycloak TOTP:** https://www.keycloak.org/docs/latest/server_admin/#otp-policies

---

## 🎯 Resumo Executivo

**Problema:** MFA configurado mas sem acesso ao authenticator  
**Solução:** Reset via Keycloak Admin Console  
**Tempo:** 5 minutos  
**Resultado:** QR code aparece novamente no login

**Comando rápido:**
```bash
./scripts/reset-mfa.sh
# Escolha: Opção 1
# Siga as instruções
```

**Passo a passo manual:**
1. http://localhost:8180/admin/ (admin/admin)
2. Users → marcus → Credentials
3. Delete OTP/TOTP
4. Login novamente → QR code reaparece!
5. Escaneie com Microsoft Authenticator
6. Digite código de 6 dígitos
7. ✅ Pronto!
