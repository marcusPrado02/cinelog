package com.cine.cinelog.core.domain.error;
/**
 * Classe de configuração Spring para gerenciamento de duplicateexception.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public class DuplicateException extends RuntimeException {
    public DuplicateException(String message) {
        super(message);
    }
}