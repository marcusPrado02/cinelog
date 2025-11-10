package com.cine.cinelog.shared.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationHeaderFilterTest {

    private Tracer tracer;
    private CorrelationHeaderFilter filter;
    private ServletRequest req;
    private ServletResponse res;
    private HttpServletResponse httpRes;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        MDC.clear();
        tracer = mock(Tracer.class);
        filter = new CorrelationHeaderFilter(tracer);
        req = mock(ServletRequest.class);
        res = mock(ServletResponse.class);
        httpRes = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void when_currentSpan_is_null_should_set_requestId_and_not_trace_or_span_headers() throws Exception {
        when(tracer.currentSpan()).thenReturn(null);

        filter.doFilter(req, httpRes, chain);

        verify(chain).doFilter(req, httpRes);

        // X-Request-Id must be set with some non-null value
        ArgumentCaptor<String> reqIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpRes).setHeader(eq("X-Request-Id"), reqIdCaptor.capture());
        assertNotNull(reqIdCaptor.getValue());
        assertFalse(reqIdCaptor.getValue().isEmpty());

        // trace/span headers should not be set
        verify(httpRes, never()).setHeader(eq("X-Trace-Id"), anyString());
        verify(httpRes, never()).setHeader(eq("X-Span-Id"), anyString());

        // requestId must be removed from MDC after processing
        assertNull(MDC.get("requestId"));
    }

    @Test
    void when_currentSpan_present_and_txId_in_mdc_should_set_all_headers_and_clean_requestId() throws Exception {
        Span span = mock(Span.class);
        TraceContext spanContext = mock(TraceContext.class);
        when(spanContext.traceId()).thenReturn("trace-123");
        when(spanContext.spanId()).thenReturn("span-456");
        when(span.context()).thenReturn(spanContext);
        when(tracer.currentSpan()).thenReturn(span);

        // put a txId in MDC before invoking filter
        MDC.put("txId", "tx-999");

        filter.doFilter(req, httpRes, chain);

        verify(chain).doFilter(req, httpRes);

        // trace/span headers set
        verify(httpRes).setHeader("X-Trace-Id", "trace-123");
        verify(httpRes).setHeader("X-Span-Id", "span-456");

        // request id set
        ArgumentCaptor<String> reqIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpRes).setHeader(eq("X-Request-Id"), reqIdCaptor.capture());
        assertNotNull(reqIdCaptor.getValue());

        // tx id header set
        verify(httpRes).setHeader("X-Tx-Id", "tx-999");

        // requestId removed but txId remains untouched
        assertNull(MDC.get("requestId"));
        assertEquals("tx-999", MDC.get("txId"));
    }
}