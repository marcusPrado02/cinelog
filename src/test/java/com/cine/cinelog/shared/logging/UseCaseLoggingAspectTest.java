package com.cine.cinelog.shared.logging;

import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.*;

class UseCaseLoggingAspectTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void aroundUseCase_success_returnsResultAndInvokesProceed() throws Throwable {
        UseCaseLoggingAspect aspect = new UseCaseLoggingAspect();

        // set configuration fields via reflection
        Field sampleRateField = UseCaseLoggingAspect.class.getDeclaredField("sampleRate");
        sampleRateField.setAccessible(true);
        sampleRateField.setDouble(aspect, 1.0); // ensure sampling does not skip

        Field slowThresholdField = UseCaseLoggingAspect.class.getDeclaredField("slowThresholdMs");
        slowThresholdField.setAccessible(true);
        slowThresholdField.setLong(aspect, 0L); // make slow condition true (covers that branch)

        // prepare mocks
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toShortString()).thenReturn("UseCase.execute()");
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getArgs()).thenReturn(new Object[] { "arg1", 2 });
        when(pjp.proceed()).thenReturn("OK_RESULT");

        // put some MDC values so code reads them without NPE
        MDC.put("trace_id", "trace-1");
        MDC.put("span_id", "span-1");

        Object result = aspect.aroundUseCase(pjp);

        assertEquals("OK_RESULT", result);
        verify(pjp, times(1)).proceed();
    }

    @Test
    void aroundUseCase_whenProceedThrows_exceptionIsPropagated() throws Throwable {
        UseCaseLoggingAspect aspect = new UseCaseLoggingAspect();

        // ensure sampling does not skip
        Field sampleRateField = UseCaseLoggingAspect.class.getDeclaredField("sampleRate");
        sampleRateField.setAccessible(true);
        sampleRateField.setDouble(aspect, 1.0);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toShortString()).thenReturn("UseCase.execute()");
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.getArgs()).thenReturn(new Object[0]);

        RuntimeException boom = new RuntimeException("boom");
        when(pjp.proceed()).thenThrow(boom);

        MDC.put("trace_id", "t2");
        MDC.put("span_id", "s2");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> aspect.aroundUseCase(pjp));
        assertSame(boom, thrown);
        verify(pjp, times(1)).proceed();
    }

    @Test
    void privateSafeMethods_handleUnprintableObjects() throws Exception {
        UseCaseLoggingAspect aspect = new UseCaseLoggingAspect();

        // access private safe(Object) method
        Method safeMethod = UseCaseLoggingAspect.class.getDeclaredMethod("safe", Object.class);
        safeMethod.setAccessible(true);

        Object badToString = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("nope");
            }
        };

        Object safeResult = safeMethod.invoke(aspect, badToString);
        assertEquals("<unprintable>", safeResult);

        // access private safeArgs(Object[]) method
        Method safeArgsMethod = UseCaseLoggingAspect.class.getDeclaredMethod("safeArgs", Object[].class);
        safeArgsMethod.setAccessible(true);

        // create args array that will cause Arrays.toString to attempt to call toString
        // on element (which throws)
        Object[] args = new Object[] { badToString };
        Object safeArgsResult = safeArgsMethod.invoke(aspect, (Object) args);
        // Arrays.toString may throw and then safeArgs returns "<args_unprintable>"
        assertEquals("<args_unprintable>", safeArgsResult);
    }
}