# Teste de Envio de E-mails - Relatórios

## 📚 Documentação Relacionada

- **[EMAIL-QUICKSTART.md](EMAIL-QUICKSTART.md)** - Guia rápido TL;DR (comece aqui!)
- **[TROUBLESHOOTING-EMAIL.md](TROUBLESHOOTING-EMAIL.md)** - ❌ Erros? Soluções aqui!
- **[RUN-DEV-EXAMPLES.md](RUN-DEV-EXAMPLES.md)** - Exemplos de saída do run-dev.sh
- **Este arquivo** - Documentação completa e detalhada

## 🚀 Início Rápido (Para Preguiçosos)

**Um comando faz tudo:**

```bash
# Inicia TODOS os serviços automaticamente
./scripts/start-email-test-env.sh

# Depois, teste os e-mails
./scripts/test-email-reports.sh
```

O script `start-email-test-env.sh`:

- ✅ Verifica e inicia MailHog, MySQL, Redis
- ✅ Detecta se API está rodando
- ✅ Oferece iniciar a API automaticamente
- ✅ Aguarda tudo estar pronto
- ✅ Mostra resumo final

**Se algo falhar**, consulte [TROUBLESHOOTING-EMAIL.md](TROUBLESHOOTING-EMAIL.md)

---

## 🎯 Novidade: run-dev.sh Integrado

O script `run-dev.sh` agora detecta automaticamente `.env.mail`:

```bash
# Inicia aplicação e detecta configuração de e-mail automaticamente
./scripts/run-dev.sh

# Se .env.mail existe → Usa SMTP real
# Se não existe → Usa MailHog (desenvolvimento)
```

Não precisa mais fazer `source .env.mail` manualmente! 🎉

---

## �📧 Configuração de E-mails

### Destinatário vs Remetente

**DESTINATÁRIO (TO)**: `marcus.prado@pitang.com`

- Para onde os e-mails de teste são enviados
- Este é o e-mail que receberá todos os relatórios
- Definido no script via variável `TEST_EMAIL`

**REMETENTE (FROM)**: Configurado via `MAIL_USER`

- De onde os e-mails saem (exemplo: `noreply@cinelog.com`, `seu-email@gmail.com`)
- Configurado nas variáveis de ambiente
- Você define qual e-mail aparecerá como remetente

### ⚠️ IMPORTANTE: Dois Modos de Operação

#### 🔨 Modo Desenvolvimento (Padrão - MailHog)

Por padrão, a aplicação usa **MailHog** (servidor SMTP local que captura e-mails):

- ✅ E-mails são "enviados" com sucesso
- ✅ Você pode visualizá-los com design completo (HTML, CSS, imagens)
- ❌ Eles **NÃO chegam** no destinatário real (`marcus.prado@pitang.com`)
- 📧 Ficam capturados localmente: **http://localhost:8025**

#### 🚀 Modo Produção (SMTP Real)

Quando você configurar um servidor SMTP real (Gmail, SendGrid, etc):

- ✅ E-mails são **enviados de verdade** pela internet
- ✅ Eles **CHEGAM** na caixa de entrada real: `marcus.prado@pitang.com`
- 📨 O remetente será o e-mail que você configurou em `MAIL_USER`
- ⏱️ Podem demorar alguns minutos para chegar

## Como Usar o Script

### 🚀 Fluxo Simplificado (Recomendado)

O script `run-dev.sh` foi aprimorado para carregar automaticamente as configurações de e-mail:

```bash
# 1. Configure o SMTP (apenas uma vez)
./scripts/setup-email-real.sh

# 2. Inicie a aplicação (detecta .env.mail automaticamente)
./scripts/run-dev.sh
# Você verá: 📧 Carregando configurações de e-mail real (.env.mail)...

# 3. Teste os e-mails
./scripts/test-email-reports.sh

# 4. Verifique marcus.prado@pitang.com
```

**Vantagens**:

- ✅ Não precisa fazer `source .env.mail` manualmente
- ✅ Funciona tanto para MailHog quanto SMTP real
- ✅ Mostra claramente qual modo está usando no início

### 1. Pré-requisitos

```bash
# Inicie o MailHog (para modo desenvolvimento)
docker compose up -d mailhog

# Inicie a aplicação
./scripts/run-dev.sh    # Detecta automaticamente .env.mail
# OU
mvn spring-boot:run     # Usa apenas .env (MailHog)

# Acesse http://localhost:8025 no navegador para ver a interface do MailHog
```

### 2. Execute o Script

```bash
./scripts/test-email-reports.sh
```

O script irá:

- ✓ Verificar se a API e MailHog estão rodando
- ✓ Autenticar como usuário comum e admin
- ✓ Testar todos os 11 endpoints de relatórios
- ✓ Enviar e-mails para `marcus.prado@pitang.com`
- ✓ Mostrar onde visualizar os e-mails

### 3. Personalizando o E-mail de Destino

```bash
# Use seu próprio e-mail
TEST_EMAIL="seu-email@exemplo.com" ./scripts/test-email-reports.sh
```

### 4. Visualizando os E-mails

Abra **http://localhost:8025** no navegador. Você verá:

- 📨 Lista de todos os e-mails enviados
- 🎨 Preview HTML com design completo
- 📋 Cabeçalhos, corpo texto/HTML, anexos
- 🔍 Filtros por destinatário, assunto, data

## Enviando E-mails Reais para marcus.prado@pitang.com

Para que os e-mails cheguem **de verdade** na caixa de entrada `marcus.prado@pitang.com`, você precisa configurar um servidor SMTP real:

### Opção 1: Gmail como Remetente

Se você quer enviar os e-mails a partir de uma conta Gmail:

1. **Crie uma Senha de App** (não use sua senha normal):
    - Acesse: https://myaccount.google.com/apppasswords
    - Gere uma senha de 16 caracteres

2. **Configure as Variáveis**:

```bash
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USER=seu-email@gmail.com          # ← REMETENTE (de onde sai)
export MAIL_PASS=sua-senha-app-16-caracteres
export MAIL_AUTH=true
export MAIL_STARTTLS=true
```

3. **Reinicie a aplicação**:

```bash
# Opção 1: Com run-dev.sh (recomendado - carrega .env.mail automaticamente)
./scripts/run-dev.sh

# Opção 2: Com mvn diretamente
source .env.mail && mvn spring-boot:run
```

4. **Execute o script**:

```bash
./scripts/test-email-reports.sh
```

5. **Resultado**:
    - E-mails são enviados de `seu-email@gmail.com`
    - E-mails chegam em `marcus.prado@pitang.com`

### Opção 2: SendGrid como Remetente

Se você quer usar SendGrid (serviço profissional de envio de e-mails):

```bash
export MAIL_HOST=smtp.sendgrid.net
export MAIL_PORT=587
export MAIL_USER=apikey                        # ← Literal "apikey"
export MAIL_PASS=sua-api-key-do-sendgrid      # ← Sua API Key
export MAIL_AUTH=true
export MAIL_STARTTLS=true
```

**Resultado**:

- E-mails enviados via SendGrid
- E-mails chegam em `marcus.prado@pitang.com`
- Remetente configurável no painel SendGrid

### Opção 3: SMTP Corporativo (Pitang, etc)

Se você tem um servidor SMTP corporativo:

```bash
export MAIL_HOST=smtp.pitang.com              # ← Servidor SMTP da empresa
export MAIL_PORT=587
export MAIL_USER=noreply@pitang.com           # ← REMETENTE corporativo
export MAIL_PASS=senha-do-email
export MAIL_AUTH=true
export MAIL_STARTTLS=true
```

**Resultado**:

- E-mails enviados de `noreply@pitang.com`
- E-mails chegam em `marcus.prado@pitang.com`
- Visual mais profissional com domínio corporativo

## Endpoints Testados

O script testa todos os 11 endpoints de relatórios:

### Relatórios de Usuário (7)

1. **Weekly Digest** - Resumo semanal de atividades
2. **Top Rated** - Mídias mais bem avaliadas
3. **Recommendations** - Recomendações personalizadas
4. **Trending** - Em alta no momento
5. **New Releases** - Novos lançamentos
6. **Genre Spotlight** - Destaque de gênero
7. **Top Actors** - Atores mais populares

### Relatórios Admin (2)

8. **Platform Report** - Relatório administrativo da plataforma
9. **Send to All** - Envio em massa para todos os usuários

## Estrutura dos E-mails

Todos os e-mails usam templates HTML Thymeleaf profissionais:

- 🎨 **Design Responsivo**: funciona bem em desktop, tablet, mobile
- 🌗 **Tema Escuro**: visual moderno com gradientes
- 📊 **Dados Reais**: informações vindas do banco de dados
- 🖼️ **Imagens**: posters, backdrops, avatares de atores
- 🏆 **Rankings**: medalhas (ouro, prata, bronze) e badges

## Solução de Problemas

### "API não está respondendo"

```bash
# Inicie a aplicação
mvn spring-boot:run
```

### "MailHog não está rodando"

```bash
# Inicie o MailHog
docker compose up -d mailhog

# Verifique se está rodando
docker ps | grep mailhog
```

### "E-mails não aparecem no MailHog"

- Certifique-se que as variáveis `MAIL_HOST` e `MAIL_PORT` não estão configuradas para outro servidor
- Valores padrão: `localhost:1025`
- Reinicie a aplicação se mudou as variáveis

### "Erro 401 Unauthorized"

- Verifique se os usuários `marcus` e `admin` existem no banco
- Senhas padrão: `Marcus@CineLog2025!` e `Admin@CineLog2025!`
- Execute as migrations do Liquibase: `mvn liquibase:update`

### "Status 202 mas e-mail não chega"

- Status 202 = aceito para processamento assíncrono (esperado!)
- Os e-mails são processados em background
- Aguarde alguns segundos e recarregue o MailHog
- Verifique os logs da aplicação para erros: `tail -f logs/application.log`

## Arquitetura do Sistema de E-mails

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Cliente   │─────▶│  Controller  │─────▶│   Service   │
│  (Script)   │ POST │    (API)     │      │  (Async)    │
└─────────────┘      └──────────────┘      └──────┬──────┘
                                                    │
                     ┌──────────────────────────────┘
                     ▼
          ┌──────────────────┐      ┌─────────────┐
          │   Thread Pool    │─────▶│   MailHog   │ (Dev)
          │  (Async Exec)    │ SMTP │ localhost:  │
          └──────────────────┘      │    1025     │
                     │               └─────────────┘
                     │
                     │               ┌─────────────┐
                     └──────────────▶│ Gmail/SMTP  │ (Prod)
                               SMTP  │  Externo    │
                                     └─────────────┘
```

## Variáveis de Ambiente

| Variável        | Padrão      | Descrição           |
| --------------- | ----------- | ------------------- |
| `MAIL_HOST`     | `localhost` | Servidor SMTP       |
| `MAIL_PORT`     | `1025`      | Porta SMTP          |
| `MAIL_USER`     | `""`        | Usuário SMTP        |
| `MAIL_PASS`     | `""`        | Senha SMTP          |
| `MAIL_AUTH`     | `false`     | Requer autenticação |
| `MAIL_STARTTLS` | `false`     | Usa TLS             |

## Comandos Úteis

```bash
# Ver logs de e-mails enviados
tail -f logs/application.log | grep -i "mail\|email"

# Limpar todos os e-mails do MailHog
curl -X DELETE http://localhost:8025/api/v1/messages

# Ver quantos e-mails estão no MailHog
curl -s http://localhost:8025/api/v2/messages | jq '.total'

# Baixar um e-mail específico (HTML)
curl -s http://localhost:8025/api/v1/messages/{MESSAGE_ID}.html

# Parar o MailHog
docker compose down mailhog
```

## Próximos Passos

Após testar os e-mails no MailHog:

1. ✅ Valide o design dos templates
2. ✅ Verifique se os dados estão corretos
3. ✅ Teste em diferentes clientes de e-mail (Gmail, Outlook, Thunderbird)
4. ✅ Configure SMTP real para produção
5. ✅ Implemente rate limiting para evitar spam
6. ✅ Adicione unsubscribe link nos e-mails
7. ✅ Configure SPF, DKIM, DMARC para deliverability

## Referências

- [MailHog GitHub](https://github.com/mailhog/MailHog)
- [Spring Boot Mail](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [Thymeleaf Templates](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)
- [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
