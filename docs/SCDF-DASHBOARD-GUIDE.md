# SCDF Dashboard — Guia Completo de Uso

**Versao:** 1.0
**Data:** 2026-03-22
**Escopo:** Passo a passo detalhado para executar, monitorar e agendar batch jobs via Dashboard

---

## Sumario

1. [Pre-requisitos](#1-pre-requisitos)
2. [Acessando o Dashboard](#2-acessando-o-dashboard)
3. [Navegacao do Dashboard](#3-navegacao-do-dashboard)
4. [Executando um Batch Job](#4-executando-um-batch-job)
5. [Executando Jobs de Email](#5-executando-jobs-de-email)
6. [Monitorando Execucoes](#6-monitorando-execucoes)
7. [Visualizando Logs](#7-visualizando-logs)
8. [Detalhes do Spring Batch Job](#8-detalhes-do-spring-batch-job)
9. [Gerenciando Schedules](#9-gerenciando-schedules)
10. [Verificando Emails no MailHog](#10-verificando-emails-no-mailhog)
11. [Executando via REST API](#11-executando-via-rest-api)
12. [Troubleshooting](#12-troubleshooting)
13. [Referencia Rapida](#13-referencia-rapida)

---

## 1. Pre-requisitos

Antes de usar o Dashboard, certifique-se de que:

### 1.1 Infraestrutura Docker esta rodando

```bash
# Subir todos os servicos
docker compose up -d

# Verificar que SCDF e Skipper estao saudaveis
docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E "dataflow|skipper"

# Saida esperada (apos ~90 segundos):
# cinelog-dataflow   Up X minutes (healthy)
# cinelog-skipper    Up X minutes (healthy)
```

### 1.2 Imagem Docker da aplicacao existe

```bash
# Verificar se a imagem existe
docker images cinelog/cinelog-app --format 'table {{.Repository}}\t{{.Tag}}\t{{.CreatedAt}}'

# Se nao existir, construir:
docker build --network=host -t cinelog/cinelog-app:latest .
```

### 1.3 Tasks estao registradas no SCDF

```bash
# Registrar tasks e schedules (idempotente — pode executar varias vezes)
bash docker/scdf/init-scdf.sh
```

---

## 2. Acessando o Dashboard

### URL

```
http://localhost:9393/dashboard
```

### Autenticacao

Em ambiente de desenvolvimento, o Dashboard usa **Basic Auth**:

| Campo    | Valor                  |
|----------|------------------------|
| Usuario  | `admin`                |
| Senha    | `Admin@CineLog2025!`   |

O navegador vai pedir essas credenciais na primeira vez.

### Tela Inicial

Ao acessar, voce vera:

- **Menu lateral esquerdo** com opcoes de navegacao
- **Painel central** com informacoes do SCDF Server (versao, features habilitadas)
- **Barra superior** com links rapidos

---

## 3. Navegacao do Dashboard

### Menu Lateral

| Item do Menu       | O que faz | Quando usar |
|--------------------|-----------|-------------|
| **Apps**           | Lista aplicacoes Docker registradas | Para verificar se `cinelog` esta registrada |
| **Tasks/Jobs**     | Gerencia tasks, lancamentos e execucoes | **Item mais usado** — tudo sobre batch jobs |
| **Schedules**      | Gerencia agendamentos CRON | Para ver/criar/deletar schedules |
| **Audit Records**  | Historico de todas as acoes no SCDF | Para auditoria e debug |

### Submenu Tasks/Jobs

Ao clicar em **Tasks/Jobs**, o submenu se expande:

| Subitem          | O que mostra |
|------------------|--------------|
| **Definitions**  | Lista de todas as tasks registradas com botoes de acao |
| **Executions**   | Historico de todas as execucoes (COMPLETE, ERROR, RUNNING) |
| **Job Executions** | Detalhes do Spring Batch: steps, itens lidos/escritos |

---

## 4. Executando um Batch Job

### Passo a Passo: Executando `syncGenresJob`

O `syncGenresJob` e ideal para testar — e rapido (< 30s) e nao depende de outros jobs.

**Passo 1:** No menu lateral, clique em **Tasks/Jobs**.

**Passo 2:** Clique em **Definitions** (se nao estiver ja selecionado).

**Passo 3:** Na tabela, localize `syncGenresJob`. Use a barra de busca se necessario.

**Passo 4:** Na linha do `syncGenresJob`, clique no botao **Launch** (icone de play ▶).

**Passo 5:** A tela "Launch Task" abre com dois campos:

```
┌─────────────────────────────────────────────────────────┐
│                   Launch Task                           │
│                                                         │
│  Task Name: syncGenresJob                               │
│                                                         │
│  ┌─── Arguments ───────────────────────────────────┐    │
│  │ (deixe vazio para configuracao padrao)           │    │
│  │                                                  │    │
│  │ Exemplos de argumentos opcionais:                │    │
│  │ --cinelog.batch.max-pages=20                     │    │
│  └──────────────────────────────────────────────────┘    │
│                                                         │
│  ┌─── Properties (Deploy) ─────────────────────────┐    │
│  │ Adicione OBRIGATORIAMENTE:                       │    │
│  │                                                  │    │
│  │ app.cinelog.spring.cloud.deployer.bootVersion=3  │    │
│  │ deployer.*.bootVersion=3                         │    │
│  │                                                  │    │
│  └──────────────────────────────────────────────────┘    │
│                                                         │
│                          [Launch the task]               │
└─────────────────────────────────────────────────────────┘
```

> **IMPORTANTE:** Adicione as propriedades `bootVersion=3` no campo Properties.
> Sem elas, o SCDF pode tentar usar o schema Boot 2 e o lancamento falhara.

**Passo 6:** Clique no botao **Launch the task**.

**Passo 7:** Voce e redirecionado para a tela de **Executions**, onde pode
acompanhar o progresso.

### O que acontece nos bastidores

```
1. SCDF Server registra a execucao (BOOT3_TASK_EXECUTION)
2. SCDF envia instrucao ao Skipper
3. Skipper chama docker-wrapper.sh → docker-real run --rm ...
4. Container inicia com profile=task
5. entrypoint.sh corrige configs e adiciona run.id unico
6. Spring Boot sobe e executa syncGenresJob
7. Job conecta ao TMDB e sincroniza generos
8. Job grava resultados no MySQL
9. close-context encerra o Spring
10. Container para com exit code 0 (sucesso) e e removido automaticamente
```

---

## 5. Executando Jobs de Email

Os 3 jobs de email enviam relatorios por e-mail para usuarios cadastrados.
Em ambiente de desenvolvimento, os emails chegam no **MailHog**.

### 5.1 sendWeeklyDigestJob — Digest Semanal

Envia um resumo semanal personalizado para **todos os usuarios cadastrados**.

**Passo 1:** No menu lateral, clique em **Tasks/Jobs** → **Definitions**.

**Passo 2:** Localize `sendWeeklyDigestJob` e clique em **Launch** (▶).

**Passo 3:** No campo **Properties**, adicione:

```
app.cinelog.spring.cloud.deployer.bootVersion=3
deployer.*.bootVersion=3
```

**Passo 4:** Clique em **Launch the task**.

**Passo 5:** Aguarde a execucao completar (15-90 segundos dependendo do numero de usuarios).

**Passo 6:** Verifique os emails no MailHog: http://localhost:8025

Voce vera emails com assunto: `🎬 Seu resumo semanal no CineLog`

### 5.2 sendTrendingReportJob — Relatorio Trending

Envia um relatorio de midias em alta para **todos os usuarios**.

**Passo 1-4:** Mesmo processo acima, mas selecionando `sendTrendingReportJob`.

**Verificacao:** Emails com assunto: `🔥 Em alta esta semana no CineLog`

### 5.3 sendPlatformReportJob — Relatorio da Plataforma

Envia um relatorio de metricas da plataforma para o **email do administrador**.

**Destinatario:** Por padrao, usa o email configurado em `cinelog.reports.from-email`
(`noreply@cinelog.dev`). Para usar outro email, defina a variavel de ambiente
`REPORT_ADMIN_EMAIL` no container.

**Passo 1-4:** Mesmo processo acima, selecionando `sendPlatformReportJob`.

**Verificacao:** Email com assunto: `📊 Relatório da plataforma CineLog`

### Tabela de Jobs de Email

| Job | Destinatarios | Assunto | Duracao Tipica |
|---|---|---|---|
| `sendWeeklyDigestJob` | Todos os usuarios | 🎬 Seu resumo semanal no CineLog | 10-60s |
| `sendTrendingReportJob` | Todos os usuarios | 🔥 Em alta esta semana no CineLog | 10-60s |
| `sendPlatformReportJob` | Admin (1 email) | 📊 Relatório da plataforma CineLog | 5-15s |

---

## 6. Monitorando Execucoes

### 6.1 Lista de Execucoes

**Navegacao:** Tasks/Jobs → **Executions**

A tabela mostra todas as execucoes ordenadas pela mais recente:

| Coluna | Descricao |
|---|---|
| **Execution ID** | Numero unico sequencial (clicavel para detalhes) |
| **Task Name** | Nome do job executado |
| **Start Time** | Data/hora de inicio |
| **End Time** | Data/hora de termino (vazio se RUNNING) |
| **Exit Code** | `0` = sucesso, `1` = erro |
| **Exit Message** | Vazio se sucesso; stack trace se erro |

### 6.2 Status Possiveis

| Status | Cor | Significado | Acao Recomendada |
|---|---|---|---|
| `RUNNING` | Azul | Container em execucao | Aguardar ou verificar `docker ps` |
| `COMPLETE` | Verde | Job terminou com sucesso | Nenhuma |
| `ERROR` | Vermelho | Job falhou (exit code != 0) | Clicar para ver erro |

### 6.3 Detalhes de uma Execucao

Clique no **Execution ID** (numero azul) para ver:

```
┌─────────────────────────────────────────────────────────┐
│              Task Execution Details                      │
│                                                         │
│  Execution ID:      47                                  │
│  Task Name:         sendWeeklyDigestJob                 │
│  Start Time:        2026-03-23 02:32:01                 │
│  End Time:          2026-03-23 02:32:34                  │
│  Exit Code:         0                                   │
│  Exit Message:      (vazio = sucesso)                   │
│                                                         │
│  Arguments:                                             │
│    --spring.cloud.task.executionid=47                    │
│    --run.id=1774233001234                                │
│                                                         │
│  External Execution ID:                                 │
│    sendWeeklyDigestJob-abc123-def456                     │
│                                                         │
│  Resource URL:                                          │
│    Docker Resource [docker:cinelog/cinelog-app:latest]   │
│                                                         │
│  Job Execution(s):                                      │
│    [Link para Job Execution #31]                        │
│                                                         │
│  ┌─── Log ──────────────────────────────────────────┐   │
│  │ (botao ou aba para ver logs do container)        │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 6.4 Filtros

Na tela de Executions, voce pode filtrar por:

- **Task Name:** Selecione um job especifico no dropdown
- **Status:** Filtre por COMPLETE, ERROR ou RUNNING
- **Date range:** Defina periodo de busca

---

## 7. Visualizando Logs

### 7.1 Via Dashboard

1. Na tela de detalhes da execucao (apos clicar no Execution ID)
2. Procure o botao ou aba **Log**
3. Os logs do container sao exibidos no navegador

**Exemplo de log de SUCESSO (email job):**

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

INFO  --- Started CinelogApplication in 4.2 seconds
INFO  --- The following 1 profile is active: "task"
INFO  --- HikariPool-1 - Start completed.
INFO  --- [BATCH] WeeklyDigestTasklet: enviando digest semanal para todos os usuarios
INFO  --- [BATCH-SYNC] Sending weekly digest to 10 users
INFO  --- [BATCH] WeeklyDigestTasklet: concluido
INFO  --- Job: [sendWeeklyDigestJob] completed with status COMPLETED
INFO  --- Spring Cloud Task closed context.
INFO  --- Graceful shutdown complete
```

**Exemplo de log de ERRO:**

```
ERROR --- Application run failed
java.lang.IllegalStateException: Failed to execute ApplicationRunner
Caused by: org.springframework.batch.core.repository.JobExecutionAlreadyRunningException:
  A job execution for this job is already running...
```

### 7.2 Via Terminal

Se o Dashboard nao mostrar logs (container ja foi removido por `--rm`):

```bash
# Como o container e removido automaticamente, os logs so estao disponiveis
# enquanto o container esta rodando. Use docker logs em tempo real:
docker ps --filter "ancestor=cinelog/cinelog-app:latest" --format '{{.Names}}'
docker logs -f <nome-do-container>
```

> **Nota:** Com a flag `--rm` (injetada pelo docker-wrapper.sh), os logs sao
> perdidos apos o container encerrar. O SCDF Dashboard geralmente captura os
> logs antes do container ser removido, mas nem sempre.

### 7.3 Via Banco de Dados

As informacoes de sucesso/falha ficam persistidas permanentemente:

```sql
-- Ver ultimas execucoes com status e mensagem de erro
SELECT
    te.TASK_EXECUTION_ID AS id,
    te.TASK_NAME AS task,
    te.EXIT_CODE AS exit_code,
    te.START_TIME AS inicio,
    te.END_TIME AS fim,
    SUBSTRING(te.EXIT_MESSAGE, 1, 200) AS erro
FROM cinelog.BOOT3_TASK_EXECUTION te
ORDER BY te.TASK_EXECUTION_ID DESC
LIMIT 10;
```

---

## 8. Detalhes do Spring Batch Job

### 8.1 Acessando Job Executions

**Navegacao:** Tasks/Jobs → **Job Executions**

Ou clique no link **Job Execution** dentro dos detalhes de uma Task Execution.

### 8.2 Informacoes Disponiveis

| Campo | Descricao |
|---|---|
| **Job Name** | Nome do batch job (ex: `sendWeeklyDigestJob`) |
| **Job Instance ID** | Identificador da instancia (unico por nome+params) |
| **Job Execution ID** | Identificador da execucao |
| **Status** | `COMPLETED`, `FAILED`, `ABANDONED` |
| **Start Time** | Inicio da execucao do job |
| **End Time** | Termino da execucao do job |
| **Job Parameters** | Parametros usados (ex: `run.id=1774233001234`) |

### 8.3 Step Execution Details

Ao clicar em um Job Execution, voce ve os steps:

```
┌──────────────────────────────────────────────────────────────┐
│  Step Executions                                             │
│                                                              │
│  Step Name: sendWeeklyDigestStep                             │
│  Status: COMPLETED                                           │
│                                                              │
│  ┌─── Contadores ────────────────────────────────────┐       │
│  │ Read Count:      10  (itens lidos)                │       │
│  │ Write Count:     10  (itens escritos/enviados)    │       │
│  │ Skip Count:       0  (itens pulados por erro)     │       │
│  │ Commit Count:     1  (transacoes commitadas)      │       │
│  │ Rollback Count:   0  (transacoes revertidas)      │       │
│  └───────────────────────────────────────────────────┘       │
│                                                              │
│  Duracao: 33s                                                │
└──────────────────────────────────────────────────────────────┘
```

Para jobs chunk-oriented (TMDB), os contadores mostram quantos itens foram
importados/atualizados:

```
Step: importMoviesStep
  Read Count:   200  (filmes lidos do TMDB)
  Write Count:  198  (filmes salvos no MySQL)
  Skip Count:     2  (filmes com dados invalidos)
```

---

## 9. Gerenciando Schedules

### 9.1 Vendo Schedules Ativos

**Navegacao:** **Schedules** (menu lateral)

Os 8 schedules pre-configurados:

| Schedule | Task | Cron | Quando Executa |
|---|---|---|---|
| `sched-sync-genres` | syncGenresJob | `0 0 3 * * 0` | Domingo 03:00 |
| `sched-import-movies` | importMoviesJob | `0 30 3 * * 0` | Domingo 03:30 |
| `sched-import-tvshows` | importTvShowsJob | `0 0 4 * * 0` | Domingo 04:00 |
| `sched-import-credits` | importCreditsJob | `0 30 4 * * 0` | Domingo 04:30 |
| `sched-import-seasons` | importSeasonsJob | `0 0 5 * * 0` | Domingo 05:00 |
| `sched-sync-reviews` | syncReviewsJob | `0 30 5 * * 0` | Domingo 05:30 |
| `sched-enrich-images` | enrichMediaImagesJob | `0 0 6 * * 0` | Domingo 06:00 |
| `sched-enrich-profiles` | enrichPersonProfilesJob | `0 30 6 * * 0` | Domingo 06:30 |

> **Nota:** Os 3 jobs de email (`sendWeeklyDigestJob`, `sendTrendingReportJob`,
> `sendPlatformReportJob`) e o `linkTmdbJob` NAO possuem schedule — sao executados
> manualmente conforme necessidade.

### 9.2 Limitacao: Scheduling no Local Deployer

> **O botao "Create Schedule" no Dashboard NAO funciona com o Local Deployer (Docker).**
>
> Ao tentar criar um schedule, voce recebera o erro:
> `"Scheduling is not implemented for local platform."`
>
> Isso e uma limitacao do SCDF 2.11.x — scheduling nativo so funciona com
> Kubernetes (CronJob) ou Cloud Foundry (PCF Scheduler).

### 9.3 Solucao: Crontab do Host

Use o **crontab do Linux** com o script `scripts/scdf-schedule.sh` para agendar
execucoes. O script chama a REST API do SCDF com as propriedades corretas:

```bash
# Teste manual:
bash scripts/scdf-schedule.sh sendWeeklyDigestJob

# Abrir crontab:
crontab -e

# Exemplo: digest semanal toda segunda 08:00
0 8 * * 1 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh sendWeeklyDigestJob >> /tmp/scdf-cron.log 2>&1

# Exemplo: import de filmes todo domingo 03:30
30 3 * * 0 /home/maps/Projetos/cinelog/cinelog/scripts/scdf-schedule.sh importMoviesJob >> /tmp/scdf-cron.log 2>&1
```

Veja `scripts/scdf-schedule.sh` e a secao 4.6 do [SCDF-GUIDE.md](./SCDF-GUIDE.md)
para a lista completa de agendamentos recomendados.

### 9.4 Expressoes Cron (crontab Linux)

| Expressao | Significado |
|---|---|
| `0 8 * * 1` | Segunda-feira 08:00 |
| `0 18 * * 5` | Sexta-feira 18:00 |
| `0 3 * * 0` | Domingo 03:00 |
| `*/5 * * * *` | A cada 5 minutos |
| `0 0 1 * *` | Primeiro dia do mes 00:00 |

> **Nota:** Expressoes cron do Linux usam 5 campos (`M H DoM Mon DoW`),
> diferente do Spring/SCDF que usa 6 campos (com segundos).

---

## 10. Verificando Emails no MailHog

Apos executar um job de email, verifique a entrega:

### 10.1 Interface Web

Acesse: **http://localhost:8025**

O MailHog exibe todos os emails recebidos com:

- **De:** `noreply@cinelog.dev` (configuravel)
- **Para:** Email de cada usuario cadastrado
- **Assunto:** Varia por tipo de relatorio
- **Corpo:** HTML renderizado com template Thymeleaf

### 10.2 Verificando via API do MailHog

```bash
# Contar emails e ver assuntos recentes
curl -s http://localhost:8025/api/v2/messages?limit=10 | python3 -c "
import sys, json
msgs = json.load(sys.stdin)['items']
print(f'Total de emails: {len(msgs)}')
for m in msgs[:10]:
    to = m['Content']['Headers'].get('To', ['?'])[0]
    subj = m['Content']['Headers'].get('Subject', ['?'])[0]
    print(f'  Para: {to}')
    print(f'  Assunto: {subj}')
    print()
"
```

### 10.3 Limpando a Caixa do MailHog

```bash
# Deletar todos os emails (util para testes limpos)
curl -X DELETE http://localhost:8025/api/v1/messages
```

### 10.4 Tipos de Email por Job

| Job | Template | Conteudo |
|---|---|---|
| `sendWeeklyDigestJob` | `weekly-digest` | Midias assistidas na semana, estatisticas pessoais |
| `sendTrendingReportJob` | `trending` | Midias populares na plataforma |
| `sendPlatformReportJob` | `platform-report` | Metricas gerais: total usuarios, midias, reviews |

---

## 11. Executando via REST API

Todos os comandos abaixo sao equivalentes a usar o Dashboard.

### 11.1 Lancar um Job

```bash
# Lancar syncGenresJob
curl -s -X POST "http://localhost:9393/tasks/executions/launch" \
  -d "name=syncGenresJob" \
  -d "properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3" \
  -u admin:'Admin@CineLog2025!'

# Resposta:
# {"executionId":50,"schemaTarget":"boot3",...}
```

### 11.2 Lancar Job de Email

```bash
# Lancar sendWeeklyDigestJob
curl -s -X POST "http://localhost:9393/tasks/executions/launch" \
  -d "name=sendWeeklyDigestJob" \
  -d "properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3" \
  -u admin:'Admin@CineLog2025!'
```

### 11.3 Lancar com Argumentos Customizados

```bash
# importMoviesJob com mais paginas
curl -s -X POST "http://localhost:9393/tasks/executions/launch" \
  -d "name=importMoviesJob" \
  -d "properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3" \
  -d "arguments=--cinelog.batch.max-pages=50" \
  -u admin:'Admin@CineLog2025!'
```

### 11.4 Verificar Status de uma Execucao

```bash
# Substituir 50 pelo ID da execucao
curl -s "http://localhost:9393/tasks/executions/50?schemaTarget=boot3" \
  -u admin:'Admin@CineLog2025!' | python3 -m json.tool
```

### 11.5 Listar Ultimas Execucoes

```bash
curl -s "http://localhost:9393/tasks/executions?size=10&sort=TASK_EXECUTION_ID,desc&schemaTarget=boot3" \
  -u admin:'Admin@CineLog2025!' | python3 -c "
import sys, json
data = json.load(sys.stdin)
for e in data.get('_embedded', {}).get('taskExecutionResourceList', []):
    print(f'  ID:{e[\"executionId\"]}  Task:{e[\"taskName\"]}  Exit:{e[\"exitCode\"]}  Start:{e[\"startTime\"]}')
"
```

### 11.6 Listar Tasks Registradas

```bash
curl -s "http://localhost:9393/tasks/definitions" \
  -u admin:'Admin@CineLog2025!' | python3 -c "
import sys, json
data = json.load(sys.stdin)
for t in data.get('_embedded', {}).get('taskDefinitionResourceList', []):
    print(f'  {t[\"name\"]}: {t[\"dslText\"]}')
"
```

---

## 12. Troubleshooting

### 12.1 Erro "Task cannot be launched for boot2"

**Sintoma:** Ao lancar uma task, a resposta diz "cannot be launched for boot2".

**Causa:** Faltam as propriedades `bootVersion=3`.

**Solucao:** Adicione no campo Properties do Dashboard:

```
app.cinelog.spring.cloud.deployer.bootVersion=3
deployer.*.bootVersion=3
```

Ou via API, adicione no parametro `properties`.

### 12.2 Erro "JobExecutionAlreadyRunningException"

**Sintoma:** O job falha com "A job execution for this job is already running".

**Causa:** Uma execucao anterior ficou em estado inconsistente no banco de dados
(geralmente FAILED ou ABANDONED com metadados corrompidos).

**Solucao:**

```sql
-- 1. Identificar a execucao problematica
SELECT JOB_EXECUTION_ID, STATUS, VERSION, END_TIME
FROM cinelog.BOOT3_BATCH_JOB_EXECUTION
WHERE JOB_INSTANCE_ID IN (
    SELECT JOB_INSTANCE_ID FROM cinelog.BOOT3_BATCH_JOB_INSTANCE
    WHERE JOB_NAME = 'sendWeeklyDigestJob'
)
ORDER BY JOB_EXECUTION_ID DESC;

-- 2. Limpar a instancia problematica (substitua <INSTANCE_ID>)
DELETE FROM BOOT3_BATCH_STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID IN (
    SELECT STEP_EXECUTION_ID FROM BOOT3_BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (
        SELECT JOB_EXECUTION_ID FROM BOOT3_BATCH_JOB_EXECUTION WHERE JOB_INSTANCE_ID = <INSTANCE_ID>
    )
);
DELETE FROM BOOT3_BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID FROM BOOT3_BATCH_JOB_EXECUTION WHERE JOB_INSTANCE_ID = <INSTANCE_ID>
);
DELETE FROM BOOT3_BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID FROM BOOT3_BATCH_JOB_EXECUTION WHERE JOB_INSTANCE_ID = <INSTANCE_ID>
);
DELETE FROM BOOT3_BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID IN (
    SELECT JOB_EXECUTION_ID FROM BOOT3_BATCH_JOB_EXECUTION WHERE JOB_INSTANCE_ID = <INSTANCE_ID>
);
DELETE FROM BOOT3_BATCH_JOB_EXECUTION WHERE JOB_INSTANCE_ID = <INSTANCE_ID>;
DELETE FROM BOOT3_BATCH_JOB_INSTANCE WHERE JOB_INSTANCE_ID = <INSTANCE_ID>;
```

### 12.3 Container nao aparece em `docker ps`

**Sintoma:** Voce lancou a task mas nenhum container novo aparece.

**Diagnostico:**

```bash
# Verificar logs do Skipper
docker logs cinelog-skipper --tail 50

# Verificar se a imagem existe
docker images cinelog/cinelog-app --format '{{.Repository}}:{{.Tag}}'

# Verificar se o Docker socket esta acessivel
docker exec cinelog-skipper /usr/local/bin/docker-real version
```

**Causas comuns:**
- Imagem `cinelog/cinelog-app:latest` nao existe → `docker build -t cinelog/cinelog-app:latest .`
- Docker socket sem permissao → `chmod 666 /var/run/docker.sock`
- Skipper sem docker-real → verificar volumes no docker-compose.yml

### 12.4 Emails nao chegam no MailHog

**Sintoma:** Job de email completa com sucesso mas MailHog esta vazio.

**Diagnostico:**

1. Verificar se MailHog esta rodando: `docker ps | grep mailhog`
2. Verificar se a porta 1025 esta acessivel: `telnet localhost 1025`
3. Verificar config de email no entrypoint: o `spring.mail.host` deve ser `mailhog`

**Causa comum:** Nenhum usuario cadastrado no banco. Os jobs enviam para
usuarios da tabela `users`.

```sql
-- Verificar se ha usuarios cadastrados
SELECT COUNT(*) AS total_users FROM cinelog.users;
```

### 12.5 Job termina com NOOP

**Sintoma:** O job mostra `EXIT_CODE=NOOP` com mensagem "All steps already completed".

**Causa:** O Spring Batch reutilizou uma instancia anterior (mesmos parametros) cujo
step ja havia sido completado.

**Solucao:** Isso nao deveria ocorrer com o fix do `--run.id` no entrypoint. Se
ainda ocorrer, reconstrua a imagem Docker:

```bash
docker build --network=host -t cinelog/cinelog-app:latest .
```

### 12.6 SCDF nao sobe (restart loop)

```bash
# Verificar logs
docker logs cinelog-dataflow --tail 100

# Se for Flyway migration corrompida, recriar schema:
docker exec cinelog-mysql mysql -uroot -proot -e \
  "DROP SCHEMA IF EXISTS scdf; CREATE SCHEMA scdf DEFAULT CHARACTER SET utf8mb4; GRANT ALL ON scdf.* TO 'cinelog'@'%';"

# Reiniciar
docker compose restart skipper-server dataflow-server

# Re-registrar tasks
bash docker/scdf/init-scdf.sh
```

---

## 13. Referencia Rapida

### URLs

| Servico | URL | Credenciais |
|---|---|---|
| **SCDF Dashboard** | http://localhost:9393/dashboard | admin / Admin@CineLog2025! |
| **MailHog** | http://localhost:8025 | Sem auth |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | Sem auth |
| **Jaeger** | http://localhost:16686 | Sem auth |

### Os 12 Batch Jobs

| Job | Tipo | Descricao Curta |
|---|---|---|
| `syncGenresJob` | TMDB | Sincroniza generos |
| `importMoviesJob` | TMDB | Importa filmes |
| `importTvShowsJob` | TMDB | Importa series |
| `linkTmdbJob` | TMDB | Vincula midias seed |
| `importCreditsJob` | TMDB | Importa elenco |
| `importSeasonsJob` | TMDB | Importa temporadas |
| `syncReviewsJob` | TMDB | Sincroniza reviews |
| `enrichMediaImagesJob` | TMDB | Enriquece imagens |
| `enrichPersonProfilesJob` | TMDB | Enriquece perfis |
| `sendWeeklyDigestJob` | Email | Digest semanal |
| `sendTrendingReportJob` | Email | Trending report |
| `sendPlatformReportJob` | Email | Relatorio admin |

### Checklist para Lancamento

```
[ ] Docker Compose rodando (docker compose up -d)
[ ] SCDF e Skipper saudaveis (docker ps → healthy)
[ ] Imagem cinelog/cinelog-app:latest construida
[ ] Tasks registradas (bash docker/scdf/init-scdf.sh)
[ ] Propriedades bootVersion=3 preenchidas no lancamento
```

### Comandos Essenciais (Terminal)

```bash
# Infraestrutura
docker compose up -d                              # Sobe tudo
docker compose down                               # Para tudo

# Build e registro
docker build --network=host -t cinelog/cinelog-app:latest .
bash docker/scdf/init-scdf.sh

# Lancar job via API
curl -s -X POST "http://localhost:9393/tasks/executions/launch" \
  -d "name=sendWeeklyDigestJob" \
  -d "properties=app.cinelog.spring.cloud.deployer.bootVersion=3,deployer.*.bootVersion=3" \
  -u admin:'Admin@CineLog2025!'

# Verificar emails
curl -s http://localhost:8025/api/v2/messages | python3 -m json.tool

# Verificar execucoes
mysql -h 127.0.0.1 -u cinelog -pcinelog cinelog -e \
  "SELECT TASK_NAME, EXIT_CODE, START_TIME FROM BOOT3_TASK_EXECUTION ORDER BY TASK_EXECUTION_ID DESC LIMIT 5;"
```
