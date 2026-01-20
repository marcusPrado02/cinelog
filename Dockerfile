# Fase 1: build do jar
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
# baixa dependências em cache
RUN mvn -B -q dependency:go-offline

COPY src ./src

# gera o jar (pode manter testes ou pular com -DskipTests)
RUN mvn -B -DskipTests package

# Fase 2: imagem de runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# copia o jar gerado na fase anterior
COPY --from=builder /app/target/*.jar app.jar

# instala ferramentas necessárias para o HEALTHCHECK e cria um usuário não-root
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates adduser && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --gid 1000 app && \
    useradd --uid 1000 --gid app --shell /bin/sh --no-create-home --system app && \
    chown -R app:app /app

EXPOSE 8080

# variáveis padrão (você ajusta se precisar)
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/cinelog \
    SPRING_DATASOURCE_USERNAME=cinelog \
    SPRING_DATASOURCE_PASSWORD=cinelog \
    SPRING_JPA_HIBERNATE_DDL_AUTO=update

# usa usuário não-root para rodar a aplicação
USER app

# healthcheck simples que verifica o endpoint de saúde do Spring Boot
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
