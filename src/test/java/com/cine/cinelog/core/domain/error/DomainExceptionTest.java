package com.cine.cinelog.core.domain.error;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DomainExceptionTest {

    @Test
    void constructor_setsFields() {
        Map<String, Object> details = Map.of("key", "value");
        DomainException ex = new DomainException(null, details);

        assertNull(ex.getErrorCode());
        assertSame(details, ex.getDetails());
        assertNull(ex.getInstance());
        assertEquals(details.toString(), ex.getMessage());
    }

    @Test
    void constructor_withCause_setsCause() {
        Map<String, Object> details = Map.of("k", 1);
        Throwable cause = new IllegalStateException("boom");
        DomainException ex = new DomainException(null, details, cause);

        assertSame(cause, ex.getCause());
        assertEquals(details.toString(), ex.getMessage());
        assertSame(details, ex.getDetails());
    }

    @Test
    void constructor_withInstance_setsInstance() {
        Map<String, Object> details = Map.of("k", 2);
        DomainException ex = new DomainException(null, details, "instance-id");

        assertEquals("instance-id", ex.getInstance());
        assertSame(details, ex.getDetails());
    }

    @Test
    void static_of_map_and_overloads() {
        Map<String, Object> details = Map.of("a", 1);

        DomainException ex1 = DomainException.of(null, details);
        assertSame(details, ex1.getDetails());

        RuntimeException cause = new RuntimeException("cause");
        DomainException ex2 = DomainException.of(null, details, cause);
        assertSame(cause, ex2.getCause());
        assertEquals(details.toString(), ex2.getMessage());

        DomainException ex3 = DomainException.of(null, details, "inst");
        assertEquals("inst", ex3.getInstance());
    }

    @Test
    void static_of_message_createsDetailsWithMessage() {
        DomainException ex = DomainException.of(null, "some message");

        assertNotNull(ex.getDetails());
        assertEquals("some message", ex.getDetails().get("message"));
    }

    @Test
    void static_of_message_with_info_createsDetails() {
        Map<String, Number> info = Map.of("x", 5);
        DomainException ex = DomainException.of(null, "m", info);

        assertNotNull(ex.getDetails());
        assertEquals("m", ex.getDetails().get("message"));
        assertSame(info, ex.getDetails().get("info"));
    }
}
