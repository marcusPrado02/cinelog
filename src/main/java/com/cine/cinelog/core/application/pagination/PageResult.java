package com.cine.cinelog.core.application.pagination;

import java.io.Serializable;
import java.util.List;

/**
 * Classe de configuração Spring para gerenciamento de pageresult.
 *
 * <p>
 * Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.
 * </p>
 *
 * @since 1.0
 */

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) implements Serializable {
    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
