package com.cine.cinelog.shared.security;

import com.cine.cinelog.shared.observability.security.SecurityEventLogger;
import com.cine.cinelog.shared.observability.security.SecurityMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitFilter}.
 * <p>
 * Covers:
 * - Redis success path: headers set correctly
 * - Limit not exceeded: request passes through
 * - Limit exceeded: 429 returned + Retry-After header
 * - Redis failure: fallback to in-memory counter
 * - shouldNotFilter: actuator and swagger paths bypass filter
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Unit Tests")
class RateLimitFilterTest {

    @Mock
    private SecurityEventLogger securityEventLogger;

    @Mock
    private SecurityMetricsService securityMetrics;

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(securityEventLogger, securityMetrics, redisTemplate);
    }

    // ========== Happy Path ==========

    @Test
    @DisplayName("Should allow request and set rate limit headers when under limit")
    void shouldAllowRequestAndSetHeaders() throws Exception {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class)))
                .thenReturn(1L);
        when(redisTemplate.getExpire(anyString())).thenReturn(55L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/media");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
        assertThat(chain.getRequest()).isNotNull(); // chain was called
    }

    @Test
    @DisplayName("Should use AUTH_LIMIT (10) for /api/auth paths")
    void shouldUseAuthLimitForAuthPaths() throws Exception {
        // Given
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class)))
                .thenReturn(1L);
        when(redisTemplate.getExpire(anyString())).thenReturn(60L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    // ========== Rate Limit Exceeded ==========

    @Test
    @DisplayName("Should return 429 when rate limit is exceeded")
    void shouldReturn429WhenLimitExceeded() throws Exception {
        // Given — Redis returns a count above the default limit (100)
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class)))
                .thenReturn(101L);
        when(redisTemplate.getExpire(anyString())).thenReturn(30L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getContentAsString()).contains("Too Many Requests");
        assertThat(chain.getRequest()).isNull(); // chain was NOT called
        verify(securityMetrics).incrementRateLimit("general");
    }

    @Test
    @DisplayName("Should return 429 when auth limit is exceeded")
    void shouldReturn429WhenAuthLimitExceeded() throws Exception {
        // Given — 11 requests to auth endpoint (limit = 10)
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class)))
                .thenReturn(11L);
        when(redisTemplate.getExpire(anyString())).thenReturn(45L);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(429);
        verify(securityMetrics).incrementRateLimit("auth");
    }

    // ========== Redis Fallback ==========

    @Test
    @DisplayName("Should fallback to in-memory counter when Redis is unavailable")
    void shouldFallbackToInMemoryWhenRedisUnavailable() throws Exception {
        // Given — Redis throws exception
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/media");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When — should not throw
        filter.doFilter(request, response, chain);

        // Then — filter degrades gracefully; request still passes
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(chain.getRequest()).isNotNull();
    }

    // ========== shouldNotFilter ==========

    @Test
    @DisplayName("Should skip filter for /actuator/health")
    void shouldSkipActuatorHealth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Redis never called
        verifyNoInteractions(redisTemplate);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Should skip filter for /swagger-ui paths")
    void shouldSkipSwaggerUi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verifyNoInteractions(redisTemplate);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Should skip filter for /v3/api-docs")
    void shouldSkipApiDocs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs/swagger-config");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verifyNoInteractions(redisTemplate);
    }

    // ========== X-Forwarded-For ==========

    @Test
    @DisplayName("Should use first IP from X-Forwarded-For header")
    void shouldUseFirstIpFromXForwardedFor() throws Exception {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class)))
                .thenReturn(1L);
        when(redisTemplate.getExpire(anyString())).thenReturn(60L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/media");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 172.16.0.1, 192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // Verify the Redis key used the correct IP (first in XFF)
        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                argThat(keys -> keys.toString().contains("10.0.0.1")),
                any(String.class));
    }
}
