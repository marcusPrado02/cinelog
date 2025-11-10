package com.cine.cinelog.shared.logging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class WebLoggingAspectTest {

    private Logger mockLogger;

    @Before
    public void setup() throws Exception {
        // create mock logger and inject into the private static final field
        mockLogger = mock(Logger.class);
        Field logField = WebLoggingAspect.class.getDeclaredField("log");
        logField.setAccessible(true);

        // remove final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(logField, logField.getModifiers() & ~Modifier.FINAL);

        logField.set(null, mockLogger);
    }

    @After
    public void cleanup() {
        MDC.clear();
    }

    @Test
    public void aroundController_shouldReturnResult_andLogStartAndSuccess() throws Throwable {
        // arrange
        WebLoggingAspect aspect = new WebLoggingAspect();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toShortString()).thenReturn("MyController.myEndpoint()");
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getArgs()).thenReturn(new Object[] { "user@example.com", "{\"password\":\"secret\"}" });
        when(pjp.proceed()).thenReturn("OK");

        // populate MDC
        MDC.put("trace_id", "trace-123");
        MDC.put("span_id", "span-456");

        // act
        Object result = aspect.aroundController(pjp);

        // assert
        // method returns the same result
        assert "OK".equals(result);

        // verify start and success info logs were emitted (two info calls)
        verify(mockLogger, atLeast(1)).info(anyString(), any(Object[].class));
        verify(mockLogger, atLeast(1)).info(anyString(), any(), any(), any(), any());

        // verify no error log
        verify(mockLogger, never()).error(anyString(), any(Object[].class));
    }

    @Test(expected = RuntimeException.class)
    public void aroundController_shouldLogError_andRethrowException() throws Throwable {
        // arrange
        WebLoggingAspect aspect = new WebLoggingAspect();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toShortString()).thenReturn("MyController.errorEndpoint()");
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getArgs()).thenReturn(new Object[] { "input" });
        when(pjp.proceed()).thenThrow(new RuntimeException("boom"));

        // populate MDC
        MDC.put("trace_id", "t-x");
        MDC.put("span_id", "s-y");

        try {
            // act - should throw
            aspect.aroundController(pjp);
        } finally {
            // assert that error log was produced
            // capture the error log invocation
            verify(mockLogger, atLeast(1)).error(anyString(), any(Object[].class));
        }
    }

    @Test
    public void sanitize_shouldMaskSensitiveData_inArgsAndResultLogged() throws Throwable {
        // This test ensures sanitizer runs without throwing and logger receives
        // sanitized values
        WebLoggingAspect aspect = new WebLoggingAspect();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toShortString()).thenReturn("MyController.sanitizeEndpoint()");
        when(pjp.getSignature()).thenReturn(sig);
        // include email and Authorization header and JSON password to be sanitized
        when(pjp.getArgs()).thenReturn(new Object[] { "contact: user@domain.com", "Authorization: Bearer abc.def.ghi",
                "{\"password\":\"mypw\"}" });
        when(pjp.proceed()).thenReturn("{\"secret\":\"token-xyz\"}");

        // act
        Object result = aspect.aroundController(pjp);

        // assert result unchanged
        assert result != null;

        // capture the web_success log call to inspect sanitized result argument
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(mockLogger, atLeast(1)).info(anyString(), captor.capture());

        // find captured varargs arrays and look for sanitized tokens (simple check)
        boolean foundSanitized = false;
        for (Object[] varargs : captor.getAllValues()) {
            for (Object o : varargs) {
                if (o instanceof String) {
                    String s = (String) o;
                    if (s.contains("***@***") || s.contains("authorization: Bearer ***")
                            || s.contains("\"password\":\"***\"")) {
                        foundSanitized = true;
                        break;
                    }
                }
            }
            if (foundSanitized)
                break;
        }

        assert foundSanitized;
    }
}