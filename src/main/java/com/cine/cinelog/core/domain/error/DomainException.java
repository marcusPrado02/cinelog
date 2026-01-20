package com.cine.cinelog.core.domain.error;

import java.util.Map;

/**
 * Exceção de domínio padronizada.
 *
 * - Sempre carrega um {@link ErrorCode}.
 * - details: mapa com informações adicionais (usado em logs e para o client).
 * - instance: opcional, para indicar um identificador de recurso/URI
 * específico.
 * - args: parâmetros opcionais para interpolação em mensagens i18n.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;
    private final String instance;
    private final Object[] args;

    private DomainException(
            ErrorCode errorCode,
            Map<String, Object> details,
            String instance,
            Throwable cause,
            Object[] args) {

        super(details != null && !details.isEmpty()
                ? details.toString()
                : (errorCode != null ? errorCode.title : null),
                cause);

        this.errorCode = errorCode;
        this.details = details != null ? details : Map.of();
        this.instance = instance;
        this.args = args != null ? args : new Object[0];
    }

    // ==== Construtores públicos mantendo compatibilidade ====

    public DomainException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode, details, null, null, null);
    }

    public DomainException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        this(errorCode, details, null, cause, null);
    }

    public DomainException(ErrorCode errorCode, Map<String, Object> details, String instance) {
        this(errorCode, details, instance, null, null);
    }

    // ==== Getters ====

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getInstance() {
        return instance;
    }

    /**
     * Parâmetros opcionais usados para interpolação em mensagens i18n.
     */
    public Object[] getArgs() {
        return args;
    }

    // ==== Fábricas estáticas ====

    public static DomainException of(ErrorCode errorCode, Map<String, Object> details) {
        return new DomainException(errorCode, details, null, null, null);
    }

    public static DomainException of(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        return new DomainException(errorCode, details, null, cause, null);
    }

    public static DomainException of(ErrorCode errorCode, Map<String, Object> details, String instance) {
        return new DomainException(errorCode, details, instance, null, null);
    }

    /**
     * Fábrica simples com detalhe textual em "message".
     * Útil para mensagens mais específicas (ainda não i18n).
     */
    public static DomainException of(ErrorCode errorCode, String details) {
        return new DomainException(errorCode, Map.of("message", details), null, null, null);
    }

    /**
     * Fábrica com mensagem e mapa adicional. Mantida por compatibilidade.
     */
    public static DomainException of(ErrorCode errorCode, String details, Map<String, Number> info) {
        return new DomainException(errorCode, Map.of("message", details, "info", info), null, null, null);
    }

    /**
     * Nova fábrica orientada a i18n.
     * Usa os args para interpolar a mensagem definida em messages_xx.properties.
     *
     * Exemplo:
     * throw DomainException.of(ErrorCode.MEDIA_TITLE_TOO_LONG, maxLength);
     */
    public static DomainException of(ErrorCode errorCode, Object... args) {
        return new DomainException(errorCode, Map.of(), null, null, args);
    }
}
