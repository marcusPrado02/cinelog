# ADR-013: Estratégia de Versionamento de API

## Status

✅ **Aceito**

## Data

2026-03-03

## Contexto

À medida que o CineLog evolui, endpoints precisarão de mudanças incompatíveis
(`breaking changes`) — novos campos obrigatórios, renomeação de propriedades,
mudança de contrato de resposta. Sem uma estratégia explícita, clientes existentes
(web, mobile, integrações) quebrariam a cada deploy.

### Tipos de mudança que requerem versionamento

| Tipo de mudança                         | Breaking? |
| --------------------------------------- | --------- |
| Adicionar campo opcional na resposta    | ❌ Não    |
| Remover campo da resposta               | ✅ Sim    |
| Renomear campo                          | ✅ Sim    |
| Alterar tipo de um campo                | ✅ Sim    |
| Mudança em regra de negócio do endpoint | Depende   |
| Novo endpoint                           | ❌ Não    |

## Decisão

### Estratégia: Versionamento por URL (Path Versioning)

```text
/api/v{N}/recurso
```

Exemplos ativos:

```text
GET  /api/v1/media
POST /api/v1/admin/media
GET  /api/v1/users
GET  /api/v1/watch-entries
```

### Justificativa da escolha

| Abordagem                     | Prós                                    | Contras                            |
| ----------------------------- | --------------------------------------- | ---------------------------------- |
| **Path Versioning (adotado)** | Explícito, cacheável, fácil de roteador | URLs longas                        |
| Header Versioning             | URLs limpas                             | Difícil de testar no browser/curl  |
| Media Type Versioning         | Padrão REST puro                        | Alta complexidade de implementação |
| Query Param (?v=1)            | Simples                                 | Não cacheável por proxies; ruidoso |

A escolha de **Path Versioning** se alinha com:

- Facilidade de log/rastreamento por version (Logstash, Loki, Splunk)
- Roteamento explícito no API Gateway / Load Balancer
- Geração de documentação OpenAPI por grupo de versão (`springdoc-openapi`)

### Regras de versionamento

#### 1. Quando criar uma nova versão

Crie `/api/v2/...` quando houver uma **breaking change**:

- Remoção ou renomeação de campo em request/response DTO
- Mudança de semântica de um contrato existente
- Alteração de tipo de parâmetro obrigatório

#### 2. Ciclo de vida de versões

```text
v1 (CURRENT)  →  v2 (NEW)  →  v1 (DEPRECATED)  →  v1 (SUNSET)
                               6 meses aviso         + 3 meses
```

| Estado       | Descrição                                             |
| ------------ | ----------------------------------------------------- |
| `CURRENT`    | Versão recomendada; recebe novas features             |
| `DEPRECATED` | Mantida para retrocompatibilidade; sem novas features |
| `SUNSET`     | Retorna HTTP 410 Gone com header `Sunset: <data ISO>` |

#### 3. Headers de comunicação

Versões deprecated devem retornar:

```http
Deprecation: true
Sunset: Sat, 01 Jan 2027 00:00:00 GMT
Link: <https://api.cinelog.com/api/v2/media>; rel="successor-version"
```

#### 4. Documentação OpenAPI por versão

```java
// OpenApiConfig.java — grupos separados por versão
@Bean
public GroupedOpenApi v1Api() {
    return GroupedOpenApi.builder()
        .group("v1")
        .pathsToMatch("/api/v1/**")
        .build();
}
```

### URLs de administração e infraestrutura

Endpoints `/admin/**` e `/actuator/**` são versionados implicitamente pelo produto
(não seguem o prefixo `/api/vN`), pois são consumidos apenas por operadores internos.

### Compatibilidade com autenticação

JWT tokens são agnósticos de versão — um token válido serve qualquer versão ativa da API.
A lógica de autorização (roles) é idêntica entre versões.

## Alternativas Consideradas

| Alternativa                      | Rejeitado por                                         |
| -------------------------------- | ----------------------------------------------------- |
| Sem versionamento de API         | Qualquer mudança quebraria clientes sem aviso         |
| Versionamento por módulo/feature | Inconsistência; complexidade de roteamento            |
| GraphQL (sem versionamento)      | Fora do escopo do projeto atual; REST já estabelecido |

## Consequências

### Positivas

- Clientes têm garantia de estabilidade enquanto `v1` estiver `CURRENT`
- Logs de acesso indicam qual versão está sendo usada (observabilidade)
- OpenAPI documenta cada versão separadamente no Swagger UI

### Negativas / Trade-offs

- Manutenção de múltiplas versões simultaneamente aumenta carga de desenvolvimento
- Controllers duplicados temporariamente durante migração de versão

## Estratégia de Evolução de DTOs

### Organização de pacotes por versão

Quando uma breaking change exige `/api/v2`, os DTOs da nova versão ficam em
subpacotes separados para evitar colisão de nomes:

```text
features/media/web/dto/
├── v1/
│   ├── MediaResponse.java        ← versão atual (CURRENT)
│   └── MediaCreateRequest.java
└── v2/
    ├── MediaResponseV2.java      ← nova versão (durante migração)
    └── MediaCreateRequestV2.java
```

Os controllers v1 e v2 coexistem temporariamente:

```java
// v1 — mantido enquanto DEPRECATED
@RequestMapping("/api/v1/media")
public class MediaControllerV1 { ... }

// v2 — nova versão CURRENT
@RequestMapping("/api/v2/media")
public class MediaControllerV2 { ... }
```

### Regras para DTOs

| Mudança em DTO                      | Ação necessária          |
|-------------------------------------|--------------------------|
| Adicionar campo opcional            | Sem nova versão          |
| Remover campo                       | Nova versão obrigatória  |
| Renomear campo                      | Nova versão obrigatória  |
| Alterar tipo (ex: String → Integer) | Nova versão obrigatória  |
| Adicionar campo obrigatório         | Nova versão obrigatória  |

### Nomenclatura

- DTOs de v1 (versão atual) mantêm nome simples: `MediaResponse`
- DTOs de versões futuras usam sufixo: `MediaResponseV2`
- Após sunset de v1, `MediaResponseV2` pode ser renomeado para `MediaResponse`
  em refactoring interno (sem impacto nos clientes)

### Deprecação via OpenAPI

```java
@Schema(deprecated = true, description = "Deprecated: use /api/v2/media")
public record MediaResponse(...) { }
```

## Referências

- [ADR-001: Arquitetura Hexagonal](./ADR-001-arquitetura-hexagonal.md)
- REST API Versioning — Roy Fielding
- [Microsoft REST API Guidelines — Versioning](https://github.com/microsoft/api-guidelines)
- `OpenApiConfig.java`, `SecurityConfig.java`
