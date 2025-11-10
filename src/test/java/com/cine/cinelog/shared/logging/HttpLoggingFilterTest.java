package com.cine.cinelog.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HttpLoggingFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void clientIp_prefersXForwardedFor_firstInList() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        HttpLoggingFilter f = new HttpLoggingFilter();
        String ip = (String) invokePrivate(f, "clientIp", new Class[] { HttpServletRequest.class },
                new Object[] { req });
        assertEquals("1.2.3.4", ip);
    }

    @Test
    void clientIp_usesXRealIp_ifNoXFF() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn("9.9.9.9");
        HttpLoggingFilter f = new HttpLoggingFilter();
        String ip = (String) invokePrivate(f, "clientIp", new Class[] { HttpServletRequest.class },
                new Object[] { req });
        assertEquals("9.9.9.9", ip);
    }

    @Test
    void clientIp_usesRemoteAddr_asFallback() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("7.7.7.7");
        HttpLoggingFilter f = new HttpLoggingFilter();
        String ip = (String) invokePrivate(f, "clientIp", new Class[] { HttpServletRequest.class },
                new Object[] { req });
        assertEquals("7.7.7.7", ip);
    }

    @Test
    void mask_hidesBearerAndEmailAndPassword_and_truncates() throws Exception {
        HttpLoggingFilter f = new HttpLoggingFilter();

        // set maxLen small to exercise truncation
        setPrivateField(f, "maxLen", 10);

        String input = "Authorization: Bearer ABCDEFGHIJKLMNOPQRSTUVWXYZ user@example.com {\"password\":\"secretpass\"} longtext12345";
        String masked = (String) invokePrivate(f, "mask", new Class[] { String.class }, new Object[] { input });

        assertTrue(masked.contains("authorization: Bearer ***") || masked.contains("Bearer ***"));
        assertFalse(masked.contains("user@example.com"));
        assertFalse(masked.contains("secretpass"));
        assertTrue(masked.endsWith("...<truncated>"));
    }

    @Test
    void doFilterInternal_success_runsChain_and_clearsMdc() throws Exception {
        HttpLoggingFilter f = new HttpLoggingFilter();
        setPrivateField(f, "maxLen", 2000);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/x");
        when(req.getQueryString()).thenReturn("q=1");
        when(req.getHeader("User-Agent")).thenReturn("ua");
        when(req.getHeader("X-Forwarded-For")).thenReturn("2.2.2.2");
        when(req.getRemoteAddr()).thenReturn("ignored");

        HttpServletResponse originalResp = mock(HttpServletResponse.class);
        // stub writer and output stream so copyBodyToResponse can write
        StringWriter sw = new StringWriter();
        when(originalResp.getWriter()).thenReturn(new PrintWriter(sw));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(originalResp.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int b) {
                baos.write(b);
            }
        });

        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            ServletResponse resp = (ServletResponse) inv.getArguments()[1];
            assertTrue(resp instanceof ContentCachingResponseWrapper);
            HttpServletResponse wrapper = (HttpServletResponse) resp;
            wrapper.setStatus(201);
            wrapper.getWriter().write("ok");
            return null;
        }).when(chain).doFilter(any(), any());

        // call
        f.doFilterInternal(req, originalResp, chain);

        // after filter, MDC request_id should be removed
        assertNull(MDC.get("request_id"));
        verify(chain, times(1)).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_exceptionPropagates_and_clearsMdc() throws Exception {
        HttpLoggingFilter f = new HttpLoggingFilter();
        setPrivateField(f, "maxLen", 2000);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/err");
        when(req.getHeader("User-Agent")).thenReturn("ua");
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("1.1.1.1");

        HttpServletResponse originalResp = mock(HttpServletResponse.class);
        when(originalResp.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(originalResp.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int b) {
            }
        });

        FilterChain chain = mock(FilterChain.class);
        doThrow(new RuntimeException("boom")).when(chain).doFilter(any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> f.doFilterInternal(req, originalResp, chain));
        assertEquals("boom", ex.getMessage());
        assertNull(MDC.get("request_id"));
    }

    // --- helpers to call private methods / set private fields ---

    private Object invokePrivate(Object target, String name, Class<?>[] paramTypes, Object[] args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}