# 🚀 Getting Started

> Guia passo a passo para instalar, configurar e rodar o CineLog localmente.

---

## Pré-requisitos

| Ferramenta         | Versão Mínima | Verificação              |
| ------------------ | ------------- | ------------------------ |
| **Java (JDK)**     | 21            | `java -version`          |
| **Maven**          | 3.9+          | `mvn -version`           |
| **Docker**         | 24+           | `docker --version`       |
| **Docker Compose** | v2+           | `docker compose version` |
| **Git**            | 2.40+         | `git --version`          |

> 💡 O projeto inclui o **Maven Wrapper** (`./mvnw`), portanto você não precisa instalar o Maven globalmente.

---

## 1. Clone o Repositório

```bash
git clone https://github.com/marcusPrado02/cinelog.git
cd cinelog
```

---

## 2. Suba a Infraestrutura (Docker)

```bash
docker compose up -d
```

Isso inicia os seguintes serviços:

| Serviço            | Porta     | Descrição                |
| ------------------ | --------- | ------------------------ |
| **MySQL 8**        | 3306      | Banco de dados principal |
| **Redis 7**        | 6379      | Cache (AOF persistence)  |
| **Prometheus**     | 9090      | Coleta de métricas       |
| **Grafana**        | 3000      | Dashboards (admin/admin) |
| **Tempo**          | 3200      | Tracing distribuído      |
| **OTEL Collector** | 4317/4318 | Coletor OpenTelemetry    |
| **Jaeger**         | 16686     | UI de tracing            |
| **Elasticsearch**  | 9200      | Armazenamento de logs    |
| **Kibana**         | 5601      | Visualização de logs     |
| **Logstash**       | 5000      | Processamento de logs    |

---

## 3. Configure as Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto:

```bash
# Banco de Dados (já configurado no docker-compose, mas pode customizar)
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/cinelog
SPRING_DATASOURCE_USERNAME=cinelog
SPRING_DATASOURCE_PASSWORD=cinelog

# JWT (mude em produção!)
CINELOG_SECURITY_JWT_SECRET=MINHA_CHAVE_MEGA_SECRETA_DE_NO_MINIMO_32_CHARS

# TMDb API (obtenha em https://www.themoviedb.org/settings/api)
TMDB_API_KEY=coloque_sua_key_aqui

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

---

## 4. Rode a Aplicação

### Opção A: Maven (desenvolvimento)

```bash
./mvnw spring-boot:run
```

### Opção B: JAR compilado

```bash
./mvnw clean package -DskipTests
java -jar target/cinelog-0.0.1-SNAPSHOT.jar
```

### Opção C: Docker (com Docker Compose)

```bash
docker compose --profile app up -d
```

A aplicação estará disponível em: **http://localhost:8080**

---

## 5. Verifique se está funcionando

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI (abra no navegador)
open http://localhost:8080/swagger-ui.html
```

---

## 6. Primeiros Passos com a API

### 6.1 Criar um usuário

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Marcus",
    "email": "marcus@cinelog.com",
    "password": "Senh@Forte123!"
  }'
```

### 6.2 Fazer login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "marcus@cinelog.com",
    "password": "Senh@Forte123!"
  }'
```

> Copie o `accessToken` retornado para usar nas próximas chamadas.

### 6.3 Criar uma mídia

```bash
curl -X POST http://localhost:8080/api/v1/media \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "title": "Inception",
    "type": "MOVIE",
    "releaseYear": 2010
  }'
```

### 6.4 Registrar como assistido

```bash
curl -X POST http://localhost:8080/api/v1/watch-entries \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "mediaId": 1,
    "status": "COMPLETED",
    "rating": 9.5,
    "review": "Uma obra-prima de Christopher Nolan"
  }'
```

---

## Spring Profiles

| Profile          | Uso                   | Comando                                  |
| ---------------- | --------------------- | ---------------------------------------- |
| **dev** (padrão) | Desenvolvimento local | `./mvnw spring-boot:run`                 |
| **test**         | Testes automatizados  | `./mvnw test`                            |
| **docker**       | Docker Compose        | Variável `SPRING_PROFILES_ACTIVE=docker` |
| **perf**         | Testes de performance | K6 + profile perf                        |
| **prod**         | Produção              | Variável `SPRING_PROFILES_ACTIVE=prod`   |

---

## Comandos Úteis

```bash
# Compilar
./mvnw compile

# Testes
./mvnw test

# Testes com cobertura
./mvnw clean verify

# Gerar relatório de cobertura
open target/site/jacoco/index.html

# Verificar dependências (OWASP)
./mvnw dependency-check:check

# Gerar SBOM (CycloneDX)
./mvnw cyclonedx:makeAggregateBom

# Lint (Checkstyle + PMD + SpotBugs)
./mvnw checkstyle:check pmd:check spotbugs:check

# Enforcer (Java 21+ e Maven 3.9+)
./mvnw enforcer:enforce
```

---

## Troubleshooting

| Problema                     | Solução                                                       |
| ---------------------------- | ------------------------------------------------------------- |
| **Porta 3306 em uso**        | `docker compose down` antes de subir novamente                |
| **MySQL connection refused** | Espere ~10s após `docker compose up` para o MySQL inicializar |
| **OutOfMemoryError**         | Aumente heap: `JAVA_OPTS=-Xmx512m`                            |
| **TMDb 401 Unauthorized**    | Verifique a variável `TMDB_API_KEY` no `.env`                 |
| **Testes falhando**          | Certifique-se de que Docker está rodando (Testcontainers)     |
| **Build lento**              | Use `-DskipTests` para compilação rápida                      |

---

## Próximos Passos

- 📖 [API Reference](API-Reference) — Documentação completa de todos os endpoints
- 🏗️ [Architecture](Architecture) — Entenda a arquitetura do projeto
- 🔒 [Security](Security) — OWASP Top 10:2025 implementado
- 🧪 [Testing](Testing) — Estratégia de testes
