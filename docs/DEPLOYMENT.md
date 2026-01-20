# 🚀 Guia de Deployment - CineLog

## Índice

1. [Visão Geral](#visão-geral)
2. [Ambientes](#ambientes)
3. [Build](#build)
4. [Docker](#docker)
5. [Deploy em Cloud](#deploy-em-cloud)
6. [CI/CD](#cicd)
7. [Monitoramento](#monitoramento)
8. [Troubleshooting](#troubleshooting)

---

## Visão Geral

Este guia cobre o processo de deploy do CineLog em diferentes ambientes.

### Arquitetura de Deploy

```
┌──────────────┐
│   GitHub     │
│  Repository  │
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│   GitHub Actions │
│    (CI/CD)       │
└──────┬───────────┘
       │
       ├────────────┐
       ▼            ▼
┌─────────┐   ┌──────────┐
│  Build  │   │   Test   │
└────┬────┘   └────┬─────┘
     │             │
     └─────┬───────┘
           ▼
    ┌──────────────┐
    │ Docker Image │
    └──────┬───────┘
           │
           ├─────────────┬─────────────┐
           ▼             ▼             ▼
    ┌──────────┐  ┌──────────┐ ┌──────────┐
    │   Dev    │  │ Staging  │ │   Prod   │
    └──────────┘  └──────────┘ └──────────┘
```

---

## Ambientes

### 1. Development (Local)

**Propósito**: Desenvolvimento local

**Configuração**:

```yaml
# application-dev.yml
spring:
    profiles:
        active: dev
    datasource:
        url: jdbc:mysql://localhost:3306/cinelog
    jpa:
        show-sql: true
logging:
    level:
        root: DEBUG
```

**Iniciar**:

```bash
docker-compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Test

**Propósito**: Testes automatizados

**Configuração**:

```yaml
# application-test.yml
spring:
    datasource:
        url: jdbc:tc:mysql:8.0:///test
    jpa:
        hibernate:
            ddl-auto: create-drop
```

**Executar**:

```bash
./mvnw test -Dspring.profiles.active=test
```

### 3. Staging

**Propósito**: Testes antes de produção

**Configuração**:

```yaml
# application-staging.yml
spring:
    datasource:
        url: ${DB_URL}
        username: ${DB_USER}
        password: ${DB_PASSWORD}
logging:
    level:
        root: INFO
```

### 4. Production

**Propósito**: Ambiente de produção

**Configuração**:

```yaml
# application-prod.yml
spring:
    datasource:
        url: ${DB_URL}
        username: ${DB_USER}
        password: ${DB_PASSWORD}
        hikari:
            maximum-pool-size: 20
logging:
    level:
        root: WARN
management:
    endpoints:
        web:
            exposure:
                include: health,info,metrics,prometheus
```

---

## Build

### Build Local

```bash
# Limpa e compila
./mvnw clean compile

# Executa testes
./mvnw test

# Gera JAR
./mvnw package

# Gera JAR sem testes
./mvnw package -DskipTests

# Verifica qualidade do código
./mvnw verify
```

### Artefatos Gerados

```
target/
├── cinelog-0.0.1-SNAPSHOT.jar          # JAR executável
├── cinelog-0.0.1-SNAPSHOT.jar.original # JAR sem dependências
├── classes/                             # Classes compiladas
├── test-classes/                        # Classes de teste
└── site/
    └── jacoco/                          # Relatório de cobertura
```

### Executar JAR

```bash
# Execução básica
java -jar target/cinelog-0.0.1-SNAPSHOT.jar

# Com profile específico
java -jar target/cinelog-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Com variáveis de ambiente
DB_URL=jdbc:mysql://prod-db:3306/cinelog \
DB_USER=cinelog \
DB_PASSWORD=secret \
java -jar target/cinelog-0.0.1-SNAPSHOT.jar
```

---

## Docker

### Dockerfile

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria usuário não-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia JAR do stage anterior
COPY --from=builder /app/target/cinelog-*.jar app.jar

# Expõe porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Entrypoint
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

### Build da Imagem

```bash
# Build
docker build -t cinelog:latest .

# Build com tag específica
docker build -t cinelog:0.0.1 .

# Build multi-plataforma
docker buildx build --platform linux/amd64,linux/arm64 -t cinelog:latest .
```

### Executar Container

```bash
# Execução básica
docker run -p 8080:8080 cinelog:latest

# Com variáveis de ambiente
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:mysql://mysql:3306/cinelog \
  -e DB_USER=cinelog \
  -e DB_PASSWORD=secret \
  cinelog:latest

# Com volume para logs
docker run -p 8080:8080 \
  -v /var/log/cinelog:/app/logs \
  cinelog:latest
```

### Docker Compose

```yaml
# docker-compose.prod.yml
version: "3.8"

services:
    app:
        build: .
        ports:
            - "8080:8080"
        environment:
            SPRING_PROFILES_ACTIVE: docker
            DB_HOST: mysql
            DB_PORT: 3306
            DB_NAME: cinelog
            DB_USER: cinelog
            DB_PASSWORD: ${DB_PASSWORD}
        depends_on:
            - mysql
            - redis
        networks:
            - cinelog-network
        restart: unless-stopped

    mysql:
        image: mysql:8.0
        environment:
            MYSQL_DATABASE: cinelog
            MYSQL_USER: cinelog
            MYSQL_PASSWORD: ${DB_PASSWORD}
            MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
        volumes:
            - mysql-data:/var/lib/mysql
        networks:
            - cinelog-network
        restart: unless-stopped

    redis:
        image: redis:7-alpine
        networks:
            - cinelog-network
        restart: unless-stopped

volumes:
    mysql-data:

networks:
    cinelog-network:
        driver: bridge
```

**Executar**:

```bash
docker-compose -f docker-compose.prod.yml up -d
```

---

## Deploy em Cloud

### AWS (Elastic Beanstalk)

#### 1. Preparar Aplicação

```bash
# Build JAR
./mvnw clean package

# Criar arquivo .ebextensions/01-healthcheck.config
option_settings:
  aws:elasticbeanstalk:application:environment:
    SPRING_PROFILES_ACTIVE: prod
  aws:elasticbeanstalk:environment:process:default:
    HealthCheckPath: /actuator/health
```

#### 2. Deploy

```bash
# Instalar EB CLI
pip install awsebcli

# Inicializar
eb init cinelog --platform "Corretto 21" --region us-east-1

# Criar ambiente
eb create cinelog-prod

# Deploy
eb deploy

# Abrir aplicação
eb open
```

### GCP (Cloud Run)

```bash
# Build e push da imagem
gcloud builds submit --tag gcr.io/PROJECT_ID/cinelog

# Deploy
gcloud run deploy cinelog \
  --image gcr.io/PROJECT_ID/cinelog \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod" \
  --set-env-vars="DB_URL=${DB_URL}" \
  --set-env-vars="DB_USER=${DB_USER}" \
  --set-secrets="DB_PASSWORD=db-password:latest"
```

### Azure (App Service)

```bash
# Login
az login

# Criar resource group
az group create --name cinelog-rg --location eastus

# Criar App Service plan
az appservice plan create \
  --name cinelog-plan \
  --resource-group cinelog-rg \
  --sku B1 \
  --is-linux

# Criar Web App
az webapp create \
  --resource-group cinelog-rg \
  --plan cinelog-plan \
  --name cinelog-app \
  --runtime "JAVA:21-java21"

# Deploy JAR
az webapp deploy \
  --resource-group cinelog-rg \
  --name cinelog-app \
  --src-path target/cinelog-0.0.1-SNAPSHOT.jar \
  --type jar

# Configurar variáveis
az webapp config appsettings set \
  --resource-group cinelog-rg \
  --name cinelog-app \
  --settings SPRING_PROFILES_ACTIVE=prod DB_URL=${DB_URL}
```

### Kubernetes

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
    name: cinelog
spec:
    replicas: 3
    selector:
        matchLabels:
            app: cinelog
    template:
        metadata:
            labels:
                app: cinelog
        spec:
            containers:
                - name: cinelog
                  image: cinelog:latest
                  ports:
                      - containerPort: 8080
                  env:
                      - name: SPRING_PROFILES_ACTIVE
                        value: "prod"
                      - name: DB_URL
                        valueFrom:
                            secretKeyRef:
                                name: db-secret
                                key: url
                      - name: DB_USER
                        valueFrom:
                            secretKeyRef:
                                name: db-secret
                                key: user
                      - name: DB_PASSWORD
                        valueFrom:
                            secretKeyRef:
                                name: db-secret
                                key: password
                  livenessProbe:
                      httpGet:
                          path: /actuator/health/liveness
                          port: 8080
                      initialDelaySeconds: 60
                      periodSeconds: 10
                  readinessProbe:
                      httpGet:
                          path: /actuator/health/readiness
                          port: 8080
                      initialDelaySeconds: 30
                      periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
    name: cinelog-service
spec:
    selector:
        app: cinelog
    ports:
        - protocol: TCP
          port: 80
          targetPort: 8080
    type: LoadBalancer
```

**Deploy**:

```bash
kubectl apply -f deployment.yaml
kubectl get pods
kubectl get services
```

---

## CI/CD

### GitHub Actions

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
    push:
        branches: [master, develop]
    pull_request:
        branches: [master]

jobs:
    test:
        runs-on: ubuntu-latest
        steps:
            - uses: actions/checkout@v3

            - name: Set up JDK 21
              uses: actions/setup-java@v3
              with:
                  java-version: "21"
                  distribution: "temurin"
                  cache: maven

            - name: Run tests
              run: ./mvnw verify

            - name: Upload coverage to Codecov
              uses: codecov/codecov-action@v3
              with:
                  files: ./target/site/jacoco/jacoco.xml

    build:
        needs: test
        runs-on: ubuntu-latest
        if: github.ref == 'refs/heads/master'

        steps:
            - uses: actions/checkout@v3

            - name: Set up JDK 21
              uses: actions/setup-java@v3
              with:
                  java-version: "21"
                  distribution: "temurin"

            - name: Build JAR
              run: ./mvnw package -DskipTests

            - name: Build Docker image
              run: docker build -t ghcr.io/${{ github.repository }}:${{ github.sha }} .

            - name: Login to GitHub Container Registry
              uses: docker/login-action@v2
              with:
                  registry: ghcr.io
                  username: ${{ github.actor }}
                  password: ${{ secrets.GITHUB_TOKEN }}

            - name: Push Docker image
              run: docker push ghcr.io/${{ github.repository }}:${{ github.sha }}

    deploy:
        needs: build
        runs-on: ubuntu-latest
        if: github.ref == 'refs/heads/master'

        steps:
            - name: Deploy to production
              run: |
                  # Deploy script aqui
                  echo "Deploying to production..."
```

---

## Monitoramento

### Métricas (Prometheus)

```yaml
# prometheus.yml
scrape_configs:
    - job_name: "cinelog"
      metrics_path: "/actuator/prometheus"
      static_configs:
          - targets: ["cinelog:8080"]
```

### Logs (ELK Stack)

```yaml
# logstash.conf
input {
file {
path => "/var/log/cinelog/*.log"
codec => json
}
}

filter {
json {
source => "message"
}
}

output {
elasticsearch {
hosts => ["elasticsearch:9200"]
index => "cinelog-%{+YYYY.MM.dd}"
}
}
```

### Alertas

```yaml
# alertmanager.yml
route:
    receiver: "team-email"

receivers:
    - name: "team-email"
      email_configs:
          - to: "team@cinelog.com"
            from: "alerts@cinelog.com"
            smarthost: "smtp.gmail.com:587"
```

---

## Troubleshooting

### Problemas Comuns

#### 1. Aplicação não inicia

```bash
# Verificar logs
docker logs cinelog-app

# Verificar conectividade com banco
docker exec cinelog-app nc -zv mysql 3306
```

#### 2. Alto uso de memória

```bash
# Ajustar heap size
java -Xms512m -Xmx2g -jar app.jar

# No Docker
docker run -m 2g cinelog:latest
```

#### 3. Conexões de banco esgotadas

```yaml
# Aumentar pool size
spring:
    datasource:
        hikari:
            maximum-pool-size: 20
```

---

**Última atualização**: Dezembro 2025
