package com.cine.cinelog.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import java.io.IOException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObservabilityContextFilterTest {

    private final ObservabilityContextFilter filter = new ObservabilityContextFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void doFilter_populatesMdc_fromHeaders_and_is_cleared_after() throws IOException, ServletException {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("X-Trace-Id")).thenReturn("trace-123");
        when(req.getHeader("X-Span-Id")).thenReturn("span-456");
        when(req.getHeader("User-Agent")).thenReturn("JUnit-Agent");
        when(req.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/test");
        when(req.getQueryString()).thenReturn("a=1");
        when(res.getStatus()).thenReturn(201);

        // During chain.doFilter we assert MDC is populated as expected
        doAnswer(invocation -> {
            assertEquals("trace-123", MDC.get("traceId"));
            assertEquals("span-456", MDC.get("spanId"));
            assertEquals("GET", MDC.get("method"));
            assertEquals("/api/test", MDC.get("path"));
            assertEquals("a=1", MDC.get("query"));
            assertEquals("10.0.0.1", MDC.get("clientIp"));
            assertEquals("JUnit-Agent", MDC.get("userAgent"));
            // status and tookMs are populated only after chain returns (in finally)
            assertNull(MDC.get("status"));
            assertNull(MDC.get("tookMs"));
            return null;
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(req, res, chain);

        // After filter completes MDC must be cleared
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("spanId"));
        assertNull(MDC.get("method"));
        assertNull(MDC.get("path"));
        assertNull(MDC.get("query"));
        assertNull(MDC.get("clientIp"));
        assertNull(MDC.get("userAgent"));
        assertNull(MDC.get("status"));
        assertNull(MDC.get("tookMs"));

        // verify logger was invoked (no exception thrown)
        verify(chain, times(1)).doFilter(req, res);
        verify(res, atLeastOnce()).getStatus();
    }

    @Test
    void doFilter_generatesIds_and_uses_remoteAddr_when_headers_missing() throws IOException, ServletException {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // No trace/span headers
        when(req.getHeader("X-Trace-Id")).thenReturn(null);
        when(req.getHeader("X-Span-Id")).thenReturn(null);
        // User-Agent absent -> safe("") expected
        when(req.getHeader("User-Agent")).thenReturn(null);
        // X-Forwarded-For absent -> use remoteAddr
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/no-headers");
        when(req.getQueryString()).thenReturn(null);
        when(res.getStatus()).thenReturn(200);

        doAnswer(invocation -> {
            String traceId = MDC.get("traceId");
            String spanId = MDC.get("spanId");
            assertNotNull(traceId);
            assertNotNull(spanId);
            // newId() produces a UUID without hyphens -> length 32
            assertEquals(32, traceId.length());
            assertEquals(32, spanId.length());
            assertEquals("127.0.0.1", MDC.get("clientIp"));
            assertEquals("", MDC.get("userAgent")); // safe(null) -> empty string
            assertEquals("POST", MDC.get("method"));
            assertEquals("/no-headers", MDC.get("path"));
            assertEquals("", MDC.get("query"));
            return null;
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(req, res, chain);

        // MDC cleared after filter
        assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
    }
}