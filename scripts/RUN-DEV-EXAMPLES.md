# 📋 Saídas do run-dev.sh - Exemplos

Este documento mostra como o `run-dev.sh` se comporta nos diferentes cenários de configuração de e-mail.

---

## Cenário 1: MailHog (Desenvolvimento) - Sem .env.mail

**Comando**: `./scripts/run-dev.sh`

**Saída**:

```
📧 Usando MailHog (desenvolvimento) - E-mails em http://localhost:8025
   Para enviar e-mails reais, execute: ./scripts/setup-email-real.sh

Iniciando CinelogApplication com perfil: dev

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)
...
```

**Comportamento**:

- ✅ Carrega apenas `.env`
- ✅ Variáveis de e-mail padrão (localhost:1025 - MailHog)
- ✅ E-mails capturados localmente, não enviados pela internet
- ✅ Visualização em http://localhost:8025

---

## Cenário 2: SMTP Real (Produção) - Com .env.mail (Gmail)

**Comando**: `./scripts/run-dev.sh`

**Saída**:

```
📧 Carregando configurações de e-mail real (.env.mail)...
   REMETENTE: marcus.dev@gmail.com
   SERVIDOR: smtp.gmail.com:587
   DESTINATÁRIO (testes): marcus.prado@pitang.com

Iniciando CinelogApplication com perfil: dev

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)
...
```

**Comportamento**:

- ✅ Carrega `.env` primeiro
- ✅ Sobrescreve com variáveis do `.env.mail`
- ✅ Mostra claramente que está usando SMTP real
- ✅ E-mails enviados DE VERDADE via Gmail
- ✅ E-mails chegam em marcus.prado@pitang.com

---

## Cenário 3: SMTP Corporativo - Com .env.mail (Pitang)

**Comando**: `./scripts/run-dev.sh`

**Saída**:

```
📧 Carregando configurações de e-mail real (.env.mail)...
   REMETENTE: noreply@pitang.com
   SERVIDOR: smtp.pitang.com:587
   DESTINATÁRIO (testes): marcus.prado@pitang.com

Iniciando CinelogApplication com perfil: dev

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)
...
```

**Comportamento**:

- ✅ Usa servidor SMTP corporativo (Pitang)
- ✅ Remetente corporativo (noreply@pitang.com)
- ✅ Visual mais profissional com domínio da empresa
- ✅ E-mails chegam no destinatário interno (pitang.com)

---

## Cenário 4: SendGrid - Com .env.mail

**Comando**: `./scripts/run-dev.sh`

**Saída**:

```
📧 Carregando configurações de e-mail real (.env.mail)...
   REMETENTE: apikey
   SERVIDOR: smtp.sendgrid.net:587
   DESTINATÁRIO (testes): marcus.prado@pitang.com

Iniciando CinelogApplication com perfil: dev

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)
...
```

**Comportamento**:

- ✅ Usa SendGrid como provedor de e-mail
- ✅ Alta deliverability (e-mails não caem em spam)
- ✅ Métricas profissionais no painel SendGrid
- ✅ Remetente configurado no painel SendGrid (não 'apikey')

---

## Como Alternar Entre os Modos

### MailHog → SMTP Real

```bash
# 1. Configure o SMTP real
./scripts/setup-email-real.sh

# 2. Reinicie a aplicação (detecta .env.mail automaticamente)
./scripts/run-dev.sh
```

### SMTP Real → MailHog

```bash
# 1. Remova ou renomeie o arquivo
mv .env.mail .env.mail.backup

# 2. Reinicie a aplicação (volta para MailHog)
./scripts/run-dev.sh
```

### Alternar Configurações de SMTP

```bash
# 1. Reconfigure (sobrescreve .env.mail)
./scripts/setup-email-real.sh

# 2. Reinicie a aplicação
./scripts/run-dev.sh
```

---

## Verificando Qual Modo Está Ativo

### Antes de Iniciar

```bash
# Verifica se .env.mail existe
if [[ -f .env.mail ]]; then
    echo "✓ SMTP Real configurado"
    grep "MAIL_USER" .env.mail
else
    echo "✓ MailHog (desenvolvimento)"
fi
```

### Durante a Execução

Observe a primeira linha da saída do `run-dev.sh`:

- Se mostrar **"Usando MailHog"** → Modo desenvolvimento
- Se mostrar **"Carregando configurações de e-mail real"** → Modo produção

### Após Iniciar (via logs)

```bash
# Verifica os logs da aplicação
tail -f logs/application.log | grep -i "mail\|smtp"

# Saída exemplo (MailHog):
# JavaMailSenderImpl : Using default host localhost
# JavaMailSenderImpl : Using default port 1025

# Saída exemplo (Gmail):
# JavaMailSenderImpl : Using host smtp.gmail.com
# JavaMailSenderImpl : Using port 587
```

---

## Troubleshooting

### run-dev.sh não mostra mensagem de e-mail

**Problema**: Script inicia mas não mostra se é MailHog ou SMTP
**Solução**: Versão antiga do script, atualize:

```bash
git pull origin master
./scripts/run-dev.sh
```

### .env.mail existe mas usa MailHog mesmo assim

**Problema**: Variáveis não estão sendo carregadas
**Causa**: Erro de sintaxe no .env.mail
**Solução**: Verifique o arquivo:

```bash
cat .env.mail
# Deve conter linhas como:
# export MAIL_HOST=smtp.gmail.com
# export MAIL_PORT=587
# ...
```

### E-mails não chegam mesmo com .env.mail configurado

**Problema**: SMTP configurado mas e-mails não são enviados
**Diagnóstico**:

1. Verifique os logs: `tail -f logs/application.log | grep -i "mail"`
2. Teste credenciais: `telnet smtp.gmail.com 587`
3. Para Gmail: use Senha de App, não senha normal
4. Verifique firewall/antivírus bloqueando porta 587

---

## Exemplos de Automação

### Script para Testar Ambos os Modos

```bash
#!/bin/bash
# test-both-modes.sh

echo "=== TESTE 1: MailHog ==="
mv .env.mail .env.mail.backup 2>/dev/null || true
./scripts/run-dev.sh &
sleep 15
./scripts/test-email-reports.sh
pkill -f spring-boot

echo ""
echo "=== TESTE 2: SMTP Real ==="
mv .env.mail.backup .env.mail 2>/dev/null || true
./scripts/run-dev.sh &
sleep 15
./scripts/test-email-reports.sh
pkill -f spring-boot

echo "TESTES CONCLUÍDOS!"
```

### CI/CD - Sempre usar MailHog

```yaml
# .github/workflows/test.yml
- name: Run Email Tests (MailHog)
  run: |
      # Garante que não tem .env.mail (usa MailHog)
      rm -f .env.mail

      # Inicia aplicação
      ./scripts/run-dev.sh &
      sleep 20

      # Testa e-mails
      ./scripts/test-email-reports.sh
```

---

## Resumo das Vantagens

| Aspecto          | Antes                     | Depois                         |
| ---------------- | ------------------------- | ------------------------------ |
| **Configuração** | Manual `source .env.mail` | Automática no run-dev.sh       |
| **Feedback**     | Silencioso                | Mostra modo e remetente        |
| **Alternância**  | Requer exports manuais    | Apenas criar/remover .env.mail |
| **Documentação** | Implícita                 | Explícita no início            |
| **Erros**        | Difícil diagnosticar      | Visível imediatamente          |

## Dúvidas Frequentes

**P: O .env.mail é committado ao Git?**
R: NÃO! Está no .gitignore. Contém credenciais sensíveis.

**P: Posso ter múltiplos arquivos .env.mail?**
R: Sim! Use nomes como `.env.mail.gmail`, `.env.mail.sendgrid` e copie antes de usar.

**P: Como saber se minha configuração está correta?**
R: Execute `./scripts/run-dev.sh` e observe a primeira linha. Deve mostrar o servidor SMTP esperado.

**P: E se eu quiser usar mvn spring-boot:run diretamente?**
R: Use `source .env.mail && mvn spring-boot:run` (método antigo ainda funciona).
