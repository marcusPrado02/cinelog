package com.cine.cinelog.shared.web.dto;

import java.util.List;

/**
 * DTO genérico de resposta paginada para endpoints que retornam listas.
 * 
 * <p>
 * Encapsula os dados de uma página de resultados com metadados de paginação:
 * <ul>
 * <li>content: lista de itens da página atual (tipo genérico T)</li>
 * <li>page: número da página atual (começa em 0)</li>
 * <li>size: quantidade de itens por página</li>
 * <li>totalElements: total de elementos em todas as páginas</li>
 * <li>totalPages: total de páginas disponíveis</li>
 * <li>hasNext: indica se existe próxima página</li>
 * <li>hasPrevious: indica se existe página anterior</li>
 * </ul>
 * 
 * <p>
 * Utilizado para padronizar respostas paginadas na API REST.
 * 
 * @param <T> tipo dos elementos da lista
 * @since 1.0
 */
public record PageResponse<T>(
                List<T> content,
                int page,
                int size,
                long totalElements,
                int totalPages,
                boolean hasNext,
                boolean hasPrevious) {
}