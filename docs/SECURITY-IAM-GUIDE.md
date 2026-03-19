# 🎯 Guia de Apresentação — Segurança & IAM (Semanas 1-2)

> **Propósito:** Documento de referência para apresentação ao senior sobre implementação de segurança e IAM no CineLog.
>
> **Atualizado:** 2025-03-13  
> **Sprints:** Semana 1 (Fundamentos) + Semana 2 (IAM/OAuth2/SSO/MFA)

---

## 📋 Índice

1. [Resumo Executivo](#resumo-executivo)
2. [Semana 1 — Fundamentos de Segurança](#semana-1--fundamentos-de-segurança)
3. [Semana 2 — IAM e Protocolos](#semana-2--iam-e-protocolos)
4. [Scripts de Teste Automatizado](#scripts-de-teste-automatizado)
5. [Métricas & Observabilidade](#métricas--observabilidade)
6. [Checklist para Demo ao Senior](#checklist-para-demo-ao-senior)
7. [Glossário de Conceitos](#glossário-de-conceitos)
8. [Referências Técnicas](#referências-técnicas)

---

## 📋 Resumo Executivo

### Visão Geral

O projeto implementou **arquitetura dual de autenticação** (JWT local HS384 + OAuth2/Keycloak RS256) com foco em segurança OWASP, observabilidade e testes automatizados.

**Entregas principais:**

- ✅ Autenticação JWT local (HS384) com refresh token rotation
- ✅ Method-Level Security com RBAC (@PreAuthorize)
- ✅ OAuth2 + Keycloak (RS256, JWKS validation)
- ✅ SSO (Single Sign-On) via Keycloak realm
- ✅ MFA/TOTP habilitado (Google Authenticator)
- ✅ Authorization Code + PKCE no Swagger
- ✅ SAML design documentado (implementação futura)
- ✅ 42 testes automatizados via script bash
- ✅ 117 classes de teste JUnit (629 métodos @Test)

---

## ✅ Semana 1 — Fundamentos de Segurança

### 1.1. Autenticação JWT Local

**Algoritmo:** HS384 (HMAC-SHA384)  
**Biblioteca:** JJWT (io.jsonwebtoken)  
**Estrutura:** `header.payload.signature`

**Endpoints implementados:**

```
POST /api/auth/register  → Cria conta + retorna tokens
POST /api/auth/login     → Valida credenciais + retorna tokens
POST /api/auth/refresh   → Renova access token usando refresh token
POST /api/auth/logout    → Revoga refresh token
```

**Validações de segurança:**

- ✅ Password policy: ≥8 chars, 1 maiúscula, 1 minúscula, 1 dígito, 1 especial
- ✅ Account lockout: 5 tentativas erradas → bloqueio 15min
- ✅ Refresh token rotation: token antigo revogado ao gerar novo par
- ✅ Bcrypt para hash de senhas (cost factor 12)

**Tempo de vida dos tokens:**
| Token Type | TTL | Storage | Revogável? |
|------------|-----|---------|------------|
| Access Token | 1h | Cliente (JWT stateless) | ❌ Válido até expirar |
| Refresh Token | 30d | Banco de dados | ✅ Delete explícito |

**Código-chave:**

```java
// src/.../security/JwtTokenService.java
public String generateAccessToken(User user) {
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("roles", user.getRoles())
        .issuedAt(new Date())
        .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
        .signWith(getSecretKey(), Jwts.SIG.HS384)
        .compact();
}
```

---

### 1.2. Method-Level Security (RBAC)

**Annotations em uso:**

```java
@PreAuthorize("hasRole('ADMIN')")           // Antes da execução
@PostAuthorize("returnObject.userId == #userId")  // Depois da execução
@Secured("ROLE_ADMIN")                      // Alternativa mais simples
```

**Mapeamento de controllers:**
| Controller | Path | Annotation |
|------------|------|------------|
| `BatchJobController` | `/api/v1/admin/batch` | `@PreAuthorize("hasRole('ADMIN')")` |
| `AdminMediaController` | `/api/v1/admin/media` | `@PreAuthorize("hasRole('ADMIN')")` |
| `DeadLetterAdminController` | `/admin/dlq` | `@PreAuthorize("hasAnyRole('ADMIN', 'OPS')")` |
| `UserController.list()` | `GET /api/v1/users` | `@PreAuthorize("hasRole('ADMIN')")` |
| `UserController.update()` | `PUT /api/v1/users/{id}` | `@PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(#id)")` |

**Fluxo de validação:**

```
Request → FilterChain → JwtAuthFilter → SecurityContext setado
                              ↓
                    Controller method invoked
                              ↓
              Spring AOP intercepta (@PreAuthorize)
                              ↓
       SpEL expression avaliada (hasRole, isOwner, etc)
                              ↓
         ✅ Autorizado → executa método
         ❌ Negado → AccessDeniedException (403)
```

**Teste de segurança:**

```java
@Test
@WithMockUser(roles = "USER")
void userCannotAccessAdminEndpoint() {
    mockMvc.perform(get("/api/v1/admin/batch"))
        .andExpect(status().isForbidden());  // 403
}
```

---

### 1.3. Threat Modeling — STRIDE

**Framework:** Microsoft STRIDE (6 categorias de ameaças)

| Ameaça                     | Exemplo de ataque                              | Mitigação implementada                                                   |
| -------------------------- | ---------------------------------------------- | ------------------------------------------------------------------------ |
| **S**poofing               | Atacante se passa por admin                    | JWT assinado (HMAC), refresh token único por usuário                     |
| **T**ampering              | Alterar payload do JWT (mudar role USER→ADMIN) | Signature validation (HS384/RS256), tamper detection filter              |
| **R**epudiation            | "Não fui eu que deletei o usuário"             | Logs estruturados JSON com userId, traceId, requestId                    |
| **I**nfo Disclosure        | Vazamento de senhas em logs                    | Bcrypt hash, tokens não aparecem em logs, HTTPS obrigatório              |
| **D**enial of Service      | 10.000 requests/segundo                        | Rate limiting via Redis (Fixed Window: 10 req/min auth, 100 req/min API) |
| **E**levation of Privilege | User comum tenta acessar `/admin/dlq`          | RBAC com @PreAuthorize, roles checadas no JWT                            |

**Documentação:** `docs/security/STRIDE-threat-model.md`

---

### 1.4. Dependency Analysis

**Ferramentas ativas:**

1. **GitHub Dependabot** — Alerts automáticos de CVEs em dependências
2. **OWASP Dependency Check** — Plugin Maven (`mvn dependency-check:check`)
3. **Análise manual pré-release** — Review crítico de Spring Security, JJWT, Keycloak

**Processo:**

```bash
# Rodar análise de dependências:
mvn org.owasp:dependency-check-maven:check

# Relatório gerado em:
target/dependency-check-report.html
```

**Bibliotecas críticas auditadas:**

- `spring-boot-starter-security:3.5.11` — Sem CVEs conhecidos
- `jjwt-api:0.12.6` — Atualizado (versões antigas tinham vulnerabilidades de timing)
- `keycloak-spring-boot-starter:26.1.0` — Última stable

---

### 1.5. ADRs — Architecture Decision Records

**3 ADRs criados** em `docs/adr/`:

#### ADR-AUTH-001: Estratégia de Autenticação Dual

```markdown
# Status

Accepted

# Context

- Usuários novos querem onboarding rápido (register local)
- Empresas B2B exigem SSO (Keycloak/SAML)
- Não podemos forçar todos a usar Keycloak no MVP

# Decision

Implementar dual auth:

1. JWT local (HS384) para register/login simples
2. OAuth2 Keycloak (RS256) para SSO enterprise

# Consequences

✅ Flexibilidade (atende ambos públicos)
✅ Migração gradual (local → Keycloak)
❌ Complexidade nos filtros (2 mecanismos coexistindo)
❌ Testes precisam cobrir ambos fluxos
```

#### ADR-AUTHZ-001: Modelo de Autorização

```markdown
# Decision

RBAC (Role-Based Access Control) via Spring Security roles

# Alternatives Considered

- ABAC (Attribute-Based) → Muito complexo para MVP
- ACL (Access Control List) → Performance ruim para consultas

# Rationale

RBAC é suficiente para 90% dos casos:

- USER: acesso básico (criar watch entries, reviews)
- ADMIN: gestão (users, batch jobs, DLQ)
- OPS: operações (DLQ, health checks, logs)
```

#### ADR-API-VERSION-001: Versionamento de API

```markdown
# Decision

Path-based versioning: `/api/v1/*`, `/api/v2/*`

# Alternatives

- Header-based (Accept: application/vnd.api.v1+json) → Ruim para Swagger
- Query param (?version=1) → Anti-pattern REST

# Rationale

- Explicidade (URL mostra versão)
- Cache-friendly (CDNs podem cachear por path)
- Swagger UI friendly (cada versão = tab separada)
```

#### ADR-IAM-001: Integração IAM e Protocolos

```markdown
# Decision

Keycloak como IdP principal, suporte a OAuth2/OIDC + SAML (design)

# SAML Status

Design completo, implementação futura (quando houver demanda B2B)

- SP Metadata: gerado pelo Spring SAML2
- IdP: Keycloak realm `cinelog`
- Attribute mappings: email, firstName, lastName, roles[]

# Why not implement SAML now?

- OAuth2/OIDC cobre 95% dos casos
- SAML é para integrações enterprise específicas (LDAP corporativo)
- Custo-benefício baixo sem cliente B2B confirmado
```

---

### 1.6. Rate Limiting & Security Filters

**Algoritmo:** Fixed Window via Redis  
**Implementação:** `RateLimitFilter` (ordem 1 na filter chain)

**Limites configurados:**
| Escopo | Limite | Janela | Storage |
|--------|--------|--------|---------|
| `/api/auth/*` | 10 req/min | 60s | Redis key: `rate_limit:{ip}:auth` |
| `/api/v1/*` | 100 req/min | 60s | Redis key: `rate_limit:{ip}:api` |
| `/admin/*` | 50 req/min | 60s | Redis key: `rate_limit:{ip}:admin` |

**Fluxo:**

```
Request → RateLimitFilter
            ↓
    Redis INCR rate_limit:{ip}:{path}
            ↓
    Counter > limit? → 429 Too Many Requests
                    ↓
                  Next filter (SqlInjectionFilter)
```

**Redis commands:**

```bash
# Ver contadores ativos:
docker exec cinelog-redis redis-cli KEYS "rate_limit:*"

# Flush (para testes):
docker exec cinelog-redis redis-cli FLUSHDB
```

**Outros filtros de segurança:**

1. **RateLimitFilter** (ordem 1) — DoS protection
2. **SqlInjectionFilter** (ordem 2) — Input sanitization
3. **JwtAuthenticationFilter** (ordem 3) — Dual auth detection
4. **BearerTokenAuthenticationFilter** (ordem 4) — OAuth2 validation
5. **UsernamePasswordAuthenticationFilter** (ordem 5) — Fallback (não usado)

---

## ✅ Semana 2 — IAM e Protocolos

### 2.1. OAuth2 + Keycloak Integration

**Keycloak Setup:**

- **Versão:** 26.1.0
- **Realm:** `cinelog`
- **Issuer URI:** `http://localhost:8180/realms/cinelog`
- **JWKS URI:** `http://localhost:8180/realms/cinelog/protocol/openid-connect/certs`
- **Database:** H2 in-memory (dev) / PostgreSQL (prod)

**Spring Security Config:**

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}")
    private String keycloakIssuerUri;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // ... outros filtros ...
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthConverter())
                )
                .bearerTokenResolver(keycloakOnlyBearerTokenResolver())
            )
            .build();
    }

    // Só resolve tokens Keycloak (evita conflito com JWT local)
    @Bean
    BearerTokenResolver keycloakOnlyBearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        return request -> {
            String token = resolver.resolve(request);
            if (token == null) return null;

            // Decodifica payload e verifica issuer
            String payload = new String(Base64.getUrlDecoder().decode(
                token.split("\\.")[1]
            ));

            return payload.contains(keycloakIssuerUri) ? token : null;
        };
    }
}
```

**Clients configurados:**
| Client | Type | PKCE | Direct Access | Uso |
|--------|------|------|---------------|-----|
| `cinelog-app` | Público | ✅ | ✅ (dev only) | Swagger UI, frontend SPA |
| `cinelog-backend` | Confidencial | ❌ | ❌ | Backend-to-backend, introspection |

**Configuração no realm (`docker/keycloak/cinelog-realm.json`):**

```json
{
    "client": "cinelog-app",
    "enabled": true,
    "publicClient": true,
    "directAccessGrantsEnabled": true, // Resource Owner Password (testes)
    "redirectUris": [
        "http://localhost:8080/swagger-ui/*",
        "http://localhost:3000/*"
    ],
    "attributes": {
        "pkce.code.challenge.method": "S256"
    }
}
```

**Validação RS256:**

```
1. Token Keycloak recebido → Authorization: Bearer eyJhbGc...
2. Spring extrai kid (key ID) do header JWT
3. Busca chave pública correspondente no JWKS endpoint
4. Valida assinatura RSA com chave pública
5. Verifica issuer, expiration, audience
6. Extrai claims (email, roles, sub) → SecurityContext
```

---

### 2.2. SSO (Single Sign-On)

**Como funciona:**

1. User faz login em **App A** (Swagger UI) → Keycloak cria sessão (cookie httpOnly)
2. User acessa **App B** (Keycloak Account Console) → Keycloak detecta sessão
3. Se sessão válida → retorna token automaticamente (sem pedir senha)
4. Logout em qualquer app → `end_session_endpoint` revoga sessão em todos

**Endpoints OIDC Discovery:**

```bash
# Metadata do realm:
curl http://localhost:8180/realms/cinelog/.well-known/openid-configuration | jq

# Retorna:
{
  "issuer": "http://localhost:8180/realms/cinelog",
  "authorization_endpoint": ".../protocol/openid-connect/auth",
  "token_endpoint": ".../protocol/openid-connect/token",
  "userinfo_endpoint": ".../protocol/openid-connect/userinfo",
  "end_session_endpoint": ".../protocol/openid-connect/logout",
  "jwks_uri": ".../protocol/openid-connect/certs"
}
```

**Teste manual:**

1. Abra Swagger: http://localhost:8080/swagger-ui/
2. Authorize → keycloak-sso → login com `marcus@cinelog.com`
3. Abra nova aba: http://localhost:8180/realms/cinelog/account
4. **Resultado esperado:** Já logado (sem pedir senha) — SSO funcionando!

**Sessões ativas (Keycloak Admin):**

```
Admin Console → Realm cinelog → Sessions
- Lista todos os users com sessão ativa
- Mostra clients conectados
- Permite revogar sessões manualmente
```

---

### 2.3. MFA (Multi-Factor Authentication)

**Protocolo:** TOTP (Time-based One-Time Password) — RFC 6238  
**Apps compatíveis:** Google Authenticator, FreeOTP, Microsoft Authenticator, Authy

**Usuários com MFA habilitado:**
| Usuário | Email | Senha | TOTP | Roles |
|---------|-------|-------|------|-------|
| marcus | `marcus@cinelog.com` | `Marcus@CineLog2025!` | ✅ | USER, ADMIN, OPS, CONTENT_ADMIN, MEDIA_ADMIN, USER_ADMIN |
| alice-mfa | `alice@cinelog.com` | `AliceMfa@CineLog2025!` | ✅ | USER |

**Fluxo de primeira configuração:**

```
1. Login no Keycloak Account Console:
   http://localhost:8180/realms/cinelog/account

2. Keycloak detecta: required action = CONFIGURE_TOTP

3. Exibe QR code + secret manual (backup)

4. Scan QR code com app authenticator

5. Insira código de 6 dígitos gerado

6. Keycloak valida código → MFA ativo ✅

7. Próximos logins:
   - Senha → tela de TOTP
   - Insere código atual (válido por 30s)
   - Token retornado contém claim de MFA verificado
```

**Secret TOTP (base32):**

```
Exemplo: JBSWY3DPEHPK3PXP
Algoritmo: HMAC-SHA1
Período: 30 segundos
Dígitos: 6
```

**Testar no Swagger:**

```
1. Swagger UI → Authorize (keycloak-sso)
2. Login: marcus@cinelog.com / Marcus@CineLog2025!
3. Tela de TOTP aparece
4. Abra Google Authenticator → código de 6 dígitos
5. Insira código → Autenticado ✅
6. Token JWT contém: "acr": "1" (authentication context reference)
```

**Código Keycloak (realm JSON):**

```json
{
    "username": "marcus",
    "email": "marcus@cinelog.com",
    "requiredActions": ["CONFIGURE_TOTP"],
    "credentials": [
        {
            "type": "password",
            "value": "Marcus@CineLog2025!"
        }
    ],
    "realmRoles": ["USER", "ADMIN", "OPS"]
}
```

---

### 2.4. Authorization Code + PKCE

**O que é PKCE:** Proof Key for Code Exchange (extensão OAuth2 para clients públicos)

**Problema que resolve:**

- SPAs e mobile apps não podem guardar client_secret (código é público)
- Atacante pode interceptar authorization code (URL redirect)
- PKCE garante que só quem iniciou o flow pode trocar o code por token

**Fluxo completo:**

```
1. Client gera code_verifier aleatório (43-128 chars):
   code_verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

2. Client cria code_challenge:
   code_challenge = BASE64URL(SHA256(code_verifier))
                  = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

3. Redirect para Authorization Server:
   GET /auth
     ?response_type=code
     &client_id=cinelog-app
     &redirect_uri=http://localhost:8080/callback
     &scope=openid profile email
     &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
     &code_challenge_method=S256

4. User faz login → AS retorna code:
   HTTP/1.1 302 Found
   Location: http://localhost:8080/callback?code=ABC123...

5. Client troca code por token (enviando verifier original):
   POST /token
   {
     "grant_type": "authorization_code",
     "code": "ABC123...",
     "client_id": "cinelog-app",
     "redirect_uri": "http://localhost:8080/callback",
     "code_verifier": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
   }

6. AS valida:
   SHA256(code_verifier) == code_challenge? ✅
   → Retorna access_token + id_token
```

**Configuração no Swagger (swagger-config.yaml):**

```yaml
securitySchemes:
    keycloak-sso:
        type: oauth2
        flows:
            authorizationCode:
                authorizationUrl: http://localhost:8180/realms/cinelog/protocol/openid-connect/auth
                tokenUrl: http://localhost:8180/realms/cinelog/protocol/openid-connect/token
                scopes:
                    openid: OpenID Connect
                    profile: Profile info
                    email: Email address
        x-pkce-enabled: true # Swagger UI detecta e gera PKCE automaticamente
```

**Teste no Swagger:**

```
1. Swagger UI → Authorize (keycloak-sso)
2. Chrome DevTools → Network tab
3. Observe request para /auth → query params incluem code_challenge
4. Após login, request para /token → body inclui code_verifier
5. Keycloak valida → token retornado ✅
```

---

### 2.5. SAML (Design Documentado)

**Status:** Design completo, implementação planejada para demanda B2B futura.

**Documento:** `src/.../security/SamlIntegrationPreparation.java` (classe de documentação)

**Arquitetura planejada:**

```
Service Provider (SP) = CineLog backend
Identity Provider (IdP) = Keycloak realm `cinelog`

Fluxo SP-Initiated:
1. User acessa /saml/login no CineLog
2. SP gera AuthnRequest (XML assinado)
3. Redirect para IdP: /realms/cinelog/protocol/saml
4. User faz login no Keycloak
5. IdP retorna SAML Response (XML assinado com assertions)
6. SP valida assinatura XML + extrai attributes
7. Cria sessão local → redirect para app
```

**Attribute Mappings:**
| SAML Attribute | Claim no JWT | Exemplo |
|----------------|--------------|---------|
| `urn:oid:0.9.2342.19200300.100.1.3` | email | marcus@cinelog.com |
| `urn:oid:2.5.4.42` | given_name | Marcus |
| `urn:oid:2.5.4.4` | family_name | Prado |
| `urn:oid:1.2.840.113549.1.9.1` | email_verified | true |
| Custom: `roles` | roles | ["USER","ADMIN"] |

**SP Metadata (seria gerado por Spring SAML2):**

```xml
<EntityDescriptor entityID="http://localhost:8080/saml/metadata">
  <SPSSODescriptor>
    <AssertionConsumerService
      Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
      Location="http://localhost:8080/saml/SSO"
      index="0" />
    <KeyDescriptor use="signing">
      <KeyInfo>
        <X509Data>
          <X509Certificate>MII...</X509Certificate>
        </X509Data>
      </KeyInfo>
    </KeyDescriptor>
  </SPSSODescriptor>
</EntityDescriptor>
```

**Por que não implementado agora:**

- OAuth2/OIDC atende 95% dos casos de uso
- SAML é complexo (XML signing, metadata exchange)
- Só vale a pena para integrações enterprise com LDAP/AD corporativo
- Custo-benefício baixo sem cliente B2B confirmado

**Quando implementar:**

- Cliente enterprise solicitar (ex: TOTVS, SAP, Oracle)
- Integração com Active Directory Federation Services (ADFS)
- Requisito de compliance (ex: órgãos governamentais)

---

### 2.6. Dual Auth Architecture — Deep Dive

**Problema:** Como aceitar 2 tipos de token JWT (HS384 local + RS256 Keycloak) no mesmo endpoint sem conflito?

**Solução:** Filtros cooperativos com detecção de issuer.

**Filter Chain (ordem de execução):**

```
Request com "Authorization: Bearer <token>"
        │
        ▼
┌─── 1. RateLimitFilter ──────────────────────┐
│ Redis check → contador < limite?            │
└──────────────────┬───────────────────────────┘
                   ▼
┌─── 2. SqlInjectionFilter ───────────────────┐
│ Sanitiza inputs → remove SQL keywords       │
└──────────────────┬───────────────────────────┘
                   ▼
┌─── 3. JwtAuthenticationFilter ──────────────┐  ← Customizado
│ 1. Extrai token do header                   │
│ 2. Base64-decode payload (sem validar ainda)│
│ 3. Verifica campo "iss" (issuer):           │
│                                              │
│    Se iss == "http://localhost:8180/..."    │
│       → Token Keycloak                       │
│       → Retorna null (delega próximo filtro)│
│                                              │
│    Se iss == null ou outro:                 │
│       → Token local                          │
│       → Valida HMAC-SHA384 com secret key   │
│       → Extrai subject (userId)             │
│       → Carrega UserDetails do banco        │
│       → Seta SecurityContext                │
│       → Continue filter chain               │
└──────────────────┬───────────────────────────┘
                   ▼
┌─── 4. BearerTokenAuthenticationFilter ──────┐  ← Spring OAuth2 RS
│ BearerTokenResolver customizado:            │
│                                              │
│ keycloakOnlyBearerTokenResolver():           │
│   - Decodifica payload novamente            │
│   - Se iss != Keycloak → retorna null       │
│   - Se iss == Keycloak → retorna token      │
│                                              │
│ Se token resolvido (não null):              │
│   → Busca chave pública no JWKS             │
│   → Valida assinatura RSA-SHA256            │
│   → Verifica exp, aud, iss                  │
│   → Extrai claims do Keycloak               │
│   → Seta SecurityContext                    │
│                                              │
│ Se null:                                     │
│   → Skip (SecurityContext já foi setado)    │
└──────────────────┬───────────────────────────┘
                   ▼
┌─── 5. @PreAuthorize Interceptor ────────────┐
│ Spring AOP valida roles no SecurityContext  │
└──────────────────┬───────────────────────────┘
                   ▼
               Controller
```

**Código do BearerTokenResolver customizado:**

```java
@Bean
BearerTokenResolver keycloakOnlyBearerTokenResolver() {
    DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

    return request -> {
        String token = defaultResolver.resolve(request);
        if (token == null) return null;

        try {
            // Decodifica payload (parte 2 do JWT)
            String[] parts = token.split("\\.");
            String payload = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
            );

            // Parse JSON e verifica issuer
            JsonNode json = objectMapper.readTree(payload);
            String issuer = json.path("iss").asText();

            // Só resolve tokens Keycloak
            if (keycloakIssuerUri != null && issuer.equals(keycloakIssuerUri)) {
                return token;
            }

            // Token local → retorna null (JwtAuthFilter já tratou)
            return null;

        } catch (Exception e) {
            log.warn("Failed to parse token issuer", e);
            return null;
        }
    };
}
```

**Por que funciona:**

1. `JwtAuthenticationFilter` roda **antes** → detecta tipo de token
2. Se for token local → valida e seta `SecurityContext` → próximo filtro vê contexto populado
3. Se for token Keycloak → **não** seta contexto → delega para `BearerTokenAuthFilter`
4. `BearerTokenAuthFilter` só processa se `BearerTokenResolver` retornar não-null
5. Resolver customizado só retorna token se issuer == Keycloak → evita conflito

**Teste de coexistência:**

```bash
# 1. Login local (JWT HS384):
LOCAL_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cinelog.dev","password":"Admin@2025!"}' \
  | jq -r '.accessToken')

# 2. Login Keycloak (JWT RS256):
KC_TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/cinelog/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=cinelog-app" \
  -d "username=marcus" \
  -d "password=Marcus@CineLog2025!" \
  | jq -r '.access_token')

# 3. Testar ambos no mesmo endpoint:
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $LOCAL_TOKEN" \
  http://localhost:8080/api/v1/media
# → 200 ou 404 (autenticado)

curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $KC_TOKEN" \
  http://localhost:8080/api/v1/media
# → 200 ou 404 (autenticado)

# 4. Sem token:
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/v1/media
# → 401 Unauthorized
```

**Estrutura dos tokens (decodificados):**

```json
// Token Local (HS384):
{
  "alg": "HS384",
  "typ": "JWT"
}
{
  "sub": "user-uuid-123",
  "email": "admin@cinelog.dev",
  "roles": ["USER", "ADMIN"],
  "iat": 1710334800,
  "exp": 1710338400
  // iss: AUSENTE → filtro detecta como local
}

// Token Keycloak (RS256):
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "kc-key-id-456"
}
{
  "sub": "kc-user-uuid-789",
  "email": "marcus@cinelog.com",
  "iss": "http://localhost:8180/realms/cinelog",  ← Chave da detecção
  "aud": "account",
  "realm_access": {
    "roles": ["USER", "ADMIN", "OPS"]
  },
  "iat": 1710334800,
  "exp": 1710338400
}
```

---

## 🧪 Scripts de Teste Automatizado

### Script 1: `run-all-tests.sh` — Cobertura Completa de API

**Propósito:** Testar **todas** as rotas da API (86 testes).

**Como executar:**

```bash
# Básico (app em localhost:8080):
./api-tests/run-all-tests.sh

# Com URL customizada:
BASE_URL="http://app.example.com" ./api-tests/run-all-tests.sh
```

**18 seções + 86 assertions:**
| # | Seção | Testes | Status típico |
|---|-------|--------|---------------|
| 1 | Auth (register, login, refresh) | 6 | 6/6 PASS |
| 2 | Genres CRUD | 5 | 5/5 PASS |
| 3 | Media CRUD | 5 | 5/5 PASS |
| 4 | Seasons CRUD | 5 | 5/5 PASS |
| 5 | Episodes CRUD | 5 | 5/5 PASS |
| 6 | People CRUD | 5 | 5/5 PASS |
| 7 | Credits CRUD | 5 | 5/5 PASS |
| 8 | Watch Entries CRUD | 5 | 5/5 PASS |
| 9 | Watchlist | 3 | 3/3 PASS |
| 10 | Search & Discovery | 6 | 6/6 PASS |
| 11 | Recommendations & Popularity | 6 | 6/6 PASS |
| 12 | User Insights | 2 | 2/2 PASS |
| 13 | Watch Progress | 3 | 3/3 PASS |
| 14 | Reports (preview) | 6 | 6/6 PASS |
| 15 | Admin (users, DLQ, batch) | 8 | 6/8 PASS (2 skip: Kafka/TMDb) |
| 16 | Observability (actuator) | 5 | 5/5 PASS |
| 17 | Security (401/403, RBAC) | 4 | 4/4 PASS |
| 18 | Cleanup | 2 | 2/2 PASS |

**Resultado esperado:** **79/86 PASS**, 0 FAIL, 7 SKIP (pula Kafka e TMDb se indisponíveis).

**Exemplo de saída:**

```
═══════════════════════════════════════════════════════════
  🚀 CineLog API — Teste Automatizado de Todas as Rotas
═══════════════════════════════════════════════════════════
  Base URL: http://localhost:8080
  Horário:  2025-03-13 10:30:15
═══════════════════════════════════════════════════════════

══════════════════════════════════════════════════
  1. AUTENTICAÇÃO
══════════════════════════════════════════════════
── Registro de usuário ──
  ✅ PASS [201] POST /api/auth/register
    → Token recebido: eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9...
── Login do usuário ──
  ✅ PASS [200] POST /api/auth/login
── Refresh token ──
  ✅ PASS [200] POST /api/auth/refresh

...

══════════════════════════════════════════════════
  RESULTADO FINAL
══════════════════════════════════════════════════
  Total   : 86
  Passou  : 79
  Falhou  : 0
  Pulou   : 7
  ✅ Todos os testes críticos passaram!
══════════════════════════════════════════════════
```

---

### Script 2: `demo-security-senior.sh` — Demo de Segurança

**Propósito:** Demonstração interativa das features de segurança (Semanas 1+2).

**Pré-requisitos:**

```bash
# App rodando com profile dev (OAuth2 ativo):
./mvnw spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev

# Keycloak deve estar acessível:
curl -s http://localhost:8180/realms/cinelog/.well-known/openid-configuration | jq .issuer
# → "http://localhost:8180/realms/cinelog"
```

**Modos de execução:**

```bash
# Modo INTERATIVO (pausa entre seções — ideal para demo ao vivo):
./api-tests/demo-security-senior.sh

# Modo AUTOMÁTICO (CI/apresentação rápida, sem pausas):
AUTO_MODE=true ./api-tests/demo-security-senior.sh
```

**Estrutura do script (42 testes, 9 seções):**

#### 📋 SEMANA 1 — Fundamentos (7 seções, 28 testes)

**1.1 — JWT Local (HS384)** [5 testes]

```
✅ POST /api/auth/register → 201 Created
✅ POST /api/auth/login → 200 OK
✅ Token structure → header.payload.signature válidos
✅ Payload decode → userId, email, roles presentes
✅ GET /api/v1/media (autenticado) → 200 ou 404
```

**1.2 — Password Policy** [4 testes]

```
✅ Senha fraca (123) → 400 Bad Request
✅ Senha sem maiúscula → 400
✅ Email duplicado → 409 Conflict
✅ Login com credenciais erradas → 401 Unauthorized
```

**1.3 — Refresh Token** [3 testes]

```
✅ POST /api/auth/refresh (token válido) → 200 + novos tokens
✅ Usar refresh antigo novamente → 401 (rotação funcionando)
✅ Access token expira em 1h
```

**1.4 — RBAC** [4 testes]

```
✅ USER acessa GET /api/v1/media → 200
✅ USER tenta GET /api/v1/users → 403 Forbidden
✅ ADMIN acessa GET /api/v1/users → 200
✅ ADMIN acessa GET /admin/dlq → 200
```

**1.5 — Method Security (@PreAuthorize)** [3 testes]

```
✅ Mostrar annotation no código (BatchJobController)
✅ USER tenta POST /api/v1/admin/batch/genres → 403
✅ ADMIN executa POST /api/v1/admin/batch/genres → 202 Accepted
```

**1.6 — Endpoints Públicos vs Protegidos** [2 testes]

```
✅ GET /actuator/health (sem token) → 200 OK
✅ GET /api/v1/media (sem token) → 401 Unauthorized
```

**1.7 — Logout / Revogação** [2 testes]

```
✅ POST /api/auth/logout → 204 No Content
✅ Tentar usar refresh revogado → 401
⚠️  Access token ainda válido até expirar (stateless JWT)
```

#### 🌐 SEMANA 2 — IAM (9 seções, 14 testes)

**2.1 — OIDC Discovery** [2 testes]

```
✅ GET /.well-known/openid-configuration → 200 OK
✅ JSON contém issuer, jwks_uri, authorization_endpoint
```

**2.2 — OAuth2 Token (Keycloak)** [3 testes]

```
✅ POST /protocol/openid-connect/token (password grant) → 200
✅ Token structure → alg=RS256, kid presente
✅ Payload decode → iss="http://localhost:8180/...", realm_access.roles
```

**2.3 — Dual Auth (Local + Keycloak)** [4 testes]

```
✅ GET /api/v1/media (token local) → 200/404
✅ GET /api/v1/media (token Keycloak) → 200/404
✅ Headers de resposta → X-Trace-Id, X-Span-Id presentes
✅ Ambos tokens válidos no mesmo endpoint
```

**2.4 — Token Refresh (Keycloak)** [2 testes]

```
✅ POST /token (grant_type=refresh_token) → 200
✅ Novo access_token retornado
```

**2.5 — Introspection / Userinfo** [2 testes]

```
✅ GET /protocol/openid-connect/userinfo → 200
✅ JSON contém email, name, preferred_username
```

**2.6 — MFA/TOTP** [1 teste]

```
✅ Verificar configuração de marcus:
   GET /admin/realms/cinelog/users/{marcusId} → requiredActions include CONFIGURE_TOTP
```

**2.7 — SSO (Single Sign-On)** [2 testes]

```
✅ Verificar sessões ativas → GET /admin/realms/cinelog/users/{aliceId}/sessions
✅ Listar clients do realm → JSON contém cinelog-app, cinelog-backend
```

**2.8 — Authorization Code + PKCE** [1 teste]

```
✅ Gerar PKCE challenge → code_verifier + SHA256
✅ Montar Authorization URL com code_challenge
✅ GET Keycloak /auth (tela de login) → 200 OK
💡 Swagger UI: Authorize → Login → PKCE automático
```

**2.9 — Arquitetura Summary** [1 teste]

```
✅ Banner final com camadas de segurança:
   - SecurityFilterChain (5 filtros)
   - Roles e permissões
   - Keycloak clients
   - MFA via TOTP
```

**Resultado esperado:** **42/42 PASS**, 0 FAIL, 0 SKIP.

**Exemplo de saída (modo automático):**

```
   _____ _            _                 ____                        _ _
  / ____(_)          | |               / ___|  ___  ___ _   _ _ __ (_) |_ _   _
 | |     _ _ __   ___| |     ___   __ \___ \ / _ \/ __| | | | '__|| | __| | | |
 | |    | | '_ \ / _ \ |    / _ \ / _` |__) |  __/ (__| |_| | |  | | |_| |_| |
 |_|    |_|_| |_|\___|_|___|\___/ \__, |____/ \___|\___|\__,_|_|  |_|\__|\__, |
                                   __/ |                                  __/ |
                                  |___/                                  |___/
  Demo de Segurança — Sprint Semanas 1 & 2

  App:       http://localhost:8080
  Keycloak:  http://localhost:8180
  Horário:   2025-03-13 10:45:30

  ⚡ Modo automático (sem pausas)

══════════════════════════════════════════════════════════════════════════
  SEMANA 1 — Fundamentos de Segurança e Arquitetura
══════════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────┐
│  1.1 — Autenticação JWT (HS256 local)
└─────────────────────────────────────────────────────────────────┘
💡 JWT gerado pelo backend (HMAC-SHA384, secret key).
💡 Estrutura: header.payload.signature (Base64 URL-safe).
  → Registrando usuário demo...
    ✔ PASS  POST /api/auth/register (HTTP 201)
      Token recebido: eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9...
  → Fazendo login...
    ✔ PASS  POST /api/auth/login (HTTP 200)

[... output continua ...]

╔═══════════════════════════════════════════════════════════════════╗
║  RESULTADO FINAL
╠═══════════════════════════════════════════════════════════════════╣
║  Total:    42
║  Passed:  42
║  Failed:  0
║  Skipped: 0
╠═══════════════════════════════════════════════════════════════════╣
║  ✅ TODOS OS TESTES PASSARAM!
╠═══════════════════════════════════════════════════════════════════╣
║  Semana 1: JWT HS384, Password Policy, RBAC, Refresh Token,
║           Method Security, Public vs Protected, Logout
║  Semana 2: OIDC Discovery, OAuth2 RS256, Dual Auth,
║           Token Refresh, Introspection, MFA/TOTP, SSO, PKCE
╠═══════════════════════════════════════════════════════════════════╣
║  🎯 Próximos passos para demo MFA interativa:
║  1. Abrir http://localhost:8180/realms/cinelog/account
║  2. Login: marcus / Marcus@CineLog2025!
║  3. Escanear QR code com Google Authenticator
║  4. Após MFA ativo → logar via Swagger com senha + TOTP
╚═══════════════════════════════════════════════════════════════════╝
```

---

## 📊 Métricas & Observabilidade

### Métricas de Segurança (Micrometer)

**Endpoint:** `/actuator/metrics` (requer role ADMIN)

**Counters implementados:**
| Métrica | Tags | Incrementa quando |
|---------|------|-------------------|
| `cinelog.security.auth_failures_total` | `reason`: invalid_credentials, account_locked | Login falha |
| `cinelog.security.account_lockouts_total` | — | Conta bloqueada (5 tentativas) |
| `cinelog.security.jwt_failures_total` | `reason`: expired, invalid_signature, malformed | JWT inválido |
| `cinelog.security.rate_limit_total` | `path_class`: auth, api, admin | Rate limit hit (429) |
| `cinelog.security.sqli_attempts_total` | — | SQL injection detectado |
| `cinelog.security.access_denied_total` | — | 403 Forbidden (RBAC) |
| `cinelog.security.tamper_detected_total` | `type`: header, payload | Tampering detectado |
| `cinelog.security.sensitive_access_total` | `resource`: users, admin, dlq | Acesso a recurso sensível |

**Como testar:**

```bash
# 1. Gerar falhas de login (como admin):
for i in {1..3}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"fake@test.com","password":"wrong"}'
done

# 2. Consultar métrica (requer token ADMIN):
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  'http://localhost:8080/actuator/metrics/cinelog.security.auth_failures_total' \
  | jq

# Resposta:
{
  "name": "cinelog.security.auth_failures_total",
  "measurements": [
    {"statistic": "COUNT", "value": 3.0}
  ],
  "availableTags": [
    {
      "tag": "reason",
      "values": ["invalid_credentials"]
    }
  ]
}

# 3. Filtrar por tag:
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  'http://localhost:8080/actuator/metrics/cinelog.security.auth_failures_total?tag=reason:invalid_credentials' \
  | jq '.measurements[0].value'
# → 3.0
```

**Implementação (exemplo):**

```java
@Component
public class SecurityMetricsService {

    private final MeterRegistry registry;

    public void recordAuthFailure(String reason) {
        Counter.builder("cinelog.security.auth_failures_total")
            .tag("reason", reason)
            .description("Tentativas de login falhadas")
            .register(registry)
            .increment();
    }

    public void recordRateLimitHit(String pathClass) {
        Counter.builder("cinelog.security.rate_limit_total")
            .tag("path_class", pathClass)
            .description("Rate limit acionado")
            .register(registry)
            .increment();
    }
}
```

---

### Logs Estruturados (JSON + MDC)

**Formato:** JSON Lines (cada log = 1 linha JSON)  
**Appender:** Logback com LogstashEncoder

**Campos automáticos (MDC):**
| Campo | Origem | Exemplo | Quando presente |
|-------|--------|---------|-----------------|
| `timestamp` | Logback | `2025-03-13T10:45:30.123Z` | Sempre |
| `level` | Logback | `INFO`, `WARN`, `ERROR` | Sempre |
| `logger` | Logback | `c.c.c.features.auth.AuthService` | Sempre |
| `message` | Log statement | `User logged in successfully` | Sempre |
| `traceId` | ObservabilityContextFilter | `abc123def456ghi789` | Todos os requests HTTP |
| `spanId` | ObservabilityContextFilter | `span-xyz` | Todos os requests HTTP |
| `requestId` | ObservabilityContextFilter | `req-uuid-123` | Todos os requests HTTP |
| `userId` | JwtAuthFilter / OAuth2 | `user-uuid-456` | Requests autenticados |
| `tookMs` | HttpLoggingFilter | `45` | Logs de resposta HTTP |
| `status` | HttpLoggingFilter | `200`, `401`, `500` | Logs de resposta HTTP |
| `method` | HttpLoggingFilter | `GET`, `POST` | Logs de request HTTP |
| `path` | HttpLoggingFilter | `/api/v1/media` | Logs de request HTTP |

**Exemplo de log:**

```json
{
    "timestamp": "2025-03-13T10:45:30.123Z",
    "level": "INFO",
    "thread": "http-nio-8080-exec-5",
    "logger": "c.c.c.features.auth.application.AuthService",
    "message": "User logged in successfully",
    "traceId": "abc123def456ghi789",
    "spanId": "span-xyz123",
    "requestId": "req-uuid-abc-123",
    "userId": "user-uuid-456",
    "email": "marcus@cinelog.com",
    "method": "POST",
    "path": "/api/auth/login",
    "status": 200,
    "tookMs": 45
}
```

**Configuração (logback-spring.xml):**

```xml
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
  <encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeMdcKeyName>traceId</includeMdcKeyName>
    <includeMdcKeyName>spanId</includeMdcKeyName>
    <includeMdcKeyName>requestId</includeMdcKeyName>
    <includeMdcKeyName>userId</includeMdcKeyName>
    <includeMdcKeyName>tookMs</includeMdcKeyName>
    <includeMdcKeyName>status</includeMdcKeyName>
    <includeMdcKeyName>method</includeMdcKeyName>
    <includeMdcKeyName>path</includeMdcKeyName>
  </encoder>
</appender>
```

---

### Headers de Tracing (Propagação)

**Auto-injetados em todas as respostas HTTP:**

```
X-Trace-Id: abc123def456ghi789
X-Span-Id: span-xyz123
X-Request-Id: req-uuid-abc-123
```

**Como testar:**

```bash
curl -v http://localhost:8080/actuator/health 2>&1 | grep -i "x-trace\|x-span\|x-request"

# Output:
< X-Trace-Id: abc123def456ghi789
< X-Span-Id: span-xyz123
< X-Request-Id: req-uuid-abc-123
```

**Implementação (ObservabilityContextFilter):**

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain chain) throws ServletException, IOException {
    String traceId = UUID.randomUUID().toString().replace("-", "");
    String spanId = "span-" + traceId.substring(0, 16);
    String requestId = "req-" + UUID.randomUUID().toString();

    MDC.put("traceId", traceId);
    MDC.put("spanId", spanId);
    MDC.put("requestId", requestId);

    response.setHeader("X-Trace-Id", traceId);
    response.setHeader("X-Span-Id", spanId);
    response.setHeader("X-Request-Id", requestId);

    try {
        chain.doFilter(request, response);
    } finally {
        MDC.clear();
    }
}
```

---

### Stack de Observabilidade (Planejada)

**Grafana + Prometheus + Loki + Tempo:**

```bash
# Subir stack completa:
docker compose up -d prometheus grafana loki promtail tempo jaeger otel-collector
```

| Serviço        | URL                                 | Propósito                             |
| -------------- | ----------------------------------- | ------------------------------------- |
| **Grafana**    | http://localhost:3000 (admin/admin) | Dashboards (métricas + logs + traces) |
| **Prometheus** | http://localhost:9090               | Time-series DB (métricas)             |
| **Loki**       | http://localhost:3100               | Log aggregation                       |
| **Tempo**      | http://localhost:3200               | Distributed tracing                   |
| **Jaeger**     | http://localhost:16686              | Trace UI (alternativa)                |

**Datasources provisionados automaticamente:**

- Prometheus → scrape `/actuator/prometheus` (15s interval)
- Loki → recebe logs via Promtail
- Tempo → recebe traces via OpenTelemetry Collector

**Dashboards incluídos:**

1. **Business Metrics** — Counters de negócio (logins, cadastros, watch entries)
2. **Infrastructure** — JVM memory, GC, threads, HTTP latency
3. **Security** — Auth failures, rate limits, access denied (403)

---

## 🎯 Checklist para Demo ao Senior

### Preparação (5 minutos)

**Infraestrutura:**

- [ ] Docker Compose up: `docker compose -f docker/docker-compose.dev.yml up -d`
- [ ] Keycloak healthy: `curl http://localhost:8180/realms/cinelog/.well-known/openid-configuration | jq .issuer`
- [ ] MySQL healthy: `docker exec cinelog-mysql mysqladmin ping -p`
- [ ] Redis healthy: `docker exec cinelog-redis redis-cli PING`

**Aplicação:**

- [ ] App rodando com profile `dev`: `./mvnw spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev`
- [ ] Health check OK: `curl http://localhost:8080/actuator/health | jq .status` # → "UP"
- [ ] Swagger acessível: Abrir http://localhost:8080/swagger-ui/

**Navegador:**

- [ ] Tab 1: Swagger UI (http://localhost:8080/swagger-ui/)
- [ ] Tab 2: Keycloak Admin (http://localhost:8180/admin)
- [ ] Tab 3: Keycloak Account (http://localhost:8180/realms/cinelog/account)
- [ ] DevTools aberto (F12) na tab do Swagger

---

### Demo Script (20-25 minutos)

#### Parte 1: Semana 1 — Fundamentos (10 min)

**1.1. JWT Local (3min)**

- [ ] Swagger → POST /api/auth/register
    ```json
    {
        "name": "Demo User",
        "email": "demo@test.com",
        "password": "Demo@Secure2025!"
    }
    ```
- [ ] Copiar `accessToken` da resposta
- [ ] Abrir https://jwt.io → colar token → mostrar header/payload
    - **Header:** `{"alg":"HS384","typ":"JWT"}`
    - **Payload:** `{"sub":"user-id","email":"...","roles":["USER"]}`
- [ ] Swagger → Authorize (BearerAuth) → colar token
- [ ] GET /api/v1/media → 200 ou 404 (autenticado! ✅)

**1.2. Password Policy (2min)**

- [ ] POST /api/auth/register com senha fraca:
    ```json
    { "name": "Test", "email": "weak@test.com", "password": "123" }
    ```

    - **Esperado:** 400 Bad Request, mensagem "senha muito curta"
- [ ] Tentar mesmo email novamente → 409 Conflict

**1.3. RBAC (3min)**

- [ ] Como USER (token atual):
    - [ ] GET /api/v1/users → **403 Forbidden** ✅
    - [ ] GET /api/v1/media → **200** ✅
- [ ] Login como ADMIN:
    ```json
    { "email": "admin@cinelog.dev", "password": "Admin@CineLog2025!" }
    ```
- [ ] Authorize com token ADMIN
- [ ] GET /api/v1/users → **200 OK** (lista de users) ✅

**1.4. Métricas de Segurança (2min)**

- [ ] Tab DevTools → Network
- [ ] Fazer 3 logins com senha errada
- [ ] GET /actuator/metrics/cinelog.security.auth_failures_total → `{"value": 3}`
- [ ] Mostrar tags filtradas: `?tag=reason:invalid_credentials`

---

#### Parte 2: Semana 2 — IAM (10 min)

**2.1. Keycloak Discovery (1min)**

- [ ] Nova tab: http://localhost:8180/realms/cinelog/.well-known/openid-configuration
- [ ] Mostrar campos no JSON:
    - `issuer`
    - `authorization_endpoint`
    - `token_endpoint`
    - `jwks_uri`

**2.2. OAuth2 Login via Swagger (3min)**

- [ ] Swagger → Authorize (keycloak-sso) → clique em "Authorize"
- [ ] Redirect para Keycloak login
- [ ] Login: `marcus@cinelog.com` / `Marcus@CineLog2025!`
- [ ] DevTools → Network → filtrar "token" → mostrar request POST com `code_verifier` (PKCE ✅)
- [ ] Token retornado → copiar access_token
- [ ] jwt.io → colar → mostrar:
    - **Header:** `{"alg":"RS256","kid":"..."}`
    - **Payload:** `{"iss":"http://localhost:8180/...","realm_access":{"roles":[...]}}` ✅

**2.3. Dual Auth (2min)**

- [ ] Testar ambos tokens no mesmo endpoint (GET /api/v1/media):
    - Token local (HS384) → 200 ✅
    - Token Keycloak (RS256) → 200 ✅
- [ ] Mostrar código `SecurityConfig.java`:
    ```java
    .bearerTokenResolver(keycloakOnlyBearerTokenResolver())
    ```

**2.4. MFA/TOTP (4min)**

- [ ] Nova tab: http://localhost:8180/realms/cinelog/account
- [ ] Login: `marcus@cinelog.com` / `Marcus@CineLog2025!`
- [ ] **Se 1ª vez:** Tela de configuração TOTP aparece
    - [ ] Abrir Google Authenticator no celular
    - [ ] Escanear QR code
    - [ ] Inserir código de 6 dígitos
    - [ ] "MFA configurado com sucesso!" ✅
- [ ] **Se já configurado:** Já mostra dashboard (sem pedir TOTP — SSO ativo!)
- [ ] Fazer logout → tentar login novamente
    - [ ] Agora pede TOTP após senha ✅

**2.5. SSO Demo (bonus, 2min)**

- [ ] Com sessão ativa do marcus na Account Console
- [ ] Voltar ao Swagger → Authorize (keycloak-sso)
- [ ] **Não pede senha** — redirect automático ✅ (SSO!)
- [ ] Keycloak Admin → Realm cinelog → Sessions → mostrar sessão de marcus

---

#### Parte 3: Arquitetura & ADRs (5 min)

**3.1. Dual Auth Code Review (3min)**

- [ ] Abrir `SecurityConfig.java`:

    ```java
    // JwtAuthenticationFilter (ordem 3):
    if (isKeycloakToken(token)) {
        return null; // Delega OAuth2 filter
    } else {
        validateLocalToken(token);
        setSecurityContext();
    }

    // keycloakOnlyBearerTokenResolver (ordem 4):
    String payload = decodePayload(token);
    if (payload.contains(keycloakIssuerUri)) {
        return token; // OAuth2 processa
    }
    return null; // Skip (já foi tratado)
    ```

**3.2. ADRs (2min)**

- [ ] Abrir `docs/adr/ADR-AUTH-001.md`
- [ ] Ler seção "Decision" e "Consequences"
- [ ] Explicar tradeoff: ✅ Flexibilidade vs ❌ Complexidade

---

#### Parte 4: Testes Automatizados (5 min)

**4.1. Script de demo (3min)**

- [ ] Terminal: `AUTO_MODE=true ./api-tests/demo-security-senior.sh`
- [ ] Assistir output colorido (42 testes)
- [ ] Resultado final: **42/42 PASS** ✅

**4.2. Testes JUnit (2min)**

- [ ] Abrir IDE → mostrar classes de teste:
    - `AuthServiceTest` (unitário)
    - `KeycloakOAuth2IntegrationTest` (integração com Testcontainers)
    - `SecurityMethodAnnotationTest` (RBAC)
- [ ] Rodar suite: `./mvnw test`
- [ ] Relatório JaCoCo: `target/site/jacoco/index.html` → 80%+ coverage

---

## 📚 Glossário de Conceitos

### Autenticação & Autorização

#### **Autenticação (Authentication)**

Verificar **quem** você é.

- **Exemplo:** Login com email + senha → sistema confirma identidade
- **No projeto:** JWT local (HS384) ou OAuth2 (Keycloak RS256)

#### **Autorização (Authorization)**

Verificar **o que** você pode fazer após autenticado.

- **Exemplo:** Admin pode deletar users, User regular não
- **No projeto:** RBAC via `@PreAuthorize("hasRole('ADMIN')")`

---

### JWT (JSON Web Token)

**Estrutura:** `header.payload.signature` (3 partes separadas por `.`)

**Exemplo decodificado:**

```json
// Header (algoritmo + tipo)
{"alg":"HS384","typ":"JWT"}

// Payload (dados do usuário)
{
  "sub":"user-123",
  "email":"alice@test.com",
  "roles":["USER"],
  "iat":1710334800,  // Issued At
  "exp":1710338400   // Expiration (1h depois)
}

// Signature (garante integridade)
HMACSHA384(
  base64(header) + "." + base64(payload),
  secret_key
)
```

**Tipos de algoritmo:**
| Tipo | Algoritmo | Chave | Onde usar | Usado no projeto |
|------|-----------|-------|-----------|------------------|
| Simétrico | **HS384** (HMAC-SHA384) | Secret compartilhada | Backend gera e valida tokens | ✅ JWT local |
| Assimétrico | **RS256** (RSA-SHA256) | Privada (assina) + Pública (valida) | IdP externo (Keycloak) | ✅ OAuth2 tokens |

---

### OAuth2 & OpenID Connect

#### **OAuth2**

Framework de **autorização** (não autenticação!) para delegar acesso sem expor credenciais.

**Atores:**

- **Resource Owner:** Usuário (dono dos dados)
- **Client:** App que quer acessar (Swagger UI, frontend)
- **Authorization Server (AS):** Emite tokens (Keycloak)
- **Resource Server (RS):** API protegida (CineLog backend)

**Fluxos (Grants):**
| Grant | Uso | Segurança | Status no projeto |
|-------|-----|-----------|-------------------|
| **Authorization Code** | Web apps com backend | ✅ Seguro | Usado (Swagger) |
| **Authorization Code + PKCE** | SPAs, mobile (sem client_secret) | ✅ Seguro | Usado (cinelog-app) |
| **Client Credentials** | Machine-to-machine | ✅ Seguro | Planejado (M2M) |
| ~~Implicit~~ | ❌ Obsoleto (token na URL) | ❌ Inseguro | Não usar |
| ~~Password~~ | ❌ Legado (credenciais diretas) | ⚠️ Só dev/test | Habilitado (testes) |

#### **PKCE (Proof Key for Code Exchange)**

**Pronuncia:** "pixie"

**PROBLEMA:** SPA baixa código (authorization code) via redirect → atacante intercepta URL.

**SOLUÇÃO PKCE:**

1. Client gera `code_verifier` aleatório (43-128 chars)
2. Client cria `code_challenge = SHA256(code_verifier)`
3. Redirect: `?code_challenge=xyz&code_challenge_method=S256`
4. AS retorna `code`
5. Client troca code por token, enviando `code_verifier` original
6. AS valida: `SHA256(code_verifier) == code_challenge`? ✅

**No projeto:** Swagger UI usa PKCE automaticamente com client `cinelog-app`.

#### **OpenID Connect (OIDC)**

Camada de **autenticação** sobre OAuth2. Adiciona `id_token` (JWT com dados do user).

**Diferença:**

- **OAuth2:** "Pode acessar meus filmes?" → `access_token`
- **OIDC:** "Quem é você?" → `id_token` + `access_token`

**Endpoints OIDC:**

- `.well-known/openid-configuration` — Metadata do realm
- `/protocol/openid-connect/userinfo` — Dados do user autenticado

---

### SSO & MFA

#### **SSO (Single Sign-On)**

Login **uma vez**, acesso a **múltiplos apps** do mesmo realm.

**Como funciona:**

1. Login em App A → Keycloak cria sessão (cookie httpOnly)
2. Acessa App B → Keycloak vê sessão válida → retorna token sem pedir senha
3. Logout em qualquer app → sessão revogada em todos

**No projeto:** `marcus` loga no Swagger → pode acessar Account Console sem senha.

#### **MFA (Multi-Factor Authentication)**

Autenticação com **2+ fatores:**

- **Factor 1:** Algo que você **sabe** (senha)
- **Factor 2:** Algo que você **tem** (celular com TOTP)
- ~~Factor 3~~: Algo que você **é** (biometria) — não implementado

**TOTP (Time-based One-Time Password):**

- RFC 6238 (usado por Google Authenticator, FreeOTP)
- Código de 6 dígitos válido por 30 segundos
- Baseado em secret compartilhado (QR code no setup)

**Algoritmo:**

```
TOTP = Truncate(HMAC-SHA1(secret, time_step))
time_step = floor(current_unix_time / 30)
```

---

### Segurança

#### **RBAC (Role-Based Access Control)**

Autorização baseada em **papéis** (roles), não usuários individuais.

**No projeto:**

- Roles: `USER`, `ADMIN`, `OPS`
- Endpoint verifica: `hasRole('ADMIN')`? → sim/não
- Implementado via Spring Method Security

**Alternativas (não usadas):**

- **ABAC** (Attribute-Based) — Complexo, baseado em atributos dinâmicos
- **ACL** (Access Control List) — Por recurso individual, não escala

#### **STRIDE Threat Model**

Framework Microsoft para categorizar ameaças:

| Letra | Ameaça                              | Exemplo                     |
| ----- | ----------------------------------- | --------------------------- |
| **S** | Spoofing (falsificação identidade)  | Atacante se passa por admin |
| **T** | Tampering (modificação dados)       | Alterar role no JWT         |
| **R** | Repudiation (negação ação)          | "Não fui eu que deletou"    |
| **I** | Information Disclosure (vazamento)  | Senha em logs               |
| **D** | Denial of Service (negação serviço) | Flood de requests           |
| **E** | Elevation of Privilege (escalação)  | User vira admin             |

#### **Rate Limiting**

Limitar número de requests em janela de tempo.

**Algoritmo no projeto — Fixed Window:**

- Storage: Redis (key = `rate_limit:{ip}:{path_class}`)
- TTL: 60 segundos
- Limite: 10/min (auth), 100/min (API)
- Ação: Se counter > limite → 429 Too Many Requests

**Alternativas:**

- **Sliding Window:** Mais suave, mas complexo
- **Token Bucket:** Permite bursts controlados
- **Leaky Bucket:** Taxa constante

#### **Account Lockout**

Bloquear conta após N tentativas falhadas.

**No projeto:**

- 5 tentativas → lockout 15 minutos
- Retorna 423 Locked (não 401) para evitar brute-force

---

### Arquitetura

#### **ADR (Architecture Decision Record)**

Documento que explica **por que** uma decisão técnica foi tomada.

**Template:**

```markdown
# Status

Accepted / Proposed / Deprecated

# Context

Problema/necessidade que motivou a decisão

# Decision

O que decidimos fazer

# Consequences

Tradeoffs (pros/cons)
```

#### **JWKS (JSON Web Key Set)**

Endpoint que expõe chaves públicas RSA para validar tokens RS256.

**URL Keycloak:**

```
http://localhost:8180/realms/cinelog/protocol/openid-connect/certs
```

**Estrutura:**

```json
{
    "keys": [
        {
            "kid": "abc123", // Key ID (identifica a chave)
            "kty": "RSA", // Key Type
            "alg": "RS256",
            "use": "sig", // Uso: assinatura
            "n": "0vx7agoebGc...", // RSA Modulus (chave pública)
            "e": "AQAB" // RSA Exponent
        }
    ]
}
```

**Fluxo de validação:**

1. Backend recebe token com `kid: "abc123"` no header
2. Busca chave correspondente no JWKS
3. Valida assinatura com chave pública RSA
4. Se válido → token aceito

---

### Tokens

#### **Access Token vs Refresh Token**

| Característica            | Access Token              | Refresh Token        |
| ------------------------- | ------------------------- | -------------------- |
| **Tempo de vida**         | Curto (1h)                | Longo (30d)          |
| **Propósito**             | Acesso imediato a APIs    | Renovar access token |
| **Storage**               | Cliente (stateless JWT)   | Banco de dados       |
| **Revogável?**            | ❌ (válido até expirar)   | ✅ (delete no banco) |
| **Incluído em requests?** | ✅ (header Authorization) | ❌ (só em /refresh)  |

**Fluxo de refresh:**

```
1. Access token expira (após 1h)
2. Cliente: POST /api/auth/refresh + refresh_token
3. Backend valida refresh no banco → gera novo par
4. Refresh antigo é DELETADO (rotação segura)
5. Cliente recebe novo access + refresh
```

#### **Token Rotation (Refresh Token)**

Ao renovar access token, **revogar refresh antigo** e gerar novo.

**Por que:** Se refresh vazar, atacante só pode usar 1x. No próximo uso legítimo, refresh roubado é invalidado.

---

### Observabilidade

#### **MDC (Mapped Diagnostic Context)**

ThreadLocal map para adicionar campos em **todos** os logs de uma thread.

**Exemplo:**

```java
MDC.put("userId", user.getId());
log.info("Action performed");
// Log terá: {"message":"Action performed","userId":"user-123",...}
MDC.clear();
```

**No projeto:** Campos automáticos via `ObservabilityContextFilter`:

- `traceId`, `spanId`, `requestId` (todos os requests)
- `userId` (requests autenticados)
- `tookMs`, `status`, `method`, `path` (responses HTTP)

#### **Distributed Tracing**

Rastrear request completo através de múltiplos services.

**Conceitos:**

- **Trace:** Operação completa (ex: "salvar filme")
- **Span:** Etapa dentro do trace (ex: "validar", "salvar DB", "publicar evento")
- **TraceId:** Identificador único propagado via header `X-Trace-Id`

**Stack (planejada):** Zipkin ou Jaeger com OpenTelemetry.

---

## 🔧 Ferramentas & Stack

### Keycloak

**O que é:** IAM (Identity and Access Management) open-source.

**Features:**

- Authorization Server (OAuth2/OIDC)
- User Federation (LDAP, AD)
- SSO, MFA (TOTP), social login (Google/GitHub)
- Admin Console web
- Protocolos: OAuth2, OIDC, SAML

**Conceitos:**

- **Realm:** Namespace isolado (ex: `cinelog`)
- **Client:** App que usa Keycloak (ex: `cinelog-app`)
- **User:** Conta com credenciais
- **Role:** Papel (realm roles vs client roles)

### Spring Security

**O que é:** Framework de segurança Java.

**Componentes:**

- **SecurityFilterChain:** Define regras (quem acessa o quê)
- **AuthenticationManager:** Valida credenciais
- **UserDetailsService:** Carrega user do banco
- **JwtDecoder:** Valida e decodifica JWT

### Redis

**O que é:** In-memory key-value store.

**Uso no projeto:**

- Rate limiting counters (TTL automático)
- Cache (futuro)

**Comandos úteis:**

```bash
# Listar keys:
redis-cli KEYS "rate_limit:*"

# Flush (testes):
redis-cli FLUSHDB

# Ver valor:
redis-cli GET rate_limit:127.0.0.1:auth
```

### bcrypt

**O que é:** Hash de senha com **salt** automático + **cost factor**.

**No projeto:**

- `BCryptPasswordEncoder` (cost 12 = 2^12 iterações ~ 250ms)
- Protege contra rainbow tables e brute-force

---

## 🚀 Abreviações Rápidas

| Sigla      | Significado                         | Contexto                  |
| ---------- | ----------------------------------- | ------------------------- |
| **IAM**    | Identity and Access Management      | Keycloak                  |
| **SSO**    | Single Sign-On                      | Login compartilhado       |
| **MFA**    | Multi-Factor Authentication         | TOTP                      |
| **TOTP**   | Time-based One-Time Password        | RFC 6238                  |
| **RBAC**   | Role-Based Access Control           | Autorização               |
| **PKCE**   | Proof Key for Code Exchange         | OAuth2 seguro             |
| **OIDC**   | OpenID Connect                      | OAuth2 + autenticação     |
| **JWKS**   | JSON Web Key Set                    | Chaves públicas RS256     |
| **SAML**   | Security Assertion Markup Language  | SSO enterprise            |
| **HMAC**   | Hash-based Message Auth Code        | HS384                     |
| **RSA**    | Rivest-Shamir-Adleman               | RS256                     |
| **ADR**    | Architecture Decision Record        | Docs de decisão           |
| **STRIDE** | Spoofing, Tampering, Repudiation... | Threat modeling           |
| **OWASP**  | Open Web App Security Project       | Top 10 vulnerabilidades   |
| **MDC**    | Mapped Diagnostic Context           | Logs contextuais          |
| **AOP**    | Aspect-Oriented Programming         | @PreAuthorize via proxy   |
| **TTL**    | Time To Live                        | Expiração                 |
| **DLQ**    | Dead Letter Queue                   | Mensagens não processadas |

---

## 📖 Referências Técnicas

### Documentação Oficial

- **OAuth2/OIDC:** https://oauth.net/2/
- **JWT:** https://jwt.io/
- **Spring Security:** https://docs.spring.io/spring-security/reference/
- **Keycloak:** https://www.keycloak.org/documentation
- **OWASP Top 10:** https://owasp.org/www-project-top-ten/
- **RFC 6749 (OAuth2):** https://datatracker.ietf.org/doc/html/rfc6749
- **RFC 7519 (JWT):** https://datatracker.ietf.org/doc/html/rfc7519
- **RFC 6238 (TOTP):** https://datatracker.ietf.org/doc/html/rfc6238

### Documentos do Projeto

| Documento           | Path                                   | Conteúdo                                       |
| ------------------- | -------------------------------------- | ---------------------------------------------- |
| **Testing Guide**   | `docs/TESTING_GUIDE.md`                | Guia completo de testes (§5-6, §8, §11.2, §16) |
| **ADR Auth**        | `docs/adr/ADR-AUTH-001.md`             | Decisão de dual auth                           |
| **ADR IAM**         | `docs/adr/ADR-IAM-001.md`              | Keycloak, OAuth2, SAML design                  |
| **ADR Authz**       | `docs/adr/ADR-AUTHZ-001.md`            | RBAC vs ABAC vs ACL                            |
| **Threat Model**    | `docs/security/STRIDE-threat-model.md` | Análise STRIDE                                 |
| **Security Config** | `src/.../security/SecurityConfig.java` | Filter chain, dual auth                        |
| **Script Demo**     | `api-tests/demo-security-senior.sh`    | 42 testes automatizados                        |
| **Realm Export**    | `docker/keycloak/cinelog-realm.json`   | Configuração Keycloak                          |

---

## ✅ Pontos-Chave para Destacar na Apresentação

1. **Dual Auth é diferencial competitivo**
    - Onboarding rápido (register local) para MVP
    - SSO enterprise (Keycloak) para B2B
    - Ambos coexistem sem conflito

2. **MFA production-ready**
    - TOTP via Keycloak (Google Authenticator)
    - Sem código extra no backend
    - Usuário `marcus` configurado e testável

3. **Observabilidade end-to-end**
    - TraceId em todos os logs
    - Headers de tracing nas respostas
    - Métricas de segurança (auth failures, rate limits)

4. **Testes automatizados executáveis**
    - 42 testes (script bash) → senior pode rodar e ver 42/42 PASS
    - 117 classes JUnit (629 métodos) → coverage ≥80%

5. **ADRs documentam tradeoffs**
    - Por que dual auth (não só Keycloak)?
    - Por que SAML está em design (não implementado)?
    - Decisões técnicas justificadas

6. **OWASP compliance**
    - Rate limiting (DoS protection)
    - Account lockout (brute-force)
    - SQL injection filter
    - Password policy
    - Bcrypt (não plaintext)
    - Refresh token rotation

7. **Arquitetura extensível**
    - SAML design pronto (implement when needed)
    - Multi-tenancy preparado (Keycloak realms)
    - Escalável (stateless JWT + Redis distribuído)

---

**Última atualização:** 2025-03-13  
**Autor:** Marcus Prado  
**Versão:** 1.0.0
