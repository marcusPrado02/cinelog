# ADR-010: Monorepo para Organização do Projeto

## Status

✅ **Aceito**

## Data

2025-12-01

## Contexto

Ao iniciar o CineLog, precisávamos decidir sobre a organização do código-fonte:

1. **Estrutura de repositórios** - Um repo ou múltiplos?
2. **Organização de código** - Como estruturar módulos?
3. **Compartilhamento de código** - Reutilização entre componentes
4. **Versionamento** - Como versionar componentes?
5. **CI/CD** - Pipeline de build e deploy
6. **Evolução futura** - Preparar para possível migração a microservices

### Problema

Precisávamos escolher entre:

**Monorepo (Single Repository):**

- Tudo em um repositório
- Backend, configs, docs, scripts
- Versionamento unificado

**Multi-repo (Multiple Repositories):**

- Repositórios separados por componente
- cinelog-api, cinelog-events, cinelog-docs
- Versionamento independente

**Poly-repo:**

- Repositórios separados por feature
- Múltiplos repos, sem compartilhamento

## Decisão

Adotamos **Monorepo** para organizar todo o código do CineLog em um único repositório.

### Estrutura

```
cinelog/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cine/cinelog/
│   │   │       ├── core/              # Domain + Application
│   │   │       ├── features/          # Adapters por feature
│   │   │       └── shared/            # Cross-cutting
│   │   └── resources/
│   │       ├── db/                    # Liquibase migrations
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   │
│   └── test/                          # Testes (mesmo espelho)
│       ├── java/
│       └── resources/
│
├── docs/                              # Documentação
│   ├── INDEX.md
│   ├── GETTING_STARTED.md
│   ├── adr/                           # Architecture Decision Records
│   ├── api/                           # API documentation
│   └── architecture/                  # Diagramas e design
│
├── scripts/                           # Scripts de automação
│   ├── setup.sh
│   ├── deploy.sh
│   └── fix-tests.py
│
├── docker/                            # Docker configs
│   ├── docker-compose.dev.yml
│   ├── docker-compose.observability.yml
│   └── mysql-init.sql
│
├── observability/                     # Observability configs
│   ├── prometheus.yml
│   ├── grafana/
│   └── otel-collector-config.yaml
│
├── performance/                       # Performance tests
│   └── k6/
│
├── config/                            # Code quality configs
│   ├── checkstyle.xml
│   ├── pmd-rules.xml
│   └── spotbugs-exclude.xml
│
├── pom.xml                            # Maven config
├── Dockerfile
├── docker-compose.yml
├── README.md
└── .gitignore
```

### Princípios

1. **Single Source of Truth** - Tudo no mesmo repo
2. **Estrutura Clara** - Pastas bem definidas
3. **Versionamento Unificado** - Uma versão para tudo
4. **Build Único** - Um pipeline de CI/CD
5. **Fácil Navegação** - Developer experience otimizada

## Alternativas Consideradas

### 1. Multi-repo (Repositórios Separados)

**Estrutura hipotética:**

```
cinelog-api/          # Backend API
cinelog-events/       # Event schemas
cinelog-docs/         # Documentação
cinelog-infra/        # Terraform/K8s configs
cinelog-scripts/      # Scripts
```

**Prós:**

- Ownership claro por equipe
- Versionamento independente
- Deploy independente
- Repos menores

**Contras:**

- **Compartilhamento difícil** - Código duplicado
- **Múltiplos PRs** - Mudanças cross-repo complexas
- **Versionamento complexo** - Compatibilidade entre repos
- **CI/CD multiplicado** - Pipeline por repo
- **Onboarding difícil** - Novos devs precisam clonar múltiplos repos

**Por que não escolhemos:** Overhead operacional muito alto para um único time.

### 2. Poly-repo (Repositórios por Feature)

**Estrutura hipotética:**

```
cinelog-media/        # Feature de media
cinelog-users/        # Feature de users
cinelog-watchlist/    # Feature de watchlist
cinelog-core/         # Core compartilhado
```

**Prós:**

- Máximo isolamento
- Deploy ultra-independente
- Escala para times grandes

**Contras:**

- **Extrema fragmentação**
- **Dependency hell**
- **Impossível de manter** com time pequeno
- **Versionamento caótico**

**Por que não escolhemos:** Over-engineering extremo para nosso caso.

### 3. Monolith sem Estrutura

**Estrutura hipotética:**

```
src/
└── main/
    └── java/
        └── com/example/
            ├── MediaController.java
            ├── UserController.java
            ├── MediaRepository.java
            ├── MediaService.java
            └── ... (tudo misturado)
```

**Prós:**

- Simples inicialmente
- Zero overhead

**Contras:**

- **Caótico** conforme cresce
- **Difícil navegar**
- **Sem separação de conceitos**
- **Manutenção impossível**

**Por que não escolhemos:** Não escala nem para curto prazo.

### 4. Microservices Prematuros

**Estrutura hipotética:**

```
media-service/        # Microservice de media
user-service/         # Microservice de users
watchlist-service/    # Microservice de watchlist
api-gateway/          # Gateway
```

**Prós:**

- Escalabilidade independente
- Deploy independente
- Tecnologias diferentes

**Contras:**

- **Over-engineering** inicial
- **Complexidade operacional** (Kubernetes, service mesh)
- **Network overhead**
- **Debugging difícil**
- **Custos elevados**

**Por que não escolhemos:** Desnecessário para início do projeto (YAGNI).

## Consequências

### Positivas ✅

1. **Simplicidade Operacional**
    - Um repositório para clonar
    - Um pipeline de CI/CD
    - Um build
    - Uma versão

2. **Compartilhamento Fácil**

    ```java
    // Módulos compartilhados no mesmo repo
    features/media/  → usa → core/domain/
    features/users/  → usa → core/domain/
    features/watchlist/ → usa → core/domain/
    ```

3. **Refactoring Atômico**

    ```bash
    # Um PR pode mudar múltiplos módulos
    git commit -m "Refactor: Move Media to core domain"
    # features/media/Media.java → core/domain/Media.java
    # Todos os imports atualizados no mesmo commit
    ```

4. **Versionamento Simples**

    ```xml
    <version>1.0.0</version>  # Uma versão para tudo
    ```

5. **Onboarding Rápido**

    ```bash
    git clone https://github.com/maps/cinelog.git
    cd cinelog
    ./mvnw clean install
    # Tudo funcionando!
    ```

6. **Histórico Completo**
    - Todo histórico no Git
    - Fácil rastrear mudanças
    - Blame funciona cross-módulos

7. **Testes Integrados**

    ```java
    @SpringBootTest  // Testa toda aplicação
    class IntegrationTest {
        // Testa features + core + persistence juntos
    }
    ```

8. **Deploy Unificado**

    ```bash
    mvn clean package
    docker build -t cinelog:1.0.0 .
    docker push cinelog:1.0.0
    # Um artefato, um deploy
    ```

9. **Documentação Centralizada**

    ```
    docs/
    ├── INDEX.md              # Um lugar para toda doc
    ├── GETTING_STARTED.md
    ├── adr/
    └── architecture/
    ```

10. **Code Search Eficiente**
    ```bash
    # Buscar em todo codebase
    git grep "Media"
    # Encontra em core, features, tests
    ```

### Negativas ❌

1. **Build Monolítico**
    - Mudar 1 linha = rebuild tudo
    - CI pode demorar (5-10 min)
    - Mitigado com cache de build

2. **Não Escala para Times Grandes**
    - 50+ devs = conflitos de merge
    - Precisa disciplina em PRs
    - Pode evoluir para multi-repo futuramente

3. **Deploy Acoplado**
    - Deploy de tudo junto
    - Não dá para deployar só uma feature
    - Rollback é all-or-nothing

4. **Repo Grande**
    - Clone inicial mais demorado
    - Git operations mais lentas
    - Mitigado com shallow clone

5. **Ownership Menos Claro**
    - Quem é dono de cada módulo?
    - CODEOWNERS ajuda
    - Requer documentação

### Trade-offs Aceitáveis

| Trade-off                        | Justificativa                      |
| -------------------------------- | ---------------------------------- |
| Build monolítico → Simplicidade  | Time pequeno, build cache mitiga   |
| Deploy acoplado → Menos overhead | Não precisamos deploy independente |
| Repo grande → Histórico completo | Vale a pena para rastreabilidade   |

## Implementação

### CODEOWNERS

**.github/CODEOWNERS:**

```
# Global owners
* @maps

# Core domain
/src/main/java/com/cine/cinelog/core/ @maps @tech-lead

# Features
/src/main/java/com/cine/cinelog/features/media/ @maps
/src/main/java/com/cine/cinelog/features/users/ @maps

# Infrastructure
/docker/ @maps @devops
/observability/ @maps @devops

# Docs
/docs/ @maps @docs-team
```

### CI/CD Pipeline

**.github/workflows/ci.yml:**

```yaml
name: CI

on:
    push:
        branches: [main, develop]
    pull_request:
        branches: [main, develop]

jobs:
    build:
        runs-on: ubuntu-latest

        steps:
            - uses: actions/checkout@v4

            - name: Set up JDK 21
              uses: actions/setup-java@v4
              with:
                  java-version: "21"
                  distribution: "temurin"
                  cache: maven

            - name: Build
              run: mvn clean verify

            - name: Upload coverage
              uses: codecov/codecov-action@v3
```

### Versionamento

**pom.xml:**

```xml
<groupId>com.cine</groupId>
<artifactId>cinelog</artifactId>
<version>1.0.0</version>
<packaging>jar</packaging>
```

**Semantic Versioning:**

- **1.0.0** - Major version
- **1.1.0** - Minor version (new features)
- **1.1.1** - Patch version (bug fixes)

### Estrutura de Branches

```
main          # Produção (estável)
  ↑
develop       # Development (integração)
  ↑
feature/      # Features (branches temporárias)
  ├── feature/add-genres
  ├── feature/watchlist
  └── feature/recommendations
```

## Evolução Futura

### Path to Microservices (se necessário)

```
Fase 1: Monorepo Modular (ATUAL)
cinelog/
├── features/media/
├── features/users/
└── features/watchlist/

Fase 2: Mono-repo com Módulos Maven
cinelog/
├── media-module/pom.xml
├── users-module/pom.xml
└── watchlist-module/pom.xml

Fase 3: Multi-repo (SE TIME CRESCER)
media-service/        # Repo separado
users-service/        # Repo separado
watchlist-service/    # Repo separado
```

**Critérios para Split:**

- Time > 20 devs
- Deploy independente necessário
- Tecnologias diferentes por módulo
- Escalabilidade independente necessária

## Boas Práticas

### 1. Estrutura Clara

✅ **Bom:**

```
src/main/java/com/cine/cinelog/
├── core/              # Core domain (sem deps)
├── features/          # Features por bounded context
└── shared/            # Cross-cutting concerns
```

### 2. README por Módulo

```
features/media/README.md
features/users/README.md
features/watchlist/README.md
```

### 3. Testes Espelhados

```
src/main/java/features/media/MediaService.java
src/test/java/features/media/MediaServiceTest.java
```

### 4. Documentação Centralizada

```
docs/
├── INDEX.md           # Índice principal
├── adr/               # Decisões arquiteturais
└── features/
    ├── media.md
    └── users.md
```

## Validação

### Métricas de Sucesso

✅ **Onboarding**: < 30 minutos (clone → build → run)  
✅ **CI/CD**: < 10 minutos por build  
✅ **Developer Experience**: 4.5/5 (survey)  
✅ **Zero problemas** de versionamento  
✅ **Zero problemas** de compartilhamento de código

### Lições Aprendidas

1. **Monorepo funciona bem** para times pequenos/médios
2. **Estrutura clara é essencial** - Evitar misturar tudo
3. **Cache de build importante** - Reduz tempo de CI
4. **CODEOWNERS ajuda** - Clarifica ownership
5. **Pode evoluir** - Monorepo → Multi-repo se necessário

## Ferramentas

### Build Cache

```xml
<!-- Maven -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <reuseForks>true</reuseForks>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Shallow Clone

```bash
# Clone apenas último commit (mais rápido)
git clone --depth 1 https://github.com/maps/cinelog.git
```

### Sparse Checkout (se repo ficar muito grande)

```bash
# Clone apenas parte do repo
git clone --filter=blob:none --sparse https://github.com/maps/cinelog.git
cd cinelog
git sparse-checkout set src/main/java
```

## Comparação

| Aspecto          | Monorepo ✅ | Multi-repo ❌ |
| ---------------- | ----------- | ------------- |
| Clones           | 1           | 3-5           |
| Pipelines CI/CD  | 1           | 3-5           |
| Versionamento    | Simples     | Complexo      |
| Compartilhamento | Fácil       | Difícil       |
| Refactoring      | Atômico     | Múltiplos PRs |
| Onboarding       | Rápido      | Lento         |
| Deploy           | Acoplado    | Independente  |
| Escala p/ times  | 5-20 devs   | 20+ devs      |

## Referências

- [Monorepo - Google Engineering](https://research.google/pubs/pub45424/)
- [Why Google Stores Billions of Lines of Code in a Single Repository](https://cacm.acm.org/magazines/2016/7/204032-why-google-stores-billions-of-lines-of-code-in-a-single-repository/fulltext)
- [Monorepo vs Multi-repo](https://www.thoughtworks.com/insights/blog/monorepo-vs-multirepo)
- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)

## Revisões

- **2025-12-01**: Decisão inicial aceita
- **2026-01-15**: Validado - funcionando perfeitamente para time atual
- **Próxima revisão**: Reavaliar se time crescer para 20+ devs

---

**Mantido por:** Time CineLog  
**Próxima revisão:** Julho 2026 ou quando time crescer significativamente
