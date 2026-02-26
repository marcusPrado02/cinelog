package com.cine.cinelog.shared.security;

/**
 * Exceção lançada quando um limite de negócio é excedido.
 *
 * <p>
 * A04 (OWASP) — Diferente de {@code AccessDeniedException} (que é sobre
 * permissão),
 * esta exceção indica que o usuário <b>tem permissão</b> mas excedeu
 * a <b>cota de uso</b> do recurso.
 * </p>
 *
 * <p>
 * Mapeada para HTTP 429 (Too Many Requests) no {@code GlobalExceptionHandler}.
 * </p>
 *
 * @since 1.1
 * @see BusinessLimitValidator
 */
public class BusinessLimitExceededException extends RuntimeException {

    public BusinessLimitExceededException(String message) {
        super(message);
    }
}
