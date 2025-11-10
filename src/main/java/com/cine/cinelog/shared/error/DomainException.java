package com.cine.cinelog.shared.error;

/**
 * Exceção genérica de domínio.
 */
public class DomainException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public DomainException(String message) {
        this(message, "domain_error", 422);
    }

    public DomainException(String message, String code, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}