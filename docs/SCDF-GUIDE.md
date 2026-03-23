# Spring Cloud Data Flow — Guia Completo para o CineLog

---

## Sumario

1. [Sobre o Spring Cloud Data Flow](#1-sobre-o-spring-cloud-data-flow)
2. [Arquitetura — Como Funciona](#2-arquitetura--como-funciona)
3. [Pre-requisitos e Setup Inicial](#3-pre-requisitos-e-setup-inicial)
4. [Tutorial: Executando um Batch Job pela Interface](#4-tutorial-executando-um-batch-job-pela-interface)
5. [Tutorial: Disparando via API REST](#5-tutorial-disparando-via-api-rest)
6. [Os 9 Batch Jobs do CineLog](#6-os-9-batch-jobs-do-cinelog)
7. [Monitoramento e Historico](#7-monitoramento-e-historico)
8. [Decisoes de Implementacao](#8-decisoes-de-implementacao)
9. [Troubleshooting](#9-troubleshooting)
10. [Referencia Rapida](#10-referencia-rapida)

---

## 1. Sobre o Spring Cloud Data Flow

### O que e SCDF?

Spring Cloud Data Flow (SCDF) e uma plataforma de orquestracao para pipelines de dados
e batch jobs em ambientes cloud-native. No CineLog, ele funciona como o **orquestrador
central** dos 9 batch jobs que sincronizam dados com a API do TMDB.

### Por que o CineLog precisa de SCDF?

Antes do SCDF, os batch jobs do CineLog eram disparados por `@Scheduled` anotacoes
diretamente no codigo Java. Isso trazia varios problemas:

| Problema com `@Scheduled`                  | Solucao com SCDF                                      |
|--------------------------------------------|-------------------------------------------------------|
| Crons hardcoded no codigo-fonte            | Crons configurados via Dashboard, sem redeploy        |
| Sem historico de execucoes                 | Historico completo persistido no MySQL                |
| Sem dashboard visual                       | Dashboard web com status em tempo real                |
| Disparo manual requer redeploy             | Disparo com um clique no Dashboard ou via curl        |
| Job roda no mesmo processo da API          | Container efemero isolado (sem afetar a API)          |
| Sem visibilidade de falhas                 | Exit code, logs e metricas por execucao               |
| Dificil escalar ou isolar jobs             | Cada job roda em seu proprio container Docker         |

### Beneficios Principais

- **Dashboard visual:** Interface web para disparar, monitorar e ver logs dos jobs
  sem precisar de acesso SSH ou terminal.

- **Historico persistido:** Toda execucao fica registrada no MySQL com data/hora,
  duracao, exit code, argumentos usados e mensagem de erro (se houver). Voce pode
  consultar o historico de meses atras.

- **Disparo manual sem redeploy:** Precisa re-executar o `importMoviesJob` com
  `max-pages=50`? Basta lancar pelo Dashboard com o argumento — sem tocar no codigo,
  sem rebuild, sem downtime na API.

- **Containers efemeros isolados:** Cada job roda em um container Docker separado.
  Se o job consumir muita memoria ou travar, ele nao afeta a API principal nem
  outros jobs. O container e criado na hora e destruido ao final.

---

## 2. Arquitetura — Como Funciona

### Diagrama do Fluxo Completo

```
                         [Usuario]
                            |
                   (1) Clica "Launch"
                   ou envia curl POST
                            |
                            v
    +-----------------------------------------------+
    |           SCDF Server (:9393)                 |
    |                                               |
    |  - Recebe requisicao de lancamento            |
    |  - Valida task definition                     |
    |  - Registra TASK_EXECUTION no MySQL (scdf)    |
    |  - Delega deploy ao Skipper                   |
    +----------------------+------------------------+
                           |
                  (2) POST /api/deployers
                           |
                           v
    +-----------------------------------------------+
    |          Skipper Server (:7577)                |
    |                                               |
    |  - Recebe instrucao de deploy                 |
    |  - Usa Docker Deployer (docker.sock)          |
    |  - Monta comando `docker run` com:            |
    |    - imagem: cinelog/cinelog-app:latest        |
    |    - rede: cinelog_default                     |
    |    - env vars: datasource, redis, profile     |
    |    - args: --spring.batch.job.name=<job>       |
    +----------------------+------------------------+
                           |
                  (3) docker run
                           |
                           v
    +-----------------------------------------------+
    |     Container Efemero (cinelog-app:latest)     |
    |     SPRING_PROFILES_ACTIVE=task               |
    |                                               |
    |  (4) Spring Boot inicia                       |
    |  (5) Conecta ao MySQL (db:3306/cinelog)       |
    |  (6) Conecta ao Redis (redis:6379)            |
    |  (7) Executa o batch job Spring Batch         |
    |      - Le dados do TMDB API                   |
    |      - Processa e persiste no MySQL           |
    |  (8) Registra resultado em TASK_EXECUTION     |
    |  (9) Envia metricas via OTLP push             |
    | (10) Encerra contexto Spring (close-context)  |
    | (11) Container para com exit code 0 ou 1      |
    +-------+----------+----------+-----------------+
            |          |          |
            v          v          v
    +----------+  +--------+  +-------------------+
    |  MySQL   |  | Redis  |  | OTel Collector    |
    |  :3306   |  | :6379  |  | :4318             |
    |          |  |        |  |   -> Prometheus    |
    | cinelog  |  | cache  |  |   -> Tempo/Jaeger  |
    | (app)    |  | de     |  |      (traces)     |
    |          |  | TMDB   |  |                   |
    | scdf     |  |        |  |                   |
    | (SCDF)   |  |        |  |                   |
    +----------+  +--------+  +-------------------+
```

### Componentes e Seus Papeis

| Componente          | Papel                                                                      |
|---------------------|----------------------------------------------------------------------------|
| **SCDF Server**     | Orquestrador principal. Expoe Dashboard web e REST API. Gerencia definicoes de tasks, schedules e historico de execucoes. Persiste metadados no schema `scdf` do MySQL. |
| **Skipper Server**  | Responsavel pelo deploy real dos containers. Recebe instrucoes do SCDF Server e executa `docker run` via Docker socket montado como volume. |
| **Docker Deployer** | Mecanismo interno do Skipper que converte a instrucao de deploy em um comando Docker. Usa `/var/run/docker.sock` para comunicar com o Docker daemon do host. |
| **Container Efemero** | Instancia temporaria da imagem `cinelog/cinelog-app:latest` que executa um unico batch job e encerra. Nao fica rodando permanentemente. |
| **MySQL**           | Dois schemas: `cinelog` (dados da app, tabelas Batch e Task) e `scdf` (metadados internos do SCDF/Skipper). |
| **Redis**           | Cache de respostas da TMDB API. Os containers efemeros acessam o mesmo Redis da app principal. |
| **OTel Collector**  | Recebe metricas e traces via OTLP push (necessario para containers efemeros que nao suportam Prometheus pull). |

### Container Efemero vs Aplicacao de Longa Duracao

A aplicacao CineLog pode rodar de duas formas:

```
+------------------------------+-----------------------------------+
|     App de Longa Duracao     |      Container Efemero (Task)     |
+------------------------------+-----------------------------------+
| Profile: dev, docker         | Profile: task                     |
| Processo permanente          | Processo temporario               |
| Serve endpoints REST         | Executa um unico batch job        |
| @Scheduled desabilitado*     | spring.batch.job.enabled=true     |
| Prometheus pull (scrape)     | OTLP push (container morre)      |
| Logs em arquivo + ELK        | Logs em console texto simples     |
| Nao encerra sozinha          | close-context-enabled=true        |
+------------------------------+-----------------------------------+
```

*Os agendamentos sao gerenciados pelo SCDF, nao mais pelo `@Scheduled` do Spring.

### Profile `task` vs Profile `dev`

O profile `task` (`application-task.yml`) configura a app para comportamento de
container efemero:

```yaml
spring:
  batch:
    job:
      enabled: true         # Executa o job automaticamente ao iniciar

  cloud:
    task:
      close-context-enabled: true       # Encerra a app apos o job terminar
      initialize-enabled: true          # Cria tabelas TASK_* se necessario

management:
  otlp:
    metrics:
      export:
        enabled: true                   # Push de metricas (nao pull)
        step: 5s
```

O profile `dev` (usado no desenvolvimento local com `./mvnw spring-boot:run`)
desabilita `spring.batch.job.enabled` e usa Prometheus pull normal.

---

## 3. Pre-requisitos e Setup Inicial

### Passo 1: Subir a Infraestrutura

```bash
# Na raiz do projeto
cd /home/maps/Projetos/cinelog/cinelog

# Sobe MySQL, Redis, SCDF Server, Skipper, observabilidade e demais servicos
docker compose up -d
```

Aguarde os servicos ficarem saudaveis (cerca de 90 segundos para o SCDF):

```bash
# Verificar status dos containers SCDF
docker ps --format '{{.Names}} {{.Status}}' | grep -E "dataflow|skipper"

# Saida esperada (apos ~90s):
# cinelog-dataflow Up 2 minutes (healthy)
# cinelog-skipper  Up 2 minutes (healthy)
```

### Passo 2: Construir a Imagem Docker da Aplicacao

```bash
# Na raiz do projeto — gera a imagem que os containers efemeros usarao
docker build -t cinelog/cinelog-app:latest .
```

**Importante:** Sempre reconstrua a imagem apos alterar codigo-fonte dos batch jobs.
O SCDF usa a tag `:latest`, entao o proximo lancamento automaticamente pegara a nova
imagem. Nao e necessario re-registrar as tasks.

### Passo 3: Registrar Tasks e Schedules no SCDF

```bash
# Script idempotente — pode executar multiplas vezes sem duplicar registros
bash docker/scdf/init-scdf.sh
```

O script faz tres coisas:

1. **Registra a app `cinelog`** — associa o nome `cinelog` a imagem Docker
   `docker:cinelog/cinelog-app:latest` no registro de apps do SCDF.

2. **Cria 12 task definitions** — cada uma aponta para a app `cinelog` com o argumento
   `--spring.batch.job.name=<nomeDoJob>` (9 TMDB + 3 email).

3. **Cria 8 schedules** — agendamentos CRON semanais para os jobs TMDB (exceto
   `linkTmdbJob` e os 3 jobs de email que sao somente manuais).

Saida esperada:

```
=== SCDF Init: Registrando app, tasks e schedules ===
  SCDF URL: http://localhost:9393
  Image:    docker:cinelog/cinelog-app:latest

Aguardando SCDF... OK

--- App Registration ---
  [ok] App 'cinelog' registrada (docker:cinelog/cinelog-app:latest)

--- Task Definitions (9) ---
  [ok] Task 'syncGenresJob' registrada (job: syncGenresJob)
  [ok] Task 'importMoviesJob' registrada (job: importMoviesJob)
  ...

--- Schedules (8) ---
  [ok] Schedule 'sched-sync-genres' (0 0 3 * * 0)
  ...

=== SCDF Init: Concluido ===
  Tasks registradas: 9
  Dashboard: http://localhost:9393/dashboard
```

### Verificacao Final

```bash
# Confirma que SCDF responde
curl -sf http://localhost:9393/about | python3 -m json.tool | head -5

# Confirma que as 9 tasks estao registradas
curl -sf http://localhost:9393/tasks/definitions | \
  python3 -c "
import sys, json
data = json.load(sys.stdin)
for t in data.get('_embedded', {}).get('taskDefinitionResourceList', []):
    print(f\"  {t['name']}: {t['dslText']}\")
"
```

---

## 4. Tutorial: Executando um Batch Job pela Interface

Este tutorial assume que voce nunca usou o SCDF antes. Cada passo inclui exatamente
o que clicar e o que esperar ver na tela.

### 4.1 Acessando o Dashboard

1. Abra o navegador e acesse:

   ```
   http://localhost:9393/dashboard
   ```

2. O Dashboard abre diretamente — nao pede login em modo desenvolvimento.

3. Voce vera a tela inicial com:
   - Um **menu lateral esquerdo** com os itens de navegacao
   - Um **painel central** com informacoes gerais sobre o SCDF Server (versao,
     features habilitadas, status)

4. O menu lateral tem os seguintes itens:

   | Item do Menu       | O que mostra                                                          |
   |--------------------|-----------------------------------------------------------------------|
   | **Apps**           | Aplicacoes registradas (imagens Docker). No CineLog, voce vera a app `cinelog` do tipo `task`. |
   | **Tasks/Jobs**     | Tudo sobre tasks: definicoes, lancamento, execucoes passadas, logs de cada execucao. Este e o item mais usado. |
   | **Schedules**      | Agendamentos CRON gerenciados pelo SCDF. Voce pode criar, ver e deletar schedules aqui. |
   | **Audit Records**  | Historico de todas as acoes realizadas no SCDF: criacao de tasks, lancamentos, delecoes. Util para auditoria. |

### 4.2 Verificando Tasks Registradas

1. No menu lateral esquerdo, clique em **Tasks/Jobs**.

2. O submenu se expande. Clique em **Definitions** (ou a tela ja mostra as definicoes
   automaticamente).

3. Voce vera uma tabela listando todas as tasks registradas. Cada linha mostra:
   - **Name:** Nome da task (ex: `syncGenresJob`)
   - **Definition:** Comando que sera executado (ex: `cinelog --spring.batch.job.name=syncGenresJob`)
   - **Status:** Se a task tem alguma execucao ativa no momento
   - **Acoes:** Botoes de Launch (play), Destroy, e detalhes

4. As 9 tasks do CineLog devem aparecer assim:

   | Task Name                | Definition (DSL)                                              |
   |--------------------------|---------------------------------------------------------------|
   | `syncGenresJob`          | `cinelog --spring.batch.job.name=syncGenresJob`               |
   | `importMoviesJob`        | `cinelog --spring.batch.job.name=importMoviesJob`             |
   | `importTvShowsJob`       | `cinelog --spring.batch.job.name=importTvShowsJob`            |
   | `importCreditsJob`       | `cinelog --spring.batch.job.name=importCreditsJob`            |
   | `importSeasonsJob`       | `cinelog --spring.batch.job.name=importSeasonsJob`            |
   | `syncReviewsJob`         | `cinelog --spring.batch.job.name=syncReviewsJob`              |
   | `enrichMediaImagesJob`   | `cinelog --spring.batch.job.name=enrichMediaImagesJob`        |
   | `enrichPersonProfilesJob`| `cinelog --spring.batch.job.name=enrichPersonProfilesJob`     |
   | `linkTmdbJob`            | `cinelog --spring.batch.job.name=linkTmdbJob`                 |

   Se a tabela estiver vazia, execute `bash docker/scdf/init-scdf.sh` novamente.

### 4.3 Lancando uma Task (Executando um Batch Job)

Vamos usar o `syncGenresJob` como exemplo — ele e o mais rapido e ideal para testar.

**Passo 1:** No menu lateral, clique em **Tasks/Jobs**.

**Passo 2:** Na lista de tasks, localize `syncGenresJob`. Voce pode usar a barra de
busca no topo da tabela se houver muitas tasks.

**Passo 3:** Na linha do `syncGenresJob`, clique no botao **Launch** (icone de play,
geralmente um triangulo apontando para a direita). Isso abre a tela de lancamento.

**Passo 4:** Na tela "Launch Task", voce vera dois campos principais:

- **Arguments (Argumentos):** Parametros de linha de comando passados para a app.
  Para execucao padrao, **deixe vazio**. Se quiser customizar, voce pode adicionar
  argumentos como:
  ```
  --cinelog.batch.max-pages=20
  ```

- **Properties (Propriedades de Deploy):** Configuracoes que o SCDF injeta no
  container. O `deployer-env.properties` ja configura as variaveis padrao via o
  script `init-scdf.sh`, mas se precisar sobrescrever manualmente, adicione as
  propriedades abaixo (uma por linha):

  ```
  deployer.syncGenresJob.local.docker.network=cinelog_default
  app.syncGenresJob.spring.datasource.url=jdbc:mysql://db:3306/cinelog?useSSL=false&allowPublicKeyRetrieval=true
  app.syncGenresJob.spring.datasource.username=cinelog
  app.syncGenresJob.spring.datasource.password=cinelog
  app.syncGenresJob.spring.data.redis.host=redis
  app.syncGenresJob.spring.profiles.active=task
  app.syncGenresJob.spring.mail.host=mailhog
  app.syncGenresJob.spring.mail.port=1025
  app.syncGenresJob.spring.kafka.bootstrap-servers=localhost:9092
  ```

  **Nota:** Na maioria dos casos, as propriedades ja estao configuradas pelo
  `deployer-env.properties` e voce nao precisa adicionar nada manualmente. So
  adicione se estiver depurando ou sobrescrevendo valores.

**Passo 5:** Clique no botao **Launch the task** (botao azul/verde no canto inferior
ou superior direito da tela).

**Passo 6 — O que acontece nos bastidores:**

```
Voce clica "Launch"
       |
       v
SCDF Server registra a execucao no MySQL (schema scdf)
       |
       v
SCDF pede ao Skipper para deploiar o container
       |
       v
Skipper executa: docker run --network cinelog_default \
    -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/cinelog?... \
    -e SPRING_PROFILES_ACTIVE=task \
    cinelog/cinelog-app:latest \
    --spring.batch.job.name=syncGenresJob
       |
       v
Container inicia, executa syncGenresJob, grava resultados, encerra
       |
       v
SCDF atualiza TASK_EXECUTION com exit code e duracao
```

**Passo 7:** Apos clicar em "Launch", voce e redirecionado automaticamente para a
tela de **Executions**, onde pode acompanhar o progresso.

### 4.4 Acompanhando a Execucao

1. Apos lancar a task, voce esta na tela **Tasks/Jobs -> Executions**.

2. A execucao mais recente aparece no topo da lista. Os campos visiveis sao:

   | Campo              | Descricao                                                    |
   |--------------------|--------------------------------------------------------------|
   | **Execution ID**   | Identificador unico da execucao (numero sequencial)          |
   | **Task Name**      | Nome da task (ex: `syncGenresJob`)                           |
   | **Start Time**     | Data e hora de inicio da execucao                            |
   | **End Time**       | Data e hora de termino (vazio enquanto estiver rodando)       |
   | **Exit Code**      | `0` = sucesso, qualquer outro valor = erro                   |
   | **Status**         | Estado atual da execucao                                     |

3. Os possiveis estados sao:

   | Status        | Significado                                                    |
   |---------------|----------------------------------------------------------------|
   | `RUNNING`     | Container em execucao, job ainda processando                   |
   | `COMPLETE`    | Job terminou com sucesso (exit code 0)                         |
   | `ERROR`       | Job falhou (exit code != 0). Clique para ver a causa           |

4. Para ver os **detalhes completos** de uma execucao, clique no **Execution ID**
   (numero azul clicavel). A tela de detalhes mostra:

   - **Exit Code:** `0` para sucesso, `1` para erro
   - **Exit Message:** Mensagem descritiva do resultado (vazio em caso de sucesso,
     contem a stack trace em caso de erro)
   - **Start Time / End Time:** Timestamps exatos
   - **Arguments:** Lista dos argumentos passados na hora do lancamento
   - **External Execution ID:** ID do container Docker que executou o job
   - **Resource URL:** Imagem Docker usada
   - **Job Execution(s):** Link para os detalhes do Spring Batch Job, incluindo
     steps executados, itens lidos/escritos/pulados

5. Para ver os **steps do job**, clique no link de **Job Execution** dentro dos
   detalhes da task execution. Voce vera:

   - Nome do step (ex: `syncGenresStep`)
   - Status do step (`COMPLETED` ou `FAILED`)
   - Read Count (itens lidos da TMDB API)
   - Write Count (itens gravados no MySQL)
   - Skip Count (itens pulados por erro)
   - Duracao do step

### 4.5 Vendo os Logs

#### Via Dashboard

1. Na tela de detalhes da execucao (apos clicar no Execution ID), procure o botao
   ou aba **Log**.

2. Os logs do container Docker sao exibidos diretamente no navegador. Voce vera a
   saida do Spring Boot, incluindo:

   ```
   --- Exemplo de log de SUCESSO ---

     .   ____          _            __ _ _
    /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
   ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
    \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
     '  |____| .__|_| |_|_| |_\__, | / / / /
    =========|_|==============|___/=/_/_/_/

   2026-03-19T03:00:01.234Z  INFO  --- Started CinelogApplication in 4.231 seconds
   2026-03-19T03:00:01.567Z  INFO  --- Executing step: [syncGenresStep]
   2026-03-19T03:00:03.890Z  INFO  --- TMDB genres fetched: 19 movie genres, 16 tv genres
   2026-03-19T03:00:04.123Z  INFO  --- Step: [syncGenresStep] executed in 2s556ms
   2026-03-19T03:00:04.234Z  INFO  --- Job: [syncGenresJob] completed with status COMPLETED
   2026-03-19T03:00:04.345Z  INFO  --- Spring Cloud Task closed context.
   ```

   ```
   --- Exemplo de log de ERRO ---

   2026-03-19T03:00:01.234Z  INFO  --- Started CinelogApplication in 4.231 seconds
   2026-03-19T03:00:01.567Z  INFO  --- Executing step: [importMoviesStep]
   2026-03-19T03:00:02.890Z ERROR  --- WebClientResponseException: 401 Unauthorized
   2026-03-19T03:00:02.891Z ERROR  --- TMDB API key is missing or invalid
   2026-03-19T03:00:03.123Z  INFO  --- Step: [importMoviesStep] executed in 1s556ms
   2026-03-19T03:00:03.234Z  INFO  --- Job: [importMoviesJob] completed with status FAILED
   ```

#### Via Terminal (docker logs)

Se o Dashboard nao mostrar os logs ou voce preferir o terminal:

```bash
# Listar containers de task recentes (inclusive os que ja encerraram)
docker ps -a --filter "ancestor=cinelog/cinelog-app:latest" \
  --format 'table {{.Names}}\t{{.Status}}\t{{.CreatedAt}}'

# Ver logs de um container especifico
docker logs <container-name>

# Ver logs com follow (enquanto o container estiver rodando)
docker logs -f <container-name>

# Ver apenas as ultimas 50 linhas
docker logs --tail 50 <container-name>

# Filtrar por job name nos containers
docker ps -a --filter "ancestor=cinelog/cinelog-app:latest" \
  --format '{{.Names}} {{.Status}}' | grep syncGenresJob
```

### 4.6 Agendando Execucoes (Schedules)

#### Limitacao: Scheduling no Local Deployer

> **IMPORTANTE:** O SCDF Local Deployer (Docker) **NAO suporta scheduling nativo**.
> O botao "Create Schedule" no Dashboard retorna o erro:
> `"Scheduling is not implemented for local platform."`
>
> Scheduling nativo so funciona com Kubernetes (CronJob) ou Cloud Foundry (PCF Scheduler).
> Para ambiente local/Docker, use o **crontab do host** conforme descrito abaixo.

#### Solucao: Crontab do Host + Script `scdf-schedule.sh`

O script `scripts/scdf-schedule.sh` lanca tasks via REST API do SCDF e pode ser
chamado pelo crontab do Linux:

```bash
# Uso direto (teste):
bash scripts/scdf-schedule.sh sendWeeklyDigestJob

# Com argumentos customizados:
bash scripts/scdf-schedule.sh importMoviesJob "--cinelog.batch.max-pages=50"
```

#### Configurando o Crontab

```bash
# Abrir o crontab para edicao
crontab -e

# Adicionar os agendamentos desejados (horario local):
# ── TMDB Import (domingos) ──
0  3 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh syncGenresJob           >> /tmp/scdf-cron.log 2>&1
30 3 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh importMoviesJob         >> /tmp/scdf-cron.log 2>&1
0  4 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh importTvShowsJob        >> /tmp/scdf-cron.log 2>&1
30 4 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh importCreditsJob        >> /tmp/scdf-cron.log 2>&1
0  5 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh importSeasonsJob        >> /tmp/scdf-cron.log 2>&1
30 5 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh syncReviewsJob          >> /tmp/scdf-cron.log 2>&1
0  6 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh enrichMediaImagesJob    >> /tmp/scdf-cron.log 2>&1
30 6 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh enrichPersonProfilesJob >> /tmp/scdf-cron.log 2>&1

# ── Email Reports ──
0  8 * * 1 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh sendWeeklyDigestJob     >> /tmp/scdf-cron.log 2>&1
0 18 * * 5 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh sendTrendingReportJob   >> /tmp/scdf-cron.log 2>&1
0  6 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh sendPlatformReportJob   >> /tmp/scdf-cron.log 2>&1
```

#### Verificando Agendamentos

```bash
# Listar crontab ativo
crontab -l

# Ver log de execucoes
tail -f /tmp/scdf-cron.log
```

#### Expressoes Cron Comuns

| Expressao | Significado |
|---|---|
| `0 8 * * 1` | Segunda-feira 08:00 |
| `0 18 * * 5` | Sexta-feira 18:00 |
| `0 3 * * 0` | Domingo 03:00 |
| `*/5 * * * *` | A cada 5 minutos |
| `0 0 1 * *` | Primeiro dia do mes 00:00 |

> **Nota:** Quando migrar para Kubernetes, os crontabs serao substituidos por
> CronJobs nativos gerenciados pelo SCDF Kubernetes Deployer.

---

## 5. Tutorial: Disparando via API REST

O SCDF expoe uma API REST completa. Todos os comandos abaixo podem ser executados
a partir de qualquer terminal com `curl`.

### Lancar uma Task

```bash
# Lancar syncGenresJob com configuracao padrao
curl -X POST "http://localhost:9393/tasks/executions/launch" \
  -d "name=syncGenresJob" \
  -d "properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3" \
  -u admin:'Admin@CineLog2025!'

# Resposta: ID da execucao criada
# {"executionId": 42, "schemaTarget": "boot3"}
```

```bash
# Lancar importMoviesJob com argumento customizado (mais paginas)
curl -X POST "http://localhost:9393/tasks/executions/launch" \
  -d "name=importMoviesJob" \
  -d "properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3" \
  -d "arguments=--cinelog.batch.max-pages=20" \
  -u admin:'Admin@CineLog2025!'
```

```bash
# Lancar job de email (digest semanal)
curl -X POST "http://localhost:9393/tasks/executions/launch" \
  -d "name=sendWeeklyDigestJob" \
  -d "properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3" \
  -u admin:'Admin@CineLog2025!'
```

**Importante:** Use o endpoint `/tasks/executions/launch` (nao `/tasks/executions`)
e inclua `bootVersion=3` nas properties. Sem isso, o SCDF tenta usar schema Boot 2.

### Ver Execucoes Recentes

```bash
# Ultimas 5 execucoes (ordenadas pela mais recente)
curl -s "http://localhost:9393/tasks/executions?size=5&sort=TASK_EXECUTION_ID,desc" \
  | python3 -c "
import sys, json
data = json.load(sys.stdin)
for e in data.get('_embedded', {}).get('taskExecutionResourceList', []):
    print(f\"  ID:{e['executionId']}  Task:{e['taskName']}  Exit:{e['exitCode']}  Start:{e['startTime']}\")
"
```

### Ver Detalhes de uma Execucao Especifica

```bash
# Substitua 42 pelo ID da execucao
curl -s "http://localhost:9393/tasks/executions/42" | python3 -m json.tool
```

### Ver Definicoes de Tasks

```bash
curl -s "http://localhost:9393/tasks/definitions" \
  | python3 -c "
import sys, json
data = json.load(sys.stdin)
for t in data.get('_embedded', {}).get('taskDefinitionResourceList', []):
    print(f\"  {t['name']}: {t['dslText']}\")
"
```

### Ver Apps Registradas

```bash
curl -s "http://localhost:9393/apps?type=task" | python3 -m json.tool
```

### Criar um Schedule via API

```bash
curl -X POST "http://localhost:9393/tasks/schedules" \
  -d "scheduleName=sched-custom-movies" \
  -d "taskDefinitionName=importMoviesJob" \
  -d "properties=scheduler.cron.expression=0 0 */12 * * *"
```

### Listar Schedules

```bash
curl -s "http://localhost:9393/tasks/schedules" \
  | python3 -c "
import sys, json
data = json.load(sys.stdin)
for s in data.get('_embedded', {}).get('scheduleInfoResourceList', []):
    props = s.get('scheduleProperties', {})
    cron = props.get('spring.cloud.scheduler.cron.expression', 'N/A')
    print(f\"  {s['scheduleName']}: task={s['taskDefinitionName']}  cron={cron}\")
"
```

### Deletar um Schedule

```bash
curl -X DELETE "http://localhost:9393/tasks/schedules/sched-custom-movies"
```

### Limpar Execucoes Finalizadas

```bash
# Limpar execucoes completadas (sem deletar as de erro para analise)
curl -X DELETE "http://localhost:9393/tasks/executions?completed=true&actions=CLEANUP,REMOVE_DATA"
```

---

## 6. Os 12 Batch Jobs do CineLog

### Jobs de Importacao TMDB (9 jobs)

| Job                        | Descricao                                                  | TMDB API Endpoint          | Dependencias (deve rodar antes)       | Schedule          |
|----------------------------|------------------------------------------------------------|----------------------------|---------------------------------------|-------------------|
| `syncGenresJob`            | Sincroniza generos de filmes e series do TMDB              | `/genre/movie/list`, `/genre/tv/list` | Nenhuma                     | Dom 03:00 UTC     |
| `importMoviesJob`          | Importa filmes populares via TMDB Discover                 | `/discover/movie`          | `syncGenresJob` (generos sao FK)      | Dom 03:30 UTC     |
| `importTvShowsJob`         | Importa series populares via TMDB Discover                 | `/discover/tv`             | `syncGenresJob` (generos sao FK)      | Dom 04:00 UTC     |
| `linkTmdbJob`              | Vincula midias de seed ao TMDB via busca por titulo         | `/search/movie`, `/search/tv` | Midias de seed no banco            | Somente manual    |
| `importCreditsJob`         | Importa elenco e equipe (cast/crew) de midias com tmdb_id  | `/movie/{id}/credits`, `/tv/{id}/credits` | `importMoviesJob` e/ou `importTvShowsJob` | Dom 04:30 UTC |
| `importSeasonsJob`         | Importa temporadas e episodios de series                   | `/tv/{id}/season/{n}`      | `importTvShowsJob`                    | Dom 05:00 UTC     |
| `syncReviewsJob`           | Sincroniza reviews de usuarios do TMDB                     | `/movie/{id}/reviews`, `/tv/{id}/reviews` | `importMoviesJob` e/ou `importTvShowsJob` | Dom 05:30 UTC |
| `enrichMediaImagesJob`     | Enriquece midias que nao possuem poster ou backdrop         | `/movie/{id}/images`, `/tv/{id}/images` | `importMoviesJob` e/ou `importTvShowsJob` | Dom 06:00 UTC |
| `enrichPersonProfilesJob`  | Enriquece perfis de pessoas sem foto ou biografia           | `/person/{id}`             | `importCreditsJob`                    | Dom 06:30 UTC     |

### Jobs de Email/Relatorios (3 jobs)

| Job                        | Descricao                                                  | Destinatarios              | Verificacao (dev)                     | Schedule          |
|----------------------------|------------------------------------------------------------|----------------------------|---------------------------------------|-------------------|
| `sendWeeklyDigestJob`      | Envia digest semanal personalizado para cada usuario       | Todos os usuarios          | MailHog http://localhost:8025         | Somente manual    |
| `sendTrendingReportJob`    | Envia relatorio de midias em alta para cada usuario        | Todos os usuarios          | MailHog http://localhost:8025         | Somente manual    |
| `sendPlatformReportJob`    | Envia relatorio de metricas da plataforma para admin       | Admin (1 email)            | MailHog http://localhost:8025         | Somente manual    |

Os jobs de email usam variantes **blocking** do `ReportEmailService` para garantir
que todos os emails sao enviados antes do container encerrar. Em dev, os emails
chegam no MailHog (http://localhost:8025). Em producao, sao enviados via Gmail/SMTP
configurado no `.env.mail`.

### Parametros de Configuracao dos Jobs

```yaml
cinelog:
  batch:
    max-pages: 10          # Paginas do TMDB por execucao (cada pagina = 20 itens)
    chunk-size: 20         # Itens processados por commit do Spring Batch
    skip-limit: 50         # Maximo de itens com erro tolerados por job
    retry-limit: 3         # Tentativas em caso de erro de rede
```

Para sobrescrever na hora do lancamento, passe como argumento:

```bash
curl -X POST "http://localhost:9393/tasks/executions" \
  -d "name=importMoviesJob" \
  -d "arguments=--cinelog.batch.max-pages=50 --cinelog.batch.chunk-size=10"
```

### Ordem Recomendada para Carga Inicial

Quando voce esta populando o catalogo do zero (banco vazio), execute os jobs nesta
ordem. A ordem importa porque existem dependencias entre as tabelas:

```
PASSO  JOB                       POR QUE NESTA ORDEM
─────  ────────────────────────   ─────────────────────────────────────────────
  1    syncGenresJob              Generos sao FK nas tabelas de midia.
                                  Sem generos, filmes e series nao podem ser
                                  importados corretamente.

  2    importMoviesJob            Popula a tabela de filmes. Depende dos generos
                                  do passo 1.

  3    importTvShowsJob           Popula a tabela de series. Depende dos generos
                                  do passo 1.

  4    linkTmdbJob                Vincula midias que ja existiam no banco (seed
                                  manual ou migracao) ao TMDB via busca por
                                  titulo. So faz sentido se houver midias sem
                                  tmdb_id no banco.

  5    importCreditsJob           Importa elenco e equipe. Depende das midias
                                  dos passos 2 e 3 (precisa de tmdb_id).

  6    importSeasonsJob           Importa temporadas e episodios. Depende das
                                  series do passo 3.

  7    enrichMediaImagesJob       Busca posters e backdrops para midias que
                                  ainda nao tem. Depende das midias dos passos
                                  2 e 3.

  8    enrichPersonProfilesJob    Busca fotos e biografias para pessoas sem
                                  perfil completo. Depende do passo 5 (credits).

  9    syncReviewsJob             Importa reviews do TMDB. Depende das midias
                                  dos passos 2 e 3. Deixado por ultimo porque
                                  nao e pre-requisito de nenhum outro job.
```

**Dica para carga inicial:** Passe `--cinelog.batch.max-pages=20` ou mais nos
argumentos para trazer um volume maior de dados na primeira execucao.

### Tolerancia a Falhas

Os batch jobs sao resilientes a erros de rede e dados corrompidos:

```
Retry:  3 tentativas com backoff exponencial (1s -> 2s -> 4s, max 10s)
Skip:   Ate 50 itens com erro por job (100 para reviews)
Erros retentados: WebClientResponseException, RestClientException,
                  ConnectException, SocketTimeoutException
```

| Situacao                       | Comportamento                                          |
|--------------------------------|--------------------------------------------------------|
| TMDB API fora do ar            | Retry 3x com backoff, depois skip item                 |
| Midia duplicada no banco       | Skip (constraint violation), continua processamento    |
| Rate limit do TMDB (429)       | Retry com backoff exponencial                          |
| Mais de 50 erros em um job     | Job falha (FAILED), itens ja escritos sao mantidos     |
| Job anterior ainda rodando     | `JobInstanceAlreadyCompleteException` — job nao inicia |

---

## 7. Monitoramento e Historico

### Via SCDF Dashboard (recomendado)

1. **Tasks/Jobs -> Executions** — lista todas as execucoes com status, duracao e exit
   code. Filtre por task name ou status (COMPLETE, ERROR, RUNNING).

2. Clique em uma execucao para ver detalhes completos: argumentos, exit message, logs,
   e link para os steps do Spring Batch.

3. **Tasks/Jobs -> Job Executions** — visualizacao especifica dos jobs Spring Batch,
   com detalhamento por step (read count, write count, skip count).

### Via Banco de Dados (Queries SQL)

As execucoes sao persistidas em duas camadas de tabelas no schema `cinelog`:

- **Tabelas SCDF_TASK_*:** Gerenciadas pelo Spring Cloud Task. Registram a execucao
  da task (container).
- **Tabelas BATCH_*:** Gerenciadas pelo Spring Batch. Registram a execucao do job
  e seus steps internos.

```sql
-- ============================================================================
-- Historico de execucoes de tasks (nivel container)
-- ============================================================================
SELECT
    TASK_EXECUTION_ID   AS id,
    TASK_NAME           AS task,
    EXIT_CODE           AS exit_code,
    START_TIME          AS inicio,
    END_TIME            AS fim,
    TIMESTAMPDIFF(SECOND, START_TIME, END_TIME) AS duracao_seg,
    EXIT_MESSAGE        AS mensagem
FROM cinelog.BOOT3_TASK_EXECUTION
ORDER BY TASK_EXECUTION_ID DESC
LIMIT 20;

-- ============================================================================
-- Historico de execucoes de jobs Spring Batch (nivel job)
-- ============================================================================
SELECT
    bji.JOB_NAME    AS job,
    bje.STATUS      AS status,
    bje.START_TIME  AS inicio,
    bje.END_TIME    AS fim,
    TIMESTAMPDIFF(SECOND, bje.START_TIME, bje.END_TIME) AS duracao_seg,
    bje.EXIT_CODE   AS exit_code,
    bje.EXIT_MESSAGE AS mensagem
FROM cinelog.BOOT3_BATCH_JOB_EXECUTION bje
JOIN cinelog.BATCH_JOB_INSTANCE bji
    ON bje.JOB_INSTANCE_ID = bji.JOB_INSTANCE_ID
ORDER BY bje.START_TIME DESC
LIMIT 20;

-- ============================================================================
-- Detalhamento por step (itens lidos, escritos, pulados)
-- ============================================================================
SELECT
    bse.STEP_NAME      AS step,
    bse.STATUS          AS status,
    bse.READ_COUNT      AS lidos,
    bse.WRITE_COUNT     AS escritos,
    bse.SKIP_COUNT      AS pulados,
    bse.COMMIT_COUNT    AS commits,
    bse.ROLLBACK_COUNT  AS rollbacks,
    TIMESTAMPDIFF(SECOND, bse.START_TIME, bse.END_TIME) AS duracao_seg,
    bse.EXIT_MESSAGE    AS mensagem
FROM cinelog.BOOT3_BATCH_STEP_EXECUTION bse
ORDER BY bse.START_TIME DESC
LIMIT 20;

-- ============================================================================
-- Steps com falha (para debug rapido)
-- ============================================================================
SELECT
    bse.STEP_NAME      AS step,
    bse.STATUS          AS status,
    bse.READ_COUNT      AS lidos,
    bse.WRITE_COUNT     AS escritos,
    bse.SKIP_COUNT      AS pulados,
    SUBSTRING(bse.EXIT_MESSAGE, 1, 200) AS erro_resumido
FROM cinelog.BOOT3_BATCH_STEP_EXECUTION bse
WHERE bse.STATUS = 'FAILED'
ORDER BY bse.START_TIME DESC
LIMIT 20;

-- ============================================================================
-- Resumo agregado: quantas vezes cada job rodou e taxa de sucesso
-- ============================================================================
SELECT
    bji.JOB_NAME                                        AS job,
    COUNT(*)                                            AS total_execucoes,
    SUM(CASE WHEN bje.STATUS = 'COMPLETED' THEN 1 ELSE 0 END) AS sucesso,
    SUM(CASE WHEN bje.STATUS = 'FAILED'    THEN 1 ELSE 0 END) AS falha,
    ROUND(AVG(TIMESTAMPDIFF(SECOND, bje.START_TIME, bje.END_TIME)), 1) AS duracao_media_seg
FROM cinelog.BOOT3_BATCH_JOB_EXECUTION bje
JOIN cinelog.BATCH_JOB_INSTANCE bji
    ON bje.JOB_INSTANCE_ID = bji.JOB_INSTANCE_ID
GROUP BY bji.JOB_NAME
ORDER BY bji.JOB_NAME;
```

### Via Prometheus / Grafana

Os containers efemeros enviam metricas via OTLP push para o OTel Collector, que
exporta para o Prometheus.

```bash
# Verificar metricas de batch expostas
curl -s http://localhost:9090/api/v1/label/__name__/values \
  | python3 -c "
import sys, json
names = json.load(sys.stdin).get('data', [])
for n in names:
    if 'batch' in n.lower() or 'task' in n.lower():
        print(f'  {n}')
"
```

Metricas disponiveis:

| Metrica                          | Descricao                                |
|----------------------------------|------------------------------------------|
| `batch_job_duration_seconds`     | Duracao total do job                     |
| `batch_job_completed_total`      | Contador de jobs completados (por status) |
| `batch_step_items_read_total`    | Itens lidos por step                     |
| `batch_step_items_written_total` | Itens escritos por step                  |
| `batch_step_items_skipped_total` | Itens pulados por step (erro tolerado)   |
| `batch_step_duration_seconds`    | Duracao do step                          |

**Dashboard Grafana:** Acesse http://localhost:3000 (admin/admin) para visualizar
dashboards pre-configurados com essas metricas.

---

## 8. Decisoes de Implementacao

Esta secao explica as decisoes tecnicas mais importantes da integracao SCDF e seus
motivos. Util para quem esta dando manutencao no projeto ou enfrentando erros
relacionados.

### Por que MariaDB Connector/J?

**Problema:** A imagem oficial do SCDF (`springcloud/spring-cloud-dataflow-server:2.11.5`)
nao inclui o MySQL Connector/J (`com.mysql.cj.jdbc.Driver`) no classpath. Tentar
usar esse driver resulta em `ClassNotFoundException`.

**Solucao:** A imagem ja inclui o MariaDB Connector/J (`org.mariadb.jdbc.Driver`),
que e totalmente compativel com MySQL 8.0. Usamos esse driver tanto no SCDF Server
quanto no Skipper Server:

```yaml
SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.mariadb.jdbc.Driver
```

### Por que `permitMysqlScheme=true`?

**Problema:** O MariaDB Connector/J por padrao espera URLs com prefixo `jdbc:mariadb://`.
Porem, o SCDF usa o prefixo da URL (`jdbc:mysql://` vs `jdbc:mariadb://`) para
detectar qual banco esta sendo usado e aplicar as migrations Flyway corretas. Se
usarmos `jdbc:mariadb://`, o SCDF aplica migrations de MariaDB que sao
incompativeis com MySQL 8.0.

**Solucao:** O parametro `permitMysqlScheme=true` na URL permite que o MariaDB
Connector/J aceite URLs `jdbc:mysql://`, fazendo o SCDF detectar MySQL corretamente:

```
jdbc:mysql://db:3306/scdf?useSSL=false&allowPublicKeyRetrieval=true&permitMysqlScheme=true
```

### Por que Docker CLI estatico + docker-wrapper.sh?

**Problema:** O SCDF/Skipper precisa executar `docker run` para lancar containers de
task, mas a imagem oficial do SCDF nao inclui o Docker CLI. Alem disso, o SCDF Local
Deployer 2.11.x pode hardcodar `--network bridge` e nao adiciona `--rm` para limpeza
automatica de containers.

**Solucao:** Um binario estatico do Docker CLI (`docker-cli`) e montado como `docker-real`,
e um script wrapper (`docker-wrapper.sh`) e montado como `docker`:

```yaml
volumes:
  - ./docker/scdf/docker-cli:/usr/local/bin/docker-real:ro      # Binario real
  - ./docker/scdf/docker-wrapper.sh:/usr/local/bin/docker:ro    # Wrapper
```

O wrapper intercepta comandos `docker run` e:
1. Substitui `--network bridge` por `--network cinelog_default`
2. Injeta `--rm` para garantir que o container seja removido apos encerrar

Isso e mais confiavel que a opcao `DELETE_CONTAINER_ON_EXIT` do SCDF, pois o
Docker daemon remove o container atomicamente — sem depender do Skipper para cleanup.

### Por que Resilience4j 2.2.0?

**Problema:** O Spring Cloud Data Flow 2.11.x traz como dependencia transitiva o
`spring-cloud-starter-circuitbreaker-resilience4j`. A versao 2.3.0 do Resilience4j
removeu a classe `RxJava3FallbackDecorator`, causando `ClassNotFoundException` na
inicializacao.

**Solucao:** Fixamos a versao do Resilience4j em 2.2.0 no `pom.xml` e incluimos
explicitamente `resilience4j-rxjava3`:

```xml
<resilience4j.version>2.2.0</resilience4j.version>

<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>${resilience4j.version}</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-rxjava3</artifactId>
    <version>${resilience4j.version}</version>
</dependency>
```

### Por que TaskConfigurer explicito?

**Problema:** Quando a app tem multiplos beans `DataSource` (ex: datasource-proxy
wrapping o DataSource real para logging SQL), o Spring Cloud Task nao sabe qual usar
e falha com `NoUniqueBeanDefinitionException`.

**Solucao:** A classe `TaskConfig` registra um `DefaultTaskConfigurer` explicito
apontando para o DataSource correto:

```java
@Configuration
@EnableTask
@Profile("task")
public class TaskConfig {
    @Bean
    TaskConfigurer taskConfigurer(DataSource dataSource) {
        return new DefaultTaskConfigurer(dataSource);
    }
}
```

Arquivo: `src/main/java/com/cine/cinelog/features/batch/config/TaskConfig.java`

### Por que @Primary no TransactionManager?

**Problema:** Com Spring Batch + Spring Cloud Task + datasource-proxy, existem
multiplos `PlatformTransactionManager` no contexto. O Spring Batch precisa de
exatamente um para gerenciar transacoes de chunks.

**Solucao:** O `TaskConfig` declara um `@Primary` `DataSourceTransactionManager`:

```java
@Bean
@Primary
PlatformTransactionManager taskPrimaryTransactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
}
```

Isso garante que o Spring Batch usa este TransactionManager quando existem ambiguidades.

### Por que profile `task` no logback-spring.xml?

**Problema:** Containers efemeros nao precisam (e nao devem) escrever logs em arquivo
nem enviar para ELK/Loki. Eles existem por segundos/minutos e os volumes de log
nao estao montados.

**Solucao:** O `logback-spring.xml` tem uma secao especifica para o profile `task`
que usa apenas console output em texto simples (sem JSON, sem file appenders):

```xml
<springProfile name="task">
    <root level="INFO">
        <appender-ref ref="PLAIN_CONSOLE" />
    </root>
</springProfile>
```

Isso mantem os logs legiveis no Dashboard do SCDF e no `docker logs`, sem overhead
de formatacao.

### Por que `close-context-enabled: true`?

**Problema:** Por padrao, uma aplicacao Spring Boot fica rodando indefinidamente apos
o job terminar (aguardando requests HTTP, threads do scheduler, etc.). Isso significa
que containers de task nunca encerrariam por conta propria.

**Solucao:** A propriedade `spring.cloud.task.close-context-enabled=true` no profile
`task` faz o Spring Cloud Task encerrar o `ApplicationContext` automaticamente apos
o job concluir, causando o shutdown do container:

```yaml
spring:
  cloud:
    task:
      close-context-enabled: true
```

Sem isso, os containers ficariam pendurados indefinidamente consumindo recursos.

---

## 9. Troubleshooting

### SCDF nao sobe (restart loop)

**Sintoma:** O container `cinelog-dataflow` fica reiniciando. `docker ps` mostra
status `Restarting`.

**Diagnostico:**

```bash
docker logs cinelog-dataflow --tail 100
```

**Causa mais comum:** Flyway migration falhou (schema corrompido ou incompativel).

**Solucao:**

```bash
# 1. Pare o SCDF
docker stop cinelog-dataflow cinelog-skipper

# 2. Recrie o schema scdf do zero
docker exec cinelog-mysql mysql -uroot -proot -e \
  "DROP SCHEMA IF EXISTS scdf; \
   CREATE SCHEMA scdf DEFAULT CHARACTER SET utf8mb4; \
   GRANT ALL PRIVILEGES ON scdf.* TO 'cinelog'@'%'; \
   FLUSH PRIVILEGES;"

# 3. Reinicie SCDF e Skipper
docker start cinelog-skipper
docker start cinelog-dataflow

# 4. Aguarde ~90 segundos e re-registre tasks
bash docker/scdf/init-scdf.sh
```

### Container de task nao e criado

**Sintoma:** Voce clica "Launch" no Dashboard, a execucao aparece mas fica em
`RUNNING` e nenhum container novo aparece em `docker ps`.

**Diagnostico:**

```bash
# Verificar logs do Skipper (ele e quem cria os containers)
docker logs cinelog-skipper --tail 100

# Verificar se o Docker socket esta acessivel
docker exec cinelog-skipper ls -la /var/run/docker.sock

# Verificar se o Docker CLI esta disponivel
docker exec cinelog-skipper docker version
```

**Causas comuns e solucoes:**

| Causa                                      | Solucao                                             |
|--------------------------------------------|-----------------------------------------------------|
| Docker socket sem permissao                | `chmod 666 /var/run/docker.sock` no host            |
| Docker CLI nao esta montado                | Verificar volume `./docker/scdf/docker-cli:/usr/local/bin/docker:ro` no docker-compose.yml |
| Imagem `cinelog/cinelog-app:latest` nao existe | `docker build -t cinelog/cinelog-app:latest .`    |

### ClassNotFoundException na inicializacao do container

**Sintoma:** O container de task e criado mas morre imediatamente com
`ClassNotFoundException`.

**Diagnostico:**

```bash
# Ver logs do container que falhou
docker ps -a --filter "ancestor=cinelog/cinelog-app:latest" --format '{{.Names}} {{.Status}}' | head -5
docker logs <nome-do-container>
```

**Causas comuns:**

| Classe nao encontrada                          | Causa                                                | Solucao                                       |
|------------------------------------------------|------------------------------------------------------|-----------------------------------------------|
| `RxJava3FallbackDecorator`                     | Resilience4j 2.3.0 incompativel                     | Fixar em 2.2.0 no pom.xml (ja feito)         |
| `com.mysql.cj.jdbc.Driver`                     | Container efemero tentando usar MySQL driver          | Usar `org.mariadb.jdbc.Driver` ou manter padrao |
| `DefaultTaskConfigurer`                        | spring-cloud-task nao esta no classpath               | Verificar dependencia no pom.xml              |

### Container de task nao consegue conectar ao MySQL/Redis

**Sintoma:** Log do container mostra `Communications link failure` ou
`Connection refused`.

**Causa:** O container foi criado em uma rede Docker diferente da rede do MySQL/Redis.

**Diagnostico:**

```bash
# Verificar em qual rede o container de task esta
docker inspect <container-name> --format '{{json .NetworkSettings.Networks}}' | python3 -m json.tool

# Verificar rede do MySQL
docker inspect cinelog-mysql --format '{{json .NetworkSettings.Networks}}' | python3 -m json.tool
```

**Solucao:** Garantir que a propriedade de deploy inclui a rede correta:

```
deployer.<taskName>.local.docker.network=cinelog_default
```

Se o nome da rede for diferente (depende do diretorio do projeto), descubra com:

```bash
docker network ls | grep cinelog
```

### Task fica em RUNNING indefinidamente

**Sintoma:** A execucao aparece como `RUNNING` no Dashboard mas o container ja morreu.

**Diagnostico:**

```bash
# Verificar se o container existe
docker ps --filter "ancestor=cinelog/cinelog-app:latest"

# Se nao existe, o SCDF perdeu tracking
```

**Solucao:**

```bash
# Limpar a execucao orfanada via API
curl -X DELETE "http://localhost:9393/tasks/executions/<ID>?action=CLEANUP"
```

### TMDB API retorna 401 Unauthorized

**Sintoma:** Log do container mostra `WebClientResponseException: 401 Unauthorized`.

**Causa:** A variavel de ambiente `TMDB_API_KEY` nao esta configurada ou expirou.

**Solucao:** Verifique que a API key esta definida. Ela pode ser injetada via
`deployer-env.properties` ou como argumento no lancamento:

```bash
# Via argumento
curl -X POST "http://localhost:9393/tasks/executions" \
  -d "name=importMoviesJob" \
  -d "arguments=--tmdb.api-key=SUA_CHAVE_AQUI"
```

### Erro "Job instance already exists" / JobInstanceAlreadyCompleteException

**Sintoma:** O job nao inicia e o log mostra que ja existe uma instancia completa
com os mesmos parametros.

**Causa:** Spring Batch identifica jobs por nome + parametros. Se voce ja executou
o mesmo job com os mesmos parametros e ele completou com sucesso, ele nao roda
novamente (por design — para garantir idempotencia).

**Solucao:** Adicione um parametro unico para forcar nova execucao:

```bash
curl -X POST "http://localhost:9393/tasks/executions" \
  -d "name=importMoviesJob" \
  -d "arguments=--run.id=$(date +%s)"
```

### Reconstruir imagem apos mudanca no codigo

```bash
# Rebuild da imagem
docker build -t cinelog/cinelog-app:latest .

# Nao precisa re-registrar tasks — SCDF usa a tag :latest
# O proximo lancamento ja usara a nova imagem automaticamente
```

---

## 10. Referencia Rapida

### URLs dos Servicos

| Servico           | URL                                    | Credenciais         |
|-------------------|----------------------------------------|---------------------|
| SCDF Dashboard    | http://localhost:9393/dashboard         | admin / Admin@CineLog2025! |
| SCDF REST API     | http://localhost:9393                   | Mesmas do Dashboard |
| Skipper API       | http://localhost:7577/api               | Sem auth (interno)  |
| MySQL             | localhost:3306                          | cinelog / cinelog    |
| Redis             | localhost:6379                          | Sem auth            |
| Grafana           | http://localhost:3000                   | admin / admin       |
| Prometheus        | http://localhost:9090                   | Sem auth            |
| Jaeger (traces)   | http://localhost:16686                  | Sem auth            |
| Kibana (logs)     | http://localhost:5601                   | Sem auth            |
| MailHog (email)   | http://localhost:8025                   | Sem auth            |
| Keycloak          | http://localhost:8180/admin             | admin / admin       |

### Schemas MySQL

| Schema   | Conteudo                                                     |
|----------|--------------------------------------------------------------|
| `cinelog` | Dados da app (midias, usuarios, reviews), tabelas Spring Batch (`BATCH_*`), tabelas Spring Cloud Task (`SCDF_TASK_*`) |
| `scdf`   | Metadados internos do SCDF Server e Skipper (task definitions, audit records, Flyway migrations) |

### Comandos Essenciais

```bash
# ── Infraestrutura ──
docker compose up -d                              # Sobe tudo
docker compose down                               # Para tudo
docker compose ps                                 # Status dos containers

# ── Build e Registro ──
docker build -t cinelog/cinelog-app:latest .       # Build da imagem
bash docker/scdf/init-scdf.sh                     # Registra tasks e schedules

# ── Lancamento de Tasks ──
curl -X POST "http://localhost:9393/tasks/executions" -d "name=syncGenresJob"
curl -X POST "http://localhost:9393/tasks/executions" -d "name=importMoviesJob" \
  -d "arguments=--cinelog.batch.max-pages=20"

# ── Monitoramento ──
curl -s "http://localhost:9393/tasks/executions?size=5&sort=TASK_EXECUTION_ID,desc" \
  | python3 -m json.tool

# ── Logs ──
docker ps -a --filter "ancestor=cinelog/cinelog-app:latest" \
  --format 'table {{.Names}}\t{{.Status}}\t{{.CreatedAt}}'
docker logs <container-name>

# ── Troubleshooting ──
docker logs cinelog-dataflow --tail 50            # Logs do SCDF Server
docker logs cinelog-skipper --tail 50             # Logs do Skipper
docker exec cinelog-mysql mysql -ucinelog -pcinelog -e \
  "SELECT TASK_NAME, EXIT_CODE, START_TIME FROM cinelog.BOOT3_TASK_EXECUTION ORDER BY TASK_EXECUTION_ID DESC LIMIT 5;"

# ── Limpeza ──
docker ps -a --filter "ancestor=cinelog/cinelog-app:latest" -q | xargs docker rm  # Remove containers de task parados
```

### Arquivos Relevantes do Projeto

```
docker-compose.yml                              # Definicao dos servicos SCDF, Skipper, MySQL, Redis
docker/scdf/entrypoint.sh                       # Correcoes SCDF 2.11.x (driver, prefixos, mail, run.id)
docker/scdf/init-scdf.sh                        # Script de registro de tasks e schedules
docker/scdf/deployer-env.properties             # Variaveis de ambiente injetadas nos containers de task
docker/scdf/docker-cli                          # Binario estatico do Docker CLI (montado como docker-real)
docker/scdf/docker-wrapper.sh                   # Wrapper: injeta --rm e corrige --network
src/main/resources/application-task.yml         # Profile de container efemero
src/main/resources/logback-spring.xml           # Logging (secao profile task)
src/main/java/.../batch/config/TaskConfig.java  # Spring Cloud Task manual + @Primary TxManager
src/main/java/.../batch/config/BatchJobsConfig.java  # Definicao dos 12 batch jobs
src/main/java/.../batch/jobs/reports/*.java     # Tasklets dos 3 jobs de email
src/main/java/.../reports/email/ReportEmailService.java  # Orquestrador de emails (blocking + async)
```

### Documentacao Relacionada

| Documento | Conteudo |
|---|---|
| **[SCDF-IMPLEMENTATION.md](./SCDF-IMPLEMENTATION.md)** | Detalhes tecnicos da implementacao SCDF |
| **[SCDF-DASHBOARD-GUIDE.md](./SCDF-DASHBOARD-GUIDE.md)** | Guia passo a passo do Dashboard |
| **[BATCH-PERFORMANCE.md](./BATCH-PERFORMANCE.md)** | Metricas, SLOs e tuning dos batch jobs |
| **[SLI-DEFINITIONS.md](./SLI-DEFINITIONS.md)** | SLIs, SLOs e regras de alerta |
| **[ADR-014](./adr/ADR-014-scdf-batch-orchestration.md)** | Decisao arquitetural do SCDF |
```
