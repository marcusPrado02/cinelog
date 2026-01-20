package com.cine.cinelog.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Classe base para testes de integração com Testcontainers.
 *
 * <p>
 * <strong>Feature:</strong> PR7 - Integration Testing Framework
 *
 * <p>
 * Configura automaticamente containers Docker para:
 * <ul>
 * <li>MySQL 8.0 (banco de dados)</li>
 * <li>Kafka 7.6.1 (mensageria)</li>
 * <li>Redis 7.2 (cache)</li>
 * </ul>
 *
 * <p>
 * <strong>Uso:</strong>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 *     class MyIntegrationTest extends AbstractIntegrationTest {
 *
 *         &#64;Autowired
 *         private MyService service;
 *
 *         @Test
 *         void testWithRealDatabase() {
 *             // Test implementation
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>
 * <strong>Containers Reutilizáveis:</strong>
 * Os containers são compartilhados entre testes para performance.
 * Inicialização: ~15-20 segundos (primeira vez), ~2-3s (testes seguintes).
 *
 * <p>
 * <strong>Configuração:</strong>
 * - Profile ativo: "test"
 * - Server port: random (evita conflitos)
 * - Containers: singleton (reusable)
 *
 * @since 1.0 (PR7)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    // ========== Testcontainers Configuration ==========

    /**
     * MySQL 8.0 container (singleton, reusable).
     */
    protected static final MySQLContainer<?> mysqlContainer;

    /**
     * Kafka 7.6.1 container (Confluent Platform, singleton, reusable).
     */
    protected static final KafkaContainer kafkaContainer;

    /**
     * Redis 7.2 container (singleton, reusable).
     */
    protected static final GenericContainer<?> redisContainer;

    static {
        // MySQL Container (MySQL 8.0)
        mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.39"))
                .withDatabaseName("cinelog_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true); // Reutilizar container entre execuções

        // Kafka Container (Confluent Platform 7.6.1)
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
                .withReuse(true);

        // Redis Container (Redis 7.2)
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);

        // Start all containers
        mysqlContainer.start();
        kafkaContainer.start();
        redisContainer.start();
    }

    /**
     * Configura propriedades dinâmicas para Spring Boot Test.
     *
     * <p>
     * Sobrescreve configurações de application-test.yml com valores
     * dos containers Testcontainers.
     *
     * @param registry registry de propriedades dinâmicas
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL Configuration
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Kafka Configuration
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafkaContainer::getBootstrapServers);

        // Redis Configuration
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        // Liquibase: auto-apply migrations
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.drop-first", () -> "false"); // Keep data between tests

        // Kafka Topics: auto-create
        registry.add("spring.kafka.admin.auto-create", () -> "true");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    /**
     * Hook executado antes de todos os testes da classe.
     *
     * <p>
     * Pode ser sobrescrito por subclasses para inicialização adicional.
     */
    @BeforeAll
    static void setUpBase() {
        // Verify containers are running
        if (!mysqlContainer.isRunning()) {
            throw new IllegalStateException("MySQL container failed to start");
        }
        if (!kafkaContainer.isRunning()) {
            throw new IllegalStateException("Kafka container failed to start");
        }
        if (!redisContainer.isRunning()) {
            throw new IllegalStateException("Redis container failed to start");
        }
    }

    /**
     * Retorna a base URL da aplicação (para REST Assured, etc.).
     *
     * @return base URL (ex: http://localhost:8080)
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Retorna o bootstrap servers do Kafka (para testes).
     *
     * @return Kafka bootstrap servers
     */
    protected String getKafkaBootstrapServers() {
        return kafkaContainer.getBootstrapServers();
    }

    /**
     * Retorna a JDBC URL do MySQL (para testes).
     *
     * @return MySQL JDBC URL
     */
    protected String getMySqlJdbcUrl() {
        return mysqlContainer.getJdbcUrl();
    }

    /**
     * Retorna o host do Redis (para testes).
     *
     * @return Redis host
     */
    protected String getRedisHost() {
        return redisContainer.getHost();
    }

    /**
     * Retorna a porta mapeada do Redis (para testes).
     *
     * @return Redis port
     */
    protected int getRedisPort() {
        return redisContainer.getMappedPort(6379);
    }
}
