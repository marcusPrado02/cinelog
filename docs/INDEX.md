# 📚 Documentação do CineLog - Índice Geral

Bem-vindo à documentação completa do CineLog! Esta é uma plataforma moderna de gerenciamento de mídias construída com Java 21 e Spring Boot 3.

---

## 🚀 Início Rápido

Novo no projeto? Comece por aqui:

1. **[Guia de Início Rápido](./GETTING_STARTED.md)** - Configure e execute o projeto em minutos
2. **[README Principal](../README.md)** - Visão geral do projeto e modelo de dados
3. **[Guia da API](./api/API_GUIDE.md)** - Como usar os endpoints REST
4. **[FAQ](./FAQ.md)** - Perguntas frequentes e troubleshooting

---

## 📖 Documentação Core (9 Guias Essenciais)

### 1️⃣ Para Desenvolvedores

| Documento                                      | Descrição                                           | Quando Usar               |
| ---------------------------------------------- | --------------------------------------------------- | ------------------------- |
| **[🚀 GETTING_STARTED](./GETTING_STARTED.md)** | Setup inicial, pré-requisitos e primeiros passos    | Primeiro dia no projeto   |
| **[🔧 DEVELOPMENT](./DEVELOPMENT.md)**         | Padrões de código, estrutura do projeto e workflows | Durante o desenvolvimento |
| **[🧪 TESTING](./TESTING.md)**                 | Estratégia de testes, cobertura e boas práticas     | Ao escrever testes        |
| **[🎨 DESIGN_PATTERNS](./DESIGN_PATTERNS.md)** | Strategy, State e Template Method patterns          | Arquitetura e design      |

### 2️⃣ Para Operações

| Documento                                  | Descrição                                 | Quando Usar               |
| ------------------------------------------ | ----------------------------------------- | ------------------------- |
| **[🚀 DEPLOYMENT](./DEPLOYMENT.md)**       | Build, Docker, CI/CD e deploy em cloud    | Deploy e produção         |
| **[📊 OBSERVABILITY](./OBSERVABILITY.md)** | Logs, métricas, tracing e dashboards      | Monitoramento e debugging |
| **[🔒 SECURITY](./SECURITY.md)**           | Autenticação, autorização e boas práticas | Segurança e auditoria     |

### 3️⃣ Referência e Suporte

| Documento                          | Descrição                              | Quando Usar             |
| ---------------------------------- | -------------------------------------- | ----------------------- |
| **[❓ FAQ](./FAQ.md)**             | Perguntas frequentes e troubleshooting | Problemas e dúvidas     |
| **[📝 CHANGELOG](./CHANGELOG.md)** | Histórico de versões e mudanças        | Ver evolução do projeto |

### 4️⃣ Batch Jobs e SCDF

| Documento                                                       | Descrição                                             |
| --------------------------------------------------------------- | ----------------------------------------------------- |
| **[SCDF Guide](./SCDF-GUIDE.md)**                              | Guia completo do Spring Cloud Data Flow               |
| **[SCDF Implementation](./SCDF-IMPLEMENTATION.md)**            | Detalhes tecnicos da implementacao SCDF               |
| **[SCDF Dashboard Guide](./SCDF-DASHBOARD-GUIDE.md)**         | Passo a passo para executar batch jobs via Dashboard  |
| **[Batch Performance](./BATCH-PERFORMANCE.md)**                | Metricas, SLOs e tuning dos 12 batch jobs             |
| **[SLI Definitions](./SLI-DEFINITIONS.md)**                    | SLIs, SLOs e regras de alerta para batch e API        |

### 5️⃣ Recursos Adicionais

| Recurso                                                       | Descrição                            |
| ------------------------------------------------------------- | ------------------------------------ |
| **[API REST](./api/)**                                        | Documentacao detalhada dos endpoints          |
| **[Arquitetura](./architecture/)**                            | Decisoes arquiteturais e ADRs                 |
| **[Eventos](./events/)**                                      | Documentacao do sistema de eventos            |
| **[Swagger UI](http://localhost:8080/swagger-ui/index.html)** | Documentacao interativa (dev)                 |
| **[OpenAPI Spec](http://localhost:8080/v3/api-docs)**         | Especificacao OpenAPI 3.0                     |

---

## 📂 Estrutura da Documentação

```
docs/
├── INDEX.md                    # 👈 Você está aqui (Navegação central)
│
├── 🚀 Guias Essenciais (9 arquivos)
│   ├── GETTING_STARTED.md      # Setup e configuração inicial
│   ├── DEVELOPMENT.md          # Guia de desenvolvimento
│   ├── TESTING.md              # Estratégia de testes
│   ├── DESIGN_PATTERNS.md      # Patterns implementados
│   ├── DEPLOYMENT.md           # Deploy e CI/CD
│   ├── OBSERVABILITY.md        # Logs, métricas e tracing
│   ├── SECURITY.md             # Segurança e autenticação
│   ├── FAQ.md                  # Perguntas frequentes
│   └── CHANGELOG.md            # Histórico de versões
│
├── 📦 Batch Jobs e SCDF
│   ├── SCDF-GUIDE.md               # Guia completo do SCDF
│   ├── SCDF-IMPLEMENTATION.md      # Detalhes tecnicos da implementacao
│   ├── SCDF-DASHBOARD-GUIDE.md     # Guia passo a passo do Dashboard
│   ├── BATCH-PERFORMANCE.md        # Metricas e SLOs dos batch jobs
│   └── SLI-DEFINITIONS.md          # SLIs e alertas
│
└── 📁 Recursos Especializados
    ├── api/                    # Documentação da API REST
    ├── architecture/           # Decisões arquiteturais (ADRs)
    ├── adr/                    # Architecture Decision Records
    └── events/                 # Sistema de eventos
```

---

## 🎓 Conceitos-Chave do Projeto

### Arquitetura

- **Clean Architecture + Hexagonal** - Separação clara entre domínio, aplicação e infraestrutura
- **Domain-Driven Design (DDD)** - Organizado por contextos delimitados (Media, User, Content)
- **Event-Driven Architecture** - Kafka com EventEnvelope, Outbox Pattern e rastreamento distribuído

📖 Ver detalhes em: [ARCHITECTURE](./architecture/ARCHITECTURE.md)

### Padrões de Design

- **Strategy Pattern** - Sistema de recomendações (Content-Based, Collaborative, Hybrid)
- **State Pattern** - Gerenciamento de estados do WatchEntry
- **Template Method** - Validação específica por tipo de mídia
- **CQRS** - Separação de read/write models (User Insights, Media Popularity)
- **Outbox Pattern** - Publicação confiável de eventos com retry exponencial
- **Inbox Pattern** - Processamento idempotente de eventos Kafka

📖 Ver detalhes em: [DESIGN_PATTERNS](./DESIGN_PATTERNS.md)

### Observabilidade (3 Pilares)

1. **Logs** - JSON estruturado com MDC e correlationId
2. **Métricas** - Prometheus + Micrometer com dashboards Grafana
3. **Tracing** - OpenTelemetry com propagação end-to-end

📖 Ver detalhes em: [OBSERVABILITY](./OBSERVABILITY.md)

---

## 🚀 Guias por Cenário

### 🔨 Quero desenvolver uma nova feature

1. Leia o **[Guia de Desenvolvimento](./DEVELOPMENT.md)**
2. Clone o repositório: `git clone https://github.com/marcusPrado02/cinelog.git`
3. Siga o workflow: Feature branch → Testes → PR
4. Consulte os **[Design Patterns](./DESIGN_PATTERNS.md)** para decisões arquiteturais

### 🚀 Quero fazer deploy

1. Leia o **[Guia de Deployment](./DEPLOYMENT.md)**
2. Build: `./mvnw clean package -DskipTests`
3. Docker: `docker build -t cinelog:latest .`
4. Configure variáveis de ambiente (DB, Kafka, Redis)
5. Execute: `docker-compose up -d`

### 🔍 Quero investigar um problema

1. Acesse **[Actuator](http://localhost:8080/actuator/health)** para status da aplicação
2. Verifique **logs** em `logs/application.log` (JSON estruturado)
3. Consulte **métricas** no [Grafana](http://localhost:3000)
4. Analise **tracing** no [Tempo](http://localhost:3200)
5. Use **correlationId** para rastreamento end-to-end

📖 Ver: [OBSERVABILITY](./OBSERVABILITY.md)

### 🔌 Quero integrar com a API

1. Leia o **[Guia da API](./api/API_GUIDE.md)**
2. Teste endpoints no **[Swagger UI](http://localhost:8080/swagger-ui/index.html)**
3. Implemente autenticação JWT (consulte [SECURITY](./SECURITY.md))
4. Trate erros seguindo padrão Problem Details (RFC 7807)

---

## 🔧 Stack Tecnológica

| Categoria           | Tecnologia          | Versão | Propósito                                  |
| ------------------- | ------------------- | ------ | ------------------------------------------ |
| **Linguagem**       | Java                | 21     | Virtual Threads, Records, Pattern Matching |
| **Framework**       | Spring Boot         | 3.5.7  | Backend framework                          |
| **Persistência**    | MySQL               | 8.0    | Banco de dados relacional                  |
| **Migrações**       | Liquibase           | 5.0.1  | Versionamento do schema                    |
| **Cache**           | Redis               | 7      | Cache distribuído                          |
| **Mensageria**      | Kafka               | 3.9    | Event streaming                            |
| **Observabilidade** | Prometheus/Grafana  | -      | Métricas e dashboards                      |
| **Tracing**         | OpenTelemetry/Tempo | -      | Rastreamento distribuído                   |
| **Documentação**    | OpenAPI/Swagger     | 3.0    | Documentação interativa                    |

---

## 📊 Features Principais

### 🎯 PR6 - Features de Negócio (ATUAL)

**5 novas features prontas para produção:**

- 📊 **User Insights** - Estatísticas agregadas do usuário (CQRS + Kafka)
- 🔥 **Media Popularity** - Rankings trending e top-rated (CQRS + Kafka)
- 🔍 **Media Search** - Busca avançada com filtros dinâmicos (Specification Pattern)
- 📺 **Watch Progress** - Rastreamento de progresso em séries (Value Object)
- 🎯 **Recommendations** - Sistema de recomendações personalizadas (Strategy Pattern)

**Detalhes completos:** Ver [CHANGELOG](./CHANGELOG.md#pr6-features-de-negócio)

---

## 🤝 Contribuindo

Quer contribuir? Veja o [guia de contribuição](../CONTRIBUTING.md) para:

- Código de conduta e valores do projeto
- Processo de Pull Request e code review
- Padrões de código e convenções
- Como reportar bugs efetivamente
- Como sugerir e discutir features

**Quick Start para Contribuidores:**

```bash
# 1. Fork e clone
git clone https://github.com/SEU_USER/cinelog.git
cd cinelog

# 2. Crie uma branch
git checkout -b feature/minha-feature

# 3. Desenvolva e teste
./mvnw clean verify

# 4. Commit seguindo convenção
git commit -m "feat(modulo): adiciona funcionalidade X"

# 5. Push e abra PR
git push origin feature/minha-feature
```

---

## 📞 Suporte e Comunidade

### Canais Oficiais

- 📖 **Documentação**: Você está aqui! Explore [todos os guias](#-documentação-core-9-guias-essenciais)
- ❓ **FAQ**: [Perguntas frequentes e troubleshooting](./FAQ.md)
- 🐛 **Issues**: [Reportar bugs e problemas](https://github.com/marcusPrado02/cinelog/issues)
- 💬 **Discussions**: [Perguntas, ideias e feedback](https://github.com/marcusPrado02/cinelog/discussions)
- 📧 **Email**: contato@cinelog.com
- 💼 **LinkedIn**: [CineLog Project](https://linkedin.com/company/cinelog)

### Encontrou um Problema?

1. **Verifique o [FAQ](./FAQ.md)** - Maioria dos problemas já foram resolvidos
2. **Busque nas [Issues](https://github.com/marcusPrado02/cinelog/issues)** - Pode já estar reportado
3. **Abra uma nova Issue** com:
    - Descrição clara do problema
    - Passos para reproduzir
    - Comportamento esperado vs atual
    - Logs relevantes
    - Ambiente (OS, Java version, Docker version)

### Quer Sugerir uma Feature?

1. **Verifique o [Roadmap](../ROADMAP.md)** - Pode já estar planejado
2. **Abra uma [Discussion](https://github.com/marcusPrado02/cinelog/discussions)** - Discuta a ideia
3. **Crie uma RFC** (Request for Comments) se for grande
4. **Implemente e abra PR** seguindo o [guia de contribuição](../CONTRIBUTING.md)

---

## 🎯 Roadmap e Futuro

### Próximas Features (Q1 2026)

- ✅ **CQRS completo** - Read/Write models separados
- ✅ **Design Patterns** - Strategy, State, Template Method
- ⏳ **Autenticação OAuth** - Google, GitHub, Facebook
- ⏳ **API GraphQL** - Alternativa ao REST
- ⏳ **Notificações** - Email e push notifications
- ⏳ **Upload de imagens** - S3/CloudFront integration

### Em Avaliação (Q2 2026)

- 🤔 **Recomendações ML** - Machine Learning based
- 🤔 **Multi-idioma** - i18n support
- 🤔 **WebSocket** - Real-time updates
- 🤔 **Exportação de dados** - CSV, JSON, Excel
- 🤔 **API pública** - Third-party integrations

Veja o [roadmap completo](../ROADMAP.md) para mais detalhes.

---

## 📊 Estatísticas do Projeto

**Status Atual (Janeiro 2026):**

| Métrica             | Valor                                       |
| ------------------- | ------------------------------------------- |
| Versão              | 0.7.0 (PR6 completo)                        |
| Cobertura de Testes | 82%                                         |
| Endpoints REST      | 35+                                         |
| Design Patterns     | 5 (CQRS, Strategy, State, Template, Outbox) |
| Migrations          | 10 changesets                               |
| Linhas de Código    | ~15,000                                     |
| Batch Jobs          | 12 (9 TMDB + 3 Email)                       |
| Documentação        | 12 guias + ADRs                             |
| Contributors        | 3 ativos                                    |

**Performance Benchmarks:**

| Endpoint                     | p50  | p95  | p99  | RPS   |
| ---------------------------- | ---- | ---- | ---- | ----- |
| GET /api/v1/media            | 15ms | 30ms | 50ms | 2000+ |
| POST /api/v1/media           | 25ms | 50ms | 80ms | 1500+ |
| GET /api/users/{id}/insights | 20ms | 40ms | 60ms | 1800+ |

_Medido em ambiente staging (4 cores, 8GB RAM)_

---

## 🏆 Reconhecimentos

Tecnologias e ferramentas que tornaram este projeto possível:

- **Spring Boot** - Framework base
- **Java 21** - Linguagem moderna
- **MySQL** - Banco de dados confiável
- **Kafka** - Event streaming
- **Redis** - Cache distribuído
- **Docker** - Containerização
- **Prometheus/Grafana** - Observabilidade
- **OpenTelemetry** - Distributed tracing
- **Liquibase** - Database migrations
- **MapStruct** - Object mapping
- **JUnit 5** - Testing framework
- **Testcontainers** - Integration testing

E toda a comunidade open source! 💚

---

## 📜 Licença

Este projeto está licenciado sob a **MIT License** - veja o arquivo [LICENSE](../LICENSE) para detalhes.

**Em resumo:**

- ✅ Uso comercial permitido
- ✅ Modificação permitida
- ✅ Distribuição permitida
- ✅ Uso privado permitido
- ❗ Sem garantia
- ❗ Sem responsabilidade

---

## 🌟 Star History

Se este projeto foi útil para você, considere dar uma ⭐ no GitHub!

[![Star History Chart](https://api.star-history.com/svg?repos=marcusPrado02/cinelog&type=Date)](https://star-history.com/#marcusPrado02/cinelog&Date)

---

**Última Atualização:** Marco 2026
**Versão da Documentação:** 3.0 (SCDF + Email Batch Jobs)
**Mantenedores:** [@marcusPrado02](https://github.com/marcusPrado02) e time CineLog

---

<div align="center">

**Feito com ❤️ pela comunidade CineLog**

[🏠 Home](../README.md) • [📚 Documentação](./INDEX.md) • [🐛 Issues](https://github.com/marcusPrado02/cinelog/issues) • [💬 Discussões](https://github.com/marcusPrado02/cinelog/discussions)

</div>
