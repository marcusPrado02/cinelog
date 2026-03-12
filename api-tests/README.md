# CineLog API Tests (VSCode REST Client)

Arquivos HTTP para testes manuais da API usando a extensão [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) do VSCode.

## Quick Start

1. Instale a extensão [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client).
2. Abra [`auth.http`](./auth.http) e execute o request **register** e depois **login** (clique em "Send Request").
3. Copie o `accessToken` da resposta e cole no `@token` no topo do arquivo.
4. Faça o mesmo nos outros `.http` que for usar — cada arquivo possui suas variáveis `@token`, `@mediaId`, etc.
5. Para operações admin: execute **loginAdmin** em `auth.http` e cole em `@adminToken`.
6. Para operações DLQ/OPS: execute **loginOps** e cole em `@opsToken`.
7. Abra qualquer `.http` e clique em **Send Request** acima de qualquer bloco `###`.

> **Dica:** As variáveis de ambiente também estão configuradas em `.vscode/settings.json`.
> Selecione um ambiente via **Ctrl+Shift+P → "Rest Client: Switch Environment"** para sobrescrever os valores padrão.

## Workflow Recomendado

```text
auth.http (login) → media.http (criar mídia) → catalog.http (gêneros, pessoas, créditos, temporadas, episódios)
                                               → activity.http (watch entries, watchlist, progresso)
                                               → discovery.http (busca, popularidade, recomendações, insights)
                                               → admin.http (admin media, DLQ)
                                               → batch.http (sync gêneros → importar filmes → séries → créditos → temporadas)
                                               → health.http (actuator, métricas)
```

## Arquivos

| Arquivo                              | Endpoints Cobertos                                                             | Auth   |
| ------------------------------------ | ------------------------------------------------------------------------------ | ------ |
| [`auth.http`](./auth.http)           | POST /api/auth/login · register · refresh · logout + cenários de erro          | Nenhum |
| [`media.http`](./media.http)         | /api/v1/media CRUD · /api/v1/admin/media · searchUC · busca facetada + textual | Token  |
| [`users.http`](./users.http)         | /api/v1/users CRUD · /me/stats + cenários de permissão                         | Admin  |
| [`catalog.http`](./catalog.http)     | gêneros · pessoas · créditos · temporadas · episódios (CRUD completo cada)     | Token  |
| [`activity.http`](./activity.http)   | watch-entries · watchlist · watch-progress                                     | Token  |
| [`discovery.http`](./discovery.http) | busca · top-rated · trending · most-watched · recomendações · insights         | Misto  |
| [`admin.http`](./admin.http)         | /api/v1/admin/media · /admin/dlq (dead-letter queue) + cenários de permissão   | Admin  |
| [`batch.http`](./batch.http)         | /api/v1/admin/batch/genres · movies · tv-shows · credits · seasons             | Admin  |
| [`health.http`](./health.http)       | /actuator/\* · loggers · caches · /v3/api-docs                                 | Misto  |

## Variáveis de Ambiente

Cada arquivo `.http` define suas variáveis no topo com `@variavel = valor`.
Alternativamente, `.vscode/settings.json` possui `rest-client.environmentVariables` com os ambientes `local`, `docker` e `staging`:

| Variável       | Descrição                                            |
| -------------- | ---------------------------------------------------- |
| `baseUrl`      | URL base da API (padrão: `http://localhost:8080`)    |
| `token`        | Access token de um usuário com role USER             |
| `adminToken`   | Access token de um usuário com role ADMIN            |
| `opsToken`     | Access token de um usuário com role OPS              |
| `userId`       | ID do usuário para requisições user-scoped           |
| `adminUserId`  | ID de um usuário ADMIN                               |
| `mediaId`      | ID de uma mídia para requisições que precisam de uma |
| `genreId`      | ID de um gênero                                      |
| `personId`     | ID de uma pessoa                                     |
| `seasonId`     | ID de uma temporada                                  |
| `episodeId`    | ID de um episódio                                    |
| `creditId`     | ID de um crédito                                     |
| `watchEntryId` | ID de um registro de visualização                    |
| `dlqMessageId` | ID de uma mensagem na dead-letter queue              |

Três ambientes configurados: `local`, `docker` e `staging`.

## Roles

| Role    | Permissões                                                               |
| ------- | ------------------------------------------------------------------------ |
| `USER`  | Watch entries próprios, watchlist própria, perfil próprio, recomendações |
| `ADMIN` | CRUD completo em todos os recursos, gestão de usuários, admin media      |
| `OPS`   | Gerenciamento da dead-letter queue, acesso ao actuator                   |

## DTOs de Referência Rápida

### Campos dos Update DTOs (⚠️ diferem dos Create DTOs)

| DTO                    | Campos                                                                                              |
| ---------------------- | --------------------------------------------------------------------------------------------------- |
| `MediaUpdateRequest`   | title, type, releaseYear, originalTitle, originalLanguage, posterUrl, backdropUrl, overview, tmdbId |
| `GenreUpdateRequest`   | name                                                                                                |
| `PersonUpdateRequest`  | name, birthDate?, placeOfBirth?                                                                     |
| `CreditUpdateRequest`  | role, characterName?, orderIndex? (**sem** mediaId/personId)                                        |
| `SeasonUpdateRequest`  | seasonNumber, name?, airDate? (**sem** mediaId)                                                     |
| `EpisodeUpdateRequest` | episodeNumber, name?, airDate? (**sem** seasonId)                                                   |
| `UserUpdateRequest`    | name                                                                                                |
