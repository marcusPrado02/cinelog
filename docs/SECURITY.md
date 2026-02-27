# 🔒 Documentação de Segurança - CineLog

## Índice

1. [Visão Geral](#visão-geral)
2. [Autenticação](#autenticação)
3. OWASP Top 10:2025
    - [A01:2025 — Broken Access Control](#a012025-owasp--broken-access-control)
    - [A02:2025 — Security Misconfiguration](#a022025-owasp--security-misconfiguration)
    - [A03:2025 — Software Supply Chain Failures](#a032025-owasp--software-supply-chain-failures)
    - [A04:2025 — Cryptographic Failures](#a042025-owasp--cryptographic-failures)
    - [A05:2025 — Injection](#a052025-owasp--injection)
    - [A06:2025 — Insecure Design](#a062025-owasp--insecure-design)
    - [A07:2025 — Authentication Failures](#a072025-owasp--authentication-failures)
    - [A08:2025 — Software or Data Integrity Failures](#a082025-owasp--software-or-data-integrity-failures)
    - [A09:2025 — Security Logging and Alerting Failures](#a092025-owasp--security-logging-and-alerting-failures)
    - [A10:2025 — Mishandling of Exceptional Conditions](#a102025-owasp--mishandling-of-exceptional-conditions)
4. [Proteções Implementadas](#proteções-implementadas)
5. [Boas Práticas](#boas-práticas)
6. [Configuração](#configuração)
7. [Auditoria](#auditoria)
8. [Resposta a Incidentes](#resposta-a-incidentes)
9. [Checklist de Segurança](#checklist-de-segurança)

---

## Visão Geral

O CineLog implementa múltiplas camadas de segurança para proteger dados e funcionalidades.

### Princípios de Segurança

1. **Defense in Depth**: Múltiplas camadas de proteção
2. **Least Privilege**: Acesso mínimo necessário
3. **Secure by Default**: Configurações seguras por padrão
4. **Zero Trust**: Validação contínua
5. **Privacy by Design**: Privacidade desde o design

---

## Autenticação

### JWT (JSON Web Tokens)

#### Estrutura

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9  ← Header
.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0  ← Payload
.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c  ← Signature
```

#### Claims do Token

```json
{
    "sub": "user123",
    "email": "user@example.com",
    "roles": ["USER"],
    "iat": 1701345600,
    "exp": 1701349200
}
```

#### Configuração

```yaml
cinelog:
    security:
        jwt:
            secret: ${JWT_SECRET}
            expiration-seconds: 3600 # 1 hora
```

#### Implementação

```java
@Component
public class JwtTokenProvider {

    @Value("${cinelog.security.jwt.secret}")
    private String secret;

    @Value("${cinelog.security.jwt.expiration-seconds}")
    private long expirationSeconds;

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
            .setSubject(Long.toString(userPrincipal.getId()))
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }
}
```

### Refresh Tokens

**Fluxo**:

```
1. Login → Access Token (1h) + Refresh Token (30 dias)
2. Access Token expira
3. Cliente usa Refresh Token
4. Server valida e retorna novo Access Token
5. Repetir até Refresh Token expirar
```

**Implementação**:

```java
@PostMapping("/refresh")
public TokenResponse refresh(@RequestBody RefreshTokenRequest request) {
    String refreshToken = request.getRefreshToken();

    if (!jwtTokenProvider.validateToken(refreshToken)) {
        throw new InvalidTokenException("Refresh token inválido");
    }

    Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
    User user = userService.findById(userId);

    String newAccessToken = jwtTokenProvider.generateAccessToken(user);

    return new TokenResponse(newAccessToken, refreshToken);
}
```

---

## A01:2025 (OWASP) — Broken Access Control

> **O que é?** Controle de acesso quebrado acontece quando um sistema permite que usuários realizem ações
> ou acessem recursos para os quais **não têm permissão**. É o risco #1 do OWASP Top 10 porque
> ocorre com altíssima frequência e tem impacto direto: vazamento de dados, manipulação indevida,
> escalação de privilégios.

> **Cenários reais de ataque que este tópico previne:**
>
> - Um usuário comum altera a URL de `/api/v1/users/me` para `/api/v1/admin/users` e acessa a lista de todos os usuários.
> - Um atacante descobre `/actuator/env` aberto e extrai variáveis de ambiente (inclusive secrets).
> - Um endpoint de delete não verifica a role e qualquer autenticado consegue apagar recursos.

O CineLog aplica autorização em **duas camadas complementares** (defense in depth):

1. **Camada 1 — URL Authorization**: filtro HTTP que intercepta **toda request** antes de chegar ao controller.
2. **Camada 2 — Method Security**: anotações no próprio método Java que validam permissões granulares.

Se uma camada falhar (ex: alguém remove um `@PreAuthorize` por engano), a outra ainda bloqueia.

---

### Camada 1: URL Authorization (`HttpSecurity.authorizeHttpRequests`)

> **O que é?** É a configuração central do Spring Security que define, **por padrão, para toda URL
> da aplicação**, quem pode acessar o quê. Funciona como um "porteiro" na entrada: antes mesmo
> do código do controller executar, o Spring verifica a request contra essas regras.

> **Conceitos-chave:**
>
> - **`permitAll()`** — qualquer pessoa (mesmo sem login) pode acessar. Usado para endpoints públicos.
> - **`authenticated()`** — exige um token JWT válido. Qualquer usuário logado acessa.
> - **`hasRole("ADMIN")`** — exige que o JWT contenha a role `ROLE_ADMIN`.
> - **`hasAnyRole("ADMIN", "OPS")`** — aceita qualquer uma das roles listadas.
> - **`anyRequest().authenticated()`** — regra "pega-tudo" no final: tudo que não foi explicitamente
>   liberado acima **requer autenticação**. Isso é o princípio **deny-by-default**.

Configuração atual no projeto:

```java
http.authorizeHttpRequests(auth -> auth
    // Endpoints PÚBLICOS — sem token necessário
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/auth/**").permitAll()

    // Atuator — apenas health check e info (superfície mínima)
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()

    // ADMIN — somente role ADMIN
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

    // Admin DLQ — aceita ADMIN ou OPS (equipe de operações)
    .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPS")

    // TUDO MAIS — requer autenticação (deny-by-default)
    .anyRequest().authenticated());
```

**Por que o actuator é restrito?** Endpoints como `/actuator/env`, `/actuator/heapdump` e
`/actuator/configprops` podem expor secrets, estrutura interna e dados de memória.
Deixar apenas `/health` e `/info` públicos segue o princípio de **superfície mínima de ataque**.

**Por que `/admin/**`tem regra separada?** O controller`DeadLetterAdminController`usa o path`/admin/dlq/**`(fora de`/api/v1/admin/**`). Sem essa regra explícita, a URL cairia no
`anyRequest().authenticated()` e **qualquer usuário logado** acessaria funcionalidades de admin.

---

### Camada 2: Method Security (`@PreAuthorize` e `@SecureOperation`)

> **O que é?** São anotações colocadas diretamente nos métodos Java. Mesmo que a URL passe pelo
> filtro HTTP, o Spring valida **novamente** no nível do método se o usuário tem a permissão
> necessária. É a segunda barreira (defense in depth).

> **`@EnableMethodSecurity`** — ativa esse mecanismo no Spring. Sem essa anotação, `@PreAuthorize`
> e `@SecureOperation` são **ignorados silenciosamente** (risco grave).

#### `@SecureOperation` — anotação customizada do CineLog

> **O que é `enforce`?** É um flag booleano que controla se a anotação **bloqueia** (`true`) ou
> apenas **registra métrica** (`false`).
>
> - `enforce=true` (padrão): se o usuário não tiver a permission, lança `AccessDeniedException` → HTTP 403.
> - `enforce=false`: permite a execução mas registra métrica `cinelog.security.access_total` com `outcome=DENIED`.
>   Útil para observabilidade em migração gradual de permissões.
>
> **Por que `enforce=true` é o padrão?** Segue o princípio **fail-closed** (na dúvida, nega).
> Se alguém adicionar `@SecureOperation` sem especificar `enforce`, o comportamento seguro é o default.

> **O que é `value`?** É a **permission string** que o usuário precisa ter nas suas `GrantedAuthority`
> do Spring Security. Ex: `"CONTENT_ADMIN"`, `"USER_ADMIN"`, `"MEDIA_ADMIN"`.
> Se `value` estiver **vazio** e `enforce=true`, o acesso é **negado** (fail-closed — não existe
> "permissão em branco").

Exemplo real do projeto:

```java
// Somente quem tem authority "USER_ADMIN" pode executar
@SecureOperation(module = "USER", value = "USER_ADMIN")
public User execute(User user) { ... }

// Somente quem tem authority "CONTENT_ADMIN" pode deletar gêneros
@SecureOperation(module = "GENRE", value = "CONTENT_ADMIN")
public void execute(Long genreId) { ... }
```

---

### Resumo: o que cada regra previne

| Proteção                                    | Cenário de ataque prevenido                            |
| ------------------------------------------- | ------------------------------------------------------ |
| `anyRequest().authenticated()`              | Acesso anônimo a qualquer endpoint não público         |
| `.hasRole("ADMIN")` em `/api/v1/admin/**`   | Usuário comum acessando painel admin                   |
| `.hasAnyRole("ADMIN","OPS")` em `/admin/**` | Usuário comum acessando DLQ admin                      |
| Actuator restrito a `/health` e `/info`     | Atacante extraindo secrets via `/actuator/env`         |
| `@SecureOperation(enforce=true)`            | Método executado sem permission mesmo com URL liberada |
| `@EnableMethodSecurity`                     | Anotações de segurança sendo silenciosamente ignoradas |

### Checklist A01:2025

1. Usuário sem token em endpoint protegido → `401 Unauthorized`.
2. Usuário autenticado sem role adequada em `/admin/**` → `403 Forbidden`.
3. Usuário com `ROLE_ADMIN` ou `ROLE_OPS` em `/admin/**` → acesso permitido.
4. Endpoint com `@SecureOperation` sem authority requerida → `403` quando `enforce=true`.
5. `/actuator/env` ou `/actuator/heapdump` sem token → `401` (não está no `permitAll`).

---

## A02:2025 (OWASP) — Security Misconfiguration

### O que é?

> **Security Misconfiguration** é quando o sistema está tecnicamente funcional mas
> **configurado de forma insegura**. É o risco mais comum do OWASP Top 10 porque
> envolve não apenas código, mas **configuração de servidor, framework, banco de dados,
> cloud e dependências**.
>
> **Analogia:** é como ter uma casa com fechadura de alta segurança, mas deixar a chave
> debaixo do tapete. A fechadura (código) está correta — o problema é a **configuração**.
>
> **Por que é tão frequente?**
>
> - Frameworks vêm com configurações **permissivas** para facilitar desenvolvimento
> - Desenvolvedores copiam configs de dev para prod sem revisar
> - Features de debug/diagnóstico ficam ativadas em produção
> - Valores padrão são documentados publicamente (atacantes conhecem todos)
>
> **Exemplos reais de misconfiguration:**
>
> | Misconfiguration                      | O que o atacante ganha                                       |
> | ------------------------------------- | ------------------------------------------------------------ |
> | Swagger aberto em produção            | Mapa completo da API sem esforço de reconhecimento           |
> | `/actuator/env` público               | JWT_SECRET, credenciais do banco de dados                    |
> | `/actuator/heapdump` público          | Tokens JWT ativos, dados pessoais em memória                 |
> | Stack traces em respostas HTTP        | Nome de classes, estrutura interna, versão de libs           |
> | CORS com `*` (qualquer origem)        | Roubar dados do usuário via site malicioso                   |
> | Headers sem `nosniff`, sem HSTS       | MIME sniffing → XSS, downgrade para HTTP → man-in-the-middle |
> | `Server: Apache-Coyote/1.1` no header | Identificação da stack → busca de CVEs específicas           |

---

### Proteções implementadas

---

#### 1. Security Headers HTTP

##### O que são?

> São headers de **resposta** que instruem o browser a ativar proteções de segurança.
> O servidor diz ao browser: "para este site, aplique estas regras". Sem eles, o browser
> usa comportamentos padrão que frequentemente são inseguros (retrocompatibilidade).

##### Headers configurados no CineLog

> | Header                      | Valor                                                      | O que previne            | Cenário de ataque                                                            |
> | --------------------------- | ---------------------------------------------------------- | ------------------------ | ---------------------------------------------------------------------------- |
> | `X-Content-Type-Options`    | `nosniff`                                                  | **MIME sniffing**        | Upload de `.jpg` que é HTML+JS → browser executa script → XSS                |
> | `X-Frame-Options`           | `DENY`                                                     | **Clickjacking**         | Site malicioso coloca CineLog num `<iframe>` invisível e engana cliques      |
> | `Strict-Transport-Security` | `max-age=31536000; includeSubDomains`                      | **Downgrade HTTPS→HTTP** | Atacante na mesma WiFi força HTTP e intercepta tráfego (MITM)                |
> | `Content-Security-Policy`   | `default-src 'self'; script-src 'self'; ...`               | **XSS, data injection**  | Mesmo que atacante injete `<script src="evil.com">`, browser recusa carregar |
> | `Referrer-Policy`           | `strict-origin-when-cross-origin`                          | **Vazamento de URL**     | Token em query string não é enviado para sites externos via Referer          |
> | `Permissions-Policy`        | `camera=(), microphone=(), geolocation=(), payment=()`     | **Acesso a hardware**    | Site comprometido não ativa câmera/microfone silenciosamente                 |
> | `Cache-Control`             | `no-store, no-cache, must-revalidate, private` (em `/api`) | **Cache de dados**       | Após logout, dados pessoais não ficam no cache do browser/proxy              |
> | `Server`                    | _(vazio)_                                                  | **Fingerprinting**       | Atacante não descobre versão do Tomcat → não busca CVEs específicas          |
> | `X-Powered-By`              | _(removido)_                                               | **Fingerprinting**       | Idem — remove identificação da tecnologia                                    |

##### O que é MIME sniffing?

> O browser tenta "adivinhar" o tipo de um arquivo ignorando o `Content-Type` declarado
> pelo servidor. Lê os primeiros bytes e compara com assinaturas conhecidas. Um arquivo com
> `<html>` nos primeiros bytes pode ser interpretado como HTML mesmo que o servidor declare
> `image/jpeg`. Com `nosniff`, o browser **respeita** o Content-Type declarado.

##### O que é Clickjacking?

> ```
> ┌─── Site do atacante (visível) ──────────┐
> │                                          │
> │  "Clique aqui para ganhar um iPhone!"    │
> │         [ CLIQUE AQUI ]                  │
> │                                          │
> │  ┌── CineLog em iframe (invisível) ──┐  │
> │  │                                    │  │
> │  │     [ Deletar minha conta ]  ←──── │──│── Botão real
> │  │                                    │  │
> │  └────────────────────────────────────┘  │
> └──────────────────────────────────────────┘
> ```
>
> Com `X-Frame-Options: DENY`, o browser **recusa** carregar o CineLog dentro de iframe.

##### O que é Content-Security-Policy (CSP)?

> CSP é uma **whitelist** de recursos que o browser pode carregar:
>
> ```
> Content-Security-Policy: default-src 'self'; script-src 'self'; ...
> ```
>
> | Diretiva                           | Significado                                            |
> | ---------------------------------- | ------------------------------------------------------ |
> | `default-src 'self'`               | Por padrão, só carrega do próprio domínio              |
> | `script-src 'self'`                | Scripts só do próprio domínio (bloqueia CDNs externas) |
> | `style-src 'self' 'unsafe-inline'` | Estilos do domínio + inline (Swagger UI)               |
> | `frame-ancestors 'none'`           | Ninguém pode embeber este site em iframe               |
> | `base-uri 'self'`                  | Impede que `<base>` redirecione URLs relativas         |
> | `form-action 'self'`               | Formulários só enviam para o próprio domínio           |
>
> Mesmo que um atacante injete `<script src="https://evil.com/keylogger.js">`,
> o browser **recusa carregar** porque `evil.com` não está na whitelist.

Implementação: `com.cine.cinelog.shared.config.SecurityHeadersConfig`

---

#### 2. CORS restritivo por profile

##### O que é CORS?

> **Cross-Origin Resource Sharing** controla quais **origens** (domínios)
> podem acessar a API via JavaScript no browser.
>
> **O que é uma "origem"?** Combinação de `protocolo + domínio + porta`:
>
> ```
> https://cinelog.com       ← origem A
> https://api.cinelog.com   ← origem B (domínio diferente)
> http://localhost:3000     ← origem C
> http://localhost:8080     ← origem D (porta diferente)
> ```

##### Cenário de ataque sem CORS restritivo

> ```
> 1. Usuário está logado no CineLog (JWT armazenado no browser)
> 2. Visita evil.com (site do atacante)
> 3. JavaScript em evil.com faz: fetch("https://api.cinelog.com/api/users/me")
> 4. Browser envia o JWT automaticamente (credentials: 'include')
> 5. Se CORS permite "*", a resposta chega ao JavaScript do atacante
> 6. Atacante lê dados pessoais, reviews, watchlist...
> ```

##### Configuração por profile

> | Profile  | Origens permitidas                                     | Justificativa              |
> | -------- | ------------------------------------------------------ | -------------------------- |
> | **dev**  | `localhost:3000`, `localhost:5173`, `localhost:4200`   | Frontend React/Vue/Angular |
> | **prod** | Apenas `${CORS_ALLOWED_ORIGINS}` (env var obrigatória) | Domínio real do frontend   |
>
> **⚠️ NUNCA usar `*` com credentials!** CORS com `allowCredentials(true)` e
> `allowedOrigins("*")` é **rejeitado** pelos browsers modernos (e pelo Spring).
> É obrigatório listar origens explicitamente.

```yaml
# Produção: origins via variável de ambiente
cinelog:
  security:
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS}

# Dev: múltiplas origens para desenvolvimento local
cinelog:
  security:
    cors:
      allowed-origins: http://localhost:3000,http://localhost:5173,http://localhost:4200
```

Implementação: `com.cine.cinelog.shared.config.CorsConfig`

---

#### 3. Actuator protegido por SecurityFilterChain dedicado

##### O que é o Actuator?

> São endpoints operacionais que o Spring Boot expõe. **Essenciais para ops,
> perigosíssimos se públicos.**
>
> **Tabela de risco por endpoint:**
>
> | Endpoint                | Risco          | O que expõe                                          |
> | ----------------------- | -------------- | ---------------------------------------------------- |
> | `/actuator/health`      | 🟢 Baixo       | Apenas UP/DOWN (sem detalhes em prod)                |
> | `/actuator/info`        | 🟢 Baixo       | Versão da build                                      |
> | `/actuator/prometheus`  | 🟢 Baixo       | Métricas (necessário para Grafana)                   |
> | `/actuator/metrics`     | 🟡 Médio       | Métricas de performance (pode revelar carga)         |
> | `/actuator/env`         | 🔴 **Crítico** | **Variáveis de ambiente** (JWT_SECRET, DB password!) |
> | `/actuator/configprops` | 🔴 **Crítico** | Todas as propriedades (podem ter secrets)            |
> | `/actuator/heapdump`    | 🔴 **Crítico** | **Dump de memória** (tokens ativos, dados em cache!) |
> | `/actuator/beans`       | 🟠 Alto        | Todos os beans Spring (revela arquitetura)           |
> | `/actuator/mappings`    | 🟠 Alto        | Todos os endpoints HTTP (mapa da API)                |
> | `/actuator/threaddump`  | 🟠 Alto        | Estado de threads (pode ter dados sensíveis)         |

##### Configuração por profile

> | Profile  | Endpoints expostos                                      | Autenticação                                 |
> | -------- | ------------------------------------------------------- | -------------------------------------------- |
> | **dev**  | health, info, env, beans, mappings, metrics, prometheus | Nenhuma (dev local)                          |
> | **prod** | **Apenas health, info, prometheus**                     | health/info/prometheus público, demais ADMIN |

##### SecurityFilterChain dedicado

> A proteção usa um `SecurityFilterChain` com `@Order(1)` separado do chain principal
> (`@Order(2)`). Isso garante que requests para `/actuator/**` sejam avaliadas por
> regras específicas antes de cair no chain genérico:
>
> ```java
> @Configuration
> @Order(1)  // Avaliado ANTES do SecurityConfig (@Order 2)
> public class ActuatorSecurityConfig {
>
>     @Bean
>     SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
>         http
>             .securityMatcher(EndpointRequest.toAnyEndpoint())
>             .authorizeHttpRequests(auth -> auth
>                 .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
>                     .permitAll()
>                 .anyRequest().hasRole("ADMIN")  // Todos os demais exigem ADMIN
>             );
>         return http.build();
>     }
> }
> ```

```yaml
# application-prod.yml: endpoints sensíveis DESABILITADOS
management:
    endpoint:
        env:
            enabled: false # /actuator/env NUNCA em produção
        heapdump:
            enabled: false # /actuator/heapdump NUNCA em produção
        configprops:
            enabled: false # /actuator/configprops NUNCA em produção
        beans:
            enabled: false
        mappings:
            enabled: false
```

Implementação: `com.cine.cinelog.shared.config.ActuatorSecurityConfig`

---

#### 4. Swagger desabilitado em produção

##### Por que desabilitar?

> O Swagger UI expõe em uma interface interativa:
>
> - **Todos os endpoints** da API (incluindo admin e internos)
> - **Schemas** completos de request/response (estrutura dos DTOs)
> - **Exemplos** de payloads válidos
> - **"Try it out"** — interface para testar endpoints ao vivo
>
> Isso dá ao atacante um **mapa completo** da API sem nenhum esforço de reconhecimento.
> É como entregar a planta da casa para um ladrão.

##### Como funciona

> A classe `OpenApiConfig` usa `@Profile({"dev", "test", "default"})`,
> o que significa que os beans do Swagger só são registrados nesses profiles.
> Em produção, o bean simplesmente **não existe**, e o `springdoc` é desabilitado
> via `application-prod.yml`:
>
> ```yaml
> # application-prod.yml
> springdoc:
>     swagger-ui:
>         enabled: false # UI desabilitada
>     api-docs:
>         enabled: false # JSON da spec desabilitado
> ```
>
> **Defesa em profundidade:** mesmo em dev, o Swagger exige JWT para endpoints
> protegidos (configurado via `SecurityScheme` no OpenAPI).

---

#### 5. Configuração de erros por profile

##### O que pode vazar em mensagens de erro?

> | Propriedade Spring               | O que expõe quando ativa                                |
> | -------------------------------- | ------------------------------------------------------- |
> | `include-message: always`        | Mensagem da exceção Java (pode ter SQL, paths internos) |
> | `include-stacktrace: always`     | Stack trace completo (classes, linhas, versão de libs)  |
> | `include-exception: true`        | Nome completo da classe de exceção                      |
> | `include-binding-errors: always` | Nomes de campos e regras de validação internas          |
>
> **Cenário real:**
>
> ```json
> // ❌ Resposta em produção com include-stacktrace: always
> {
>   "status": 500,
>   "error": "Internal Server Error",
>   "trace": "org.postgresql.util.PSQLException: ERROR: relation \"users\" ...\n
>             at org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(v6.4.1)\n
>             at com.cine.cinelog.features.users.repository.UserRepository..."
> }
> // Atacante agora sabe: PostgreSQL, Hibernate 6.4.1, estrutura de packages
> ```

##### Configuração segura

> | Profile  | include-message | include-stacktrace | include-exception | whitelabel |
> | -------- | --------------- | ------------------ | ----------------- | ---------- |
> | **base** | `never`         | `never`            | `false`           | `false`    |
> | **dev**  | `always`        | `on_param`         | (herda base)      | (herda)    |
> | **prod** | `never`         | `never`            | `false`           | `false`    |

---

#### 6. Remoção de fingerprinting

##### O que é fingerprinting?

> Técnica de **identificar tecnologias** usadas pelo servidor analisando headers,
> mensagens de erro e formatos de resposta.
>
> ```http
> # ❌ ANTES (expõe tecnologia)
> Server: Apache-Coyote/1.1
> X-Powered-By: Spring Boot 3.3.0
>
> # ✅ DEPOIS (nenhuma informação)
> Server:
> (X-Powered-By removido)
> ```
>
> **Por que importa?** Cada versão de software tem CVEs publicadas. Se o atacante sabe
> que é "Tomcat 10.1.24", busca vulnerabilidades específicas em 30 segundos. Sem essa
> informação, precisa testar genericamente — muito mais lento e ruidoso nos logs.

Implementação: `SecurityHeadersConfig` (remove `Server` e `X-Powered-By`) + `server.server-header: ""` no YAML

---

### Comparativo dev vs prod

> | Aspecto               | Dev                                                     | Prod                             |
> | --------------------- | ------------------------------------------------------- | -------------------------------- |
> | Swagger               | ✅ Ativo                                                | ❌ Desabilitado                  |
> | Stack traces          | ✅ Visíveis (`on_param`)                                | ❌ Nunca                         |
> | Actuator endpoints    | health, info, env, beans, mappings, metrics, prometheus | Apenas health, info, prometheus  |
> | Actuator show-details | `always`                                                | `never`                          |
> | CORS origins          | localhost:3000/5173/4200                                | Apenas `${CORS_ALLOWED_ORIGINS}` |
> | Error messages        | Detalhadas                                              | Genéricas                        |
> | SQL logging           | DEBUG                                                   | WARN                             |
> | Security logging      | DEBUG                                                   | WARN                             |
> | Heapdump              | Disponível                                              | **Desabilitado**                 |
> | Server header         | Vazio                                                   | Vazio                            |

---

### Checklist A02:2025

- [x] Security headers completos (CSP, HSTS, nosniff, X-Frame-Options, Permissions-Policy, Referrer-Policy)
- [x] Headers de fingerprinting removidos (Server, X-Powered-By)
- [x] Cache-Control em respostas de API (`no-store, private`)
- [x] CORS restritivo por profile (nunca `*` com credentials)
- [x] Actuator protegido: SecurityFilterChain dedicado com @Order(1)
- [x] Actuator: heapdump, env, configprops desabilitados em prod
- [x] Swagger desabilitado em produção via profile + springdoc config
- [x] OpenApiConfig com `@Profile({"dev", "test", "default"})`
- [x] Mensagens de erro sem stack trace em produção
- [x] `server.server-header: ""` (não expõe Tomcat)
- [x] `whitelabel.enabled: false`
- [x] Configurações separadas por profile (application-dev/prod.yml)
- [ ] Scan de CVEs em dependências (OWASP Dependency Check) — **pendente**
- [ ] Security headers validados via securityheaders.com — **pendente**
- [ ] CSP em modo report-only para ajuste fino — **pendente**

---

## A03:2025 (OWASP) — Software Supply Chain Failures

### O que é?

> **Software Supply Chain Failures** acontece quando o software é comprometido através
> de seus **componentes de terceiros** — bibliotecas, frameworks, plugins, imagens de
> container ou ferramentas de build. É uma categoria **nova no OWASP 2025**, desmembrada
> de A06:2021 (Vulnerable and Outdated Components) e A08:2021 (Software and Data
> Integrity Failures).
>
> **Analogia:** imagine que você cozinha em casa com ingredientes comprados no mercado.
> Mesmo que sua técnica culinária seja perfeita, se um dos ingredientes estiver
> **contaminado na fábrica**, o prato final está comprometido. Você não escreveu o
> código da dependência, mas ela roda com os **mesmos privilégios** da sua aplicação.
>
> **Números que importam:**
>
> | Estatística                                                | Fonte                          |
> | ---------------------------------------------------------- | ------------------------------ |
> | 80% do código de uma aplicação moderna vem de dependências | Synopsys OSSRA 2024            |
> | 1 em cada 8 downloads de open-source contém CVE conhecida  | Sonatype State of SSC 2024     |
> | Tempo médio entre CVE publicada e exploit: **5 dias**      | Mandiant M-Trends 2024         |
> | 96% das vulnerabilidades têm patch disponível              | Snyk Open Source Security 2024 |
>
> **Tipos de ataque à supply chain:**
>
> | Tipo                              | Como funciona                                                          | Exemplo real                                              |
> | --------------------------------- | ---------------------------------------------------------------------- | --------------------------------------------------------- |
> | **Dependency Confusion**          | Atacante publica pacote malicioso com nome similar no registry público | `ua-parser-js` npm (2021) — 8M downloads/semana           |
> | **Typosquatting**                 | Nome quase idêntico ao original (`sprig-boot` em vez de `spring-boot`) | `colors.js` e `faker.js` (2022)                           |
> | **Comprometimento de maintainer** | Conta do mantenedor é invadida; atacante publica versão maliciosa      | `event-stream` npm (2018) — roubo de Bitcoin              |
> | **CVE não corrigida**             | Dependência usa versão com vulnerabilidade conhecida há meses          | Log4Shell (CVE-2021-44228) — afetou milhões de aplicações |
> | **Build pipeline poisoning**      | Atacante compromete o CI/CD para injetar código malicioso              | SolarWinds Orion (2020) — 18.000 organizações afetadas    |
> | **Licença restritiva**            | Dependência usa GPL/AGPL, forçando open-source do projeto inteiro      | Problemas legais em software proprietário                 |

---

### Proteções implementadas

---

#### 1. OWASP Dependency-Check (CVE scanning)

##### O que é?

> É uma ferramenta que escaneia **todas as dependências** do projeto (diretas e
> transitivas) e compara contra o **NVD** (National Vulnerability Database do NIST) —
> o maior banco de dados público de vulnerabilidades do mundo.
>
> **Como funciona:**
>
> ```
> pom.xml (dependências declaradas)
>     │
>     ▼
> Maven resolve dependências transitivas
>     │
>     ▼ (todas as .jar, incluindo dependências de dependências)
> ┌─────────────────────────────────────┐
> │ OWASP Dependency-Check              │
> │                                     │
> │ 1. Identifica cada .jar (CPE match) │
> │ 2. Consulta NVD (CVE database)      │
> │ 3. Calcula CVSS score               │
> │ 4. Gera relatório HTML/JSON/SARIF   │
> │ 5. FALHA se score >= 7.0 (HIGH)     │
> └─────────────────────────────────────┘
>     │
>     ▼
> Build PASSA ou FALHA
> ```
>
> **O que é CVSS?** Common Vulnerability Scoring System — escala de 0 a 10 que
> classifica a severidade de uma vulnerabilidade:
>
> | Score      | Severidade   | Ação no CineLog                             |
> | ---------- | ------------ | ------------------------------------------- |
> | 0.0        | Nenhuma      | Informativo                                 |
> | 0.1 – 3.9  | **LOW**      | Monitorar, atualizar no próximo sprint      |
> | 4.0 – 6.9  | **MEDIUM**   | Atualizar em até 30 dias                    |
> | 7.0 – 8.9  | **HIGH**     | ⛔ **Build falha**, atualizar imediatamente |
> | 9.0 – 10.0 | **CRITICAL** | ⛔ **Build falha**, hotfix emergencial      |
>
> **Por que o threshold é 7.0?** CVEs HIGH e CRITICAL frequentemente têm exploits
> públicos e são ativamente exploradas. Permitir deploy com essas vulnerabilidades
> é risco inaceitável.

##### Como usar

> ```bash
> # Executar scan de CVEs
> ./mvnw dependency-check:check
>
> # Relatório gerado em:
> # target/dependency-check/dependency-check-report.html  (visual)
> # target/dependency-check/dependency-check-report.json  (consumo por ferramentas)
> ```
>
> **Exemplo de saída quando encontra CVE:**
>
> ```
> [WARNING] CVE-2024-22259 — spring-web-6.1.4.jar
>   CVSS Score: 8.1 (HIGH)
>   Descrição: Open redirect vulnerability in UriComponentsBuilder
>   Solução: Atualizar para spring-web >= 6.1.5
>
> [ERROR] Build falhou: 1 dependência com CVSS >= 7.0
> ```

##### Supressão de falsos positivos

> Nem toda CVE reportada é aplicável. Pode ser:
>
> - **Falso positivo:** a ferramenta associou a CVE errada ao componente
> - **Não aplicável:** a CVE afeta uma funcionalidade que o CineLog não usa
>
> Para esses casos, usamos o arquivo de supressões:
> `src/main/resources/dependency-check-suppressions.xml`
>
> **Regras para suprimir:**
>
> 1. Cada supressão **DEVE** ter comentário explicando **por que** não se aplica
> 2. **DEVE** incluir data da análise e responsável
> 3. **DEVE** ser revisada a cada 90 dias (quarterly review)
> 4. **NUNCA** suprimir sem investigar primeiro

---

#### 2. SBOM — Software Bill of Materials (CycloneDX)

##### O que é?

> SBOM é um **inventário completo** de todos os componentes, bibliotecas e dependências
> que compõem o software — incluindo versão, licença e hashes criptográficos.
>
> **Analogia:** é o "rótulo de ingredientes" (tabela nutricional) do software. Assim
> como alimentos são obrigados a listar todos os ingredientes, software crítico está
> sendo obrigado a fornecer SBOM (Executive Order 14028 dos EUA, 2021).
>
> **Para que serve:**
>
> | Caso de uso                | Exemplo                                                                                 |
> | -------------------------- | --------------------------------------------------------------------------------------- |
> | **Resposta a incidentes**  | Log4Shell publicada → em 5 minutos sabe se o projeto usa Log4j                          |
> | **Auditoria de segurança** | Auditor externo valida que todas as dependências estão atualizadas                      |
> | **Compliance regulatório** | Governo/enterprise exige SBOM para contratos                                            |
> | **Gestão de licenças**     | Identifica se alguma dependência tem licença incompatível (GPL em projeto proprietário) |
>
> **Formato CycloneDX 1.5:**
>
> ```json
> {
>     "bomFormat": "CycloneDX",
>     "specVersion": "1.5",
>     "components": [
>         {
>             "type": "library",
>             "name": "spring-boot-starter-web",
>             "version": "3.5.9",
>             "group": "org.springframework.boot",
>             "hashes": [{ "alg": "SHA-256", "content": "a1b2c3d4..." }],
>             "licenses": [{ "license": { "id": "Apache-2.0" } }]
>         }
>     ]
> }
> ```

##### Como usar

> ```bash
> # Gerar SBOM
> ./mvnw cyclonedx:makeBom
>
> # Arquivos gerados:
> # target/cinelog-sbom.json   (formato JSON, consumo por ferramentas)
> # target/cinelog-sbom.xml    (formato XML, padrão OWASP)
> ```
>
> O SBOM é gerado **automaticamente** durante `mvn package` e publicado como
> artefato no GitHub Actions (retido por 90 dias).

---

#### 3. Maven Enforcer (regras de build)

##### O que é?

> É um plugin que **impõe regras** no processo de build. Se qualquer regra for violada,
> o build **falha imediatamente** (fail-fast). Funciona como um "guarda de trânsito"
> que impede práticas inseguras.
>
> **Regras configuradas:**
>
> | Regra                               | O que previne                     | Cenário de ataque                                               |
> | ----------------------------------- | --------------------------------- | --------------------------------------------------------------- |
> | `requireJavaVersion [21,)`          | Build com Java desatualizado      | Java < 21 sem patches de segurança recentes                     |
> | `requireMavenVersion [3.9,)`        | Maven antigo com bugs conhecidos  | Maven < 3.9 com vulnerabilidades no resolver                    |
> | `banDuplicatePomDependencyVersions` | Duas versões da mesma dependência | Versão vulnerável sobrescreve versão corrigida                  |
> | `requireReleaseDeps`                | Dependências SNAPSHOT em release  | SNAPSHOT pode mudar a qualquer momento → build não reproduzível |
>
> **O que é o problema de dependências duplicadas?**
>
> ```
> cinelog-app
> ├── spring-boot-starter-web
> │   └── spring-core:6.1.5        ← versão A
> └── spring-boot-starter-data-jpa
>     └── spring-core:6.1.3        ← versão B (conflito!)
> ```
>
> Sem o Enforcer, o Maven escolhe silenciosamente uma das versões (regra
> de "nearest wins"). Se escolher 6.1.3 e o fix de segurança está em 6.1.5, o projeto
> está vulnerável sem ninguém saber. O Enforcer **força** a resolução explícita.

##### Como usar

> ```bash
> # Executar regras do Enforcer
> ./mvnw enforcer:enforce
> ```

---

#### 4. Dependabot (atualizações automáticas)

##### O que é?

> É um serviço **integrado ao GitHub** que monitora as dependências do projeto e
> cria **Pull Requests automaticamente** quando:
>
> 1. Uma nova versão é publicada (patch, minor ou major)
> 2. Uma CVE é descoberta numa dependência usada
>
> **Fluxo:**
>
> ```
> Maven Central publica spring-boot 3.5.10 (patch de segurança)
>     │
>     ▼
> Dependabot detecta (scan semanal, segunda-feira 06:00 BRT)
>     │
>     ▼
> Cria PR automático: "deps: bump spring-boot from 3.5.9 to 3.5.10"
>     │
>     ▼
> CI roda (testes, dependency-check, SBOM)
>     │
>     ▼
> Reviewer aprova e faz merge
> ```
>
> **Agrupamento inteligente:** dependências do mesmo ecossistema são agrupadas
> num único PR (ex: todas as libs Spring Boot juntas, todas as de teste juntas).
> Isso reduz ruído e facilita review.
>
> **Grupos configurados:**
>
> | Grupo           | Dependências agrupadas                             |
> | --------------- | -------------------------------------------------- |
> | `spring-boot`   | `org.springframework.boot*`, `cloud*`, `security*` |
> | `testing`       | `org.junit*`, `org.mockito*`, `org.assertj*`       |
> | `observability` | `io.micrometer*`, `io.opentelemetry*`              |

Configuração: `.github/dependabot.yml`

---

#### 5. Dependency Review (gate em PRs)

##### O que é?

> É um GitHub Action que analisa **apenas as dependências NOVAS ou ALTERADAS** num
> Pull Request. Diferente do Dependency-Check (que escaneia tudo), este foca
> especificamente no **delta** do PR.
>
> **Cenário prevenido:**
>
> ```
> Desenvolvedor adiciona no pom.xml:
>   <dependency>
>     <groupId>com.fasterxml.jackson.core</groupId>
>     <artifactId>jackson-databind</artifactId>
>     <version>2.13.0</version>  ← versão com CVE-2022-42003 (HIGH)
>   </dependency>
>
> Dependency Review:
>   ⛔ BLOCKED — jackson-databind:2.13.0 has known vulnerability
>              CVE-2022-42003 (CVSS: 7.5)
>   Recommendation: use version >= 2.13.4.2
>
> PR não pode ser mergeado até corrigir a versão.
> ```
>
> **Também verifica licenças:** PRs que introduzem dependências com licenças
> restritivas (GPL, AGPL) são **bloqueados** automaticamente.

Configuração: `.github/workflows/dependency-review.yml`

---

#### 6. Pipeline de segurança (CI/CD)

##### Visão geral do pipeline

> ```
> Push/PR para master
>     │
>     ├─── 🛡️ OWASP Dependency-Check ─── Relatório HTML/JSON → Artifacts
>     │                                    SARIF → GitHub Security tab
>     │
>     ├─── 📋 CycloneDX SBOM ──────────── cinelog-sbom.json → Artifacts (90 dias)
>     │
>     ├─── ⚖️ Maven Enforcer ──────────── Java 21+, Maven 3.9+, sem duplicatas
>     │
>     └─── 📜 License Check ───────────── Relatório de licenças → Artifacts
>
> Pull Request para master
>     │
>     └─── 🔎 Dependency Review ────────── Bloqueia CVE HIGH+ e licenças GPL
>
> Scan semanal (domingo 00:00 UTC)
>     │
>     └─── 🛡️ OWASP Dependency-Check ─── Detecta CVEs publicadas durante a semana
> ```
>
> **Por que scan semanal além do push?** CVEs novas são publicadas diariamente.
> Uma dependência que estava limpa na segunda pode ter CVE na sexta. O scan
> semanal garante que mesmo sem commits, o projeto é monitorado.

Configuração: `.github/workflows/security-scan.yml`

---

#### 7. Caso prático: remediação de CVEs (pom.xml)

##### Contexto

> O scanner OSV (Trunk) detectou **15 issues** em dependências transitivas do projeto
> CineLog — 6 HIGH, 6 MEDIUM, 2 LOW e 1 INFO. Este caso documenta o processo completo
> de análise, categorização e correção, servindo como referência para futuras remediações.

##### Metodologia de correção

> O processo segue a hierarquia de menor impacto:
>
> 1. **Atualizar o BOM pai** (Spring Boot) — corrige todas as dependências que ele gerencia
> 2. **Atualizar propriedades explicitas** (`kafka.version`) — corrige dependências controladas pelo projeto
> 3. **Override via `<dependencyManagement>`** — só para transitivas que nenhum BOM gerencia
>
> **Por que essa ordem?** Quanto menor o escopo da mudança, menor o risco de regressão.
> Subir o Spring Boot de 3.5.9 → 3.5.11 é um patch seguro que corrige múltiplas CVEs
> de uma vez. Override manual de transitivas é último recurso.

##### CVEs detectadas e correções aplicadas

> | Severidade | Dependência         | Versão vulnerável | Versão corrigida | CVE / Advisory                                     | Estratégia                                            |
> | ---------- | ------------------- | ----------------- | ---------------- | -------------------------------------------------- | ----------------------------------------------------- |
> | **HIGH**   | `jose4j`            | 0.9.4             | **0.9.6**        | GHSA-3677-xxcr-wjqv (DoS via JWE)                  | `<dependencyManagement>` override                     |
> | **HIGH**   | `kafka-clients`     | 3.9.0             | **3.9.2**        | GHSA-76qp-h5mr-frr4                                | Propriedade `kafka.version`                           |
> | **HIGH**   | `kafka_2.13`        | 3.9.0             | **3.9.2**        | GHSA-vgq5-3255-v292                                | Propriedade `kafka.version`                           |
> | **HIGH**   | `lz4-java`          | 1.8.0             | **1.8.1**        | GHSA-cmp6-m4wj-q63q, GHSA-vqf4-7m7x-wgfc           | Resolvida pelo Kafka 3.9.2 + `<dependencyManagement>` |
> | **HIGH**   | `assertj-core`      | 3.27.6            | **3.27.7**       | GHSA-rqfh-9r24-8c9r (XXE)                          | Spring Boot BOM 3.5.11                                |
> | **HIGH**   | `commons-beanutils` | 1.9.4             | **1.11.0**       | GHSA-wxr5-93ph-8wr9                                | `<dependencyManagement>` override                     |
> | **MEDIUM** | `commons-compress`  | 1.24.0            | **1.27.1**       | GHSA-4265-ccf5-phj5, GHSA-4g9r-vxhx-9pgx (OOM/DoS) | `<dependencyManagement>` override                     |
> | **MEDIUM** | `commons-lang3`     | 3.17.0            | 3.17.0           | GHSA-j288-q9x7-2f5v                                | Gerenciada pelo Liquibase — monitorando               |
> | **LOW**    | `logback-core`      | 1.5.22            | **1.5.32**       | GHSA-qqpg-mvqg-649v                                | Spring Boot BOM 3.5.11                                |
> | **INFO**   | `spring-boot`       | 3.5.9             | **3.5.11**       | Múltiplos patches                                  | Atualização de parent POM                             |
>
> **Bônus:** a atualização do Spring Boot e do Kafka trouxe upgrades adicionais:
> `spring-kafka` 3.3.11 → **3.3.13**, `zookeeper` 3.8.5 → **3.8.6**.

##### O que é `<dependencyManagement>`?

> É a seção do `pom.xml` que permite **sobrescrever a versão** de dependências
> transitivas (aquelas que vêm "de carona" com outra lib) sem adicioná-las
> diretamente no `<dependencies>`.
>
> **Por que é necessário?** Quando uma CVE está numa dependência transitiva
> (ex: `jose4j` vem via `kafka_2.13` → `jose4j`), você não controla qual versão
> o Kafka traz. O `<dependencyManagement>` faz o Maven usar a versão que **você**
> definiu, ignorando a que a dependência pai declarou.
>
> ```xml
> <dependencyManagement>
>     <dependencies>
>         <!-- Override: força jose4j 0.9.6 mesmo que kafka traga 0.9.4 -->
>         <dependency>
>             <groupId>org.bitbucket.b_c</groupId>
>             <artifactId>jose4j</artifactId>
>             <version>0.9.6</version>
>         </dependency>
>     </dependencies>
> </dependencyManagement>
> ```
>
> **Como verificar:** use `mvn dependency:tree -Dincludes=<groupId>:<artifactId>` para
> confirmar que a versão resolvida é a corrigida.

##### Comando de validação

> ```bash
> # 1. Verificar as versões resolvidas das dependências afetadas
> ./mvnw dependency:tree -DskipTests \
>   -Dincludes=org.bitbucket.b_c:jose4j,org.lz4:lz4-java,commons-beanutils:commons-beanutils,org.apache.commons:commons-compress
>
> # 2. Confirmar que o build compila sem erros
> ./mvnw compile -DskipTests
>
> # 3. Rodar enforcer para validar regras
> ./mvnw enforcer:enforce
>
> # 4. Scan completo de CVEs (precisa NVD_API_KEY para velocidade)
> ./mvnw dependency-check:check
> ```

---

### Checklist A03:2025

- [x] OWASP Dependency-Check integrado ao Maven (`failBuildOnCVSS=7`)
- [x] Arquivo de supressões com template documentado
- [x] SBOM gerado automaticamente (CycloneDX 1.5, JSON + XML)
- [x] Maven Enforcer: Java 21+, Maven 3.9+, sem duplicatas, sem SNAPSHOT
- [x] Dependabot configurado (scan semanal, agrupamento por ecossistema)
- [x] Dependency Review em PRs (bloqueia CVE HIGH+ e licenças GPL)
- [x] GitHub Actions: pipeline de segurança com 4 jobs
- [x] SARIF upload para GitHub Security tab
- [x] Scan semanal automático (cron domingo 00:00 UTC)
- [x] Relatórios retidos como artifacts (30-90 dias)
- [x] Remediação de CVEs via `<dependencyManagement>` + atualizações de BOM
- [x] Spring Boot 3.5.9 → 3.5.11, Kafka 3.9.0 → 3.9.2
- [ ] Verificação de assinatura GPG de artefatos Maven — **pendente**
- [ ] Integração com Snyk ou Grype para cobertura adicional — **pendente**
- [ ] SLSA (Supply-chain Levels for Software Artifacts) level 2+ — **pendente**
- [ ] Política de atualização: patch em 7 dias, minor em 30 dias — **pendente**

---

## A04:2025 (OWASP) — Cryptographic Failures

> **O que é?** Esta categoria cobre situações em que dados sensíveis (senhas, tokens, dados
> pessoais) são **expostos** porque não foram criptografados adequadamente — seja em trânsito
> (rede), em repouso (banco de dados) ou em logs/respostas de erro.

> **Cenários reais de ataque que este tópico previne:**
>
> - Atacante intercepta tráfego HTTP (man-in-the-middle) e captura tokens JWT e credenciais.
> - Banco de dados é comprometido e senhas são lidas porque estavam em texto plano.
> - Logs da aplicação são acessados e contêm tokens, senhas ou CPFs em claro.
> - Resposta de erro 500 inclui stack trace com nomes de tabelas, queries e paths internos.
> - JWT é forjado porque o secret é fraco demais (ex: "secret123").

### Proteções implementadas

---

#### 1. Criptografia em trânsito (TLS/HTTPS)

> **O que é TLS?** Transport Layer Security é o protocolo que criptografa a comunicação entre
> o cliente (browser/app) e o servidor. Todas as requests HTTP passam por um "túnel" criptografado,
> impedindo que intermediários leiam ou alterem os dados.
>
> **O que é HSTS?** HTTP Strict Transport Security é um header que instrui o browser a **sempre**
> usar HTTPS, mesmo que o usuário digite `http://`. Previne ataques de downgrade onde o atacante
> força a conexão para HTTP.

**Cenário prevenido:** atacante em rede Wi-Fi pública captura requests HTTP e extrai Bearer tokens.

Configuração de produção:

```yaml
server:
    ssl:
        enabled: true
        key-store: ${SSL_KEYSTORE_PATH}
        key-store-password: ${SSL_KEYSTORE_PASSWORD}
        key-store-type: PKCS12
        enabled-protocols: TLSv1.3,TLSv1.2 # Apenas versões seguras
```

HSTS no código:

```java
.httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)        // Aplica também a subdomínios
        .maxAgeInSeconds(31536000))     // 1 ano — browser lembra por todo esse período
```

---

#### 2. Hashing de senhas (BCrypt)

> **O que é hashing?** É uma função matemática de **mão única**: transforma a senha "Abc@1234"
> em algo como `$2a$12$LJ3m4ys...` que **não pode ser revertido** para a senha original.
> Diferente de criptografia (que é reversível com uma chave), hashing é **irreversível por design**.
>
> **O que é BCrypt?** É um algoritmo de hashing especificamente projetado para senhas. Ele:
>
> 1. **Adiciona um salt aleatório** — duas senhas iguais geram hashes diferentes, impedindo ataques
>    com rainbow tables (tabelas pré-computadas de hashes comuns).
> 2. **É deliberadamente lento** — cada hash leva dezenas de milissegundos. Isso é irrelevante para
>    um login legítimo, mas torna ataques de força bruta (testar milhões de senhas) inviáveis.
>
> **O que é o fator de trabalho (cost factor)?** É um número que controla **quantas vezes** o
> algoritmo executa internamente. A fórmula é $2^{fator}$ iterações:
>
> | Fator | Iterações | Tempo aproximado por hash |
> | ----- | --------- | ------------------------- |
> | 10    | 1.024     | ~100ms                    |
> | 12    | 4.096     | ~300ms                    |
> | 14    | 16.384    | ~1s                       |
>
> **Por que fator 12?** É o equilíbrio recomendado pela OWASP entre segurança e UX:
> lento o suficiente para inviabilizar brute force, rápido o suficiente para não impactar o login.

**Cenário prevenido:** banco de dados é comprometido; atacante obtém a tabela `users` mas encontra
apenas hashes BCrypt — reverter cada hash levaria **séculos** de computação.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // Fator 12 = 2^12 = 4.096 iterações internas
    return new BCryptPasswordEncoder(12);
}
```

---

#### 3. JWT Secret — Validação automática

> **O que é o JWT secret?** É a chave usada para **assinar** o token JWT (HMAC-SHA). Quem possui
> essa chave pode:
>
> 1. **Gerar** tokens válidos (o servidor faz isso no login).
> 2. **Verificar** se um token é legítimo (o servidor faz isso em cada request).
> 3. **Forjar** tokens (isso é o que um atacante faria se descobrisse a chave).
>
> **Por que mínimo 32 caracteres?** HMAC-SHA256 opera com chaves de 256 bits (32 bytes).
> Um secret menor que isso:
>
> - Reduz o espaço de chaves, facilitando brute force.
> - Pode ser encontrado em wordlists comuns de secrets (ex: "secret", "changeme").
>
> **O que é fail-fast?** Significa que a aplicação **recusa iniciar** se detectar uma configuração
> insegura. É melhor a aplicação **não subir** do que subir com um secret fraco e operar em risco.

**Cenário prevenido:** desenvolvedor esquece de configurar `JWT_SECRET` em produção e a aplicação
sobe com valor default fraco. Com fail-fast, isso **nunca acontece** — o boot falha imediatamente.

```java
private static void validateSecret(String secret) {
    if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
        throw new IllegalStateException(
            "JWT secret deve ter no mínimo 32 caracteres. "
            + "Gere um com: openssl rand -base64 32");
    }
}
```

Geração recomendada:

```bash
# Gera 32 bytes aleatórios criptograficamente seguros, codificados em Base64
openssl rand -base64 32
```

---

#### 4. Mascaramento de dados sensíveis em logs

> **O que é?** É a prática de substituir dados sensíveis por placeholders (`***MASKED***`) antes
> de gravá-los em log. Logs são frequentemente armazenados em plain text, replicados para sistemas
> de monitoramento (ELK, Grafana Loki) e acessados por múltiplas equipes.
>
> **Por que é necessário?** Mesmo que o banco esteja seguro, se alguém logar
> `log.info("Login: user={}, password={}", user, password)`, a senha aparece em texto no arquivo
> de log, no Kibana, no Grafana, potencialmente em backups não criptografados.

**Cenários prevenidos:**

- Desenvolvedor loga o payload completo de uma request que contém `password`.
- Log de auditoria registra header `Authorization: Bearer eyJ...` — token completo exposto.
- Dados de PII (email, CPF) aparecem em logs de debug e são rastreáveis.

A classe `SensitiveDataMasker`:

| O que mascara    | Exemplo antes        | Depois                     |
| ---------------- | -------------------- | -------------------------- |
| Campos de senha  | `password=Abc@1234`  | `password=***MASKED***`    |
| Bearer tokens    | `Bearer eyJhbGci...` | `Bearer ***MASKED***`      |
| JSON com secrets | `{"token":"abc123"}` | `{"token":"***MASKED***"}` |
| Emails           | `user@example.com`   | `us***@example.com`        |

```java
@Autowired
private SensitiveDataMasker masker;

// Antes: log.info("Request: {}", payload);            ← PERIGOSO
// Depois: log.info("Request: {}", masker.mask(payload)); ← SEGURO
```

---

#### 5. Proteção contra exposição de informações internas

> **O que é?** Quando uma aplicação retorna stack traces, nomes de classes, queries SQL ou
> mensagens de exceção ao cliente, o atacante ganha **informação gratuita** sobre a arquitetura
> interna. Isso facilita ataques subsequentes (information disclosure → exploit dirigido).
>
> **O que cada propriedade controla?**
>
> | Propriedade              | Valor seguro | O que expõe se habilitado                                                      |
> | ------------------------ | ------------ | ------------------------------------------------------------------------------ |
> | `include-message`        | `never`      | Mensagem da exceção Java (ex: "Column 'x' not found")                          |
> | `include-binding-errors` | `never`      | Detalhes de campos inválidos com nomes internos                                |
> | `include-exception`      | `false`      | Classe completa da exceção (ex: `org.hibernate.exception.SQLGrammarException`) |
> | `include-stacktrace`     | `never`      | Stack trace completo com classes, linhas e queries                             |

**Cenário prevenido:** endpoint retorna `500` com stack trace contendo
`com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure` — atacante
descobre que é MySQL e tenta explorar vulnerabilidades específicas desse DBMS.

Configuração padrão (produção):

```yaml
server:
    error:
        include-message: never
        include-binding-errors: never
        include-exception: false
        include-stacktrace: never
```

Perfil dev sobrescreve para manter usabilidade:

```yaml
# application-dev.yml
server:
    error:
        include-message: always
        include-binding-errors: always
```

No `GlobalExceptionHandler`: detalhes de constraint do banco (nomes de tabela, chaves únicas)
**não aparecem na resposta HTTP** — são logados internamente em nível `DEBUG` apenas.

---

#### 6. Security Headers

> **O que são?** São headers HTTP que o servidor envia nas respostas para instruir o browser
> sobre como se comportar de forma segura. Não dependem do código da aplicação — são diretivas
> para o browser do usuário.

| Header                      | Valor                                 | O que significa                                            | Ataque prevenido                                                                                        |
| --------------------------- | ------------------------------------- | ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `X-Frame-Options`           | `DENY`                                | Proíbe que a página seja carregada dentro de um `<iframe>` | **Clickjacking**: atacante coloca a aplicação em iframe invisível e engana o usuário a clicar em botões |
| `X-Content-Type-Options`    | `nosniff`                             | Impede o browser de "adivinhar" o tipo MIME de um arquivo  | **MIME sniffing**: browser interpreta um arquivo texto como HTML/JS e executa código malicioso          |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Força HTTPS por 1 ano, incluindo subdomínios               | **SSL stripping**: atacante intercepta primeira request HTTP antes do redirect para HTTPS               |

```java
.headers(headers -> headers
        .frameOptions(frame -> frame.deny())
        .contentTypeOptions(cto -> {})
        .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000)))
```

---

### Checklist A04:2025

- [x] HTTPS obrigatório em produção (TLS 1.2+)
- [x] HSTS habilitado (1 ano, incluindo subdomínios)
- [x] BCrypt fator ≥ 12 para senhas
- [x] JWT secret ≥ 32 caracteres (validação fail-fast)
- [x] Mascaramento de dados sensíveis em logs
- [x] Stack traces/mensagens de erro suprimidos em produção
- [x] Detalhes de constraint/schema do banco não expostos ao cliente
- [x] Security headers (X-Frame-Options, X-Content-Type-Options, HSTS)
- [ ] Criptografia em repouso para PII (email) — **pendente**
- [ ] Rotação automática de JWT secret — **pendente**
- [ ] Integração com HSM/KMS para chaves — **pendente**

### Dados sensíveis identificados

| Campo             | Armazenamento   | Proteção atual           | Status                     |
| ----------------- | --------------- | ------------------------ | -------------------------- |
| `User.password`   | DB              | BCrypt hash (fator 12)   | ✅                         |
| `User.email`      | DB              | Texto plano              | ⚠️ Considerar criptografia |
| `JWT_SECRET`      | Env var         | Validação ≥ 32 chars     | ⚠️ Migrar para Vault       |
| Logs da aplicação | Arquivo/console | `SensitiveDataMasker`    | ✅                         |
| Respostas de erro | HTTP response   | `include-message: never` | ✅                         |

---

## A05:2025 (OWASP) — Injection

### O que é?

**Injeção** ocorre quando dados não confiáveis (input do usuário) são enviados a um interpretador como parte de um comando ou consulta. O atacante "injeta" código malicioso que é **interpretado** pelo sistema como se fosse instruções legítimas.

> **Analogia**: Imagine que você pede a um funcionário para buscar o arquivo "João". Se em vez de "João" alguém escrever "João. Depois apague todos os arquivos", o funcionário executaria ambos os comandos sem perceber a maldade.

A injeção é **consistentemente uma das vulnerabilidades mais críticas** porque permite:

- **Leitura** de dados que não deveria acessar (senhas, PII)
- **Modificação** ou **destruição** de dados
- **Execução de comandos** no servidor
- **Bypass** completo de autenticação

### Tipos de injeção e cenários de ataque

#### 1. SQL Injection

**O que é**: O atacante manipula consultas SQL através de inputs da aplicação.

**Cenário de ataque — Tautologia (bypass de login):**

```
POST /api/auth/login
{
  "email": "' OR '1'='1' --",
  "password": "qualquer"
}
```

**O que aconteceria SEM proteção:**

```sql
-- A query montada por concatenação ficaria:
SELECT * FROM users WHERE email = '' OR '1'='1' --' AND password = 'qualquer'

-- Como '1'='1' é SEMPRE verdadeiro, retorna TODOS os usuários
-- O "--" transforma o resto em comentário, ignorando a verificação de senha
```

**Resultado**: Login como o primeiro usuário do banco (geralmente admin).

**Cenário de ataque — UNION-based (extração de dados):**

```
GET /api/movies?title=' UNION SELECT email, password, null, null FROM users --
```

**O que aconteceria SEM proteção:**

```sql
SELECT title, description, year, rating FROM movies WHERE title = ''
UNION
SELECT email, password, null, null FROM users --'

-- Retorna senhas hasheadas junto com resultados de filmes!
```

**Cenário de ataque — Time-based Blind Injection:**

```
GET /api/movies?title=' OR SLEEP(5) --
```

**O que aconteceria**: Se a resposta demora 5 segundos, o atacante confirma que a injeção funciona. Pode então extrair dados um caractere por vez.

#### 2. Log Injection (Log Forging)

**O que é**: O atacante injeta caracteres de controle (`\n`, `\r`) em valores que serão gravados em logs, criando **linhas falsas** no arquivo de log.

**Cenário de ataque:**

```
POST /api/auth/login
{
  "email": "hacker@evil.com\n2024-06-15 12:00:00 INFO  Acesso PERMITIDO para admin@cinelog.com",
  "password": "123"
}
```

**O que aconteceria SEM proteção:**

```log
2024-06-15 12:00:00 WARN  Login falhou para hacker@evil.com
2024-06-15 12:00:00 INFO  Acesso PERMITIDO para admin@cinelog.com
```

A segunda linha é **completamente falsa**, mas parece legítima! Isso pode:

- **Mascarar ataques** em análise forense
- **Criar falsos positivos** confundindo a equipe de segurança
- **Injetar ANSI escape codes** para manipular exibição em terminais

#### 3. Header Injection / CRLF Injection

**O que é**: O atacante injeta `\r\n` em valores que são usados em headers HTTP, podendo criar headers falsos ou até injetar corpo de resposta.

**Cenário de ataque:**

```
GET /api/redirect?url=normal%0d%0aSet-Cookie:%20admin=true
```

**Resultado sem proteção**: O navegador recebe um cookie `admin=true` injetado pelo atacante.

#### 4. Command Injection (OS Injection)

**O que é**: O atacante injeta comandos do sistema operacional quando a aplicação executa processos externos.

**Cenário de ataque:**

```
GET /api/report?filename=report.pdf;rm -rf /
```

**Nota**: O CineLog não executa processos externos, então este tipo não se aplica diretamente — mas documentamos para completude educacional.

### Proteções implementadas no CineLog

Adotamos **defesa em profundidade** — múltiplas camadas independentes:

```
           Requisição HTTP
                 │
                 ▼
    ┌─────────────────────────┐
    │   SqlInjectionFilter    │ ← Camada 1: bloqueia payloads antes
    │   (OncePerRequestFilter)│    de chegar ao controller
    └────────────┬────────────┘
                 │
                 ▼
    ┌─────────────────────────┐
    │   Bean Validation       │ ← Camada 2: @NotBlank, @Size, @Email
    │   (DTOs tipados)        │    rejeita inputs malformados
    └────────────┬────────────┘
                 │
                 ▼
    ┌─────────────────────────┐
    │   JPA + Prepared        │ ← Camada 3: parametrização de queries
    │   Statements            │    IMPOSSÍVEL injetar SQL via bind vars
    └────────────┬────────────┘
                 │
                 ▼
    ┌─────────────────────────┐
    │   InputSanitizer        │ ← Camada 4: sanitiza antes de gravar
    │   (logs e telemetria)   │    em logs (anti Log Injection)
    └─────────────────────────┘
```

#### Camada 1 — `SqlInjectionFilter` (defesa perimetral)

| Aspecto                | Detalhe                                                      |
| ---------------------- | ------------------------------------------------------------ |
| **Classe**             | `com.cine.cinelog.shared.security.SqlInjectionFilter`        |
| **Tipo**               | `OncePerRequestFilter` (executa uma vez por request)         |
| **Posição no chain**   | Antes de `JwtAuthenticationFilter`                           |
| **O que inspeciona**   | Todos os parâmetros da query string                          |
| **O que ignora**       | Paths estáticos (/swagger-ui, /v3/api-docs, /actuator)       |
| **Resposta ao ataque** | HTTP 400 com mensagem genérica (não revela padrão detectado) |

**Padrões detectados:**

| Padrão detectado          | Tipo de ataque               |
| ------------------------- | ---------------------------- |
| `UNION [ALL] SELECT`      | Extração de dados            |
| `DROP TABLE`              | Destruição de dados          |
| `INSERT INTO`             | Criação de registros         |
| `DELETE FROM`             | Remoção de dados             |
| `UPDATE <table> SET`      | Alteração de dados           |
| `xp_cmdshell`             | Execução de comandos (MSSQL) |
| `/* ... */`               | Bypass por comentário inline |
| `' OR '1'='1`             | Tautologia (bypass de login) |
| `'; --`                   | Terminação de query          |
| `EXEC[UTE]`               | Execução de procedures       |
| `information_schema`      | Enumeração de estrutura      |
| `WAITFOR DELAY`           | Blind injection (MSSQL)      |
| `BENCHMARK()` / `SLEEP()` | Blind injection (MySQL)      |

**Exemplo de bloqueio:**

```java
// Requisição maliciosa:
// GET /api/movies?title=' UNION SELECT email,password FROM users --
//
// SqlInjectionFilter detecta "UNION SELECT" no parâmetro "title"
// → Log WARN com IP, URI, parâmetro (sanitizado)
// → Retorna HTTP 400: {"detail": "A requisição contém caracteres inválidos."}
// → Requisição NUNCA chega ao MovieController
```

#### Camada 2 — Bean Validation (tipagem forte)

Os DTOs do Spring usam anotações de validação que rejeitam inputs malformados:

```java
public record CreateUserRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 72) String password
) {}
```

- `@Email` impede que `' OR '1'='1` passe como email
- `@Size(max=72)` limita senhas, impedindo payloads longos
- `@NotBlank` rejeita strings vazias ou só com espaços

#### Camada 3 — JPA + Prepared Statements (proteção primária)

```java
// ✅ SEGURO — parâmetro é bind variable, NUNCA interpolado no SQL
@Query("SELECT m FROM MediaEntity m WHERE m.title = :title")
List<MediaEntity> findByTitle(@Param("title") String title);

// O Hibernate gera:
// PreparedStatement: SELECT * FROM media WHERE title = ?
// Bind: ps.setString(1, "' UNION SELECT...")
// O banco trata o valor como STRING LITERAL, não como SQL
```

**Por que Prepared Statements são eficazes:**

| Sem Prepared Statement        | Com Prepared Statement                  |
| ----------------------------- | --------------------------------------- |
| `WHERE title = '' OR '1'='1'` | `WHERE title = ?` → bind `' OR '1'='1'` |
| Banco **interpreta** como SQL | Banco trata como **texto literal**      |
| Retorna todos os registros    | Retorna 0 registros (título não existe) |

#### Camada 4 — `InputSanitizer` (proteção contra Log Injection)

| Método                 | O que faz                                                   | Ataque prevenido            |
| ---------------------- | ----------------------------------------------------------- | --------------------------- |
| `sanitizeForLog()`     | Remove `\r`, `\n`, `\t` e ANSI escapes; trunca em 200 chars | Log Injection / Log Forging |
| `containsSqlPattern()` | Detecta padrões SQL maliciosos via regex                    | SQL Injection               |
| `sanitize()`           | Remove chars de controle (0x00-0x1F, 0x7F), limita tamanho  | Buffer overflow lógico      |

**Uso no `SecurityBoundaryAspect`:**

```java
// Antes (vulnerável a log injection):
log.warn("Acesso negado para usuário={}", username);

// Depois (A03 — sanitizado):
String safeUser = InputSanitizer.sanitizeForLog(username);
log.warn("Acesso negado para usuário={}", safeUser);
```

### Checklist A05:2025

- [x] JPA com Prepared Statements em todas as queries
- [x] `SqlInjectionFilter` como camada perimetral (query params)
- [x] `InputSanitizer.sanitizeForLog()` em todos os logs com dados do usuário
- [x] `InputSanitizer.containsSqlPattern()` com 14+ padrões de ataque
- [x] Bean Validation com `@Email`, `@Size`, `@NotBlank` nos DTOs
- [x] Resposta genérica no filtro (não revela padrão detectado)
- [x] Paths estáticos excluídos do filtro (sem falsos positivos)
- [x] IP do atacante registrado no log para análise forense
- [x] Filtro posicionado antes da autenticação JWT
- [x] Sem concatenação de strings em queries JPQL/SQL
- [ ] Testar com payloads do OWASP SQLi Cheat Sheet — **pendente**
- [ ] Adicionar rate limiting para IPs com tentativas de injeção — **pendente**

---

## A06:2025 (OWASP) — Insecure Design

### O que é?

> **Design Inseguro** é fundamentalmente diferente das outras categorias do OWASP Top 10.
> Não se trata de um **bug de implementação** (como esquecer de parametrizar uma query), mas
> da **ausência de controles de segurança na arquitetura** do sistema.
>
> Mesmo que o código esteja "correto" — sem bugs, sem vulnerabilidades técnicas — se o
> **design não previu cenários de abuso**, o sistema é vulnerável.

> **Analogia:** imagine uma casa com fechaduras de alta qualidade em todas as portas (boa
> implementação), mas o arquiteto esqueceu de colocar porta nos fundos (design inseguro).
> Nenhuma quantidade de "código bem escrito" resolve uma falha de design — é preciso
> **repensar a arquitetura**.

> **A diferença prática entre implementação insegura e design inseguro:**
>
> | Implementação insegura (A01:2025, A04:2025, A05:2025) | Design inseguro (A06:2025)                                     |
> | ----------------------------------------------------- | -------------------------------------------------------------- |
> | Query SQL concatenada com input                       | Nenhum limite de quantas queries um usuário pode fazer por dia |
> | Senha armazenada em texto plano                       | Nenhum controle de tentativas de login (brute force)           |
> | Stack trace exposto na resposta                       | Mensagens de erro que revelam se um email existe no sistema    |
> | JWT sem validação de assinatura                       | Nenhum controle de fluxo em operações multi-step               |
>
> **Princípio central:** um sistema seguro por design assume que **todo usuário é
> potencialmente malicioso** e modela controles para cada cenário de abuso — **antes** de
> escrever código.

---

### 1. Rate Limiting (anti brute force e anti DoS)

#### O que é Rate Limiting?

> É o mecanismo que controla **quantas requisições** um cliente pode fazer num período
> de tempo. Sem ele, um atacante pode:
>
> | Ataque                  | O que acontece                                        | Impacto                       |
> | ----------------------- | ----------------------------------------------------- | ----------------------------- |
> | **Brute force**         | Testa milhares de senhas/segundo no `/api/auth/login` | Conta comprometida            |
> | **Credential stuffing** | Usa lista de credenciais vazadas de outros sites      | Contas comprometidas em massa |
> | **DoS**                 | Envia milhões de requests legítimas                   | Servidor indisponível         |
> | **Scraping**            | Extrai todos os dados da API programaticamente        | Vazamento massivo de dados    |
> | **Resource exhaustion** | Cria milhares de registros (reviews, mídia)           | Banco de dados lotado         |

#### Algoritmo: Fixed Window

> **Como funciona:**
>
> ```
> Janela 1 (00:00 — 00:59)           Janela 2 (01:00 — 01:59)
> ┌────────────────────────────┐      ┌────────────────────────────┐
> │ Req 1, 2, 3... 100         │      │ Contador reseta → 0        │
> │ Req 101 → HTTP 429         │      │ Req 1, 2, 3...             │
> └────────────────────────────┘      └────────────────────────────┘
> ```
>
> 1. O tempo é dividido em janelas fixas de **60 segundos**.
> 2. Cada IP recebe um contador que incrementa a cada request.
> 3. Quando a janela expira, o contador zera automaticamente.
> 4. Se o contador ultrapassar o limite → HTTP 429 (Too Many Requests).
>
> **Vantagens:** simples, baixo uso de memória, fácil de entender.
>
> **Desvantagem:** na fronteira entre duas janelas, um cliente pode alcançar até 2x o
> limite (100 no final da janela 1 + 100 no início da janela 2). Em produção, considerar
> **Sliding Window** ou **Token Bucket** como alternativas.

#### Limites configurados

> | Tipo de endpoint                 | Limite por minuto | Justificativa                            |
> | -------------------------------- | ----------------- | ---------------------------------------- |
> | `/api/auth/**` (login, registro) | **10**            | Humano real não tenta login 10x/min      |
> | Demais endpoints autenticados    | **100**           | Uso normal de API com margem confortável |
> | Swagger, health check            | **Sem limite**    | Ferramentas de dev e monitoramento       |
>
> **Por que auth tem limite muito menor?** Endpoints de autenticação são os principais alvos
> de brute force. Um humano real digita errado no máximo 3-5 vezes. Se alguém tenta 10 vezes
> em 1 minuto, é quase certamente um ataque automatizado.

#### Headers de resposta (RFC 6585)

> O filtro adiciona headers informativos para que o cliente saiba seu status de rate limit:
>
> ```http
> HTTP/1.1 200 OK
> X-RateLimit-Limit: 100          ← Limite total da janela
> X-RateLimit-Remaining: 87       ← Requisições restantes
> X-RateLimit-Reset: 1740614460   ← Timestamp Unix de quando a janela reseta
>
> HTTP/1.1 429 Too Many Requests
> Retry-After: 34                 ← Segundos até poder tentar novamente
> X-RateLimit-Limit: 10
> X-RateLimit-Remaining: 0
> X-RateLimit-Reset: 1740614460
> ```
>
> Esses headers permitem que clientes bem-comportados (apps mobile, frontends)
> implementem **backoff** automático, mostrando "tente novamente em X segundos".

#### Resolução de IP do cliente (atrás de proxy)

> ```
> Cliente → Nginx/ALB (proxy) → Spring Boot
>            │                      │
>            │ X-Forwarded-For:     │ request.getRemoteAddr()
>            │ 189.1.2.3            │ retorna IP do PROXY
>            │                      │
>            └──── IP real ─────────┘
> ```
>
> **Ordem de prioridade para identificar o IP:**
>
> 1. `X-Forwarded-For` — header padrão de proxies (primeiro IP da cadeia)
> 2. `X-Real-IP` — configuração alternativa do Nginx
> 3. `request.getRemoteAddr()` — fallback (IP direto)
>
> **⚠️ Cuidado:** o header `X-Forwarded-For` pode ser **falsificado** pelo cliente.
> Em produção, o proxy DEVE ser configurado para **sobrescrever** (não concatenar):
>
> ```nginx
> # Nginx: garante que o X-Forwarded-For contém apenas o IP real
> proxy_set_header X-Forwarded-For $remote_addr;
> ```

**Implementação:** `com.cine.cinelog.shared.security.RateLimitFilter`

---

### 2. Anti-Enumeração de Usuários

#### O que é enumeração?

> É quando o atacante descobre **quais usuários existem** no sistema baseando-se em
> **diferenças nas respostas** da API.
>
> **Cenário de ataque SEM proteção:**
>
> ```
> POST /api/auth/login  { "email": "admin@cinelog.com", "password": "errada" }
> → 401: "Senha incorreta"              ← atacante sabe: EMAIL EXISTE
>
> POST /api/auth/login  { "email": "naoexiste@x.com", "password": "errada" }
> → 404: "Usuário não encontrado"       ← atacante sabe: EMAIL NÃO EXISTE
> ```
>
> Com duas respostas diferentes, o atacante mapeia quais emails estão cadastrados.
> Depois usa essas listas para **credential stuffing** (testar senhas vazadas de outros
> serviços — estudos mostram que ~65% dos usuários reutilizam senhas).
>
> **A enumeração não acontece apenas por mensagens.** Pode ser por:
>
> | Vetor de enumeração             | Como funciona                                           |
> | ------------------------------- | ------------------------------------------------------- |
> | **Mensagem de erro diferente**  | "Usuário não encontrado" vs "Senha incorreta"           |
> | **Código HTTP diferente**       | 404 vs 401                                              |
> | **Tempo de resposta diferente** | Email inexistente = 5ms; existente = 300ms (fez BCrypt) |
> | **Endpoint de registro**        | "Email já cadastrado" confirma existência               |
> | **Endpoint de reset de senha**  | "Email não encontrado" confirma inexistência            |

#### 3 técnicas de prevenção implementadas

> **Técnica 1 — Mensagens genéricas:**
>
> ```java
> // ❌ INSEGURO — revela se email existe
> if (user == null) throw new Exception("Usuário não encontrado");
> if (!passwordMatch) throw new Exception("Senha incorreta");
>
> // ✅ SEGURO — mensagem IDÊNTICA em ambos os casos
> throw new AuthenticationException("Credenciais inválidas.");
> ```
>
> **Técnica 2 — Timing noise (anti timing-attack):**
>
> ```java
> // Problema: mesmo com mensagem idêntica, o TEMPO denuncia
> // - Email inexistente: 5ms (não fez BCrypt)
> // - Email existente:   300ms (fez BCrypt, fator 12)
> //
> // Solução: delay aleatório de 100-300ms em TODA resposta de auth
> antiEnumerationService.addTimingNoise();
> ```
>
> **Por que o delay é aleatório com `SecureRandom`?** Se fosse fixo (ex: sempre 200ms), o
> atacante calcularia a média e ainda detectaria a diferença. Com variação aleatória via
> `SecureRandom` (entropia do SO), a distribuição de tempos se torna indistinguível.
>
> **Técnica 3 — Mensagens genéricas em registro e reset de senha:**
>
> ```java
> // ❌ INSEGURO
> "Este email já está cadastrado."
>
> // ✅ SEGURO
> "Se este email estiver disponível, você receberá um link de confirmação."
> ```

**Implementação:** `com.cine.cinelog.shared.security.AntiEnumerationService`

---

### 3. Limites de Negócio (Business Logic Abuse Prevention)

#### O que são limites de negócio?

> São restrições que fazem sentido no **contexto do domínio**, não apenas no nível técnico.
> Rate limiting controla "quantas requests por minuto"; business limits controlam
> "quantos recursos por entidade de domínio".
>
> **Cenário de ataque SEM business limits:**
>
> ```
> Atacante cria conta gratuita
> → Script: 1 request/segundo (abaixo do rate limit de 100/min)
> → Cria 86.400 reviews por dia
> → Banco de dados cresce 100MB/dia com lixo
> → Performance degrada para todos os usuários
> → Storage enche → aplicação CAIA
> ```
>
> Cada request individual é "válida" (autenticada, formato correto, dentro do rate limit).
> Mas o **volume acumulado** é abusivo.

#### Limites definidos para o CineLog

> | Recurso                 | Limite    | Justificativa                        |
> | ----------------------- | --------- | ------------------------------------ |
> | Reviews por dia/usuário | **50**    | Humano real não avalia 50 filmes/dia |
> | Itens na watchlist      | **1.000** | Limite razoável para lista pessoal   |
> | Itens por operação bulk | **100**   | Previne payloads gigantes            |
> | Upload de imagem        | **5 MB**  | Previne resource exhaustion          |

#### Exemplo de uso no service layer

> ```java
> @Service
> public class ReviewService {
>
>     private final BusinessLimitValidator limitValidator;
>     private final ReviewRepository reviewRepository;
>
>     public Review createReview(CreateReviewRequest request, Long userId) {
>         // Conta reviews do usuário criadas hoje
>         long todayCount = reviewRepository.countByUserIdAndCreatedAtToday(userId);
>
>         // Valida contra o limite de negócio
>         limitValidator.validateLimit(
>             todayCount,
>             BusinessLimitValidator.MAX_REVIEWS_PER_DAY,
>             "reviews diárias"
>         );
>         // Se passou, cria a review normalmente
>         // Se excedeu → BusinessLimitExceededException → HTTP 429
>     }
> }
> ```

**Implementação:** `com.cine.cinelog.shared.security.BusinessLimitValidator`

---

### 4. Cadeia de filtros completa (ordem de execução)

> A **ordem** dos filtros importa para segurança e performance. A cadeia do CineLog:
>
> ```
> Request HTTP
>     │
>     ▼
> ┌──────────────────────────┐
> │ 1. RateLimitFilter       │ ← Bloqueia antes de qualquer processamento
> │    (por IP, Fixed Window) │    Previne DoS e brute force
> └──────────┬───────────────┘
>            │
>            ▼
> ┌──────────────────────────┐
> │ 2. SqlInjectionFilter    │ ← Detecta payloads maliciosos (A05:2025)
> │    (query params)         │    Antes de autenticação (economiza CPU)
> └──────────┬───────────────┘
>            │
>            ▼
> ┌──────────────────────────┐
> │ 3. JwtAuthFilter         │ ← Valida token e define SecurityContext
> │    (Bearer token)         │    Após filtros de segurança perimetral
> └──────────┬───────────────┘
>            │
>            ▼
> ┌──────────────────────────┐
> │ 4. URL Authorization     │ ← hasRole, authenticated, permitAll (A01:2025)
> │    (HttpSecurity)         │    Baseado no SecurityContext do passo 3
> └──────────┬───────────────┘
>            │
>            ▼
> ┌──────────────────────────┐
> │ 5. Controller +          │ ← @SecureOperation, @PreAuthorize (A01:2025)
> │    Method Security        │    Validação granular no método
> └──────────┬───────────────┘
>            │
>            ▼
> ┌──────────────────────────┐
> │ 6. BusinessLimit         │ ← Limites de domínio por recurso (A06:2025)
> │    Validator              │    Validação no service layer
> └──────────────────────────┘
> ```
>
> **Por que essa ordem?**
>
> - **Rate limit primeiro:** se o cliente está fazendo DoS, não faz sentido gastar CPU
>   verificando SQL injection ou validando JWT.
> - **SQL injection antes do JWT:** se o payload é malicioso, rejeitar antes de decodificar
>   o token (que envolve operações criptográficas).
> - **Business limits por último:** só faz sentido verificar cotas de domínio para
>   usuários autenticados e autorizados.

---

### Checklist A06:2025

- [x] Rate limiting por IP (Fixed Window, 100/min geral, 10/min auth)
- [x] Headers de rate limit (X-RateLimit-Limit, Remaining, Reset, Retry-After)
- [x] Resolução de IP real via X-Forwarded-For / X-Real-IP
- [x] Anti-enumeração: mensagens genéricas de erro em auth
- [x] Anti-enumeração: timing noise com SecureRandom (100-300ms)
- [x] Business limits: cotas por recurso (reviews/dia, watchlist, bulk, upload)
- [x] `BusinessLimitExceededException` → HTTP 429 via GlobalExceptionHandler
- [x] Cadeia de filtros ordenada (rate limit → injection → auth → authorization)
- [x] Endpoints Swagger/health excluídos do rate limit
- [x] Log de rate limit excedido com IP sanitizado
- [ ] Rate limiting distribuído com Redis (múltiplas instâncias) — **pendente**
- [ ] Rate limiting por usuário autenticado (além de IP) — **pendente**
- [ ] CAPTCHA após N tentativas falhas de login — **pendente**
- [ ] Threat modeling formal com abuse cases documentados — **pendente**

---

## A07:2025 (OWASP) — Authentication Failures

> **Status:** ✅ implementado.
>
> Esta categoria foca em **falhas de autenticação**: credenciais fracas, ausência de proteção
> contra brute-force, sessões mal gerenciadas, ausência de refresh token rotation, e
> mecanismos de recuperação de senha inseguros.

### O que são Authentication Failures?

Authentication Failures referem-se a qualquer fraqueza no processo de autenticação que
permita a um atacante:

1. **Adivinhar credenciais** por brute-force ou credential stuffing
2. **Reutilizar tokens roubados** por ausência de rotation
3. **Explorar senhas fracas** por falta de política de complexidade
4. **Enumerar usuários** por diferenças em respostas de erro
5. **Manter sessões ativas indefinidamente** sem mecanismo de revogação

### Cenários de ataque reais

| Cenário                 | Ataque                                   | Sem proteção                     | Com proteção (CineLog)                     |
| ----------------------- | ---------------------------------------- | -------------------------------- | ------------------------------------------ |
| **Brute-force**         | Bot tenta 10.000 senhas/minuto           | Conta comprometida               | Bloqueio após 5 tentativas + rate limiting |
| **Credential stuffing** | Credenciais vazadas de outro site        | Login com senha reutilizada      | Política de senha + lista de comprometidas |
| **Token theft**         | Refresh token interceptado               | Acesso permanente                | Rotation detecta reuso e revoga família    |
| **Enumeração**          | "Email não encontrado" vs "Senha errada" | Atacante descobre emails válidos | Mensagem genérica: "Credenciais inválidas" |
| **Senha fraca**         | Senha "123456"                           | Aceita pelo sistema              | Rejeitada pela política de complexidade    |

### Proteções implementadas no CineLog

#### 1. Política de complexidade de senha (`PasswordPolicyValidator`)

Validamos senhas contra múltiplas regras ANTES de aceitar o registro:

```
📁 shared/security/PasswordPolicyValidator.java
```

**Regras aplicadas:**

| Regra              | Requisito                                         | Motivação                     |
| ------------------ | ------------------------------------------------- | ----------------------------- |
| Comprimento mínimo | ≥ 8 caracteres                                    | NIST SP 800-63B recomenda ≥ 8 |
| Letra maiúscula    | ≥ 1                                               | Aumenta espaço de busca       |
| Letra minúscula    | ≥ 1                                               | Aumenta espaço de busca       |
| Dígito             | ≥ 1                                               | Aumenta espaço de busca       |
| Caractere especial | ≥ 1 (de `@#$%^&+=!?*()-_`)                        | Aumenta espaço de busca       |
| Senha comprometida | Não pode estar na lista de 70+ senhas mais comuns | Previne credential stuffing   |
| Partes do email    | Não pode conter o local-part do email             | Previne senhas previsíveis    |

**Por que isso protege?**

Um atacante com brute-force offline contra BCrypt(12) precisa de:

- Senha "123456" (6 chars, só dígitos): ~1 segundo
- Senha "P@ssw0rd!" (9 chars, todas as classes): ~centenas de anos

**Como a validação funciona:**

```java
@Component
public class PasswordPolicyValidator {

    public List<String> validate(String password, String email) {
        List<String> violations = new ArrayList<>();

        if (password.length() < MIN_LENGTH)
            violations.add("Senha deve ter no mínimo " + MIN_LENGTH + " caracteres");

        // ... verifica maiúsculas, minúsculas, dígitos, especiais

        if (COMPROMISED_PASSWORDS.contains(password.toLowerCase()))
            violations.add("Esta senha é muito comum e foi comprometida");

        // Verifica se contém partes do email
        if (email != null) {
            String localPart = email.split("@")[0].toLowerCase();
            if (localPart.length() >= 3 && password.toLowerCase().contains(localPart))
                violations.add("Senha não pode conter partes do seu email");
        }

        return violations; // vazio = senha OK
    }
}
```

**Resposta ao usuário quando senha falha (RFC 7807):**

```json
{
    "type": "https://api.cinelog.com/errors/validation",
    "title": "Password Policy Violation",
    "status": 400,
    "detail": "Senha não atende à política de segurança.",
    "violations": [
        "Senha deve conter ao menos uma letra maiúscula",
        "Senha deve conter ao menos um caractere especial (@#$%^&+=!?*()-_)"
    ]
}
```

#### 2. Account lockout por brute-force (`LoginAttemptService`)

Após **5 tentativas falhas** consecutivas, a conta é temporariamente bloqueada por **15 minutos**.

```
📁 shared/security/LoginAttemptService.java
```

**Como funciona o bloqueio:**

```
Tentativa 1: ❌ falha → contador = 1 (4 restantes)
Tentativa 2: ❌ falha → contador = 2 (3 restantes)
Tentativa 3: ❌ falha → contador = 3 (2 restantes)
Tentativa 4: ❌ falha → contador = 4 (1 restante)
Tentativa 5: ❌ falha → contador = 5 → 🔒 BLOQUEIO 15min
Tentativa 6: ⛔ rejeitada imediatamente (sem verificar credenciais)
...15 minutos depois...
Tentativa 7: ✅ sucesso → 🔓 contador resetado
```

**Detalhes de implementação:**

| Aspecto             | Decisão                          | Motivação                                        |
| ------------------- | -------------------------------- | ------------------------------------------------ |
| Storage             | `ConcurrentHashMap` in-memory    | Sem dependência externa, thread-safe             |
| Chave de bloqueio   | Email + IP (dual-key)            | Bloqueia tanto a conta quanto o IP atacante      |
| Max tentativas      | 5 (configurável)                 | Equilíbrio entre segurança e usabilidade         |
| Duração do bloqueio | 900s = 15min (configurável)      | Previne brute-force sem bloquear permanentemente |
| Reset automático    | Após período de bloqueio expirar | Sem intervenção manual necessária                |
| Logs                | Hash do email (nunca plaintext)  | Protege PII em logs                              |

**Configuração em `application.yml`:**

```yaml
cinelog:
    security:
        auth:
            max-login-attempts: 5 # Máximo de tentativas antes do bloqueio
            lock-duration-seconds: 900 # 15 minutos de bloqueio
```

**Resposta HTTP quando conta está bloqueada (423 Locked):**

```json
{
    "type": "https://api.cinelog.com/errors/account-locked",
    "title": "Account Locked",
    "status": 423,
    "detail": "Conta temporariamente bloqueada por excesso de tentativas.",
    "retryAfterSeconds": 847
}
```

**Dual-key blocking — por que bloqueamos por email E por IP?**

- **Só por email:** atacante muda de IP e continua o brute-force
- **Só por IP:** atacante usa botnet com milhares de IPs
- **Por email + IP:** atacante precisa de milhares de IPs E conhecer emails válidos

```java
// No AuthService.login():
if (loginAttemptService.isBlocked(email)) {
    throw new AccountLockedException(retryAfter);
}
if (loginAttemptService.isBlocked(clientIp)) {
    throw new AccountLockedException(retryAfter);
}

// Em caso de falha, registra AMBOS:
loginAttemptService.recordFailedAttempt(email);
loginAttemptService.recordFailedAttempt(clientIp);
```

#### 3. Refresh token rotation com reuse detection (`RefreshTokenService`)

Implementamos o padrão **refresh token rotation** com **family-based reuse detection**,
que é recomendado pelo [OAuth 2.0 Security Best Current Practice (RFC 9700)](https://www.rfc-editor.org/rfc/rfc9700).

```
📁 features/auth/service/RefreshTokenService.java
📁 features/auth/persistence/entity/RefreshTokenEntity.java
📁 features/auth/persistence/repository/RefreshTokenRepository.java
```

**O que é refresh token rotation?**

Em vez de um refresh token valer para sempre, cada uso gera um **novo** refresh token
e invalida o anterior. Se alguém tentar usar um token já usado, toda a "família" de
tokens é revogada.

**Fluxo normal (sem ataque):**

```
Login → AccessToken(1h) + RefreshToken_A
        │
        ▼ (após 1h)
/refresh com RefreshToken_A
        │
        ▼
RefreshToken_A → REVOGADO
AccessToken(1h) + RefreshToken_B ← NOVO
        │
        ▼ (após 1h)
/refresh com RefreshToken_B
        │
        ▼
RefreshToken_B → REVOGADO
AccessToken(1h) + RefreshToken_C ← NOVO
```

**Fluxo com ataque (reuse detection):**

```
Login → AccessToken(1h) + RefreshToken_A
        │
        ├── Usuário legítimo usa RefreshToken_A → OK → AccessToken + RefreshToken_B
        │
        └── 🔴 Atacante usa RefreshToken_A (roubado) → REUSE DETECTED!
            │
            ▼
            TODA A FAMÍLIA REVOGADA (RefreshToken_A, RefreshToken_B, ...)
            Usuário e atacante precisam fazer login novamente
```

**Por que revogar toda a família?**

Se um token revogado é apresentado, sabemos que:

1. O atacante tem uma cópia do token
2. Não sabemos se o último token emitido está com o usuário ou com o atacante
3. A opção mais segura é revogar **tudo** e forçar re-autenticação

**Entidade `RefreshTokenEntity` — campos importantes:**

| Campo         | Tipo         | Propósito                                            |
| ------------- | ------------ | ---------------------------------------------------- |
| `token`       | UUID, unique | Identificador do token (opaco, não-JWT)              |
| `tokenFamily` | UUID         | Agrupa tokens de uma mesma "cadeia" de rotation      |
| `userId`      | BIGINT, FK   | Dono do token                                        |
| `revoked`     | boolean      | Se foi revogado (rotation normal ou reuse detection) |
| `expiresAt`   | TIMESTAMP    | Expiração absoluta (padrão 7 dias)                   |
| `replacedBy`  | VARCHAR      | UUID do token que o substituiu                       |
| `clientIp`    | VARCHAR(45)  | IP do cliente (auditoria)                            |
| `userAgent`   | VARCHAR(500) | User-Agent do cliente (auditoria)                    |

**Limpeza automática:**

Tokens expirados são removidos automaticamente por um `@Scheduled` job:

```java
@Scheduled(cron = "0 0 2 * * ?")  // Todo dia às 02:00
@Transactional
public void cleanupExpiredTokens() {
    int deleted = repository.deleteExpiredBefore(Instant.now());
    log.info("Cleanup: {} refresh tokens expirados removidos", deleted);
}
```

**Configuração:**

```yaml
cinelog:
    security:
        auth:
            refresh-token-expiration-seconds: 604800 # 7 dias
```

#### 4. Serviço de autenticação centralizado (`AuthService`)

Todo o fluxo de autenticação passa por um serviço centralizado que integra TODAS as
proteções de A07:2025:

```
📁 features/auth/service/AuthService.java
```

**Endpoints disponíveis:**

| Método | Endpoint             | Descrição                    | Auth?     |
| ------ | -------------------- | ---------------------------- | --------- |
| POST   | `/api/auth/login`    | Login com email/senha        | Não       |
| POST   | `/api/auth/register` | Registro de novo usuário     | Não       |
| POST   | `/api/auth/refresh`  | Renovação de tokens          | Não       |
| POST   | `/api/auth/logout`   | Revogação de todos os tokens | Sim (JWT) |

**Fluxo de login — integração de proteções:**

```
POST /api/auth/login
    │
    ├── 1. LoginAttemptService.isBlocked(email)? → 423 Locked
    ├── 2. LoginAttemptService.isBlocked(clientIp)? → 423 Locked
    ├── 3. AuthenticationManager.authenticate()
    │       └── BCrypt(12) verifica senha
    │
    ├── ✅ Sucesso:
    │       ├── loginAttemptService.recordSuccessfulLogin()
    │       ├── Gerar AccessToken JWT (1h)
    │       ├── Gerar RefreshToken (7d, rotation)
    │       └── return AuthResponse { accessToken, refreshToken, expiresIn }
    │
    └── ❌ Falha:
            ├── loginAttemptService.recordFailedAttempt(email)
            ├── loginAttemptService.recordFailedAttempt(clientIp)
            ├── antiEnumerationService.addTimingNoise() ← timing constante
            └── throw BadCredentials("Credenciais inválidas.") ← msg genérica
```

**Fluxo de registro — validações:**

```
POST /api/auth/register
    │
    ├── 1. PasswordPolicyValidator.validate() → 400 se falhar
    ├── 2. userRepository.existsByEmail()? → 409 (sem revelar email)
    ├── 3. Criar UserEntity + BCrypt(12)
    ├── 4. Gerar AccessToken + RefreshToken
    └── return AuthResponse
```

**Resposta de sucesso (`AuthResponse`):**

```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tokenType": "Bearer",
    "expiresIn": 3600
}
```

#### 5. Password hashing — BCrypt fator 12

Já implementado desde a configuração inicial de segurança:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

| Fator            | Tempo p/ hash | Tempo p/ brute-force (10^8 hashes/s) |
| ---------------- | ------------- | ------------------------------------ |
| 10 (padrão)      | ~100ms        | ~3 anos para 8 chars                 |
| **12 (CineLog)** | ~400ms        | ~12 anos para 8 chars                |
| 14               | ~1.6s         | ~48 anos para 8 chars                |

#### 6. Anti-enumeração de usuários

Integrado desde A06:2025, agora **wired** no `AuthService`:

```java
// Em caso de falha, SEMPRE:
antiEnumerationService.addTimingNoise(); // Timing constante
throw new BadCredentialsException("Credenciais inválidas."); // Msg genérica
```

- **Timing constante:** `addTimingNoise()` adiciona delay aleatório para que
  respostas de "email não existe" e "senha errada" levem o mesmo tempo
- **Mensagem genérica:** nunca diferencia entre "email não encontrado" e "senha incorreta"

#### 7. Migração de banco de dados (Liquibase)

A tabela `refresh_tokens` é criada via Liquibase:

```
📁 liquibase/changes/20260227000000_create_refresh_tokens_table.xml
```

**Estrutura da tabela:**

```sql
CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    token_family VARCHAR(255) NOT NULL,
    user_id     BIGINT NOT NULL,
    user_email  VARCHAR(255) NOT NULL,
    revoked     BOOLEAN DEFAULT FALSE NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    revoked_at  TIMESTAMP NULL,
    replaced_by VARCHAR(255) NULL,
    client_ip   VARCHAR(45) NULL,
    user_agent  VARCHAR(500) NULL,

    INDEX idx_refresh_tokens_token_family (token_family),
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expires_at (expires_at),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Configuração completa em `application.yml`

```yaml
cinelog:
    security:
        jwt:
            secret: ${CINELOG_SECURITY_JWT_SECRET:...}
            expiration-seconds: 3600 # Access token: 1 hora
        auth:
            max-login-attempts: 5 # Bloqueio após 5 falhas
            lock-duration-seconds: 900 # 15 minutos de bloqueio
            refresh-token-expiration-seconds: 604800 # Refresh token: 7 dias
```

### Diagrama: fluxo de autenticação completo

```
┌──────────┐       ┌──────────────┐       ┌──────────────────┐
│  Cliente  │──────▷│ AuthController│──────▷│   AuthService    │
└──────────┘       └──────────────┘       │                  │
     │                                     │  ┌─────────────┐│
     │  POST /api/auth/login               │  │LoginAttempt  ││
     │  {email, password}                  │  │  Service     ││
     │                                     │  └──────┬──────┘│
     │                                     │         │       │
     │                                     │  ┌──────▽──────┐│
     │                                     │  │   AuthMgr   ││
     │                                     │  │ (BCrypt 12) ││
     │                                     │  └──────┬──────┘│
     │                                     │         │       │
     │                                     │  ┌──────▽──────┐│
     │                                     │  │ JwtToken    ││
     │  ◁──── AuthResponse ───────────────│  │  Service    ││
     │  {accessToken, refreshToken,        │  └─────────────┘│
     │   tokenType, expiresIn}             │  ┌─────────────┐│
     │                                     │  │RefreshToken ││
     │                                     │  │  Service    ││
     │                                     │  └─────────────┘│
     │                                     │  ┌─────────────┐│
     │                                     │  │AntiEnum     ││
     │                                     │  │  Service    ││
     │                                     │  └─────────────┘│
     │                                     └──────────────────┘
     │
     │  POST /api/auth/refresh
     │  {refreshToken: "a1b2c3d4..."}
     │         │
     │         ▼
     │  RefreshTokenService.rotateRefreshToken()
     │    ├── Token válido? → novo AccessToken + novo RefreshToken
     │    └── Token revogado? → 🔴 REUSE DETECTED → revoga toda família
     │
     │  POST /api/auth/logout
     │  Authorization: Bearer {accessToken}
     │         │
     │         ▼
     │  RefreshTokenService.revokeAllUserTokens(userId)
```

### Checklist de proteções A07:2025

- [x] JWT com validação de assinatura HMAC-SHA e expiração (1h)
- [x] BCrypt fator 12 para hashing de senhas
- [x] Política de complexidade de senha (≥8 chars, maiúscula, minúscula, dígito, especial)
- [x] Verificação contra lista de senhas comprometidas (70+ entradas)
- [x] Rejeição de senhas que contenham partes do email
- [x] Account lockout após 5 tentativas falhas (15min de bloqueio)
- [x] Bloqueio dual-key (por email E por IP)
- [x] Refresh token rotation com family-based reuse detection
- [x] Revogação automática de toda família ao detectar reuso
- [x] Limpeza automática de tokens expirados (@Scheduled diário)
- [x] Anti-enumeração de usuários (timing constante + mensagem genérica)
- [x] Endpoint de logout com revogação de todos os tokens
- [x] Tabela `refresh_tokens` com índices e FK (Liquibase)
- [x] Configuração externalizável via environment variables
- [x] Rate limiting em endpoints de auth (via RateLimitFilter — A06:2025)
- [ ] Multi-Factor Authentication (MFA/2FA) — **roadmap futuro**
- [ ] Integração com HaveIBeenPwned API para verificação online — **roadmap futuro**

---

## A08:2025 (OWASP) — Software or Data Integrity Failures

> **Status:** ✅ implementado.
>
> Esta categoria cobre falhas na verificação de integridade de software e dados:
> dados adulterados no banco, payloads Kafka modificados em trânsito, race conditions
> por falta de optimistic locking, deserialização insegura e manipulação de requests.

### O que são Software or Data Integrity Failures?

Ocorrem quando o sistema **confia cegamente** em dados, código ou atualizações sem
verificar se foram **adulterados**. A OWASP define como cenários onde:

1. **Dados no banco** são alterados diretamente (bypass da API) sem detecção
2. **Payloads de mensagens** (Kafka, filas) são modificados em trânsito
3. **Requests HTTP** são manipulados (campos imutáveis como userId alterados)
4. **Updates concorrentes** causam perda de dados (lost update problem)
5. **Deserialização** aceita classes arbitrárias (gadget chain → RCE)
6. **Registros de auditoria** são adulterados sem rastro

### Cenários de ataque reais

| Cenário                 | Ataque                                               | Sem proteção                    | Com proteção (CineLog)                           |
| ----------------------- | ---------------------------------------------------- | ------------------------------- | ------------------------------------------------ |
| **DB tampering**        | DBA malicioso altera role USER→ADMIN                 | Mudança invisível               | HMAC no campo detecta adulteração                |
| **Kafka tampering**     | Atacante altera payload no broker                    | Processamento de dados falsos   | HMAC header rejeita payload adulterado           |
| **Request tampering**   | Cliente troca userId no JSON body                    | Opera em nome de outro usuário  | `TamperProofRequestValidator` rejeita            |
| **Lost update**         | Dois requests simultâneos atualizam o mesmo registro | Último sobrescreve o primeiro   | `@Version` (optimistic locking) detecta conflito |
| **Deserialization RCE** | Classe maliciosa na mensagem Kafka                   | Remote Code Execution           | `trusted.packages` restrito                      |
| **Audit tampering**     | Atacante apaga/altera log de auditoria               | Evidência destruída             | Hash chain detecta adulteração                   |
| **Link forgery**        | Atacante modifica userId no link de reset            | Reset da senha de outro usuário | HMAC no token de ação                            |

### Proteções implementadas no CineLog

#### 1. `IntegrityService` — HMAC-SHA256 para dados críticos

Serviço central de integridade que gera e verifica assinaturas HMAC-SHA256.
Usado para proteger registros no banco e payloads de eventos.

```
📁 shared/security/IntegrityService.java
```

**Como funciona:**

```java
@Component
public class IntegrityService {

    // Gera HMAC para campos críticos de uma entidade
    public String signEntity(Object... criticalFields) {
        String content = buildSignableContent(criticalFields);
        return sign(content);  // HMAC-SHA256 → Base64 URL-safe
    }

    // Verifica integridade (comparação em tempo constante)
    public boolean verifyEntity(String storedHmac, Object... criticalFields) {
        String content = buildSignableContent(criticalFields);
        return verify(content, storedHmac);
    }
}
```

**Exemplo: protegendo campo role do usuário:**

```java
// Ao salvar/atualizar:
String hmac = integrityService.signEntity(user.getId(), user.getEmail(), user.getRole());
user.setIntegrityHmac(hmac);

// Ao carregar — verificar integridade:
boolean ok = integrityService.verifyEntity(
    user.getIntegrityHmac(), user.getId(), user.getEmail(), user.getRole());
if (!ok) {
    throw new TamperDetectedException("UserEntity", user.getId().toString());
}
```

**Detalhes técnicos:**

| Aspecto          | Decisão                                                                      |
| ---------------- | ---------------------------------------------------------------------------- |
| Algoritmo        | HMAC-SHA256                                                                  |
| Chave            | `cinelog.security.integrity.secret` (min 32 chars, fallback para JWT secret) |
| Output           | Base64 URL-safe sem padding                                                  |
| Comparação       | Tempo constante (`constantTimeEquals`) — previne timing attack               |
| Campos assinados | Concatenados com pipe (`\|`) como separador                                  |

#### 2. `@Version` — Optimistic locking em todas as entidades

**Antes:** o campo `version` existia em `AuditableEntity` mas **NÃO** tinha a anotação
`@jakarta.persistence.Version`. Resultado: nenhuma proteção contra updates concorrentes.

**Depois:** adicionamos `@Version`, ativando optimistic locking para **9 entidades**
que estendem `AuditableEntity`:

```
📁 shared/persistence/AuditableEntity.java
```

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @Version  // ← A08:2025 — Optimistic locking ativado
    @Column(name = "version", nullable = false)
    protected Long version;

    // ... createdAt, updatedAt, createdBy, updatedBy
}
```

**Entidades protegidas automaticamente:**

| Entidade            | Tabela        | Dados críticos            |
| ------------------- | ------------- | ------------------------- |
| UserEntity          | `users`       | Credenciais, role, email  |
| WatchEntryEntity    | `watch_entry` | Avaliações do usuário     |
| MediaEntity         | `media`       | Catálogo de filmes/séries |
| PersonEntity        | `people`      | Elenco e equipe           |
| GenreEntity         | `genres`      | Categorias                |
| CreditEntity        | `credits`     | Créditos                  |
| EpisodeEntity       | `episodes`    | Episódios de séries       |
| SeasonEntity        | `seasons`     | Temporadas                |
| WatchlistItemEntity | `watchlist`   | Lista de desejos          |

**Como funciona o optimistic locking:**

```
Transação A: SELECT FROM users WHERE id=1 → version=5
Transação B: SELECT FROM users WHERE id=1 → version=5

Transação A: UPDATE users SET role='ADMIN', version=6 WHERE id=1 AND version=5 → ✅ OK
Transação B: UPDATE users SET name='Hack',  version=6 WHERE id=1 AND version=5 → ❌ FALHA!
              ↳ version já é 6 (Transação A mudou) → OptimisticLockException → 409 Conflict
```

**Resposta ao cliente (RFC 7807):**

```json
{
    "type": "https://api.cinelog.com/errors/conflict",
    "title": "Concurrent Modification",
    "status": 409,
    "detail": "O registro foi modificado por outra operação. Tente novamente.",
    "errorCode": "OPTIMISTIC_LOCK_CONFLICT"
}
```

#### 3. Audit trail com hash chain — blockchain-lite (`AuditIntegrityService`)

Cada registro de auditoria contém o **hash do registro anterior**, formando uma cadeia
imutável. Se qualquer registro for deletado ou modificado no banco, a cadeia se quebra.

```
📁 shared/observability/audit/AuditIntegrityLogEntity.java
📁 shared/observability/audit/AuditIntegrityLogRepository.java
📁 shared/observability/audit/AuditIntegrityService.java
```

**Estrutura da cadeia:**

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Bloco #1   │    │  Bloco #2   │    │  Bloco #3   │    │  Bloco #N   │
│             │    │             │    │             │    │             │
│ prevHash:   │    │ prevHash:   │    │ prevHash:   │    │ prevHash:   │
│  "GENESIS"  │←───│  hash(#1)   │←───│  hash(#2)   │←───│  hash(N-1)  │
│             │    │             │    │             │    │             │
│ recordHash: │    │ recordHash: │    │ recordHash: │    │ recordHash: │
│  HMAC(...)  │    │  HMAC(...)  │    │  HMAC(...)  │    │  HMAC(...)  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

**Se alguém alterar o Bloco #2:**

```
Verificação: hash(Bloco #2_adulterado) ≠ prevHash do Bloco #3
             → CADEIA QUEBRADA → ADULTERAÇÃO DETECTADA
```

**Como registrar uma operação auditada:**

```java
// Gerar HMAC da entidade
String entityHmac = integrityService.signEntity(user.getId(), user.getEmail(), user.getRole());

// Registrar na cadeia (transação separada para não bloquear a principal)
auditIntegrityService.record(
    "UserEntity",
    user.getId().toString(),
    "UPDATE",
    entityHmac,
    currentUserId,
    "Role alterada para ADMIN"
);
```

**Verificação periódica da cadeia:**

```java
ChainVerificationResult result = auditIntegrityService.verifyChain();

if (!result.valid()) {
    // ALERTA: registros de auditoria foram adulterados!
    log.error("A08 — Cadeia de auditoria comprometida: {}", result.message());
    // Notificar equipe de segurança
}
```

**Tabela `audit_integrity_log`:**

| Coluna            | Tipo           | Propósito                                         |
| ----------------- | -------------- | ------------------------------------------------- |
| `sequence_number` | BIGINT, UNIQUE | Ordenação global da cadeia                        |
| `entity_type`     | VARCHAR(100)   | Tipo da entidade auditada                         |
| `entity_id`       | VARCHAR(100)   | ID da entidade                                    |
| `action`          | VARCHAR(20)    | CREATE, UPDATE, DELETE                            |
| `entity_hmac`     | VARCHAR(64)    | HMAC dos campos críticos no momento da operação   |
| `previous_hash`   | VARCHAR(64)    | Hash do registro anterior (ou "GENESIS")          |
| `record_hash`     | VARCHAR(64)    | HMAC(seq\|type\|id\|action\|entityHmac\|prevHash) |
| `user_id`         | BIGINT         | Quem executou a ação                              |
| `created_at`      | TIMESTAMP      | Quando                                            |

#### 4. `TamperProofRequestValidator` — Anti-tampering em requests

Valida que campos imutáveis nos requests (userId, email, role) não foram manipulados
pelo cliente.

```
📁 shared/security/TamperProofRequestValidator.java
```

**Cenário de ataque: userId tampering**

```http
POST /api/v1/reviews
Authorization: Bearer <token do user 42>
{
    "userId": 999,     ← atacante trocou de 42 para 999
    "mediaId": 1,
    "rating": 10
}
```

**Proteção:**

```java
@PostMapping
public ResponseEntity<?> createReview(@RequestBody ReviewRequest request) {
    // Valida que userId do body == userId do JWT
    tamperProofValidator.validateUserOwnership(request.getUserId());
    // Se não bater → 403 Forbidden + log de segurança
}
```

**Validações disponíveis:**

| Método                                  | Proteção                                  |
| --------------------------------------- | ----------------------------------------- |
| `validateUserOwnership(requestUserId)`  | userId no body == userId do JWT           |
| `validateEmailOwnership(requestEmail)`  | email no body == email do JWT             |
| `validateRoleNotEscalated(requestRole)` | Não-admin não pode definir role != "USER" |

**Resposta ao cliente (RFC 7807):**

```json
{
    "type": "https://api.cinelog.com/errors/integrity",
    "title": "Integrity Violation",
    "status": 403,
    "detail": "Operação rejeitada por violação de integridade.",
    "errorCode": "TAMPER_DETECTED"
}
```

#### 5. `SecureActionTokenService` — Tokens HMAC para ações

Gera tokens autocontidos com assinatura HMAC para ações como reset de senha e
confirmação de email. Impede que atacantes forjem ou manipulem links de ação.

```
📁 shared/security/SecureActionTokenService.java
```

**Formato do token:**

```
Base64URL( purpose : userId : expiresEpochSeconds : HMAC-SHA256 )
```

**Cenário de ataque sem proteção:**

```
Link de reset: /reset-password?userId=42&token=abc123
Atacante altera: /reset-password?userId=1&token=abc123    ← admin!
→ Sem HMAC: reset funciona para admin!
```

**Com proteção:**

```
Link de reset: /reset-password?token=UEFTU1dPUkRfUkVTRVQ6NDI6MTcwOTIyMDAwMDpITUFD...
Atacante altera o token → HMAC não bate → REJEITADO
```

**Como usar:**

```java
// Gerar token para reset de senha (expira em 1h)
String token = secureActionTokenService.generateToken("PASSWORD_RESET", userId);

// Verificar token quando usuário clica no link
ActionTokenPayload payload = secureActionTokenService.verifyToken(token, "PASSWORD_RESET");
Long userId = payload.userId();  // Seguro — veio do HMAC
```

**Verificações feitas:**

| Passo | Verificação                                   |
| ----- | --------------------------------------------- |
| 1     | Token decodifica corretamente (Base64)        |
| 2     | Propósito (`purpose`) bate com o esperado     |
| 3     | Não expirou (`expiresAt > now`)               |
| 4     | HMAC-SHA256 é válido (integridade do payload) |

#### 6. Proteção contra deserialização insegura (Kafka)

**Antes:** o Kafka consumer aceitava **qualquer classe Java** para deserialização:

```yaml
# ⚠️ PERIGOSO — permite Remote Code Execution
"[spring.json.trusted.packages]": "*"
```

**Depois:** restringimos para apenas os pacotes do projeto:

```yaml
# ✅ A08:2025 — Apenas pacotes confiáveis
"[spring.json.trusted.packages]": "com.cine.cinelog.core.domain.events,com.cine.cinelog.core.domain.events.watchentry,com.cine.cinelog.infrastructure.persistence.outbox,java.util,java.lang"
```

**Por que isso é crítico?**

Com `*` (wildcard), um atacante que consiga publicar uma mensagem no Kafka com uma
classe maliciosa (gadget chain) pode executar código arbitrário no servidor.
Restringindo os pacotes, apenas classes do nosso domínio são desserializadas.

#### 7. Verificação de integridade em eventos Kafka (`KafkaEventIntegrityVerifier`)

Assina payloads de eventos antes de publicar no Kafka e verifica ao consumir.

```
📁 shared/security/KafkaEventIntegrityVerifier.java
```

**Fluxo:**

```
Produtor:
  1. Serializar evento → JSON payload
  2. HMAC = KafkaEventIntegrityVerifier.signPayload(payload)
  3. Adicionar header "X-Integrity-HMAC" = HMAC
  4. Publicar no Kafka

Consumidor:
  1. Receber record do Kafka
  2. verified = KafkaEventIntegrityVerifier.verifyRecord(record)
  3. Se !verified → REJEITAR (log de segurança)
  4. Se verified → processar normalmente
```

**Backward compatibility:** eventos antigos sem header HMAC são aceitos com warning,
permitindo migração gradual.

### Configuração em `application.yml`

```yaml
cinelog:
    security:
        integrity:
            # Chave HMAC para integridade (fallback: usa JWT secret)
            secret: ${CINELOG_SECURITY_INTEGRITY_SECRET:${CINELOG_SECURITY_JWT_SECRET:...}}
        action-token:
            expiration-seconds: 3600 # Tokens de ação expiram em 1h
```

### Diagrama: camadas de integridade

```
┌─────────────────────────────────────────────────────┐
│                    REQUEST HTTP                      │
│  TamperProofRequestValidator                        │
│  ├── validateUserOwnership(body.userId vs JWT)      │
│  ├── validateEmailOwnership(body.email vs JWT)      │
│  └── validateRoleNotEscalated(body.role)            │
├─────────────────────────────────────────────────────┤
│                   PERSISTENCE                        │
│  @Version (optimistic locking)                      │
│  ├── UPDATE ... WHERE version = ? → auto-increment  │
│  └── Conflito → OptimisticLockException → 409       │
│                                                      │
│  IntegrityService (HMAC)                            │
│  ├── signEntity(fields...) → HMAC no campo          │
│  └── verifyEntity(hmac, fields...) → tamper check   │
├─────────────────────────────────────────────────────┤
│                     EVENTOS                          │
│  KafkaEventIntegrityVerifier                        │
│  ├── Produtor: header X-Integrity-HMAC              │
│  └── Consumidor: verify antes de processar          │
│                                                      │
│  Kafka trusted.packages (restrito)                  │
│  └── Apenas pacotes do domínio aceitos              │
├─────────────────────────────────────────────────────┤
│                   AUDITORIA                          │
│  AuditIntegrityService (hash chain)                 │
│  ├── Cada registro → hash do anterior               │
│  └── verifyChain() → detecta adulteração            │
├─────────────────────────────────────────────────────┤
│                  AÇÕES SEGURAS                       │
│  SecureActionTokenService                           │
│  ├── Token HMAC: purpose + userId + expiry          │
│  └── Impede forgery de links de reset/confirm       │
└─────────────────────────────────────────────────────┘
```

### Checklist de proteções A08:2025

- [x] HMAC-SHA256 para integridade de dados críticos (`IntegrityService`)
- [x] Comparação em tempo constante (previne timing attack)
- [x] `@Version` JPA para optimistic locking em 9 entidades
- [x] Handler para `OptimisticLockException` → 409 Conflict
- [x] Audit trail com hash chain — blockchain-lite (`AuditIntegrityService`)
- [x] Tabela `audit_integrity_log` via Liquibase
- [x] Verificação periódica de integridade da cadeia (`verifyChain()`)
- [x] `TamperProofRequestValidator` para anti-tampering em requests
- [x] Validação de ownership (userId, email) vs JWT autenticado
- [x] Proteção contra privilege escalation (role não-admin)
- [x] `SecureActionTokenService` para tokens HMAC de ações
- [x] Tokens de ação com propósito, expiração e HMAC
- [x] Kafka `trusted.packages` restrito (anti-RCE por deserialização)
- [x] `KafkaEventIntegrityVerifier` para assinatura de eventos
- [x] Handler para `TamperDetectedException` → 403 Forbidden
- [x] Handler para `InvalidActionTokenException` → 400 Bad Request
- [ ] Subresource Integrity (SRI) para assets frontend — **N/A (API-only)**
- [ ] SLSA provenance para artefatos de build — **roadmap futuro**

---

## A09:2025 (OWASP) — Security Logging and Alerting Failures

> **Status:** implementado.
>
> Esta categoria abrange falhas em logging, monitoramento e alertas de segurança.
> Sem logs adequados e alertas, ataques podem passar despercebidos por semanas ou meses.
> Segundo o IBM Cost of a Data Breach 2024, o tempo médio para identificar uma breach
> é **194 dias**. Com logging e alertas bem configurados, esse tempo cai para horas ou minutos.

### O problema

Imagine o seguinte cenário: um atacante está fazendo **credential stuffing** no seu endpoint
de login. Ele testa 10.000 credenciais vazadas em 30 minutos. O que acontece se você não tem
logging de segurança?

| Situação                    | Sem A09                      | Com A09                             |
| --------------------------- | ---------------------------- | ----------------------------------- |
| Brute force no login        | Ninguém percebe              | Alerta em 5 min                     |
| JWT adulterado              | Catch silencioso, log vazio  | `SEC-010` com IP, URI e razão       |
| SQL injection tentada       | Warning no log geral (ruído) | ALERT no log de segurança dedicado  |
| Quem acessou dados de quem? | Sem registro                 | Audit trail com principal + recurso |
| Rate limit atingido         | Warning sem métrica          | Counter Prometheus + alerta         |

### Cenário real: o catch silencioso

**Antes (vulnerável):**

```java
// JwtAuthenticationFilter.java — ANTES
} catch (Exception e) {
    // token inválido/expirado -> segue sem autenticar   ← SILENT!
    SecurityContextHolder.clearContext();
}
```

O atacante envia milhares de JWTs adulterados e nenhum log é gerado. Nenhuma métrica. Nenhum alerta.
A equipe de segurança nunca descobre.

**Depois (corrigido):**

```java
// JwtAuthenticationFilter.java — DEPOIS
} catch (ExpiredJwtException e) {
    securityEventLogger.log(SecurityEvent.JWT_EXPIRED, Map.of(
            "uri", InputSanitizer.sanitizeForLog(request.getRequestURI()),
            "reason", "expired"));
    securityMetrics.incrementJwtFailure("expired");
    SecurityContextHolder.clearContext();

} catch (MalformedJwtException | SignatureException e) {
    securityEventLogger.log(SecurityEvent.JWT_INVALID, Map.of(
            "uri", InputSanitizer.sanitizeForLog(request.getRequestURI()),
            "reason", e.getClass().getSimpleName()));
    securityMetrics.incrementJwtFailure("invalid");
    SecurityContextHolder.clearContext();
}
```

Agora cada JWT inválido gera: log estruturado JSON no arquivo de segurança dedicado,
métrica Micrometer (`cinelog.security.jwt_failures_total`), e avaliação de threshold
para possível alerta.

### Implementação

#### 1. Taxonomia de eventos (SecurityEvent)

Todos os eventos de segurança são tipados em um enum com campos padronizados:

```java
public enum SecurityEvent {
    AUTH_SUCCESS          ("SEC-001", Severity.INFO,     "Login bem-sucedido",                    false),
    AUTH_FAILURE          ("SEC-002", Severity.WARNING,  "Tentativa de login falha",              true),
    AUTH_LOCKED           ("SEC-003", Severity.CRITICAL, "Conta bloqueada por excesso de falhas", true),
    JWT_INVALID           ("SEC-010", Severity.WARNING,  "JWT inválido recebido",                 true),
    JWT_EXPIRED           ("SEC-011", Severity.INFO,     "JWT expirado",                          false),
    RATE_LIMITED          ("SEC-030", Severity.WARNING,  "Requisição bloqueada por rate limit",   true),
    SQL_INJECTION_ATTEMPT ("SEC-040", Severity.ALERT,    "Padrão de SQL Injection detectado",     true),
    TAMPER_DETECTED       ("SEC-041", Severity.ALERT,    "Adulteração de dados detectada",        true),
    SENSITIVE_DATA_ACCESS ("SEC-050", Severity.INFO,     "Acesso a dados sensíveis registrado",   false),
    // ... (20 eventos no total)
}
```

**Por que tipado?** Eventos de segurança precisam ser pesquisáveis e agregáveis.
Um código como `SEC-040` é mais útil em um SIEM do que a string "Padrão de SQL Injection detectado".

**Severity levels:**

| Severidade | SLF4J   | Exemplo                          | Ação esperada       |
| ---------- | ------- | -------------------------------- | ------------------- |
| INFO       | `info`  | Login OK, token refreshed        | Nenhuma (registro)  |
| WARNING    | `warn`  | Login falho, rate limit atingido | Monitorar tendência |
| CRITICAL   | `error` | Account lockout, JWT adulterado  | Investigar em 1h    |
| ALERT      | `error` | SQL injection, tampering         | Resposta imediata   |

#### 2. Logger centralizado (SecurityEventLogger)

Serviço Spring que:

1. Escreve no logger dedicado `SECURITY` (topic SLF4J separado do root logger)
2. Usa **Marker** `SECURITY` para routing no logback (appender dedicado)
3. Monta payload estruturado com campos fixos: `securityEvent`, `severity`, `principal`, `clientIp`, `traceId`, `timestamp`, `details`
4. **Mascara dados sensíveis** automaticamente via `SensitiveDataMasker` (A04)
5. **Sanitiza** valores de log via `InputSanitizer.sanitizeForLog()` (A05)
6. Encaminha eventos alertáveis para `SecurityAlertService`

```java
// Exemplo de uso em qualquer service/filter:
securityEventLogger.log(SecurityEvent.AUTH_FAILURE, Map.of(
        "reason", "invalid_credentials",
        "email", email,          // ← mascarado automaticamente? NÃO — email é exibido mascarado pelo resolvePrincipal()
        "ip", clientIp));
```

**Saída JSON gerada:**

```json
{
    "securityEvent": "SEC-002",
    "eventName": "AUTH_FAILURE",
    "severity": "WARNING",
    "description": "Tentativa de login falha",
    "timestamp": "2025-01-15T10:30:00Z",
    "traceId": "abc123def456",
    "clientIp": "192.168.1.100",
    "principal": "us***@example.com",
    "details": {
        "reason": "invalid_credentials"
    }
}
```

#### 3. Alertas automatizados (SecurityAlertService)

O serviço mantém **contadores sliding window** por tipo de evento. Quando N eventos
ocorrem dentro de M segundos, um alerta é disparado.

**Thresholds configuráveis** (application.yml):

```yaml
cinelog:
    security:
        alerting:
            window-seconds: 300 # Janela de avaliação: 5 minutos
            threshold-auth-failure: 10 # 10 falhas → possível brute force
            threshold-sqli: 3 # 3 SQLi → ataque ativo
            threshold-rate-limit: 50 # 50 rate limits → DDoS/scraping
            threshold-tamper: 1 # 1 tamper → SEMPRE alerta (zero tolerance)
```

**Fluxo de alerta:**

```
SecurityEventLogger.log(AUTH_FAILURE)
    ↓
SecurityAlertService.evaluate(AUTH_FAILURE)
    ↓ contadores: 10 em 5 min?
    ↓ SIM → triggerAlert()
        → log.error("🚨 SECURITY ALERT: SEC-002 ...")
        → Counter: cinelog.security.alerts_total{event=AUTH_FAILURE}
        → (extensível: webhook Slack, PagerDuty, email, Kafka topic)
```

**Por que sliding window e não simplesmente "a cada N"?**
Sem janela temporal, 10 falhas em 1 ano seriam alerta. Com janela de 5 minutos,
10 falhas concentradas = padrão de ataque real.

#### 4. Métricas de segurança dedicadas (SecurityMetricsService)

Métricas Micrometer separadas de BusinessMetricsService:

| Métrica                                   | Labels                      | Descrição                |
| ----------------------------------------- | --------------------------- | ------------------------ |
| `cinelog.security.auth_failures_total`    | `reason`                    | Falhas de autenticação   |
| `cinelog.security.account_lockouts_total` | —                           | Contas bloqueadas        |
| `cinelog.security.jwt_failures_total`     | `reason`                    | JWT inválido/expirado    |
| `cinelog.security.rate_limit_total`       | `path_class`                | Rate limit atingido      |
| `cinelog.security.sqli_attempts_total`    | —                           | SQL injection bloqueado  |
| `cinelog.security.access_denied_total`    | —                           | Acesso negado (403)      |
| `cinelog.security.tamper_detected_total`  | `type`                      | Tampering detectado      |
| `cinelog.security.sensitive_access_total` | `resource`                  | Acesso a dados sensíveis |
| `cinelog.security.events_total`           | `event`, `severity`, `code` | Todos os eventos         |
| `cinelog.security.alerts_total`           | `event`, `severity`         | Alertas disparados       |

**Por que separadas?** Métricas de negócio (media.created, user.registered) têm
consumidores diferentes das métricas de segurança (SOC, compliance). Prefixo `cinelog.security.*`
permite dashboard Grafana dedicado e alerting rules Prometheus independentes.

#### 5. Auditoria de acesso a dados sensíveis (DataAccessAuditAspect)

AOP aspect que intercepta métodos anotados com `@AuditSensitiveAccess`:

```java
@AuditSensitiveAccess(resource = "user_profile", action = "VIEW")
public UserDTO getUserById(Long id) {
    // execução normal — aspecto registra quem acessou
}
```

O aspecto:

1. Identifica o **principal** autenticado (quem acessou)
2. Registra o **recurso** e **ação** via SecurityEventLogger (`SEC-050`)
3. Incrementa `cinelog.security.sensitive_access_total{resource=user_profile}`
4. Funciona mesmo em caso de exceção (tentativa é registrada)

**Por que annotation-driven?** Código de auditoria em cada método é DRY violation
e inevitavelmente esquecido. Com `@AuditSensitiveAccess`, a auditoria é declarativa
e impossível de "esquecer" — se o método está anotado, o log acontece.

#### 6. Log de segurança dedicado (logback-spring.xml)

Appender dedicado com **marker-based routing**:

```xml
<!-- Só aceita eventos com marker SECURITY -->
<appender name="SECURITY_FILE" class="RollingFileAppender">
    <file>./logs/cinelog-security.log</file>
    <filter class="EvaluatorFilter">
        <evaluator class="OnMarkerEvaluator">
            <marker>SECURITY</marker>
        </evaluator>
        <onMatch>ACCEPT</onMatch>
        <onMismatch>DENY</onMismatch>
    </filter>
    <!-- Retenção: 90 dias (compliance LGPD/PCI-DSS) -->
    <rollingPolicy class="TimeBasedRollingPolicy">
        <maxHistory>90</maxHistory>
    </rollingPolicy>
</appender>
```

**Diferenças do log geral:**

| Característica    | Log geral              | Log de segurança               |
| ----------------- | ---------------------- | ------------------------------ |
| Arquivo           | `cinelog.log`          | `cinelog-security.log`         |
| Retenção          | 30 dias                | 90 dias (compliance)           |
| Conteúdo          | Tudo                   | Só eventos com marker SECURITY |
| Profiles          | dev: texto, prod: JSON | Ambos: JSON + file             |
| Destino adicional | Logstash               | Logstash (SIEM-ready)          |

**Por que 90 dias?** A LGPD (Art. 37) exige rastreabilidade de operações sobre dados pessoais.
O PCI-DSS (Req. 10.7) exige 1 ano de histórico de auditoria (90 dias online + arquivo).

#### 7. Correções em componentes existentes

**JwtAuthenticationFilter** — Catch silencioso → logging granular:

- `ExpiredJwtException` → `SEC-011` (INFO) + métrica `jwt_failures_total{reason=expired}`
- `MalformedJwtException | SignatureException` → `SEC-010` (WARNING) + métrica `jwt_failures_total{reason=invalid}`
- Genérico → `SEC-010` + métrica `jwt_failures_total{reason=unknown}`

**AuditTrailAspect** — userId era sempre `null`:

- Agora extrai do `SecurityContextHolder.getContext().getAuthentication()`
- Auditoria sem "quem fez" é inútil para compliance

**RateLimitFilter** — log.warn sem métrica:

- Agora dispara `SEC-030` via SecurityEventLogger
- Incrementa `cinelog.security.rate_limit_total{path_class=auth|general}`

**SqlInjectionFilter** — log.warn sem métrica:

- Agora dispara `SEC-040` (ALERT) via SecurityEventLogger
- Incrementa `cinelog.security.sqli_attempts_total`
- Zero tolerância: threshold de 3 tentativas em 5 min → alerta

**LoginAttemptService** — log.warn sem métrica:

- Agora dispara `SEC-003` (CRITICAL) quando conta é bloqueada
- Incrementa `cinelog.security.account_lockouts_total`

### Arquivos criados/modificados

| Arquivo                        | Ação           | Descrição                                                           |
| ------------------------------ | -------------- | ------------------------------------------------------------------- |
| `SecurityEvent.java`           | **Criado**     | Enum com 20 eventos, severidade, código e flag alertable            |
| `SecurityEventLogger.java`     | **Criado**     | Logger centralizado com marker SECURITY, mascaramento e sanitização |
| `SecurityAlertService.java`    | **Criado**     | Detecção de padrões (sliding window) + thresholds configuráveis     |
| `SecurityMetricsService.java`  | **Criado**     | 10 métricas Micrometer dedicadas a segurança                        |
| `AuditSensitiveAccess.java`    | **Criado**     | Annotation para auditoria de acesso a dados sensíveis               |
| `DataAccessAuditAspect.java`   | **Criado**     | Aspect AOP que intercepta @AuditSensitiveAccess                     |
| `logback-spring.xml`           | **Modificado** | Appender SECURITY_FILE (90 dias), marker routing, async wrapper     |
| `JwtAuthenticationFilter.java` | **Modificado** | Catch silencioso → logging granular por tipo de exceção JWT         |
| `AuditTrailAspect.java`        | **Modificado** | userId null → integrado com SecurityContext                         |
| `RateLimitFilter.java`         | **Modificado** | +SecurityEventLogger, +SecurityMetricsService                       |
| `SqlInjectionFilter.java`      | **Modificado** | +SecurityEventLogger, +SecurityMetricsService                       |
| `LoginAttemptService.java`     | **Modificado** | +SecurityEventLogger, +SecurityMetricsService                       |
| `SecurityConfig.java`          | **Modificado** | Atualizado para injetar novas dependências nos filtros              |
| `application.yml`              | **Modificado** | Bloco cinelog.security.alerting com thresholds                      |

### Checklist de conformidade

- [x] SensitiveDataMasker para mascaramento em logs (A04:2025)
- [x] InputSanitizer.sanitizeForLog() contra log injection (A05:2025)
- [x] Logging estruturado com Logback JSON
- [x] Stack de observabilidade (Prometheus, Grafana, Loki, Tempo)
- [x] Taxonomia de eventos de segurança com códigos padronizados (SEC-xxx)
- [x] Alertas automatizados com thresholds configuráveis
- [x] Log de segurança dedicado com retenção de 90 dias (compliance)
- [x] Métricas Micrometer dedicadas a segurança
- [x] Auditoria de acesso a dados sensíveis (annotation-driven)
- [x] JwtAuthenticationFilter com logging granular (antes silencioso)
- [x] AuditTrailAspect com userId integrado ao SecurityContext
- [x] Filtros de segurança (Rate Limit, SQLi, Login) com métricas e eventos

---

## A10:2025 (OWASP) — Mishandling of Exceptional Conditions

> **Status:** ✅ implementado.
>
> Esta categoria é **nova no OWASP 2025** e abrange situações em que condições
> excepcionais (erros, timeouts, estados inesperados) são tratadas de forma insegura,
> levando a vazamento de informações, estados inconsistentes ou bypass de controles.

### O que esta categoria cobre

O OWASP A10:2025 trata de cenários onde a aplicação:

1. **Engole exceções silenciosamente** — `catch (Exception e) {}` sem log nem métrica
2. **Vaza informações via stack trace** — retorna detalhes internos ao cliente
3. **Não implementa circuit breaker** — uma API externa fora do ar derruba toda a aplicação
4. **Não trata timeout** — chamadas HTTP/DB ficam penduradas indefinidamente
5. **Mata o scheduler** — uma exceção transiente em `@Scheduled` cancela todas as próximas execuções
6. **Ignora erros fatais** — `OutOfMemoryError` e `StackOverflowError` passam despercebidos
7. **Não monitora saúde de dependências** — não há health check para APIs externas ou filas

### Checklist de implementação

- [x] GlobalExceptionHandler com respostas RFC 9457 ProblemDetail
- [x] Stack traces suprimidos em produção (A02:2025)
- [x] Mensagens de erro genéricas em respostas HTTP
- [x] Circuit breaker para serviços externos (TMDb)
- [x] Tratamento de timeout em chamadas HTTP (WebClient + Resilience4j)
- [x] Fallback seguro para cada método de integração
- [x] Bulkhead para limitar chamadas concorrentes
- [x] Scheduler global error handler com logging estruturado
- [x] Proteção de todos os `@Scheduled` com try-catch
- [x] Health indicators customizados (TMDb + Outbox)
- [x] JVM safety hooks (uncaught exception handler + shutdown hook)
- [x] Handlers específicos no GlobalExceptionHandler para circuit breaker, bulkhead, timeout

---

### 1. Resilience4j — Circuit Breaker, Retry, Bulkhead

**Cenário**: A API do TMDb fica fora do ar. Sem circuit breaker, cada request do
CineLog tentaria chamar o TMDb (timeout de 3s), segurando threads e degradando
toda a aplicação. Com circuit breaker, após N falhas consecutivas, o circuito
**abre** e as próximas chamadas retornam imediatamente via fallback — sem sequer
tentar a chamada HTTP.

**Configuração** (`application.yml`):

```yaml
resilience4j:
    circuitbreaker:
        instances:
            tmdb:
                slidingWindowSize: 20 # avalia últimas 20 chamadas
                failureRateThreshold: 50 # abre se 50%+ falharem
                waitDurationInOpenState: 10s # espera 10s antes de testar recuperação
                permittedNumberOfCallsInHalfOpenState: 3
                automaticTransitionFromOpenToHalfOpenEnabled: true
                registerHealthIndicator: true # expõe estado no /actuator/health

    retry:
        instances:
            tmdb:
                maxAttempts: 3 # tenta até 3x antes de desistir
                waitDuration: 500ms # intervalo entre retries
                retryExceptions: # apenas exceções transitórias
                    - WebClientRequestException
                    - IOException

    timelimiter:
        instances:
            tmdb:
                timeoutDuration: 3s # cancela após 3s de espera
                cancelRunningFuture: true

    bulkhead:
        instances:
            tmdb:
                maxConcurrentCalls: 10 # max 10 chamadas simultâneas
                maxWaitDuration: 500ms # espera max 500ms por slot
```

**Por que cada componente?**

| Componente          | Problema resolvido                          | HTTP Status             |
| ------------------- | ------------------------------------------- | ----------------------- |
| **Circuit Breaker** | API externa fora do ar → fail-fast          | 503 Service Unavailable |
| **Retry**           | Falha transitória (rede instável) → retenta | Transparente            |
| **TimeLimiter**     | Chamada pendurada → timeout forçado         | 504 Gateway Timeout     |
| **Bulkhead**        | Pico de requests → limitar concorrência     | 429 Too Many Requests   |

---

### 2. Fallback Methods — Degradação Graciosa

**Cenário**: O TMDb está indisponível (circuit breaker OPEN). O usuário busca um
filme. Em vez de retornar 500, o fallback retorna `Optional.empty()` ou uma lista
vazia, permitindo que o front-end exiba "resultados indisponíveis no momento".

```java
// TmdbClientAdapter.java — TODOS os métodos públicos têm fallback

@Retry(name = "tmdb")
@CircuitBreaker(name = "tmdb", fallbackMethod = "fallbackSearchMovies")
@Bulkhead(name = "tmdb")
public TmdbSearchResult<TmdbMediaSummary> searchMovies(String query, Integer year, int page) {
    // chamada real ao TMDb via WebClient
}

// Fallback: retorna resultado vazio (safe default)
@SuppressWarnings("unused")
private TmdbSearchResult<TmdbMediaSummary> fallbackSearchMovies(
        String query, Integer year, int page, Throwable ex) {
    log.warn("Fallback searchMovies(query='{}') due to {}", query, ex.toString());
    return emptySearchResult(page);
}
```

**Princípio**: O fallback NUNCA deve lançar exceção. Ele deve retornar um valor
seguro (empty, default, cached) que permita à aplicação continuar operando em
modo degradado.

**Métodos protegidos**:

| Método                                     | Fallback                    |
| ------------------------------------------ | --------------------------- |
| `fetchByTmdbId()`                          | `Optional.empty()`          |
| `fetchMovie()` / `fetchTv()`               | `Optional.empty()`          |
| `searchMovies()` / `searchTvShows()`       | Lista vazia paginada        |
| `discoverMovies()` / `discoverTvShows()`   | Lista vazia paginada        |
| `fetchMovieGenres()` / `fetchTvGenres()`   | `Collections.emptyList()`   |
| `fetchMovieCredits()` / `fetchTvCredits()` | `Optional.empty()`          |
| `fetchImageConfig()`                       | Cache local ou config vazia |

---

### 3. GlobalExceptionHandler — Handlers Específicos para A10

**Cenário**: Uma `CallNotPermittedException` sobe do Resilience4j quando o circuit
breaker está OPEN. Sem handler específico, cairia no fallback genérico 500 com
mensagem "Erro inesperado" — incorreto e confuso. Com handler dedicado, retorna
503 com mensagem clara.

```java
// Circuit breaker aberto → 503
@ExceptionHandler(CallNotPermittedException.class)
public ProblemDetail handleCircuitBreakerOpen(CallNotPermittedException ex,
        HttpServletRequest req) {
    log.warn("A10 — Circuit breaker OPEN. CB: {}", ex.getCausingCircuitBreakerName());

    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
            "Serviço externo temporariamente indisponível.");
    pd.setTitle("Service Unavailable");
    pd.setType(URI.create("https://api.cinelog.com/errors/service-unavailable"));
    pd.setProperty("circuitBreaker", ex.getCausingCircuitBreakerName());
    setCommon(pd, req, "CIRCUIT_BREAKER_OPEN");
    return pd;
}

// Bulkhead cheio → 429
@ExceptionHandler(BulkheadFullException.class)
public ProblemDetail handleBulkheadFull(...) { /* 429 Too Many Requests */ }

// Tipo de parâmetro inválido → 400
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ProblemDetail handleTypeMismatch(...) { /* 400 Bad Request */ }

// Parâmetro obrigatório ausente → 400
@ExceptionHandler(MissingServletRequestParameterException.class)
public ProblemDetail handleMissingParam(...) { /* 400 Bad Request */ }

// Erro de serviço externo (WebClient) → 502
@ExceptionHandler(WebClientResponseException.class)
public ProblemDetail handleWebClientError(...) { /* 502 Bad Gateway */ }

// Erro genérico de banco (exceto DataIntegrity) → 500 com mensagem segura
@ExceptionHandler(DataAccessException.class)
public ProblemDetail handleDataAccess(...) { /* 500 sem expor detalhes SQL */ }
```

**Hierarquia de handlers** (ordem de especificidade do Spring):

```
CallNotPermittedException     → 503 (circuit breaker)
BulkheadFullException         → 429 (concorrência)
WebClientResponseException    → 502 (bad gateway)
DataIntegrityViolation        → 409 (constraint)
DataAccessException           → 500 (banco genérico)
Exception                     → 500 (fallback final)
```

---

### 4. Scheduler Error Handler — Tarefas Agendadas Seguras

**Cenário perigoso**: O `@Scheduled cleanupExpiredTokens()` executa às 02:00.
Se o banco estiver temporariamente indisponível, a exceção `DataAccessException`
sobe sem try-catch. O Spring loga e **continua**, mas em certas configurações de
pool de threads, pode haver side effects inesperados.

**Solução em duas camadas**:

**Camada 1**: try-catch individual em cada `@Scheduled`:

```java
@Scheduled(cron = "0 0 2 * * ?")
@Transactional
public void cleanupExpiredTokens() {
    try {
        Instant cutoff = Instant.now().minusSeconds(86400);
        int deleted = refreshTokenRepository.deleteExpiredBefore(cutoff);
        if (deleted > 0) {
            log.info("Housekeeping: removidos {} refresh tokens expirados", deleted);
        }
    } catch (Exception ex) {
        log.error("A10 — Falha no housekeeping de refresh tokens. "
                + "Scheduler continuará na próxima execução.", ex);
    }
}
```

**Camada 2**: `ErrorHandler` global no `TaskScheduler` (defense-in-depth):

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("cinelog-sched-");
        scheduler.setErrorHandler(throwable -> {
            if (throwable instanceof Error error) {
                // OOM, StackOverflow → re-lança (não abafar)
                log.error("FATAL em tarefa agendada", error);
                throw error;
            }
            log.error("Exceção não tratada em tarefa agendada. "
                    + "Scheduler continuará.", throwable);
        });
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
```

**Tarefas `@Scheduled` protegidas**:

| Classe                                     | Cron      | try-catch?   |
| ------------------------------------------ | --------- | ------------ |
| `OutboxPublisherJob.processOutboxEvents`   | A cada 5s | ✅ Sim       |
| `OutboxPublisherJob.cleanupOldEvents`      | 01:00     | ✅ Sim       |
| `RefreshTokenService.cleanupExpiredTokens` | 02:00     | ✅ Sim (A10) |
| `InboxHousekeepingJob.cleanup`             | 03:00     | ✅ Sim       |

---

### 5. Health Indicators — Monitoramento de Dependências

**Cenário**: O TMDb está fora do ar há 30 minutos. Sem health indicator, o
`/actuator/health` reporta `UP` — os balanceadores de carga continuam roteando
tráfego. Com health indicator customizado, o status muda para `DOWN` e o Kubernetes
pode redirecionar tráfego.

**TmdbHealthIndicator**:

```java
@Component
public class TmdbHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("tmdb");

        return switch (cb.getState()) {
            case OPEN -> Health.down()
                .withDetail("reason", "Circuit breaker OPEN")
                .withDetail("failureRate", cb.getMetrics().getFailureRate() + "%")
                .build();
            case HALF_OPEN -> Health.unknown()
                .withDetail("reason", "Testando recuperação")
                .build();
            default -> {
                // Probe leve: GET /configuration
                tmdbWebClient.get().uri("/configuration")
                    .retrieve().toBodilessEntity()
                    .block(Duration.ofSeconds(3));
                yield Health.up().build();
            }
        };
    }
}
```

**OutboxHealthIndicator**:

```java
@Component
public class OutboxHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        int pending  = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_event WHERE status = 'PENDING'", Integer.class);
        int failed = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM outbox_event WHERE status = 'FAILED_PERM'", Integer.class);

        if (failed > 10)   return Health.down().withDetail("reason", "Excesso de falhas permanentes");
        if (pending > 100)  return Health.down().withDetail("reason", "Acúmulo de pendentes");
        return Health.up().withDetail("pendingEvents", pending).build();
    }
}
```

**Endpoints de health**:

| Endpoint                  | Indica                                    |
| ------------------------- | ----------------------------------------- |
| `/actuator/health/tmdb`   | Estado da API TMDb (CB + probe HTTP)      |
| `/actuator/health/outbox` | Saúde da fila outbox (pendentes + falhas) |
| `/actuator/health/db`     | Banco MySQL (auto-config do Spring)       |
| `/actuator/health/redis`  | Cache Redis (auto-config do Spring)       |

---

### 6. JVM Safety Hooks — Erros Fatais e Shutdown

**Cenário**: Uma thread do pool de Kafka lança `OutOfMemoryError`. Sem handler
global, o erro aparece apenas no stderr (que pode não estar sendo coletado). A
thread morre silenciosamente e a aplicação continua funcionando com capacidade
reduzida — ninguém é alertado.

**Solução**:

```java
@Component
public class JvmSafetyConfig {

    @PostConstruct
    void init() {
        // Handler global para exceções não capturadas em qualquer thread
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof OutOfMemoryError) {
                System.err.println("FATAL: OOM na thread " + thread.getName());
                log.error("FATAL OOM na thread '{}'", thread.getName(), throwable);
            } else {
                log.error("Exceção não capturada na thread '{}'",
                        thread.getName(), throwable);
            }
        });

        // Shutdown hook — garante log antes do encerramento
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM shutdown hook acionado. Flushing logs.");
        }, "cinelog-shutdown-hook"));
    }

    @PreDestroy
    void onShutdown() {
        log.info("Spring context destruído. Graceful shutdown completo.");
    }
}
```

**Por que `System.err` além do logger?** Em cenários de `OutOfMemoryError`, o
logger pode falhar ao tentar alocar memória para formatar a mensagem. O `System.err`
é a última linha de defesa.

---

### 7. Resumo de Arquivos Criados/Modificados

| Arquivo                       | Ação       | Descrição                                                                                   |
| ----------------------------- | ---------- | ------------------------------------------------------------------------------------------- |
| `application.yml`             | Modificado | +timeLimiter, +bulkhead, +registerHealthIndicator                                           |
| `TmdbClientAdapter.java`      | Modificado | @CircuitBreaker + @Retry + @Bulkhead em todos os métodos + fallbacks                        |
| `GlobalExceptionHandler.java` | Modificado | +6 handlers (CallNotPermitted, Bulkhead, TypeMismatch, MissingParam, WebClient, DataAccess) |
| `RefreshTokenService.java`    | Modificado | try-catch em cleanupExpiredTokens()                                                         |
| `SchedulingConfig.java`       | Criado     | ErrorHandler global + TaskScheduler customizado                                             |
| `TmdbHealthIndicator.java`    | Criado     | Health indicator para TMDb (CB state + probe)                                               |
| `OutboxHealthIndicator.java`  | Criado     | Health indicator para outbox (pendentes + falhas)                                           |
| `JvmSafetyConfig.java`        | Criado     | Uncaught exception handler + shutdown hook                                                  |

---

## Proteções Implementadas

### 1. SQL Injection

**Proteção**: JPA com Prepared Statements

```java
// ✅ Seguro - JPA
@Query("SELECT m FROM MediaEntity m WHERE m.title = :title")
List<MediaEntity> findByTitle(@Param("title") String title);

// ❌ Vulnerável - String concatenation
@Query("SELECT m FROM MediaEntity m WHERE m.title = '" + title + "'")
```

### 2. XSS (Cross-Site Scripting)

**Proteção**: Validação e sanitização

```java
@Data
public class CreateMediaRequest {

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[a-zA-Z0-9\\s]+$", message = "Título contém caracteres inválidos")
    private String title;
}
```

### 3. CSRF (Cross-Site Request Forgery)

**Configuração atual**: CSRF desabilitado para API stateless com JWT.

```java
http.csrf(csrf -> csrf.disable());
```

**Justificativa**: a aplicação usa `SessionCreationPolicy.STATELESS` e autenticação por Bearer token.

### 4. Clickjacking

**Proteção**: X-Frame-Options header

```java
http.headers()
    .frameOptions().deny();
```

### 5. CORS

**Proteção**: Configuração restritiva

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("https://cinelog.com");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

### 6. Rate Limiting

**Proteção**: Bucket4j

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain chain) throws ServletException, IOException {

        String key = getClientIP(request);
        Bucket bucket = resolveBucket(key);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests");
        }
    }

    private Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limit).build();
        });
    }
}
```

### 7. Password Hashing

**Proteção**: BCrypt

```java
@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;

    public User createUser(CreateUserCommand command) {
        String hashedPassword = passwordEncoder.encode(command.getPassword());

        User user = new User();
        user.setPassword(hashedPassword);
        // ...

        return userRepository.save(user);
    }
}

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // Força 12
    }
}
```

---

## Boas Práticas

### 1. Senhas

**Requisitos**:

- Mínimo 8 caracteres
- Pelo menos 1 letra maiúscula
- Pelo menos 1 letra minúscula
- Pelo menos 1 número
- Pelo menos 1 caractere especial

**Validação**:

```java
@Data
public class CreateUserRequest {

    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Senha não atende aos requisitos de segurança"
    )
    private String password;
}
```

### 2. Secrets Management

**❌ Não faça**:

```java
// Hardcoded secret
String secret = "minha-chave-secreta";
```

**✅ Faça**:

```java
// Variável de ambiente
@Value("${JWT_SECRET}")
private String secret;
```

**Melhor prática**: Use serviços de gerenciamento de secrets:

- AWS Secrets Manager
- Azure Key Vault
- HashiCorp Vault

### 3. HTTPS

**Produção**: Sempre usar HTTPS

```yaml
# application-prod.yml
server:
    ssl:
        enabled: true
        key-store: classpath:keystore.p12
        key-store-password: ${KEYSTORE_PASSWORD}
        key-store-type: PKCS12
```

### 4. Auditoria de Dependências

```bash
# Verificar vulnerabilidades
./mvnw dependency-check:check

# Atualizar dependências
./mvnw versions:display-dependency-updates
```

### 5. Headers de Segurança

```java
http.headers()
    .contentSecurityPolicy("default-src 'self'")
    .and()
    .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN)
    .and()
    .xssProtection()
    .and()
    .contentTypeOptions()
    .and()
    .frameOptions().deny();
```

---

## Configuração

### application-prod.yml

```yaml
cinelog:
    security:
        enabled: true
        jwt:
            secret: ${JWT_SECRET}
            expiration-seconds: 3600
        cors:
            allowed-origins: ${CORS_ORIGINS}
        rate-limiting:
            enabled: true
            requests-per-minute: 100

spring:
    security:
        user:
            password: ${ADMIN_PASSWORD} # Senha do admin padrão

server:
    ssl:
        enabled: true
    error:
        include-message: never
        include-stacktrace: never
        include-exception: false

logging:
    level:
        org.springframework.security: DEBUG # Dev only
```

---

## Auditoria

### Tabela de Auditoria

```sql
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    action VARCHAR(50),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    timestamp DATETIME,
    ip_address VARCHAR(50),
    user_agent TEXT,
    details JSON
);
```

### Implementação

```java
@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Around("@annotation(Audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        AuditLog log = new AuditLog();
        log.setUserId(getUserId(auth));
        log.setAction(joinPoint.getSignature().getName());
        log.setTimestamp(LocalDateTime.now());

        try {
            Object result = joinPoint.proceed();
            log.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            log.setStatus("FAILURE");
            log.setDetails(e.getMessage());
            throw e;
        } finally {
            auditLogRepository.save(log);
        }
    }
}

// Uso
@Audited
@PostMapping
public Media createMedia(@RequestBody CreateMediaRequest request) {
    // ...
}
```

---

## Resposta a Incidentes

### Procedimento

1. **Detecção**
    - Monitorar logs
    - Alertas automatizados
    - Relatórios de usuários

2. **Contenção**
    - Isolar sistema afetado
    - Revogar tokens comprometidos
    - Bloquear IPs maliciosos

3. **Erradicação**
    - Corrigir vulnerabilidade
    - Atualizar dependências
    - Aplicar patches

4. **Recuperação**
    - Restaurar sistema
    - Validar funcionalidade
    - Monitorar atividade

5. **Lições Aprendidas**
    - Documentar incidente
    - Atualizar procedimentos
    - Treinar equipe

### Contatos de Emergência

- **Security Team**: security@cinelog.com
- **On-Call**: +55 11 9999-9999
- **Incident Response**: incidents@cinelog.com

---

## Checklist de Segurança

### Pre-Deploy

- [ ] Secrets não commitados
- [ ] Dependências atualizadas
- [ ] Scan de vulnerabilidades executado
- [ ] Testes de segurança passando
- [ ] HTTPS configurado
- [ ] Rate limiting habilitado
- [ ] Logs de auditoria ativos
- [ ] Backups configurados

### Post-Deploy

- [ ] Monitoramento ativo
- [ ] Alertas configurados
- [ ] Documentação atualizada
- [ ] Equipe notificada
- [ ] Testes de penetração agendados

---

**Última atualização**: Fevereiro 2026
