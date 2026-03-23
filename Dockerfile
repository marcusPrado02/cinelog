# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2: runtime
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Cria usuario non-root (usa IDs altos para evitar conflito com existentes)
RUN groupadd -r cinelog && useradd -r -g cinelog -s /bin/false cinelog

COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/logs && chown -R cinelog:cinelog /app

USER cinelog
EXPOSE 8080

# O SCDF injeta seu datasource via SPRING_APPLICATION_JSON, mas a app precisa
# do schema 'cinelog'. O entrypoint script sobrescreve via command line args
# (precedencia mais alta que SPRING_APPLICATION_JSON) quando SPRING_PROFILES_ACTIVE=task.
COPY --chown=cinelog:cinelog docker/scdf/entrypoint.sh /app/entrypoint.sh
# Copiar .env para que o entrypoint carregue TMDB_API_KEY e outras variaveis
COPY --chown=cinelog:cinelog .env* /app/
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
