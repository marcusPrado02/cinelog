# 🚀 Guia Rápido: Envio de E-mails de Teste

## 🎯 Novidade: Integração com run-dev.sh

O script `run-dev.sh` agora detecta automaticamente as configurações de e-mail!

```bash
# Se .env.mail existe → Usa SMTP real (e-mails chegam de verdade)
# Se .env.mail NÃO existe → Usa MailHog (e-mails capturados localmente)

./scripts/run-dev.sh
```

**Vantagens**:

- ✅ Sem necessidade de `source .env.mail` manual
- ✅ Detecção automática do modo (dev/prod)
- ✅ Feedback visual no início (mostra remetente e servidor)

---

## TL;DR - Começo Rápido

### 🎯 Método Mais Fácil (Recomendado para Iniciantes)

**Um único comando inicia tudo automaticamente:**

```bash
# Inicia TODOS os serviços necessários (MailHog, MySQL, Redis, API)
./scripts/start-email-test-env.sh

# Depois execute os testes
./scripts/test-email-reports.sh
```

O script `start-email-test-env.sh` faz TUDO por você:

- ✅ Verifica e inicia MailHog
- ✅ Verifica e inicia MySQL/Redis
- ✅ Detecta se API está rodando
- ✅ Oferece iniciar a API automaticamente
- ✅ Aguarda tudo estar pronto
- ✅ Mostra status final

---

### 🔧 Método Manual (Para Quem Já Conhece o Projeto)

### 📧 Configuração de E-mails

- **DESTINATÁRIO (TO)**: `marcus.prado@pitang.com` ← fixo, para onde vão os e-mails
- **REMETENTE (FROM)**: configurado por você via `MAIL_USER` ← de onde saem os e-mails

---

## Modo 1️⃣: Desenvolvimento (MailHog) - Padrão

E-mails **não chegam** no destinatário real, ficam capturados localmente.

```bash
# 1. Inicie MailHog
docker compose up -d mailhog

# 2. Inicie a aplicação (detecta que NÃO tem .env.mail → usa MailHog)
./scripts/run-dev.sh
# Saída: 📧 Usando MailHog (desenvolvimento) - E-mails em http://localhost:8025

# 3. Execute os testes
./scripts/test-email-reports.sh

# 4. Visualize os e-mails
# Abra: http://localhost:8025
```

✅ **Vantagens**: Rápido, sem configuração, sem risco de enviar e-mails acidentalmente
❌ **Limitação**: E-mails não chegam no marcus.prado@pitang.com de verdade

---

## Modo 2️⃣: Produção (SMTP Real)

E-mails **chegam de verdade** na caixa de entrada `marcus.prado@pitang.com`.

### Método Assistido (Recomendado)

```bash
# 1. Execute o assistente de configuração
./scripts/setup-email-real.sh

# Escolha uma opção:
# - Gmail (pessoal)
# - SendGrid (profissional)
# - SMTP Corporativo (Pitang)
# - Outro SMTP

# 2. Reinicie a aplicação (run-dev.sh carrega .env.mail automaticamente)
./scripts/run-dev.sh

# 3. Execute os testes
./scripts/test-email-reports.sh

# 4. Verifique sua caixa de entrada
# marcus.prado@pitang.com deve receber os e-mails!
```

### Método Manual (Avançado)

```bash
# 1. Configure as variáveis (exemplo Gmail)
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USER=seu-email@gmail.com      # ← REMETENTE
export MAIL_PASS=sua-senha-app
export MAIL_AUTH=true
export MAIL_STARTTLS=true

# 2. Reinicie a aplicação
mvn spring-boot:run

# 3. Execute os testes
./scripts/test-email-reports.sh
```

✅ **Vantagens**: E-mails chegam de verdade, teste completo end-to-end
❌ **Limitação**: Requer configuração de SMTP, pode ter limites de envio

---

## 📊 O que é Testado

O script testa **11 endpoints** de relatórios por e-mail:

### Relatórios de Usuário (7)

- Weekly Digest - Resumo semanal 📅
- Top Rated - Mais bem avaliados ⭐
- Recommendations - Recomendações personalizadas 🎯
- Trending - Em alta 🔥
- New Releases - Novos lançamentos 🆕
- Genre Spotlight - Destaque de gênero 🎭
- Top Actors - Atores populares 🎬

### Relatórios Admin (2)

- Platform Report - Estatísticas da plataforma 📈
- Send to All - Envio em massa 📧

Total: **17 e-mails** enviados para `marcus.prado@pitang.com`

---

## 🔧 Opções de SMTP

### Gmail

```bash
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USER=seu-email@gmail.com
export MAIL_PASS=senha-app-16-caracteres  # https://myaccount.google.com/apppasswords
```

### SendGrid

```bash
export MAIL_HOST=smtp.sendgrid.net
export MAIL_PORT=587
export MAIL_USER=apikey
export MAIL_PASS=sua-api-key-sendgrid
```

### SMTP Corporativo

```bash
export MAIL_HOST=smtp.pitang.com
export MAIL_PORT=587
export MAIL_USER=noreply@pitang.com       # ← E-mail corporativo
export MAIL_PASS=senha-do-email
```

---

## 🐛 Solução de Problemas

### E-mails não aparecem no MailHog

```bash
# Verifique se está rodando
docker ps | grep mailhog

# Reinicie
docker compose restart mailhog

# Verifique os logs da aplicação
tail -f logs/application.log | grep -i mail
```

### E-mails não chegam no Gmail

```bash
# 1. Verifique se configurou senha de APP (não senha normal)
# 2. Verifique se a aplicação foi reiniciada APÓS export
# 3. Aguarde alguns minutos
# 4. Verifique spam/lixeira
# 5. Verifique logs: tail -f logs/application.log
```

### Erro 401 Unauthorized nos testes

```bash
# Usuários padrão não existem, execute migrations
mvn liquibase:update

# Ou crie manualmente:
# marcus / Marcus@CineLog2025!
# admin / Admin@CineLog2025!
```

---

## 📖 Documentação Completa

Para mais detalhes, consulte:

- [scripts/README-EMAIL-TESTING.md](README-EMAIL-TESTING.md) - Documentação completa
- [scripts/test-email-reports.sh](test-email-reports.sh) - Script de testes
- [scripts/setup-email-real.sh](setup-email-real.sh) - Assistente de configuração

---

## 💡 Dicas

**Desenvolvimento**: Use MailHog sempre que possível para evitar:

- Atingir limites de envio do SMTP
- Enviar e-mails acidentalmente
- Expor credenciais SMTP

**Produção**: Configure SMTP corporativo ou SendGrid para:

- E-mails chegarem de verdade
- Ter métricas de deliverability
- Aparência profissional (domínio corporativo)

**Reutilizar Configuração**: Após executar `setup-email-real.sh`:

```bash
# O run-dev.sh carrega .env.mail automaticamente!
./scripts/run-dev.sh

# Ou se preferir usar mvn diretamente:
source .env.mail && mvn spring-boot:run
```
