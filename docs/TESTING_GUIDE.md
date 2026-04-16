# 🎬 CineLog — Guia Completo de Testes e Validação

> **Objetivo:** Roteiro passo-a-passo para demonstrar **todas** as funcionalidades do CineLog a um avaliador sênior, cobrindo: infra local, autenticação dual (JWT local + OAuth2/Keycloak), CRUDs via Swagger, segurança (RBAC, MFA, PKCE), observabilidade, scripts automatizados e testes unitários/integração.
>
> **Atualizado em:** 2025-03-13 — inclui scripts bash automatizados (`run-all-tests.sh`, `demo-security-senior.sh`), dual auth, usuário `marcus` com MFA, e inventário atualizado de 117 classes / 629 métodos de teste.

---

## Sumário

1. [Pré-requisitos](#1-pré-requisitos)
2. [Subindo a Infraestrutura (Docker Compose)](#2-subindo-a-infraestrutura)
3. [Subindo a Aplicação](#3-subindo-a-aplicação)
4. [Acesso ao Swagger UI](#4-acesso-ao-swagger-ui)
5. [Autenticação Local (JWT HS384)](#5-autenticação-local-jwt)
6. [Autenticação OAuth2 / Keycloak (SSO)](#6-autenticação-oauth2--keycloak-sso)
7. [Testando os CRUDs via Swagger](#7-testando-os-cruds-via-swagger)
    - 7.1 [Genres](#71-genres)
    - 7.2 [Media (Filmes/Séries)](#72-media-filmesséries)
    - 7.3 [Seasons](#73-seasons)
    - 7.4 [Episodes](#74-episodes)
    - 7.5 [People (Pessoas)](#75-people)
    - 7.6 [Credits](#76-credits)
    - 7.7 [Users](#77-users)
    - 7.8 [Watch Entries](#78-watch-entries)
    - 7.9 [Watchlist](#79-watchlist)
    - 7.10 [Search & Discovery](#710-search--discovery)
    - 7.11 [Recommendations](#711-recommendations)
    - 7.12 [Popularity](#712-popularity)
    - 7.13 [User Insights](#713-user-insights)
    - 7.14 [Watch Progress](#714-watch-progress)
    - 7.15 [Admin — Dead Letter Queue](#715-admin--dead-letter-queue)
    - 7.16 [Admin — Media (TMDb Sync)](#716-admin--media-tmdb-sync)
    - 7.17 [Reports & Email](#717-reports--email)
    - 7.18 [Admin — Batch Jobs (TMDb Sync)](#718-admin--batch-jobs-tmdb-sync)
8. [Validando Segurança](#8-validando-segurança)
9. [Observabilidade (Health, Metrics, Tracing)](#9-observabilidade)
10. [Rodando os Testes Automatizados (JUnit)](#10-rodando-os-testes-automatizados)
11. [Scripts de Teste Automatizado (Bash)](#11-scripts-de-teste-automatizado-bash)
12. [Referência Rápida — Todos os Endpoints](#12-referência-rápida--todos-os-endpoints)
13. [Problemas Conhecidos & Notas](#13-problemas-conhecidos--notas)
14. [Testes de E-mail & Relatórios](#14-testes-de-e-mail--relatórios)
15. [Observabilidade & Métricas de Negócio](#15-observabilidade--métricas-de-negócio)
16. [Arquitetura Dual Auth (JWT Local + Keycloak OAuth2)](#16-arquitetura-dual-auth)

---

## 1. Pré-requisitos

| Ferramenta              | Versão mínima                | Verificar                                            |
| ----------------------- | ---------------------------- | ---------------------------------------------------- |
| **Java**                | 21+                          | `java -version`                                      |
| **Docker**              | 24+                          | `docker --version`                                   |
| **Docker Compose**      | v2+                          | `docker compose version`                             |
| **Maven** (via wrapper) | 3.9+                         | `./mvnw --version`                                   |
| **curl + jq**           | Qualquer                     | `which curl jq`                                      |
| **Portas livres**       | 8080, 8180, 3306, 6379, 9092 | `ss -tlnp \| grep -E '8080\|8180\|3306\|6379\|9092'` |

### Stack tecnológica

| Componente              | Versão     |
| ----------------------- | ---------- |
| Spring Boot             | 3.5.11     |
| Java (pom)              | 21         |
| Keycloak                | 26.1.0     |
| MySQL                   | 8.0        |
| Redis                   | 7 (Alpine) |
| Kafka (Confluent)       | 7.5.0      |
| JaCoCo                  | 0.8.14     |
| Mockito                 | 5.12.0     |
| ArchUnit                | 1.3.0      |
| Testcontainers-Keycloak | 3.4.0      |
| JJWT                    | 0.12.6     |

---

## 2. Subindo a Infraestrutura

```bash
# Na raiz do projeto — serviços principais:
docker compose up -d db redis keycloak mailhog

# Kafka + Zookeeper (definidos em arquivo separado):
docker compose -f docker/docker-compose.dev.yml up -d zookeeper kafka

# Stack de observabilidade (Prometheus, Grafana, Loki, Tempo, Jaeger):
docker compose up -d prometheus grafana loki promtail tempo jaeger otel-collector
```

Aguarde todos ficarem healthy:

```bash
docker compose ps
docker compose -f docker/docker-compose.dev.yml ps
# Todos devem mostrar "healthy" ou "running"
```

### Verificações rápidas

| Serviço        | URL de teste                                                          | Esperado            |
| -------------- | --------------------------------------------------------------------- | ------------------- |
| MySQL          | `docker exec cinelog-mysql mysqladmin ping -u cinelog -pcinelog`      | `mysqld is alive`   |
| Redis          | `docker exec cinelog-redis redis-cli ping`                            | `PONG`              |
| Keycloak       | http://localhost:8180                                                 | Tela de boas-vindas |
| Keycloak Admin | http://localhost:8180/admin (admin/admin)                             | Console admin       |
| Keycloak Realm | http://localhost:8180/realms/cinelog/.well-known/openid-configuration | JSON OpenID Connect |

---

## 3. Subindo a Aplicação

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Aguarde a mensagem:

```
Started CinelogApplication in X.XXs
```

Teste rápido:

```bash
curl -s http://localhost:8080/actuator/health | jq .
# Esperado: {"status":"UP"}
```

---

## 4. Acesso ao Swagger UI

Abra no navegador: **http://localhost:8080/swagger-ui/index.html**

Você verá todos os endpoints organizados por grupo. Há dois esquemas de autenticação:

- **BearerAuth** — JWT local (login/register do próprio app)
- **keycloak-sso** — OAuth2 Authorization Code + PKCE via Keycloak

---

## 5. Autenticação Local (JWT)

### 5.1. Registrar um usuário

No Swagger, abra **Auth → POST /api/auth/register** e use:

```json
{
    "name": "Marcus Test",
    "email": "marcus@cinelog.dev",
    "password": "MarcusTest@2025!"
}
```

> A senha deve ter: ≥ 8 chars, 1 maiúscula, 1 minúscula, 1 dígito, 1 caractere especial.

**Resposta esperada (201):**

```json
{
    "accessToken": "eyJhb...",
    "refreshToken": "uuid-string",
    "tokenType": "Bearer",
    "expiresIn": 3600
}
```

### 5.2. Login

**Auth → POST /api/auth/login**:

```json
{
    "email": "marcus@cinelog.dev",
    "password": "MarcusTest@2025!"
}
```

### 5.3. Autorizar o Swagger

1. Copie o `accessToken` retornado
2. No Swagger, clique no cadeado **Authorize** (topo da página)
3. Em **BearerAuth**, cole o token (sem o prefixo "Bearer ")
4. Clique **Authorize**

A partir de agora, todas as requisições incluirão o header `Authorization: Bearer <token>`.

### 5.4. Refresh Token

**Auth → POST /api/auth/refresh**:

```json
{
    "refreshToken": "<refresh_token_do_login>"
}
```

### 5.5. Logout

**Auth → POST /api/auth/logout** (precisa estar autenticado via Swagger Authorize)

**Resposta esperada:** `204 No Content`

---

## 6. Autenticação OAuth2 / Keycloak (SSO)

### 6.1. Via Swagger UI (Authorization Code + PKCE)

1. No Swagger, clique em **Authorize**
2. Na seção **keycloak-sso**, clique **Authorize**
3. Será redirecionado para o login do Keycloak
4. Use as credenciais:

| Usuário     | Senha                   | Roles                                                    | MFA  |
| ----------- | ----------------------- | -------------------------------------------------------- | ---- |
| `alice`     | `Alice@CineLog2025!`    | USER                                                     | Não  |
| `admin`     | `Admin@CineLog2025!`    | USER, ADMIN                                              | Não  |
| `alice-mfa` | `AliceMfa@CineLog2025!` | USER                                                     | TOTP |
| `marcus`    | `Marcus@CineLog2025!`   | USER, ADMIN, OPS, CONTENT_ADMIN, MEDIA_ADMIN, USER_ADMIN | TOTP |

5. Após o login, será redirecionado de volta ao Swagger com token OAuth2
6. Todas as requisições agora usarão o token Keycloak

### 6.2. Via cURL (Resource Owner Password — apenas para teste)

```bash
# Obter token Keycloak para 'alice'
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/cinelog/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=cinelog-app" \
  -d "username=alice" \
  -d "password=Alice@CineLog2025!" \
  | jq -r '.access_token')

echo $TOKEN

# Usar o token na API
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/media | jq .
```

### 6.3. MFA (Multi-Factor Authentication)

1. Faça login como `marcus` (ou `alice-mfa`) no Keycloak: http://localhost:8180/realms/cinelog/account
2. Na **primeira vez**, Keycloak pedirá para configurar o TOTP:
    - Escaneie o QR code com **Google Authenticator** / **FreeOTP** / **Microsoft Authenticator**
    - Insira o código de 6 dígitos gerado pelo app
3. Nos logins seguintes, será sempre pedido o código TOTP além da senha
4. Após MFA configurado, teste no Swagger: **Authorize → keycloak-sso** → login com `marcus` + senha + TOTP

### 6.4. Console Admin do Keycloak

- URL: http://localhost:8180/admin
- Credenciais: `admin` / `admin`
- Realm: selecione **cinelog** no dropdown
- Aqui é possível: criar usuários, ver sessões, configurar flows de autenticação, ver logs de eventos

---

## 7. Testando os CRUDs via Swagger

> **Importante:** Para cada seção abaixo, certifique-se de estar autenticado (seção 5.3 ou 6.1).

### 7.1. Genres

| Ação              | Endpoint                     | Body de exemplo                  |
| ----------------- | ---------------------------- | -------------------------------- |
| **Criar**         | `POST /api/v1/genres`        | `{"name": "Ação", "tmdbId": 28}` |
| **Listar**        | `GET /api/v1/genres`         | — (paginado)                     |
| **Buscar por ID** | `GET /api/v1/genres/{id}`    | —                                |
| **Atualizar**     | `PUT /api/v1/genres/{id}`    | `{"name": "Ação/Aventura"}`      |
| **Deletar**       | `DELETE /api/v1/genres/{id}` | —                                |

**Validações a testar:**

- [ ] Criar sem `name` → `400 Bad Request`
- [ ] Buscar ID inexistente → `404 Not Found`
- [ ] Deletar ID inexistente → `404 Not Found`

---

### 7.2. Media (Filmes/Séries)

| Ação              | Endpoint                    | Body de exemplo                                                                                                                                                                       |
| ----------------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Criar**         | `POST /api/v1/media`        | `{"title": "The Matrix", "type": "MOVIE", "releaseYear": 1999, "synopsis": "Neo descobre a verdade", "posterUrl": "https://image.tmdb.org/t/p/w500/aF8ylMEn4f28KNHhq5SAhNvIzC7.jpg"}` |
| **Listar**        | `GET /api/v1/media`         | — (paginado)                                                                                                                                                                          |
| **Buscar por ID** | `GET /api/v1/media/{id}`    | —                                                                                                                                                                                     |
| **Atualizar**     | `PUT /api/v1/media/{id}`    | `{"title": "The Matrix Reloaded", "releaseYear": 2003}`                                                                                                                               |
| **Deletar**       | `DELETE /api/v1/media/{id}` | —                                                                                                                                                                                     |

**Tipos válidos para `type`:** `MOVIE`, `SERIES`

**Validações a testar:**

- [ ] Criar sem `title` → `400`
- [ ] Criar com `type` inválido → `400`
- [ ] Criar com `releaseYear` no futuro → verificar comportamento

---

### 7.3. Seasons

> Seasons são vinculadas a uma Media do tipo `SERIES`.

| Ação              | Endpoint                      | Body de exemplo                                                                  |
| ----------------- | ----------------------------- | -------------------------------------------------------------------------------- |
| **Criar**         | `POST /api/v1/seasons`        | `{"mediaId": 1, "seasonNumber": 1, "title": "Temporada 1", "releaseYear": 2020}` |
| **Listar**        | `GET /api/v1/seasons`         | —                                                                                |
| **Buscar por ID** | `GET /api/v1/seasons/{id}`    | —                                                                                |
| **Atualizar**     | `PUT /api/v1/seasons/{id}`    | `{"title": "Temporada 1 - Revisada"}`                                            |
| **Deletar**       | `DELETE /api/v1/seasons/{id}` | —                                                                                |

**Validações:**

- [ ] Criar season para Media tipo `MOVIE` → deve falhar
- [ ] Criar season com `seasonNumber` duplicado → deve falhar

---

### 7.4. Episodes

| Ação              | Endpoint                       | Body de exemplo                                                                 |
| ----------------- | ------------------------------ | ------------------------------------------------------------------------------- |
| **Criar**         | `POST /api/v1/episodes`        | `{"seasonId": 1, "episodeNumber": 1, "title": "Piloto", "durationMinutes": 45}` |
| **Listar**        | `GET /api/v1/episodes`         | —                                                                               |
| **Buscar por ID** | `GET /api/v1/episodes/{id}`    | —                                                                               |
| **Atualizar**     | `PUT /api/v1/episodes/{id}`    | `{"title": "Pilot Episode"}`                                                    |
| **Deletar**       | `DELETE /api/v1/episodes/{id}` | —                                                                               |

---

### 7.5. People

| Ação              | Endpoint                     | Body de exemplo                                                                      |
| ----------------- | ---------------------------- | ------------------------------------------------------------------------------------ |
| **Criar**         | `POST /api/v1/people`        | `{"name": "Keanu Reeves", "biography": "Ator canadense", "birthDate": "1964-09-02"}` |
| **Listar**        | `GET /api/v1/people`         | —                                                                                    |
| **Buscar por ID** | `GET /api/v1/people/{id}`    | —                                                                                    |
| **Atualizar**     | `PUT /api/v1/people/{id}`    | `{"biography": "Ator canadense famoso por Matrix"}`                                  |
| **Deletar**       | `DELETE /api/v1/people/{id}` | —                                                                                    |

---

### 7.6. Credits

> Vincula uma Person a uma Media com um papel (ator, diretor, etc.).

| Ação              | Endpoint                      | Body de exemplo                                                          |
| ----------------- | ----------------------------- | ------------------------------------------------------------------------ |
| **Criar**         | `POST /api/v1/credits`        | `{"mediaId": 1, "personId": 1, "role": "ACTOR", "characterName": "Neo"}` |
| **Listar**        | `GET /api/v1/credits`         | —                                                                        |
| **Buscar por ID** | `GET /api/v1/credits/{id}`    | —                                                                        |
| **Atualizar**     | `PUT /api/v1/credits/{id}`    | `{"characterName": "Thomas Anderson / Neo"}`                             |
| **Deletar**       | `DELETE /api/v1/credits/{id}` | —                                                                        |

---

### 7.7. Users

| Ação              | Endpoint                     | Body de exemplo                                                                 | Auth             |
| ----------------- | ---------------------------- | ------------------------------------------------------------------------------- | ---------------- |
| **Criar**         | `POST /api/v1/users`         | `{"name": "Test User", "email": "test@test.com", "password": "TestUser@2025!"}` | Auth             |
| **Listar**        | `GET /api/v1/users`          | —                                                                               | **ADMIN only**   |
| **Buscar por ID** | `GET /api/v1/users/{id}`     | —                                                                               | ADMIN ou próprio |
| **Atualizar**     | `PUT /api/v1/users/{id}`     | `{"name": "Updated Name"}`                                                      | ADMIN ou próprio |
| **Deletar**       | `DELETE /api/v1/users/{id}`  | —                                                                               | **ADMIN only**   |
| **Minhas stats**  | `GET /api/v1/users/me/stats` | —                                                                               | Auth             |

**Validações de segurança:**

- [ ] Como USER (alice), tentar `GET /api/v1/users` → `403 Forbidden`
- [ ] Como USER, tentar `DELETE /api/v1/users/{outro_id}` → `403 Forbidden`
- [ ] Como ADMIN, `GET /api/v1/users` → `200` com lista

---

### 7.8. Watch Entries

> Registros de "assisti este conteúdo" com rating e status.

| Ação              | Endpoint                            | Body de exemplo                                                                              |
| ----------------- | ----------------------------------- | -------------------------------------------------------------------------------------------- |
| **Criar**         | `POST /api/v1/watch-entries`        | `{"userId": 1, "mediaId": 1, "status": "COMPLETED", "rating": 9.5, "review": "Obra-prima!"}` |
| **Listar**        | `GET /api/v1/watch-entries`         | Params: `userId`, `mediaId`, `page`, `size`                                                  |
| **Buscar por ID** | `GET /api/v1/watch-entries/{id}`    | —                                                                                            |
| **Atualizar**     | `PUT /api/v1/watch-entries/{id}`    | `{"rating": 10.0, "review": "Revi e é ainda melhor"}`                                        |
| **Deletar**       | `DELETE /api/v1/watch-entries/{id}` | —                                                                                            |

**Status possíveis:** `PLANNING`, `WATCHING`, `COMPLETED`, `DROPPED`

**Validações:**

- [ ] Rating fora de 0-10 → `400`
- [ ] Status `COMPLETED` → `WATCHING` (transição de estado — verificar regras)

---

### 7.9. Watchlist

> Lista de "quero assistir".

| Ação          | Endpoint                             | Body de exemplo  |
| ------------- | ------------------------------------ | ---------------- |
| **Adicionar** | `POST /api/v1/watchlist`             | `{"mediaId": 1}` |
| **Listar**    | `GET /api/v1/watchlist`              | — (paginado)     |
| **Remover**   | `DELETE /api/v1/watchlist/{mediaId}` | —                |

---

### 7.10. Search & Discovery

| Ação                | Endpoint                     | Parâmetros                                                                                                      |
| ------------------- | ---------------------------- | --------------------------------------------------------------------------------------------------------------- |
| **Busca avançada**  | `GET /api/media/search`      | `text`, `type`, `yearMin`, `yearMax`, `ratingMin`, `ratingMax`, `genreIds`, `page`, `size`, `sort`, `direction` |
| **Busca por texto** | `GET /api/media/search/text` | `q`, `page`, `size`                                                                                             |

**Exemplos:**

```
GET /api/media/search?text=Matrix&type=MOVIE&page=0&size=10
GET /api/media/search/text?q=Matrix
```

---

### 7.11. Recommendations

| Ação                   | Endpoint                                             | Parâmetros           |
| ---------------------- | ---------------------------------------------------- | -------------------- |
| **Recomendações**      | `GET /api/users/{userId}/recommendations`            | `limit` (default 20) |
| **Por estratégia**     | `GET /api/users/{userId}/recommendations/{strategy}` | `limit`              |
| **Listar estratégias** | `GET /api/users/{userId}/recommendations/strategies` | —                    |

**Estratégias disponíveis:** `content-based`, `collaborative`, `hybrid`

---

### 7.12. Popularity

| Ação                   | Endpoint                      | Parâmetros                  |
| ---------------------- | ----------------------------- | --------------------------- |
| **Mais bem avaliados** | `GET /api/media/top-rated`    | `limit` (default 20)        |
| **Trending**           | `GET /api/media/trending`     | `period` (7d, 30d), `limit` |
| **Mais assistidos**    | `GET /api/media/most-watched` | `limit`                     |

---

### 7.13. User Insights

| Ação                     | Endpoint                                  |
| ------------------------ | ----------------------------------------- |
| **Insights do usuário**  | `GET /api/users/{userId}/insights`        |
| **Verificar existência** | `GET /api/users/{userId}/insights/exists` |

---

### 7.14. Watch Progress

| Ação                    | Endpoint                                 | Body de exemplo                              |
| ----------------------- | ---------------------------------------- | -------------------------------------------- |
| **Atualizar progresso** | `POST /api/watchentries/{id}/progress`   | `{"currentEpisode": 5, "totalEpisodes": 24}` |
| **Ver progresso**       | `GET /api/watchentries/{id}/progress`    | —                                            |
| **Remover progresso**   | `DELETE /api/watchentries/{id}/progress` | —                                            |

---

### 7.15. Admin — Dead Letter Queue

> **Requer role ADMIN ou OPS.**

| Ação                  | Endpoint                         |
| --------------------- | -------------------------------- |
| **Listar DLQ**        | `GET /admin/dlq`                 |
| **Detalhe de evento** | `GET /admin/dlq/{id}`            |
| **Replay evento**     | `POST /admin/dlq/{id}/replay`    |
| **Ignorar evento**    | `POST /admin/dlq/{id}/ignore`    |
| **Estatísticas**      | `GET /admin/dlq/stats`           |
| **Listar tópicos**    | `GET /admin/dlq/topics`          |
| **Consumer groups**   | `GET /admin/dlq/consumer-groups` |

---

### 7.16. Admin — Media (TMDb Sync)

> **Requer role ADMIN.**

| Ação                | Endpoint                   | Body                 |
| ------------------- | -------------------------- | -------------------- |
| **Criar via admin** | `POST /api/v1/admin/media` | `MediaCreateRequest` |

> **Nota:** Este endpoint é um stub em desenvolvimento. Vai criar a resposta mas sem persistir no banco.

---

### 7.17. Reports, Email & PDF

> Requer autenticação. Endpoints `/admin/reports/*` requerem role **ADMIN**.
> Os emails são enviados via MailHog (http://localhost:8025) em ambiente de desenvolvimento.
> PDFs são gerados sob demanda via **Gotenberg** (`docker compose up -d gotenberg`).

#### User Reports (autenticado)

| Ação                       | Endpoint                                  | Descrição                            |
| -------------------------- | ----------------------------------------- | ------------------------------------ |
| **Ver digest semanal**     | `GET /api/v1/reports/weekly-digest`       | JSON do resumo semanal               |
| **Enviar digest**          | `POST /api/v1/reports/weekly-digest`      | Envio imediato por email             |
| **📄 PDF digest**          | `GET /api/v1/reports/weekly-digest/pdf`   | Download PDF                         |
| **Ver top avaliados**      | `GET /api/v1/reports/top-rated`           | Relatório de mídias mais avaliadas   |
| **Enviar top avaliados**   | `POST /api/v1/reports/top-rated`          | Envio imediato por email             |
| **📄 PDF top-rated**       | `GET /api/v1/reports/top-rated/pdf`       | Download PDF                         |
| **Ver recomendações**      | `GET /api/v1/reports/recommendations`     | Relatório de recomendações           |
| **Enviar recomendações**   | `POST /api/v1/reports/recommendations`    | Envio imediato por email             |
| **📄 PDF recomendações**   | `GET /api/v1/reports/recommendations/pdf` | Download PDF                         |
| **Ver trending**           | `GET /api/v1/reports/trending`            | Relatório de trending                |
| **Enviar trending**        | `POST /api/v1/reports/trending`           | Envio imediato por email             |
| **📄 PDF trending**        | `GET /api/v1/reports/trending/pdf`        | Download PDF                         |
| **Ver top actors**         | `GET /api/v1/reports/top-actors`          | Atores com filmes mais bem avaliados |
| **Enviar top actors**      | `POST /api/v1/reports/top-actors`         | Envio imediato por email             |
| **📄 PDF top-actors**      | `GET /api/v1/reports/top-actors/pdf`      | Download PDF                         |
| **Ver new releases**       | `GET /api/v1/reports/new-releases`        | Novos títulos adicionados            |
| **Enviar new releases**    | `POST /api/v1/reports/new-releases`       | Envio imediato por email             |
| **📄 PDF new-releases**    | `GET /api/v1/reports/new-releases/pdf`    | Download PDF                         |
| **Ver genre spotlight**    | `GET /api/v1/reports/genre-spotlight`     | Análise profunda de um gênero        |
| **Enviar genre spotlight** | `POST /api/v1/reports/genre-spotlight`    | Envio imediato por email             |
| **📄 PDF genre-spotlight** | `GET /api/v1/reports/genre-spotlight/pdf` | Download PDF                         |

#### Admin Reports

| Ação                         | Endpoint                                 | Descrição                                     |
| ---------------------------- | ---------------------------------------- | --------------------------------------------- |
| **Ver platform report**      | `GET /api/v1/admin/reports/platform`     | Relatório geral da plataforma                 |
| **Enviar platform report**   | `POST /api/v1/admin/reports/platform`    | Enviar por email (admin)                      |
| **📄 PDF platform**          | `GET /api/v1/admin/reports/platform/pdf` | Download PDF (landscape)                      |
| **Disparar envios em massa** | `POST /api/v1/admin/reports/send-to-all` | Enviar trending para todos os usuários ativos |

#### Como testar o envio de email

1. Execute `POST /api/v1/reports/weekly-digest` no Swagger
2. Abra http://localhost:8025 (MailHog)
3. O email com o template dark/cinema deve aparecer na caixa de entrada
4. Inspecione o HTML para ver o template renderizado

#### Como testar a geração de PDF

1. Garanta que o Gotenberg está rodando: `docker compose up -d gotenberg`
2. Execute `GET /api/v1/reports/weekly-digest/pdf` no Swagger ou via cURL:
    ```bash
    curl -s -H "Authorization: Bearer $TOKEN" \
      http://localhost:8080/api/v1/reports/weekly-digest/pdf -o digest.pdf
    ```
3. Abra o arquivo `digest.pdf` — deve conter o relatório com tema dark/cinema CineLog
4. Para relatórios com parâmetros: `GET /api/v1/reports/trending/pdf?days=30&limit=5`
5. O relatório de plataforma (admin) é gerado em **paisagem**: `GET /api/v1/admin/reports/platform/pdf`

#### PDF como anexo de email

Quando configurado `cinelog.reports.pdf.attach-to-email=true`, os POSTs de envio anexam o PDF automaticamente.
Se o Gotenberg estiver offline, o email é enviado normalmente sem anexo (fail-safe).

**Templates de e-mail:** `weekly-digest`, `top-rated`, `recommendations`, `trending`, `platform-report`, `top-actors`, `new-releases`, `genre-spotlight`

**Templates de PDF:** mesmos nomes, em `templates/pdf/` — tema dark com CSS inline para Gotenberg

---

### 7.18. Admin — Batch Jobs (TMDb Sync)

> **Requer role ADMIN.** Dispara jobs Spring Batch para popular o banco com dados do TMDb.

| Ação                    | Endpoint                            | Parâmetros             |
| ----------------------- | ----------------------------------- | ---------------------- |
| **Importar gêneros**    | `POST /api/v1/admin/batch/genres`   | —                      |
| **Importar filmes**     | `POST /api/v1/admin/batch/movies`   | `maxPages` (default 5) |
| **Importar séries**     | `POST /api/v1/admin/batch/tv-shows` | `maxPages`             |
| **Importar créditos**   | `POST /api/v1/admin/batch/credits`  | —                      |
| **Importar temporadas** | `POST /api/v1/admin/batch/seasons`  | —                      |

**Exemplo via cURL:**

```bash
# Obter token admin e disparar import de filmes (5 páginas)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@cinelog.dev","password":"Admin@CineLog2025!"}' \
  | jq -r '.accessToken')

curl -X POST "http://localhost:8080/api/v1/admin/batch/movies?maxPages=5" \
  -H "Authorization: Bearer $TOKEN"
```

> Requer a variável `TMDB_API_KEY` configurada no ambiente (JWT Bearer do TMDb).

---

## 8. Validando Segurança

### 8.1. Dual Auth — JWT Local + Keycloak coexistentes

O CineLog suporta **dois mecanismos de autenticação** no mesmo SecurityFilterChain:

| Mecanismo       | Algoritmo | Quando usar                      | Quem valida                       |
| --------------- | --------- | -------------------------------- | --------------------------------- |
| **JWT Local**   | HS384     | Login/register via `/api/auth/*` | `JwtAuthenticationFilter`         |
| **OAuth2 (KC)** | RS256     | Login via Keycloak (Swagger SSO) | `BearerTokenAuthenticationFilter` |

**Como funciona:** O `JwtAuthenticationFilter` decodifica o payload do token e verifica o campo `iss`. Se `iss` corresponde ao Keycloak (`http://localhost:8180/realms/cinelog`), o filtro delega para o `BearerTokenAuthenticationFilter` (OAuth2 Resource Server). Caso contrário, valida como token local HS384.

Um `BearerTokenResolver` customizado garante que o OAuth2 Resource Server **só processe tokens Keycloak**, evitando que tente validar tokens locais com JWKS RS256.

```
SecurityFilterChain (ordem dos filtros):
┌─────────────────────────────────────────────────┐
│  1. RateLimitFilter          (proteção DDoS)    │
│  2. SqlInjectionFilter       (input sanitization)│
│  3. JwtAuthenticationFilter  (detecção dual auth)│
│     ├─ Token local (HS384) → valida interno     │
│     └─ Token KC (RS256)    → delega passo 4     │
│  4. BearerTokenAuthFilter    (OAuth2 Resource)  │
│  5. @PreAuthorize / @PostAuthorize (method-lvl) │
└─────────────────────────────────────────────────┘
```

### 8.2. Testes de autenticação

| Teste                                    | Como fazer                                             | Resultado esperado |
| ---------------------------------------- | ------------------------------------------------------ | ------------------ |
| **Sem token**                            | `curl http://localhost:8080/api/v1/media`              | `401 Unauthorized` |
| **Token expirado**                       | Esperar expiração (1h) ou gerar token com data passada | `401`              |
| **Token inválido**                       | `curl -H "Authorization: Bearer abc123" ...`           | `401`              |
| **Token Keycloak em endpoint protegido** | Obter token Keycloak e acessar `/api/v1/media`         | `200` ou `404`     |
| **Token local em endpoint protegido**    | Login local e acessar `/api/v1/media`                  | `200` ou `404`     |
| **Ambos tokens no mesmo endpoint**       | Testar com cada tipo separadamente                     | Ambos aceitos      |

### 8.3. Testes de autorização (RBAC)

| Teste             | Usuário       | Endpoint                          | Esperado |
| ----------------- | ------------- | --------------------------------- | -------- |
| USER acessa media | alice (USER)  | `GET /api/v1/media`               | `200`    |
| USER cria media   | alice (USER)  | `POST /api/v1/media`              | `201`    |
| USER lista users  | alice (USER)  | `GET /api/v1/users`               | `403`    |
| USER deleta user  | alice (USER)  | `DELETE /api/v1/users/{id}`       | `403`    |
| ADMIN lista users | admin (ADMIN) | `GET /api/v1/users`               | `200`    |
| ADMIN acessa DLQ  | admin (ADMIN) | `GET /admin/dlq`                  | `200`    |
| USER acessa DLQ   | alice (USER)  | `GET /admin/dlq`                  | `403`    |
| USER acessa batch | alice (USER)  | `POST /api/v1/admin/batch/genres` | `403`    |

**Annotations de segurança em uso:**

| Controller                  | Path prefix           | Annotation                                                |
| --------------------------- | --------------------- | --------------------------------------------------------- |
| `BatchJobController`        | `/api/v1/admin/batch` | `@PreAuthorize("hasRole('ADMIN')")` (classe)              |
| `AdminMediaController`      | `/api/v1/admin/media` | `@PreAuthorize("hasRole('ADMIN')")` (classe)              |
| `DeadLetterAdminController` | `/admin/dlq`          | `@PreAuthorize("hasAnyRole('ADMIN', 'OPS')")` (classe)    |
| `UserController`            | `/api/v1/users`       | Misto: ADMIN para list/delete, ADMIN-or-self para put/get |
| `ReportController`          | `/api/v1/reports`     | `isAuthenticated()` (user) / `hasRole('ADMIN')` (admin)   |

### 8.4. Rate Limiting

O rate limiter usa **Fixed Window** via Redis:

| Escopo               | Limite      | Janela |
| -------------------- | ----------- | ------ |
| API geral            | 100 req/min | 60s    |
| Auth (`/api/auth/*`) | 10 req/min  | 60s    |

Ao exceder o limite: **HTTP 429 Too Many Requests**

Para resetar manualmente (Redis em Docker):

```bash
docker exec cinelog-redis redis-cli EVAL \
  "local k=redis.call('keys','ratelimit:*');if #k>0 then return redis.call('del',unpack(k)) else return 0 end" 0
```

### 8.5. Account Lockout

1. Tente login com senha errada **5 vezes consecutivas**
2. Na 6ª tentativa → `423 Locked` (conta bloqueada por 15 min)
3. Aguarde 15 min ou reinicie a aplicação

### 8.6. Password Policy (Registro)

Requisitos: **≥ 8 chars**, 1 maiúscula, 1 minúscula, 1 dígito, 1 caractere especial.

| Senha              | Resultado                                  |
| ------------------ | ------------------------------------------ |
| `123`              | `400` — muito curta                        |
| `abcdefghijklm`    | `400` — sem maiúsculas, dígitos, especiais |
| `Demo@Secure2025!` | `201` — válida                             |

---

## 9. Observabilidade

### 9.1. Health Check

```bash
curl http://localhost:8080/actuator/health | jq .
# Esperado: {"status":"UP","components":{"db":{"status":"UP"}, "redis":{"status":"UP"}, ...}}
```

Health indicators customizados incluídos:

- `tmdb` — conectividade com a API do TMDb
- `outbox` — estado do outbox de eventos Kafka

### 9.2. Endpoints Actuator Expostos

| Endpoint            | URL                    | Descrição                               |
| ------------------- | ---------------------- | --------------------------------------- |
| Health              | `/actuator/health`     | Status da aplicação e dependências      |
| Info                | `/actuator/info`       | Versão e nome da aplicação              |
| Prometheus (scrape) | `/actuator/prometheus` | Todas as métricas em formato Prometheus |
| Métricas JSON       | `/actuator/metrics`    | Lista de meters disponíveis             |
| Loggers             | `/actuator/loggers`    | Consulta e altera log levels em runtime |
| Caches              | `/actuator/caches`     | Gerencia caches Redis                   |

```bash
# Info da aplicação
curl http://localhost:8080/actuator/info | jq .

# Métricas Prometheus
curl http://localhost:8080/actuator/prometheus | grep cinelog

# Listar todos os meters
curl http://localhost:8080/actuator/metrics | jq .names

# Métricas de timer (gerado pelo @Measured / MetricsAspect)
curl 'http://localhost:8080/actuator/metrics/cinelog.method.execution' | jq .

# Listar loggers
curl http://localhost:8080/actuator/loggers | jq '.loggers["com.cine.cinelog"]'

# Alterar nível de log em runtime (sem reiniciar)
curl -X POST http://localhost:8080/actuator/loggers/com.cine.cinelog.features \
  -H 'Content-Type: application/json' \
  -d '{"configuredLevel": "DEBUG"}'
```

### 9.3. Métricas de Negócio (BusinessMetricsService)

Métricas customizadas disponíveis em `/actuator/prometheus`:

| Nome da métrica                             | Tipo    | Descrição                         |
| ------------------------------------------- | ------- | --------------------------------- |
| `cinelog_business_auth_login_total`         | Counter | Logins (tag: `success`)           |
| `cinelog_business_user_registered_total`    | Counter | Usuários registrados              |
| `cinelog_business_media_created_total`      | Counter | Mídias criadas (tag: `type`)      |
| `cinelog_business_watchentry_created_total` | Counter | Watch entries criadas             |
| `cinelog_business_watchlist_added_total`    | Counter | Adições à watchlist               |
| `cinelog_business_rating_given_total`       | Counter | Ratings registrados               |
| `cinelog_integration_tmdb_calls_total`      | Counter | Chamadas à API TMDb               |
| `cinelog_integration_duration`              | Timer   | Latência de integrações externas  |
| `cinelog_batch_job_executed_total`          | Counter | Jobs Batch executados             |
| `cinelog_method_execution`                  | Timer   | Tempo de execução (MetricsAspect) |

### 9.4. Tracing — Propagação de Trace Headers

Todo request gera headers de rastreamento na resposta:

```bash
curl -v http://localhost:8080/actuator/health 2>&1 | grep -i 'x-trace\|x-span\|x-request'
# Esperado:
# X-Trace-Id: <traceId>
# X-Span-Id: <spanId>
# X-Request-Id: <uuid>
```

Os campos `traceId`, `spanId`, `userId`, `requestId`, `tookMs`, `status` aparecem em **todos os logs JSON** (MDC automático via `ObservabilityContextFilter`).

### 9.5. Stack de Observabilidade Completa

```bash
# Subir toda a stack de observabilidade (já no docker-compose principal):
docker compose up -d prometheus grafana loki promtail tempo jaeger otel-collector
```

| Serviço    | URL                                 | Descrição                       |
| ---------- | ----------------------------------- | ------------------------------- |
| Grafana    | http://localhost:3000 (admin/admin) | Dashboards (auto-provisionados) |
| Prometheus | http://localhost:9090               | Dados de métricas               |
| Loki       | http://localhost:3100               | Logs centralizados              |
| Tempo      | http://localhost:3200               | Traces distribuídos             |
| Jaeger UI  | http://localhost:16686              | Visualização de traces          |

**Grafana — Datasources e Dashboards automáticos:**
Ao subir o Grafana, os datasources (Prometheus, Loki, Tempo) e 3 dashboards são provisionados automaticamente:

- **Business Metrics** — counters de negócio, logins, mídias, watchlist
- **Infrastructure & Performance** — JVM, conexões, latência HTTP
- **Logs** — busca de logs via Loki com correlação de traces

### 9.6. Testes de Observabilidade via .http

O arquivo `api-tests/observability.http` cobre todos os endpoints Actuator:

```
api-tests/observability.http  — health, metrics, loggers, caches, tracing headers
```

---

## 10. Rodando os Testes Automatizados (JUnit)

### 10.1. Testes Unitários (sem Docker)

```bash
# ~115 classes, ~629 métodos — NÃO precisa de Docker
./mvnw test -Dtest='!*IT,!*IntegrationTest,!*ConnectivityTest,!CinelogApplicationTest' \
  -Dspring.profiles.active=test
```

### 10.2. Testes de Integração (precisa Docker)

```bash
# Precisa de Docker rodando para Testcontainers (MySQL, Redis, Kafka, Keycloak)
./mvnw test -Dtest='*IT,*IntegrationTest,*ConnectivityTest' \
  -Dspring.profiles.active=test
```

### 10.3. Todos os testes + cobertura

```bash
./mvnw clean verify
# Relatório JaCoCo em: target/site/jacoco/index.html
```

Se VS Code Tasks estiverem configuradas:

```
Ctrl+Shift+P → Run Task → "Test (coverage) + Report"
```

### 10.4. Teste específico

```bash
# Exemplo: apenas testes de Auth
./mvnw test -Dtest='AuthServiceTest'

# Apenas testes de Media
./mvnw test -Dtest='*MediaService*'

# Apenas testes de segurança
./mvnw test -Dtest='RateLimitFilterTest,SecurityMethodAnnotationTest,JwtTokenServiceTest'
```

### 10.5. Testes de Arquitetura (ArchUnit)

```bash
./mvnw test -Dtest='LayeredArchitectureTest'
```

> Verifica que a arquitetura hexagonal é respeitada (core não depende de infra, etc.)

### 10.6. Inventário de Classes de Teste (117 arquivos, 115 com @Test, 629 métodos)

| Categoria                          | Classes | Exemplos                                                                                                |
| ---------------------------------- | ------- | ------------------------------------------------------------------------------------------------------- |
| Use cases (application/service)    | 45      | `CreateMediaServiceTest`, `DeleteWatchEntryServiceTest`                                                 |
| Domain (VOs, policies, spec)       | 15      | `RatingTest`, `WatchEntryTest`, `DefaultRatingPolicyTest`, `YearTest`, `TitleTest`                      |
| Web controllers                    | 8       | `MediaControllerTest`, `UserControllerTest`, `CreditControllerTest`                                     |
| Persistence adapters               | 10      | `MediaRepositoryAdapterTest`, `WatchEntryRepositoryIT`, `MediaGenreJdbcRepositoryTest`                  |
| Shared / Infrastructure / Security | 14      | `RateLimitFilterTest`, `JwtTokenServiceTest`, `ObservabilityContextFilterTest`, `HttpLoggingFilterTest` |
| Events / Messaging                 | 6       | `EventEnvelopeTest`, `WatchEntryCreatedConsumerTest`, `EventPayloadValidatorTest`                       |
| Integration (Testcontainers)       | 3       | `KeycloakOAuth2IntegrationTest`, `TestcontainersConnectivityTest`, `AbstractIntegrationTest`            |
| Architecture (ArchUnit)            | 1       | `LayeredArchitectureTest`                                                                               |
| Observers / Updaters               | 3       | `MediaPopularityUpdaterTest`, `UserStatsUpdaterTest`                                                    |
| Config / OpenAPI                   | 5       | `OpenApiConfigTest`, `OpenApiGroupsTest`, `UseCaseConfigTest`, `DataSourceProxyConfigTest`              |
| Aspects / Cross-cutting            | 3       | `UseCaseLoggingAspectTest`, `WebLoggingAspectTest`, `CorrelationHeaderFilterTest`                       |

---

## 11. Scripts de Teste Automatizado (Bash)

Além dos testes JUnit, o projeto inclui **scripts bash** que testam a API completa via `curl` + `jq`, com saída colorida e contadores de pass/fail.

### 11.1. `api-tests/run-all-tests.sh` — Teste Completo de Rotas

Testa **todas as rotas da API** com ~86 assertions automatizadas.

```bash
# Rodar (app deve estar de pé na porta 8080):
./api-tests/run-all-tests.sh

# Com URL customizada:
BASE_URL="http://localhost:8080" ./api-tests/run-all-tests.sh
```

**Seções cobertas (18 grupos, 86 testes):**

| #   | Seção                           | Testes |
| --- | ------------------------------- | ------ |
| 1   | Auth (register, login, refresh) | 6      |
| 2   | Genres CRUD                     | 5      |
| 3   | Media CRUD                      | 5      |
| 4   | Seasons CRUD                    | 5      |
| 5   | Episodes CRUD                   | 5      |
| 6   | People CRUD                     | 5      |
| 7   | Credits CRUD                    | 5      |
| 8   | Watch Entries CRUD              | 5      |
| 9   | Watchlist                       | 3      |
| 10  | Search & Discovery              | 6      |
| 11  | Recommendations & Popularity    | 6      |
| 12  | User Insights                   | 2      |
| 13  | Watch Progress                  | 3      |
| 14  | Reports                         | 6      |
| 15  | Admin (users, DLQ, batch)       | 8      |
| 16  | Observability (actuator)        | 5      |
| 17  | Security (401/403, RBAC)        | 4      |
| 18  | Cleanup                         | 2      |

**Resultado típico:** 79/86 pass, 0 fail, 7 skip (skips = infra opcional como Kafka/TMDb).

### 11.2. `api-tests/demo-security-senior.sh` — Demo de Segurança (Sprint Semanas 1 & 2)

Demo interativa para apresentação ao senior, cobrindo **segurança + IAM/OAuth2/SSO/MFA**.

```bash
# Modo automático (sem pausas):
AUTO_MODE=true ./api-tests/demo-security-senior.sh

# Modo interativo (pausa entre seções — ideal para demo ao vivo):
./api-tests/demo-security-senior.sh
```

> **Pré-requisito:** App rodando com profile `dev` (`-Dspring-boot.run.profiles=dev`) e Keycloak ativo na porta 8180.

**Seções cobertas (42 testes):**

| Semana | Seção | Tópico                                      |
| ------ | ----- | ------------------------------------------- |
| 1      | 1.1   | Autenticação JWT (HS384 local)              |
| 1      | 1.2   | Política de Senhas (validações de registro) |
| 1      | 1.3   | Refresh Token (rotação segura)              |
| 1      | 1.4   | RBAC — Role-Based Access Control            |
| 1      | 1.5   | Method-Level Security (@PreAuthorize)       |
| 1      | 1.6   | Endpoints Públicos vs Protegidos            |
| 1      | 1.7   | Logout / Revogação de Token                 |
| 2      | 2.1   | OpenID Connect Discovery                    |
| 2      | 2.2   | OAuth2 Token via Keycloak (RS256)           |
| 2      | 2.3   | Dual Auth (local HS384 + Keycloak RS256)    |
| 2      | 2.4   | Token Refresh via Keycloak                  |
| 2      | 2.5   | Introspection / Userinfo                    |
| 2      | 2.6   | MFA/TOTP (verificação de configuração)      |
| 2      | 2.7   | SSO (sessões compartilhadas, clients)       |
| 2      | 2.8   | Authorization Code + PKCE (Swagger OAuth2)  |
| 2      | 2.9   | Resumo da Arquitetura de Segurança          |

**Features do script:**

- Rate limit flush automático via `docker exec cinelog-redis redis-cli`
- Saída colorida com banners, seções e narração explicativa
- Decodificação de tokens JWT (header + payload) com exibição formatada
- Auto-detecção do Keycloak (se indisponível, pula Semana 2)
- Limpeza automática do usuário de teste no final

**Resultado esperado:** 42/42 pass.

### 11.3. Arquivos `.http` (REST Client / VS Code)

11 arquivos `.http` para teste manual via REST Client (VS Code):

| Arquivo              | Escopo                                         |
| -------------------- | ---------------------------------------------- |
| `auth.http`          | Register, login, refresh, logout               |
| `media.http`         | CRUD de filmes/séries                          |
| `catalog.http`       | Genres, seasons, episodes, people, credits     |
| `discovery.http`     | Search, top-rated, trending, most-watched      |
| `users.http`         | Users CRUD, stats, insights, recommendations   |
| `activity.http`      | Watch entries, watchlist, watch progress       |
| `reports.http`       | Relatórios (preview + envio email)             |
| `admin.http`         | Admin endpoints (users, media, DLQ)            |
| `batch.http`         | Batch jobs (TMDb sync)                         |
| `health.http`        | Health check, actuator info                    |
| `observability.http` | Métricas, prometheus, loggers, caches, tracing |

Configuração local:

```bash
cp api-tests/rest-client.env.json.example api-tests/rest-client.env.json
# Edite rest-client.env.json com seus tokens — está no .gitignore
```

---

## 12. Referência Rápida — Todos os Endpoints

### Públicos (sem autenticação)

| Método | Path                 | Descrição         |
| ------ | -------------------- | ----------------- |
| POST   | `/api/auth/register` | Criar conta       |
| POST   | `/api/auth/login`    | Login (JWT local) |
| POST   | `/api/auth/refresh`  | Renovar token     |
| GET    | `/actuator/health`   | Health check      |
| GET    | `/actuator/info`     | Info da aplicação |
| GET    | `/swagger-ui/**`     | Swagger UI        |
| GET    | `/v3/api-docs/**`    | OpenAPI JSON      |

### Autenticados (qualquer role)

| Método | Path                              | Descrição                 |
| ------ | --------------------------------- | ------------------------- |
| POST   | `/api/auth/logout`                | Logout (revogar tokens)   |
| CRUD   | `/api/v1/media/**`                | Filmes e Séries           |
| CRUD   | `/api/v1/genres/**`               | Gêneros                   |
| CRUD   | `/api/v1/seasons/**`              | Temporadas                |
| CRUD   | `/api/v1/episodes/**`             | Episódios                 |
| CRUD   | `/api/v1/people/**`               | Pessoas                   |
| CRUD   | `/api/v1/credits/**`              | Créditos                  |
| CRUD   | `/api/v1/watch-entries/**`        | Registros de visualização |
| CRUD   | `/api/v1/watchlist/**`            | Lista de desejos          |
| GET    | `/api/v1/users/me/stats`          | Suas estatísticas         |
| GET    | `/api/media/search`               | Busca avançada            |
| GET    | `/api/media/search/text`          | Busca por texto           |
| GET    | `/api/media/top-rated`            | Mais bem avaliados        |
| GET    | `/api/media/trending`             | Em alta                   |
| GET    | `/api/media/most-watched`         | Mais assistidos           |
| GET    | `/api/users/{id}/recommendations` | Recomendações             |
| GET    | `/api/users/{id}/insights`        | Insights do usuário       |
| CRUD   | `/api/watchentries/{id}/progress` | Progresso                 |

### ADMIN only

| Método | Path                                | Descrição                              |
| ------ | ----------------------------------- | -------------------------------------- |
| GET    | `/api/v1/users`                     | Listar todos os usuários               |
| DELETE | `/api/v1/users/{id}`                | Deletar usuário                        |
| POST   | `/api/v1/admin/media`               | Criar mídia (admin)                    |
| GET    | `/api/v1/admin/reports/platform`    | Relatório da plataforma                |
| POST   | `/api/v1/admin/reports/platform`    | Enviar relatório da plataforma         |
| POST   | `/api/v1/admin/reports/send-to-all` | Disparar digest para todos os usuários |
| POST   | `/api/v1/admin/batch/genres`        | Importar gêneros do TMDb               |
| POST   | `/api/v1/admin/batch/movies`        | Importar filmes do TMDb                |
| POST   | `/api/v1/admin/batch/tv-shows`      | Importar séries do TMDb                |
| POST   | `/api/v1/admin/batch/credits`       | Importar créditos do TMDb              |
| POST   | `/api/v1/admin/batch/seasons`       | Importar temporadas do TMDb            |

### ADMIN ou OPS

| Método | Path                     | Descrição           |
| ------ | ------------------------ | ------------------- |
| GET    | `/admin/dlq`             | Dead Letter Queue   |
| POST   | `/admin/dlq/{id}/replay` | Re-processar evento |
| GET    | `/admin/dlq/stats`       | Estatísticas DLQ    |

---

## 13. Problemas Conhecidos & Notas

### ⚠️ Itens a observar na demonstração

| #   | Item                                                                                             | Impacto | Nota                                               |
| --- | ------------------------------------------------------------------------------------------------ | ------- | -------------------------------------------------- |
| 1   | Alguns endpoints não seguem `/api/v1/` (search, popularity, insights, progress, recommendations) | Baixo   | Funcional, mas inconsistência de versionamento     |
| 2   | `AdminMediaController.create` é um stub                                                          | Baixo   | Retorna dados mas não persiste                     |
| 3   | Endpoints de update em Genre/Episode/Season/Credit/Person não exigem role específica             | Médio   | Qualquer autenticado pode atualizar                |
| 4   | Recomendações retornam scores aleatórios                                                         | Baixo   | Implementação placeholder                          |
| 5   | SAML2 está apenas em design — classe `SamlIntegrationPreparation.java` documenta o plano         | Info    | Intencional (ADR-IAM-001)                          |
| 6   | `directAccessGrantsEnabled: true` no Keycloak — usado pelos testes mas deprecado no OAuth 2.1    | Baixo   | Apenas em ambiente dev/test                        |
| 7   | Rate limit de auth (10/min) pode afetar scripts se rodados em sequência rápida                   | Médio   | Scripts já fazem flush via `docker exec redis-cli` |
| 8   | JWT stateless: access token permanece válido após logout até expirar (refresh é revogado)        | Info    | Comportamento esperado de JWT                      |

### ✅ Pontos fortes para destacar

- **Arquitetura Hexagonal** validada por ArchUnit (`LayeredArchitectureTest`)
- **Dual Auth**: JWT local (HS384) + OAuth2 Keycloak (RS256) coexistem no mesmo SecurityFilterChain com `BearerTokenResolver` customizado
- **MFA** via TOTP configurado no Keycloak realm (usuários `marcus` e `alice-mfa`)
- **117 arquivos de teste (629 métodos @Test)**, cobrindo unitários, integração e arquitetura
- **Scripts de teste bash**: `run-all-tests.sh` (86 testes, todas as rotas) + `demo-security-senior.sh` (42 testes, demo de segurança)
- **Testcontainers** (MySQL, Kafka, Redis, Keycloak) para testes de integração reprodutíveis
- **Observabilidade completa**: health checks customizados (TMDb, Outbox), métricas Micrometer, tracing OpenTelemetry, logs JSON estruturados com MDC (userId, traceId, tookMs), Grafana auto-provisionado (Prometheus + Loki + Tempo)
- **Reports & Email**: 5 tipos de relatório com templates dark/cinema HTML, envio via MailHog em dev
- **Spring Batch**: 5 jobs para sincronização com TMDb
- **Segurança OWASP**: rate limiting (Fixed Window Redis), SQL injection filter, CORS configurável, account lockout, refresh token rotation, password policy, input sanitization, tamper-proof validation
- **Event-Driven**: Outbox pattern com Kafka, Dead Letter Queue, idempotent consumer (inbox)
- **Design Patterns**: Strategy (recomendações), State (watch entry status), Specification (popular media), Observer (domain events), Template Method (base consumer)

---

## 14. Testes de E-mail & Relatórios

### Pré-requisito: `TEST_EMAIL` via variável de ambiente

> ⚠️ **Nunca commite endereços de e-mail reais no repositório.**  
> Use a variável de ambiente `TEST_EMAIL` ou a configuração local do REST Client — ambas ficam fora do controle de versão.

#### REST Client (VS Code)

1. Copie o template de configuração:

    ```bash
    cp api-tests/rest-client.env.json.example api-tests/rest-client.env.json
    # rest-client.env.json está no .gitignore — edite localmente
    ```

2. Preencha o arquivo copiado:

    ```json
    {
        "test": {
            "testEmail": "seu.real@email.com"
        }
    }
    ```

3. No arquivo `api-tests/reports.http`, a variável `{{testEmail}}` será substituída automaticamente.

#### Script bash

```bash
# Passa o e-mail via variável de ambiente (nunca hardcode no script):
TEST_EMAIL="seu.real@email.com" ./scripts/run-full-tests.sh

# Sem TEST_EMAIL → usa fallback test@mailhog.local (capturado pelo MailHog)
./scripts/run-full-tests.sh
```

### Endpoints de Relatórios

| Tipo                | Preview (GET)                         | Envio (POST — 202)                       |
| ------------------- | ------------------------------------- | ---------------------------------------- |
| Weekly Digest       | `GET /api/v1/reports/weekly-digest`   | `POST /api/v1/reports/weekly-digest`     |
| Top Rated           | `GET /api/v1/reports/top-rated`       | `POST /api/v1/reports/top-rated`         |
| Recommendations     | `GET /api/v1/reports/recommendations` | `POST /api/v1/reports/recommendations`   |
| Trending            | `GET /api/v1/reports/trending`        | `POST /api/v1/reports/trending`          |
| Platform (ADMIN)    | `GET /api/v1/admin/reports/platform`  | `POST /api/v1/admin/reports/platform`    |
| Send-to-All (ADMIN) | —                                     | `POST /api/v1/admin/reports/send-to-all` |

### Payload para POST (todos os tipos)

```json
{
    "email": "{{testEmail}}", // omita para usar o e-mail do usuário autenticado
    "limit": 10 // opcional; obrigatório para top-rated
}
```

### MailHog — Verificar e-mails capturados

Após enviar qualquer POST de relatório em dev:

```
http://localhost:8025   →  MailHog Web UI
```

### Validações de erro esperadas

| Cenário                     | Payload                               | HTTP esperado |
| --------------------------- | ------------------------------------- | ------------- |
| `limit: 0`                  | `{"limit": 0}`                        | **400**       |
| e-mail com formato inválido | `{"email": "nao-eh-email"}`           | **400**       |
| POST admin sem role ADMIN   | usar `@token` em vez de `@adminToken` | **403**       |

---

## 15. Observabilidade & Métricas de Negócio

Use o arquivo `api-tests/observability.http` para testar todos os endpoints abaixo.

### Actuator — Saúde & Informações

| Endpoint                         | Acesso  | Descrição                             |
| -------------------------------- | ------- | ------------------------------------- |
| `GET /actuator/health`           | Público | Status geral (db, redis, kafka, disk) |
| `GET /actuator/health/liveness`  | Público | Liveness probe K8s                    |
| `GET /actuator/health/readiness` | Público | Readiness probe K8s                   |
| `GET /actuator/info`             | Público | Versão, git sha, Java version         |

### Actuator — Métricas (requer ADMIN)

| Endpoint                        | Descrição                   |
| ------------------------------- | --------------------------- |
| `GET /actuator/metrics`         | Lista todas as métricas     |
| `GET /actuator/metrics/{name}`  | Valor e tags de uma métrica |
| `GET /actuator/prometheus`      | Scrape format (text/plain)  |
| `GET /actuator/caches`          | Estado das caches           |
| `DELETE /actuator/caches`       | Invalida todas as caches    |
| `GET /actuator/loggers/{name}`  | Nível de log de um logger   |
| `POST /actuator/loggers/{name}` | Muda nível em runtime (204) |

### Métricas de Segurança (`cinelog.security.*`)

Consulte via `GET /actuator/metrics/{metric_name}`. Use `?tag=reason:VALUE` para filtrar por tag.

| Métrica                                   | Tags disponíveis                                      |
| ----------------------------------------- | ----------------------------------------------------- |
| `cinelog.security.auth_failures_total`    | `reason`: `invalid_credentials`, `account_locked`     |
| `cinelog.security.account_lockouts_total` | —                                                     |
| `cinelog.security.jwt_failures_total`     | `reason`: `expired`, `invalid_signature`, `malformed` |
| `cinelog.security.rate_limit_total`       | `path_class`: `auth`, `api`                           |
| `cinelog.security.sqli_attempts_total`    | —                                                     |
| `cinelog.security.access_denied_total`    | —                                                     |
| `cinelog.security.tamper_detected_total`  | `type`: `header`, `payload`                           |
| `cinelog.security.sensitive_access_total` | `resource`: `users`, `admin`                          |

**Como acionar falhas e verificar contadores:**

```bash
# 1. Faça login com credenciais erradas → incrementa auth_failures_total
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"x","password":"wrong"}'

# 2. Verifique o counter
curl -s http://localhost:8080/actuator/metrics/cinelog.security.auth_failures_total \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.measurements[0].value'

# 3. Consulte com filtro de tag
curl -s "http://localhost:8080/actuator/metrics/cinelog.security.auth_failures_total?tag=reason:invalid_credentials" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.measurements[0].value'
```

### Métricas de Negócio (`cinelog.business.*`)

| Métrica                                            | Quando incrementa                 |
| -------------------------------------------------- | --------------------------------- |
| `cinelog.business.watch_entries_created_total`     | Novo watch entry criado           |
| `cinelog.business.reports_sent_total`              | POST /reports/\* retorna 202      |
| `cinelog.business.recommendations_generated_total` | Endpoint de recomendações chamado |
| `cinelog.business.searches_total`                  | Busca realizada                   |

**Fluxo para validar métricas de negócio:**

```bash
# 1. Antes: leia o valor atual
BEFORE=$(curl -s http://localhost:8080/actuator/metrics/cinelog.business.watch_entries_created_total \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.measurements[0].value')

# 2. Crie um watch entry
curl -X POST http://localhost:8080/api/watchentries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mediaId":"<ID>","rating":9,"review":"Ótimo!"}'

# 3. Depois: leia novamente — deve ser BEFORE + 1
AFTER=$(curl -s http://localhost:8080/actuator/metrics/cinelog.business.watch_entries_created_total \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.measurements[0].value')
echo "Delta: $(echo "$AFTER - $BEFORE" | bc)"
```

### Rate-Limit — Validar bloqueio e contador

O arquivo `api-tests/observability.http` contém um bloco de **11 requests** (`rateLimitTrigger01` … `rateLimitTrigger11`) para disparar o rate-limit:

1. Execute os blocos em sequência rápida no REST Client (ou use o script abaixo).
2. A partir do 11º request, espera-se **HTTP 429 Too Many Requests**.
3. Verifique que `cinelog.security.rate_limit_total` incrementou.

```bash
# Disparo rápido via shell:
for i in $(seq 1 12); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"ratelimituser","password":"wrong"}')
  echo "Attempt $i: $STATUS"
  [[ "$STATUS" == "429" ]] && echo "Rate-limit acionado!" && break
done
```

### Log Level em Runtime

```bash
# Eleva o logger de segurança para DEBUG sem reiniciar:
curl -X POST http://localhost:8080/actuator/loggers/com.cinelog.infrastructure.security \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# Retorna para INFO:
curl -X POST http://localhost:8080/actuator/loggers/com.cinelog.infrastructure.security \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "INFO"}'
```

---

## 16. Arquitetura Dual Auth (JWT Local + Keycloak OAuth2)

O CineLog implementa **dois mecanismos de autenticação coexistentes** no mesmo `SecurityFilterChain`. Isso é possível graças a uma combinação de filtros customizados e um `BearerTokenResolver` seletivo.

### Como funciona

```
Request com "Authorization: Bearer <token>"
        │
        ▼
┌─ JwtAuthenticationFilter ─────────────────────────┐
│  1. Extrai o token do header                      │
│  2. Base64-decode do payload                      │
│  3. Verifica campo "iss" (issuer)                 │
│                                                    │
│  Se iss == keycloakIssuerUri:                     │
│    → Skip (delega para BearerTokenAuthFilter)     │
│                                                    │
│  Se iss == null ou diferente:                     │
│    → Valida como JWT local (HS384, JJWT)         │
│    → Extrai subject (userId)                      │
│    → Carrega UserDetails do banco                 │
│    → Seta SecurityContext                         │
└───────────────────────────────────────────────────┘
        │
        ▼
┌─ BearerTokenAuthenticationFilter (OAuth2 RS) ─────┐
│  keycloakOnlyBearerTokenResolver():               │
│    → Só resolve tokens com iss == keycloakIssuer  │
│    → Retorna null para tokens locais              │
│                                                    │
│  Se token resolvido:                              │
│    → Valida via JWKS (RS256)                      │
│    → Seta SecurityContext com claims Keycloak     │
│                                                    │
│  Se null:                                         │
│    → Skip (SecurityContext já foi setado acima)   │
└───────────────────────────────────────────────────┘
```

### Configuração

O OAuth2 Resource Server só é ativado com o profile `dev`:

```yaml
# application-dev.yml
spring:
    security:
        oauth2:
            resourceserver:
                jwt:
                    issuer-uri: http://localhost:8180/realms/cinelog
```

Sem o profile `dev`, apenas a autenticação JWT local (HS384) está ativa.

### Classes envolvidas

| Classe                            | Responsabilidade                                          |
| --------------------------------- | --------------------------------------------------------- |
| `JwtAuthenticationFilter`         | Detecta tipo de token (local vs KC) pelo campo `iss`      |
| `SecurityConfig`                  | Configura `keycloakOnlyBearerTokenResolver()` customizado |
| `JwtTokenService`                 | Gera e valida tokens locais HS384 (JJWT)                  |
| `BearerTokenAuthenticationFilter` | Spring OAuth2 RS — valida tokens Keycloak via JWKS RS256  |

### Keycloak Clients

| Client            | Tipo         | Uso                               | PKCE |
| ----------------- | ------------ | --------------------------------- | ---- |
| `cinelog-app`     | Público      | Swagger UI, frontend              | Sim  |
| `cinelog-backend` | Confidencial | Machine-to-machine, introspection | Não  |

### Testando a coexistência

```bash
# Subir com profile dev:
./mvnw spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev

# Token local (HS384):
LOCAL_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cinelog.dev","password":"Admin@CineLog2025!"}' | jq -r '.accessToken')

# Token Keycloak (RS256):
KC_TOKEN=$(curl -s -X POST http://localhost:8180/realms/cinelog/protocol/openid-connect/token \
  -d "grant_type=password&client_id=cinelog-app&username=alice&password=Alice@CineLog2025!" \
  | jq -r '.access_token')

# Ambos devem funcionar no mesmo endpoint:
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $LOCAL_TOKEN" http://localhost:8080/api/v1/media
# → 200 ou 404

curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $KC_TOKEN" http://localhost:8080/api/v1/media
# → 200 ou 404

# Sem token → 401:
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/media
# → 401
```

> **Dica:** Use `AUTO_MODE=true ./api-tests/demo-security-senior.sh` para uma demonstração automatizada completa da dual auth (seção 2.3 do script).
