# 🎬 CineLog — Guia Completo de Testes e Validação

> **Objetivo:** Este documento é um roteiro passo-a-passo para demonstrar **todas** as funcionalidades do CineLog a um avaliador sênior, cobrindo: subir a infra local, registrar/autenticar, testar cada CRUD via Swagger, validar segurança, observabilidade e rodar os testes automatizados.

---

## Sumário

1. [Pré-requisitos](#1-pré-requisitos)
2. [Subindo a Infraestrutura (Docker Compose)](#2-subindo-a-infraestrutura)
3. [Subindo a Aplicação](#3-subindo-a-aplicação)
4. [Acesso ao Swagger UI](#4-acesso-ao-swagger-ui)
5. [Autenticação Local (JWT)](#5-autenticação-local-jwt)
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
10. [Rodando os Testes Automatizados](#10-rodando-os-testes-automatizados)
11. [Referência Rápida — Todos os Endpoints](#11-referência-rápida--todos-os-endpoints)
12. [Problemas Conhecidos & Notas](#12-problemas-conhecidos--notas)

---

## 1. Pré-requisitos

| Ferramenta              | Versão mínima                | Verificar                                            |
| ----------------------- | ---------------------------- | ---------------------------------------------------- |
| **Java**                | 21                           | `java -version`                                      |
| **Docker**              | 24+                          | `docker --version`                                   |
| **Docker Compose**      | v2+                          | `docker compose version`                             |
| **Maven** (via wrapper) | 3.9+                         | `./mvnw --version`                                   |
| **Portas livres**       | 8080, 8180, 3306, 6379, 9092 | `ss -tlnp \| grep -E '8080\|8180\|3306\|6379\|9092'` |

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

> A senha deve ter: ≥ 12 chars, 1 maiúscula, 1 minúscula, 1 dígito, 1 caractere especial.

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

| Usuário     | Senha                   | Roles                        |
| ----------- | ----------------------- | ---------------------------- |
| `alice`     | `Alice@CineLog2025!`    | USER                         |
| `admin`     | `Admin@CineLog2025!`    | USER, ADMIN                  |
| `alice-mfa` | `AliceMfa@CineLog2025!` | USER (pedirá TOTP na 1ª vez) |

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

1. Faça login como `alice-mfa` no Keycloak
2. Na **primeira vez**, Keycloak pedirá para configurar o TOTP:
    - Escaneie o QR code com Google Authenticator / FreeOTP / Microsoft Authenticator
    - Insira o código gerado
3. Nos logins seguintes, será sempre pedido o código TOTP

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

### 7.17. Reports & Email

> Requer autenticação. Endpoints `/admin/reports/*` requerem role **ADMIN**.
> Os emails são enviados via MailHog (http://localhost:8025) em ambiente de desenvolvimento.

#### User Reports (autenticado)

| Ação                       | Endpoint                               | Descrição                           |
| -------------------------- | -------------------------------------- | ----------------------------------- |
| **Ver digest semanal**     | `GET /api/v1/reports/weekly-digest`    | HTML/JSON do resumo semanal         |
| **Enviar digest**          | `POST /api/v1/reports/weekly-digest`   | Envio imediato por email            |
| **Ver top avaliados**      | `GET /api/v1/reports/top-rated`        | Relatório de mídias mais avaliadas  |
| **Enviar top avaliados**   | `POST /api/v1/reports/top-rated`       | Envio imediato por email            |
| **Ver recomendações**      | `GET /api/v1/reports/recommendations`  | Relatório de recomendações          |
| **Enviar recomendações**   | `POST /api/v1/reports/recommendations` | Envio imediato por email            |
| **Ver trending**           | `GET /api/v1/reports/trending`         | Relatório de trending               |
| **Enviar trending**        | `POST /api/v1/reports/trending`        | Envio imediato por email            |

#### Admin Reports

| Ação                          | Endpoint                                 | Descrição                        |
| ----------------------------- | ---------------------------------------- | -------------------------------- |
| **Ver platform report**       | `GET /api/v1/admin/reports/platform`     | Relatório geral da plataforma    |
| **Enviar platform report**    | `POST /api/v1/admin/reports/platform`    | Enviar por email (admin)         |
| **Disparar envios em massa**  | `POST /api/v1/admin/reports/send-to-all` | Enviar digest para todos os usuários ativos |

**Como testar o envio de email:**

1. Execute `POST /api/v1/reports/weekly-digest` no Swagger
2. Abra http://localhost:8025 (MailHog)
3. O email com o template dark/cinema deve aparecer na caixa de entrada
4. Inspecione o HTML para ver o template renderizado

**Templates disponíveis:** `weekly-digest`, `top-rated`, `recommendations`, `trending`, `platform-report`

---

### 7.18. Admin — Batch Jobs (TMDb Sync)

> **Requer role ADMIN.** Dispara jobs Spring Batch para popular o banco com dados do TMDb.

| Ação                     | Endpoint                            | Parâmetros             |
| ------------------------ | ----------------------------------- | ---------------------- |
| **Importar gêneros**     | `POST /api/v1/admin/batch/genres`   | —                      |
| **Importar filmes**      | `POST /api/v1/admin/batch/movies`   | `maxPages` (default 5) |
| **Importar séries**      | `POST /api/v1/admin/batch/tv-shows` | `maxPages`             |
| **Importar créditos**    | `POST /api/v1/admin/batch/credits`  | —                      |
| **Importar temporadas**  | `POST /api/v1/admin/batch/seasons`  | —                      |

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

### 8.1. Testes de autenticação

| Teste                                    | Como fazer                                             | Resultado esperado |
| ---------------------------------------- | ------------------------------------------------------ | ------------------ |
| **Sem token**                            | `curl http://localhost:8080/api/v1/media`              | `401 Unauthorized` |
| **Token expirado**                       | Esperar expiração (1h) ou gerar token com data passada | `401`              |
| **Token inválido**                       | `curl -H "Authorization: Bearer abc123" ...`           | `401`              |
| **Token Keycloak em endpoint protegido** | Obter token keycloak e acessar `/api/v1/media`         | `200`              |
| **Token local em endpoint protegido**    | Login local e acessar `/api/v1/media`                  | `200`              |

### 8.2. Testes de autorização (RBAC)

| Teste             | Usuário       | Endpoint                    | Esperado |
| ----------------- | ------------- | --------------------------- | -------- |
| USER acessa media | alice (USER)  | `GET /api/v1/media`         | `200`    |
| USER cria media   | alice (USER)  | `POST /api/v1/media`        | `201`    |
| USER lista users  | alice (USER)  | `GET /api/v1/users`         | `403`    |
| USER deleta user  | alice (USER)  | `DELETE /api/v1/users/{id}` | `403`    |
| ADMIN lista users | admin (ADMIN) | `GET /api/v1/users`         | `200`    |
| ADMIN acessa DLQ  | admin (ADMIN) | `GET /admin/dlq`            | `200`    |
| USER acessa DLQ   | alice (USER)  | `GET /admin/dlq`            | `403`    |

### 8.3. Account Lockout

1. Tente login com senha errada **5 vezes consecutivas**
2. Na 6ª tentativa → `423 Locked` (conta bloqueada por 15 min)
3. Aguarde 15 min ou reinicie a aplicação

### 8.4. Password Policy (Registro)

| Senha             | Resultado                                              |
| ----------------- | ------------------------------------------------------ |
| `123`             | `400` — muito curta                                    |
| `abcdefghijklm`   | `400` — sem maiúsculas, dígitos, especiais             |
| `StrongP@ss2025!` | `201` — válida (≥12, maiúsc, minúsc, dígito, especial) |

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

| Endpoint                     | URL                                      | Descrição                        |
| ---------------------------- | ---------------------------------------- | -------------------------------- |
| Health                       | `/actuator/health`                       | Status da aplicação e dependências |
| Info                         | `/actuator/info`                         | Versão e nome da aplicação       |
| Prometheus (scrape)          | `/actuator/prometheus`                   | Todas as métricas em formato Prometheus |
| Métricas JSON                | `/actuator/metrics`                      | Lista de meters disponíveis      |
| Loggers                      | `/actuator/loggers`                      | Consulta e altera log levels em runtime |
| Caches                       | `/actuator/caches`                       | Gerencia caches Redis            |

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

| Nome da métrica                          | Tipo    | Descrição                         |
| ---------------------------------------- | ------- | --------------------------------- |
| `cinelog_business_auth_login_total`      | Counter | Logins (tag: `success`)           |
| `cinelog_business_user_registered_total` | Counter | Usuários registrados              |
| `cinelog_business_media_created_total`   | Counter | Mídias criadas (tag: `type`)      |
| `cinelog_business_watchentry_created_total` | Counter | Watch entries criadas          |
| `cinelog_business_watchlist_added_total` | Counter | Adições à watchlist               |
| `cinelog_business_rating_given_total`    | Counter | Ratings registrados               |
| `cinelog_integration_tmdb_calls_total`   | Counter | Chamadas à API TMDb               |
| `cinelog_integration_duration`           | Timer   | Latência de integrações externas  |
| `cinelog_batch_job_executed_total`       | Counter | Jobs Batch executados             |
| `cinelog_method_execution`              | Timer   | Tempo de execução (MetricsAspect) |

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

| Serviço    | URL                                 | Descrição                              |
| ---------- | ----------------------------------- | -------------------------------------- |
| Grafana    | http://localhost:3000 (admin/admin) | Dashboards (auto-provisionados)        |
| Prometheus | http://localhost:9090               | Dados de métricas                      |
| Loki       | http://localhost:3100               | Logs centralizados                     |
| Tempo      | http://localhost:3200               | Traces distribuídos                    |
| Jaeger UI  | http://localhost:16686              | Visualização de traces                 |

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

## 10. Rodando os Testes Automatizados

### 10.1. Testes Unitários (sem Docker)

```bash
# ~110 classes — NÃO precisa de Docker
./mvnw test -Dtest='!*IT,!*IntegrationTest,!*ConnectivityTest,!CinelogApplicationTest' \
  -Dspring.profiles.active=test
```

### 10.2. Testes de Integração (precisa Docker)

```bash
# Precisa de Docker rodando para Testcontainers
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
```

### 10.5. Testes de Arquitetura (ArchUnit)

```bash
./mvnw test -Dtest='LayeredArchitectureTest'
```

> Verifica que a arquitetura hexagonal é respeitada (core não depende de infra, etc.)

### 10.6. Inventário de classes de teste (113 total)

| Categoria                    | Classes | Exemplos                                             |
| ---------------------------- | ------- | ---------------------------------------------------- |
| Use cases (application)      | ~50     | `CreateMediaServiceTest`, `CreateWatchEntryServiceTest` |
| Domain (VOs, policies, spec) | ~10     | `RatingTest`, `WatchEntryTest`, `DefaultRatingPolicyTest` |
| Web controllers              | ~10     | `MediaControllerTest`, `UserControllerTest`          |
| Persistence adapters         | ~10     | `MediaRepositoryAdapterTest`, `WatchEntryRepositoryIT` |
| Infrastructure               | ~8      | `EventEnvelopeTest`, `BaseKafkaConsumerTest`, `RetryStrategyTest` |
| Observability / Security     | ~8      | `ObservabilityContextFilterTest`, `JwtTokenServiceTest` |
| Integration (Testcontainers) | 3       | `KeycloakOAuth2IntegrationTest`, `TestcontainersConnectivityTest` |
| Architecture (ArchUnit)      | 1       | `LayeredArchitectureTest` |
| Shared / Config              | ~10     | `GlobalExceptionHandlerTest`, `OpenApiConfigTest`    |

---

## 11. Referência Rápida — Todos os Endpoints

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

| Método | Path                                    | Descrição                             |
| ------ | --------------------------------------- | ------------------------------------- |
| GET    | `/api/v1/users`                         | Listar todos os usuários              |
| DELETE | `/api/v1/users/{id}`                    | Deletar usuário                       |
| POST   | `/api/v1/admin/media`                   | Criar mídia (admin)                   |
| GET    | `/api/v1/admin/reports/platform`        | Relatório da plataforma               |
| POST   | `/api/v1/admin/reports/platform`        | Enviar relatório da plataforma        |
| POST   | `/api/v1/admin/reports/send-to-all`     | Disparar digest para todos os usuários |
| POST   | `/api/v1/admin/batch/genres`            | Importar gêneros do TMDb              |
| POST   | `/api/v1/admin/batch/movies`            | Importar filmes do TMDb               |
| POST   | `/api/v1/admin/batch/tv-shows`          | Importar séries do TMDb               |
| POST   | `/api/v1/admin/batch/credits`           | Importar créditos do TMDb             |
| POST   | `/api/v1/admin/batch/seasons`           | Importar temporadas do TMDb           |

### ADMIN ou OPS

| Método | Path                     | Descrição           |
| ------ | ------------------------ | ------------------- |
| GET    | `/admin/dlq`             | Dead Letter Queue   |
| POST   | `/admin/dlq/{id}/replay` | Re-processar evento |
| GET    | `/admin/dlq/stats`       | Estatísticas DLQ    |

---

## 12. Problemas Conhecidos & Notas

### ⚠️ Itens a observar na demonstração

| #   | Item                                                                                             | Impacto | Nota                                           |
| --- | ------------------------------------------------------------------------------------------------ | ------- | ---------------------------------------------- |
| 1   | Alguns endpoints não seguem `/api/v1/` (search, popularity, insights, progress, recommendations) | Baixo   | Funcional, mas inconsistência de versionamento |
| 2   | `AdminMediaController.create` é um stub                                                          | Baixo   | Retorna dados mas não persiste                 |
| 3   | Endpoints de update em Genre/Episode/Season/Credit/Person não exigem `@SecureOperation`          | Médio   | Qualquer autenticado pode atualizar            |
| 4   | Recomendações retornam scores aleatórios                                                         | Baixo   | Implementação placeholder                      |
| 5   | SAML2 está apenas em design — classe `SamlIntegrationPreparation.java` documenta o plano         | Info    | Intencional (ADR-IAM-001)                      |
| 6   | `directAccessGrantsEnabled: true` no Keycloak — usado pelos testes mas deprecado no OAuth 2.1    | Baixo   | Apenas em ambiente dev/test                    |

### ✅ Pontos fortes para destacar

- **Arquitetura Hexagonal** validada por ArchUnit (`LayeredArchitectureTest`)
- **Dual Auth**: JWT local + OAuth2 Keycloak coexistem no mesmo SecurityFilterChain
- **MFA** via TOTP configurado no Keycloak realm
- **113 classes de teste**, cobrindo unitários, integração e arquitetura
- **Testcontainers** (MySQL, Kafka, Redis, Keycloak) para testes de integração reprodutíveis
- **Observabilidade completa**: health checks customizados (TMDb, Outbox), métricas Micrometer, tracing OpenTelemetry, logs JSON estruturados com MDC enriquecido (userId, traceId, tookMs), Grafana auto-provisionado (Prometheus + Loki + Tempo)
- **Reports & Email**: 5 tipos de relatório com templates dark/cinema HTML, envio via MailHog em dev
- **Spring Batch**: 5 jobs para sincronização com TMDb (319+ mídias, 126490+ episódios importados)
- **Segurança OWASP**: rate limiting, SQL injection filter, CORS configurável, account lockout, refresh token rotation, password policy, input sanitization, tamper-proof validation
- **Event-Driven**: Outbox pattern com Kafka, Dead Letter Queue, idempotent consumer (inbox)
- **Design Patterns**: Strategy (recomendações), State (watch entry status), Specification (popular media), Observer (domain events), Template Method (base consumer)
