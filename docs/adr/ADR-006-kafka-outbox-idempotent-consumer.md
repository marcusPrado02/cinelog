# ADR-006: Kafka + Outbox Pattern + Idempotent Consumer

**Status:** Aceito  
**Data:** 2026-01-04  
**Decisores:** Time de Arquitetura  
**Tags:** #eventos #kafka #outbox #idempotência #mensageria

## Contexto

O sistema CineLog precisa publicar eventos de domínio (watch entries criados, ratings, etc.) para permitir:

-   Processamento assíncrono de side-effects
-   Integração com sistemas externos
-   Event sourcing parcial para auditoria
-   CQRS read models
-   Notificações em tempo real

**Desafios:**

1. Garantir consistência entre persistência do agregado e publicação do evento
2. Evitar perda de eventos em caso de falhas
3. Evitar duplicação no processamento (at-most-once vs at-least-once)
4. Tolerância a falhas da infraestrutura de mensageria

## Decisão

Adotamos a combinação de **3 patterns**:

### 1. Outbox Pattern (Transactional Outbox)

**Implementação:**

-   Tabela `outbox_event` no mesmo banco de dados transacional
-   Domain events são gravados no outbox **na mesma transação** do agregado
-   Job agendado (`OutboxPublisherJob`) processa eventos PENDING
-   Status: PENDING → SENT/FAILED (com retry exponencial backoff)

**Benefícios:**

-   ✅ Consistência transacional garantida (ACID)
-   ✅ At-least-once delivery (evento nunca se perde)
-   ✅ Desacoplamento: Kafka pode estar offline temporariamente
-   ✅ Auditoria completa de eventos publicados

**Trade-offs:**

-   ❌ Overhead de armazenamento (tabela cresce)
-   ❌ Latência adicional (assíncrono via job)
-   ⚠️ Requer housekeeping (limpeza de eventos antigos)

### 2. Kafka como Message Broker

**Implementação:**

-   Topics versionados: `cinelog.{aggregate}.{event}.v{version}`
    -   Exemplo: `cinelog.watchentry.created.v1`
-   Headers com metadata: eventId, aggregateId, eventType, version
-   Producer com `acks=all`, `enable.idempotence=true`, retries
-   Consumer com `manual ack`, `earliest offset`

**Benefícios:**

-   ✅ Throughput alto e baixa latência
-   ✅ Durabilidade via replication factor
-   ✅ Ordem garantida por partition (mesmo aggregateId = mesma partition)
-   ✅ Log imutável para replay/debugging

**Trade-offs:**

-   ❌ Infraestrutura adicional (Zookeeper + Kafka brokers)
-   ❌ Complexidade operacional
-   ⚠️ At-least-once delivery (duplicatas possíveis)

### 3. Idempotent Consumer (Inbox Pattern)

**Implementação:**

-   Tabela `inbox_event` com `eventId` como PK
-   Consumer verifica inbox **antes** de processar
-   Se eventId existe → descarta (duplicata)
-   Se não existe → processa + insere eventId **na mesma transação**

**Benefícios:**

-   ✅ Exactly-once semantics no processamento
-   ✅ Proteção contra re-delivery do Kafka
-   ✅ Proteção contra retry do producer
-   ✅ Simples e confiável

**Trade-offs:**

-   ❌ Overhead de storage (inbox cresce)
-   ❌ Query adicional por mensagem
-   ⚠️ Requer housekeeping

### 4. Dead Letter Queue (DLQ)

**Implementação:**

-   Topic `cinelog.dlq` para eventos que falharam após todos os retries
-   `DefaultErrorHandler` com backoff exponencial
-   Permite análise manual e replay posterior

## Fluxo Completo

```
[UseCase]
   ↓ (1) Salva Agregado + DomainEvent no Outbox (TRANSAÇÃO ACID)
[Database] ← outbox_event (PENDING)
   ↓ (2) Job processa outbox a cada 5s
[OutboxPublisherJob]
   ↓ (3) Publica no Kafka
[Kafka Topic: watchentry.created.v1]
   ↓ (4) Consumer recebe
[WatchEntryCreatedConsumer]
   ↓ (5) Verifica inbox_event (eventId exists?)
   ├─ SIM → descarta (duplicata)
   └─ NÃO → Processa + Insere eventId (TRANSAÇÃO)
[Side-effect: Update Stats, Send Notification, etc]
```

## Alternativas Consideradas

### Alt 1: Publicação Direta (sem Outbox)

```java
// UseCase
repo.save(aggregate);
kafkaTemplate.send(topic, event); // ❌ PROBLEMA: não é transacional
```

**Rejeitado:** Perda de eventos se Kafka falhar após commit DB.

### Alt 2: 2-Phase Commit (2PC)

Transação distribuída entre DB e Kafka.

**Rejeitado:**

-   Kafka não suporta 2PC nativamente
-   Complexidade extrema
-   Performance ruim

### Alt 3: Change Data Capture (CDC - Debezium)

Lê WAL/binlog do MySQL e publica no Kafka.

**Rejeitado (por enquanto):**

-   Maior complexidade operacional
-   Menos controle sobre formato de eventos
-   Bom para futuro (event sourcing completo)

### Alt 4: Event Sourcing Completo

Aggregates reconstituídos a partir de eventos.

**Rejeitado (por enquanto):**

-   Mudança arquitetural muito grande
-   Complexidade de leitura (projections)
-   Considerar no futuro se CQRS evoluir

## Consequências

### Positivas

✅ **Confiabilidade:** Nenhum evento se perde (outbox + Kafka durability)  
✅ **Idempotência:** Processamento exactly-once garantido (inbox)  
✅ **Auditoria:** Log completo de eventos no outbox e Kafka  
✅ **Escalabilidade:** Kafka distribui carga entre consumers  
✅ **Desacoplamento:** Produtores e consumidores independentes  
✅ **Tolerância a Falhas:** Retry automático com backoff + DLQ

### Negativas

❌ **Complexidade:** 3 patterns + infraestrutura Kafka  
❌ **Latência:** Eventos processados assincronamente (5s delay configurável)  
❌ **Storage:** Tabelas outbox + inbox crescem (requer housekeeping)  
❌ **Operacional:** Monitorar Kafka, job do outbox, consumidores

### Mitigações

-   **Housekeeping:** Job diário remove eventos SENT > 7 dias
-   **Monitoramento:** Métricas de lag, failed events, DLQ size
-   **Alertas:** OutboxPublisherJob com circuit breaker se Kafka offline
-   **Observabilidade:** Logs estruturados + tracing distribuído

## Configuração

```yaml
outbox:
    publisher:
        enabled: true
        fixed-rate-ms: 5000 # Processa a cada 5s
        batch-size: 100
        retention-days: 7

spring.kafka:
    bootstrap-servers: localhost:9092
    producer:
        acks: all
        retries: 3
        enable.idempotence: true
    consumer:
        group-id: cinelog-consumer-group
        enable-auto-commit: false
```

## Referências

-   [Outbox Pattern - Microservices.io](https://microservices.io/patterns/data/transactional-outbox.html)
-   [Idempotent Consumer - Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/patterns/messaging/IdempotentReceiver.html)
-   [Kafka Documentation](https://kafka.apache.org/documentation/)
-   [Chris Richardson - Microservices Patterns](https://www.manning.com/books/microservices-patterns)

## Notas

-   Versionar eventos (v1, v2...) para permitir evolução sem breaking changes
-   Considerar schema registry (Confluent, Apicurio) para governança de schemas
-   Avaliar CDC (Debezium) no futuro para event sourcing completo
-   Monitorar tamanho das tabelas outbox/inbox e ajustar housekeeping
