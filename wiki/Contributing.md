# 🤝 Contributing

> Guia de contribuição para o projeto CineLog.

---

## Como Contribuir

1. **Fork** o repositório
2. Crie uma **branch** a partir de `master`
3. Faça suas alterações seguindo as convenções
4. Abra um **Pull Request**

---

## Pré-requisitos

| Ferramenta | Versão |
| ---------- | ------ |
| Java       | 21+    |
| Maven      | 3.9+   |
| Docker     | 24+    |
| Git        | 2.x    |

---

## Setup Local

```bash
# Clone o fork
git clone https://github.com/SEU_USER/cinelog.git
cd cinelog

# Suba as dependências
docker compose up -d db redis kafka

# Rode os testes
./mvnw clean verify

# Inicie a aplicação
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Branch Naming

```
<tipo>/<descricao-curta>
```

| Tipo        | Uso                 | Exemplo                            |
| ----------- | ------------------- | ---------------------------------- |
| `feature/`  | Nova funcionalidade | `feature/add-watchlist-export`     |
| `fix/`      | Correção de bug     | `fix/jwt-token-expiration`         |
| `docs/`     | Documentação        | `docs/update-api-guide`            |
| `refactor/` | Refatoração         | `refactor/media-service-split`     |
| `test/`     | Testes              | `test/add-integration-tests-media` |
| `chore/`    | Manutenção          | `chore/update-dependencies`        |

---

## Conventional Commits

Seguimos a especificação [Conventional Commits](https://www.conventionalcommits.org/):

```
<tipo>(<escopo>): <descrição>

[corpo opcional]

[rodapé opcional]
```

### Tipos

| Tipo       | Quando usar                        |
| ---------- | ---------------------------------- |
| `feat`     | Nova feature                       |
| `fix`      | Correção de bug                    |
| `docs`     | Documentação                       |
| `style`    | Formatação (sem mudança de lógica) |
| `refactor` | Refatoração                        |
| `test`     | Adição/correção de testes          |
| `chore`    | Tarefas de build/dependências      |
| `perf`     | Melhorias de performance           |
| `ci`       | Configuração de CI/CD              |

### Exemplos

```
feat(media): adicionar endpoint de busca por gênero

fix(auth): corrigir expiração de refresh token

docs(wiki): adicionar página de Design Patterns

test(watchentry): adicionar testes de transição de estado

refactor(outbox): extrair OutboxScheduler para classe própria
```

---

## Pull Request

### Checklist

Antes de abrir o PR, verifique:

- [ ] Código compila sem erros (`./mvnw compile`)
- [ ] Todos os testes passam (`./mvnw test`)
- [ ] Cobertura ≥ 80% (`./mvnw verify`)
- [ ] Checkstyle sem violações
- [ ] PMD sem issues críticas
- [ ] Testes novos para código novo
- [ ] Documentação atualizada (se aplicável)

### Template de PR

```markdown
## Descrição

Breve descrição do que foi feito.

## Tipo de Mudança

- [ ] Bug fix
- [ ] Nova feature
- [ ] Breaking change
- [ ] Documentação

## Como Testar

1. Passo 1
2. Passo 2
3. Verificar resultado

## Screenshots (se aplicável)

## Checklist

- [ ] Testes passando
- [ ] Cobertura mantida
- [ ] Sem warnings de compilação
```

---

## Code Review

### O que revisar

| Aspecto            | O que verificar                        |
| ------------------ | -------------------------------------- |
| **Funcionalidade** | Código faz o que propõe?               |
| **Testes**         | Cenários cobertos? Edge cases?         |
| **Arquitetura**    | Segue hexagonal? Ports & Adapters?     |
| **Segurança**      | Input validation? Sem dados sensíveis? |
| **Performance**    | N+1 queries? Cache adequado?           |
| **Naming**         | Nomes claros e consistentes?           |

### Convenções no Review

- ✅ Aprovar quando estiver OK
- 💬 Comentar para sugestões opcionais
- 🔄 Solicitar mudanças para issues bloqueantes
- **Não** bloquear por estilo se Checkstyle/PMD não flagrou

---

## Quality Tools

O projeto usa análise estática integrada ao build:

| Ferramenta                 | Arquivo de Config             | Objetivo             |
| -------------------------- | ----------------------------- | -------------------- |
| **Checkstyle**             | `config/checkstyle.xml`       | Estilo de código     |
| **PMD**                    | `config/pmd-rules.xml`        | Bugs e más práticas  |
| **SpotBugs**               | `config/spotbugs-exclude.xml` | Bugs potenciais      |
| **JaCoCo**                 | pom.xml (plugin)              | Cobertura de testes  |
| **OWASP Dependency-Check** | pom.xml (plugin)              | CVEs em dependências |

Rodar todas as verificações:

```bash
./mvnw clean verify
```

---

## Estrutura de Pacotes

Ao adicionar código, siga a estrutura hexagonal:

```
com.cine.cinelog.{bounded-context}/
├── core/
│   ├── domain/        ← Entidades, Value Objects
│   ├── application/
│   │   ├── port/in/   ← Use Cases (interfaces)
│   │   ├── port/out/  ← Repository ports
│   │   └── service/   ← Implementações dos use cases
│   └── exception/     ← Exceções de domínio
├── infrastructure/
│   ├── adapter/out/   ← Repositórios JPA, clientes HTTP
│   ├── config/        ← Beans de configuração
│   └── mapper/        ← MapStruct mappers
└── web/
    ├── controller/    ← REST controllers
    ├── dto/           ← Request/Response DTOs
    └── mapper/        ← Web mappers
```

---

## Referências

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Guia de Getting Started](Getting-Started)
- [Arquitetura](Architecture)
