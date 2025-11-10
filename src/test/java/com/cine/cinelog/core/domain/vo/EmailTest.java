package com.cine.cinelog.core.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.cine.cinelog.core.domain.error.DomainException;
import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void validEmailConstructorShouldSetValue() {
        Email e = new Email("user@example.com");
        assertEquals("user@example.com", e.value());
    }

    @Test
    void validEmailOfShouldCreateInstance() {
        Email e = Email.of("name.surname@sub.domain.com");
        assertEquals("name.surname@sub.domain.com", e.value());
    }

    @Test
    void nullEmailShouldThrowDomainException() {
        DomainException ex = assertThrows(DomainException.class, () -> new Email(null));
        assertTrue(ex.getMessage().contains("Email inválido"));
    }

    @Test
    void invalidEmailsShouldThrowDomainException() {
        String[] invalids = {
                "",
                "plainaddress",
                "noatsign.com",
                "user@.com",
                "user@com",
                "user @example.com",
                "user@example",
                "user@example..com",
                "user@@example.com",
                " user@example.com ",
                "user@ example.com"
        };

        for (String inv : invalids) {
            DomainException ex = assertThrows(DomainException.class, () -> new Email(inv));
            assertTrue(ex.getMessage().contains("Email inválido"), "expected message for input: " + inv);
        }
    }
}