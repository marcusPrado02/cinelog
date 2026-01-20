package com.cine.cinelog.core.application.pagination;

import org.hibernate.query.Page;
/**
 * Classe de configuração Spring para gerenciamento de pagequery.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public record PageQuery(
        int page,
        int size,
        String sort,
        String direction) {
    public PageQuery {
        if (page < 0)
            page = 0;
        if (size <= 0)
            size = 20;
        if (size > 100)
            size = 100;
        if (sort == null || sort.isBlank())
            sort = "id";
        if (direction == null || direction.isBlank())
            direction = "ASC";
    }

    public PageQuery(int page, int size) {
        this(page, size, "id", "ASC");
    }

    public String toString() {
        return "PageQuery{" +
                "page=" + page +
                ", size=" + size +
                ", sort='" + sort + '\'' +
                ", direction='" + direction + '\'' +
                '}';
    }

}