# Catálogo de Eventos - CineLog

**Versão**: 1.0  
**Data**: 04/01/2026  
**Propósito**: Documentar todos os eventos Kafka publicados pelo CineLog

---

## 📋 Índice

-   [Visão Geral](#visão-geral)
-   [Formato Padrão (EventEnvelope)](#formato-padrão-eventenvelope)
-   [Eventos de WatchEntry](#eventos-de-watchentry)
-   [Eventos de Media](#eventos-de-media)
-   [Eventos de User](#eventos-de-user)
-   [Convenções de Nomenclatura](#convenções-de-nomenclatura)
-   [Versionamento](#versionamento)

---

## Visão Geral

Todos os eventos do CineLog seguem o padrão **EventEnvelope** que garante:

-   ✅ **Rastreabilidade**: eventId único (UUID)
-   ✅ **Versionamento**: schema version explícito
-   ✅ **Observabilidade**: correlationId, causationId, traceparent
-   ✅ **Auditoria**: occurredAt (timestamp UTC)
-   ✅ **Origem**: producer identificado

---

## Formato Padrão (EventEnvelope)

### Estrutura Base

```json
{
    "event_id": "550e8400-e29b-41d4-a716-446655440000",
    "type": "watch_entry_created",
    "version": 1,
    "occurred_at": "2026-01-04T20:30:45.123Z",
    "producer": "cinelog",
    "metadata": {
        "correlationId": "req-abc-123",
        "causationId": "evt-xyz-789",
        "traceparent": "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01",
        "userId": "42"
    },
    "payload": {
        // Dados específicos do evento
    }
}
```

### Campos Obrigatórios

| Campo         | Tipo     | Descrição                               |
| ------------- | -------- | --------------------------------------- |
| `event_id`    | UUID     | Identificador único do evento           |
| `type`        | String   | Tipo do evento (snake_case)             |
| `version`     | Integer  | Versão do schema do payload (>= 1)      |
| `occurred_at` | ISO 8601 | Timestamp UTC de ocorrência             |
| `producer`    | String   | Sistema produtor ("cinelog")            |
| `payload`     | Object   | Dados do evento (schema varia por tipo) |

### Metadados Opcionais

| Campo                    | Tipo   | Descrição                                 |
| ------------------------ | ------ | ----------------------------------------- |
| `metadata.correlationId` | UUID   | ID para rastrear fluxo de requisição      |
| `metadata.causationId`   | UUID   | ID do evento que causou este              |
| `metadata.traceparent`   | String | W3C Trace Context                         |
| `metadata.userId`        | String | ID do usuário que originou (se aplicável) |

---

## Eventos de WatchEntry

### 1. `watch_entry_created`

**Tópico**: `cinelog.watch-entry.created`  
**Versão**: `1`  
**Descrição**: Disparado quando uma nova entrada é adicionada à watchlist do usuário.

#### Schema do Payload

```json
{
    "watch_entry_id": 123,
    "user_id": 42,
    "media_id": 999,
    "episode_id": null,
    "rating": null,
    "comment": null,
    "watched_at": null,
    "status": "PLANNING",
    "created_at": "2026-01-04T20:30:45.123Z"
}
```

#### Exemplo Completo

```json
{
    "event_id": "a1b2c3d4-e5f6-4a5b-8c7d-9e8f7a6b5c4d",
    "type": "watch_entry_created",
    "version": 1,
    "occurred_at": "2026-01-04T20:30:45.123Z",
    "producer": "cinelog",
    "metadata": {
        "correlationId": "req-user-42-add-watchlist",
        "userId": "42"
    },
    "payload": {
        "watch_entry_id": 123,
        "user_id": 42,
        "media_id": 999,
        "episode_id": null,
        "rating": null,
        "comment": null,
        "watched_at": null,
        "status": "PLANNING",
        "created_at": "2026-01-04T20:30:45.123Z"
    }
}
```

#### Comportamento

-   **Trigger**: POST /watchentries (controller)
-   **Consumidores**:
    -   `WatchEntryCreatedConsumer`: Processa e atualiza estatísticas de usuário
    -   `UserStatsProjection`: Atualiza read model de insights do usuário
-   **Idempotência**: Controlada por `inbox_event` (event_id + consumer_group)

---

### 2. `watch_entry_updated`

**Tópico**: `cinelog.watch-entry.updated`  
**Versão**: `1`  
**Descrição**: Disparado quando uma entrada da watchlist é atualizada (rating, comment, status, etc).

#### Schema do Payload

```json
{
    "watch_entry_id": 123,
    "user_id": 42,
    "media_id": 999,
    "episode_id": null,
    "rating": 8.5,
    "comment": "Excelente série!",
    "watched_at": "2026-01-04",
    "status": "COMPLETED",
    "updated_at": "2026-01-04T20:35:10.456Z",
    "changed_fields": ["rating", "comment", "status"]
}
```

#### Exemplo Completo

```json
{
    "event_id": "b2c3d4e5-f6a7-4b5c-8d7e-9f8a7b6c5d4e",
    "type": "watch_entry_updated",
    "version": 1,
    "occurred_at": "2026-01-04T20:35:10.456Z",
    "producer": "cinelog",
    "metadata": {
        "correlationId": "req-user-42-rate-series",
        "userId": "42"
    },
    "payload": {
        "watch_entry_id": 123,
        "user_id": 42,
        "media_id": 999,
        "rating": 8.5,
        "comment": "Excelente série!",
        "status": "COMPLETED",
        "updated_at": "2026-01-04T20:35:10.456Z",
        "changed_fields": ["rating", "comment", "status"]
    }
}
```

#### Comportamento

-   **Trigger**: PUT /watchentries/{id}
-   **Consumidores**:
    -   `UserStatsProjection`: Atualiza médias e contadores
    -   `MediaPopularityProjection`: Atualiza ratings médios
-   **Idempotência**: Controlada por inbox_event

---

### 3. `watch_entry_deleted`

**Tópico**: `cinelog.watch-entry.deleted`  
**Versão**: `1`  
**Descrição**: Disparado quando uma entrada é removida da watchlist.

#### Schema do Payload

```json
{
    "watch_entry_id": 123,
    "user_id": 42,
    "media_id": 999,
    "deleted_at": "2026-01-04T20:40:00.789Z"
}
```

#### Exemplo Completo

```json
{
    "event_id": "c3d4e5f6-a7b8-4c5d-8e7f-9a8b7c6d5e4f",
    "type": "watch_entry_deleted",
    "version": 1,
    "occurred_at": "2026-01-04T20:40:00.789Z",
    "producer": "cinelog",
    "metadata": {
        "correlationId": "req-user-42-remove-watchlist",
        "userId": "42"
    },
    "payload": {
        "watch_entry_id": 123,
        "user_id": 42,
        "media_id": 999,
        "deleted_at": "2026-01-04T20:40:00.789Z"
    }
}
```

#### Comportamento

-   **Trigger**: DELETE /watchentries/{id}
-   **Consumidores**:
    -   `UserStatsProjection`: Decrementa contadores
    -   `MediaPopularityProjection`: Recalcula ratings

---

## Eventos de Media

### 4. `media_created`

**Tópico**: `cinelog.media.created`  
**Versão**: `1`  
**Descrição**: Disparado quando uma nova mídia é cadastrada no sistema.

#### Schema do Payload

```json
{
    "media_id": 999,
    "title": "Breaking Bad",
    "type": "SERIES",
    "release_year": 2008,
    "genres": "drama,crime,thriller",
    "overview": "A high school chemistry teacher...",
    "vote_average": 9.3,
    "created_at": "2026-01-04T19:00:00.000Z"
}
```

---

### 5. `media_updated`

**Tópico**: `cinelog.media.updated`  
**Versão**: `1`  
**Descrição**: Disparado quando informações de uma mídia são atualizadas.

#### Schema do Payload

```json
{
    "media_id": 999,
    "title": "Breaking Bad",
    "changed_fields": ["vote_average", "overview"],
    "updated_at": "2026-01-04T19:10:00.000Z"
}
```

---

## Eventos de User

### 6. `user_registered`

**Tópico**: `cinelog.user.registered`  
**Versão**: `1`  
**Descrição**: Disparado quando um novo usuário se registra.

#### Schema do Payload

```json
{
    "user_id": 42,
    "username": "john_doe",
    "email": "john@example.com",
    "created_at": "2026-01-04T18:00:00.000Z"
}
```

**Nota**: Email pode ser redacted/hash conforme política de PII.

---

## Convenções de Nomenclatura

### Tipos de Evento

-   **Formato**: `{aggregate}_{action}` (snake_case)
-   **Exemplos**:
    -   `watch_entry_created` ✅
    -   `media_updated` ✅
    -   `user_registered` ✅
    -   `WatchEntryCreated` ❌ (evitar PascalCase em types)

### Tópicos Kafka

-   **Formato**: `cinelog.{aggregate}.{action}` (kebab-case)
-   **Exemplos**:
    -   `cinelog.watch-entry.created` ✅
    -   `cinelog.media.updated` ✅
    -   `cinelog.user.registered` ✅

### Campos do Payload

-   **Formato**: snake_case
-   **Sufixos temporais**: `_at` para timestamps
-   **IDs**: `{entity}_id`

---

## Versionamento

### Regras de Compatibilidade

#### Mudanças Compatíveis (minor - incrementa version)

-   ✅ Adicionar campo **opcional** ao payload
-   ✅ Adicionar novo metadado
-   ✅ Adicionar valor a enum existente
-   ✅ Tornar campo obrigatório em opcional

**Exemplo**: v1 → v2

```json
// v1
{
  "watch_entry_id": 123,
  "user_id": 42
}

// v2 (compatível - novo campo opcional)
{
  "watch_entry_id": 123,
  "user_id": 42,
  "tags": []  // NOVO, opcional
}
```

#### Mudanças Incompatíveis (major - novo tipo de evento)

-   ❌ Remover campo existente
-   ❌ Renomear campo
-   ❌ Mudar tipo de campo
-   ❌ Tornar campo opcional em obrigatório
-   ❌ Remover valor de enum

**Ação**: Criar novo tipo de evento (ex: `watch_entry_created_v2`)

### Estratégia de Consumo

-   **Consumers devem ignorar campos desconhecidos** (forward compatibility)
-   **Consumers devem ter defaults para campos opcionais** (backward compatibility)
-   **Validação de version**: Consumer rejeita versions não suportadas → DLQ

---

## Validação de Envelope

### Regras Obrigatórias

Consumers **DEVEM** validar:

1. ✅ `event_id` não é null e é UUID válido
2. ✅ `type` não é null e não é vazio
3. ✅ `version` > 0
4. ✅ `occurred_at` não é null e é timestamp válido
5. ✅ `producer` = "cinelog"
6. ✅ `payload` não é null

### Tratamento de Erros

-   **Envelope inválido** → Enviar para **DLQ** (`cinelog.dlq`)
-   **Version não suportada** → Enviar para DLQ
-   **Payload inválido** → Enviar para DLQ
-   **Processamento falha** → Retry (backoff exponencial) → DLQ após N tentativas

---

## Referências

-   **EventEnvelope**: `com.cine.cinelog.infrastructure.messaging.events.EventEnvelope`
-   **EventEnvelopeFactory**: `com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeFactory`
-   **Outbox Pattern**: `docs/OUTBOX_PATTERN.md`
-   **W3C Trace Context**: https://www.w3.org/TR/trace-context/

---

**Última Atualização**: 04/01/2026  
**Mantido por**: CineLog Team
