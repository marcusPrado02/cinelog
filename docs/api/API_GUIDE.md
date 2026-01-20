# 📖 Guia Completo da API - CineLog

## Índice

1. [Visão Geral](#visão-geral)
2. [Autenticação](#autenticação)
3. [Endpoints](#endpoints)
4. [Modelos de Dados](#modelos-de-dados)
5. [Códigos de Status](#códigos-de-status)
6. [Exemplos de Uso](#exemplos-de-uso)
7. [Rate Limiting](#rate-limiting)
8. [Versionamento](#versionamento)

---

## Visão Geral

A API do CineLog é uma **REST API** que segue os princípios RESTful e utiliza JSON como formato de troca de dados.

### Base URL

```
http://localhost:8080/api/v1
```

### Headers Comuns

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}
```

### Documentação Interativa

-   **Swagger UI**: http://localhost:8080/swagger-ui/index.html
-   **OpenAPI Spec**: http://localhost:8080/v3/api-docs

---

## Autenticação

### Registro de Usuário

**Endpoint**: `POST /api/v1/auth/register`

**Request Body**:

```json
{
    "name": "João Silva",
    "email": "joao@example.com",
    "password": "senha123"
}
```

**Response** (201 Created):

```json
{
    "id": 1,
    "name": "João Silva",
    "email": "joao@example.com",
    "createdAt": "2025-12-10T10:30:00Z"
}
```

### Login

**Endpoint**: `POST /api/v1/auth/login`

**Request Body**:

```json
{
    "email": "joao@example.com",
    "password": "senha123"
}
```

**Response** (200 OK):

```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
}
```

### Refresh Token

**Endpoint**: `POST /api/v1/auth/refresh`

**Request Body**:

```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK):

```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
}
```

---

## Endpoints

### 📽️ Media (Mídias)

#### Listar Mídias

**Endpoint**: `GET /api/v1/media`

**Query Parameters**:

-   `page` (int): Número da página (default: 0)
-   `size` (int): Tamanho da página (default: 20)
-   `sort` (string): Campo de ordenação (ex: `title,asc`)
-   `type` (enum): Filtrar por tipo (`MOVIE` ou `SERIES`)
-   `year` (int): Filtrar por ano de lançamento

**Response** (200 OK):

```json
{
    "content": [
        {
            "id": 1,
            "title": "Matrix",
            "type": "MOVIE",
            "releaseYear": 1999,
            "originalTitle": "The Matrix",
            "originalLanguage": "en",
            "overview": "Um programador descobre a verdade sobre a realidade",
            "posterUrl": "https://...",
            "backdropUrl": "https://...",
            "genres": [
                {
                    "id": 1,
                    "name": "Ficção Científica"
                }
            ],
            "createdAt": "2025-12-10T10:30:00Z",
            "updatedAt": "2025-12-10T10:30:00Z"
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20,
        "sort": {
            "sorted": true,
            "unsorted": false
        }
    },
    "totalPages": 5,
    "totalElements": 100,
    "last": false,
    "first": true
}
```

#### Buscar Mídia por ID

**Endpoint**: `GET /api/v1/media/{id}`

**Response** (200 OK):

```json
{
    "id": 1,
    "title": "Matrix",
    "type": "MOVIE",
    "releaseYear": 1999,
    "originalTitle": "The Matrix",
    "originalLanguage": "en",
    "overview": "Um programador descobre a verdade sobre a realidade",
    "posterUrl": "https://...",
    "backdropUrl": "https://...",
    "genres": [
        {
            "id": 1,
            "name": "Ficção Científica"
        }
    ],
    "credits": [
        {
            "id": 1,
            "person": {
                "id": 1,
                "name": "Keanu Reeves"
            },
            "role": "ACTOR",
            "characterName": "Neo",
            "orderIndex": 1
        }
    ],
    "createdAt": "2025-12-10T10:30:00Z",
    "updatedAt": "2025-12-10T10:30:00Z"
}
```

#### Criar Mídia

**Endpoint**: `POST /api/v1/media`

**Request Body**:

```json
{
    "title": "Matrix",
    "type": "MOVIE",
    "releaseYear": 1999,
    "originalTitle": "The Matrix",
    "originalLanguage": "en",
    "overview": "Um programador descobre a verdade sobre a realidade",
    "posterUrl": "https://...",
    "backdropUrl": "https://...",
    "genreIds": [1, 2]
}
```

**Response** (201 Created):

```json
{
    "id": 1,
    "title": "Matrix",
    "type": "MOVIE",
    "releaseYear": 1999,
    "createdAt": "2025-12-10T10:30:00Z"
}
```

#### Atualizar Mídia

**Endpoint**: `PUT /api/v1/media/{id}`

**Request Body**: (mesma estrutura de criação)

**Response** (200 OK): (mídia atualizada)

#### Deletar Mídia

**Endpoint**: `DELETE /api/v1/media/{id}`

**Response** (204 No Content)

---

### 🎭 Genres (Gêneros)

#### Listar Gêneros

**Endpoint**: `GET /api/v1/genres`

**Response** (200 OK):

```json
[
    {
        "id": 1,
        "name": "Ficção Científica"
    },
    {
        "id": 2,
        "name": "Ação"
    }
]
```

#### Criar Gênero

**Endpoint**: `POST /api/v1/genres`

**Request Body**:

```json
{
    "name": "Terror"
}
```

**Response** (201 Created):

```json
{
    "id": 3,
    "name": "Terror"
}
```

---

### 👥 Users (Usuários)

#### Listar Usuários

**Endpoint**: `GET /api/v1/users`

**Query Parameters**:

-   `page` (int): Número da página
-   `size` (int): Tamanho da página

**Response** (200 OK):

```json
{
    "content": [
        {
            "id": 1,
            "name": "João Silva",
            "email": "joao@example.com",
            "createdAt": "2025-12-10T10:30:00Z"
        }
    ],
    "totalElements": 50
}
```

#### Buscar Usuário por ID

**Endpoint**: `GET /api/v1/users/{id}`

**Response** (200 OK):

```json
{
    "id": 1,
    "name": "João Silva",
    "email": "joao@example.com",
    "createdAt": "2025-12-10T10:30:00Z"
}
```

#### Atualizar Usuário

**Endpoint**: `PUT /api/v1/users/{id}`

**Request Body**:

```json
{
    "name": "João Silva Santos",
    "email": "joao.santos@example.com"
}
```

**Response** (200 OK): (usuário atualizado)

---

### 📺 Seasons (Temporadas)

#### Listar Temporadas de uma Mídia

**Endpoint**: `GET /api/v1/media/{mediaId}/seasons`

**Response** (200 OK):

```json
[
    {
        "id": 1,
        "mediaId": 1,
        "seasonNumber": 1,
        "name": "Primeira Temporada",
        "airDate": "2011-04-17",
        "episodeCount": 10
    }
]
```

#### Criar Temporada

**Endpoint**: `POST /api/v1/media/{mediaId}/seasons`

**Request Body**:

```json
{
    "seasonNumber": 1,
    "name": "Primeira Temporada",
    "airDate": "2011-04-17"
}
```

**Response** (201 Created): (temporada criada)

---

### 🎬 Episodes (Episódios)

#### Listar Episódios de uma Temporada

**Endpoint**: `GET /api/v1/seasons/{seasonId}/episodes`

**Response** (200 OK):

```json
[
    {
        "id": 1,
        "seasonId": 1,
        "episodeNumber": 1,
        "name": "Winter Is Coming",
        "airDate": "2011-04-17",
        "overview": "Eddard Stark é convocado..."
    }
]
```

#### Criar Episódio

**Endpoint**: `POST /api/v1/seasons/{seasonId}/episodes`

**Request Body**:

```json
{
    "episodeNumber": 1,
    "name": "Winter Is Coming",
    "airDate": "2011-04-17",
    "overview": "Eddard Stark é convocado..."
}
```

**Response** (201 Created): (episódio criado)

---

### 🎟️ WatchEntry (Registro de Visualização)

#### Listar Registros de um Usuário

**Endpoint**: `GET /api/v1/users/{userId}/watch-entries`

**Response** (200 OK):

```json
[
    {
        "id": 1,
        "userId": 1,
        "mediaId": 1,
        "episodeId": null,
        "rating": 9,
        "comment": "Filme incrível!",
        "watchedAt": "2025-12-10",
        "createdAt": "2025-12-10T10:30:00Z"
    }
]
```

#### Criar Registro

**Endpoint**: `POST /api/v1/watch-entries`

**Request Body**:

```json
{
    "userId": 1,
    "mediaId": 1,
    "episodeId": null,
    "rating": 9,
    "comment": "Filme incrível!",
    "watchedAt": "2025-12-10"
}
```

**Response** (201 Created): (registro criado)

---

### 🎭 Credits (Créditos)

#### Listar Créditos de uma Mídia

**Endpoint**: `GET /api/v1/media/{mediaId}/credits`

**Response** (200 OK):

```json
[
    {
        "id": 1,
        "mediaId": 1,
        "person": {
            "id": 1,
            "name": "Keanu Reeves",
            "birthDate": "1964-09-02",
            "placeOfBirth": "Beirut, Lebanon"
        },
        "role": "ACTOR",
        "characterName": "Neo",
        "orderIndex": 1
    }
]
```

#### Adicionar Crédito

**Endpoint**: `POST /api/v1/media/{mediaId}/credits`

**Request Body**:

```json
{
    "personId": 1,
    "role": "ACTOR",
    "characterName": "Neo",
    "orderIndex": 1
}
```

**Response** (201 Created): (crédito criado)

---

### 👤 People (Pessoas)

#### Listar Pessoas

**Endpoint**: `GET /api/v1/people`

**Query Parameters**:

-   `page` (int): Número da página
-   `size` (int): Tamanho da página
-   `name` (string): Filtrar por nome

**Response** (200 OK):

```json
{
    "content": [
        {
            "id": 1,
            "name": "Keanu Reeves",
            "birthDate": "1964-09-02",
            "placeOfBirth": "Beirut, Lebanon"
        }
    ]
}
```

#### Criar Pessoa

**Endpoint**: `POST /api/v1/people`

**Request Body**:

```json
{
    "name": "Keanu Reeves",
    "birthDate": "1964-09-02",
    "placeOfBirth": "Beirut, Lebanon"
}
```

**Response** (201 Created): (pessoa criada)

---

### 📋 Watchlist (Lista de Desejos)

#### Obter Watchlist do Usuário

**Endpoint**: `GET /api/v1/users/{userId}/watchlist`

**Response** (200 OK):

```json
{
    "userId": 1,
    "items": [
        {
            "id": 1,
            "media": {
                "id": 1,
                "title": "Matrix",
                "type": "MOVIE"
            },
            "addedAt": "2025-12-10T10:30:00Z"
        }
    ]
}
```

#### Adicionar à Watchlist

**Endpoint**: `POST /api/v1/users/{userId}/watchlist`

**Request Body**:

```json
{
    "mediaId": 1
}
```

**Response** (201 Created)

#### Remover da Watchlist

**Endpoint**: `DELETE /api/v1/users/{userId}/watchlist/{mediaId}`

**Response** (204 No Content)

---

## Modelos de Dados

### MediaType (Enum)

```
MOVIE  - Filme
SERIES - Série
```

### Role (Enum)

```
ACTOR    - Ator/Atriz
DIRECTOR - Diretor
WRITER   - Roteirista
PRODUCER - Produtor
```

### Estrutura de Paginação

```json
{
    "content": [],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20,
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "totalPages": 5,
    "totalElements": 100,
    "last": false,
    "first": true,
    "size": 20,
    "number": 0,
    "numberOfElements": 20,
    "empty": false
}
```

---

## Códigos de Status

### Sucesso (2xx)

-   **200 OK**: Requisição bem-sucedida
-   **201 Created**: Recurso criado com sucesso
-   **204 No Content**: Requisição bem-sucedida sem conteúdo de retorno

### Erro do Cliente (4xx)

-   **400 Bad Request**: Dados inválidos
-   **401 Unauthorized**: Não autenticado
-   **403 Forbidden**: Sem permissão
-   **404 Not Found**: Recurso não encontrado
-   **409 Conflict**: Conflito (ex: email duplicado)
-   **422 Unprocessable Entity**: Validação falhou

### Erro do Servidor (5xx)

-   **500 Internal Server Error**: Erro interno
-   **503 Service Unavailable**: Serviço temporariamente indisponível

### Estrutura de Erro

```json
{
    "timestamp": "2025-12-10T10:30:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Título é obrigatório",
    "path": "/api/v1/media",
    "traceId": "abc123"
}
```

---

## Exemplos de Uso

### Fluxo Completo: Criar e Assistir um Filme

#### 1. Registrar Usuário

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Santos",
    "email": "maria@example.com",
    "password": "senha123"
  }'
```

#### 2. Fazer Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria@example.com",
    "password": "senha123"
  }'
```

**Guarde o token**: `export TOKEN="eyJhbGciOiJ..."`

#### 3. Criar um Filme

```bash
curl -X POST http://localhost:8080/api/v1/media \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Inception",
    "type": "MOVIE",
    "releaseYear": 2010,
    "originalTitle": "Inception",
    "originalLanguage": "en",
    "overview": "Um ladrão que rouba segredos através do uso da tecnologia de compartilhamento de sonhos",
    "genreIds": [1, 2]
  }'
```

#### 4. Registrar Visualização

```bash
curl -X POST http://localhost:8080/api/v1/watch-entries \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": 1,
    "mediaId": 1,
    "rating": 10,
    "comment": "Obra-prima do cinema!",
    "watchedAt": "2025-12-10"
  }'
```

#### 5. Listar Minhas Visualizações

```bash
curl http://localhost:8080/api/v1/users/1/watch-entries \
  -H "Authorization: Bearer $TOKEN"
```

---

## Rate Limiting

### Limites Atuais

-   **Requisições autenticadas**: 100 req/minuto
-   **Requisições não autenticadas**: 20 req/minuto
-   **Login**: 5 tentativas/minuto

### Headers de Rate Limit

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1701345600
```

---

## Versionamento

A API utiliza versionamento via URL:

-   **Versão atual**: `/api/v1/*`
-   **Compatibilidade**: Mantida por pelo menos 6 meses após nova versão
-   **Deprecação**: Anunciada com 3 meses de antecedência

### Header de Versão

```http
X-API-Version: 1.0.0
```

---

## Filtros e Ordenação

### Filtros Disponíveis

**Media**:

-   `type`: MOVIE ou SERIES
-   `year`: Ano de lançamento
-   `genreId`: ID do gênero

**WatchEntry**:

-   `userId`: ID do usuário
-   `mediaId`: ID da mídia
-   `rating`: Nota (1-10)

### Ordenação

Use o parâmetro `sort`:

```
GET /api/v1/media?sort=title,asc
GET /api/v1/media?sort=releaseYear,desc&sort=title,asc
```

---

## Boas Práticas

### 1. Sempre use HTTPS em produção

```
https://api.cinelog.com/v1/*
```

### 2. Inclua Accept Header

```http
Accept: application/json
```

### 3. Use ETags para Cache

```http
ETag: "33a64df551425fcc55e4d42a148795d9f25f89d4"
If-None-Match: "33a64df551425fcc55e4d42a148795d9f25f89d4"
```

### 4. Trate Erros Adequadamente

```javascript
try {
    const response = await fetch("/api/v1/media/999");
    if (!response.ok) {
        const error = await response.json();
        console.error("API Error:", error.message);
    }
} catch (err) {
    console.error("Network Error:", err);
}
```

---

## Suporte

-   **Documentação**: http://localhost:8080/swagger-ui/index.html
-   **Issues**: https://github.com/marcusPrado02/cinelog/issues
-   **Email**: suporte@cinelog.com

---

**Última atualização**: Dezembro 2025
