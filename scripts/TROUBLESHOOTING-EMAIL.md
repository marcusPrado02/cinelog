# ❌ Script de Teste Falhou? Veja as Soluções

## Problema Comum: "API não está respondendo"

Se você viu esta mensagem ao executar `./scripts/test-email-reports.sh`:

```
✗ API não está respondendo em http://localhost:8080
ℹ Inicie a aplicação primeiro com: mvn spring-boot:run
```

## ✅ Solução Rápida (Recomendada)

Execute o script que **inicia tudo automaticamente**:

```bash
./scripts/start-email-test-env.sh
```

Esse script vai:

1. ✅ Iniciar MailHog (se não estiver rodando)
2. ✅ Iniciar MySQL (se não estiver rodando)
3. ✅ Iniciar Redis (se não estiver rodando)
4. ✅ Perguntar se quer iniciar a API
5. ✅ Aguardar tudo estar pronto
6. ✅ Mostrar status final

Depois, execute os testes:

```bash
./scripts/test-email-reports.sh
```

---

## 🔧 Solução Manual (Passo a Passo)

Se preferir fazer manualmente:

### 1. Inicie os Serviços Docker

```bash
# MailHog (para capturar e-mails)
docker compose up -d mailhog

# MySQL (banco de dados)
docker compose up -d db

# Redis (cache)
docker compose up -d redis

# Verifique se estão rodando
docker ps
```

### 2. Aguarde MySQL Inicializar

```bash
# MySQL demora ~30 segundos para ficar pronto
sleep 30

# Ou verifique manualmente:
docker logs cinelog-db
# Procure por: "ready for connections"
```

### 3. Inicie a Aplicação

**Opção A: Com run-dev.sh (Recomendado)**

```bash
./scripts/run-dev.sh
```

- Detecta automaticamente `.env.mail`
- Mostra claramente qual modo de e-mail está usando
- Carrega todas as variáveis de ambiente

**Opção B: Com Maven Diretamente**

```bash
mvn spring-boot:run
```

- Usa apenas `.env`
- Para SMTP real, faça: `source .env.mail && mvn spring-boot:run`

### 4. Aguarde a Aplicação Iniciar

```bash
# Acompanhe os logs
tail -f logs/application.log

# Ou teste manualmente até responder:
curl http://localhost:8080/api/v1/health
```

A aplicação demora **30-60 segundos** para iniciar completamente.

### 5. Execute os Testes

```bash
./scripts/test-email-reports.sh
```

---

## 🐛 Outros Problemas

### "MailHog não está rodando"

```bash
# Inicie o MailHog
docker compose up -d mailhog

# Verifique se está rodando
docker ps | grep mailhog

# Teste a interface
curl http://localhost:8025
```

### "jq não está instalado"

```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq

# Conda (se usa Anaconda)
conda install -c conda-forge jq
```

### "Falha na autenticação do usuário comum"

O script tenta autenticar com:

- Usuário: `marcus`
- Senha: `Marcus@CineLog2025!`

Se esses usuários não existem:

```bash
# Execute as migrations do Liquibase
mvn liquibase:update

# Ou crie os usuários manualmente via SQL
```

### "Porta 8080 já está em uso"

```bash
# Encontre o processo usando a porta
lsof -i :8080

# Ou no Linux
ss -tlnp | grep 8080

# Mate o processo
kill -9 <PID>
```

### API Inicia mas Retorna Erros 500

Verifique os logs:

```bash
tail -f logs/application.log

# Problemas comuns:
# - Banco de dados não conectou
# - Migrations não rodaram
# - Credenciais incorretas no .env
```

---

## 📋 Checklist de Verificação

Antes de executar `test-email-reports.sh`, certifique-se:

- [ ] Docker está rodando: `docker ps`
- [ ] MailHog respondendo: `curl http://localhost:8025`
- [ ] MySQL pronto: `docker logs cinelog-db | grep "ready for connections"`
- [ ] API respondendo: `curl http://localhost:8080/api/v1/health`
- [ ] Usuários existem no banco (marcus, admin)

---

## 🚀 Fluxo Completo (Copy-Paste Friendly)

```bash
# 1. Use o script automático
./scripts/start-email-test-env.sh

# 2. Quando tudo estiver verde, execute os testes
./scripts/test-email-reports.sh

# 3. Visualize os e-mails
# - MailHog: http://localhost:8025
# - Ou verifique: marcus.prado@pitang.com (se configurou SMTP real)
```

---

## 💡 Dicas

**Para Desenvolvimento Diário:**

1. Deixe os containers sempre rodando: `docker compose up -d`
2. Use `./scripts/run-dev.sh` para iniciar a API
3. Mantenha `.env.mail` apenas quando for testar envio real

**Para Apresentações/Demos:**

1. Configure `.env.mail` uma vez: `./scripts/setup-email-real.sh`
2. Use `./scripts/start-email-test-env.sh` para garantir que tudo está ok
3. E-mails chegarão em `marcus.prado@pitang.com` de verdade

**Para CI/CD:**

1. Remova `.env.mail` (se existir)
2. Use apenas MailHog
3. Valide templates HTML, não deliverability

---

## 📞 Ainda com Problemas?

1. Verifique os logs:

    ```bash
    # Logs da aplicação
    tail -f logs/application.log

    # Logs do Docker
    docker compose logs -f

    # Logs de serviço específico
    docker logs cinelog-db
    docker logs mailhog
    ```

2. Reinicie tudo:

    ```bash
    # Pare tudo
    docker compose down
    pkill -f spring-boot

    # Inicie novamente
    ./scripts/start-email-test-env.sh
    ```

3. Verifique as configurações:

    ```bash
    # .env existe?
    ls -la .env

    # .env.mail configurado corretamente?
    cat .env.mail

    # Portas disponíveis?
    lsof -i :8080
    lsof -i :3306
    lsof -i :6379
    lsof -i :8025
    ```
