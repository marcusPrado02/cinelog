# ADR-012: Modelo de Autorização — RBAC com Method Security

## Status

✅ **Aceito**

## Data

2026-03-03

## Contexto

Com a autenticação definida ([ADR-011](./ADR-011-authentication-strategy.md)), é preciso
estabelecer **como** os usuários autenticados são autorizados a executar operações.

### Problema

Sem um modelo de autorização explícito:

- Qualquer usuário autenticado poderia acessar dados de outros usuários
- Operações administrativas (delete de usuários, gerenciamento de mídia) ficam acessíveis a todos
- Difícil auditar quem pode fazer o quê
- Viola o princípio do **Menor Privilégio** (OWASP A01:2021)

### Requisitos

| Requisito                             | Tipo      |
| ------------------------------------- | --------- |
| Separação ADMIN / USER                | Must Have |
| Usuário só acessa seus próprios dados | Must Have |
| Defense-in-depth (URL + método)       | Must Have |
| Auditoria de operações privilegiadas  | Must Have |
| Suporte a role OPS (infra/operação)   | Should    |

## Decisão

Adotamos **RBAC (Role-Based Access Control)** em duas camadas complementares,
com `@EnableMethodSecurity` habilitado em `SecurityConfig`.

### Roles definidas

| Role    | Descrição                                                 |
| ------- | --------------------------------------------------------- |
| `USER`  | Usuário padrão — acessa apenas seus próprios recursos     |
| `ADMIN` | Administrador do sistema — acesso irrestrito              |
| `OPS`   | Operações / infraestrutura — acesso a DLQ, Actuator, etc. |

### Camada 1 — URL-based (SecurityConfig)

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPS")
    .anyRequest().authenticated()
)
```

### Camada 2 — Method Security (Defense-in-Depth)

`@EnableMethodSecurity` habilita `@PreAuthorize` e `@PostAuthorize` nos controllers e services.

#### Padrões de autorização por operação

| Operação                               | Anotação                                                                                                                              |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| Admin: criar/editar mídia              | `@PreAuthorize("hasRole('ADMIN')")`                                                                                                   |
| Admin: listar / deletar usuários       | `@PreAuthorize("hasRole('ADMIN')")`                                                                                                   |
| Usuário: atualizar próprio perfil      | `@PreAuthorize("hasRole('ADMIN') or @springSecurityCurrentUserProvider.getCurrentUser().map(u -> u.id().equals(#id)).orElse(false)")` |
| Usuário: ver próprio perfil            | `@PostAuthorize("hasRole('ADMIN') or returnObject.body.email == authentication.name")`                                                |
| Usuário: listar próprias watch entries | `@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")`                                                     |
| DLQ Admin                              | `@PreAuthorize("hasAnyRole('ADMIN', 'OPS')")` (classe inteira)                                                                        |

#### Exemplo de aplicação no código

```java
// AdminMediaController — toda a classe requer ADMIN
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController { ... }

// UserController — granular por método
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<PageResponse<UserResponse>> list(...) { ... }

@PostAuthorize("hasRole('ADMIN') or returnObject.body.email == authentication.name")
public ResponseEntity<UserResponse> getById(@PathVariable Long id) { ... }
```

### Armazenamento de roles

`UserEntity.role` é um campo `String` — ex: `"USER"`, `"ADMIN"`, `"OPS"`.
`CinelogUserDetails.getAuthorities()` retorna `ROLE_<role>` (prefixo obrigatório do Spring Security).

```java
// CinelogUserDetails
public Collection<? extends GrantedAuthority> getAuthorities() {
    String role = user.getRole() != null ? user.getRole() : "USER";
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
}
```

### Auditoria

Operações privilegiadas são anotadas com `@AuditableAction` + `@SecureOperation`,
gerando logs estruturados com `module`, `action` e `userId` do executor.

## Alternativas Consideradas

| Alternativa        | Rejeitado por                                              |
| ------------------ | ---------------------------------------------------------- |
| ACL (por objeto)   | Complexidade de infraestrutura alta; não necessário agora  |
| Attribute-Based AC | ABAC é mais poderoso, mas desnecessário neste domínio      |
| Apenas URL-based   | Não protege contra acesso direto a controllers por injeção |

## Consequências

### Positivas

- Dois pontos de controle independentes: URL filter + method annotation
- `@PostAuthorize` garante que a resposta não expose dados de outro usuário, mesmo que o service execute
- Alinhado a OWASP A01:2021 Broken Access Control

### Negativas / Trade-offs

- `@PostAuthorize` executa a lógica antes de verificar a autorização (o UseCase já roda); mitigado pelo fato de a resposta ser bloqueada antes de retornar ao cliente
- SpEL expressions longas são difíceis de testar sem contexto de segurança real

## Referências

- [ADR-011: Estratégia de Autenticação](./ADR-011-authentication-strategy.md)
- OWASP A01:2021 — Broken Access Control
- Spring Security Reference — Method Security
- `SecurityConfig.java`, `AdminMediaController.java`, `UserController.java`, `WatchEntryController.java`
