package com.cine.cinelog.core.domain.enums;
/**
 * Classe de configuração Spring para gerenciamento de userrole.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public enum UserRole {
    USER,
    ADMIN;

    public String asSpringRole() {
        return "ROLE_" + name();
    }
}