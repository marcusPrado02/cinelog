# ADR-014: Spring Cloud Data Flow como Orquestrador de Batch Jobs

## Status

✅ **Aceito**

## Data

2026-03-19

## Contexto

O CineLog possui 9 Spring Batch jobs para importação de dados do TMDB (créditos, imagens, pessoas, reviews, seasons, mídia, etc.). Anteriormente, o agendamento era feito por `@Scheduled` em `BatchSchedulerConfig` combinado com uma API REST em `BatchJobController` para execução manual.

### Problemas identificados

1. **Crons hardcoded** — qualquer alteração de horário exigia redeploy da aplicação
2. **Sem histórico centralizado** — execuções passadas não eram facilmente consultáveis
3. **Sem dashboard visual** — operadores dependiam de logs e endpoints REST para monitorar jobs
4. **Sem pause/resume** — impossível pausar um job em execução sem intervenção manual
5. **Acoplamento com a aplicação principal** — jobs executavam no mesmo processo do servidor web, competindo por recursos

## Decisão

Integrar **Spring Cloud Data Flow (SCDF) 2.11.5** como orquestrador central de batch jobs, usando **Docker Deployer** para lançar containers efêmeros por job.

### Mudanças arquiteturais

- **Remover `BatchSchedulerConfig`** — SCDF assume todo o scheduling via sua interface visual/REST
- **Deprecar `BatchJobController`** — a REST API do SCDF substitui os endpoints de execução manual
- **Profile `task`** — execução em container efêmero, com configuração de logging e ciclo de vida adaptados

### Decisões técnicas detalhadas

#### 1. MariaDB Connector/J no SCDF

A imagem oficial do SCDF não inclui o driver MySQL. Utilizamos o MariaDB Connector/J com `permitMysqlScheme=true` para conectar ao MySQL existente sem trocar a URL de conexão.

#### 2. Docker CLI estático montado como volume

O container do SCDF não possui o binário Docker. O Docker CLI é compilado estaticamente e montado como volume (`/usr/local/bin/docker`) para que o Docker Deployer consiga criar containers de task.

#### 3. `TaskConfigurer` explícito

Com múltiplos `DataSource` beans no contexto (aplicação + Spring Cloud Task), é necessário um `TaskConfigurer` explícito para indicar qual `DataSource` o Spring Cloud Task deve usar para persistir metadados de execução.

#### 4. `@Primary` TransactionManager

O Spring Cloud Task registra um `springCloudTaskTransactionManager` que conflita com o `TransactionManager` padrão da aplicação. A anotação `@Primary` no transaction manager principal resolve a ambiguidade.

#### 5. Resilience4j 2.2.0 (não 2.3.0)

A versão 2.3.0 do Resilience4j é incompatível com Spring Boot 3.5 — causa crash em `FallbackConfigurationOnMissingBean` durante a inicialização do contexto. A versão 2.2.0 é estável e compatível.

#### 6. `resilience4j-rxjava3` como dependência explícita

O autoconfig do `resilience4j-spring-boot3` espera a presença do módulo `resilience4j-rxjava3` no classpath. Sem essa dependência explícita, o contexto falha ao inicializar.

#### 7. Profile `task` no logback-spring.xml

Em container efêmero, file appenders não funcionam (o filesystem é descartado ao término). O profile `task` configura logging exclusivamente para `STDOUT`, garantindo que logs sejam capturados pelo Docker e pelo SCDF.

#### 8. `close-context-enabled: true`

Configuração do Spring Cloud Task para encerrar o contexto Spring automaticamente após a execução do job. Sem isso, o container permanece ativo indefinidamente após o término da task.

#### 9. Rede Docker `cinelog_default`

Os containers de task precisam acessar MySQL e Redis. São lançados na rede Docker `cinelog_default` para comunicação direta com os serviços de infraestrutura via nome de serviço.

#### 10. Spring Cloud BOM 2024.0.0

Versão compatível com Spring Boot 3.5. Garante alinhamento de dependências entre Spring Cloud Task, Spring Cloud Deployer e Spring Cloud Data Flow.

## Alternativas Consideradas

### 1. Manter `@Scheduled` + `BatchJobController`

**Prós:**

- Sem infraestrutura adicional
- Simplicidade operacional

**Contras:**

- Crons hardcoded exigem redeploy
- Sem dashboard visual
- Sem histórico centralizado de execuções
- Jobs competem por recursos com a aplicação web

**Por que não escolhemos:** Limitações operacionais crescentes à medida que o número de jobs aumentava.

### 2. Kubernetes CronJob

**Prós:**

- Nativo do Kubernetes
- Isolamento de recursos
- Scheduling flexível

**Contras:**

- Requer cluster Kubernetes (overhead para ambiente atual baseado em Docker Compose)
- Sem dashboard específico para batch
- Sem correlação automática com Spring Batch metadata

**Por que não escolhemos:** Overhead de infraestrutura desproporcional para o estágio atual do projeto.

### 3. Apache Airflow

**Prós:**

- Dashboard robusto
- DAGs flexíveis
- Ecossistema rico

**Contras:**

- Stack Python — desalinhado com o ecossistema Java/Spring do CineLog
- Sem integração nativa com Spring Batch
- Overhead de configuração e manutenção

**Por que não escolhemos:** Falta de integração nativa com Spring Batch e desalinhamento tecnológico.

## Consequências

### Positivas

- **Dashboard visual** — operadores monitoram jobs, histórico e logs via interface web do SCDF
- **Histórico persistido** — todas as execuções ficam registradas no banco do SCDF com status, duração e parâmetros
- **Containers isolados** — cada job executa em container efêmero, sem competir por recursos com a aplicação principal
- **Sem redeploy para mudar crons** — schedules são configuráveis via SCDF UI/REST sem alterar código
- **Integração nativa com Spring Batch** — SCDF entende metadados do Spring Batch (steps, chunks, status)

### Negativas / Trade-offs

- **Mais infraestrutura** — requer Skipper + SCDF como serviços adicionais no Docker Compose
- **Docker CLI estático como volume** — workaround necessário pela ausência do binário na imagem SCDF
- **Complexidade de debugging** — logs distribuídos entre container de task (efêmero) e SCDF server
- **Dependência de versões específicas** — Resilience4j 2.2.0 e Spring Cloud BOM 2024.0.0 são constraints rígidas

### Riscos

| Risco | Mitigação |
| ----- | --------- |
| SCDF 2.11.5 não é a versão mais recente | Monitorar releases; planejar upgrade quando Spring Boot 3.5 for suportado oficialmente |
| Docker Deployer menos maduro que Kubernetes Deployer | Avaliar migração para Kubernetes Deployer se a infraestrutura evoluir para K8s |
| Containers efêmeros dificultam debugging pós-falha | Logs centralizados via Docker + SCDF; considerar integração com Loki/ELK |

## Referências

- [Spring Cloud Data Flow — Documentação Oficial](https://spring.io/projects/spring-cloud-dataflow)
- [Spring Cloud Task — Reference Guide](https://spring.io/projects/spring-cloud-task)
- [ADR-001: Arquitetura Hexagonal](./ADR-001-arquitetura-hexagonal.md)
- `BatchSchedulerConfig.java`, `BatchJobController.java`, `BatchJobsConfig.java`

## Revisões

- **2026-03-19**: Decisão inicial aceita

---

**Mantido por:** Time CineLog
**Próxima revisão:** Setembro 2026
