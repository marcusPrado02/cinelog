package com.cine.cinelog.core.domain.error;
/**
 * Classe de configuração Spring para gerenciamento de notfoundexception.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
