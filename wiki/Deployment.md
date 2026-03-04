# 🚀 Deployment

> Docker, Kubernetes, CI/CD e deploy em cloud do CineLog.

---

## Ambientes

| Profile    | Uso                   | Banco            | Cache           | Kafka           |
| ---------- | --------------------- | ---------------- | --------------- | --------------- |
| **dev**    | Desenvolvimento local | MySQL local      | Redis local     | Kafka local     |
| **test**   | Testes automatizados  | Testcontainers   | —               | Testcontainers  |
| **docker** | Docker Compose        | MySQL container  | Redis container | Kafka container |
| **perf**   | Testes de carga       | MySQL container  | Redis container | Kafka container |
| **prod**   | Produção              | MySQL gerenciado | Redis cluster   | Kafka cluster   |

---

## Docker

### Dockerfile (Multi-stage)

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S cinelog && adduser -S cinelog -G cinelog
WORKDIR /app
COPY --from=build /app/target/cinelog-*.jar app.jar
USER cinelog
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

### Build & Run

```bash
# Build da imagem
docker build -t cinelog:latest .

# Run com Docker Compose
docker compose up -d

# Logs
docker compose logs -f app
```

---

## Docker Compose (Produção)

```yaml
services:
    app:
        image: cinelog:latest
        ports:
            - "8080:8080"
        environment:
            SPRING_PROFILES_ACTIVE: docker
            SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/cinelog
            CINELOG_SECURITY_JWT_SECRET: ${JWT_SECRET}
            TMDB_API_KEY: ${TMDB_API_KEY}
        depends_on:
            db:
                condition: service_healthy
            redis:
                condition: service_started
        deploy:
            resources:
                limits:
                    memory: 512M
                    cpus: "1.0"
        restart: unless-stopped

    db:
        image: mysql:8.0
        environment:
            MYSQL_DATABASE: cinelog
            MYSQL_USER: cinelog
            MYSQL_PASSWORD: ${DB_PASSWORD}
            MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
        volumes:
            - dbdata:/var/lib/mysql
        healthcheck:
            test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
            interval: 10s
            timeout: 5s
            retries: 5

    redis:
        image: redis:7-alpine
        command: redis-server --appendonly yes
        volumes:
            - redis-data:/data
```

---

## Kubernetes

### Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
    name: cinelog-api
spec:
    replicas: 3
    selector:
        matchLabels:
            app: cinelog-api
    template:
        metadata:
            labels:
                app: cinelog-api
        spec:
            containers:
                - name: cinelog-api
                  image: cinelog:latest
                  ports:
                      - containerPort: 8080
                  env:
                      - name: SPRING_PROFILES_ACTIVE
                        value: "prod"
                      - name: CINELOG_SECURITY_JWT_SECRET
                        valueFrom:
                            secretKeyRef:
                                name: cinelog-secrets
                                key: jwt-secret
                  resources:
                      requests:
                          memory: "256Mi"
                          cpu: "250m"
                      limits:
                          memory: "512Mi"
                          cpu: "1000m"
                  livenessProbe:
                      httpGet:
                          path: /actuator/health/liveness
                          port: 8080
                      initialDelaySeconds: 60
                      periodSeconds: 15
                  readinessProbe:
                      httpGet:
                          path: /actuator/health/readiness
                          port: 8080
                      initialDelaySeconds: 30
                      periodSeconds: 10
```

### Service

```yaml
apiVersion: v1
kind: Service
metadata:
    name: cinelog-api
spec:
    type: LoadBalancer
    ports:
        - port: 80
          targetPort: 8080
    selector:
        app: cinelog-api
```

---

## CI/CD (GitHub Actions)

```yaml
name: CI/CD Pipeline
on:
    push:
        branches: [master]
    pull_request:
        branches: [master]

jobs:
    test:
        runs-on: ubuntu-latest
        services:
            mysql:
                image: mysql:8.0
                env:
                    MYSQL_DATABASE: cinelog_test
                    MYSQL_ROOT_PASSWORD: test
                ports:
                    - 3306:3306
        steps:
            - uses: actions/checkout@v4
            - uses: actions/setup-java@v4
              with:
                  distribution: temurin
                  java-version: 21
                  cache: maven
            - run: ./mvnw clean verify

    build:
        needs: test
        runs-on: ubuntu-latest
        steps:
            - uses: actions/checkout@v4
            - run: docker build -t cinelog:${{ github.sha }} .
            - run: docker push cinelog:${{ github.sha }}

    deploy:
        needs: build
        if: github.ref == 'refs/heads/master'
        runs-on: ubuntu-latest
        steps:
            - run: kubectl set image deployment/cinelog-api cinelog-api=cinelog:${{ github.sha }}
```

---

## Cloud Providers

### AWS (Elastic Beanstalk)

```bash
eb init cinelog --platform "Java 21" --region sa-east-1
eb create cinelog-prod --instance_type t3.small
eb deploy
```

### GCP (Cloud Run)

```bash
gcloud run deploy cinelog \
  --image cinelog:latest \
  --platform managed \
  --region southamerica-east1 \
  --memory 512Mi \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod
```

### Azure (App Service)

```bash
az webapp create --name cinelog \
  --resource-group cinelog-rg \
  --plan cinelog-plan \
  --runtime "JAVA:21-java21"
az webapp deploy --name cinelog \
  --src-path target/cinelog-0.0.1-SNAPSHOT.jar
```

---

## Variáveis de Ambiente (Produção)

| Variável                      | Obrigatória | Descrição                          |
| ----------------------------- | ----------- | ---------------------------------- |
| `SPRING_PROFILES_ACTIVE`      | ✅          | Profile ativo (`prod`)             |
| `SPRING_DATASOURCE_URL`       | ✅          | URL JDBC do MySQL                  |
| `SPRING_DATASOURCE_USERNAME`  | ✅          | Usuário do banco                   |
| `SPRING_DATASOURCE_PASSWORD`  | ✅          | Senha do banco                     |
| `CINELOG_SECURITY_JWT_SECRET` | ✅          | Chave JWT (≥32 chars)              |
| `TMDB_API_KEY`                | ✅          | API key do TMDb                    |
| `SPRING_REDIS_HOST`           | ❌          | Host do Redis (default: localhost) |
| `CORS_ALLOWED_ORIGINS`        | ❌          | Origens CORS permitidas            |

---

## Checklist de Produção

- [ ] Variáveis de ambiente configuradas (especialmente JWT_SECRET)
- [ ] TLS/HTTPS habilitado (reverse proxy ou cloud provider)
- [ ] Banco de dados com backup automatizado
- [ ] Redis com persistência (AOF ou RDB)
- [ ] Monitoring stack ativo (Prometheus + Grafana)
- [ ] Log aggregation configurado (ELK ou similar)
- [ ] Health checks configurados no load balancer
- [ ] Rate limiting ajustado para produção
- [ ] CORS configurado para domínio real
- [ ] Dependency-Check sem CVEs críticas
