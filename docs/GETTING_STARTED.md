# 🚀 Guia de Início Rápido - CineLog

Este guia ajudará você a configurar e executar o projeto CineLog em seu ambiente local.

## 📋 Pré-requisitos

Antes de começar, certifique-se de ter as seguintes ferramentas instaladas:

### Obrigatório

- **Java 21** (JDK) - [Download](https://adoptium.net/)
- **Maven 3.9+** (ou use o Maven Wrapper incluído)
- **Docker** e **Docker Compose** - [Download](https://docs.docker.com/get-docker/)
- **Git** - [Download](https://git-scm.com/)

### Opcional

- **IDE**: IntelliJ IDEA, Eclipse ou VS Code
- **Postman** ou **Insomnia** para testar APIs
- **MySQL Workbench** para gerenciar o banco de dados

## 🔧 Configuração do Ambiente

### 1. Clone o Repositório

```bash
git clone https://github.com/marcusPrado02/cinelog.git
cd cinelog
```

### 2. Configure as Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto (opcional, mas recomendado):

```env
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=cinelog
DB_USER=cinelog
DB_PASSWORD=cinelog

# TMDB API (opcional - para integração com The Movie Database)
TMDB_API_KEY=your_api_key_here

# JWT Security
JWT_SECRET=MINHA_CHAVE_MEGA_SECRETA_DE_NO_MINIMO_32_CHARS
JWT_EXPIRATION=3600

# Application
SPRING_PROFILES_ACTIVE=dev
```

### 3. Inicie os Serviços de Infraestrutura

O projeto utiliza Docker Compose para gerenciar as dependências:

```bash
# Inicia MySQL, Redis e outros serviços
docker-compose up -d

# Verifica se os containers estão rodando
docker-compose ps
```

Os serviços disponíveis são:

- **MySQL**: porta 3306
- **Redis**: porta 6379
- **Grafana**: porta 3000 (observabilidade)
- **Prometheus**: porta 9090 (métricas)
- **Tempo**: porta 3200 (tracing)

### 4. Execute as Migrações do Banco de Dados

As migrações são executadas automaticamente pelo Liquibase quando a aplicação inicia, mas você pode executá-las manualmente:

```bash
./mvnw liquibase:update
```

Para reverter a última migração:

```bash
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

## 🏃 Executando a Aplicação

### Modo de Desenvolvimento

```bash
# Usando Maven Wrapper (recomendado)
./mvnw spring-boot:run

# Ou com perfil específico
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Modo de Produção

```bash
# Build do projeto
./mvnw clean package -DskipTests

# Execução do JAR
java -jar target/cinelog-0.0.1-SNAPSHOT.jar
```

### Usando Docker

```bash
# Build da imagem
docker build -t cinelog:latest .

# Execução do container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  --network cinelog_network \
  cinelog:latest
```

## 🧪 Executando os Testes

### Testes Unitários

```bash
./mvnw test
```

### Testes de Integração

```bash
./mvnw verify
```

### Testes com Cobertura (JaCoCo)

```bash
# Executa testes e gera relatório de cobertura
./mvnw clean verify

# Abre o relatório no navegador
open target/site/jacoco/index.html
```

## 📚 Acessando a Documentação da API

Após iniciar a aplicação, acesse:

### Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON

```
http://localhost:8080/v3/api-docs
```

### Actuator Endpoints

```
http://localhost:8080/actuator
http://localhost:8080/actuator/health
http://localhost:8080/actuator/metrics
http://localhost:8080/actuator/prometheus
```

## 🔍 Verificação da Instalação

Execute os seguintes comandos para verificar se tudo está funcionando:

```bash
# 1. Verifica a saúde da aplicação
curl http://localhost:8080/actuator/health

# 2. Lista todas as mídias (deve retornar array vazio inicialmente)
curl http://localhost:8080/api/v1/media

# 3. Verifica se o banco está acessível
docker exec -it cinelog-mysql mysql -ucinelog -pcinelog -e "SHOW DATABASES;"
```

## 🎯 Primeiros Passos

### 1. Criar um Usuário

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@example.com",
    "password": "senha123"
  }'
```

### 2. Fazer Login (se autenticação estiver habilitada)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "password": "senha123"
  }'
```

### 3. Criar uma Mídia

```bash
curl -X POST http://localhost:8080/api/v1/media \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "title": "Matrix",
    "type": "MOVIE",
    "releaseYear": 1999,
    "originalTitle": "The Matrix",
    "originalLanguage": "en",
    "overview": "Um programador descobre a verdade sobre a realidade"
  }'
```

## 🐛 Troubleshooting

### Porta 8080 já está em uso

```bash
# Identifique o processo
lsof -i :8080

# Mate o processo ou mude a porta da aplicação
./mvnw spring-boot:run -Dserver.port=8081
```

### MySQL não inicia

```bash
# Remove containers antigos e volumes
docker-compose down -v

# Reinicia os serviços
docker-compose up -d
```

### Erro de permissão no Maven Wrapper

```bash
chmod +x mvnw
```

### Problemas com Liquibase

```bash
# Limpa o banco e recria
docker-compose down -v
docker-compose up -d mysql
./mvnw liquibase:dropAll
./mvnw liquibase:update
```

## 📖 Próximos Passos

Agora que você tem o projeto rodando, explore:

1. **[Documentação da Arquitetura](./architecture/ARCHITECTURE.md)** - Entenda a estrutura do projeto
2. **[Guia de API](./api/API_GUIDE.md)** - Aprenda a usar os endpoints REST
3. **[Guia de Desenvolvimento](./DEVELOPMENT.md)** - Padrões de código e workflows
4. **[Design Patterns](./DESIGN_PATTERNS.md)** - Patterns GoF implementados
5. **[ADRs - Decisões de Arquitetura](./adr/)** - Contexto de decisões técnicas

## 💡 Dicas Úteis

### Perfis do Spring

- **`dev`** - Desenvolvimento local com logs detalhados e hot-reload
- **`test`** - Testes automatizados com H2 in-memory
- **`docker`** - Execução em containers
- **`perf`** - Testes de performance com métricas detalhadas
- **`prod`** - Produção com otimizações e segurança

### Atalhos do Maven

```bash
# Compilação rápida (sem testes)
./mvnw clean compile -DskipTests

# Build completo com qualidade
./mvnw clean verify

# Limpeza profunda (incluindo .m2)
./mvnw dependency:purge-local-repository

# Update de dependências
./mvnw versions:display-dependency-updates
```

### Comandos Docker Úteis

```bash
# Ver logs em tempo real
docker-compose logs -f app

# Reiniciar apenas um serviço
docker-compose restart mysql

# Executar comando no container
docker-compose exec mysql mysql -ucinelog -pcinelog

# Ver uso de recursos
docker stats

# Limpar tudo (cuidado!)
docker-compose down -v --remove-orphans
```

### Debug com IntelliJ IDEA

1. Crie uma configuração **Spring Boot**
2. Defina **VM options**: `-Dspring.profiles.active=dev -Xmx2g`
3. Habilite **"Update classes and resources"** para hot-reload
4. Use **Evaluate Expression** (Alt+F8) para debugar valores

### Ferramentas de Desenvolvimento

| Ferramenta    | URL                                         | Propósito                |
| ------------- | ------------------------------------------- | ------------------------ |
| Swagger UI    | http://localhost:8080/swagger-ui/index.html | Testar endpoints         |
| Actuator      | http://localhost:8080/actuator              | Métricas e health        |
| Grafana       | http://localhost:3000                       | Dashboards (admin/admin) |
| Prometheus    | http://localhost:9090                       | Métricas raw             |
| MySQL Adminer | http://localhost:8081                       | Admin do banco           |

## 🔥 Fluxo de Trabalho Recomendado

### 1. Primeira Vez no Projeto

```bash
# Clone e entre no diretório
git clone https://github.com/marcusPrado02/cinelog.git && cd cinelog

# Inicie infraestrutura
docker-compose up -d

# Aguarde MySQL inicializar (±30s)
docker-compose logs -f mysql | grep "ready for connections"

# Build e execute
./mvnw clean spring-boot:run

# Abra Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

### 2. Dia a Dia de Desenvolvimento

```bash
# Atualizar código
git pull origin master

# Reiniciar infraestrutura se necessário
docker-compose restart

# Executar com hot-reload
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"

# Executar testes após mudanças
./mvnw test -Dtest=ClasseTest
```

### 3. Antes de Commitar

```bash
# Executa todos os checks de qualidade
./mvnw clean verify

# Formata o código
./mvnw spotless:apply

# Verifica se há dependências desatualizadas
./mvnw versions:display-dependency-updates

# Commit seguindo padrão
git add .
git commit -m "feat(modulo): descrição da mudança"
```

## 🎓 Tutoriais Rápidos

### Criar uma Nova Feature Completa

```bash
# 1. Crie a entidade de domínio
# src/main/java/com/cine/cinelog/core/domain/model/NomeEntidade.java

# 2. Crie a interface do repository (port)
# src/main/java/com/cine/cinelog/core/application/ports/out/NomeRepositoryPort.java

# 3. Crie o use case
# src/main/java/com/cine/cinelog/core/application/usecase/NomeService.java

# 4. Implemente o adapter de persistência
# src/main/java/com/cine/cinelog/features/nome/persistence/NomeRepositoryAdapter.java

# 5. Crie o controller REST
# src/main/java/com/cine/cinelog/features/nome/web/controller/NomeController.java

# 6. Crie os testes
# src/test/java/com/cine/cinelog/features/nome/

# 7. Execute os testes
./mvnw test
```

### Adicionar uma Nova Migration Liquibase

```bash
# 1. Crie o changeset
# src/main/resources/db/changelog/changes/V00X__descricao.sql

# 2. Registre no master
# Adicione em db/changelog/db.changelog-master.yaml

# 3. Execute localmente
./mvnw liquibase:update

# 4. Verifique se aplicou
./mvnw liquibase:status

# 5. Para desfazer (se necessário)
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

### Adicionar um Novo Endpoint

```java
// 1. Defina o DTO de request
public record CreateMediaRequest(
    @NotBlank String title,
    @NotNull MediaType type,
    Integer releaseYear
) {}

// 2. Crie o controller method
@PostMapping
public ResponseEntity<MediaResponse> create(@Valid @RequestBody CreateMediaRequest request) {
    Media media = mediaService.create(request);
    return ResponseEntity.status(CREATED)
        .body(mediaMapper.toResponse(media));
}

// 3. Documente com OpenAPI
@Operation(summary = "Create a new media")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Media created"),
    @ApiResponse(responseCode = "400", description = "Invalid request")
})

// 4. Teste
@Test
void shouldCreateMedia() {
    given()
        .contentType(JSON)
        .body(request)
    .when()
        .post("/api/v1/media")
    .then()
        .statusCode(201);
}
```

## 🆘 Precisa de Ajuda?

### Problemas Comuns e Soluções

| Problema                | Solução                                          |
| ----------------------- | ------------------------------------------------ |
| Porta 8080 em uso       | `./mvnw spring-boot:run -Dserver.port=8081`      |
| MySQL não inicia        | `docker-compose down -v && docker-compose up -d` |
| Testes falhando         | `./mvnw clean test`                              |
| Build lento             | `./mvnw -T 1C clean install` (1 thread por core) |
| Hot-reload não funciona | Habilite devtools e rebuild o projeto            |
| Erro de memória         | Aumente heap: `export MAVEN_OPTS="-Xmx2g"`       |

### Canais de Suporte

- **GitHub Issues** - [Reportar bugs](https://github.com/marcusPrado02/cinelog/issues)
- **GitHub Discussions** - [Fazer perguntas](https://github.com/marcusPrado02/cinelog/discussions)
- **Documentação** - [Ver todos os guias](./INDEX.md)
- **FAQ** - [Perguntas frequentes](./FAQ.md)

### Recursos Adicionais

- 📚 [Arquitetura Completa](./architecture/ARCHITECTURE.md)
- 🎨 [Design Patterns](./DESIGN_PATTERNS.md)
- 🧪 [Guia de Testes](./TESTING.md)
- 🚀 [Deploy em Produção](./DEPLOYMENT.md)
- 📊 [Observabilidade](./OBSERVABILITY.md)
- 🔒 [Segurança](./SECURITY.md)

## 📚 Glossário

- **ADR** - Architecture Decision Record (decisões documentadas)
- **CQRS** - Command Query Responsibility Segregation
- **DDD** - Domain-Driven Design
- **DTO** - Data Transfer Object
- **JaCoCo** - Java Code Coverage
- **JWT** - JSON Web Token
- **Liquibase** - Ferramenta de migração de banco de dados
- **MapStruct** - Framework de mapeamento de objetos
- **Outbox Pattern** - Pattern para publicação confiável de eventos
- **Strategy Pattern** - Pattern para algoritmos intercambiáveis
- **Use Case** - Caso de uso (lógica de negócio)

---

**Última atualização**: Dezembro 2025
