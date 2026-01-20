package com.cine.cinelog.shared.web.dto;

import com.cine.cinelog.core.application.pagination.PageResult;

import java.util.List;
import java.util.function.Function;

/**
 * Mapper utilitário para converter PageResult (domínio) em PageResponse (DTO).
 * 
 * <p>
 * Facilita a conversão de resultados paginados da camada de aplicação para DTOs
 * da API REST.
 * Aplica uma função de transformação em cada elemento da página, permitindo
 * mapear
 * objetos de domínio para seus respectivos DTOs de resposta.
 * 
 * <p>
 * Exemplo de uso:
 * 
 * <pre>
 * PageResult&lt;Media&gt; pageResult = service.listMedia(...);
 * PageResponse&lt;MediaResponse&gt; response = PageResponseMapper.from(pageResult, mediaMapper::toResponse);
 * </pre>
 * 
 * @since 1.0
 */
public final class PageResponseMapper {

    private PageResponseMapper() {
    }

    /**
     * Converte PageResult em PageResponse aplicando função de mapeamento.
     * 
     * @param <T>        tipo do elemento no PageResult (domínio)
     * @param <R>        tipo do elemento no PageResponse (DTO)
     * @param pageResult resultado paginado da camada de aplicação
     * @param mapper     função para mapear de T para R
     * @return PageResponse com elementos transformados
     */
    public static <T, R> PageResponse<R> from(PageResult<T> pageResult, Function<T, R> mapper) {
        List<R> content = pageResult.content()
                .stream()
                .map(mapper)
                .toList();

        return new PageResponse<>(
                content,
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages(),
                pageResult.hasNext(),
                pageResult.hasPrevious());
    }
}