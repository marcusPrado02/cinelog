# ADR-005: JWT para Autenticação Stateless

## Status

✅ **Aceito**

## Data

2025-12-05

## Contexto

Precisávamos implementar autenticação e autorização para o CineLog, considerando:

1. **Stateless** - Sem armazenar sessão no servidor (escala horizontalmente)
2. **Segurança** - Proteger endpoints sensíveis
3. **Performance** - Validação rápida de tokens
4. **Mobile-Friendly** - Suporte a apps móveis
5. **Microservices-Ready** - Preparado para arquitetura distribuída
6. **Token Expiration** - Controle de tempo de vida
7. **Refresh Tokens** - Renovação sem relogin

### Problema

Autenticação tradicional baseada em sessão:

- **Session Store** - Redis/DB necessário (stateful)
- **Sticky Sessions** - Load balancer complexo
- **Não escala bem** - Sessões replicadas entre instâncias
- **Difícil para mobile** - Cookies não funcionam bem

## Decisão

Adotamos **JWT (JSON Web Tokens)** com **Spring Security** para autenticação stateless.

### Arquitetura

```
┌─────────┐                ┌──────────┐              ┌──────────┐
│ Client  │                │   API    │              │   DB     │
└────┬────┘                └────┬─────┘              └────┬─────┘
     │                          │                         │
     │ 1. POST /auth/login      │                         │
     │ {username, password}     │                         │
     ├─────────────────────────>│                         │
     │                          │ 2. Validate credentials │
     │                          ├────────────────────────>│
     │                          │<────────────────────────┤
     │                          │ 3. Generate JWT         │
     │ 4. Return JWT            │                         │
     │<─────────────────────────┤                         │
     │                          │                         │
     │ 5. GET /api/media        │                         │
     │ Header: Authorization:   │                         │
     │   Bearer <JWT>           │                         │
     ├─────────────────────────>│                         │
     │                          │ 6. Validate JWT         │
     │                          │ (signature + expiration)│
     │                          │                         │
     │                          │ 7. Extract user info    │
     │                          │                         │
     │                          │ 8. Process request      │
     │                          ├────────────────────────>│
     │                          │<────────────────────────┤
     │ 9. Return response       │                         │
     │<─────────────────────────┤                         │
     │                          │                         │
```

### Estrutura do JWT

**Header:**

```json
{
    "alg": "HS256",
    "typ": "JWT"
}
```

**Payload:**

```json
{
    "sub": "12345", // User ID
    "username": "john.doe", // Username
    "roles": ["ROLE_USER", "ROLE_ADMIN"], // Authorities
    "iat": 1704067200, // Issued at
    "exp": 1704070800 // Expiration (1 hora)
}
```

**Signature:**

```
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

### Token Types

1. **Access Token** - Curta duração (1 hora), usado em requisições
2. **Refresh Token** - Longa duração (7 dias), renova access token

## Alternativas Consideradas

### 1. Session-Based Authentication (Spring Session)

**Prós:**

- Controle total sobre sessões
- Revogação imediata
- Familiar para devs

**Contras:**

- Stateful (Redis/DB necessário)
- Sticky sessions no load balancer
- Não escala horizontalmente bem
- Complexo para mobile

**Por que não escolhemos:** Stateful não combina com cloud-native.

### 2. OAuth2 com servidor de autorização externo (Keycloak)

**Prós:**

- Solução enterprise completa
- SSO (Single Sign-On)
- User management built-in
- Multi-tenancy

**Contras:**

- Overhead operacional (mais um serviço)
- Over-engineering para escopo atual
- Complexidade adicional
- Vendor lock-in

**Por que não escolhemos:** Over-engineering para estágio inicial.

### 3. API Keys

**Prós:**

- Simples de implementar
- Sem expiração (se necessário)
- Fácil de usar

**Contras:**

- Sem controle de expiração
- Difícil revogação
- Não transporta informações (roles)
- Menos seguro

**Por que não escolhemos:** Não oferece segurança e features necessárias.

### 4. Basic Authentication

**Prós:**

- Extremamente simples
- HTTP standard
- Fácil debug

**Contras:**

- Credenciais em cada request
- Sem expiração
- Inseguro sem HTTPS
- Não tem refresh

**Por que não escolhemos:** Não é adequado para aplicações modernas.

### 5. SAML

**Prós:**

- Enterprise standard
- SSO
- XML-based

**Contras:**

- Complexo demais
- XML verboso
- Overhead de performance
- Overkill para API REST

**Por que não escolhemos:** Muito complexo para nosso caso de uso.

## Consequências

### Positivas ✅

1. **Stateless e Escalável**
    - Servidor não armazena sessão
    - Escala horizontalmente sem problemas
    - Sem necessidade de Redis/sticky sessions

2. **Mobile-Friendly**
    - Funciona perfeitamente com apps móveis
    - Token armazenado localmente
    - Não depende de cookies

3. **Informações no Token**

    ```java
    // Extrair informações sem consultar DB
    String username = jwtService.extractUsername(token);
    List<String> roles = jwtService.extractRoles(token);
    ```

4. **Performance**
    - Validação de token = verificar signature (criptografia)
    - Sem consulta a DB em cada request
    - < 1ms para validar

5. **Cross-Domain**
    - Funciona em qualquer domínio
    - CORS-friendly
    - Não depende de cookies

6. **Microservices-Ready**
    - Tokens validáveis por qualquer serviço
    - Shared secret ou PKI
    - Sem centralização

7. **Controle de Expiração**
    ```java
    // Access token: 1 hora
    // Refresh token: 7 dias
    ```

### Negativas ❌

1. **Tamanho do Token**
    - JWT pode ficar grande (200-500 bytes)
    - Enviado em cada request (overhead)
    - Mitigado com compressão HTTP

2. **Não Revogável Facilmente**
    - Token válido até expirar
    - Revogação requer blacklist (stateful)
    - Mitigado com tokens de curta duração

3. **Secret Management**
    - Secret deve ser forte e protegido
    - Rotação de secret é complexa
    - Comprometimento = invalidar todos tokens

4. **Clock Skew**
    - Servidores devem ter relógios sincronizados
    - Diferenças causam problemas de expiração
    - Mitigado com NTP

5. **Token Theft**
    - Se roubado, é válido até expirar
    - XSS pode roubar de localStorage
    - CSRF em cookies HttpOnly

### Trade-offs Aceitáveis

| Trade-off                        | Justificativa                      |
| -------------------------------- | ---------------------------------- |
| Tamanho token → Stateless        | Vale overhead para eliminar sessão |
| Revogação difícil → Performance  | Tokens curtos mitigam risco        |
| Secret management → Simplicidade | Mais simples que PKI               |

## Implementação

### Configuração

**application.yml:**

```yaml
app:
    security:
        jwt:
            secret: ${JWT_SECRET:changeme-in-production-use-at-least-256-bits}
            access-token-expiration: 3600000 # 1 hora (ms)
            refresh-token-expiration: 604800000 # 7 dias (ms)
            issuer: cinelog-api
```

### JWT Service

**JwtService.java:**

```java
package com.cine.cinelog.shared.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.security.jwt.issuer}")
    private String issuer;

    // Gerar Access Token
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());

        return buildToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    // Gerar Refresh Token
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails.getUsername(), refreshTokenExpiration);
    }

    // Construir token
    private String buildToken(
            Map<String, Object> extraClaims,
            String subject,
            long expiration
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extrair username
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extrair roles
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("roles");
    }

    // Extrair claim genérico
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Validar token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Verificar expiração
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extrair data de expiração
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extrair todos claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Obter chave de assinatura
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### JWT Filter

**JwtAuthenticationFilter.java:**

```java
package com.cine.cinelog.shared.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Extrair token do header
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        // Se usuário não autenticado ainda
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Carregar user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validar token
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Criar authentication object
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Setar no SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### Authentication Controller

**AuthController.java:**

```java
package com.cine.cinelog.features.auth.web.controller;

import com.cine.cinelog.features.auth.web.dto.*;
import com.cine.cinelog.shared.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {

        // Autenticar credenciais
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // Carregar user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

        // Gerar tokens
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                3600 // 1 hora em segundos
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {

        // Extrair username do refresh token
        String username = jwtService.extractUsername(request.refreshToken());

        // Carregar user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Validar refresh token
        if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
            return ResponseEntity.status(401).build();
        }

        // Gerar novo access token
        String accessToken = jwtService.generateAccessToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(
                accessToken,
                request.refreshToken(), // Mesmo refresh token
                "Bearer",
                3600
        ));
    }
}
```

### Security Configuration

**SecurityConfig.java:**

```java
package com.cine.cinelog.shared.config;

import com.cine.cinelog.shared.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

## Uso

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "password123"
  }'

# Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Usar Token

```bash
curl http://localhost:8080/api/v1/media \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

## Segurança

### Boas Práticas

1. **Secret forte** (256+ bits)

    ```yaml
    jwt:
        secret: ${JWT_SECRET} # Nunca hardcode!
    ```

2. **HTTPS obrigatório** em produção

3. **Tokens curtos** (1 hora)

4. **Refresh tokens** (7 dias)

5. **HttpOnly cookies** para web (prevenir XSS)

6. **Blacklist** para logout (Redis)

7. **Rate limiting** em /login

## Validação

### Métricas de Sucesso

✅ **< 1ms** para validar token  
✅ **Zero session storage** (stateless)  
✅ **Mobile apps** funcionando  
✅ **Zero problemas** de escalabilidade

### Lições Aprendidas

1. **Tokens curtos são essenciais** - Reduzem janela de ataque
2. **Refresh tokens funcionam bem** - UX não é impactada
3. **Secret deve ser forte** - 256+ bits obrigatório
4. **Blacklist complica** - Preferir tokens curtos

## Referências

- [JWT.io](https://jwt.io/)
- [RFC 7519 - JWT](https://tools.ietf.org/html/rfc7519)
- [Spring Security + JWT](https://www.baeldung.com/spring-security-oauth-jwt)
- [JJWT Library](https://github.com/jwtk/jjwt)

## Revisões

- **2025-12-05**: Decisão inicial aceita
- **2026-01-15**: Validado - funcionando perfeitamente

---

**Mantido por:** Time CineLog  
**Próxima revisão:** Julho 2026
