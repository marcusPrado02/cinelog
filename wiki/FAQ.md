# ❓ FAQ

> Perguntas frequentes sobre o CineLog.

---

## Geral

<details>
<summary><strong>O que é o CineLog?</strong></summary>

Uma plataforma de registro e acompanhamento de filmes e séries, com funcionalidades de catálogo, avaliação, review, listas de acompanhamento e integração com o TMDb. Construída com Spring Boot 3, Java 21, Arquitetura Hexagonal e boas práticas de engenharia de software.

</details>

<details>
<summary><strong>Quais são os principais diferenciais técnicos?</strong></summary>

- Arquitetura Hexagonal com Clean Architecture
- OWASP Top 10:2025 completo (10/10)
- Event-Driven com Kafka + Outbox/Inbox Pattern
- Observabilidade com os 3 pilares (métricas, logs, tracing)
- 5 Design Patterns implementados
- 10 ADRs documentados
- 82%+ de cobertura de testes
  </details>

---

## Instalação

<details>
<summary><strong>Quais são os pré-requisitos?</strong></summary>

| Ferramenta     | Versão                 |
| -------------- | ---------------------- |
| Java           | 21+                    |
| Maven          | 3.9+ (ou use `./mvnw`) |
| Docker         | 24+                    |
| Docker Compose | v2+                    |

</details>

<details>
<summary><strong>Como subir o projeto pela primeira vez?</strong></summary>

```bash
git clone https://github.com/marcusPrado02/cinelog.git
cd cinelog
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Acesse: `http://localhost:8080/swagger-ui.html`

</details>

<details>
<summary><strong>Erro: "Port 3306 already in use"</strong></summary>

Pare o MySQL local ou altere a porta no `docker-compose.yml`:

```yaml
ports:
    - "3307:3306" # Use 3307 em vez de 3306
```

E atualize `application-dev.yml` para conectar em `localhost:3307`.

</details>

<details>
<summary><strong>Erro: "Java 21 not found"</strong></summary>

Instale o JDK 21 via SDKMAN:

```bash
sdk install java 21.0.5-tem
sdk use java 21.0.5-tem
```

Ou use o Maven Wrapper que respeita `JAVA_HOME`.

</details>

---

## API

<details>
<summary><strong>Como autenticar na API?</strong></summary>

1. Registre um usuário: `POST /api/v1/auth/register`
2. Faça login: `POST /api/v1/auth/login`
3. Use o access token: `Authorization: Bearer <token>`
4. Renove com refresh token: `POST /api/v1/auth/refresh`
 </details>

<details>
<summary><strong>Quanto tempo duram os tokens?</strong></summary>

| Token         | TTL    | Renovação                 |
| ------------- | ------ | ------------------------- |
| Access Token  | 15 min | Via refresh token         |
| Refresh Token | 7 dias | Token rotation automática |

</details>

<details>
<summary><strong>Como funciona a paginação?</strong></summary>

Todos os endpoints de listagem suportam:

```
GET /api/v1/media?page=0&size=20&sort=title,asc
```

Resposta:

```json
{
  "content": [...],
  "page": { "size": 20, "number": 0, "totalElements": 150, "totalPages": 8 }
}
```

</details>

<details>
<summary><strong>Quais são os possíveis status de um WatchEntry?</strong></summary>

```
PLANNING → WATCHING → COMPLETED
              ↓           ↓
            DROPPED    (rewatch → WATCHING)
```

Transições são controladas pelo **State Pattern**. Transições inválidas retornam HTTP 422.

</details>

---

## Arquitetura

<details>
<summary><strong>Por que Arquitetura Hexagonal?</strong></summary>

- **Testabilidade**: domínio testável sem framework
- **Inversão de dependência**: core não depende de infra
- **Flexibilidade**: trocar MySQL por PostgreSQL sem alterar regras de negócio
- **Clareza**: separação explícita de responsabilidades

Veja [ADR-001](ADR-Index) para detalhes.

</details>

<details>
<summary><strong>Por que Kafka + Outbox e não publicar direto no Kafka?</strong></summary>

O **Outbox Pattern** resolve o problema de **dual-write**: se a transação do banco falhar após o envio ao Kafka, os dados ficam inconsistentes. Com Outbox, o evento é salvo na mesma transação que a entidade, e um scheduler envia para o Kafka depois.

Veja [ADR-006](ADR-Index) para detalhes.

</details>

<details>
<summary><strong>Como funciona a idempotência dos eventos?</strong></summary>

O **Inbox Pattern**: cada evento tem um `eventId` único. O consumer verifica na tabela `inbox_event` se já processou aquele ID antes de executar. Isso garante **exactly-once semantics** no nível da aplicação.

</details>

---

## Segurança

<details>
<summary><strong>Quais categorias OWASP estão implementadas?</strong></summary>

Todas as 10 categorias do OWASP Top 10:2025:

| #   | Categoria                     | Status |
| --- | ----------------------------- | ------ |
| A01 | Broken Access Control         | ✅     |
| A02 | Cryptographic Failures        | ✅     |
| A03 | Injection                     | ✅     |
| A04 | Insecure Design               | ✅     |
| A05 | Security Misconfiguration     | ✅     |
| A06 | Vulnerable Components         | ✅     |
| A07 | Authentication Failures       | ✅     |
| A08 | Data Integrity Failures       | ✅     |
| A09 | Logging & Monitoring Failures | ✅     |
| A10 | SSRF / Exceptional Conditions | ✅     |

Veja [Security](Security) para detalhes.

</details>

<details>
<summary><strong>Como o rate limiting funciona?</strong></summary>

Via **Bucket4j** com Redis backend:

- Login: 5 tentativas por IP em 15 min
- API geral: 100 requests/min por usuário
- Registros: 3 por hora por IP

Retorna `HTTP 429 Too Many Requests` quando excedido.

</details>

---

## Cache

<details>
<summary><strong>Quando o cache é invalidado?</strong></summary>

| Método          | Invalidação                                               |
| --------------- | --------------------------------------------------------- |
| **TTL**         | Expira automaticamente (1h para mídias, 24h para gêneros) |
| **@CacheEvict** | Ao atualizar ou deletar entidade                          |
| **Manual**      | Via endpoint admin (se necessário)                        |

</details>

<details>
<summary><strong>O que acontece se o Redis estiver down?</strong></summary>

A aplicação continua funcionando normalmente — as requisições vão direto ao banco de dados. O cache é uma otimização, não um requisito. Quando o Redis voltar, o cache é popular sob demanda.

</details>

---

## Performance

<details>
<summary><strong>Como rodar testes de carga?</strong></summary>

```bash
# Smoke test (10 VUs, 30s)
k6 run performance/k6/smoke.js

# Load test para mídias
k6 run performance/k6/load-media.js

# Fluxo completo de autenticação
k6 run performance/k6/auth-flow.js
```

</details>

<details>
<summary><strong>Quais são os thresholds de performance?</strong></summary>

| Métrica        | Threshold   |
| -------------- | ----------- |
| p(95) latência | < 500ms     |
| Taxa de erro   | < 1%        |
| Throughput     | > 100 req/s |

</details>

---

## Observabilidade

<details>
<summary><strong>Quais dashboards estão disponíveis no Grafana?</strong></summary>

1. **Business Metrics** — registros, logins, mídias criadas
2. **Infrastructure & Performance** — JVM, HikariCP, latências
3. **Logs** — centralização e busca via Loki/ELK

Acesse: `http://localhost:3000` (admin/admin)

</details>

<details>
<summary><strong>Como ver os traces distribuídos?</strong></summary>

Via **Tempo** no Grafana ou **Jaeger UI**:

- Grafana Tempo: `http://localhost:3000` → Explore → Tempo
- Jaeger: `http://localhost:16686`

Cada requisição HTTP gera um `traceId` retornado no header `X-Trace-Id`.

</details>

---

## Troubleshooting

<details>
<summary><strong>Testes falham com "Connection refused" para MySQL</strong></summary>

Verifique se o Docker está rodando e os containers estão up:

```bash
docker compose ps
docker compose up -d db
```

Teste a comunicação: `docker exec -it cinelog-db mysql -ucinelog -pcinelog cinelog`

</details>

<details>
<summary><strong>Build falha no Checkstyle/PMD</strong></summary>

Rode individualmente para identificar:

```bash
./mvnw checkstyle:check
./mvnw pmd:check
```

Os arquivos de regras estão em `config/`.

</details>

<details>
<summary><strong>Kafka consumer não consome mensagens</strong></summary>

1. Verifique se o Kafka está rodando: `docker compose ps kafka`
2. Cheque os tópicos: `docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092`
3. Verifique o consumer group: log da aplicação deve mostrar `o.a.k.c.c.internals.ConsumerCoordinator`
 </details>

<details>
<summary><strong>Erro OWASP Dependency-Check em CI</strong></summary>

O plugin `dependency-check-maven` verifica CVEs. Para atualizar a base:

```bash
./mvnw org.owasp:dependency-check-maven:update-only
```

Para suprimir falsos positivos, edite `config/dependency-check-suppression.xml`.

</details>
