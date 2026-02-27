# 📡 API Reference

> Documentação completa da API REST do CineLog. Base URL: `/api/v1`

---

## Autenticação

Todas as rotas (exceto registro e login) requerem um **Bearer Token JWT** no header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Limites de taxa (Rate Limiting)

| Contexto | Limite |
|---|---|
| Autenticado | 100 requests/min |
| Não autenticado | 20 requests/min |
| Login (`/auth/login`) | 5 tentativas/min |

---

## Endpoints

### Auth

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Registrar novo usuário | ❌ |
| `POST` | `/api/v1/auth/login` | Login (retorna JWT + refreshToken) | ❌ |
| `POST` | `/api/v1/auth/refresh` | Renovar token (rotation) | ❌ |

<details>
<summary><strong>POST /api/v1/auth/register</strong></summary>

**Request Body:**
```json
{
  "name": "Marcus Prado",
  "email": "marcus@cinelog.com",
  "password": "Senh@Forte123!"
}
```

**Response (201):**
```json
{
  "id": 1,
  "name": "Marcus Prado",
  "email": "marcus@cinelog.com",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

**Regras de senha (A07:2025):**
- Mínimo 8 caracteres
- Pelo menos 1 maiúscula, 1 minúscula, 1 número, 1 especial
- Não pode ser senha comum (dicionário de 10.000+ senhas)
</details>

<details>
<summary><strong>POST /api/v1/auth/login</strong></summary>

**Request Body:**
```json
{
  "email": "marcus@cinelog.com",
  "password": "Senh@Forte123!"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Account Lockout**: 5 tentativas → conta bloqueada por 15 min (423 Locked).
</details>

---

### Media

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/v1/media` | Listar mídias (paginado) | ✅ |
| `GET` | `/api/v1/media/{id}` | Buscar mídia por ID | ✅ |
| `POST` | `/api/v1/media` | Criar mídia | ✅ |
| `PUT` | `/api/v1/media/{id}` | Atualizar mídia | ✅ |
| `DELETE` | `/api/v1/media/{id}` | Remover mídia | ✅ |

<details>
<summary><strong>GET /api/v1/media</strong> — Listar com filtros</summary>

**Query Parameters:**

| Parâmetro | Tipo | Default | Descrição |
|---|---|---|---|
| `page` | int | 0 | Página (zero-based) |
| `size` | int | 20 | Items por página |
| `sort` | string | `createdAt,desc` | Campo e direção de ordenação |
| `type` | string | — | Filtro por tipo: `MOVIE`, `SERIES` |
| `title` | string | — | Busca por título (LIKE) |

**Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Inception",
      "type": "MOVIE",
      "releaseYear": 2010,
      "tmdbId": 27205,
      "posterUrl": "https://image.tmdb.org/t/p/w500/...",
      "voteAverage": 8.4,
      "createdAt": "2025-01-15T10:30:00Z"
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
}
```
</details>

<details>
<summary><strong>POST /api/v1/media</strong> — Criar mídia</summary>

**Request Body:**
```json
{
  "title": "Inception",
  "type": "MOVIE",
  "releaseYear": 2010,
  "overview": "A thief who steals corporate secrets...",
  "tmdbId": 27205
}
```

**Validações:**
- `title`: obrigatório, máx 255 caracteres
- `type`: obrigatório, enum `MOVIE` ou `SERIES`
- `releaseYear`: entre 1888 e ano atual +1
- `tmdbId`: opcional, integra automaticamente com TMDb
</details>

---

### Genres

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/v1/genres` | Listar gêneros | ✅ |
| `POST` | `/api/v1/genres` | Criar gênero | ✅ |

---

### Users

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/v1/users` | Listar usuários | ✅ |
| `GET` | `/api/v1/users/{id}` | Buscar usuário | ✅ |
| `PUT` | `/api/v1/users/{id}` | Atualizar usuário | ✅ |

---

### Watch Entries

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/v1/watch-entries` | Listar por usuário | ✅ |
| `POST` | `/api/v1/watch-entries` | Criar registro | ✅ |

**Status permitidos** (State Pattern):

```
PLANNING → WATCHING → COMPLETED
PLANNING → WATCHING → DROPPED
WATCHING → PLANNING (re-plan)
```

---

### Seasons & Episodes

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/v1/media/{id}/seasons` | Listar temporadas | ✅ |
| `POST` | `/api/v1/media/{id}/seasons` | Criar temporada | ✅ |
| `GET` | `/api/v1/seasons/{id}/episodes` | Listar episódios | ✅ |
| `POST` | `/api/v1/seasons/{id}/episodes` | Criar episódio | ✅ |

---

### Credits & People

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/v1/media/{id}/credits` | Listar créditos | ✅ |
| `POST` | `/api/v1/media/{id}/credits` | Adicionar crédito | ✅ |
| `GET` | `/api/v1/people` | Listar pessoas | ✅ |
| `POST` | `/api/v1/people` | Criar pessoa | ✅ |

**Roles disponíveis**: `ACTOR`, `DIRECTOR`, `WRITER`, `PRODUCER`

---

### Watchlist

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/v1/watchlist` | Obter watchlist do usuário | ✅ |
| `POST` | `/api/v1/watchlist/{mediaId}` | Adicionar à watchlist | ✅ |
| `DELETE` | `/api/v1/watchlist/{mediaId}` | Remover da watchlist | ✅ |

---

## Formato de Erro (RFC 9457)

Todas as respostas de erro seguem o formato **ProblemDetail**:

```json
{
  "type": "https://api.cinelog.com/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "Payload inválido.",
  "instance": "/api/v1/media",
  "timestamp": "2025-01-15T10:30:00Z",
  "traceId": "abc123",
  "errorCode": "GEN_VALIDATION",
  "fieldErrors": [
    {
      "field": "title",
      "message": "não pode estar em branco",
      "rejectedValue": null
    }
  ]
}
```

### Status Codes

| Código | Significado | Quando |
|---|---|---|
| `200` | OK | GET, PUT com sucesso |
| `201` | Created | POST com sucesso |
| `204` | No Content | DELETE com sucesso |
| `400` | Bad Request | Validação falhou, JSON malformado |
| `401` | Unauthorized | Token JWT inválido ou ausente |
| `403` | Forbidden | Sem permissão para o recurso |
| `404` | Not Found | Recurso não existe |
| `409` | Conflict | Duplicata, optimistic lock |
| `423` | Locked | Conta bloqueada |
| `429` | Too Many Requests | Rate limit excedido |
| `500` | Internal Error | Erro inesperado |
| `502` | Bad Gateway | Erro em serviço externo (TMDb) |
| `503` | Service Unavailable | Circuit breaker aberto |

---

## Paginação

Todas as listagens retornam paginação no formato Spring:

```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

**Parâmetros de paginação:**
- `page`: número da página (zero-based)
- `size`: itens por página (default: 20, max: 100)
- `sort`: campo e direção (`title,asc` ou `createdAt,desc`)

---

## Swagger / OpenAPI

| URL | Descrição |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI interativo |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON spec |

---

## Versionamento da API

A API usa versionamento via URL path: `/api/v1/...`

Quando uma breaking change for necessária, uma nova versão será criada (`/api/v2/...`) mantendo a anterior em operação por 6 meses.
