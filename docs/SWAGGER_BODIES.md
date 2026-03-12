# CineLog — Swagger Request Bodies

> **Base URL:** `http://localhost:8080`  
> **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`  
> **Autenticação:** Cole o token JWT no botão **Authorize** do Swagger (`Bearer <token>`)

---

## Fluxo Recomendado

```
1. POST /api/auth/register   → pega o accessToken
2. Authorize no Swagger      → Bearer <accessToken>
3. Explorar as demais rotas
```

---

## Índice

- [Auth](#auth)
- [Genres](#genres)
- [Media](#media)
- [Media Search](#media-search)
- [People](#people)
- [Credits](#credits)
- [Seasons](#seasons)
- [Episodes](#episodes)
- [Users](#users)
- [Watch Entries](#watch-entries)
- [Watch Progress](#watch-progress)
- [Watchlist](#watchlist)
- [Recommendations](#recommendations)
- [User Insights](#user-insights)
- [Media Popularity](#media-popularity)
- [Admin — Dead Letter Queue](#admin--dead-letter-queue)

---

## Auth

> Base: `/api/auth`  
> Rotas **públicas** (sem token).

---

### `POST /api/auth/register` — Cadastrar novo usuário

```json
{
    "name": "Marcus Prado",
    "email": "marcus@cinelog.dev",
    "password": "MinhaSenh@123"
}
```

**Resposta (200):**

```json
{
    "accessToken": "eyJhbGciOiJIUzI1N...",
    "refreshToken": "d3b2c1a0-...",
    "tokenType": "Bearer"
}
```

---

### `POST /api/auth/login` — Login

```json
{
    "email": "marcus@cinelog.dev",
    "password": "MinhaSenh@123"
}
```

Login com usuário seed (admin):

```json
{
    "email": "admin@cinelog.dev",
    "password": "Admin@12345"
}
```

---

### `POST /api/auth/refresh` — Renovar tokens

```json
{
    "refreshToken": "d3b2c1a0-9f8e-7d6c-5b4a-3210fedcba98"
}
```

> Substitua o valor pelo `refreshToken` retornado no login/register.

---

### `POST /api/auth/logout` — Logout

> **Requer JWT.** Sem body. Revoga todos os refresh tokens do usuário autenticado.

```
(sem body)
```

---

## Genres

> Base: `/api/v1/genres`  
> **CREATE/DELETE** requer role `CONTENT_ADMIN`.

---

### `POST /api/v1/genres` — Criar gênero

```json
{
    "name": "Cyberpunk"
}
```

---

### `PUT /api/v1/genres/{id}` — Atualizar gênero

```
Path: /api/v1/genres/50
```

```json
{
    "name": "Ficção Científica Clássica"
}
```

---

### `GET /api/v1/genres/{id}` — Buscar gênero por ID

```
Path: /api/v1/genres/1
```

> Sem body.

---

### `GET /api/v1/genres` — Listar gêneros (paginado)

```
Query params (opcionais):
  page=0
  size=20
  sort=name,asc
```

> Sem body.

---

### `DELETE /api/v1/genres/{id}` — Remover gênero

```
Path: /api/v1/genres/65
```

> Sem body.

---

## Media

> Base: `/api/v1/media`

---

### `POST /api/v1/media` — Criar mídia (filme)

```json
{
    "title": "Dune: Part Three",
    "type": "MOVIE",
    "releaseYear": 2026,
    "originalTitle": "Dune: Part Three",
    "originalLanguage": "en",
    "posterUrl": "https://image.tmdb.org/t/p/w500/dune3.jpg",
    "backdropUrl": "https://image.tmdb.org/t/p/original/dune3_bg.jpg",
    "overview": "A conclusão épica da saga de Paul Atreides.",
    "tmdbId": 99999
}
```

### `POST /api/v1/media` — Criar mídia (série)

```json
{
    "title": "Severance",
    "type": "SERIES",
    "releaseYear": 2022,
    "originalTitle": "Severance",
    "originalLanguage": "en",
    "posterUrl": "https://image.tmdb.org/t/p/w500/severance.jpg",
    "backdropUrl": "https://image.tmdb.org/t/p/original/severance_bg.jpg",
    "overview": "Trabalhadores submetem-se a um procedimento cirúrgico que divide memórias pessoais e profissionais.",
    "tmdbId": 95396
}
```

---

### `PUT /api/v1/media/{id}` — Atualizar mídia

```
Path: /api/v1/media/1
```

```json
{
    "title": "The Shawshank Redemption",
    "type": "MOVIE",
    "releaseYear": 1994,
    "originalTitle": "The Shawshank Redemption",
    "originalLanguage": "en",
    "posterUrl": "https://image.tmdb.org/t/p/w500/shawshank.jpg",
    "backdropUrl": "https://image.tmdb.org/t/p/original/shawshank_bg.jpg",
    "overview": "Dois homens presos criam um vínculo durante anos, encontrando consolo e eventual redenção.",
    "tmdbId": 278
}
```

---

### `GET /api/v1/media/{id}` — Buscar mídia por ID

```
Path: /api/v1/media/1
```

> Sem body.

---

### `GET /api/v1/media` — Listar mídias (paginado)

```
Query params (opcionais):
  page=0
  size=20
  sort=id,asc
```

> Sem body.

---

### `DELETE /api/v1/media/{id}` — Remover mídia

```
Path: /api/v1/media/122
```

> Sem body. Requer role `MEDIA_ADMIN`.

---

### `POST /api/v1/media/search` — Busca avançada com body

> **GET com body não funciona em navegadores.** Use `POST`.

```json
{
    "text": "Inception",
    "type": "MOVIE",
    "yearMin": 2000,
    "yearMax": 2023,
    "ratingMin": 7.0,
    "ratingMax": 10.0,
    "genreIds": [1, 50],
    "page": 0,
    "size": 10,
    "sortBy": "releaseYear",
    "sortDirection": "DESC"
}
```

---

## Media Search

> Base: `/api/media`  
> Endpoints de busca via **query string** (Specification Pattern).

---

### `GET /api/media/search` — Busca avançada

```
Query params (todos opcionais):
  text=Inception
  type=MOVIE
  yearMin=2000
  yearMax=2023
  ratingMin=7.5
  ratingMax=10.0
  genreIds=1,50,51
  page=0
  size=20
  sort=averageRating
  direction=DESC
```

> Sem body.

**Exemplos prontos:**

| Caso                        | URL                                                                             |
| --------------------------- | ------------------------------------------------------------------------------- |
| Filmes de ficção científica | `/api/media/search?type=MOVIE&genreIds=51`                                      |
| Séries com rating ≥ 8.5     | `/api/media/search?type=SERIES&ratingMin=8.5&sort=averageRating&direction=DESC` |
| Lançamentos 2020-2024       | `/api/media/search?yearMin=2020&yearMax=2024&sort=releaseYear&direction=DESC`   |
| Busca por Matrix            | `/api/media/search?text=Matrix&type=MOVIE`                                      |

---

### `GET /api/media/search/text` — Busca simples por texto

```
Query params:
  q=Star Wars
  page=0
  size=20
```

> Sem body.

---

### `GET /api/media/top-rated` — Top rated

```
Query params:
  limit=20
```

> Sem body.

---

### `GET /api/media/trending` — Trending

```
Query params:
  limit=20
  period=WEEK   (opções: DAY, WEEK, MONTH, ALL_TIME)
```

> Sem body.

---

### `GET /api/media/most-watched` — Mais assistidos

```
Query params:
  limit=20
```

> Sem body.

---

## People

> Base: `/api/v1/people`  
> **CREATE/DELETE** requer role `CONTENT_ADMIN`.

---

### `POST /api/v1/people` — Criar pessoa

```json
{
    "name": "Zendaya Coleman",
    "birthDate": "1996-09-01",
    "placeOfBirth": "Oakland, California, EUA"
}
```

---

### `PUT /api/v1/people/{id}` — Atualizar pessoa

```
Path: /api/v1/people/185
```

```json
{
    "name": "Tim Robbins",
    "birthDate": "1958-10-16",
    "placeOfBirth": "West Covina, California, EUA"
}
```

---

### `GET /api/v1/people/{id}` — Buscar pessoa por ID

```
Path: /api/v1/people/185
```

> Sem body.

---

### `GET /api/v1/people` — Listar pessoas (paginado)

```
Query params (opcionais):
  page=0
  size=20
  sort=name,asc
```

> Sem body.

---

### `DELETE /api/v1/people/{id}` — Remover pessoa

```
Path: /api/v1/people/282
```

> Sem body.

---

## Credits

> Base: `/api/v1/credits`  
> **CREATE/DELETE** requer role `CONTENT_ADMIN`.

---

### `POST /api/v1/credits` — Criar crédito

```json
{
    "mediaId": 1,
    "personId": 185,
    "role": "ACTOR",
    "characterName": "Andy Dufresne",
    "orderIndex": 0
}
```

Outro exemplo (diretora):

```json
{
    "mediaId": 5,
    "personId": 190,
    "role": "DIRECTOR",
    "characterName": null,
    "orderIndex": 0
}
```

> Valores válidos para `role`: `ACTOR`, `ACTRESS`, `DIRECTOR`, `WRITER`, `PRODUCER`, `COMPOSER`, `CINEMATOGRAPHER`, `OTHER`

---

### `PUT /api/v1/credits/{id}` — Atualizar crédito

```
Path: /api/v1/credits/1
```

```json
{
    "role": "ACTOR",
    "characterName": "Red Redding",
    "orderIndex": 1
}
```

---

### `GET /api/v1/credits/{id}` — Buscar crédito por ID

```
Path: /api/v1/credits/1
```

> Sem body.

---

### `GET /api/v1/credits` — Listar créditos (paginado)

```
Query params (opcionais):
  page=0
  size=20
  sort=id,asc
```

> Sem body.

---

### `DELETE /api/v1/credits/{id}` — Remover crédito

```
Path: /api/v1/credits/10
```

> Sem body.

---

## Seasons

> Base: `/api/v1/seasons`  
> **CREATE/DELETE** requer role `CONTENT_ADMIN`.  
> `mediaId` deve ser uma mídia do tipo `SERIES`.

---

### `POST /api/v1/seasons` — Criar temporada

```json
{
    "mediaId": 2,
    "seasonNumber": 1,
    "name": "Temporada 1",
    "airDate": "2011-04-17"
}
```

Temporada especial (número 0):

```json
{
    "mediaId": 2,
    "seasonNumber": 0,
    "name": "Especiais",
    "airDate": null
}
```

---

### `PUT /api/v1/seasons/{id}` — Atualizar temporada

```
Path: /api/v1/seasons/1
```

```json
{
    "seasonNumber": 1,
    "name": "Season One",
    "airDate": "2011-04-17"
}
```

---

### `GET /api/v1/seasons/{id}` — Buscar temporada por ID

```
Path: /api/v1/seasons/1
```

> Sem body.

---

### `GET /api/v1/seasons` — Listar temporadas (paginado)

```
Query params (opcionais):
  page=0
  size=20
  sort=id,asc
```

> Sem body.

---

### `DELETE /api/v1/seasons/{id}` — Remover temporada

```
Path: /api/v1/seasons/1
```

> Sem body. A temporada não pode ter episódios.

---

## Episodes

> Base: `/api/v1/episodes`  
> **CREATE/DELETE** requer role `CONTENT_ADMIN`.

---

### `POST /api/v1/episodes` — Criar episódio

```json
{
    "seasonId": 1,
    "episodeNumber": 1,
    "name": "Winter Is Coming",
    "airDate": "2011-04-17"
}
```

---

### `PUT /api/v1/episodes/{id}` — Atualizar episódio

```
Path: /api/v1/episodes/1
```

```json
{
    "episodeNumber": 1,
    "name": "Winter Is Coming — Extended",
    "airDate": "2011-04-17"
}
```

---

### `GET /api/v1/episodes/{id}` — Buscar episódio por ID

```
Path: /api/v1/episodes/1
```

> Sem body.

---

### `GET /api/v1/episodes` — Listar episódios (paginado)

```
Query params (opcionais):
  page=0
  size=20
  sort=episodeNumber,asc
```

> Sem body.

---

### `DELETE /api/v1/episodes/{id}` — Remover episódio

```
Path: /api/v1/episodes/1
```

> Sem body.

---

## Users

> Base: `/api/v1/users`  
> **LIST** requer role `ADMIN`. **UPDATE** requer ser o próprio usuário ou `ADMIN`.

---

### `POST /api/v1/users` — Criar usuário (admin)

```json
{
    "name": "Fernanda Lima",
    "email": "fernanda@cinelog.dev"
}
```

> **Nota:** Para criar usuário com senha, use `POST /api/auth/register`.

---

### `PUT /api/v1/users/{id}` — Atualizar usuário

```
Path: /api/v1/users/11
```

```json
{
    "name": "Ana Cinéfila Atualizado"
}
```

---

### `GET /api/v1/users/{id}` — Buscar usuário por ID

```
Path: /api/v1/users/11
```

> Sem body.

---

### `GET /api/v1/users` — Listar usuários (paginado) — `ADMIN`

```
Query params (opcionais):
  page=0
  size=20
  sort=name,asc
```

> Sem body.

---

### `DELETE /api/v1/users/{id}` — Remover usuário — `ADMIN`

```
Path: /api/v1/users/5
```

> Sem body. Falha se usuário tiver watch entries ou watchlist.
> Use IDs sem histórico: `5` (john@example.com), `6` (marcus@cinelog.dev).

---

### `GET /api/v1/users/me/stats` — Estatísticas do usuário autenticado

```
(sem body, sem path param)
```

> Requer JWT. Retorna estatísticas do usuário atual.

---

## Watch Entries

> Base: `/api/v1/watch-entries`  
> Registro de mídias/episódios assistidos com rating opcional.

---

### `POST /api/v1/watch-entries` — Registrar visualização de filme

```json
{
    "userId": 11,
    "mediaId": 1,
    "episodeId": null,
    "rating": 9,
    "comment": "Obra-prima absoluta. Roteiro impecável.",
    "watchedAt": "2026-03-12"
}
```

### `POST /api/v1/watch-entries` — Registrar visualização de episódio

```json
{
    "userId": 11,
    "mediaId": null,
    "episodeId": 1,
    "rating": 8,
    "comment": "Episódio piloto incrível.",
    "watchedAt": "2026-03-10"
}
```

### `POST /api/v1/watch-entries` — Sem rating

```json
{
    "userId": 12,
    "watchedAt": null
}
```

> **Regra:** Informe `mediaId` OU `episodeId`, nunca ambos.  
> `rating` aceita valores de **0 a 10**.

---

### `PUT /api/v1/watch-entries/{id}` — Atualizar visualização

```
Path: /api/v1/watch-entries/1
```

```json
{
    "userId": 11,
    "mediaId": 1,
    "episodeId": null,
    "rating": 10,
    "comment": "Reassisti e agora acho ainda melhor. 10/10.",
    "watchedAt": "2026-03-12"
}
```

---

### `GET /api/v1/watch-entries` — Listar por usuário (paginado)

```
Query params:
  userId=11         (obrigatório)
  mediaId=1         (opcional)
  episodeId=        (opcional)
  minRating=7       (opcional)
  from=2026-01-01   (opcional, formato yyyy-MM-dd)
  to=2026-12-31     (opcional)
  page=0
  size=20
```

> Sem body.

---

### `GET /api/v1/watch-entries/{id}` — Buscar por ID

```
Path: /api/v1/watch-entries/1
```

> Sem body.

---

### `DELETE /api/v1/watch-entries/{id}` — Remover

```
Path: /api/v1/watch-entries/1
```

> Sem body.

---

## Watch Progress

> Base: `/api/watchentries`  
> Controle de progresso de séries (temporada/episódio/tempo).

---

### `POST /api/watchentries/{id}/progress` — Criar/Atualizar progresso

```
Path: /api/watchentries/1/progress
```

```json
{
    "currentSeason": 1,
    "currentEpisode": 3,
    "watchedDurationSeconds": 1820,
    "totalDurationSeconds": 3600
}
```

Progresso completo do episódio:

```json
{
    "currentSeason": 2,
    "currentEpisode": 5,
    "watchedDurationSeconds": 2700,
    "totalDurationSeconds": 2700
}
```

> `currentSeason` ≥ 1, `currentEpisode` ≥ 1, `watchedDurationSeconds` ≥ 0, `totalDurationSeconds` > 0

---

### `GET /api/watchentries/{id}/progress` — Buscar progresso

```
Path: /api/watchentries/1/progress
```

> Sem body.

---

### `DELETE /api/watchentries/{id}/progress` — Deletar progresso

```
Path: /api/watchentries/1/progress
```

> Sem body.

---

## Watchlist

> Base: `/api/v1/watchlist`  
> Lista de desejos do usuário autenticado.

---

### `POST /api/v1/watchlist` — Adicionar à watchlist

```json
{
    "mediaId": 15
}
```

---

### `DELETE /api/v1/watchlist/{mediaId}` — Remover da watchlist

```
Path: /api/v1/watchlist/15
```

> Sem body.

---

### `GET /api/v1/watchlist` — Listar watchlist do usuário autenticado

```
Query params (opcionais):
  page=0
  size=20
  sort=addedAt,desc
```

> Sem body.

---

## Recommendations

> Base: `/api/users/{userId}`  
> Recomendações personalizadas por estratégia.

---

### `GET /api/users/{userId}/recommendations` — Recomendações automáticas

> Use IDs com histórico: **11** (Ana), **12** (Bruno), **13** (Carla), **14** (Diego).

```
Path: /api/users/11/recommendations
Query params:
  limit=20   (opcional, padrão 20, máx 100)
```

> Sem body. Usa a melhor estratégia disponível (hybrid → content-based → collaborative).

---

### `GET /api/users/{userId}/recommendations/strategies` — Estratégias disponíveis

```
Path: /api/users/11/recommendations/strategies
```

> Sem body.

---

### `GET /api/users/{userId}/recommendations/{strategy}` — Estratégia específica

```
Path: /api/users/11/recommendations/hybrid
Query params:
  limit=20
```

| Estratégia      | Descrição                                          |
| --------------- | -------------------------------------------------- |
| `content-based` | Baseado em gêneros/atributos das mídias assistidas |
| `collaborative` | Baseado em usuários com gostos similares           |
| `hybrid`        | Combina as duas (recomendado)                      |

> Sem body.

---

## User Insights

> Base: `/api/users`  
> Estatísticas de visualização (CQRS Read Model — atualização eventual via Kafka).

---

### `GET /api/users/{userId}/insights` — Insights do usuário

> Use IDs com histórico: **11** (Ana), **12** (Bruno), **13** (Carla), **14** (Diego).

```
Path: /api/users/11/insights
```

> Sem body.

**Resposta esperada:**

```json
{
    "userId": 11,
    "totalWatched": 10,
    "totalMovies": 7,
    "totalSeries": 3,
    "avgRating": 9.22,
    "lastWatchedAt": "2025-10-05",
    "updatedAt": "2026-03-12T01:27:36"
}
```

---

### `GET /api/users/{userId}/insights/exists` — Verificar se usuário tem stats

```
Path: /api/users/11/insights/exists
```

> Sem body. Retorna 200 se usuário tem stats, 404 se não tem.

---

## Media Popularity

> Base: `/api/media`  
> Rankings e tendências (CQRS Read Model — atualização eventual via Kafka).

---

### `GET /api/media/top-rated` — Top rated

```
Query params:
  limit=20   (opcional, padrão 20, máx 100)
```

> Sem body. Mínimo de 3 avaliações para aparecer.

---

### `GET /api/media/trending` — Trending (mais assistidos recentemente)

```
Query params:
  limit=20
  period=WEEK   (DAY | WEEK | MONTH | ALL_TIME)
```

> Sem body.

---

### `GET /api/media/most-watched` — Mais assistidos de todos os tempos

```
Query params:
  limit=20
```

> Sem body.

---

## Admin — Dead Letter Queue

> Base: `/admin/dlq`  
> **Requer role `ADMIN` ou `OPS`.**  
> Gerenciamento de eventos com falha no Kafka.

---

### `GET /admin/dlq` — Listar eventos DLQ

```
Query params (opcionais):
  status=PENDING_REPLAY   (PENDING_REPLAY | REPLAYED | IGNORED)
  topic=cinelog.watch-entries.created
  page=0
  size=20
  sort=createdAt,desc
```

> Sem body.

---

### `GET /admin/dlq/{id}` — Detalhe de evento DLQ

```
Path: /admin/dlq/1
```

> Sem body.

---

### `POST /admin/dlq/{id}/replay` — Reprocessar evento

```
Path: /admin/dlq/1/replay
```

> Sem body. Reenvia o evento para o tópico Kafka original.

---

### `POST /admin/dlq/{id}/ignore` — Ignorar evento

```
Path: /admin/dlq/1/ignore
```

> Sem body. Marca o evento como ignorado.

---

### `GET /admin/dlq/stats` — Estatísticas do DLQ

```
(sem params)
```

> Sem body.

---

### `GET /admin/dlq/topics` — Tópicos com eventos DLQ

```
(sem params)
```

> Sem body.

---

## Admin — Media (ADMIN only)

> Base: `/api/v1/admin/media`  
> **Requer role `ADMIN`.**

---

### `POST /api/v1/admin/media` — Criar mídia via admin

```json
{
    "title": "Avatar: Fire and Ash",
    "type": "MOVIE",
    "releaseYear": 2025,
    "originalTitle": "Avatar: Fire and Ash",
    "originalLanguage": "en",
    "posterUrl": "https://image.tmdb.org/t/p/w500/avatar3.jpg",
    "backdropUrl": "https://image.tmdb.org/t/p/original/avatar3_bg.jpg",
    "overview": "A terceira parte da saga de Pandora.",
    "tmdbId": 83533
}
```

---

## Referência Rápida de Enums

### `MediaType`

| Valor    | Descrição   |
| -------- | ----------- |
| `MOVIE`  | Filme       |
| `SERIES` | Série de TV |

### `Credit role` (string livre)

| Valor Comum       | Descrição                |
| ----------------- | ------------------------ |
| `ACTOR`           | Ator                     |
| `ACTRESS`         | Atriz                    |
| `DIRECTOR`        | Diretor(a)               |
| `WRITER`          | Roteirista               |
| `PRODUCER`        | Produtor(a)              |
| `COMPOSER`        | Compositor(a)            |
| `CINEMATOGRAPHER` | Diretor(a) de fotografia |

### `DlqStatus`

| Valor            | Descrição                  |
| ---------------- | -------------------------- |
| `PENDING_REPLAY` | Aguardando reprocessamento |
| `REPLAYED`       | Já reprocessado            |
| `IGNORED`        | Marcado como ignorado      |

### `TrendingPeriod`

| Valor      | Descrição       |
| ---------- | --------------- |
| `DAY`      | Último dia      |
| `WEEK`     | Última semana   |
| `MONTH`    | Último mês      |
| `ALL_TIME` | Todos os tempos |

---

## IDs de Referência (dados seed)

| Entidade              | IDs disponíveis   | Exemplo                                                                                              |
| --------------------- | ----------------- | ---------------------------------------------------------------------------------------------------- |
| Media (filmes/séries) | 1 – 122           | `1` = The Shawshank Redemption                                                                       |
| People                | 185 – 282         | `185` = Tim Robbins                                                                                  |
| Genres                | 1, 50 – 65        | `1` = Ação, `51` = Ficção Científica                                                                 |
| Users (com histórico) | 11 – 14           | `11` = ana@cinelog.dev, `12` = bruno@cinelog.dev, `13` = carla@cinelog.dev, `14` = diego@cinelog.dev |
| Users (sem histórico) | 1, 2, 4, 5, 6, 10 | `10` = demo@cinelog.dev (admin)                                                                      |

> **Insights e Recommendations:** Use userId **11–14** — são os únicos com `watch_entry` e `user_stats` populados via Kafka.

---

_Gerado em 12/03/2026 — CineLog v1.0_
