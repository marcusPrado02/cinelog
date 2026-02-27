# 🔒 Security — OWASP Top 10:2025

> Implementação completa do OWASP Top 10:2025 com Spring Security no CineLog.

---

## Visão Geral

O CineLog implementa **todos os 10 itens** do OWASP Top 10:2025 como camada de segurança defense-in-depth. Cada categoria foi estudada, analisada quanto a gaps no projeto e implementada com código + documentação.

### Stack de Segurança

| Componente | Tecnologia |
|---|---|
| **Autenticação** | JWT (HMAC-SHA256) via jjwt 0.12.6 |
| **Autorização** | Spring Security 6.x + `@PreAuthorize` |
| **Hashing** | BCrypt (cost factor 12) |
| **Sessions** | STATELESS (sem sessões) |
| **CORS** | Configuração por profile |
| **Rate Limiting** | Filtro customizado (Bucket4j pattern) |
| **Supply Chain** | OWASP Dependency-Check + CycloneDX SBOM |
| **Resiliência** | Resilience4j (Circuit Breaker, Retry, Bulkhead) |

---

## OWASP Top 10:2025 — Status

| # | Categoria | Status | Resumo |
|---|---|---|---|
| **A01** | Broken Access Control | ✅ | `@PreAuthorize`, ownership checks, CORS strict |
| **A02** | Security Misconfiguration | ✅ | Headers seguros, stack traces suprimidos, actuator restrito |
| **A03** | Software Supply Chain Failures | ✅ | Dependency-Check, CycloneDX SBOM, Enforcer, CVE fixes |
| **A04** | Cryptographic Failures | ✅ | BCrypt(12), JWT HMAC-SHA, TLS, sem dados sensíveis em logs |
| **A05** | Injection | ✅ | JPA prepared statements, SQLi filter, input validation |
| **A06** | Insecure Design | ✅ | Rate limiting, anti-enumeration, business limits, Secure-by-Design |
| **A07** | Authentication Failures | ✅ | Account lockout, password policy, refresh token rotation |
| **A08** | Software or Data Integrity Failures | ✅ | HMAC integrity check, optimistic locking, secure action tokens |
| **A09** | Security Logging and Alerting | ✅ | SecurityEventLogger, alerting service, dedicated SECURITY appender |
| **A10** | Mishandling of Exceptional Conditions | ✅ | Circuit breaker, fallbacks, health indicators, JVM safety hooks |

---

## A01:2025 — Broken Access Control

### O que foi implementado

- **`@PreAuthorize`** em todos os endpoints sensíveis
- **Ownership checks**: usuário só acessa seus próprios dados
- **CORS estrito**: apenas origens configuradas por profile
- **Method-level security**: `@EnableMethodSecurity` habilitado
- **SecurityFilterChain com `@Order`**: múltiplas chains para públicas e privadas

### Exemplo

```java
@PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
public UserResponse getUser(Long userId) { ... }
```

---

## A02:2025 — Security Misconfiguration

### O que foi implementado

- **Stack traces suprimidos** em produção (`server.error.include-stacktrace: never`)
- **Headers seguros**: `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`
- **Actuator restrito**: apenas `health`, `info`, `prometheus` expostos
- **Health details ocultos**: `show-details: never`
- **Server header removido**: `server.server-header: ""`

---

## A03:2025 — Software Supply Chain Failures

### O que foi implementado

- **OWASP Dependency-Check** (plugin Maven v11.1.1): verifica CVEs em dependências
- **CycloneDX SBOM**: gera BOM de componentes em formato CycloneDX
- **Maven Enforcer**: exige Java 21+ e Maven 3.9+
- **Dependency Management overrides**: fixes para jose4j, lz4-java, commons-beanutils, commons-compress
- **Trusted packages** no Kafka consumer: whitelist explícita

---

## A04:2025 — Cryptographic Failures

### O que foi implementado

- **BCrypt** com cost factor 12 para hashing de senhas
- **JWT HMAC-SHA256** com chave ≥32 caracteres via variável de ambiente
- **Dados sensíveis nunca logados** (passwords, tokens, PIIs mascarados)
- **TLS recomendado** em produção (documentado no deployment guide)

---

## A05:2025 — Injection

### O que foi implementado

- **JPA Prepared Statements**: todas as queries via `@Query` com parâmetros nomeados
- **SqlInjectionFilter**: filtro customizado que detecta patterns de SQLi em parameters
- **Input validation**: `@Valid` + Bean Validation em todos os DTOs
- **Content-Type enforcement**: apenas `application/json` aceito

---

## A06:2025 — Insecure Design

### O que foi implementado

- **Rate limiting** por IP/usuário com thresholds configuráveis
- **Anti-enumeração**: login e registro retornam mensagens genéricas
- **Business limits**: `BusinessLimitExceededException` → 429
- **Threat modeling**: design review documentado

---

## A07:2025 — Authentication Failures

### O que foi implementado

- **Account lockout**: 5 tentativas → bloqueio de 15 minutos
- **Password policy**: mínimo 8 chars, maiúscula/minúscula/número/especial, dicionário de senhas comuns
- **Refresh token rotation**: token de uso único, family-based revocation
- **Token expiration**: access token 1h, refresh token 7 dias

---

## A08:2025 — Software or Data Integrity Failures

### O que foi implementado

- **HMAC integrity check**: `IntegrityService` verifica integridade de entidades críticas
- **Optimistic locking**: `@Version` em entidades com alta concorrência
- **Secure action tokens**: tokens de ação single-use com HMAC
- **Desserialização segura**: Jackson sem `@JsonTypeInfo` polymorphic

---

## A09:2025 — Security Logging and Alerting Failures

### O que foi implementado

- **SecurityEventLogger**: logging estruturado de eventos de segurança com Markers
- **SecurityAlertService**: alertas baseados em thresholds (auth failures, SQLi, tamper)
- **SECURITY appender**: arquivo dedicado com retenção de 90 dias
- **Métricas Micrometer**: contadores por tipo de evento de segurança
- **DataAccessAuditAspect**: auditoria de acesso a dados sensíveis

---

## A10:2025 — Mishandling of Exceptional Conditions

### O que foi implementado

- **Resilience4j completo**: Circuit Breaker + Retry + TimeLimiter + Bulkhead para TMDb
- **Fallback methods**: todos os 11 métodos do TmdbClientAdapter com degradação graciosa
- **GlobalExceptionHandler expandido**: handlers para `CallNotPermittedException`, `BulkheadFullException`, `WebClientResponseException`, `DataAccessException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`
- **Scheduler error handler**: `SchedulingConfig` com `ErrorHandler` global
- **Health indicators**: `TmdbHealthIndicator` (circuit breaker + probe) e `OutboxHealthIndicator` (pendentes + falhas)
- **JVM safety**: `JvmSafetyConfig` com uncaught exception handler e shutdown hook

---

## Filtros de Segurança (Pipeline)

```
Request
  │
  ├─→ ObservabilityContextFilter (correlationId, MDC)
  ├─→ RateLimitFilter (100/min auth, 20/min unauth)
  ├─→ SqlInjectionFilter (pattern detection)
  ├─→ JwtAuthenticationFilter (token validation)
  ├─→ Spring Security FilterChain
  │     ├─→ CORS
  │     ├─→ CSRF (disabled - stateless)
  │     └─→ Authorization
  └─→ Controller
```

---

## Referências

- [OWASP Top 10:2025](https://owasp.org/Top10/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)

> 📄 Documentação completa (3.900+ linhas) no código-fonte: `docs/SECURITY.md`
