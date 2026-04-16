package com.cine.cinelog.features.reports.pdf;

/**
 * Exceção lançada quando a geração de PDF via Gotenberg falha.
 *
 * <p>
 * Cenários comuns:
 * <ul>
 * <li>Container Gotenberg indisponível</li>
 * <li>Template Thymeleaf inválido</li>
 * <li>Timeout na conversão HTML → PDF</li>
 * <li>PDF desabilitado via configuração</li>
 * </ul>
 *
 * @since 2.1
 */
public class GotenbergException extends RuntimeException {

    public GotenbergException(String message) {
        super(message);
    }

    public GotenbergException(String message, Throwable cause) {
        super(message, cause);
    }
}
