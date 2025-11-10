package com.cine.cinelog.shared.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class RequestContextTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void getReturnsNullWhenNotSet() {
        assertNull(RequestContext.get(), "RequestContext.get() should be null when not set");
    }

    @Test
    void setGetAndClearBehavior() {
        RequestContext ctx = new RequestContext(
                123L,
                "user@example.com",
                "web",
                "1.2.3",
                "trace-1",
                "span-1",
                "127.0.0.1",
                "JUnit",
                "tx-1",
                Boolean.TRUE);

        RequestContext.set(ctx);
        assertSame(ctx, RequestContext.get(), "RequestContext.get() should return the exact instance set");

        // verify fields are preserved
        RequestContext current = RequestContext.get();
        assertEquals(123L, current.userId);
        assertEquals("user@example.com", current.userEmail);
        assertEquals("web", current.sourceApp);
        assertEquals("1.2.3", current.appVersion);
        assertEquals("trace-1", current.traceId);
        assertEquals("span-1", current.spanId);
        assertEquals("127.0.0.1", current.clientIp);
        assertEquals("JUnit", current.userAgent);
        assertEquals("tx-1", current.txId);
        assertTrue(current.diagSql);

        RequestContext.clear();
        assertNull(RequestContext.get(), "RequestContext should be null after clear()");
    }

    @Test
    void threadLocalIsolationBetweenThreads() throws Exception {
        RequestContext mainCtx = new RequestContext(
                1L, "main@x", "mainApp", "v", "trace-main", "span-main", "ip-main", "ua-main", "tx-main",
                Boolean.FALSE);
        RequestContext.set(mainCtx);

        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<RequestContext> otherThreadObserved = new AtomicReference<>();
        AtomicReference<RequestContext> otherThreadSet = new AtomicReference<>();

        Thread t = new Thread(() -> {
            // before setting anything in this thread, should be null
            otherThreadObserved.set(RequestContext.get());
            // now set a different context in this thread
            RequestContext ctx = new RequestContext(
                    2L, "other@x", "otherApp", "v2", "trace-other", "span-other", "ip-other", "ua-other", "tx-other",
                    Boolean.TRUE);
            RequestContext.set(ctx);
            otherThreadSet.set(RequestContext.get());
            started.countDown();
            // keep thread alive briefly to avoid premature GC/cleanup
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        });

        t.start();
        // wait for other thread to set its value
        boolean signaled = started.await(1, TimeUnit.SECONDS);
        assertTrue(signaled, "Other thread did not complete in time");

        // other thread saw null before setting
        assertNull(otherThreadObserved.get(), "Other thread should observe null before it sets its own context");

        // main thread should still have its context
        assertSame(mainCtx, RequestContext.get(), "Main thread context must remain unchanged");

        // the context set in other thread should not affect main thread
        assertEquals(2L, otherThreadSet.get().userId);
        assertEquals("other@x", otherThreadSet.get().userEmail);
        assertSame(mainCtx, RequestContext.get(), "Main thread must still reference its original context");

        t.join();
    }
}