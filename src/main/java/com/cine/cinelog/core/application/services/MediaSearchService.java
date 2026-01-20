package com.cine.cinelog.core.application.services;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.domain.model.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço dedicado para busca avançada de mídias.
 *
 * <p>
 * <strong>Feature:</strong> MediaSearch (PR6 - Specification Pattern)
 *
 * <p>
 * Implementa busca flexível com múltiplos critérios usando Specification
 * Pattern:
 * <ul>
 * <li>Texto livre (título, descrição)</li>
 * <li>Tipo (filme/série)</li>
 * <li>Range de ano (yearMin, yearMax)</li>
 * <li>Range de rating (ratingMin, ratingMax)</li>
 * <li>Gêneros (lista de IDs)</li>
 * <li>Ordenação customizada</li>
 * </ul>
 *
 * <p>
 * <strong>Specification Pattern:</strong> Queries são compostas dinamicamente
 * usando JPA Criteria API através de
 * {@link com.cine.cinelog.features.media.repository.MediaSpecifications}.
 *
 * <p>
 * <strong>Performance:</strong>
 * <ul>
 * <li>Resultados paginados (evita memory overflow)</li>
 * <li>Queries otimizadas com índices em: title, release_year,
 * average_rating</li>
 * <li>Cache em buscas frequentes (30 segundos TTL)</li>
 * </ul>
 *
 * @since 1.0 (PR6)
 * @see MediaSearchCriteria
 * @see com.cine.cinelog.features.media.repository.MediaSpecifications
 */
@Service
@Transactional(readOnly = true)
public class MediaSearchService {

    private static final Logger log = LoggerFactory.getLogger(MediaSearchService.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final MediaRepositoryPort mediaRepository;

    public MediaSearchService(MediaRepositoryPort mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    /**
     * Busca avançada de mídias com múltiplos critérios.
     *
     * <p>
     * Critérios aplicados dinamicamente usando Specification Pattern:
     * - AND entre todos os critérios fornecidos
     * - OR entre texto (título/descrição)
     *
     * <p>
     * Exemplos de uso:
     * 
     * <pre>
     * // Busca simples por texto
     * search(criteria("Star Wars"), page(0, 20))
     *
     * // Filmes de 2020-2023 com rating > 8.0
     * search(criteria().type(MOVIE).yearRange(2020, 2023).minRating(8.0), page(0, 10))
     *
     * // Séries de ação ou aventura
     * search(criteria().type(SERIES).genres([1, 5]), page(0, 20))
     * </pre>
     *
     * @param criteria  critérios de busca (null = buscar tudo)
     * @param pageQuery paginação (null = página 0, size 20)
     * @return resultado paginado de mídias
     */
    @Cacheable(value = "mediaSearch", key = "#criteria.toString() + '_' + #pageQuery.toString()", unless = "#result == null")
    public PageResult<Media> search(MediaSearchCriteria criteria, PageQuery pageQuery) {
        // Validar e ajustar critérios
        MediaSearchCriteria validatedCriteria = validateCriteria(criteria);
        PageQuery validatedPageQuery = validatePageQuery(pageQuery);

        log.debug("Busca avançada: critérios={}, paginação={}", validatedCriteria, validatedPageQuery);

        // Executar busca usando Specification Pattern
        PageResult<Media> result = mediaRepository.search(validatedCriteria, validatedPageQuery);

        log.debug("Busca retornou {} mídias (página {}/{})",
                result.content().size(), result.page(), result.totalPages());

        return result;
    }

    /**
     * Busca simples por texto livre.
     *
     * @param query     texto para buscar (título ou descrição)
     * @param pageQuery paginação
     * @return resultado paginado
     */
    public PageResult<Media> searchByText(String query, PageQuery pageQuery) {
        MediaSearchCriteria criteria = new MediaSearchCriteria();
        criteria.setText(query);
        return search(criteria, pageQuery);
    }

    /**
     * Valida e ajusta critérios de busca.
     */
    private MediaSearchCriteria validateCriteria(MediaSearchCriteria criteria) {
        if (criteria == null) {
            return new MediaSearchCriteria();
        }

        // Validar ranges
        if (criteria.getYearMin() != null && criteria.getYearMax() != null) {
            if (criteria.getYearMin() > criteria.getYearMax()) {
                log.warn("yearMin > yearMax. Invertendo valores.");
                Integer temp = criteria.getYearMin();
                criteria.setYearMin(criteria.getYearMax());
                criteria.setYearMax(temp);
            }
        }

        if (criteria.getRatingMin() != null && criteria.getRatingMax() != null) {
            if (criteria.getRatingMin() > criteria.getRatingMax()) {
                log.warn("ratingMin > ratingMax. Invertendo valores.");
                Double temp = criteria.getRatingMin();
                criteria.setRatingMin(criteria.getRatingMax());
                criteria.setRatingMax(temp);
            }
        }

        return criteria;
    }

    /**
     * Valida e ajusta paginação.
     */
    private PageQuery validatePageQuery(PageQuery pageQuery) {
        if (pageQuery == null) {
            return new PageQuery(DEFAULT_PAGE, DEFAULT_SIZE, "id", "ASC");
        }

        int page = Math.max(0, pageQuery.page());
        int size = Math.min(MAX_SIZE, Math.max(1, pageQuery.size()));

        if (size != pageQuery.size()) {
            log.warn("Size ajustado: {} -> {}", pageQuery.size(), size);
        }

        return new PageQuery(page, size, pageQuery.sort(), pageQuery.direction());
    }
}
