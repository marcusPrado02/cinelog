# 🔒 Documentação de Segurança - CineLog

## Índice

1. [Visão Geral](#visão-geral)
2. [Autenticação](#autenticação)
3. [Autorização](#autorização)
4. [Proteções Implementadas](#proteções-implementadas)
5. [Boas Práticas](#boas-práticas)
6. [Configuração](#configuração)
7. [Auditoria](#auditoria)
8. [Resposta a Incidentes](#resposta-a-incidentes)

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

## Autorização

### A01 (OWASP) — Controle de Acesso Quebrado

O projeto aplica autorização em duas camadas complementares:

1. **URL Authorization** em `HttpSecurity.authorizeHttpRequests`.
2. **Method Security** com `@PreAuthorize` e `@SecureOperation`.

Modelo adotado: **deny-by-default** (`anyRequest().authenticated()`) com regras explícitas para superfícies administrativas.

### URL Authorization (estado atual)

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/auth/**").permitAll()
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPS")
    .anyRequest().authenticated());
```

### Method Security e `@SecureOperation`

- `@EnableMethodSecurity` está ativo.
- `@SecureOperation` usa `enforce=true` por padrão.
- Com `enforce=true`, ausência de permissão configurada resulta em **negação** (fail-closed).
- Recomendação: para operação sensível, declarar sempre `value` (permissão) e `module`.

Exemplo:

```java
@SecureOperation(module = "USER", value = "USER_ADMIN")
public User create(CreateUserCommand command) {
    // ...
}
```

### Riscos tratados nesta revisão

- **Inconsistência de path admin**: `/admin/**` agora exige `ADMIN` ou `OPS` no filtro HTTP.
- **Falsa sensação de segurança em AOP**: `@SecureOperation` segue comportamento fechado por padrão (`enforce=true`).
- **Superfície de actuator**: somente `/actuator/health` e `/actuator/info` públicos.

### Checklist rápido de validação (A01)

1. Usuário sem token em endpoint protegido → `401`.
2. Usuário autenticado sem role adequada em `/admin/**` → `403`.
3. Usuário com `ROLE_ADMIN` ou `ROLE_OPS` em `/admin/**` → acesso permitido.
4. Endpoint com `@SecureOperation` sem authority requerida → `403` quando `enforce=true`.

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
