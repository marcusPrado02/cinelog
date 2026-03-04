# Threat Model — CineLog API (STRIDE)

**Versão:** 1.0  
**Data:** 2026-03-03  
**Responsável:** Equipe CineLog  
**Revisão:** Semestral

---

## 1. Escopo e Arquitetura

```
┌─────────────┐   HTTPS    ┌────────────────────────────┐
│   Client    │───────────▶│  API Gateway / Load Balancer│
│ (Browser /  │            └────────────┬───────────────┘
│  Mobile)    │                         │ JWT Bearer
└─────────────┘                         ▼
                            ┌───────────────────────┐
                            │  Spring Boot REST API  │
                            │  (cinelog-app)         │
                            └──┬──────┬──────┬──────┘
                               │      │      │
                        ┌──────▼─┐ ┌──▼───┐ ┌▼────────┐
                        │ MySQL  │ │Redis │ │  Kafka  │
                        │  (DB)  │ │Cache │ │(Events) │
                        └────────┘ └──────┘ └─────────┘
                               │
                        ┌──────▼─────────┐
                        │  Observability  │
                        │ (Grafana/Loki/  │
                        │  Prometheus/    │
                        │   Tempo)        │
                        └────────────────┘
```

### Trust Boundaries

| Boundary                      | Descrição                                                  |
| ----------------------------- | ---------------------------------------------------------- |
| **Internet → API Gateway**    | Tráfego externo não confiável — TLS obrigatório            |
| **API Gateway → cinelog-app** | Rede interna — JWT já validado                             |
| **cinelog-app → MySQL/Redis** | Rede privada Docker — sem autenticação mútua (mTLS futuro) |
| **cinelog-app → Kafka**       | Rede interna — producer/consumer autenticados por senha    |

---

## 2. Ativos a Proteger

| Ativo                                      | Classificação | Impacto se comprometido |
| ------------------------------------------ | ------------- | ----------------------- |
| Credenciais de usuário                     | Crítico       | Account takeover        |
| JWT secret key                             | Crítico       | Forge de tokens         |
| Dados pessoais (email, histórico de mídia) | Sensível      | Violação LGPD           |
| MySQL password                             | Crítico       | Dump total do banco     |
| Kafka credentials                          | Alto          | Injeção de eventos      |
| Logs / traces                              | Médio         | Information disclosure  |

---

## 3. Análise STRIDE

### S — Spoofing (Falsificação de Identidade)

| ID  | Ameaça                                                                | Componente              | Probabilidade | Impacto | Risco |
| --- | --------------------------------------------------------------------- | ----------------------- | ------------- | ------- | ----- |
| S1  | Attacker forja JWT com `alg: none` ou chave fraca                     | JwtAuthenticationFilter | Baixa         | Crítico | Alto  |
| S2  | Credential stuffing / brute force no `/api/auth/login`                | AuthController          | Média         | Alto    | Alto  |
| S3  | Impersonation via user enumeration (login retorna mensagem diferente) | AuthController          | Média         | Médio   | Médio |

**Mitigações implementadas:**

- **S1**: `jjwt 0.12.6` com HS256 e chave de 256 bits; `alg: none` rejeitado por padrão; segredo via variável de ambiente — `JwtTokenService`
- **S2**: `LoginAttemptService` bloqueia após 5 falhas; `RateLimitFilter` (100 req/min por IP) — ver `SecurityConfig`
- **S3**: `AntiEnumerationService` retorna resposta uniforme independente de login não encontrado vs. senha errada

**Risco residual S1:** Baixo. **S2/S3:** Baixo após mitigação.

---

### T — Tampering (Adulteração de Dados)

| ID  | Ameaça                                                      | Componente                           | Probabilidade | Impacto | Risco |
| --- | ----------------------------------------------------------- | ------------------------------------ | ------------- | ------- | ----- |
| T1  | SQL Injection via parâmetros de query                       | Todos controllers                    | Média         | Crítico | Alto  |
| T2  | Adulteração de payload JWT (altering claims in-transit)     | JwtAuthenticationFilter              | Baixa         | Crítico | Médio |
| T3  | Mensagem Kafka adulterada (DLQ replay de evento forjado)    | DeadLetterService                    | Baixa         | Alto    | Médio |
| T4  | Manipulação de IDs em path/query para acessar dados alheios | UserController, WatchEntryController | Média         | Alto    | Alto  |
| T5  | Mass assignment via campos extras no request body           | Todos controllers                    | Baixa         | Médio   | Baixo |

**Mitigações implementadas:**

- **T1**: `SqlInjectionFilter` bloqueia padrões SQL conhecidos; JPA com parâmetros `?` (prepared statements) em todas as queries — `SqlInjectionFilter`
- **T2**: Assinatura HMAC-SHA256; qualquer alteração invalida a assinatura — `JwtTokenService`
- **T3**: `KafkaEventIntegrityVerifier` valida HMAC dos eventos; `DeadLetterAdminController` exige role ADMIN/OPS — `@PreAuthorize`
- **T4**: `@PostAuthorize` verifica ownership; `@PreAuthorize` restringe operações de admin — `UserController`, `WatchEntryController`
- **T5**: DTOs com `@JsonIgnoreProperties(ignoreUnknown = true)` e `@Valid` restringem campos aceitos

**Risco residual T1/T4:** Baixo. **T3:** Médio (monitoramento necessário).

---

### R — Repudiation (Repúdio)

| ID  | Ameaça                                                             | Componente        | Probabilidade | Impacto | Risco |
| --- | ------------------------------------------------------------------ | ----------------- | ------------- | ------- | ----- |
| R1  | Usuário nega ter realizado uma ação sensível (delete, atualização) | Todos controllers | Média         | Médio   | Médio |
| R2  | Admin nega ter alterado dados em produção                          | Admin controllers | Baixa         | Alto    | Médio |

**Mitigações implementadas:**

- **R1/R2**: `@AuditableAction` registra `userId`, `action`, `module`, `timestamp`, `remoteIp` em log estruturado JSON correlacionado com `traceId`/`spanId` — `AuditableAction`, `ObservabilityContextFilter`
- Logs persistidos no Loki (Grafana Stack) com retenção de 90 dias

**Risco residual:** Baixo (logs auditáveis e imutáveis via append-only no Loki).

---

### I — Information Disclosure (Exposição de Informação)

| ID  | Ameaça                                                      | Componente             | Probabilidade | Impacto | Risco |
| --- | ----------------------------------------------------------- | ---------------------- | ------------- | ------- | ----- |
| I1  | Stack trace exposto na resposta de erro                     | GlobalExceptionHandler | Baixa         | Médio   | Baixo |
| I2  | Dados sensíveis em logs (senha, token)                      | Todos                  | Média         | Alto    | Alto  |
| I3  | Resposta de `/api/v1/users` revela dados de outros usuários | UserController         | Média         | Médio   | Médio |
| I4  | Actuator endpoints expostos publicamente                    | ActuatorSecurityConfig | Baixa         | Alto    | Médio |
| I5  | JWT payload decodificável — claims sensíveis expostos       | JwtTokenService        | Alta          | Médio   | Médio |

**Mitigações implementadas:**

- **I1**: `GlobalExceptionHandler` retorna `ProblemDetail` sem stack trace; perfil `prod` desabilita detalhes de exceção
- **I2**: `SensitiveDataMasker` mascara senhas/tokens nos logs; MDC não armazena credenciais
- **I3**: `@PostAuthorize("hasRole('ADMIN') or returnObject.body.email == authentication.name")` impede que usuário veja dados de outro
- **I4**: `ActuatorSecurityConfig (@Order 1)` restringe `/actuator/**` a roles `ACTUATOR`/`OPS`
- **I5**: Claims JWT não contêm dados sensíveis (apenas `sub: email`, `userId`, `role`); payload não é criptografado (by design no JWT)

**Risco residual I2:** Médio (dependente de disciplina nos logs). **I5:** Aceito (JWT payload é padrão público).

---

### D — Denial of Service (Negação de Serviço)

| ID  | Ameaça                                                          | Componente           | Probabilidade | Impacto | Risco |
| --- | --------------------------------------------------------------- | -------------------- | ------------- | ------- | ----- |
| D1  | Flood de requisições no endpoint de login                       | AuthController       | Alta          | Alto    | Alto  |
| D2  | Payload gigante no corpo da requisição (body flooding)          | Todos controllers    | Média         | Médio   | Médio |
| D3  | Slow HTTP attack (Slowloris)                                    | Spring Boot / Tomcat | Baixa         | Alto    | Médio |
| D4  | Kafka consumer lag — mensagens poison-pill travam processamento | Kafka Consumer       | Baixa         | Médio   | Baixo |

**Mitigações implementadas:**

- **D1**: `RateLimitFilter` (100 req/min por IP, 429 Too Many Requests) + `LoginAttemptService` (lockout por username)
- **D2**: `spring.servlet.multipart.max-request-size=10MB` e `server.tomcat.max-http-form-post-size` configurados
- **D3**: `server.tomcat.connection-timeout` e `keep-alive-timeout` no `application.yml`
- **D4**: `@RetryableTopic` + `DeadLetterService` isolam mensagens com falha no processamento

**Risco residual D1:** Baixo. **D3:** Médio (controle de rede/infra necessário além do app).

---

### E — Elevation of Privilege (Escalada de Privilégio)

| ID  | Ameaça                                                          | Componente                           | Probabilidade | Impacto | Risco |
| --- | --------------------------------------------------------------- | ------------------------------------ | ------------- | ------- | ----- |
| E1  | Usuário altera o próprio campo `role` via endpoint de update    | UserController                       | Média         | Crítico | Alto  |
| E2  | Bypass de `@PreAuthorize` por invocação direta de service       | Services internos                    | Baixa         | Alto    | Médio |
| E3  | IDOR — acesso a recursos de outros usuários via ID manipulation | UserController, WatchEntryController | Média         | Alto    | Alto  |
| E4  | JWT com role elevado forjado (se chave comprometida)            | JwtTokenService                      | Baixa         | Crítico | Alto  |

**Mitigações implementadas:**

- **E1**: DTO de atualização (`UserUpdateRequest`) NÃO inclui campo `role`; campo ignorado por design; role só mutável via admin endpoint
- **E2**: `@EnableMethodSecurity(proxyTargetClass = true)` garante proxy AOP; advice não bypassável por chamada direta ao bean (Spring proxy) — mas chamadas entre beans do mesmo contexto não passam pelo proxy (limitação do Spring AOP documentada)
- **E3**: Dupla proteção: URL filter (`/api/v1/admin/**`) + `@PreAuthorize`/`@PostAuthorize` no método
- **E4**: JWT secret ≥256 bits via env var; rotação semestral recomendada; sem persistência do secret em código

**Risco residual E1:** Baixo. **E2:** Médio (documentar como restrição arquitetural — não chamar services privilegiados entre si sem passar pela camada de segurança). **E4:** Baixo dado tamanho da chave.

---

## 4. Resumo de Riscos

```
        IMPACTO
CRÍTICO │ S1   T2       │ E4
ALTO    │ S2 T1 T4   I2 │ E1 E3
MÉDIO   │ S3 T3 R1 I3 D3│ E2
BAIXO   │    T5 I1   D4 │
        └────────────────────
          BAIXA  MÉDIA  ALTA
                PROBABILIDADE
```

| Risco Total | Contagem |
| ----------- | -------- |
| **Alto**    | 6        |
| **Médio**   | 10       |
| **Baixo**   | 5        |

---

## 5. Ameaças Aceitas / Fora de Escopo

| Ameaça                                         | Justificativa                                                 |
| ---------------------------------------------- | ------------------------------------------------------------- |
| Ataques físicos ao servidor                    | Responsabilidade da infraestrutura / cloud provider           |
| Supply chain attack (código malicioso em deps) | Parcialmente mitigado por Dependabot + OWASP Dependency Check |
| Insider threat (admin malicioso)               | Auditoria de logs + processo de RH fora do escopo técnico     |
| Zero-day em Spring Boot / JVM                  | Dependente de patch management                                |

---

## 6. Ações Pendentes (Backlog de Segurança)

| Prioridade | Ação                                                                               | Responsável | Prazo    |
| ---------- | ---------------------------------------------------------------------------------- | ----------- | -------- |
| P1         | Implementar refresh token com rotação (mitigar S1 residual em tokens longos)       | Dev         | Sprint 3 |
| P1         | Habilitar mTLS entre `cinelog-app` ↔ MySQL em produção                             | Infra       | Sprint 4 |
| P2         | Adicionar `Content-Security-Policy` header na resposta (mitigar XSS em Swagger UI) | Dev         | Sprint 3 |
| P2         | Testar E2 (bypass de @PreAuthorize via self-invocation) e documentar restrição     | Dev         | Sprint 2 |
| P3         | Avaliar MFA (TOTP) para contas ADMIN                                               | Dev/Product | Sprint 5 |
| P3         | Integrar OWASP ZAP no pipeline CI/CD para DAST                                     | Dev/Infra   | Sprint 4 |

---

## 7. Referências

- STRIDE: [Microsoft Threat Modeling](https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats)
- OWASP Top 10 2021
- [ADR-011: Estratégia de Autenticação](../adr/ADR-011-authentication-strategy.md)
- [ADR-012: Modelo de Autorização](../adr/ADR-012-authorization-model.md)
- `SecurityConfig.java`, `JwtTokenService.java`, `RateLimitFilter.java`, `SqlInjectionFilter.java`
