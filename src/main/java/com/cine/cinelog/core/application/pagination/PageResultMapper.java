package com.cine.cinelog.core.application.pagination;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;
/**
 * Mapper responsável pela conversão entre PageResult e seus DTOs.
 * 
 * <p>Utiliza MapStruct para gerar implementações automatizadas das conversões
 * entre a camada de domínio e a camada de apresentação/persistência.</p>
 * 
 * @since 1.0
 * @see PageResult
 */

public class PageResultMapper {

    public static <T, R> PageResult<R> from(Page<T> page, Function<T, R> mapper) {
        List<R> content = page.getContent()
                .stream()
                .map(mapper)
                .toList();

        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
