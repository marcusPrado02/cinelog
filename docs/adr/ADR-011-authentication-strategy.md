# ADR-011: Estratégia de Autenticação

## Status

✅ **Aceito**

## Data

2026-03-03

## Contexto

Com a base do projeto estabelecida (ver [ADR-005](./ADR-005-jwt-authentication.md) para detalhes da
implementação JWT), foi necessário formalizar a **estratégia completa de autenticação** da aplicação,
abrangendo não apenas o mecanismo de token, mas também:

- Proteção de credenciais em trânsito e em repouso
- Política de senhas
- Mecanismos complementares (Basic Auth para Actuator/Healthcheck)
- Defesa contra ataques de força bruta e enumeração de usuários

### Requisitos de Negócio

| Requisito                    | Prioridade |
| ---------------------------- | ---------- |
| Autenticação stateless       | Must Have  |
| Proteção contra brute force  | Must Have  |
| Senhas armazenadas com hash  | Must Have  |
| Token com tempo de expiração | Must Have  |
| Refresh token sem re-login   | Should     |
| MFA (2FA)                    | Could      |

## Decisão

### 1. Camadas de autenticação

```
┌──────────────────────────────────────────────────────────┐
│  Camada 1: TLS/HTTPS obrigatório (HSTS max-age=31536000) │
│  Camada 2: Rate limiting (RateLimitFilter, 100 req/min)  │
│  Camada 3: SQL Injection screening (SqlInjectionFilter)  │
│  Camada 4: JWT stateless via JwtAuthenticationFilter     │
│  Camada 5: BCrypt(12) para passwords em repouso         │
└──────────────────────────────────────────────────────────┘
```

### 2. Fluxo de Autenticação

```
POST /api/auth/login
  → LoginAttemptService.check() (bloqueio após N falhas)
  → DaoAuthenticationProvider (buscaUserDetailsService + BCrypt.matches)
  → JwtTokenService.generateToken() → { accessToken, refreshToken }

Requisição autenticada:
  → RateLimitFilter
  → SqlInjectionFilter
  → JwtAuthenticationFilter (verifica assinatura + expiração)
  → SecurityContextHolder.getContext().setAuthentication(...)
  → Controller / UseCase
```

### 3. Política de Senhas (`PasswordPolicyValidator`)

| Critério              | Valor     |
| --------------------- | --------- |
| Comprimento mínimo    | 8 chars   |
| Maiúscula obrigatória | ✅        |
| Dígito obrigatório    | ✅        |
| Caractere especial    | ✅        |
| Hash algorithm        | BCrypt 12 |

### 4. Proteção contra Enumeração (`AntiEnumerationService`)

- Resposta de login unificada (sucesso e falha retornam mesmo formato)
- Delay constante para evitar timing attacks
- `LoginAttemptService`: bloqueio após 5 tentativas falhas (lockout por IP + username)

### 5. Basic Auth (Actuator)

O endpoint `/actuator/**` é protegido por `@Order(1) ActuatorSecurityConfig`
com `HttpBasicConfigurer` restrito à rede interna e roles `ACTUATOR` / `OPS`.

## Alternativas Consideradas

| Alternativa            | Rejeitado por                                                     |
| ---------------------- | ----------------------------------------------------------------- |
| Sessão HTTP (stateful) | Não escala horizontalmente; requer sticky session ou shared store |
| OAuth2 / OIDC          | Complexidade de infraestrutura excessiva na fase atual            |
| API Keys               | Sem granularidade de role; difícil de revogar por usuário         |

## Consequências

### Positivas

- Stateless: sem necessidade de session store para escalar
- BCrypt 12 + HSTS + Rate Limit: conformidade com OWASP Top 10 A02/A04/A07
- `AntiEnumerationService` dificulta user enumeration attacks

### Negativas / Trade-offs

- JWT não pode ser revogado antes da expiração (mitigado por access token de curta duração)
- Cada requisição recalcula BCrypt na validação de Basic Auth do Actuator

## Referências

- [ADR-005: JWT para Autenticação Stateless](./ADR-005-jwt-authentication.md)
- OWASP Authentication Cheat Sheet
- NIST SP 800-63B (Digital Identity Guidelines)
- `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `LoginAttemptService.java`
