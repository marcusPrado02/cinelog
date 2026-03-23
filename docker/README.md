# Docker Compose - Ambiente de Desenvolvimento

Este arquivo configura todos os serviços necessários para executar o CineLog em ambiente de desenvolvimento local.

## 🐳 Serviços Disponíveis

### 1. MySQL (Banco de Dados)

-   **Porta:** 3306
-   **Database:** cinelog
-   **Usuário:** cinelog / cinelog
-   **Root Password:** root

### 2. Redis (Cache)

-   **Porta:** 6379
-   **Uso:** Cache de dados, sessões

### 3. Zookeeper (Coordenação Kafka)

-   **Porta:** 2181
-   **Uso:** Coordenação de brokers Kafka

### 4. Kafka (Message Broker)

-   **Portas:**
    -   9092 (localhost - para aplicação Java)
    -   29092 (inter-container - para comunicação interna)
-   **Uso:** Mensageria de eventos (Domain Events via Outbox Pattern)
-   **Tópicos criados automaticamente:**
    -   `cinelog.watchentry.created.v1`
    -   `cinelog.watchentry.rated.v1`
    -   `cinelog.dlq` (Dead Letter Queue)

### 5. Kafka UI (Interface Web)

-   **Porta:** 8090
-   **Acesso:** http://localhost:8090
-   **Uso:** Visualizar topicos, mensagens, consumidores, configuracao Kafka

### 6. Skipper Server (SCDF Deployer)

-   **Container:** `cinelog-skipper`
-   **Porta:** 7577
-   **Uso:** Gerencia o deploy de containers Docker para tasks SCDF. Utiliza o Docker Deployer
    para lancar containers efemeros de batch jobs.
-   **Docker CLI:** O binario estatico (`docker/scdf/docker-cli`) e montado como `docker-real`,
    e o wrapper (`docker/scdf/docker-wrapper.sh`) e montado como `docker`. O wrapper:
    -   Substitui `--network bridge` por `--network cinelog_default`
    -   Injeta `--rm` em comandos `docker run` para auto-limpeza de containers

### 7. Dataflow Server (SCDF Dashboard e REST API)

-   **Container:** `cinelog-dataflow`
-   **Porta:** 9393
-   **Acesso:** http://localhost:9393/dashboard
-   **Uso:** Dashboard web para registrar, lancar e monitorar tasks (batch jobs).
    Expoe REST API para automacao e integracao com scripts.

### 8. SCDF Scheduler (Agendamento Cron)

-   **Container:** `cinelog-scdf-scheduler`
-   **Imagem:** `alpine:3.20` (~8MB)
-   **Uso:** Substitui o scheduler nativo do SCDF (nao disponivel no Local Deployer).
    Usa `crond` do Alpine para chamar a REST API do SCDF nos horarios configurados.
-   **Configuracao:** Edite `docker/scdf/schedules.cron` e reinicie com
    `docker compose restart scdf-scheduler`
-   **Por que existe:** O botao "Create Schedule" do Dashboard SCDF nao funciona com
    o Docker Deployer (Local Platform). Esse container resolve a limitacao.

## 🚀 Como Usar

### Iniciar todos os serviços:

```bash
cd docker
docker-compose -f docker-compose.dev.yml up -d
```

### Verificar status:

```bash
docker-compose -f docker-compose.dev.yml ps
```

### Ver logs:

```bash
# Todos os serviços
docker-compose -f docker-compose.dev.yml logs -f

# Serviço específico
docker-compose -f docker-compose.dev.yml logs -f kafka
```

### Parar todos os serviços:

```bash
docker-compose -f docker-compose.dev.yml down
```

### Parar e remover volumes (limpa dados):

```bash
docker-compose -f docker-compose.dev.yml down -v
```

## 🔍 Healthchecks

Todos os serviços possuem healthchecks configurados:

-   **MySQL:** `mysqladmin ping`
-   **Redis:** `redis-cli ping`
-   **Zookeeper:** `nc -z localhost 2181`
-   **Kafka:** `kafka-broker-api-versions`

## 📊 Kafka UI - Funcionalidades

Acesse http://localhost:8090 para:

-   ✅ Visualizar tópicos e partições
-   ✅ Consumir mensagens de qualquer tópico
-   ✅ Publicar mensagens de teste
-   ✅ Monitorar consumer groups e lag
-   ✅ Ver configuração de brokers
-   ✅ Verificar schema registry (se habilitado)

## 🛠️ Configuração da Aplicação

A aplicação Java deve usar estas variáveis de ambiente:

```bash
# MySQL
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/cinelog
SPRING_DATASOURCE_USERNAME=cinelog
SPRING_DATASOURCE_PASSWORD=cinelog

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Já configurado em `application.yml` com valores padrão.

## 🐞 Troubleshooting

### Kafka não inicia:

```bash
# Limpar volumes e reiniciar
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d
```

### Erro de conexão Kafka:

-   Certifique-se de usar `localhost:9092` na aplicação Java
-   Use `kafka:29092` para comunicação entre containers Docker

### MySQL não aceita conexão:

```bash
# Verificar se está healthy
docker-compose -f docker-compose.dev.yml ps

# Ver logs
docker-compose -f docker-compose.dev.yml logs mysql
```

## 📝 Notas

-   **Volumes:** MySQL usa volume nomeado `mysql-data` (persiste entre restarts)
-   **Network:** Todos os serviços usam network bridge `cinelog-network`
-   **Auto-create topics:** Kafka cria tópicos automaticamente quando necessário
-   **Retention:** Mensagens Kafka são mantidas por 7 dias (168h)

## 🔗 Links Úteis

-   Kafka UI: http://localhost:8090
-   MySQL: localhost:3306
-   Redis: localhost:6379
-   Aplicação CineLog: http://localhost:8080 (após iniciar)
