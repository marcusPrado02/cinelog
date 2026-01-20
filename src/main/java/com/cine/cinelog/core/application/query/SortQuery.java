package com.cine.cinelog.core.application.query;
/**
 * Classe de configuração Spring para gerenciamento de sortquery.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public record SortQuery(
        String field,
        SortDirection direction) {
}
