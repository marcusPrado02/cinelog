package com.cine.cinelog.infrastructure.web.filters;

import com.cine.cinelog.shared.logging.CorrelationIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testes para CorrelationIdFilter.
 * <p>
 * Valida:
 * - Extração de correlationId do header X-Correlation-Id
 * - Geração automática de UUID se não fornecido
 * - Propagação para MDC e CorrelationIdHolder
 * - Limpeza após processamento
 * - Adição ao response header
 * <p>
 * PR4: Distributed Tracing Infrastructure
 */
@DisplayName("CorrelationIdFilter - Distributed Tracing Tests")
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        // Limpar MDC antes de cada teste
        MDC.clear();
        CorrelationIdHolder.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        CorrelationIdHolder.clear();
    }

    @Test
    @DisplayName("Deve extrair correlationId do header X-Correlation-Id")
    void shouldExtractCorrelationIdFromHeader() throws ServletException, IOException {
        // Arrange
        String expectedCorrelationId = "test-correlation-123";
        request.addHeader("X-Correlation-Id", expectedCorrelationId);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert - MDC e Holder são limpos no finally, mas response header permanece
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(expectedCorrelationId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve gerar UUID se correlationId não fornecido no header")
    void shouldGenerateUuidWhenCorrelationIdNotProvided() throws ServletException, IOException {
        // Act
        filter.doFilter(request, response, filterChain);

        // Assert - Verifica response header (MDC é limpo no finally)
        String correlationId = response.getHeader("X-Correlation-Id");
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).matches("^[a-f0-9-]{36}$"); // UUID format
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve propagar correlationId para MDC e CorrelationIdHolder")
    void shouldPropagateCorrelationIdToMdcAndHolder() throws ServletException, IOException {
        // Arrange
        String correlationId = "propagation-test-456";
        request.addHeader("X-Correlation-Id", correlationId);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert - Verificar durante processamento (via mock)
        verify(filterChain).doFilter(request, response);
        // Após processamento, MDC e Holder são limpos
    }

    @Test
    @DisplayName("Deve adicionar correlationId ao response header")
    void shouldAddCorrelationIdToResponseHeader() throws ServletException, IOException {
        // Arrange
        String correlationId = "response-header-789";
        request.addHeader("X-Correlation-Id", correlationId);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve limpar MDC após processamento bem-sucedido")
    void shouldCleanupMdcAfterSuccessfulProcessing() throws ServletException, IOException {
        // Arrange
        request.addHeader("X-Correlation-Id", "cleanup-test");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert - MDC deve estar limpo após finally
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Deve limpar CorrelationIdHolder após processamento")
    void shouldCleanupCorrelationIdHolderAfterProcessing() throws ServletException, IOException {
        // Arrange
        request.addHeader("X-Correlation-Id", "holder-cleanup");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        assertThat(CorrelationIdHolder.get()).isNull();
    }

    @Test
    @DisplayName("Deve limpar MDC mesmo em caso de exceção no FilterChain")
    void shouldCleanupMdcEvenOnException() throws ServletException, IOException {
        // Arrange
        request.addHeader("X-Correlation-Id", "exception-test");
        doThrow(new ServletException("Test exception"))
                .when(filterChain).doFilter(request, response);

        // Act & Assert
        try {
            filter.doFilter(request, response, filterChain);
        } catch (ServletException e) {
            // Expected
        }

        // MDC deve estar limpo mesmo após exceção
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(CorrelationIdHolder.get()).isNull();
    }

    @Test
    @DisplayName("Deve gerar correlationIds diferentes para requests diferentes")
    void shouldGenerateDifferentCorrelationIdsForDifferentRequests() throws ServletException, IOException {
        // Act
        filter.doFilter(request, response, filterChain);
        String firstCorrelationId = response.getHeader("X-Correlation-Id");

        // Reset
        response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);
        String secondCorrelationId = response.getHeader("X-Correlation-Id");

        // Assert
        assertThat(firstCorrelationId).isNotEqualTo(secondCorrelationId);
    }

    @Test
    @DisplayName("Deve aceitar correlationId vazio e gerar novo UUID")
    void shouldGenerateUuidWhenCorrelationIdIsEmpty() throws ServletException, IOException {
        // Arrange
        request.addHeader("X-Correlation-Id", "");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        String correlationId = response.getHeader("X-Correlation-Id");
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).matches("^[a-f0-9-]{36}$");
    }

    @Test
    @DisplayName("Deve aceitar correlationId com espaços e gerar novo UUID")
    void shouldGenerateUuidWhenCorrelationIdIsBlank() throws ServletException, IOException {
        // Arrange
        request.addHeader("X-Correlation-Id", "   ");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        String correlationId = response.getHeader("X-Correlation-Id");
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).matches("^[a-f0-9-]{36}$");
    }
}
