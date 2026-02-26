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

### Checklist rápido de validação (A01)

1. Usuário sem token em endpoint protegido → `401 Unauthorized`.
2. Usuário autenticado sem role adequada em `/admin/**` → `403 Forbidden`.
3. Usuário com `ROLE_ADMIN` ou `ROLE_OPS` em `/admin/**` → acesso permitido.
4. Endpoint com `@SecureOperation` sem authority requerida → `403` quando `enforce=true`.
5. `/actuator/env` ou `/actuator/heapdump` sem token → `401` (não está no `permitAll`).

---

## A02 (OWASP) — Falhas Criptográficas e Exposição de Dados Sensíveis

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

### Checklist A02

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

## A03 (OWASP) — Injeção

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

### Checklist A03

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

## A04 (OWASP) — Design Inseguro

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
> | Implementação insegura (A01-A03) | Design inseguro (A04)                                          |
> | -------------------------------- | -------------------------------------------------------------- |
> | Query SQL concatenada com input  | Nenhum limite de quantas queries um usuário pode fazer por dia |
> | Senha armazenada em texto plano  | Nenhum controle de tentativas de login (brute force)           |
> | Stack trace exposto na resposta  | Mensagens de erro que revelam se um email existe no sistema    |
> | JWT sem validação de assinatura  | Nenhum controle de fluxo em operações multi-step               |
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
> │ 2. SqlInjectionFilter    │ ← Detecta payloads maliciosos (A03)
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
> │ 4. URL Authorization     │ ← hasRole, authenticated, permitAll (A01)
> │    (HttpSecurity)         │    Baseado no SecurityContext do passo 3
> └──────────┬───────────────┘
>            │
>            ▼
> ┌──────────────────────────┐
> │ 5. Controller +          │ ← @SecureOperation, @PreAuthorize (A01)
> │    Method Security        │    Validação granular no método
> └──────────┬───────────────┘
>            │
>            ▼
> ┌──────────────────────────┐
> │ 6. BusinessLimit         │ ← Limites de domínio por recurso (A04)
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

### Checklist A04

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
