package com.cine.cinelog.features.batch.jobs.media;

import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.tmdb.TmdbDiscoverQuery;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSearchResult;
import com.cine.cinelog.features.batch.config.BatchJobProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ItemReader} que lê páginas de mídias do TMDB usando a API Discover.
 *
 * <p>
 * Carrega antecipadamente todas as páginas configuradas (eager load) para
 * evitar
 * problemas de lazy-iteration com o WebClient bloqueante.
 * </p>
 */
@Component
@StepScope
public class TmdbMediaPageReader implements ItemReader<TmdbMediaSummary> {

    private static final Logger log = LoggerFactory.getLogger(TmdbMediaPageReader.class);

    private final TmdbClientPort tmdbClient;
    private final BatchJobProperties batchProperties;

    /**
     * Tipo de mídia injetado como parâmetro de job (MOVIE ou SERIES).
     */
    @Value("#{jobParameters['mediaType']}")
    private String mediaTypeParam;

    /**
     * Máximo de páginas a ler (injetado como parâmetro de job; usa default se
     * ausente).
     */
    @Value("#{jobParameters['maxPages'] ?: 0}")
    private Long maxPagesParam;

    private List<TmdbMediaSummary> buffer;
    private int bufferIndex;

    public TmdbMediaPageReader(TmdbClientPort tmdbClient, BatchJobProperties batchProperties) {
        this.tmdbClient = tmdbClient;
        this.batchProperties = batchProperties;
    }

    @Override
    public TmdbMediaSummary read() {
        if (buffer == null) {
            buffer = loadAllPages();
            bufferIndex = 0;
        }
        if (bufferIndex >= buffer.size()) {
            return null; // fim
        }
        return buffer.get(bufferIndex++);
    }

    private List<TmdbMediaSummary> loadAllPages() {
        MediaType type = MediaType.valueOf(mediaTypeParam);
        long maxPg = maxPagesParam != null && maxPagesParam > 0 ? maxPagesParam : batchProperties.getMaxPages();

        List<TmdbMediaSummary> all = new ArrayList<>();

        for (int page = 1; page <= maxPg; page++) {
            TmdbDiscoverQuery query = TmdbDiscoverQuery.builder()
                    .type(type)
                    .sortBy(batchProperties.getSortBy())
                    .page(page)
                    .build();

            TmdbSearchResult<TmdbMediaSummary> result;
            if (type == MediaType.MOVIE) {
                result = tmdbClient.discoverMovies(query);
            } else {
                result = tmdbClient.discoverTvShows(query);
            }

            if (result == null || result.getResults() == null || result.getResults().isEmpty()) {
                log.debug("Nenhum resultado na página {} para {}; encerrando leitura.", page, type);
                break;
            }

            all.addAll(result.getResults());
            log.debug("Lidas {} mídias (tipo={}, página={}/{})", result.getResults().size(), type, page, maxPg);

            if (page >= result.getTotalPages()) {
                break;
            }
        }

        log.info("Total de {} mídias carregadas do TMDB para processamento (tipo={}).", all.size(), type);
        return all;
    }
}
