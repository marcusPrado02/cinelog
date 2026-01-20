package com.cine.cinelog.core.domain.error;
/**
 * Classe de configuração Spring para gerenciamento de forbiddenoperationexception.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}