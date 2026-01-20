# 📊 CineLog - Observability Stack

Stack completa de observabilidade para o CineLog, incluindo métricas, logs, traces e dashboards.

## 🎯 O que está incluído

### Serviços

-   **Prometheus**: Coleta e armazena métricas
-   **Loki**: Agregação e consulta de logs
-   **Tempo**: Distributed tracing
-   **Grafana**: Visualização e dashboards
-   **OpenTelemetry Collector**: Coleta de telemetria
-   **Promtail**: Shipping de logs para o Loki

### Dashboards

-   **Business Metrics**: Métricas de negócio (usuários, mídias, ratings, etc)
-   **Infrastructure & Performance**: JVM, HTTP, Database, etc
-   **Logs**: Visualização e análise de logs

## 🚀 Como Usar

### Iniciar a stack

```bash
cd /home/maps/Projetos/cinelog/cinelog/docker
docker-compose -f docker-compose.observability.yml up -d
```

### Acessar os serviços

| Serviço    | URL                     | Credenciais      |
| ---------- | ----------------------- | ---------------- |
| Grafana    | <http://localhost:3000> | admin / admin123 |
| Prometheus | <http://localhost:9090> | -                |
| Loki       | <http://localhost:3100> | -                |
| Tempo      | <http://localhost:3200> | -                |

### Parar a stack

```bash
docker-compose -f docker-compose.observability.yml down
```

## 📂 Estrutura de Arquivos

```text
observability/
├── grafana/
│   ├── dashboards/
│   │   ├── 1-business-metrics.json
│   │   ├── 2-infrastructure-performance.json
│   │   └── 3-logs.json
│   └── provisioning/
│       ├── datasources/
│       │   └── datasources.yml
│       └── dashboards/
│           └── dashboard-provider.yml
├── prometheus.yml
├── tempo.yaml
├── otel-collector-config.yaml
├── promtail-config.yaml
└── README.md (este arquivo)
```

## 📚 Documentação

-   **[Quick Start](../docs/OBSERVABILITY_QUICKSTART.md)**: Comece em 5 minutos
-   **[Dashboard Guide](../docs/OBSERVABILITY_DASHBOARD_GUIDE.md)**: Guia completo dos dashboards
-   **[Observability Docs](../docs/OBSERVABILITY.md)**: Documentação técnica completa
-   **[Status](../docs/OBSERVABILITY_STATUS_UPDATED.md)**: Status da implementação

## 🔧 Configuração

### Prometheus

O Prometheus está configurado para:

-   Scrape da aplicação a cada 5 segundos
-   Retenção de dados: 30 dias
-   Endpoint: `http://host.docker.internal:8080/actuator/prometheus`

### Loki

O Loki está configurado para:

-   Armazenamento local
-   Logs estruturados em JSON
-   Correlação com traces via traceId

### Tempo

O Tempo está configurado para:

-   Receber traces via OpenTelemetry
-   Armazenamento local
-   Integração com Loki para logs

### Grafana

O Grafana está configurado com:

-   Datasources pré-configurados (Prometheus, Loki, Tempo)
-   Dashboards provisionados automaticamente
-   Autenticação básica (admin / admin123)

## 🎨 Dashboards

### 1. Business Metrics

Monitora KPIs de negócio:

-   Usuários registrados
-   Taxa de login (sucesso/falha)
-   Mídias criadas por tipo
-   Watch entries e ratings
-   Atividade de gêneros

### 2. Infrastructure & Performance

Monitora saúde da aplicação:

-   CPU e memória JVM
-   Taxa de requisições HTTP
-   Latência (p95, p99)
-   Connection pool do banco
-   Taxa de erros

### 3. Logs

Visualização de logs:

-   Stream de logs em tempo real
-   Filtros por nível (ERROR, WARN, INFO)
-   Busca por texto
-   Correlação com traces

## 🔍 Queries Úteis

### PromQL (Prometheus)

```promql
# Taxa de requisições HTTP
rate(http_server_requests_seconds_count[5m])

# Uso de memória heap
jvm_memory_used_bytes{area="heap"}

# Mídias criadas nas últimas 24h
increase(cinelog_business_media_created_total[24h])
```

### LogQL (Loki)

```logql
# Todos os logs
{job="cinelog"}

# Apenas erros
{job="cinelog"} |= "ERROR"

# Logs de um controller
{job="cinelog"} |= "MediaController"
```

### TraceQL (Tempo)

```traceql
# Traces lentos (>1s)
{service.name="cinelog" && duration > 1s}

# Traces com erro
{service.name="cinelog" && status=error}
```

## 🐛 Troubleshooting

### Grafana não inicia

```bash
docker logs cinelog-grafana
docker restart cinelog-grafana
```

### Métricas não aparecem

1. Verifique se a aplicação está expondo métricas:

```bash
curl http://localhost:8080/actuator/prometheus
```

2. Verifique os targets do Prometheus: <http://localhost:9090/targets>

### Logs não aparecem

1. Verifique o Loki: `curl http://localhost:3100/ready`
2. Verifique o Promtail: `docker logs cinelog-promtail`

## 🔒 Segurança

### Produção

Para ambiente de produção, altere:

1. **Credenciais do Grafana**: Mude em `docker-compose.observability.yml`

```yaml
environment:
    GF_SECURITY_ADMIN_PASSWORD: sua_senha_forte_aqui
```

2. **Autenticação anônima**: Desabilite o acesso anônimo

```yaml
environment:
    GF_AUTH_ANONYMOUS_ENABLED: "false"
```

3. **HTTPS**: Configure SSL/TLS
4. **Firewall**: Limite acesso aos serviços

## 📊 Métricas Disponíveis

### Métricas de Negócio (cinelog*business*\*)

-   `auth_login_total`: Total de logins
-   `user_registered_total`: Total de usuários registrados
-   `media_created_total`: Total de mídias criadas
-   `watch_entry_created_total`: Total de watch entries
-   `rating_given_total`: Total de ratings dados
-   `genre_created_total`: Total de gêneros criados
-   E mais...

### Métricas de Sistema

-   `jvm_memory_*`: Métricas de memória JVM
-   `jvm_threads_*`: Métricas de threads
-   `http_server_requests_*`: Métricas HTTP
-   `hikaricp_connections_*`: Pool de conexões
-   `process_cpu_usage`: Uso de CPU

## 🤝 Contribuindo

Ao adicionar novas features:

1. **Adicione métricas de negócio** relevantes
2. **Use logs estruturados** com níveis apropriados
3. **Adicione traces** para operações críticas
4. **Atualize dashboards** se necessário

## 📝 Licença

Este projeto está sob a licença MIT.
