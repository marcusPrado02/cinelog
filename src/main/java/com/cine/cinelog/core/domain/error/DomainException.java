package com.cine.cinelog.core.domain.error;

import java.util.Map;

public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;
    private final String instance;

    public DomainException(ErrorCode errorCode, Map<String, Object> details) {
        super(details.toString());
        this.errorCode = errorCode;
        this.details = details;
        this.instance = null;
    }

    public DomainException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        super(details.toString(), cause);
        this.errorCode = errorCode;
        this.details = details;
        this.instance = null;
    }

    public DomainException(ErrorCode errorCode, Map<String, Object> details, String instance) {
        super(details.toString());
        this.errorCode = errorCode;
        this.details = details;
        this.instance = instance;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getInstance() {
        return instance;
    }

    public static DomainException of(ErrorCode errorCode, Map<String, Object> details) {
        return new DomainException(errorCode, details);
    }

    public static DomainException of(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        return new DomainException(errorCode, details, cause);
    }

    public static DomainException of(ErrorCode errorCode, Map<String, Object> details, String instance) {
        return new DomainException(errorCode, details, instance);
    }

    public static DomainException of(ErrorCode errorCode, String details) {
        return new DomainException(errorCode, Map.of("message", details));
    }

    public static DomainException of(ErrorCode ratingNotAllowed, String string, Map<String, Number> of) {
        return new DomainException(ratingNotAllowed, Map.of("message", string, "info", of));
    }

}