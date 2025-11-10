package com.cine.cinelog.shared.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Produz respostas no formato RFC 7807 (application/problem+json).
 * Sempre inclui: type, title, status, detail, instance, e extensões:
 * - traceId, timestamp, path, errorCode
 * - fieldErrors (quando houver validação)
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final URI TYPE_VALIDATION = URI.create("https://api.cinelog.com/errors/validation");
    private static final URI TYPE_NOT_FOUND = URI.create("https://api.cinelog.com/errors/not-found");
    private static final URI TYPE_CONFLICT = URI.create("https://api.cinelog.com/errors/conflict");
    private static final URI TYPE_DOMAIN = URI.create("https://api.cinelog.com/errors/domain");
    private static final URI TYPE_BAD_REQUEST = URI.create("https://api.cinelog.com/errors/bad-request");
    private static final URI TYPE_INTERNAL = URI.create("https://api.cinelog.com/errors/internal");

    // ===== Validação (DTO @Valid) =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpServletRequest req) {
        var status = HttpStatus.BAD_REQUEST;
        var pd = ProblemDetail.forStatusAndDetail(status, "Payload inválido.");
        pd.setTitle("Validation failed");
        pd.setType(TYPE_VALIDATION);
        setCommon(pd, req, "validation_error");

        List<Map<String, Object>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    // ===== Validação (ConstraintViolation fora de DTO) =====
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest req) {
        var status = HttpStatus.BAD_REQUEST;
        var pd = ProblemDetail.forStatusAndDetail(status, "Parâmetros inválidos.");
        pd.setTitle("Constraint violation");
        pd.setType(TYPE_VALIDATION);
        setCommon(pd, req, "constraint_violation");

        List<Map<String, Object>> violations = ex.getConstraintViolations()
                .stream()
                .map(this::toConstraintError)
                .toList();

        pd.setProperty("fieldErrors", violations);
        return pd;
    }

    // ===== JSON malformado / tipos errados =====
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleBadJson(HttpMessageNotReadableException ex,
            HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Requisição malformada ou tipos incompatíveis.");
        pd.setTitle("Bad request");
        pd.setType(TYPE_BAD_REQUEST);
        setCommon(pd, req, "bad_request");
        return pd;
    }

    // ===== Regras de negócio =====
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex, HttpServletRequest req) {
        var status = HttpStatus.valueOf(ex.getHttpStatus());
        var pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle("Domain rule violation");
        pd.setType(TYPE_DOMAIN);
        setCommon(pd, req, ex.getCode());
        return pd;
    }

    // ===== Not Found (tanto JPA quanto custom) =====
    @ExceptionHandler({ EntityNotFoundException.class, NoSuchElementException.class })
    public ProblemDetail handleNotFound(Exception ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
        pd.setTitle("Not found");
        pd.setType(TYPE_NOT_FOUND);
        setCommon(pd, req, "not_found");
        return pd;
    }

    // ===== Conflitos/violação de integridade =====
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Operação violou uma restrição de integridade.");
        pd.setTitle("Conflict");
        pd.setType(TYPE_CONFLICT);
        setCommon(pd, req, "integrity_violation");

        /**
         * Detalhe da violação de integridade (mensagem do banco de dados).
         */
        String constraint = Optional.ofNullable(ex.getCause())
                .map(Throwable::getMessage)
                .orElse(null);
        if (constraint != null && !constraint.isBlank()) {
            pd.setProperty("dbMessage", truncate(constraint, 400));
        }
        return pd;
    }

    // ===== Erros que já são ErrorResponseException (Spring) =====
    @ExceptionHandler(ErrorResponseException.class)
    public ProblemDetail handleErrorResponseException(ErrorResponseException ex, HttpServletRequest req) {
        ProblemDetail pd = ex.getBody();
        if (pd == null) {
            pd = ProblemDetail.forStatus(ex.getStatusCode());
        }
        if (pd.getType() == null) {
            pd.setType(TYPE_BAD_REQUEST);
        }
        if (pd.getTitle() == null) {
            pd.setTitle(ex.getStatusCode().toString());
        }
        setCommon(pd, req, "spring_error");
        return pd;
    }

    // ===== Fallback 500 =====
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnknown(Exception ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado.");
        pd.setTitle("Internal error");
        pd.setType(TYPE_INTERNAL);
        setCommon(pd, req, "internal_error");
        return pd;
    }

    // ===== Helpers =====

    private void setCommon(ProblemDetail pd, HttpServletRequest req, String code) {
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("traceId", Optional.ofNullable(MDC.get("traceId")).orElse(""));
        pd.setProperty("spanId", Optional.ofNullable(MDC.get("spanId")).orElse(""));
        pd.setProperty("errorCode", code);
    }

    private Map<String, Object> toFieldError(FieldError fe) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", fe.getField());
        m.put("message", Optional.ofNullable(fe.getDefaultMessage()).orElse("invalid"));
        m.put("rejectedValue", fe.getRejectedValue());
        m.put("object", fe.getObjectName());
        return m;
    }

    private Map<String, Object> toConstraintError(ConstraintViolation<?> cv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : null);
        m.put("message", cv.getMessage());
        m.put("rejectedValue", cv.getInvalidValue());
        return m;
    }

    private static String truncate(String s, int max) {
        if (s == null)
            return null;
        return s.length() <= max ? s : s.substring(0, max) + "...(trunc)";
    }
}
