package com.cine.cinelog.shared.error;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.DuplicateException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.error.ForbiddenOperationException;
import com.cine.cinelog.shared.security.BusinessLimitExceededException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

/**
 * Produz respostas no formato RFC 7807 (application/problem+json).
 * Sempre inclui: type, title, status, detail, instance, e extensões:
 * - traceId, timestamp, path, errorCode
 * - fieldErrors (quando houver validação)
 *
 * Integra com MessageSource para internacionalizar mensagens com base em:
 * - ErrorCode.messageKey + ".title" → title
 * - ErrorCode.messageKey + ".detail" → detail
 */
/**
 * Classe de configuração Spring para gerenciamento de globalexceptionhandler.
 *
 * <p>
 * Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.
 * </p>
 *
 * @since 1.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI TYPE_VALIDATION = URI.create("https://api.cinelog.com/errors/validation");
    private static final URI TYPE_NOT_FOUND = URI.create("https://api.cinelog.com/errors/not-found");
    private static final URI TYPE_CONFLICT = URI.create("https://api.cinelog.com/errors/conflict");
    private static final URI TYPE_DOMAIN = URI.create("https://api.cinelog.com/errors/domain");
    private static final URI TYPE_BAD_REQUEST = URI.create("https://api.cinelog.com/errors/bad-request");
    private static final URI TYPE_INTERNAL = URI.create("https://api.cinelog.com/errors/internal");

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // ===== Validação (DTO @Valid) =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpServletRequest req) {
        log.warn("Erro de validação de payload. Path: {}, ErrorCount: {}",
                req.getRequestURI(), ex.getBindingResult().getErrorCount());

        var status = HttpStatus.BAD_REQUEST;

        var locale = LocaleContextHolder.getLocale();
        String detail = messageSource.getMessage(
                "error.validation.payloadInvalid.detail",
                null,
                "Payload inválido.",
                locale);

        String title = messageSource.getMessage(
                "error.validation.payloadInvalid.title",
                null,
                "Validation failed",
                locale);

        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(TYPE_VALIDATION);
        setCommon(pd, req, ErrorCode.GEN_VALIDATION.code);

        List<Map<String, Object>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        pd.setProperty("fieldErrors", fieldErrors);

        log.debug("Resposta de erro de validação gerada. Status: {}, FieldErrors: {}", status, fieldErrors.size());
        return pd;
    }

    // ===== Validação (ConstraintViolation fora de DTO) =====
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest req) {
        var status = HttpStatus.BAD_REQUEST;

        var locale = LocaleContextHolder.getLocale();
        String detail = messageSource.getMessage(
                "error.validation.parametersInvalid.detail",
                null,
                "Parâmetros inválidos.",
                locale);

        String title = messageSource.getMessage(
                "error.validation.parametersInvalid.title",
                null,
                "Constraint violation",
                locale);

        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(TYPE_VALIDATION);
        setCommon(pd, req, ErrorCode.GEN_VALIDATION.code);

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

        var locale = LocaleContextHolder.getLocale();
        String detail = messageSource.getMessage(
                "error.badRequest.malformed.detail",
                null,
                "Requisição malformada ou tipos incompatíveis.",
                locale);

        String title = messageSource.getMessage(
                "error.badRequest.malformed.title",
                null,
                "Bad request",
                locale);

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle(title);
        pd.setType(TYPE_BAD_REQUEST);
        setCommon(pd, req, ErrorCode.GEN_VALIDATION.code);
        return pd;
    }

    // ===== Regras de domínio padronizadas (DomainException) =====
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex, HttpServletRequest req) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = resolveStatus(errorCode);

        log.warn("Exceção de domínio. Path: {}, ErrorCode: {}, Status: {}, Message: {}",
                req.getRequestURI(), errorCode, status, ex.getMessage());

        var locale = LocaleContextHolder.getLocale();

        // Se houver "message" explícita nos details, usamos como texto de detail.
        String explicitMessage = null;
        if (ex.getDetails() != null) {
            Object msg = ex.getDetails().get("message");
            if (msg != null) {
                explicitMessage = msg.toString();
            }
        }

        // Title i18n
        String titleKey = errorCode.messageKey + ".title";
        String localizedTitle = messageSource.getMessage(
                titleKey,
                ex.getArgs(),
                errorCode.title, // fallback
                locale);

        // Detail i18n (ou explicitMessage se houver)
        String localizedDetail;
        if (explicitMessage != null) {
            localizedDetail = explicitMessage;
        } else {
            String detailKey = errorCode.messageKey + ".detail";
            localizedDetail = messageSource.getMessage(
                    detailKey,
                    ex.getArgs(),
                    errorCode.title, // fallback básico
                    locale);
        }

        var pd = ProblemDetail.forStatus(status);
        pd.setTitle(localizedTitle);
        pd.setDetail(localizedDetail);
        pd.setType(TYPE_DOMAIN);

        // instance: se vier da exception, usamos; senão, usamos o path
        if (ex.getInstance() != null) {
            pd.setInstance(URI.create(ex.getInstance()));
        } else {
            pd.setInstance(URI.create(req.getRequestURI()));
        }

        setCommon(pd, req, errorCode.code);

        // expõe o mapa de detalhes como extensão
        if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
            pd.setProperty("details", ex.getDetails());
        }
        // Também expõe a key de i18n base, caso o front queira reaproveitar
        pd.setProperty("i18nKey", errorCode.messageKey);

        return pd;
    }

    // ===== Not Found (JPA / Optional / etc.) =====
    @ExceptionHandler({ EntityNotFoundException.class, NoSuchElementException.class })
    public ProblemDetail handleNotFound(Exception ex, HttpServletRequest req) {

        var locale = LocaleContextHolder.getLocale();
        String detail = messageSource.getMessage(
                "error.notFound.generic.detail",
                null,
                "Recurso não encontrado.",
                locale);

        String title = messageSource.getMessage(
                "error.notFound.generic.title",
                null,
                "Not found",
                locale);

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail);
        pd.setTitle(title);
        pd.setType(TYPE_NOT_FOUND);
        setCommon(pd, req, ErrorCode.GEN_NOT_FOUND.code);
        return pd;
    }

    // ===== Proibido (ainda usando exceções antigas) =====
    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle(ErrorCode.OPERATION_NOT_ALLOWED.title); // pode ser i18n no futuro
        pd.setDetail(ex.getMessage());
        pd.setType(TYPE_DOMAIN);
        setCommon(pd, req, ErrorCode.OPERATION_NOT_ALLOWED.code);
        return pd;
    }

    // ===== Conflito de dados (duplicidade de chave, etc.) =====
    @ExceptionHandler(DuplicateException.class)
    public ProblemDetail handleDuplicate(DuplicateException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle(ErrorCode.GEN_CONFLICT.title); // idem
        pd.setDetail(ex.getMessage());
        pd.setType(TYPE_CONFLICT);
        setCommon(pd, req, ErrorCode.GEN_CONFLICT.code);
        return pd;
    }

    // ===== Conflitos/violação de integridade no banco =====
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {

        var locale = LocaleContextHolder.getLocale();
        String detail = messageSource.getMessage(
                "error.constraint.integrity.detail",
                null,
                "Operação violou uma restrição de integridade.",
                locale);

        String title = messageSource.getMessage(
                "error.constraint.integrity.title",
                null,
                "Conflict",
                locale);

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        pd.setTitle(title);
        pd.setType(TYPE_CONFLICT);
        setCommon(pd, req, ErrorCode.GEN_CONSTRAINT.code);

        // A02: não expor detalhes do banco (schema, constraint names) ao cliente.
        // Log interno para troubleshooting.
        String constraint = Optional.ofNullable(ex.getCause())
                .map(Throwable::getMessage)
                .orElse(null);
        if (constraint != null && !constraint.isBlank() && log.isDebugEnabled()) {
            log.debug("DataIntegrity detail (não exposto ao cliente): {}", truncate(constraint, 400));
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
        setCommon(pd, req, "SPRING_ERROR");
        return pd;
    }

    // ===== A04: Limite de negócio excedido → 429 =====
    @ExceptionHandler(BusinessLimitExceededException.class)
    public ProblemDetail handleBusinessLimitExceeded(BusinessLimitExceededException ex,
            HttpServletRequest req) {
        log.warn("Business limit excedido. Path: {}, Message: {}",
                req.getRequestURI(), ex.getMessage());

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setTitle("Too Many Requests");
        pd.setType(URI.create("https://api.cinelog.com/errors/rate-limit"));
        setCommon(pd, req, "BUSINESS_LIMIT_EXCEEDED");
        return pd;
    }

    // ===== Fallback 500 =====
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado não tratado. Path: {}, ExceptionType: {}, Message: {}",
                req.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        var locale = LocaleContextHolder.getLocale();
        String detail = messageSource.getMessage(
                "error.internal.unexpected.detail",
                null,
                "Ocorreu um erro inesperado.",
                locale);

        String title = messageSource.getMessage(
                "error.internal.unexpected.title",
                null,
                "Internal error",
                locale);

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail);
        pd.setTitle(title);
        pd.setType(TYPE_INTERNAL);
        setCommon(pd, req, ErrorCode.GEN_UNEXPECTED.code);

        log.debug("Resposta de erro interno gerada. Status: 500");
        return pd;
    }

    // ===== Helpers =====

    private void setCommon(ProblemDetail pd, HttpServletRequest req, String errorCode) {
        if (pd.getInstance() == null) {
            pd.setInstance(URI.create(req.getRequestURI()));
        }
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("traceId", Optional.ofNullable(MDC.get("traceId")).orElse(""));
        pd.setProperty("spanId", Optional.ofNullable(MDC.get("spanId")).orElse(""));
        pd.setProperty("errorCode", errorCode);
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

    private HttpStatus resolveStatus(ErrorCode code) {
        return switch (code) {
            // Not found
            case GEN_NOT_FOUND, MEDIA_NOT_FOUND, USER_NOT_FOUND -> HttpStatus.NOT_FOUND;

            // Conflito
            case GEN_CONFLICT, MEDIA_DUPLICATE -> HttpStatus.CONFLICT;

            // Validação / argumento ruim
            case GEN_VALIDATION,
                    MEDIA_TITLE_REQUIRED,
                    MEDIA_YEAR_OUT_OF_RANGE,
                    MEDIA_TITLE_TOO_LONG,
                    USER_EMAIL_INVALID,
                    INVALID_ARGUMENT ->
                HttpStatus.BAD_REQUEST;

            // RATING / coisas de domínio inválido → 422
            case RATING_OUT_OF_RANGE,
                    RATING_NOT_ALLOWED,
                    INVALID_RELEASE_YEAR,
                    INVALID_WATCH_ENTRY,
                    OPERATION_NOT_ALLOWED ->
                HttpStatus.UNPROCESSABLE_ENTITY;

            // Genérico
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
