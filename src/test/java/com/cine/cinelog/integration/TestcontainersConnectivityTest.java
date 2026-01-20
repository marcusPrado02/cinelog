package com.cine.cinelog.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de conectividade dos Testcontainers.
 *
 * <p>
 * <strong>Feature:</strong> PR7 - Integration Testing Framework
 *
 * <p>
 * Valida que todos os containers (MySQL, Kafka, Redis) estão
 * funcionando corretamente antes de executar testes de integração.
 *
 * <p>
 * <strong>Containers testados:</strong>
 * <ul>
 * <li>MySQL 8.0 - Conectividade JDBC</li>
 * <li>Kafka 7.6.1 - Producer/Consumer</li>
 * <li>Redis 7.2 - Cache operations</li>
 * </ul>
 *
 * @since 1.0 (PR7)
 */
@DisplayName("Testcontainers Connectivity Tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestcontainersConnectivityTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // ========== MySQL Connectivity Tests ==========

    @Test
    @DisplayName("Should connect to MySQL container")
    void shouldConnectToMySQL() {
        // Assert: MySQL container is running
        assertThat(mysqlContainer.isRunning())
                .as("MySQL container should be running")
                .isTrue();

        // Assert: Can execute query
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result)
                .as("Query 'SELECT 1' should return 1")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Should have correct MySQL database name")
    void shouldHaveCorrectMySQLDatabaseName() {
        // Assert: Database name is 'cinelog_test'
        String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        assertThat(dbName)
                .as("Database name should be 'cinelog_test'")
                .isEqualTo("cinelog_test");
    }

    @Test
    @DisplayName("Should have MySQL tables created by Liquibase")
    void shouldHaveMySQLTablesCreatedByLiquibase() {
        // Assert: Check if Liquibase ran successfully
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'cinelog_test'",
                Integer.class);

        assertThat(tableCount)
                .as("Should have tables created by Liquibase")
                .isGreaterThan(5); // At least core tables should exist
    }

    // ========== Kafka Connectivity Tests ==========

    @Test
    @DisplayName("Should connect to Kafka container")
    void shouldConnectToKafka() {
        // Assert: Kafka container is running
        assertThat(kafkaContainer.isRunning())
                .as("Kafka container should be running")
                .isTrue();

        // Assert: Bootstrap servers is configured
        String bootstrapServers = kafkaContainer.getBootstrapServers();
        assertThat(bootstrapServers)
                .as("Kafka bootstrap servers should be configured")
                .isNotNull()
                .contains("PLAINTEXT://");
    }

    @Test
    @DisplayName("Should have KafkaTemplate bean configured")
    void shouldHaveKafkaTemplateBeanConfigured() {
        // Assert: KafkaTemplate bean exists
        assertThat(kafkaTemplate)
                .as("KafkaTemplate bean should be auto-configured")
                .isNotNull();
    }

    // ========== Redis Connectivity Tests ==========

    @Test
    @DisplayName("Should connect to Redis container")
    void shouldConnectToRedis() {
        // Assert: Redis container is running
        assertThat(redisContainer.isRunning())
                .as("Redis container should be running")
                .isTrue();

        // Assert: Redis host and port are configured
        assertThat(redisContainer.getHost())
                .as("Redis host should be configured")
                .isNotNull();

        assertThat(redisContainer.getMappedPort(6379))
                .as("Redis port should be mapped")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Should have RedisTemplate bean configured")
    void shouldHaveRedisTemplateBeanConfigured() {
        // Assert: RedisTemplate bean exists
        assertThat(redisTemplate)
                .as("RedisTemplate bean should be auto-configured")
                .isNotNull();
    }

    @Test
    @DisplayName("Should perform basic Redis operations")
    void shouldPerformBasicRedisOperations() {
        if (redisTemplate == null) {
            // Skip test if RedisTemplate is not configured
            return;
        }

        // Arrange
        String key = "test:connectivity";
        String value = "Hello from Testcontainers!";

        // Act: Set value
        redisTemplate.opsForValue().set(key, value);

        // Assert: Get value
        Object retrieved = redisTemplate.opsForValue().get(key);
        assertThat(retrieved)
                .as("Redis should store and retrieve value")
                .isEqualTo(value);

        // Cleanup
        redisTemplate.delete(key);
    }

    // ========== Container Information Tests ==========

    @Test
    @DisplayName("Should have all containers running with correct images")
    void shouldHaveAllContainersRunningWithCorrectImages() {
        // MySQL
        assertThat(mysqlContainer.getDockerImageName())
                .as("MySQL image should be mysql:8.0.39")
                .contains("mysql:8.0.39");

        // Kafka
        assertThat(kafkaContainer.getDockerImageName())
                .as("Kafka image should be confluentinc/cp-kafka:7.6.1")
                .contains("confluentinc/cp-kafka:7.6.1");

        // Redis
        assertThat(redisContainer.getDockerImageName())
                .as("Redis image should be redis:7.2-alpine")
                .contains("redis:7.2-alpine");
    }

    @Test
    @DisplayName("Should have base URL configured")
    void shouldHaveBaseUrlConfigured() {
        // Assert: Base URL contains port
        String baseUrl = getBaseUrl();
        assertThat(baseUrl)
                .as("Base URL should contain localhost and port")
                .startsWith("http://localhost:")
                .contains(String.valueOf(port));
    }
}
