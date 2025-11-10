package com.cine.cinelog.core.domain.vo;

import java.util.regex.Pattern;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

public final class Email {
    private static final Pattern P = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final String value;

    public Email(String value) {
        if (value == null || !P.matcher(value).matches()) {
            throw DomainException.of(ErrorCode.USER_EMAIL_INVALID, "Email inválido");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Email of(String v) {
        return new Email(v);
    }
}
