# 📄 Reports API — Referência Completa

> Endpoints de **preview**, **envio por e-mail** e **download em PDF** dos relatórios do CineLog.

---

## Índice

- [Visão Geral](#visão-geral)
- [Autenticação](#autenticação)
- [Endpoints de Usuário](#endpoints-de-usuário)
    - [Weekly Digest](#weekly-digest)
    - [Top Rated](#top-rated)
    - [Recommendations](#recommendations)
    - [Trending](#trending)
    - [Top Actors](#top-actors)
    - [New Releases](#new-releases)
    - [Genre Spotlight](#genre-spotlight)
- [Endpoints de Admin](#endpoints-de-admin)
    - [Platform Report](#platform-report)
    - [Send-to-All](#send-to-all)
- [Download PDF](#download-pdf)
- [Geração de PDF — Gotenberg](#geração-de-pdf--gotenberg)
- [Templates](#templates)
- [Configuração](#configuração)
- [Fluxo Arquitetural](#fluxo-arquitetural)
- [Exemplos cURL](#exemplos-curl)

---

## Visão Geral

O módulo de relatórios fornece **8 tipos de relatório** com três modos de consumo:

| Modo                 | Verbo/Path                       | Retorno                             |
| -------------------- | -------------------------------- | ----------------------------------- |
| **Preview (JSON)**   | `GET /api/v1/reports/{tipo}`     | Dados brutos em JSON                |
| **Envio por e-mail** | `POST /api/v1/reports/{tipo}`    | `202 Accepted` — e-mail enfileirado |
| **Download PDF**     | `GET /api/v1/reports/{tipo}/pdf` | `200 OK` — `application/pdf` inline |

### Tipos de relatório

| Slug              | Descrição                            | Escopo  |
| ----------------- | ------------------------------------ | ------- |
| `weekly-digest`   | Resumo semanal do usuário            | Usuário |
| `top-rated`       | Mídias mais bem avaliadas            | Público |
| `recommendations` | Recomendações personalizadas         | Usuário |
| `trending`        | Em alta (últimos N dias)             | Público |
| `top-actors`      | Atores com filmes mais bem avaliados | Público |
| `new-releases`    | Novos títulos adicionados            | Público |
| `genre-spotlight` | Análise profunda de um gênero        | Público |
| `platform`        | Métricas globais da plataforma       | Admin   |

---

## Autenticação

Todos os endpoints requerem **Bearer token** (JWT local ou Keycloak OAuth2).

```
Authorization: Bearer <access_token>
```

Endpoints sob `/api/v1/admin/reports/*` requerem role **ADMIN**.

---

## Endpoints de Usuário

### Weekly Digest

| Método | Path                                | Descrição                          |
| ------ | ----------------------------------- | ---------------------------------- |
| `GET`  | `/api/v1/reports/weekly-digest`     | Preview JSON do digest semanal     |
| `POST` | `/api/v1/reports/weekly-digest`     | Envia digest por e-mail ao usuário |
| `GET`  | `/api/v1/reports/weekly-digest/pdf` | Download PDF do digest semanal     |

**Resposta do preview:** `WeeklyDigestData` — `recentlyWatched`, `stats`, `generatedAt`

---

### Top Rated

| Método | Path                            | Params                                  | Descrição         |
| ------ | ------------------------------- | --------------------------------------- | ----------------- |
| `GET`  | `/api/v1/reports/top-rated`     | `limit` (default 10)                    | Preview JSON      |
| `POST` | `/api/v1/reports/top-rated`     | Body: `{ "email": "...", "limit": 10 }` | Enviar por e-mail |
| `GET`  | `/api/v1/reports/top-rated/pdf` | `limit` (default 10)                    | Download PDF      |

---

### Recommendations

| Método | Path                                  | Descrição                               |
| ------ | ------------------------------------- | --------------------------------------- |
| `GET`  | `/api/v1/reports/recommendations`     | Preview de recomendações personalizadas |
| `POST` | `/api/v1/reports/recommendations`     | Enviar recomendações por e-mail         |
| `GET`  | `/api/v1/reports/recommendations/pdf` | Download PDF                            |

---

### Trending

| Método | Path                           | Params                     | Descrição         |
| ------ | ------------------------------ | -------------------------- | ----------------- |
| `GET`  | `/api/v1/reports/trending`     | `days` (7), `limit` (10)   | Preview JSON      |
| `POST` | `/api/v1/reports/trending`     | Body: `{ "email": "..." }` | Enviar por e-mail |
| `GET`  | `/api/v1/reports/trending/pdf` | `days` (7), `limit` (10)   | Download PDF      |

---

### Top Actors

| Método | Path                             | Params                                  | Descrição         |
| ------ | -------------------------------- | --------------------------------------- | ----------------- |
| `GET`  | `/api/v1/reports/top-actors`     | `limit` (default 10)                    | Preview JSON      |
| `POST` | `/api/v1/reports/top-actors`     | Body: `{ "email": "...", "limit": 10 }` | Enviar por e-mail |
| `GET`  | `/api/v1/reports/top-actors/pdf` | `limit` (default 10)                    | Download PDF      |

---

### New Releases

| Método | Path                               | Params                     | Descrição         |
| ------ | ---------------------------------- | -------------------------- | ----------------- |
| `GET`  | `/api/v1/reports/new-releases`     | `days` (30), `limit` (20)  | Preview JSON      |
| `POST` | `/api/v1/reports/new-releases`     | Body: `{ "email": "..." }` | Enviar por e-mail |
| `GET`  | `/api/v1/reports/new-releases/pdf` | `days` (30), `limit` (20)  | Download PDF      |

---

### Genre Spotlight

| Método | Path                                  | Params                                      | Descrição         |
| ------ | ------------------------------------- | ------------------------------------------- | ----------------- |
| `GET`  | `/api/v1/reports/genre-spotlight`     | `genre` (opcional)                          | Preview JSON      |
| `POST` | `/api/v1/reports/genre-spotlight`     | `genre` (query), Body: `{ "email": "..." }` | Enviar por e-mail |
| `GET`  | `/api/v1/reports/genre-spotlight/pdf` | `genre` (opcional)                          | Download PDF      |

> Se `genre` não for informado, o sistema seleciona automaticamente o gênero com mais atividade.

---

## Endpoints de Admin

### Platform Report

| Método | Path                                 | Auth  | Descrição                      |
| ------ | ------------------------------------ | ----- | ------------------------------ |
| `GET`  | `/api/v1/admin/reports/platform`     | ADMIN | Preview métricas da plataforma |
| `POST` | `/api/v1/admin/reports/platform`     | ADMIN | Enviar por e-mail              |
| `GET`  | `/api/v1/admin/reports/platform/pdf` | ADMIN | Download PDF (landscape)       |

### Send-to-All

| Método | Path                                | Auth  | Descrição                                     |
| ------ | ----------------------------------- | ----- | --------------------------------------------- |
| `POST` | `/api/v1/admin/reports/send-to-all` | ADMIN | Envia trending a **todos** os usuários ativos |

⚠️ Use com cautela — dispara e-mail em massa.

---

## Download PDF

Todos os endpoints `/pdf` retornam:

```
Content-Type: application/pdf
Content-Disposition: inline; filename="<tipo>.pdf"
```

### Headers da resposta

| Header                | Valor                                  |
| --------------------- | -------------------------------------- |
| `Content-Type`        | `application/pdf`                      |
| `Content-Disposition` | `inline; filename="weekly-digest.pdf"` |

O PDF é gerado sob demanda via **Gotenberg** e não é armazenado — cada chamada gera um novo PDF.

### PDF como anexo de e-mail

Quando `cinelog.reports.pdf.attach-to-email=true`, os envios por e-mail (`POST`) incluem automaticamente o PDF como anexo. A geração é **fail-safe**: se o Gotenberg estiver indisponível, o e-mail é enviado sem anexo.

---

## Geração de PDF — Gotenberg

O CineLog usa o **[Gotenberg](https://gotenberg.dev)** (v8) como serviço de conversão HTML → PDF via API REST.

### Arquitetura

```
ReportController
     │
     ├─ GET .../pdf ──→ GotenbergPdfService.generate()
     │                       │
     │                       ├─ 1. Thymeleaf renderiza templates/pdf/{tipo}.html
     │                       ├─ 2. POST /forms/chromium/convert/html → Gotenberg
     │                       └─ 3. Retorna byte[] do PDF
     │
     └─ POST ...     ──→ ReportEmailService
                              │
                              ├─ EmailService.sendHtml() (template email/)
                              └─ se attach-to-email=true:
                                   GotenbergPdfService.generate() → anexo PDF
```

### Vantagens

- **Zero dependência Java** — nenhuma lib PDF no `pom.xml`
- **CSS 100% moderno** — Chromium headless renderiza CSS Grid, Flexbox, `@media print`
- **Isolamento** — container Docker separado, sem impacto na JVM
- **Qualidade** — renderização idêntica ao Chrome

### Container Docker

```yaml
# docker-compose.yml
gotenberg:
    image: gotenberg/gotenberg:8
    ports:
        - "3001:3000"
    restart: unless-stopped
```

### API utilizada

```
POST http://gotenberg:3000/forms/chromium/convert/html
Content-Type: multipart/form-data

Parts:
  files     = index.html (HTML renderizado)
  marginTop = 0.8
  marginBottom = 0.8
  marginLeft = 0.6
  marginRight = 0.6
  paperWidth = 8.27   (A4)
  paperHeight = 11.69 (A4)
  printBackground = true
  emulateMediaType = print
  landscape = true|false
```

---

## Templates

### Templates de PDF (`templates/pdf/`)

| Template        | Arquivo                | Orientação   |
| --------------- | ---------------------- | ------------ |
| Weekly Digest   | `weekly-digest.html`   | Retrato      |
| Trending        | `trending.html`        | Retrato      |
| Top Rated       | `top-rated.html`       | Retrato      |
| Recommendations | `recommendations.html` | Retrato      |
| Platform Report | `platform-report.html` | **Paisagem** |
| Top Actors      | `top-actors.html`      | Retrato      |
| New Releases    | `new-releases.html`    | Retrato      |
| Genre Spotlight | `genre-spotlight.html` | Retrato      |

### Design

- **Tema dark/cinema** — fundo `#0d0d0d`, vermelho `#e50914` (CineLog brand)
- **CSS inline** — obrigatório para Gotenberg (não carrega CSS externo do classpath)
- **Fragment base** — `fragments/base.html` com estilos compartilhados
- **Badges** — Filme (vermelho), Série (azul)
- **Métricas visuais** — cards com valores em destaque, tabelas estilizadas

### Templates de E-mail (`templates/email/`)

Os templates de e-mail são independentes dos de PDF. Ambos recebem o mesmo objeto `data` como variável Thymeleaf.

---

## Configuração

### application.yml

```yaml
cinelog:
    reports:
        enabled: true
        from-email: noreply@cinelog.dev
        from-name: CineLog
        base-url: ${CINELOG_BASE_URL:http://localhost:8080}
        pdf:
            enabled: true
            gotenberg-url: ${GOTENBERG_URL:http://localhost:3001}
            timeout-seconds: 30
            attach-to-email: false
        cron:
            weekly-digest: "0 0 8 * * MON"
            trending: "0 0 18 * * FRI"
            platform-report: "0 0 6 * * SUN"
```

### Variáveis de ambiente

| Variável                              | Default                 | Descrição                       |
| ------------------------------------- | ----------------------- | ------------------------------- |
| `GOTENBERG_URL`                       | `http://localhost:3001` | URL do container Gotenberg      |
| `CINELOG_REPORTS_PDF_ENABLED`         | `true`                  | Habilita geração de PDF         |
| `CINELOG_REPORTS_PDF_ATTACH_TO_EMAIL` | `false`                 | Anexa PDF nos e-mails           |
| `CINELOG_REPORTS_PDF_TIMEOUT_SECONDS` | `30`                    | Timeout da chamada ao Gotenberg |

---

## Fluxo Arquitetural

```
┌──────────────┐     ┌────────────────────┐     ┌───────────────────┐
│              │     │                    │     │                   │
│  Controller  │────▶│  QueryService      │────▶│  Repository/JPA   │
│  (REST)      │     │  (build data)      │     │  (database)       │
│              │     │                    │     │                   │
└──────┬───────┘     └────────────────────┘     └───────────────────┘
       │
       ├─── GET .../pdf ───▶ GotenbergPdfService
       │                         │
       │                    ┌────▼────────────────┐
       │                    │ 1. Thymeleaf render  │
       │                    │ 2. POST → Gotenberg  │
       │                    │ 3. Return byte[]     │
       │                    └─────────────────────┘
       │
       └─── POST ... ─────▶ ReportEmailService
                                 │
                            ┌────▼────────────────┐
                            │ EmailService         │
                            │  ├ sendHtml()        │
                            │  └ +attachment (opt) │
                            └─────────────────────┘
```

### Classes principais

| Classe                | Pacote           | Responsabilidade                                      |
| --------------------- | ---------------- | ----------------------------------------------------- |
| `ReportController`    | `reports.web`    | Endpoints REST (preview, email, PDF)                  |
| `GotenbergPdfService` | `reports.pdf`    | Renderiza HTML via Thymeleaf + chama Gotenberg        |
| `PdfOptions`          | `reports.pdf`    | Record com opções de papel/margens (A4, A4 landscape) |
| `GotenbergException`  | `reports.pdf`    | RuntimeException para falhas do Gotenberg             |
| `ReportEmailService`  | `reports.email`  | Orquestrador: query data → send email ± PDF           |
| `EmailService`        | `reports.email`  | Low-level: Thymeleaf + JavaMailSender + attachment    |
| `ReportProperties`    | `reports.config` | Config properties (`cinelog.reports.*`)               |
| `*QueryService`       | `reports.query`  | Um por tipo de relatório (build data)                 |
| `*Data`               | `reports.data`   | DTOs com os dados de cada relatório                   |

---

## Exemplos cURL

### Preview JSON

```bash
# Weekly Digest (requer autenticação)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/reports/weekly-digest | jq .

# Trending (últimos 30 dias, top 5)
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/reports/trending?days=30&limit=5" | jq .
```

### Download PDF

```bash
# Weekly Digest como PDF
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/reports/weekly-digest/pdf \
  -o weekly-digest.pdf

# Platform Report (admin, landscape)
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/v1/admin/reports/platform/pdf \
  -o platform-report.pdf

# Top Actors (top 20)
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/reports/top-actors/pdf?limit=20" \
  -o top-actors.pdf

# Genre Spotlight — Drama
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/reports/genre-spotlight/pdf?genre=Drama" \
  -o genre-spotlight-drama.pdf
```

### Enviar e-mail

```bash
# Enviar top-rated ao e-mail do próprio usuário
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  http://localhost:8080/api/v1/reports/top-rated

# Enviar trending a um endereço específico
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "test@mailhog.local"}' \
  http://localhost:8080/api/v1/reports/trending

# Admin: enviar trending a TODOS os usuários
curl -s -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/v1/admin/reports/send-to-all
```

### Verificar no MailHog

Após o envio, abra http://localhost:8025 para ver os e-mails capturados localmente.
